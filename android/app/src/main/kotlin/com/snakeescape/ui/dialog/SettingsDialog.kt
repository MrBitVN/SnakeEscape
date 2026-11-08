package com.snakeescape.ui.dialog

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.snakeescape.ui.theme.GameColorScheme
import com.snakeescape.ui.theme.LocalAppStrings

@Composable
fun SettingsDialog(
    soundEnabled: Boolean,
    onSoundToggle: (Boolean) -> Unit,
    musicEnabled: Boolean,
    onMusicToggle: (Boolean) -> Unit,
    hapticEnabled: Boolean,
    onHapticToggle: (Boolean) -> Unit,
    themeMode: String,
    onThemeSelect: (String) -> Unit,
    language: String,
    onLanguageSelect: (String) -> Unit,
    colors: GameColorScheme,
    onDismiss: () -> Unit
) {
    val strings = LocalAppStrings.current

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = colors.surface,
            tonalElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = strings.settings,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )

                Spacer(modifier = Modifier.height(20.dp))

                // 1. Sound FX Switch
                SettingSwitchRow(
                    label = strings.soundEffects,
                    icon = "🔊",
                    checked = soundEnabled,
                    onCheckedChange = onSoundToggle,
                    colors = colors
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 2. Music Switch
                SettingSwitchRow(
                    label = strings.music,
                    icon = "🎵",
                    checked = musicEnabled,
                    onCheckedChange = onMusicToggle,
                    colors = colors
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 3. Haptics Switch
                SettingSwitchRow(
                    label = strings.haptics,
                    icon = "📳",
                    checked = hapticEnabled,
                    onCheckedChange = onHapticToggle,
                    colors = colors
                )

                Spacer(modifier = Modifier.height(18.dp))
                HorizontalDivider(color = colors.gridLine.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(14.dp))

                // 4. Theme Selector (System / Dark / Light)
                Text(
                    text = strings.theme,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textSecondary,
                    modifier = Modifier.align(Alignment.Start)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SegmentButton(
                        text = strings.themeSystem,
                        selected = themeMode.equals("SYSTEM", ignoreCase = true),
                        onClick = { onThemeSelect("SYSTEM") },
                        colors = colors,
                        modifier = Modifier.weight(1f)
                    )
                    SegmentButton(
                        text = strings.themeDark,
                        selected = themeMode.equals("DARK", ignoreCase = true),
                        onClick = { onThemeSelect("DARK") },
                        colors = colors,
                        modifier = Modifier.weight(1f)
                    )
                    SegmentButton(
                        text = strings.themeLight,
                        selected = themeMode.equals("LIGHT", ignoreCase = true),
                        onClick = { onThemeSelect("LIGHT") },
                        colors = colors,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))
                HorizontalDivider(color = colors.gridLine.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(14.dp))

                // 5. Language Selector (EN / VI)
                Text(
                    text = strings.language,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textSecondary,
                    modifier = Modifier.align(Alignment.Start)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SegmentButton(
                        text = strings.langVietnamese,
                        selected = language.equals("VI", ignoreCase = true),
                        onClick = { onLanguageSelect("VI") },
                        colors = colors,
                        modifier = Modifier.weight(1f)
                    )
                    SegmentButton(
                        text = strings.langEnglish,
                        selected = language.equals("EN", ignoreCase = true),
                        onClick = { onLanguageSelect("EN") },
                        colors = colors,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Close Button
                Button(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF10B981)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Text(
                        text = strings.close,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingSwitchRow(
    label: String,
    icon: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    colors: GameColorScheme
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(colors.surfaceVariant.copy(alpha = 0.5f))
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(text = icon, fontSize = 20.sp)
            Text(
                text = label,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = colors.textPrimary
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Color(0xFF10B981),
                uncheckedThumbColor = colors.textSecondary,
                uncheckedTrackColor = colors.gridLine
            )
        )
    }
}

@Composable
private fun SegmentButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    colors: GameColorScheme,
    modifier: Modifier = Modifier
) {
    val bg = if (selected) Color(0xFF10B981) else colors.surfaceVariant
    val textColor = if (selected) Color.White else colors.textSecondary

    Box(
        modifier = modifier
            .height(38.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(bg)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = textColor
        )
    }
}
