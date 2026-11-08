package com.snakeescape.ui.screen

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.snakeescape.logic.GameMode
import com.snakeescape.ui.theme.GameColorScheme
import com.snakeescape.ui.theme.LocalAppStrings
import com.snakeescape.ui.theme.SnakeColors

@Composable
fun HomeScreen(
    currentLevel: Int,
    highestUnlockedLevel: Int,
    coins: Int,
    gameMode: GameMode,
    colors: GameColorScheme,
    onPlayClick: (Int) -> Unit,
    onOpenLeaderboard: () -> Unit,
    onOpenShop: () -> Unit,
    onOpenSettings: () -> Unit,
    onClaimFreeCoins: () -> Unit,
    onToggleGameMode: () -> Unit
) {
    val strings = LocalAppStrings.current
    val listState = rememberLazyListState()

    // Royal purple gradient background (matching Image 1)
    val backgroundBrush = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF581C87), // Deep Royal Purple
            Color(0xFF6B21A8),
            Color(0xFF4C1D95)
        )
    )

    // Pulsing animation for the active level node
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundBrush)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // TOP BAR (matching Image 1)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Coin Capsule on Top-Left: [ 🪙  150  [+] ]
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = Color(0xFF1E1035).copy(alpha = 0.85f),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF7E22CE)),
                    modifier = Modifier
                        .clickable { onOpenShop() }
                        .padding(2.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "🪙", fontSize = 18.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "$coins",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(SnakeColors.ButtonGreen),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = "+", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Mode switch badge (Campaign / Challenge)
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (gameMode == GameMode.CHALLENGE) SnakeColors.ChallengeFlame else Color(0xFF3B82F6),
                    modifier = Modifier.clickable { onToggleGameMode() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (gameMode == GameMode.CHALLENGE) "⚡ CHALLENGE" else "🛡️ CAMPAIGN",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Settings Button on Top-Right (Blue square with gear)
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFF0284C7),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF38BDF8)),
                    modifier = Modifier
                        .size(44.dp)
                        .clickable { onOpenSettings() }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = strings.settings,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            // CENTER LEVEL MAP (Vertical Line with Nodes & Floating Badges - matching Image 1)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                // Vertical connecting center line
                Box(
                    modifier = Modifier
                        .width(14.dp)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(7.dp))
                        .background(Color(0xFF3B1D70))
                )

                // Nodes column (showing visible levels around currentLevel)
                val visibleLevels = (1..6).reversed().toList()
                Column(
                    modifier = Modifier.fillMaxHeight(),
                    verticalArrangement = Arrangement.SpaceEvenly,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    visibleLevels.forEach { lvl ->
                        val isCurrent = (lvl == currentLevel)
                        val isUnlocked = (lvl <= highestUnlockedLevel)

                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // Floating badges alongside specific nodes (Image 1)
                            if (lvl == 2) {
                                // "No Ad" Badge on Left
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 24.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    FloatingRibbonBadge(
                                        title = strings.noAds,
                                        icon = "🚫",
                                        accentColor = Color(0xFFEF4444),
                                        onClick = {}
                                    )
                                    // "Free coins" Badge on Right
                                    FloatingRibbonBadge(
                                        title = strings.freeCoins,
                                        icon = "🎁",
                                        accentColor = SnakeColors.GoldCoin,
                                        onClick = onClaimFreeCoins
                                    )
                                }
                            }

                            // The Level Node Button (matching Image 1)
                            Surface(
                                shape = CircleShape,
                                color = when {
                                    isCurrent -> Color(0xFFE2E8F0)
                                    isUnlocked -> Color(0xFFCBD5E1)
                                    else -> Color(0xFF64748B)
                                },
                                border = androidx.compose.foundation.BorderStroke(
                                    width = if (isCurrent) 4.dp else 2.5.dp,
                                    color = if (isCurrent) Color.White else Color(0xFF475569)
                                ),
                                shadowElevation = if (isCurrent) 12.dp else 4.dp,
                                modifier = Modifier
                                    .size(if (isCurrent) 68.dp else 60.dp)
                                    .scale(if (isCurrent) pulseScale else 1f)
                                    .clickable(enabled = isUnlocked) { onPlayClick(lvl) }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    if (isUnlocked) {
                                        Text(
                                            text = "$lvl",
                                            fontSize = if (isCurrent) 24.sp else 20.sp,
                                            fontWeight = FontWeight.Black,
                                            color = Color(0xFF1E293B)
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.Lock,
                                            contentDescription = "Locked",
                                            tint = Color(0xFFCBD5E1),
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // BOTTOM BIG JUICY GREEN BUTTON (matching Image 1: "Level 1" button)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp, vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = Color.Transparent,
                    shadowElevation = 10.dp,
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .height(64.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFF4ADE80), // Bright Lime Gloss
                                    Color(0xFF22C55E),
                                    Color(0xFF16A34A)  // Deep Emerald
                                )
                            )
                        )
                        .border(
                            width = 2.5.dp,
                            color = Color(0xFF86EFAC),
                            shape = RoundedCornerShape(22.dp)
                        )
                        .clickable { onPlayClick(currentLevel) }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "${strings.levelPrefix} $currentLevel",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            letterSpacing = 1.sp
                        )
                    }
                }
            }

            // BOTTOM NAVIGATION BAR with 3 Tabs (matching Image 1)
            Surface(
                color = Color(0xFF3B1569),
                tonalElevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left Tab: Leaderboard
                    BottomNavTabItem(
                        icon = Icons.Default.Person,
                        label = strings.leaderboard,
                        isSelected = false,
                        onClick = onOpenLeaderboard
                    )

                    // Center Tab: Home (Active)
                    BottomNavTabItem(
                        icon = Icons.Default.Home,
                        label = strings.home,
                        isSelected = true,
                        onClick = {}
                    )

                    // Right Tab: Shop
                    BottomNavTabItem(
                        icon = Icons.Default.ShoppingCart,
                        label = strings.shop,
                        isSelected = false,
                        onClick = onOpenShop
                    )
                }
            }
        }
    }
}

@Composable
private fun FloatingRibbonBadge(
    title: String,
    icon: String,
    accentColor: Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable { onClick() }
            .padding(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(Color(0xFF2D1252))
                .border(1.5.dp, accentColor, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(text = icon, fontSize = 22.sp)
        }
        Spacer(modifier = Modifier.height(3.dp))
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(SnakeColors.ButtonGreen)
                .padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
            Text(
                text = title,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}

@Composable
private fun BottomNavTabItem(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(if (isSelected) Color(0xFF6B21A8) else Color.Transparent),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isSelected) Color.White else Color(0xFFDDD6FE).copy(alpha = 0.6f),
                modifier = Modifier.size(24.dp)
            )
        }
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) Color.White else Color(0xFFDDD6FE).copy(alpha = 0.6f)
        )
    }
}
