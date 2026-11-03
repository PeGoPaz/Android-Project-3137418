package tech.vladr.aegis

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import tech.vladr.aegis.ui.AegisApp
import tech.vladr.aegis.ui.theme.AegisTheme

/**
 * The one activity behind the main screens. Android starts it from the
 * launcher icon; everything it shows is drawn with Jetpack Compose.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Draw behind the status and navigation bars; the screens keep clear of them themselves.
        enableEdgeToEdge()
        setContent {
            AegisTheme {
                AegisApp()
            }
        }
    }
}
