package kg.nurtelecom.o.talkingavatar.ui.mainScreen

import android.Manifest
import android.content.Intent
import android.media.MediaPlayer
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.MotionEvent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieClipSpec
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import io.github.sceneview.Scene
import io.github.sceneview.collision.HitResult
import io.github.sceneview.node.ModelNode
import io.github.sceneview.rememberCameraManipulator
import io.github.sceneview.rememberCameraNode
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberEnvironment
import io.github.sceneview.rememberEnvironmentLoader
import io.github.sceneview.rememberMaterialLoader
import io.github.sceneview.rememberModelLoader
import io.github.sceneview.rememberNodes
import io.github.sceneview.rememberOnGestureListener
import io.github.sceneview.rememberRenderer
import io.github.sceneview.rememberScene
import io.github.sceneview.rememberView
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.orbitmvi.orbit.compose.collectAsState
import org.orbitmvi.orbit.compose.collectSideEffect
import java.io.File
import java.util.Locale

@Composable
fun MainScreen() {
    val viewModel = koinViewModel<MainViewModel>()
    val state by viewModel.collectAsState()
    val context = LocalContext.current

    val coroutineScope = rememberCoroutineScope()

    val snackBarHostState = remember { SnackbarHostState() }

    var tts by remember { mutableStateOf<TextToSpeech?>(null) }
    val speechRecognizer = remember { SpeechRecognizer.createSpeechRecognizer(context) }
    var speechStartTime by remember { mutableLongStateOf(0L) }
    var mediaPlayer: MediaPlayer? = null

    val composition by rememberLottieComposition(LottieCompositionSpec.Asset("talking_man.json"))


    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) viewModel.startListening()
        else coroutineScope.launch {
            snackBarHostState.showSnackbar("Требуется разрешение на микрофон")
        }
    }

    val speechLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val data = result.data
            val results = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            results?.firstOrNull()?.let { viewModel.onSpeechResult(it) }
        }
    }

    LaunchedEffect(Unit) {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale("ru_RU")
                val maleVoice = tts?.voices?.find {
                    it.name.contains("ruc", true) ||
                            it.name.contains("male", true) ||
                            it.name.contains("rud", true)
                }
                tts?.voice = maleVoice
            } else {
                Log.e("TTS", "Initialization failed: $status")
            }
        }
    }

    viewModel.collectSideEffect { sideEffect ->
        when (sideEffect) {
            is MainSideEffect.StartSpeechRecognition -> {
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, sideEffect.language)
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                }
                speechLauncher.launch(intent)
            }

            is MainSideEffect.SpeakAnswer -> {
                val audioFile = File(context.cacheDir, "output.wav")
                tts?.synthesizeToFile(sideEffect.text, null, audioFile, "utterance_id")
                tts?.setOnUtteranceProgressListener(object : android.speech.tts.UtteranceProgressListener() {

                    override fun onDone(utteranceId: String?) {
                        mediaPlayer?.release()
                        mediaPlayer = MediaPlayer().apply {
                            setDataSource(audioFile.absolutePath)
                            prepare()
                            speechStartTime = System.nanoTime()
                            start()
                            viewModel.onSpeakingStarted()
                            setOnCompletionListener {
                                viewModel.onSpeakingFinished()
                            }
                        }
                    }

                    override fun onError(utteranceId: String?) {
                        viewModel.onSpeakingFinished()
                    }

                    override fun onStart(p0: String?) {}
                    override fun onStop(utteranceId: String?, interrupted: Boolean) {
                        super.onStop(utteranceId, interrupted)
                    }
                })
            }

            is MainSideEffect.ShowError -> {
                coroutineScope.launch {
                    snackBarHostState.showSnackbar(sideEffect.message)
                }
            }
        }
    }

    Box {
        val engine = rememberEngine()
        val modelLoader = rememberModelLoader(engine)
        val materialLoader = rememberMaterialLoader(engine)
        val environmentLoader = rememberEnvironmentLoader(engine)

        val tanjeroNode = remember {
            ModelNode(
                modelInstance = modelLoader.createModelInstance(
                    assetFileLocation = "models/girl.glb"
                ),
                scaleToUnits = 1.0f,
                centerOrigin = _root_ide_package_.io.github.sceneview.math.Position(y = -0.6f)
            ).apply {
                renderableNodes.forEach { node ->
                    Log.d(
                        "SceneView",
                        "RenderableNode: ${node.name}, MorphTargets: ${node.morphTargetNames.joinToString()}"
                    )
                }
            }
        }
        val childNodes = rememberNodes { add(tanjeroNode) }

//        Scene(
//            modifier = Modifier.fillMaxSize(),
//            engine = engine,
//            view = rememberView(engine),
//            renderer = rememberRenderer(engine),
//            scene = rememberScene(engine),
//            modelLoader = modelLoader,
//            materialLoader = materialLoader,
//            environmentLoader = environmentLoader,
//            environment = rememberEnvironment(environmentLoader) {
//                environmentLoader.createHDREnvironment(
//                    assetFileLocation = "environments/sky_2k.hdr"
//                )!!
//            },
//            cameraNode = rememberCameraNode(engine) {
//                position = _root_ide_package_.io.github.sceneview.math.Position(z = 3.0f)
//            },
//            cameraManipulator = rememberCameraManipulator(),
//            childNodes = childNodes,
//            onGestureListener = rememberOnGestureListener(
//                onDoubleTapEvent = { event, tappedNode ->
//                    tappedNode?.let { it.scale *= 0.5f }
//                }
//            ),
//            onTouchEvent = { event: MotionEvent, hitResult: HitResult? ->
//                hitResult?.let { println("World tapped : ${it.worldPosition}") }
//                false
//            },
//            onFrame = { frameTimeNanos ->
//                val renderableNode = tanjeroNode.renderableNodes.getOrNull(1) // Face
//                val morphTargetNames = renderableNode?.morphTargetNames ?: emptyList()
//
//                if (state.isSpeaking && state.visemes.isNotEmpty() && renderableNode != null && morphTargetNames.isNotEmpty()) {
//                    val elapsedTime = (System.nanoTime() - speechStartTime) / 1_000_000_000f
//                    val currentViseme = state.visemes.find {
//                        it.startTime <= elapsedTime && elapsedTime < it.startTime + it.duration
//                    }
//                    val visemeShape = currentViseme?.shape ?: "Fcl_MTH_Neutral"
//
//                    // Создаём массив весов для всех морф-таргетов
//                    val weights = FloatArray(morphTargetNames.size)
//                    val morphIndex = morphTargetNames.indexOf(visemeShape)
//                    if (morphIndex != -1) {
//                        weights[morphIndex] = 1.0f
//                        renderableNode.setMorphWeights(weights)
//                    } else {
//                        // Fallback: нейтральная поза
//                        val restIndex = morphTargetNames.indexOf("Fcl_ALL_Neutral")
//                        if (restIndex != -1) {
//                            weights[restIndex] = 1.0f
//                            renderableNode.setMorphWeights(weights)
//                        }
//                        Log.w("SceneView", "Morph target $visemeShape not found in $morphTargetNames")
//                    }
//                }
//            }
//        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp)
                .padding(bottom = 56.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            val progress by animateLottieCompositionAsState(
                composition,
                iterations = LottieConstants.IterateForever,
                clipSpec = LottieClipSpec.Progress(0.3f, 0.6f)
            )

            LottieAnimation(
                composition = composition,
                progress = {
                    if (state.isSpeaking) { progress } else { 0f }
                },
                modifier = Modifier
                    .height(360.dp)
                    .padding(start = 80.dp),
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = {
                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }) {
                Text("Задать вопрос")
            }
            SnackbarHost(
                hostState = snackBarHostState,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
//        Button(onClick = {
//            viewModel.onSpeechResult("it")
//            val faceNode = tanjeroNode.renderableNodes.getOrNull(1)
//            val morphTargetNames = faceNode?.morphTargetNames ?: return@Button
//            val weights = FloatArray(morphTargetNames.size)
//
//            val morphIndex = morphTargetNames.indexOf("Fcl_MTH_A")
//            if (morphIndex != -1) {
//                weights[morphIndex] = 1f
//                faceNode.setMorphWeights(weights)
//                Log.d("SceneView", "Activated morph target: Fcl_MTH_A")
//            } else {
//                Log.d("SceneView", "Morph target not found")
//            }
//        }) {
//            Text("👄 Проверить морф A")
//        }
    }

    DisposableEffect(Unit) {
        onDispose {
            tts?.shutdown()
            speechRecognizer.destroy()
        }
    }
}