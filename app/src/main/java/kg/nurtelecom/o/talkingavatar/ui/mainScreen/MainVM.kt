package kg.nurtelecom.o.talkingavatar.ui.mainScreen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kg.nurtelecom.o.talkingavatar.data.api.ApiService
import kg.nurtelecom.o.talkingavatar.data.models.QuestionRequest
import kotlinx.coroutines.launch
import org.orbitmvi.orbit.Container
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.container

data class MainState(
    val isListening: Boolean = false,
    val question: String = "",
    val answer: String = "",
    val isSpeaking: Boolean = false,
    val isPreparing: Boolean = false,
    val error: String? = null,
)

sealed class MainSideEffect {
    data class StartSpeechRecognition(val language: String = "ru-RU") : MainSideEffect()
    data class SpeakAnswer(val text: String) : MainSideEffect()
    data object StopSpeaking : MainSideEffect()
    data class ShowError(val message: String) : MainSideEffect()
}

class MainViewModel(private val apiService: ApiService) : ViewModel(),
    ContainerHost<MainState, MainSideEffect> {

    override val container: Container<MainState, MainSideEffect> = viewModelScope.container(MainState())

    fun stopAndRestart(){
        onSpeakingFinished()
        startListening()
    }

    fun startListening() = intent {
        reduce { state.copy(isListening = true) }
        postSideEffect(MainSideEffect.StartSpeechRecognition())
    }

    fun onSpeechResult(question: String) = intent {
        reduce { state.copy(isListening = false, question = question) }
        viewModelScope.launch {
            try {
                val response = apiService.askQuestion(QuestionRequest(question))
                reduce { state.copy(answer = response.answer, isPreparing = true) }
                postSideEffect(MainSideEffect.SpeakAnswer(response.answer))
            } catch (e: Exception) {
                reduce { state.copy(error = e.message) }
                postSideEffect(MainSideEffect.ShowError(e.message ?: "Network error"))
            }
        }
    }

    fun onSpeakingStarted() = intent {
        reduce { state.copy(isSpeaking = true, isPreparing = false) }
    }

    fun onSpeakingFinished() = intent {
        reduce { state.copy(isSpeaking = false, isPreparing = false) }
        postSideEffect(MainSideEffect.StopSpeaking)
    }
}