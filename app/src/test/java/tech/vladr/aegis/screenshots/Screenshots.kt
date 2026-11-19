package tech.vladr.aegis.screenshots

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import java.util.Locale
import tech.vladr.aegis.ui.theme.AegisTheme
import java.util.TimeZone

/**
 * Where recorded screenshots go: next to the docs, where the README shows
 * them. Paths are relative to the app module, where Gradle runs the tests.
 */
const val SCREENSHOTS = "../docs/screenshots"

/** 2026-11-18 21:30 UTC: a fixed time, so the screens show the same clock on every run. */
const val EVENING = 1795037400000L

/** The same language and time zone on every machine, so the images do not change with whoever records them. */
fun fixLocale() {
    Locale.setDefault(Locale.UK)
    TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
}

/** A screen as the app shows it: in its theme, on its background (the app's Scaffold gives it one). */
@Composable
fun Screen(content: @Composable () -> Unit) {
    AegisTheme { Surface(color = MaterialTheme.colorScheme.background, content = content) }
}
