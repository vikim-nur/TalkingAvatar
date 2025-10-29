package kg.nurtelecom.o.talkingavatar.ui.utils

import android.content.Context
import android.media.MediaPlayer
import android.speech.tts.TextToSpeech
import java.io.File
import java.util.Locale

class AudioPlayer(private val context: Context) {

    private var tts: TextToSpeech? = null
    private var mediaPlayer: MediaPlayer? = null

    fun initialize() {
        if (tts == null) {
            tts = TextToSpeech(context) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    tts?.language = Locale("ru", "RU")
                    val voice = tts?.voices?.find {
                        it.locale.language == "ru" &&
                                (it.name.contains("ruc", true)
                                        || it.name.contains("male", true)
                                        || it.name.contains("rud", true))
                    }
                    tts?.voice = voice
                }
            }
        }
    }

    fun play(
        text: String,
        onStart: () -> Unit = {},
        onFinish: () -> Unit = {},
        onError: (Throwable) -> Unit = {}
    ) {
        val audioFile = File(context.cacheDir, "tts_output.wav")
        val utteranceId = "utt_${System.currentTimeMillis()}"

        try {
            tts?.setOnUtteranceProgressListener(object : android.speech.tts.UtteranceProgressListener() {
                override fun onStart(utteranceId: String?){}

                override fun onDone(utteranceId: String?) {
                    playAudioFile(audioFile, onStart, onFinish, onError)
                }

                override fun onError(utteranceId: String?) {
                    onError(Exception("TTS ошибка преобразования текста"))
                }
            })

            val result = tts?.synthesizeToFile(text, null, audioFile, utteranceId)
            if (result == TextToSpeech.ERROR) {
                onError(Exception("Ошибка преобразования текста в файл"))
            }
        } catch (e: Exception) {
            onError(e)
        }
    }

    private fun playAudioFile(
        file: File,
        onStart: () -> Unit,
        onFinish: () -> Unit,
        onError: (Throwable) -> Unit
    ) {
        try {
            mediaPlayer?.release()
            mediaPlayer = MediaPlayer().apply {
                setDataSource(file.absolutePath)
                prepare()
                start()
                onStart()
                setOnCompletionListener {
                    onFinish()
                    release()
                }
                setOnErrorListener { _, what, extra ->
                    onError(Exception("MediaPlayer ошибка $what подробности = $extra"))
                    release()
                    true
                }
            }
        } catch (e: Exception) {
            onError(e)
        }
    }

    fun stop() {
        mediaPlayer?.let {
            if (it.isPlaying) it.stop()
            it.release()
        }
        mediaPlayer = null
        tts?.stop()
    }

    fun release() {
        stop()
        tts?.shutdown()
        tts = null
    }
}
