package kg.nurtelecom.o.talkingavatar.ui.mainScreen

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kg.nurtelecom.o.talkingavatar.data.api.ApiService
import kg.nurtelecom.o.talkingavatar.data.models.QuestionRequest
import kg.nurtelecom.o.talkingavatar.ui.mainScreen.model.Viseme
import kg.nurtelecom.o.talkingavatar.ui.mainScreen.model.textToVisemes
import kotlinx.coroutines.launch
import org.orbitmvi.orbit.Container
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.container
data class MainState(
    val isListening: Boolean = false,
    val question: String = "",
    val answer: String = "",
    val isSpeaking: Boolean = false,
    val error: String? = null,
    val visemes: List<Viseme> = emptyList()
)

sealed class MainSideEffect {
    data class StartSpeechRecognition(val language: String = "ru-RU") : MainSideEffect()
    data class SpeakAnswer(val text: String, val visemes: List<Viseme>) : MainSideEffect()
    data class ShowError(val message: String) : MainSideEffect()
}
class MainViewModel(private val apiService: ApiService) : ViewModel(),
    ContainerHost<MainState, MainSideEffect> {

    override val container: Container<MainState, MainSideEffect> = viewModelScope.container(MainState())

    fun startListening() = intent {
        reduce { state.copy(isListening = true) }
        postSideEffect(MainSideEffect.StartSpeechRecognition())
    }

    fun onSpeechResult(question: String) = intent {
        reduce { state.copy(isListening = false, question = question) }
        viewModelScope.launch {
            try {
                val response = apiService.askQuestion(QuestionRequest(question))
                val visemes = textToVisemes(response.answer)
                reduce { state.copy(answer = response.answer, visemes = visemes) }
                postSideEffect(MainSideEffect.SpeakAnswer(response.answer, visemes))
            } catch (e: Exception) {
                reduce { state.copy(error = e.message) }
                postSideEffect(MainSideEffect.ShowError(e.message ?: "Network error"))
            }
        }
    }

    fun onSpeakingStarted() = intent {
        reduce { state.copy(isSpeaking = true) }
    }

    fun onSpeakingFinished() = intent {
        reduce {
            Log.d("@@@", "onSpeakingFinished: ")
            state.copy(isSpeaking = false)
        }
    }
}