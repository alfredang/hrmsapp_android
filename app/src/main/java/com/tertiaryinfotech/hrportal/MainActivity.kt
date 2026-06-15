package com.tertiaryinfotech.hrportal

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tertiaryinfotech.hrportal.ui.AuthViewModel
import com.tertiaryinfotech.hrportal.ui.RootScreen
import com.tertiaryinfotech.hrportal.ui.theme.TertiaryHRMSTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            TertiaryHRMSTheme {
                val auth: AuthViewModel = viewModel()
                RootScreen(auth)
            }
        }
    }
}
