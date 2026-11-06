package tech.vladr.aegis.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import tech.vladr.aegis.AegisApplication
import tech.vladr.aegis.ui.alerts.AlertsScreen
import tech.vladr.aegis.ui.detail.DeviceDetailActivity
import tech.vladr.aegis.ui.map.MapScreen
import tech.vladr.aegis.ui.scan.ScanScreen
import tech.vladr.aegis.ui.settings.SettingsScreen
import tech.vladr.aegis.ui.settings.TrustedDevicesScreen

/**
 * The main screens and the bar that switches between them. Navigation
 * Compose keeps one back stack: Back from a tab returns to the map, and
 * Trusted Devices opens on top of Settings.
 */
@Composable
fun AegisApp() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val openDevice = { id: Long -> context.startActivity(DeviceDetailActivity.intent(context, id)) }
    val repository = (context.applicationContext as AegisApplication).repository
    val alertCount by repository.activeAlertCount().collectAsStateWithLifecycle(initialValue = 0)
    val backStackEntry by navController.currentBackStackEntryAsState()
    val current = backStackEntry?.destination

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                Tab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = current?.hierarchy?.any { it.route == tab.route } == true,
                        onClick = {
                            navController.navigate(tab.route) {
                                // One copy of each tab: going back leaves the app from the map.
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            // The number of alerts waiting, on the Alerts tab.
                            BadgedBox(badge = { if (tab == Tab.Alerts && alertCount > 0) Badge { Text("$alertCount") } }) {
                                Icon(tab.icon, contentDescription = null)
                            }
                        },
                        label = { Text(tab.label) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.surfaceVariant,
                        ),
                    )
                }
            }
        },
    ) { padding ->
        NavHost(navController, startDestination = Tab.Map.route, modifier = Modifier.padding(padding)) {
            composable(Tab.Map.route) { MapScreen(onDeviceClick = openDevice) }
            composable(Tab.Scan.route) { ScanScreen(onDeviceClick = openDevice) }
            composable(Tab.Alerts.route) { AlertsScreen(onDeviceClick = openDevice) }
            composable(Tab.Settings.route) { SettingsScreen(onTrustedDevices = { navController.navigate(Routes.TRUSTED) }) }
            composable(Routes.TRUSTED) { TrustedDevicesScreen(onBack = { navController.popBackStack() }) }
        }
    }
}
