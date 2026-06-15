package com.tertiaryinfotech.hrportal.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.BasicTextField
import com.tertiaryinfotech.hrportal.ui.theme.Brand

/** A glassy text field styled for the dark Premier Blue surface. Mirrors iOS PremierField. */
@Composable
fun PremierField(
    title: String,
    icon: ImageVector,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    isSecure: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Next,
    onSubmit: (() -> Unit)? = null,
) {
    var focused by remember { mutableStateOf(false) }
    var revealed by remember { mutableStateOf(false) }
    val borderColor = if (focused) Brand.Sky.copy(alpha = 0.8f) else Color.White.copy(alpha = 0.22f)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(Brand.ControlHeight.dp)
            .clip(RoundedCornerShape(Brand.Corner.dp))
            .background(Color.White.copy(alpha = 0.12f))
            .border(1.dp, borderColor, RoundedCornerShape(Brand.Corner.dp))
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(20.dp))
        Box(modifier = Modifier.weight(1f).padding(start = 12.dp), contentAlignment = Alignment.CenterStart) {
            if (value.isEmpty()) {
                Text(title, color = Color.White.copy(alpha = 0.55f))
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChangedCompat { focused = it },
                singleLine = true,
                textStyle = LocalTextStyle.current.copy(color = Color.White),
                cursorBrush = SolidColor(Brand.Sky),
                visualTransformation = if (isSecure && !revealed) PasswordVisualTransformation() else VisualTransformation.None,
                keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction),
                keyboardActions = KeyboardActions(
                    onNext = { onSubmit?.invoke() },
                    onGo = { onSubmit?.invoke() },
                    onDone = { onSubmit?.invoke() },
                    onSend = { onSubmit?.invoke() },
                ),
            )
        }
        if (isSecure) {
            IconButton(onClick = { revealed = !revealed }) {
                Icon(
                    if (revealed) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                    contentDescription = if (revealed) "Hide" else "Show",
                    tint = Color.White.copy(alpha = 0.6f),
                )
            }
        }
    }
}

// Tiny wrapper so the call site reads cleanly.
private fun Modifier.onFocusChangedCompat(onChanged: (Boolean) -> Unit): Modifier =
    this.onFocusChanged { onChanged(it.isFocused) }
