package com.snakeescape.ui.dialog

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.snakeescape.logic.RunSummary
import com.snakeescape.ui.theme.GameColorScheme
import com.snakeescape.ui.theme.LocalAppStrings
import com.snakeescape.ui.theme.SnakeColors

@Composable
fun RunSummaryDialog(
    summary: RunSummary,
    currentRank: Int,
    colors: GameColorScheme,
    onStartNewRun: () -> Unit,
    onBackToHome: () -> Unit
) {
    val strings = LocalAppStrings.current

    val totalSec = (summary.totalTimeMs / 1000).toInt()
    val mins = totalSec / 60
    val secs = totalSec % 60
    val formattedTime = String.format("%02d:%02d", mins, secs)

    Dialog(
        onDismissRequest = { /* Cannot dismiss without choosing action */ },
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = colors.surface,
            tonalElevation = 10.dp,
            modifier = Modifier.fillMaxWidth(0.94f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Trophy / Icon Header
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFFFFD54F), Color(0xFFFF9800))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "🏆", fontSize = 34.sp)
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = strings.runOver.uppercase(),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    color = colors.textPrimary,
                    letterSpacing = 1.sp
                )

                Text(
                    text = "${summary.playerName}",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF38BDF8)
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Stats Card Grid
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(colors.surfaceVariant.copy(alpha = 0.6f))
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SummaryRow(label = strings.levelsClearedLabel, value = "${summary.levelsCleared}", colors = colors)
                    Divider(color = colors.gridLine.copy(alpha = 0.5f), thickness = 0.8.dp)
                    SummaryRow(
                        label = strings.totalScoreLabel,
                        value = String.format("%,d", summary.totalScore),
                        valueColor = Color(0xFFFFB300),
                        isBold = true,
                        colors = colors
                    )
                    Divider(color = colors.gridLine.copy(alpha = 0.5f), thickness = 0.8.dp)
                    SummaryRow(label = strings.totalTimeLabel, value = formattedTime, colors = colors)
                    Divider(color = colors.gridLine.copy(alpha = 0.5f), thickness = 0.8.dp)
                    SummaryRow(
                        label = strings.currentRankLabel,
                        value = if (currentRank > 0) "#$currentRank" else "--",
                        valueColor = Color(0xFF00E676),
                        isBold = true,
                        colors = colors
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Actions: Start New Run (Lv 1, Score 0)
                Button(
                    onClick = onStartNewRun,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SnakeColors.ButtonGreen),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = strings.playAgain,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedButton(
                    onClick = onBackToHome,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(16.dp),
                    border = ButtonDefaults.outlinedButtonBorder.copy(brush = Brush.horizontalGradient(listOf(colors.gridLine, colors.gridLine)))
                ) {
                    Icon(Icons.Default.Home, contentDescription = null, tint = colors.textPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = strings.home,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    )
                }
            }
        }
    }
}

@Composable
private fun SummaryRow(
    label: String,
    value: String,
    colors: GameColorScheme,
    valueColor: Color? = null,
    isBold: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            color = colors.textSecondary,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = value,
            fontSize = 16.sp,
            fontWeight = if (isBold) FontWeight.Black else FontWeight.Bold,
            color = valueColor ?: colors.textPrimary
        )
    }
}
