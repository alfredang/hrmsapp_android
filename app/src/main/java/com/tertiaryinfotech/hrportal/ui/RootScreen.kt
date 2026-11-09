package com.tertiaryinfotech.hrportal.ui

import android.app.Activity
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.tertiaryinfotech.hrportal.ui.components.BrandHeader
import com.tertiaryinfotech.hrportal.ui.screens.MainScaffold
import com.tertiaryinfotech.hrportal.ui.theme.Brand
import com.tertiaryinfotech.hrportal.ui.theme.GradientScreen
import com.tertiaryinfotech.hrportal.ui.theme.Spacing

/** Routes between a brief launch state, the login frontend, and the signed-in home. */
@Composable
fun RootScreen(auth: AuthViewModel) {
    LaunchedEffect(Unit) { auth.bootstrap() }

    // Signed in, the top and tab bars are navy, so the system-bar icons are light. On the launch
    // and login screens the backdrop is light, so they are dark.
    val view = LocalView.current
    val lightIcons = auth.phase == AuthViewModel.Phase.SIGNED_IN
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !lightIcons
                isAppearanceLightNavigationBars = !lightIcons
            }
        }
    }

    GradientScreen {
        AnimatedContent(
            targetState = auth.phase,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "phase",
        ) { phase ->
            when (phase) {
                AuthViewModel.Phase.LOADING -> LaunchView()
                AuthViewModel.Phase.SIGNED_OUT -> LoginScreen(auth)
                AuthViewModel.Phase.SIGNED_IN -> MainScaffold(auth)
            }
        }
    }
}

/** Branded splash shown while restoring any existing session. */
@Composable
private fun LaunchView() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        BrandHeader()
        CircularProgressIndicator(color = Brand.TextPrimary, modifier = Modifier.padding(top = Spacing.xl))
    }
}
