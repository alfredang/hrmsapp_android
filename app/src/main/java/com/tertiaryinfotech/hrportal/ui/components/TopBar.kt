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
 * A screen scaffold with a flat `gray-950` top bar (white title, optional back button) — mirrors
 * the web app's header over its `bg-gray-950` page background. The background is provided by the
 * enclosing GradientScreen.
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
                title = { Text(title, color = Brand.TextPrimary, fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Brand.TextPrimary)
                        }
                    }
                },
                actions = { actions() },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Brand.Background,
                    titleContentColor = Brand.TextPrimary,
                ),
            )
        },
    ) { inner ->
        Box(modifier = Modifier.fillMaxSize()) { content(inner) }
    }
}
