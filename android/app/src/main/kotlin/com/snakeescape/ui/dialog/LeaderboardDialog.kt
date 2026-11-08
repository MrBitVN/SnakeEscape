package com.snakeescape.ui.dialog

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.snakeescape.data.LeaderboardEntry
import com.snakeescape.ui.theme.GameColorScheme
import com.snakeescape.ui.theme.LocalAppStrings
import com.snakeescape.ui.theme.SnakeColors

@Composable
fun LeaderboardDialog(
    leaderboardEntries: List<LeaderboardEntry>,
    currentUsername: String,
    colors: GameColorScheme,
    onDismiss: () -> Unit
) {
    val strings = LocalAppStrings.current

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.88f),
            shape = RoundedCornerShape(26.dp),
            color = colors.surface,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(text = "🏆", fontSize = 24.sp)
                        Text(
                            text = strings.leaderboard.uppercase(),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = colors.textPrimary
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = strings.close, tint = colors.textSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Real-time Top 50 Global High Scores",
                    fontSize = 12.sp,
                    color = colors.textSecondary
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Table Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(colors.surfaceVariant.copy(alpha = 0.5f))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = strings.rank,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textSecondary,
                        modifier = Modifier.width(44.dp)
                    )
                    Text(
                        text = strings.player,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textSecondary,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "Lv",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textSecondary,
                        modifier = Modifier.width(36.dp)
                    )
                    Text(
                        text = strings.scoreLabel,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textSecondary,
                        modifier = Modifier.width(70.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Table Content (LazyColumn Top 50)
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(leaderboardEntries) { entry ->
                        LeaderboardRowItem(entry = entry, colors = colors)
                    }
                }
            }
        }
    }
}

@Composable
private fun LeaderboardRowItem(
    entry: LeaderboardEntry,
    colors: GameColorScheme
) {
    val isTop1 = entry.rank == 1
    val isTop2 = entry.rank == 2
    val isTop3 = entry.rank == 3

    val rankBadgeColor = when {
        isTop1 -> Color(0xFFFFD700)
        isTop2 -> Color(0xFFC0C0C0)
        isTop3 -> Color(0xFFCD7F32)
        else -> colors.surfaceVariant
    }

    val rankTextColor = when {
        isTop1 || isTop2 || isTop3 -> Color.Black
        else -> colors.textSecondary
    }

    val rowBg = if (entry.isCurrentUser) {
        Color(0xFF38BDF8).copy(alpha = 0.16f)
    } else {
        colors.surfaceVariant.copy(alpha = 0.25f)
    }

    val borderModifier = if (entry.isCurrentUser) {
        Modifier.border(1.5.dp, Color(0xFF38BDF8), RoundedCornerShape(12.dp))
    } else {
        Modifier
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(borderModifier)
            .clip(RoundedCornerShape(12.dp))
            .background(rowBg)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Rank Badge
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(rankBadgeColor),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "${entry.rank}",
                fontSize = 12.sp,
                fontWeight = FontWeight.Black,
                color = rankTextColor
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Player Name & "YOU" tag
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = entry.playerName,
                fontSize = 14.sp,
                fontWeight = if (entry.isCurrentUser) FontWeight.Black else FontWeight.SemiBold,
                color = if (entry.isCurrentUser) Color(0xFF38BDF8) else colors.textPrimary
            )
            if (entry.isCurrentUser) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF38BDF8))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(text = "YOU", fontSize = 9.sp, fontWeight = FontWeight.Black, color = Color.White)
                }
            }
        }

        // Levels Cleared
        Text(
            text = "${entry.levelsCleared}",
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = colors.textSecondary,
            modifier = Modifier.width(36.dp)
        )

        // Best Score
        Text(
            text = entry.formattedScore,
            fontSize = 14.sp,
            fontWeight = FontWeight.ExtraBold,
            color = if (entry.isCurrentUser) Color(0xFFFFB300) else colors.textPrimary,
            modifier = Modifier.width(70.dp)
        )
    }
}
