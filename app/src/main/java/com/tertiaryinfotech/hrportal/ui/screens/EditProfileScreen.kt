package com.tertiaryinfotech.hrportal.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.tertiaryinfotech.hrportal.data.ApiException
import com.tertiaryinfotech.hrportal.data.EmployeeProfile
import com.tertiaryinfotech.hrportal.data.HrmsApi
import com.tertiaryinfotech.hrportal.ui.components.BrandScaffold
import com.tertiaryinfotech.hrportal.ui.components.PremierButton
import com.tertiaryinfotech.hrportal.ui.components.PremierField
import com.tertiaryinfotech.hrportal.ui.components.StatusBanner
import com.tertiaryinfotech.hrportal.ui.theme.GradientScreen
import com.tertiaryinfotech.hrportal.util.Fmt
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val GENDERS = listOf("MALE" to "Male", "FEMALE" to "Female", "OTHER" to "Other")
private val EDUCATION_LEVELS = listOf("DIPLOMA" to "Diploma", "DEGREE" to "Degree", "MASTER" to "Master", "PHD" to "PhD")

/**
 * Self-edit form for personal profile fields — mirrors the web's `employee-detail-editable.tsx`
 * personal-info section, `PATCH /api/employees/{id}` with `{personalInfo: {...}}`. Deliberately
 * omits employmentInfo/roles (admin-only fields) even though the route would technically accept
 * them for a self-PATCH.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileSheet(employee: EmployeeProfile, onDismiss: () -> Unit, onSaved: () -> Unit) {
    var fullName by remember { mutableStateOf(employee.name) }
    var email by remember { mutableStateOf(employee.email) }
    var phone by remember { mutableStateOf(employee.phone ?: "") }
    var nationality by remember { mutableStateOf(employee.nationality) }
    var nric by remember { mutableStateOf(employee.nric ?: "") }
    var address by remember { mutableStateOf(employee.address ?: "") }
    var gender by remember { mutableStateOf(employee.gender.ifEmpty { "MALE" }) }
    var educationLevel by remember { mutableStateOf(employee.educationLevel ?: "DIPLOMA") }
    val df = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US) }
    var dobMillis by remember { mutableStateOf(Fmt.parse(employee.dateOfBirth)?.time ?: System.currentTimeMillis()) }
    var showDob by remember { mutableStateOf(false) }
    var submitting by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        GradientScreen {
            BrandScaffold(title = "Edit profile", onBack = onDismiss) { inner ->
                Column(
                    modifier = Modifier
                        .padding(inner)
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    error?.let { StatusBanner(isError = true, text = it) }

                    FieldLabel("Full name")
                    PremierField(title = "Full name", icon = Icons.Filled.Person, value = fullName, onValueChange = { fullName = it })

                    FieldLabel("Email")
                    PremierField(title = "Email", icon = Icons.Filled.Email, value = email, onValueChange = { email = it }, keyboardType = KeyboardType.Email)

                    FieldLabel("Phone")
                    PremierField(title = "Phone", icon = Icons.Filled.Phone, value = phone, onValueChange = { phone = it }, keyboardType = KeyboardType.Phone)

                    FieldLabel("NRIC")
                    PremierField(title = "NRIC", icon = Icons.Filled.Person, value = nric, onValueChange = { nric = it })

                    FieldLabel("Nationality")
                    PremierField(title = "Nationality", icon = Icons.Filled.Public, value = nationality, onValueChange = { nationality = it })

                    FieldLabel("Address")
                    PremierField(title = "Address", icon = Icons.Filled.Home, value = address, onValueChange = { address = it }, imeAction = ImeAction.Done)

                    FieldLabel("Gender")
                    var genderExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(expanded = genderExpanded, onExpandedChange = { genderExpanded = it }) {
                        FieldBox(modifier = Modifier.menuAnchor()) {
                            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Text(GENDERS.firstOrNull { it.first == gender }?.second ?: gender, color = Color.White, modifier = Modifier.weight(1f))
                                Icon(Icons.Filled.ArrowDropDown, contentDescription = null, tint = Color.White)
                            }
                        }
                        ExposedDropdownMenu(expanded = genderExpanded, onDismissRequest = { genderExpanded = false }) {
                            GENDERS.forEach { (value, label) ->
                                DropdownMenuItem(text = { Text(label) }, onClick = { gender = value; genderExpanded = false })
                            }
                        }
                    }

                    FieldLabel("Education")
                    var eduExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(expanded = eduExpanded, onExpandedChange = { eduExpanded = it }) {
                        FieldBox(modifier = Modifier.menuAnchor()) {
                            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Text(EDUCATION_LEVELS.firstOrNull { it.first == educationLevel }?.second ?: educationLevel, color = Color.White, modifier = Modifier.weight(1f))
                                Icon(Icons.Filled.ArrowDropDown, contentDescription = null, tint = Color.White)
                            }
                        }
                        ExposedDropdownMenu(expanded = eduExpanded, onDismissRequest = { eduExpanded = false }) {
                            EDUCATION_LEVELS.forEach { (value, label) ->
                                DropdownMenuItem(text = { Text(label) }, onClick = { educationLevel = value; eduExpanded = false })
                            }
                        }
                    }

                    FieldLabel("Date of birth")
                    FieldBox(modifier = Modifier.clickable { showDob = true }) {
                        Text(Fmt.date(df.format(Date(dobMillis))), color = Color.White)
                    }
                    if (showDob) {
                        DateField(initial = dobMillis, onPick = { dobMillis = it; showDob = false }, onCancel = { showDob = false })
                    }

                    PremierButton(
                        title = "Save changes", icon = Icons.Filled.Save,
                        loading = submitting, enabled = fullName.isNotEmpty() && email.isNotEmpty() && nationality.isNotEmpty(),
                    ) {
                        error = null; submitting = true
                        scope.launch {
                            try {
                                HrmsApi.updateProfile(
                                    employee.id,
                                    mapOf(
                                        "fullName" to fullName,
                                        "email" to email,
                                        "phone" to phone,
                                        "dateOfBirth" to df.format(Date(dobMillis)),
                                        "gender" to gender,
                                        "nationality" to nationality,
                                        "nric" to nric,
                                        "address" to address,
                                        "educationLevel" to educationLevel,
                                    ),
                                )
                                submitting = false
                                onSaved()
                            } catch (ex: Exception) {
                                submitting = false
                                error = (ex as? ApiException)?.message ?: "Could not update your profile."
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Password-change form — mirrors the web's `password-change-card.tsx`,
 *  `PATCH /api/profile/password` with `{currentPassword, newPassword}`. */
