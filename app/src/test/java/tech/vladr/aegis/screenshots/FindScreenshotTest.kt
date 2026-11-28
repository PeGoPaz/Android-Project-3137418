package tech.vladr.aegis.screenshots

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import tech.vladr.aegis.ble.Bearing
import tech.vladr.aegis.ble.Trend
import tech.vladr.aegis.sensors.Heading
import tech.vladr.aegis.sensors.HeadingAccuracy
import tech.vladr.aegis.ui.find.FindScreen
import tech.vladr.aegis.ui.find.FindUiState

/** The Find screen halfway through a search for an AirTag: getting warmer, the arrow a little to the right. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = "w411dp-h891dp-xxhdpi")
class FindScreenshotTest {
    @get:Rule
    val compose = createComposeRule()

    @Before
    fun locale() = fixLocale()

    @Test
    fun gettingWarmer() {
        val state = FindUiState(
            level = -64,
            trend = Trend.WARMER,
            silentFor = 1,
            heading = Heading(231f, HeadingAccuracy.HIGH),
            bearing = Bearing(255f, contrastDb = 9),
        )
        compose.setContent { Screen { FindScreen(state, name = "AirTag", onBack = {}) } }
        compose.onRoot().captureRoboImage("$SCREENSHOTS/find.png")
    }
}
