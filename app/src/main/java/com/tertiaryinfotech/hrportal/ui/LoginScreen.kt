package com.tertiaryinfotech.hrportal.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Numbers
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.tertiaryinfotech.hrportal.ui.components.BannerKind
import com.tertiaryinfotech.hrportal.ui.components.BrandHeader
import com.tertiaryinfotech.hrportal.ui.components.GoogleSignInButton
import com.tertiaryinfotech.hrportal.ui.components.OrDivider
import com.tertiaryinfotech.hrportal.ui.components.PremierButton
import com.tertiaryinfotech.hrportal.ui.components.PremierField
import com.tertiaryinfotech.hrportal.ui.components.StatusBanner
import com.tertiaryinfotech.hrportal.ui.theme.Brand

/**
 * The login frontend (iOS `LoginView`): a progressive email, then password or one-time-code flow on
 * the Premier Blue backdrop. Authenticates real employees against the Coolify-hosted HRMS backend.
 */
@Composable
fun LoginScreen(auth: AuthViewModel) {
    val panel = RoundedCornerShape(24.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Spacer(Modifier.height(24.dp))
        BrandHeader()

        Column(
            modifier = Modifier
                .padding(horizontal = 22.dp)
                .fillMaxWidth()
                .clip(panel)
                .background(if (Brand.isDark) Color.White.copy(alpha = 0.06f) else Brand.Surface)
                .border(1.dp, Brand.Border, panel)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            auth.infoMessage?.let { StatusBanner(BannerKind.Info, it) }
            auth.errorMessage?.let { StatusBanner(BannerKind.Error, it) }

            when (auth.step) {
                AuthViewModel.Step.EMAIL -> EmailStep(auth)
                AuthViewModel.Step.PASSWORD -> PasswordStep(auth)
                AuthViewModel.Step.OTP -> OtpStep(auth)
            }
        }

        Text(
            "Authorized employees only \u00b7 Secured by Tertiary Infotech",
            color = Brand.TextPrimary.copy(alpha = 0.55f),
            fontSize = 11.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp),
        )
    }
}

@Composable
private fun EmailStep(auth: AuthViewModel) {
    SectionTitle("Sign in", "Enter your work email to continue.")
    PremierField(
        title = "Work email", icon = Icons.Filled.Email,
        value = auth.email, onValueChange = { auth.email = it },
        keyboardType = KeyboardType.Email, imeAction = ImeAction.Next,
        onSubmit = { auth.continueFromEmail() },
    )
    RememberRow(auth)
    PremierButton(
        title = "Continue", icon = Icons.AutoMirrored.Filled.ArrowForward,
        enabled = auth.email.isNotEmpty(),
        onClick = { auth.continueFromEmail() },
    )
    GoogleOption(auth)
}

@Composable
private fun PasswordStep(auth: AuthViewModel) {
    SectionTitle("Welcome back", auth.email)
    PremierField(
        title = "Password", icon = Icons.Filled.Lock,
        value = auth.password, onValueChange = { auth.password = it },
        isSecure = true, imeAction = ImeAction.Go,
        onSubmit = { auth.signInWithPassword() },
    )
    PremierButton(
        title = "Sign in", icon = Icons.Filled.CheckCircle,
        loading = auth.isWorking, enabled = auth.password.isNotEmpty(),
        onClick = { auth.signInWithPassword() },
    )
    GoogleOption(auth)
    LinkRow(
        "Use a one-time code" to { auth.switchToOTP() },
        "Change email" to { auth.backToEmail() },
    )
}

@Composable
private fun OtpStep(auth: AuthViewModel) {
    SectionTitle("Enter your code", "We sent a 6-digit code to ${auth.email}.")
    PremierField(
        title = "6-digit code", icon = Icons.Filled.Numbers,
        value = auth.otp, onValueChange = { new -> auth.otp = new.filter { it.isDigit() }.take(6) },
        keyboardType = KeyboardType.Number, imeAction = ImeAction.Go,
        onSubmit = { auth.verifyOTP() },
    )
    PremierButton(
        title = "Verify & sign in", icon = Icons.Filled.CheckCircle,
        loading = auth.isWorking, enabled = auth.otp.length >= 4,
        onClick = { auth.verifyOTP() },
    )
    Text(
        "Check your spam or junk folder if you don't see the email.",
        color = Brand.TextPrimary.copy(alpha = 0.6f), fontSize = 12.sp, textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
    )
    LinkRow(
        (if (auth.isResending) "Resending\u2026" else "Resend code") to { if (!auth.isResending) auth.resendOTP() },
        "Use password" to { auth.switchToPassword() },
    )
}

/**
 * Google sign-in, offered alongside password and OTP. Hidden entirely when the build carries no
 * Google client id, so it can never dead-end the user.
 */
@Composable
private fun GoogleOption(auth: AuthViewModel) {
    if (!auth.googleSignInAvailable) return
    val context = LocalContext.current

    // The Custom Tab is a separate task: if the user dismisses it without completing consent,
    // no redirect ever arrives, so release the suspended sign-in when we come back to the front.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) auth.onGoogleSignInDismissed()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    OrDivider()
    GoogleSignInButton(loading = auth.isWorking, enabled = !auth.isWorking) {
        auth.signInWithGoogle(context)
    }
}

@Composable
private fun SectionTitle(title: String, subtitle: String) {
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(title, color = Brand.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 22.sp)
        Text(subtitle, color = Brand.TextSecondary, fontSize = 15.sp)
    }
}

@Composable
private fun LinkRow(left: Pair<String, () -> Unit>, right: Pair<String, () -> Unit>) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(left.first, color = Brand.Accent, fontSize = 13.sp, modifier = Modifier.clickable(onClick = left.second))
        Text(right.first, color = Brand.Accent, fontSize = 13.sp, modifier = Modifier.clickable(onClick = right.second))
    }
}

@Composable
private fun RememberRow(auth: AuthViewModel) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text("Remember my email", color = Brand.TextPrimary.copy(alpha = 0.85f), fontSize = 13.sp)
        Switch(
            checked = auth.rememberEmail,
            onCheckedChange = { auth.setRemember(it) },
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Brand.Azure,
                checkedBorderColor = Brand.Azure,
            ),
        )
    }
}
