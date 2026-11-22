package com.tertiaryinfotech.hrportal.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MarkEmailUnread
import androidx.compose.material.icons.filled.Numbers
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.tertiaryinfotech.hrportal.data.LoginDefaults
import com.tertiaryinfotech.hrportal.ui.components.BannerKind
import com.tertiaryinfotech.hrportal.ui.components.BrandHeader
import com.tertiaryinfotech.hrportal.ui.components.GoogleSignInButton
import com.tertiaryinfotech.hrportal.ui.components.OrDivider
import com.tertiaryinfotech.hrportal.ui.components.PremierButton
import com.tertiaryinfotech.hrportal.ui.components.PremierField
import com.tertiaryinfotech.hrportal.ui.components.StatusBanner
import com.tertiaryinfotech.hrportal.ui.theme.Brand

/**
 * The login frontend (iOS `LoginView`): one email screen whose primary action follows the email (Google for
 * company addresses, a one-time code for everyone else), then the code or password step, on the Premier Blue backdrop. Authenticates real employees against the Coolify-hosted HRMS backend.
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
                .background(Brand.Surface)
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

/**
 * One screen, one tap (Alfred, 22 Nov 2026; iOS `emailStep`): a company email signs in with Google, any other email
 * (interns, part-time staff) is emailed a one-time code. Company emails also get a secondary "Email me a sign-in code"
 * button; every email gets the interns / part-time caption; a password is the last option for everyone.
 */
@Composable
private fun EmailStep(auth: AuthViewModel) {
    val context = LocalContext.current
    SectionTitle(LoginDefaults.title(auth.email), LoginDefaults.SUBTITLE)
    PremierField(
        title = "Email", icon = Icons.Filled.Email,
        value = auth.email, onValueChange = { auth.email = it },
        keyboardType = KeyboardType.Email, imeAction = ImeAction.Go,
        onSubmit = { auth.continueFromEmail(context) },
    )
    RememberRow(auth)
    if (auth.usesGoogleByDefault) {
        GoogleDismissWatcher(auth)
        Box(Modifier.semantics { testTag = "login.google" }) {
            GoogleSignInButton(loading = auth.isWorking, enabled = !auth.isWorking) { auth.signInWithGoogle(context) }
        }
        Text(
            "Signs in with your company Google account.",
            color = Brand.TextSecondary, fontSize = 12.sp, textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        // The code sign-in as a full-width button (Alfred, 22 Nov 2026: "make the OTP button more visible").
        CodeInsteadButton(enabled = !auth.isWorking) { auth.sendOTP() }
    } else {
        Box(Modifier.semantics { testTag = "login.sendCode" }) {
            PremierButton(
                title = "Email me a sign-in code", icon = Icons.Filled.MarkEmailUnread,
                loading = auth.isWorking, enabled = auth.email.isNotEmpty(),
                onClick = { auth.sendOTP() },
            )
        }
    }
    // Every email: who signs in with a code (Alfred, 22 Nov 2026: "indicate that interns can sign in using OTP").
    Row(
        modifier = Modifier.fillMaxWidth().semantics(mergeDescendants = true) { testTag = "login.internHint" },
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.School, contentDescription = null, tint = Brand.TextSecondary, modifier = Modifier.size(16.dp))
        Text(
            "Interns and part-time staff: sign in with a code sent to your email.",
            color = Brand.TextSecondary, fontSize = 12.sp, textAlign = TextAlign.Center,
            modifier = Modifier.padding(start = 6.dp),
        )
    }
    // Password is the last option (Alfred, 22 Nov 2026).
    Text(
        "Sign in with a password", color = Brand.TextSecondary, fontSize = 12.sp, textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().clickable { auth.usePassword() }.padding(vertical = 4.dp)
            .semantics { testTag = "login.usePassword" },
    )
}

/**
 * "Email me a sign-in code" as a secondary button under Continue with Google: the same height and corners as the
 * other buttons, a light-blue chip fill, accent text and a thin accent border (iOS `login.codeInstead`).
 */
@Composable
private fun CodeInsteadButton(enabled: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(Brand.Corner.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(Brand.ControlHeight.dp)
            .alpha(if (enabled) 1f else 0.55f)
            .clip(shape)
            .background(Brand.SurfaceStrong)
            .border(1.dp, Brand.Accent.copy(alpha = 0.35f), shape)
            .clickable(enabled = enabled, onClick = onClick)
            .semantics { testTag = "login.codeInstead" },
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.MarkEmailUnread, contentDescription = null, tint = Brand.Accent, modifier = Modifier.size(20.dp))
        Text("Email me a sign-in code", color = Brand.Accent, fontWeight = FontWeight.SemiBold, fontSize = 17.sp,
            modifier = Modifier.padding(start = 10.dp))
    }
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
    // Google only for company emails.
    if (auth.usesGoogleByDefault) GoogleOption(auth)
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
    // Back to the first sign-in screen (Alfred, 22 Nov 2026), the same as Change email.
    Row(
        modifier = Modifier.fillMaxWidth().clickable { auth.backToEmail() }.padding(vertical = 4.dp)
            .semantics(mergeDescendants = true) { testTag = "login.backToSignIn" },
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = null, tint = Brand.Accent, modifier = Modifier.size(18.dp))
        Text("Back to sign in", color = Brand.Accent, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

/**
 * The Custom Tab is a separate task: if the user dismisses it without completing consent, no redirect ever arrives,
 * so release the suspended sign-in when we come back to the front.
 */
@Composable
private fun GoogleDismissWatcher(auth: AuthViewModel) {
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) auth.onGoogleSignInDismissed()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
}

/** Google sign-in on the password step (company emails only; hidden when the build has no Google client id). */
@Composable
private fun GoogleOption(auth: AuthViewModel) {
    if (!auth.googleSignInAvailable) return
    val context = LocalContext.current
    GoogleDismissWatcher(auth)
    OrDivider()
    GoogleSignInButton(loading = auth.isWorking, enabled = !auth.isWorking) {
        auth.signInWithGoogle(context)
    }
    Text(
        "Use your @tertiaryinfotech.com Google account.",
        color = Brand.TextSecondary, fontSize = 12.sp, textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().semantics { testTag = "login.googleHint" },
    )
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
