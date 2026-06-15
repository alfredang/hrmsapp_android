package com.tertiaryinfotech.hrportal.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tertiaryinfotech.hrportal.ui.components.BrandHeader
import com.tertiaryinfotech.hrportal.ui.components.PremierButton
import com.tertiaryinfotech.hrportal.ui.components.PremierField
import com.tertiaryinfotech.hrportal.ui.components.StatusBanner
import com.tertiaryinfotech.hrportal.ui.theme.Brand

/**
 * The login frontend — the app's primary surface. Mirrors the web login: a progressive
 * email -> password / OTP flow, on the Premier Blue backdrop. Authenticates real employees
 * against the Coolify-hosted HRMS backend.
 */
@Composable
fun LoginScreen(auth: AuthViewModel) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(28.dp),
    ) {
        BrandHeader()

        Column(
            modifier = Modifier
                .padding(horizontal = 22.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(Color.White.copy(alpha = 0.06f))
                .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(24.dp))
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            auth.infoMessage?.let { StatusBanner(isError = false, text = it) }
            auth.errorMessage?.let { StatusBanner(isError = true, text = it) }

            when (auth.step) {
                AuthViewModel.Step.EMAIL -> EmailStep(auth)
                AuthViewModel.Step.PASSWORD -> PasswordStep(auth)
                AuthViewModel.Step.OTP -> OtpStep(auth)
            }
        }

        Text(
            "Authorized employees only · Secured by Tertiary Infotech",
            color = Color.White.copy(alpha = 0.55f),
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
        enabled = auth.email.isNotEmpty(), onClick = { auth.continueFromEmail() },
    )
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
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        TextButton(onClick = { auth.switchToOTP() }) { Text("Use a one-time code", color = Brand.Sky) }
        TextButton(onClick = { auth.backToEmail() }) { Text("Change email", color = Brand.Sky) }
    }
}

@Composable
private fun OtpStep(auth: AuthViewModel) {
    SectionTitle("Enter your code", "We sent a 6-digit code to ${auth.email}.")
    PremierField(
        title = "6-digit code", icon = Icons.Filled.Numbers,
        value = auth.otp, onValueChange = { auth.otp = it },
        keyboardType = KeyboardType.NumberPassword, imeAction = ImeAction.Go,
        onSubmit = { auth.verifyOTP() },
    )
    PremierButton(
        title = "Verify & sign in", icon = Icons.Filled.CheckCircle,
        loading = auth.isWorking, enabled = auth.otp.length >= 4,
        onClick = { auth.verifyOTP() },
    )
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        TextButton(onClick = { auth.sendOTP() }) { Text("Resend code", color = Brand.Sky) }
        TextButton(onClick = { auth.switchToPassword() }) { Text("Use password", color = Brand.Sky) }
    }
}

@Composable
private fun RememberRow(auth: AuthViewModel) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text("Remember my email", color = Color.White.copy(alpha = 0.85f), fontSize = 13.sp)
        Switch(
            checked = auth.rememberEmail,
            onCheckedChange = { auth.setRemember(it) },
            colors = SwitchDefaults.colors(checkedTrackColor = Brand.Azure),
        )
    }
}

@Composable
private fun SectionTitle(title: String, subtitle: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()) {
        Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 22.sp)
        Text(subtitle, color = Color.White.copy(alpha = 0.7f), fontSize = 14.sp)
    }
}
