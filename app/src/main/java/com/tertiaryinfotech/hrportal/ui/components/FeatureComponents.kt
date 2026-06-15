package com.tertiaryinfotech.hrportal.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tertiaryinfotech.hrportal.ui.theme.Brand
import kotlinx.coroutines.launch

/** A glassy card surface used across feature screens. */
@Composable
fun Card(
    modifier: Modifier = Modifier,
    padding: Int = 16,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White.copy(alpha = 0.07f))
            .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(18.dp))
            .padding(padding.dp),
        content = content,
    )
}

/** Small stat tile (value + label) for dashboards. */
@Composable
fun StatTile(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    tint: Color = Brand.Sky,
) {
    Card(modifier = modifier) {
        if (icon != null) Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
        Text(value, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 22.sp)
        Text(label, color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
    }
}

/** Status pill (leave/expense status colouring). */
@Composable
fun StatusPill(status: String) {
    val color = when (status.uppercase()) {
        "APPROVED", "PAID" -> Brand.Green
        "PENDING" -> Brand.Yellow
        "REJECTED", "CANCELLED" -> Brand.Red
        else -> Brand.Sky
    }
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(color.copy(alpha = 0.25f))
            .border(1.dp, color.copy(alpha = 0.5f), CircleShape)
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Text(
            status.replaceFirstChar { it.uppercase() },
            color = color, fontSize = 11.sp, fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
fun EmptyHint(icon: ImageVector, text: String) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(icon, contentDescription = null, tint = Color.White.copy(alpha = 0.6f), modifier = Modifier.size(34.dp))
        Text(text, color = Color.White.copy(alpha = 0.7f), fontSize = 14.sp, textAlign = TextAlign.Center)
    }
}

/** Async UI state machine: idle / loading / loaded / failed. Mirrors iOS LoadState. */
sealed interface LoadState<out T> {
    data object Idle : LoadState<Nothing>
    data object Loading : LoadState<Nothing>
    data class Loaded<T>(val value: T) : LoadState<T>
    data class Failed(val message: String) : LoadState<Nothing>
}

/**
 * A generic async-content container: shows a spinner, an error with retry, or content.
 * Kicks off [load] on first appearance when the state is Idle. Mirrors iOS AsyncContent.
 */
@Composable
fun <T> AsyncContent(
    state: LoadState<T>,
    load: suspend () -> Unit,
    content: @Composable (T) -> Unit,
) {
    LaunchedEffect(Unit) {
        if (state is LoadState.Idle) load()
    }
    when (state) {
        is LoadState.Idle, is LoadState.Loading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color.White)
            }
        }
        is LoadState.Failed -> {
            Column(
                modifier = Modifier.fillMaxSize().padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Icon(Icons.Filled.WifiOff, contentDescription = null, tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(40.dp))
                Text(
                    state.message, color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp,
                    textAlign = TextAlign.Center, modifier = Modifier.padding(vertical = 14.dp),
                )
                val scope = rememberCoroutineScope()
                Button(
                    onClick = { scope.launch { load() } },
                    colors = ButtonDefaults.buttonColors(containerColor = Brand.Premier),
                ) { Text("Retry") }
            }
        }
        is LoadState.Loaded -> content(state.value)
    }
}
