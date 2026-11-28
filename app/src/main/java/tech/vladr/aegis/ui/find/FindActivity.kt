package tech.vladr.aegis.ui.find

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import tech.vladr.aegis.ui.theme.AegisTheme

/**
 * The search for one Bluetooth device, opened from its page with an explicit
 * intent that carries its address and name. The screen stays on while it is
 * open: the search is a walk with the phone in hand, and a screen that goes
 * dark halfway stops the scan.
 */
class FindActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val address = intent.getStringExtra(EXTRA_ADDRESS)
        if (address == null) {
            finish()
            return
        }
        val name = intent.getStringExtra(EXTRA_NAME).orEmpty().ifEmpty { "the device" }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContent {
            AegisTheme {
                FindScreen(viewModel = viewModel(factory = FindViewModel.factory(address)), name = name, onBack = ::finish)
            }
        }
    }

    companion object {
        private const val EXTRA_ADDRESS = "tech.vladr.aegis.ADDRESS"
        private const val EXTRA_NAME = "tech.vladr.aegis.NAME"

        /** The explicit intent that starts a search for the device at [address]. */
        fun intent(context: Context, address: String, name: String): Intent =
            Intent(context, FindActivity::class.java).putExtra(EXTRA_ADDRESS, address).putExtra(EXTRA_NAME, name)
    }
}
