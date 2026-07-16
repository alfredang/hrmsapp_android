package com.tertiaryinfotech.hrportal

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.hilt.navigation.compose.hiltViewModel
import com.tertiaryinfotech.hrportal.ui.AuthViewModel
import com.tertiaryinfotech.hrportal.ui.RootScreen
import com.tertiaryinfotech.hrportal.ui.theme.Brand
import com.tertiaryinfotech.hrportal.ui.theme.TertiaryHRMSTheme
import com.tertiaryinfotech.hrportal.ui.theme.ThemePrefs
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        Brand.applyTheme(ThemePrefs.isDark(this))
        setContent {
            TertiaryHRMSTheme {
                val auth: AuthViewModel = hiltViewModel()
                RootScreen(auth)
            }
        }
    }
}
