package id.kantong.app

import android.os.Bundle
import android.view.View
import android.view.ViewTreeObserver
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import id.kantong.app.data.AppViewModel
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import id.kantong.app.data.AutoBackup
import id.kantong.app.data.Reminder
import id.kantong.app.ui.KantongApp
import id.kantong.app.ui.screens.LockGate
import id.kantong.app.ui.theme.KantongTheme

/** Width the design was drawn at (dp). Narrower phones are scaled down so layouts match the canvas exactly. */
private const val DESIGN_WIDTH_DP = 390f

class MainActivity : ComponentActivity() {
    private val vm: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        id.kantong.app.data.Prefs.init(this)
        Reminder.restore(this) // re-arms the daily reminder if it is on
        lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStop(owner: LifecycleOwner) { AutoBackup.onAppBackground(this@MainActivity) }
        })
        enableEdgeToEdge()
        // Keep the system splash on screen until the first data arrives: one splash only, no "Rp 0" flash.
        val root = findViewById<View>(android.R.id.content)
        root.viewTreeObserver.addOnPreDrawListener(object : ViewTreeObserver.OnPreDrawListener {
            override fun onPreDraw(): Boolean {
                if (!vm.data.value.loaded) return false
                root.viewTreeObserver.removeOnPreDrawListener(this)
                return true
            }
        })
        setContent {
            val base = LocalDensity.current
            val w = LocalConfiguration.current.screenWidthDp.toFloat()
            val scale = (w / DESIGN_WIDTH_DP).coerceIn(0.8f, 1f)
            CompositionLocalProvider(LocalDensity provides Density(base.density * scale, base.fontScale)) {
                KantongTheme { LockGate { KantongApp() } }
            }
        }
    }
}
