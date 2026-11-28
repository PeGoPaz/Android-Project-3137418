package tech.vladr.aegis.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import tech.vladr.aegis.detection.Sensitivity

/** Your choices on the Settings screen. */
data class Settings(
    /** List networks that hide their name. */
    val showHiddenNetworks: Boolean = true,
    /** Save a scan even when the phone had no location fix; it then has no place on the map. */
    val saveWithoutLocation: Boolean = true,
    /** Raise alerts for open and WEP networks. */
    val unsafeNetworkAlerts: Boolean = true,
    /** Show a notification when a device starts following you, not only an alert in the app. */
    val followingNotifications: Boolean = true,
    /** How readily a device counts as following you. */
    val followingSensitivity: Sensitivity = Sensitivity.NORMAL,
)

private val Context.dataStore by preferencesDataStore(name = "settings")

/** Where the settings come from: the DataStore file, or fixed values in tests. */
interface SettingsSource {
    val settings: Flow<Settings>
}

/**
 * The settings, kept with Jetpack DataStore: a small file of key-value pairs,
 * read as a Flow, so every screen sees a change at once.
 */
class SettingsStore(private val context: Context) : SettingsSource {
    private object Keys {
        val showHidden = booleanPreferencesKey("show_hidden_networks")
        val saveWithoutLocation = booleanPreferencesKey("save_without_location")
        val unsafeAlerts = booleanPreferencesKey("unsafe_network_alerts")
        val followingNotifications = booleanPreferencesKey("following_notifications")
        val sensitivity = stringPreferencesKey("following_sensitivity")
    }

    override val settings: Flow<Settings> = context.dataStore.data.map { prefs ->
        val defaults = Settings()
        Settings(
            showHiddenNetworks = prefs[Keys.showHidden] ?: defaults.showHiddenNetworks,
            saveWithoutLocation = prefs[Keys.saveWithoutLocation] ?: defaults.saveWithoutLocation,
            unsafeNetworkAlerts = prefs[Keys.unsafeAlerts] ?: defaults.unsafeNetworkAlerts,
            followingNotifications = prefs[Keys.followingNotifications] ?: defaults.followingNotifications,
            // Stored by name; a name a later version no longer knows falls back to the default.
            followingSensitivity = Sensitivity.entries.find { it.name == prefs[Keys.sensitivity] } ?: defaults.followingSensitivity,
        )
    }

    suspend fun setShowHiddenNetworks(value: Boolean) = set(Keys.showHidden, value)
    suspend fun setSaveWithoutLocation(value: Boolean) = set(Keys.saveWithoutLocation, value)
    suspend fun setUnsafeNetworkAlerts(value: Boolean) = set(Keys.unsafeAlerts, value)
    suspend fun setFollowingNotifications(value: Boolean) = set(Keys.followingNotifications, value)

    suspend fun setFollowingSensitivity(value: Sensitivity) {
        context.dataStore.edit { it[Keys.sensitivity] = value.name }
    }

    private suspend fun set(key: Preferences.Key<Boolean>, value: Boolean) {
        context.dataStore.edit { it[key] = value }
    }
}
