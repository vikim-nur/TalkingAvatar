package kg.nurtelecom.o.talkingavatar

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import kg.nurtelecom.o.talkingavatar.ui.mainScreen.MainScreen
import kg.nurtelecom.o.talkingavatar.ui.theme.TalkingAvatarTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContent {
            TalkingAvatarTheme {
                MainScreen()
            }
        }
    }
}