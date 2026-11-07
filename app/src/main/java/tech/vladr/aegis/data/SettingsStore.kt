package tech.vladr.aegis.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Your choices on the Settings screen. */
data class Settings(
    /** List networks that hide their name. */
    val showHiddenNetworks: Boolean = true,
    /** Save a scan even when the phone had no location fix; it then has no place on the map. */
    val saveWithoutLocation: Boolean = true,
    /** Raise alerts for open and WEP networks. */
    val unsafeNetworkAlerts: Boolean = true,
)

private val Context.dataStore by preferencesDataStore(name = "settings")

/**
 * The settings, kept with Jetpack DataStore: a small file of key-value pairs,
 * read as a Flow, so every screen sees a change at once.
 */
class SettingsStore(private val context: Context) {
    private object Keys {
        val showHidden = booleanPreferencesKey("show_hidden_networks")
        val saveWithoutLocation = booleanPreferencesKey("save_without_location")
        val unsafeAlerts = booleanPreferencesKey("unsafe_network_alerts")
    }

    val settings: Flow<Settings> = context.dataStore.data.map { prefs ->
        val defaults = Settings()
        Settings(
            showHiddenNetworks = prefs[Keys.showHidden] ?: defaults.showHiddenNetworks,
            saveWithoutLocation = prefs[Keys.saveWithoutLocation] ?: defaults.saveWithoutLocation,
            unsafeNetworkAlerts = prefs[Keys.unsafeAlerts] ?: defaults.unsafeNetworkAlerts,
        )
    }

    suspend fun setShowHiddenNetworks(value: Boolean) = set(Keys.showHidden, value)
    suspend fun setSaveWithoutLocation(value: Boolean) = set(Keys.saveWithoutLocation, value)
    suspend fun setUnsafeNetworkAlerts(value: Boolean) = set(Keys.unsafeAlerts, value)

    private suspend fun set(key: Preferences.Key<Boolean>, value: Boolean) {
        context.dataStore.edit { it[key] = value }
    }
}
