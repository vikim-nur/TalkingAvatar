package kg.nurtelecom.o.talkingavatar.ui.mainScreen

import android.Manifest
import android.content.Intent
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material3.Button
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieClipSpec
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import kg.nurtelecom.o.talkingavatar.R
import kg.nurtelecom.o.talkingavatar.ui.utils.AudioPlayer
import kg.nurtelecom.o.talkingavatar.ui.utils.PulseIndicator
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.orbitmvi.orbit.compose.collectAsState
import org.orbitmvi.orbit.compose.collectSideEffect

@Composable
fun MainScreen() {
    val viewModel = koinViewModel<MainViewModel>()
    val state by viewModel.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackBarHostState = remember { SnackbarHostState() }

    val audioPlayer = remember { AudioPlayer(context) }
    val speechRecognizer = remember { SpeechRecognizer.createSpeechRecognizer(context) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            if (state.isSpeaking)
                viewModel.stopAndRestart()
            else
                viewModel.startListening()
        } else scope.launch { snackBarHostState.showSnackbar("Требуется разрешение на микрофон") }
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

    LaunchedEffect(Unit) { audioPlayer.initialize() }

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
                audioPlayer.play(
                    text = sideEffect.text,
                    onStart = { viewModel.onSpeakingStarted() },
                    onFinish = { viewModel.onSpeakingFinished() },
                    onError = {
                        viewModel.onSpeakingFinished()
                        scope.launch { snackBarHostState.showSnackbar("Ошибка TTS: ${it.message}") }
                    }
                )
            }

            is MainSideEffect.ShowError -> {
                scope.launch { snackBarHostState.showSnackbar(sideEffect.message) }
            }

            MainSideEffect.StopSpeaking -> audioPlayer.stop()
        }
    }

    val composition by rememberLottieComposition(LottieCompositionSpec.Asset("talking_man.json"))
    val progress by animateLottieCompositionAsState(
        composition,
        iterations = LottieConstants.IterateForever,
        clipSpec = LottieClipSpec.Progress(0.3f, 0.6f),
        isPlaying = state.isSpeaking
    )

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .sizeIn(minHeight = 300.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (state.isPreparing) {
                    PulseIndicator(
                        modifier = Modifier.padding(vertical = 36.dp),
                        icon = R.drawable.ic_thinking
                    )
                } else {
                    LottieAnimation(
                        composition = composition,
                        progress = { if (state.isSpeaking) progress else 0f },
                        modifier = Modifier
                            .height(300.dp)
                            .padding(start = 72.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = {
                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }) {
                Text("Задать вопрос")
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    audioPlayer.release()
                    viewModel.onSpeakingFinished()
                },
                enabled = state.isSpeaking
            ) {
                Text("Стоп")
            }

            SnackbarHost(hostState = snackBarHostState)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            audioPlayer.stop()
            speechRecognizer.destroy()
        }
    }
}
