package com.snakeescape.ui.dialog

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.snakeescape.logic.GameMode
import com.snakeescape.ui.theme.GameColorScheme
import com.snakeescape.ui.theme.LocalAppStrings
import com.snakeescape.ui.theme.SnakeColors

@Composable
fun LevelSelectDialog(
    currentMode: GameMode,
    currentLevel: Int,
    highestUnlockedLevel: Int,
    highestUnlockedChallengeLevel: Int,
    onLevelSelected: (level: Int, mode: GameMode) -> Unit,
    colors: GameColorScheme,
    onDismiss: () -> Unit
) {
    val strings = LocalAppStrings.current
    var selectedMode by remember { mutableStateOf(currentMode) }

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
                    .padding(horizontal = 18.dp, vertical = 16.dp)
            ) {
                // Header: Title & Close Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = strings.selectLevel.uppercase(),
                        color = colors.textPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp
                    )

                    IconButton(onClick = onDismiss) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = strings.close,
                            tint = colors.textSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Mode Selector Segmented Tabs
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(colors.surfaceVariant.copy(alpha = 0.7f))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Campaign Tab
                    val isCampaignSelected = selectedMode == GameMode.CAMPAIGN
                    val campaignBg by animateColorAsState(
                        targetValue = if (isCampaignSelected) Color(0xFF10B981) else Color.Transparent,
                        label = "campaignTabBg"
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(11.dp))
                            .background(campaignBg)
                            .clickable { selectedMode = GameMode.CAMPAIGN }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "🛡️ ${strings.campaignMode}",
                            color = if (isCampaignSelected) Color.White else colors.textSecondary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }

                    // Challenge Tab
                    val isChallengeSelected = selectedMode == GameMode.CHALLENGE
                    val challengeBg by animateColorAsState(
                        targetValue = if (isChallengeSelected) SnakeColors.ChallengeFlame else Color.Transparent,
                        label = "challengeTabBg"
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(11.dp))
                            .background(challengeBg)
                            .clickable { selectedMode = GameMode.CHALLENGE }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "⚡ ${strings.challengeMode}",
                            color = if (isChallengeSelected) Color.White else colors.textSecondary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Mode Subtitle / Progress banner
                val totalLevels = if (selectedMode == GameMode.CAMPAIGN) 200 else 50
                val highestUnlocked = if (selectedMode == GameMode.CAMPAIGN) highestUnlockedLevel else highestUnlockedChallengeLevel

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (selectedMode == GameMode.CHALLENGE) {
                        Color(0x22FF2A85)
                    } else {
                        colors.surfaceVariant.copy(alpha = 0.5f)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (selectedMode == GameMode.CAMPAIGN) {
                                strings.levelDesc
                            } else {
                                strings.challengeDesc
                            },
                            color = if (selectedMode == GameMode.CHALLENGE) SnakeColors.ChallengeFlame else colors.textSecondary,
                            fontSize = 11.5.sp,
                            maxLines = 1,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "$highestUnlocked / $totalLevels",
                            color = if (selectedMode == GameMode.CHALLENGE) SnakeColors.ChallengeGold else Color(0xFF10B981),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Grid of levels
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 60.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(totalLevels, key = { it + 1 }) { index ->
                        val level = index + 1
                        val isUnlocked = (level <= highestUnlocked)
                        val isCurrent = (selectedMode == currentMode && level == currentLevel)

                        val cellBg = when {
                            isCurrent && selectedMode == GameMode.CHALLENGE -> SnakeColors.ChallengeFlame
                            isCurrent -> Color(0xFF10B981)
                            isUnlocked && selectedMode == GameMode.CHALLENGE -> Color(0x33FF2A85)
                            isUnlocked -> colors.surfaceVariant
                            else -> if (colors.isDark) Color(0xFF0C1322) else Color(0xFFE2E8F0)
                        }

                        val borderColor = when {
                            isCurrent -> Color.White
                            isUnlocked && selectedMode == GameMode.CHALLENGE -> SnakeColors.ChallengeGold.copy(alpha = 0.5f)
                            isUnlocked -> colors.gridLine.copy(alpha = 0.6f)
                            else -> colors.gridLine.copy(alpha = 0.2f)
                        }

                        Box(
                            modifier = Modifier
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(14.dp))
                                .background(cellBg)
                                .border(
                                    width = if (isCurrent) 2.dp else 1.dp,
                                    color = borderColor,
                                    shape = RoundedCornerShape(14.dp)
                                )
                                .clickable(enabled = isUnlocked) {
                                    onLevelSelected(level, selectedMode)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isUnlocked) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = "$level",
                                        color = if (isCurrent) Color.White else colors.textPrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    )
                                    if (level < highestUnlocked) {
                                        Icon(
                                            Icons.Default.Star,
                                            contentDescription = "Completed",
                                            tint = if (selectedMode == GameMode.CHALLENGE) SnakeColors.ChallengeGold else Color(0xFFFFD700),
                                            modifier = Modifier.size(13.dp)
                                        )
                                    } else if (selectedMode == GameMode.CHALLENGE) {
                                        Text(
                                            text = "⚡",
                                            fontSize = 9.sp
                                        )
                                    }
                                }
                            } else {
                                Icon(
                                    Icons.Default.Lock,
                                    contentDescription = strings.locked,
                                    tint = colors.textSecondary.copy(alpha = 0.4f),
                                    modifier = Modifier.size(17.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
