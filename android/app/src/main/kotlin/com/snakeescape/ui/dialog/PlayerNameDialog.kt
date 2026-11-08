package com.snakeescape.ui.dialog

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.snakeescape.ui.theme.GameColorScheme
import com.snakeescape.ui.theme.LocalAppStrings
import com.snakeescape.ui.theme.SnakeColors

@Composable
fun PlayerNameDialog(
    initialName: String,
    isMandatory: Boolean = false,
    errorMessage: String? = null,
    isLoading: Boolean = false,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
    colors: GameColorScheme
) {
    val strings = LocalAppStrings.current
    var inputName by remember { mutableStateOf(initialName) }

    val trimmed = inputName.trim()
    val isValidFormat = trimmed.length in 3..16 && Regex("^[a-zA-Z0-9_]{3,16}$").matches(trimmed)
    val isTooShort = trimmed.isNotEmpty() && trimmed.length < 3
    val hasInvalidChar = trimmed.isNotEmpty() && !Regex("^[a-zA-Z0-9_]*$").matches(trimmed)

    Dialog(
        onDismissRequest = {
            if (!isMandatory && !isLoading) onDismiss()
        },
        properties = DialogProperties(
            dismissOnBackPress = !isMandatory && !isLoading,
            dismissOnClickOutside = !isMandatory && !isLoading
        )
    ) {
        Surface(
            shape = RoundedCornerShape(26.dp),
            color = colors.surface,
            tonalElevation = 10.dp,
            modifier = Modifier.fillMaxWidth(0.92f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = strings.enterNameTitle,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = colors.textPrimary
                    )
                    if (!isMandatory) {
                        IconButton(onClick = onDismiss, enabled = !isLoading) {
                            Icon(Icons.Default.Close, contentDescription = strings.close, tint = colors.textSecondary)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = strings.enterNameDesc,
                    fontSize = 13.sp,
                    color = colors.textSecondary,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(18.dp))

                OutlinedTextField(
                    value = inputName,
                    onValueChange = {
                        if (it.length <= 16) inputName = it
                    },
                    singleLine = true,
                    enabled = !isLoading,
                    placeholder = { Text(strings.usernameHint, color = colors.textSecondary.copy(alpha = 0.6f)) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = colors.textPrimary,
                        unfocusedTextColor = colors.textPrimary,
                        focusedBorderColor = if (isValidFormat) SnakeColors.ButtonGreen else Color(0xFF38BDF8),
                        unfocusedBorderColor = colors.gridLine
                    )
                )

                // Error / Validation status message
                Spacer(modifier = Modifier.height(10.dp))
                if (errorMessage != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = SnakeColors.BlockedRed, modifier = Modifier.size(16.dp))
                        Text(text = errorMessage, color = SnakeColors.BlockedRed, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                } else if (isTooShort) {
                    Text(text = strings.usernameTooShort, color = SnakeColors.BlockedRed, fontSize = 12.sp, modifier = Modifier.fillMaxWidth())
                } else if (hasInvalidChar) {
                    Text(text = "Letters, numbers, and underscore only", color = SnakeColors.BlockedRed, fontSize = 12.sp, modifier = Modifier.fillMaxWidth())
                } else if (isValidFormat) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SnakeColors.ButtonGreen, modifier = Modifier.size(16.dp))
                        Text(text = "Valid username format (ready to reserve)", color = SnakeColors.ButtonGreen, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = {
                        if (isValidFormat && !isLoading) {
                            onConfirm(trimmed)
                        }
                    },
                    enabled = isValidFormat && !isLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SnakeColors.ButtonGreen,
                        disabledContainerColor = colors.surfaceVariant
                    ),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp), strokeWidth = 2.5.dp)
                    } else {
                        Text(
                            text = strings.confirm,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}
