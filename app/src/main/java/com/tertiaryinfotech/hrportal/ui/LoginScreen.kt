package com.tertiaryinfotech.hrportal.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Numbers
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tertiaryinfotech.hrportal.ui.components.BrandHeader
import com.tertiaryinfotech.hrportal.ui.components.PremierButton
import com.tertiaryinfotech.hrportal.ui.components.PremierField
import com.tertiaryinfotech.hrportal.ui.components.StatusBanner
import com.tertiaryinfotech.hrportal.ui.theme.Brand

/**
 * The login frontend, ported 1:1 from the web app's `(auth)/login/page.tsx`: a progressive
 * email -> OTP (default) / password flow on a flat gray-950 surface with an indigo accent.
 * Authenticates real employees against the Coolify-hosted HRMS backend.
 */
@Composable
fun LoginScreen(auth: AuthViewModel) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        BrandHeader()

        Column(
            modifier = Modifier
                .padding(horizontal = 22.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(Brand.LogoCorner.dp))
                .background(Brand.Surface)
                .border(1.dp, Brand.Border, RoundedCornerShape(Brand.LogoCorner.dp))
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
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
            fontSize = 11.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp),
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
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text("Verify your identity", color = Color.White, fontWeight = FontWeight.Medium, fontSize = 15.sp)
        Text("An OTP has been sent to ${auth.email}", color = Brand.TextSecondary, fontSize = 13.sp, textAlign = TextAlign.Center)
    }
    PremierField(
        title = "000000", icon = Icons.Filled.Numbers,
        value = auth.otp, onValueChange = { auth.otp = it },
        keyboardType = KeyboardType.NumberPassword, imeAction = ImeAction.Go,
        onSubmit = { auth.verifyOTP() },
    )
    Text(
        "Check your spam/junk folder if you don't see the email in your inbox.",
        color = Brand.TextMuted, fontSize = 11.sp, textAlign = TextAlign.Center,
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
            "Resend OTP", color = Brand.Primary, fontSize = 13.sp,
            modifier = Modifier.clickable { auth.sendOTP() },
        )
        Text("   |   ", color = Brand.TextFaint, fontSize = 13.sp)
        Text(
            "Use password instead", color = Brand.TextSecondary, fontSize = 13.sp,
            modifier = Modifier.clickable { auth.switchToPassword() },
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
        Text("Default: Password123", color = Brand.TextMuted, fontSize = 11.sp)
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
    Text(text, color = Brand.TextSecondary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
}

@Composable
private fun CenteredLink(text: String, onClick: () -> Unit) {
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Text(text, color = Brand.Primary, fontSize = 13.sp, modifier = Modifier.clickable(onClick = onClick))
    }
}

@Composable
private fun BackRow(onClick: () -> Unit) {
    Row(
        modifier = Modifier.clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(
            Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back",
            tint = Brand.TextSecondary, modifier = Modifier.padding(2.dp),
        )
        Text("Back", color = Brand.TextSecondary, fontSize = 13.sp)
    }
}

@Composable
private fun RememberRow(auth: AuthViewModel) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text("Remember my email", color = Brand.TextSecondary, fontSize = 13.sp)
        Switch(
            checked = auth.rememberEmail,
            onCheckedChange = { auth.setRemember(it) },
            colors = SwitchDefaults.colors(checkedTrackColor = Brand.Primary),
        )
    }
}
