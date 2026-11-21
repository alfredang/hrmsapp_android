package com.tertiaryinfotech.hrportal.ui

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tertiaryinfotech.hrportal.ui.components.Card
import com.tertiaryinfotech.hrportal.ui.components.PremierButton
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
import com.tertiaryinfotech.hrportal.ui.theme.Appearance
import com.tertiaryinfotech.hrportal.ui.theme.Brand
import com.tertiaryinfotech.hrportal.ui.theme.GradientScreen
import com.tertiaryinfotech.hrportal.ui.theme.Spacing

/** Routes between a brief launch state, the login frontend, and the signed-in home. */
@Composable
fun RootScreen(auth: AuthViewModel) {
    LaunchedEffect(Unit) { auth.bootstrap() }

    // Signed in, the top and tab bars are dark (navy, or grey in the dark theme), so the system-bar
    // icons are light. The launch and login screens sit on the backdrop: dark icons in the light
    // theme, light icons in the dark theme.
    val view = LocalView.current
    val lightIcons = auth.phase == AuthViewModel.Phase.SIGNED_IN || Appearance.isDark
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !lightIcons
                isAppearanceLightNavigationBars = !lightIcons
            }
        }
    }

    // Force update: a newer version is live on Play, so nothing else is shown until the app is updated.
    if (UpdateGate.required) {
        GradientScreen { UpdateRequiredScreen(UpdateGate.storeUrl) }
        return
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

/** Full-screen "Update required" (iOS `UpdateRequiredView`): one button to the Play listing. */
@Composable
private fun UpdateRequiredScreen(storeUrl: String) {
    val context = LocalContext.current
    Column(
        modifier = Modifier.fillMaxSize().padding(Spacing.xl).semantics { testTag = "update.required" },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        BrandHeader()
        Card(modifier = Modifier.padding(top = Spacing.xl)) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(Icons.Filled.SystemUpdate, contentDescription = null, tint = Brand.Accent, modifier = Modifier.size(40.dp))
                Text("Update required", color = Brand.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 22.sp)
                Text(
                    "A new version of TIA HRMS is available. Please update to continue.",
                    color = Brand.TextSecondary, fontSize = 15.sp, textAlign = TextAlign.Center,
                )
                PremierButton(title = "Update now", onClick = { openStore(context, storeUrl) })
            }
        }
    }
}

/** The Play app's own page for this app when it can open it, else the listing in a browser. */
private fun openStore(context: android.content.Context, storeUrl: String) {
    val market = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=${context.packageName}"))
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    try {
        context.startActivity(market)
    } catch (_: ActivityNotFoundException) {
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(storeUrl)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (_: ActivityNotFoundException) { /* no store or browser */ }
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
