package com.tertiaryinfotech.hrportal.ui

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.tertiaryinfotech.hrportal.ui.components.BrandHeader
import com.tertiaryinfotech.hrportal.ui.screens.MainScaffold
import com.tertiaryinfotech.hrportal.ui.theme.GradientScreen

/** Routes between a brief launch state, the login frontend, and the signed-in home. */
@Composable
fun RootScreen(auth: AuthViewModel) {
    LaunchedEffect(Unit) { auth.bootstrap() }

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
        CircularProgressIndicator(color = Color.White, modifier = Modifier.padding(top = 22.dp))
    }
}
