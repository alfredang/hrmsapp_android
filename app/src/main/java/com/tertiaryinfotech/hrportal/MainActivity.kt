package com.tertiaryinfotech.hrportal

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.hilt.navigation.compose.hiltViewModel
import com.tertiaryinfotech.hrportal.push.Push
import com.tertiaryinfotech.hrportal.ui.AuthViewModel
import com.tertiaryinfotech.hrportal.ui.RootScreen
import com.tertiaryinfotech.hrportal.ui.theme.TertiaryHRMSTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) takeLink(intent)
        setContent {
            TertiaryHRMSTheme {
                val auth: AuthViewModel = hiltViewModel()
                RootScreen(auth)
            }
        }
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
