package tech.vladr.aegis.ui.detail

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import tech.vladr.aegis.ui.theme.AegisTheme

/**
 * The page of one device. It is an activity of its own, opened with an
 * explicit intent - one that names this class - from the map or the scan
 * list. The intent carries the device's id as an extra.
 */
class DeviceDetailActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val deviceId = intent.getLongExtra(EXTRA_DEVICE_ID, -1)
        if (deviceId < 0) {
            finish()
            return
        }
        setContent {
            AegisTheme {
                DeviceDetailScreen(
                    viewModel = viewModel(factory = DeviceDetailViewModel.factory(deviceId)),
                    onBack = ::finish,
                )
            }
        }
    }

    companion object {
        private const val EXTRA_DEVICE_ID = "tech.vladr.aegis.DEVICE_ID"

        /** The explicit intent that opens a device's page. */
        fun intent(context: Context, deviceId: Long): Intent =
            Intent(context, DeviceDetailActivity::class.java).putExtra(EXTRA_DEVICE_ID, deviceId)
    }
}
