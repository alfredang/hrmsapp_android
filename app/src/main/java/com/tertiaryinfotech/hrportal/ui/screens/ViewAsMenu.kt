package com.tertiaryinfotech.hrportal.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.UnfoldMore
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tertiaryinfotech.hrportal.ui.AuthViewModel
import com.tertiaryinfotech.hrportal.ui.ViewAs
import com.tertiaryinfotech.hrportal.ui.components.Card
import com.tertiaryinfotech.hrportal.ui.theme.Brand

/**
 * View As menu (Home and More; iOS `ViewAsMenu`): the person's own view, the role previews (approvers) and
 * the Trainer Admin view (TRAINER_ADMIN). Options are tagged `viewAs.<key>`.
 */
@Composable
fun ViewAsMenu(auth: AuthViewModel, tag: String) {
    var open by remember { mutableStateOf(false) }
    val current = ViewAs.mode.title(auth.isApprover)
    Box {
        Card(
            padding = 12,
            onClick = { open = true },
            modifier = Modifier.semantics { testTag = tag; stateDescription = current },
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Visibility, contentDescription = null, tint = Brand.Accent, modifier = Modifier.size(22.dp))
                Text("View as", color = Brand.TextSecondary, fontSize = 15.sp, modifier = Modifier.padding(start = 10.dp).weight(1f))
                Text(current, color = Brand.TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                Icon(Icons.Filled.UnfoldMore, contentDescription = null, tint = Brand.TextSecondary,
                    modifier = Modifier.padding(start = 6.dp).size(18.dp))
            }
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            auth.availableViews.forEach { v ->
                DropdownMenuItem(
                    text = { Text(v.title(auth.isApprover)) },
                    leadingIcon = if (v == ViewAs.mode) ({ Icon(Icons.Filled.Check, contentDescription = null) }) else null,
                    onClick = { open = false; ViewAs.select(v) },
                    modifier = Modifier.semantics { testTag = "viewAs.${v.key}" },
                )
            }
        }
    }
}
