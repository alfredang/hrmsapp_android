package com.tertiaryinfotech.hrportal.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tertiaryinfotech.hrportal.ui.components.BrandHeader
import com.tertiaryinfotech.hrportal.ui.components.PremierButton
import com.tertiaryinfotech.hrportal.ui.components.PremierField
import com.tertiaryinfotech.hrportal.ui.components.StatusBanner
import com.tertiaryinfotech.hrportal.ui.theme.Brand
import com.tertiaryinfotech.hrportal.ui.theme.Spacing

/**
 * The login frontend, ported 1:1 from the web app's `(auth)/login/page.tsx`: a progressive
 * email -> OTP (default) / password flow on a flat gray-950 surface with an indigo accent.
 * Authenticates real employees against the Coolify-hosted HRMS backend. All colors, text sizes,
 * spacing and corner radii here are read from the `ui/theme` token files — nothing is hardcoded inline
 * (DESIGN_SYSTEM.md). Behavior — steps, states, and the branding fetch/skeleton — mirrors the
 * `/login` entry in SCREEN_MAP.md.
 */
@Composable
fun LoginScreen(auth: AuthViewModel) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(vertical = Spacing.huge),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.xxl),
    ) {
        BrandHeader(branding = auth.branding, loading = auth.brandingLoading)

        Column(
            modifier = Modifier
                .padding(horizontal = Spacing.xl)
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.medium)
                .background(Brand.Surface)
                .border(1.dp, Brand.Border, MaterialTheme.shapes.medium)
                .padding(Spacing.xl),
            verticalArrangement = Arrangement.spacedBy(Spacing.lg),
        ) {
            auth.errorMessage?.let { StatusBanner(isError = true, text = it) }
            auth.infoMessage?.let { StatusBanner(isError = false, text = it) }

            when (auth.step) {
                AuthViewModel.Step.EMAIL -> EmailStep(auth)
                AuthViewModel.Step.OTP -> OtpStep(auth)
                AuthViewModel.Step.PASSWORD -> PasswordStep(auth)
            }
        }

        Text(
            "Powered by Tertiary Infotech Academy Pte Ltd",
            color = Brand.TextFaint,
            style = MaterialTheme.typography.labelSmall,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = Spacing.xxxl),
        )
    }
}

/** email step — default entry point, primary action sends an OTP (mirrors `login/page.tsx:286-336`). */
@Composable
private fun EmailStep(auth: AuthViewModel) {
    FieldLabel("Email")
    PremierField(
        title = "name@company.com", icon = Icons.Filled.Email,
        value = auth.email, onValueChange = { auth.email = it },
        keyboardType = KeyboardType.Email, imeAction = ImeAction.Go,
        onSubmit = { auth.sendOTP() },
    )
    RememberRow(auth)
    PremierButton(
        title = "Send OTP", icon = Icons.Filled.Email,
        loading = auth.isWorking, enabled = auth.email.isNotEmpty(),
        onClick = { auth.sendOTP() },
    )
    CenteredLink("Sign in with password instead", onClick = { auth.continueFromEmail() })
}

/** otp step — 6-digit code verification (mirrors `login/page.tsx:339-409`). */
@Composable
private fun OtpStep(auth: AuthViewModel) {
    BackRow(onClick = { auth.backToEmail() })
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.xxs),
    ) {
        Text("Verify your identity", color = Brand.TextPrimary, style = MaterialTheme.typography.titleSmall)
        Text(
            buildAnnotatedString {
                withStyle(SpanStyle(color = Brand.TextSecondary)) { append("An OTP has been sent to ") }
                withStyle(SpanStyle(color = Brand.TextPrimary, fontWeight = FontWeight.Medium)) { append(auth.email) }
            },
            style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center,
        )
    }
    OtpField(value = auth.otp, onValueChange = { auth.otp = it }, onSubmit = { auth.verifyOTP() })
    Text(
        "Check your spam/junk folder if you don't see the email in your inbox.",
        color = Brand.TextMuted, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
    )
    PremierButton(
        title = "Verify & Sign In", icon = Icons.Filled.CheckCircle,
        loading = auth.isWorking, enabled = auth.otp.length >= 4,
        onClick = { auth.verifyOTP() },
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
    ) {
        Text(
            if (auth.isResending) "Resending..." else "Resend OTP",
            color = if (auth.isResending) Brand.TextMuted else Brand.Primary,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.clickable(enabled = !auth.isResending) { auth.resendOTP() },
        )
        Text("   |   ", color = Brand.TextFaint, style = MaterialTheme.typography.bodySmall)
        Text(
            "Use password instead", color = Brand.TextSecondary, style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.clickable { auth.switchToPassword() },
        )
    }
}

