package kg.nurtelecom.o.talkingavatar.ui.mainScreen.model

data class Viseme(val shape: String, val startTime: Float, val duration: Float)

val letterToViseme = mapOf(
    "а" to "Fcl_MTH_A", "я" to "Fcl_MTH_A",
    "е" to "Fcl_MTH_I", "э" to "Fcl_MTH_I", "и" to "Fcl_MTH_I", "й" to "Fcl_MTH_I",
    "о" to "Fcl_MTH_O",
    "у" to "Fcl_MTH_U", "ю" to "Fcl_MTH_U",
    "п" to "Fcl_MTH_Small", "Fcl_MTH_Small" to "Fcl_MTH_Small", "м" to "Fcl_MTH_Small",
    "ф" to "Fcl_MTH_Small", "в" to "Fcl_MTH_Small",
    "к" to "Fcl_MTH_Neutral", "г" to "Fcl_MTH_Neutral", "х" to "Fcl_MTH_Neutral",
    "т" to "Fcl_MTH_Neutral", "д" to "Fcl_MTH_Neutral", "н" to "Fcl_MTH_Neutral",
    "с" to "Fcl_MTH_Neutral", "з" to "Fcl_MTH_Neutral", "р" to "Fcl_MTH_Neutral",
    "л" to "Fcl_MTH_Neutral", "ж" to "Fcl_MTH_Neutral", "ш" to "Fcl_MTH_Neutral", "ч" to "Fcl_MTH_Neutral",
    " " to "Fcl_MTH_Neutral", "," to "Fcl_MTH_Neutral", "." to "Fcl_MTH_Neutral", "?" to "Fcl_MTH_Neutral", "!" to "Fcl_MTH_Neutral"
)

fun textToVisemes(text: String, totalDuration: Float? = null, defaultDuration: Float = 0.15f): List<Viseme> {
    val visemes = mutableListOf<Viseme>()
    var currentTime = 0f
    val letters = text.lowercase().toList()

    if (totalDuration != null && letters.isNotEmpty()) {
        val durationPerLetter = totalDuration / letters.size
        letters.forEach { char ->
            val letter = char.toString()
            val visemeShape = letterToViseme[letter] ?: "Fcl_MTH_Neutral"
            visemes.add(Viseme(visemeShape, currentTime, durationPerLetter))
            currentTime += durationPerLetter
        }
    } else {
        letters.forEach { char ->
            val letter = char.toString()
            val visemeShape = letterToViseme[letter] ?: "Fcl_MTH_Neutral"
            visemes.add(Viseme(visemeShape, currentTime, defaultDuration))
            currentTime += defaultDuration
        }
    }

    return visemes
}