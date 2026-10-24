package fr.meowsport

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import fr.meowsport.ui.MeowSportNav
import fr.meowsport.ui.theme.MeowSportTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MeowSportTheme {
                MeowSportNav()
            }
        }
    }
}