@Composable
fun ChangePasswordSheet(onDismiss: () -> Unit, onSaved: () -> Unit) {
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var submitting by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var done by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        GradientScreen {
            BrandScaffold(title = "Change password", onBack = onDismiss) { inner ->
                Column(
                    modifier = Modifier
                        .padding(inner)
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    error?.let { StatusBanner(isError = true, text = it) }

                    FieldLabel("Current password")
                    PremierField(
                        title = "Current password", icon = Icons.Filled.Lock,
                        value = currentPassword, onValueChange = { currentPassword = it }, isSecure = true,
                    )
                    FieldLabel("New password")
                    PremierField(
                        title = "New password (min 6 characters)", icon = Icons.Filled.Lock,
                        value = newPassword, onValueChange = { newPassword = it }, isSecure = true,
                    )
                    FieldLabel("Confirm new password")
                    PremierField(
                        title = "Confirm new password", icon = Icons.Filled.Lock,
                        value = confirmPassword, onValueChange = { confirmPassword = it }, isSecure = true,
                        imeAction = ImeAction.Done,
                    )

                    PremierButton(
                        title = "Update password", icon = Icons.Filled.Save,
                        loading = submitting,
                        enabled = currentPassword.isNotEmpty() && newPassword.length >= 6 && newPassword == confirmPassword,
                    ) {
                        if (newPassword != confirmPassword) { error = "New passwords don't match."; return@PremierButton }
                        error = null; submitting = true
                        scope.launch {
                            try {
                                HrmsApi.changePassword(currentPassword, newPassword)
                                submitting = false
                                done = true
                            } catch (ex: Exception) {
                                submitting = false
                                error = (ex as? ApiException)?.message ?: "Could not change your password."
                            }
                        }
                    }
                }
            }
        }
    }

    if (done) {
        AlertDialog(
            onDismissRequest = onSaved,
            confirmButton = { TextButton(onClick = onSaved) { Text("Done") } },
            title = { Text("Password updated") },
            text = { Text("Your password has been changed.") },
        )
    }
}
