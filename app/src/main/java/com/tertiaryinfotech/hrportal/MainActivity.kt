package com.tertiaryinfotech.hrportal

import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.hilt.navigation.compose.hiltViewModel
import com.tertiaryinfotech.hrportal.push.Push
import com.tertiaryinfotech.hrportal.ui.AuthViewModel
import com.tertiaryinfotech.hrportal.ui.RootScreen
import com.tertiaryinfotech.hrportal.ui.UpdateGate
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import com.tertiaryinfotech.hrportal.ui.theme.Appearance
import com.tertiaryinfotech.hrportal.ui.theme.TertiaryHRMSTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        Appearance.readSystem(resources.configuration)
        if (savedInstanceState == null) takeLink(intent)
        setContent {
            TertiaryHRMSTheme {
                val auth: AuthViewModel = hiltViewModel()
                RootScreen(auth)
            }
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        Appearance.readSystem(newConfig) // "System" follows the device
    }

    /** Force update: on every return to the app, check which version is live on Play. */
    override fun onResume() {
        super.onResume()
        lifecycleScope.launch { UpdateGate.check() }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        takeLink(intent)
    }

    /** A tapped push alert carries the web-style `link` ("/approvals", "/leave"…) to open. */
    private fun takeLink(intent: Intent?) {
        intent?.getStringExtra(Push.EXTRA_LINK)?.takeIf { it.isNotBlank() }?.let {
            Push.pendingLink = it
            intent.removeExtra(Push.EXTRA_LINK)
        }
    }
}