/**
 * The OTP entry field — deliberately not a [PremierField]. Web's `otp-input` has no icon, no
 * visible label, and very specific styling (`login/page.tsx:360-372`): centered, 24sp, monospace,
 * `tracking-[0.5em]` letter-spacing (≈12sp at this size). Replicated exactly here rather than
 * reusing the generic labeled-icon field style every other input on this screen uses.
 */
@Composable
private fun OtpField(value: String, onValueChange: (String) -> Unit, onSubmit: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    val borderColor = if (focused) Brand.Primary else Brand.BorderLight
    val otpTextStyle = MaterialTheme.typography.headlineSmall.copy(
        fontFamily = FontFamily.Monospace,
        letterSpacing = 12.sp,
        textAlign = TextAlign.Center,
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(Brand.ControlHeight.dp)
            .clip(MaterialTheme.shapes.small)
            .background(Brand.Border)
            .border(1.dp, borderColor, MaterialTheme.shapes.small)
            .padding(horizontal = Spacing.md),
        contentAlignment = Alignment.Center,
    ) {
        if (value.isEmpty()) {
            Text("000000", color = Brand.TextFaint, style = otpTextStyle, modifier = Modifier.fillMaxWidth())
        }
        BasicTextField(
            value = value,
            onValueChange = { new -> onValueChange(new.filter { it.isDigit() }.take(6)) },
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { focused = it.isFocused },
            singleLine = true,
            textStyle = otpTextStyle.copy(color = Brand.TextPrimary),
            cursorBrush = SolidColor(Brand.Primary),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Go),
            keyboardActions = KeyboardActions(onGo = { onSubmit() }),
        )
    }
}

/** password step — email + password fallback (mirrors `login/page.tsx:412-499`). */
@Composable
private fun PasswordStep(auth: AuthViewModel) {
    BackRow(onClick = { auth.backToEmail() })
    FieldLabel("Email")
    PremierField(
        title = "name@company.com", icon = Icons.Filled.Email,
        value = auth.email, onValueChange = { auth.email = it },
        keyboardType = KeyboardType.Email, imeAction = ImeAction.Next,
    )
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        FieldLabel("Password")
        Text("Default: Password123", color = Brand.TextMuted, style = MaterialTheme.typography.labelSmall)
    }
    PremierField(
        title = "Enter your password", icon = Icons.Filled.Lock,
        value = auth.password, onValueChange = { auth.password = it },
        isSecure = true, imeAction = ImeAction.Go,
        onSubmit = { auth.signInWithPassword() },
    )
    RememberRow(auth)
    PremierButton(
        title = "Sign In", icon = Icons.Filled.Key,
        loading = auth.isWorking, enabled = auth.password.isNotEmpty(),
        onClick = { auth.signInWithPassword() },
    )
    CenteredLink("Sign in with OTP instead", onClick = { auth.switchToOTP() })
}

@Composable
private fun FieldLabel(text: String) {
    Text(text, color = Brand.TextSecondary, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
}

@Composable
private fun CenteredLink(text: String, onClick: () -> Unit) {
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Text(
            text, color = Brand.Primary, style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.clickable(onClick = onClick),
        )
    }
}

@Composable
private fun BackRow(onClick: () -> Unit) {
    Row(
        modifier = Modifier.clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        Icon(
            Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back",
            tint = Brand.TextSecondary, modifier = Modifier.padding(Spacing.xxs),
        )
        Text("Back", color = Brand.TextSecondary, style = MaterialTheme.typography.bodySmall)
    }
}

/** Mirrors the web's literal HTML checkbox input (`login/page.tsx:307-315`) — a checkbox + label
 *  pair, not a switch. */
@Composable
private fun RememberRow(auth: AuthViewModel) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { auth.setRemember(!auth.rememberEmail) },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        Checkbox(
            checked = auth.rememberEmail,
            onCheckedChange = { auth.setRemember(it) },
            colors = CheckboxDefaults.colors(checkedColor = Brand.Primary),
        )
        Text("Remember my email", color = Brand.TextSecondary, style = MaterialTheme.typography.bodySmall)
    }
}
