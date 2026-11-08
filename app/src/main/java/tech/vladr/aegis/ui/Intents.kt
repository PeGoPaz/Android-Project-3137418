package tech.vladr.aegis.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.provider.Settings

/*
 * Implicit intents: they say what should be done, not which app does it,
 * and Android finds an app that can - or lets you choose one.
 */

/** Opens Android's Wi-Fi settings, where you can leave or forget a network. */
fun openWifiSettings(context: Context) {
    try {
        context.startActivity(Intent(Settings.ACTION_WIFI_SETTINGS))
    } catch (_: ActivityNotFoundException) {
        // A phone without that screen (some TVs, car units): the general settings will do.
        context.startActivity(Intent(Settings.ACTION_SETTINGS))
    }
}

/** Offers a text to any app that can send it: messages, email, notes. */
fun shareText(context: Context, subject: String, text: String) {
    val send = Intent(Intent.ACTION_SEND)
        .setType("text/plain")
        .putExtra(Intent.EXTRA_SUBJECT, subject)
        .putExtra(Intent.EXTRA_TEXT, text)
    context.startActivity(Intent.createChooser(send, "Share report"))
}
