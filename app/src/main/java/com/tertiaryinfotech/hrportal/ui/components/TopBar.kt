package com.tertiaryinfotech.hrportal.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import com.tertiaryinfotech.hrportal.ui.theme.Brand

/**
 * A screen scaffold with the opaque navy top bar. Only full-screen dialogs use it; regular
 * destinations share the top bar in MainScaffold. The background comes from GradientScreen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrandScaffold(
    title: String,
    onBack: (() -> Unit)? = null,
    actions: @Composable () -> Unit = {},
    content: @Composable (androidx.compose.foundation.layout.PaddingValues) -> Unit,
) {
    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text(title, color = Brand.OnBar, fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Brand.BarAccent)
                        }
                    }
                },
                actions = { actions() },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Brand.Bar,
                    titleContentColor = Brand.OnBar,
                ),
            )
        },
    ) { inner ->
        Box(modifier = Modifier.fillMaxSize()) { content(inner) }
    }
}
