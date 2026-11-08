package com.snakeescape.ui.dialog

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.snakeescape.ui.theme.GameColorScheme
import com.snakeescape.ui.theme.LocalAppStrings
import com.snakeescape.ui.theme.SnakeColors

@Composable
fun ShopDialog(
    coins: Int,
    onBuyBooster: (type: String) -> Unit,
    onClaimFreeCoins: () -> Unit,
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
                .fillMaxWidth(0.92f)
                .wrapContentHeight(),
            shape = RoundedCornerShape(26.dp),
            color = colors.surface,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🛒 ${strings.shop.uppercase()}",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = colors.textPrimary
                    )

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = strings.close, tint = colors.textSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Coin Balance Badge
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = colors.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = strings.coins,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.textSecondary
                        )

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "🪙", fontSize = 20.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "$coins",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Black,
                                color = SnakeColors.GoldCoin
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Free Coins Banner
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0x2222C55E),
                    border = androidx.compose.foundation.BorderStroke(1.dp, SnakeColors.ButtonGreen.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "🎁 ${strings.freeCoins}",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = SnakeColors.ButtonGreen
                            )
                            Text(
                                text = "Watch ad to get +50 Coins",
                                fontSize = 12.sp,
                                color = colors.textSecondary
                            )
                        }

                        Button(
                            onClick = onClaimFreeCoins,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = SnakeColors.ButtonGreen)
                        ) {
                            Text("+50 🪙", fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "BOOSTER PACKS",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textSecondary,
                    modifier = Modifier.align(Alignment.Start)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Booster Items
                ShopBoosterCard(
                    icon = Icons.Default.Star,
                    name = "3x ${strings.boosterHint}",
                    desc = "Highlights the winning snake",
                    cost = 50,
                    canAfford = coins >= 50,
                    onBuy = { onBuyBooster("HINT") },
                    colors = colors
                )

                Spacer(modifier = Modifier.height(8.dp))

                ShopBoosterCard(
                    icon = Icons.Default.Info,
                    name = "3x ${strings.boosterGridVision}",
                    desc = "Reveals all snake escape lines",
                    cost = 50,
                    canAfford = coins >= 50,
                    onBuy = { onBuyBooster("GRID_VISION") },
                    colors = colors
                )
            }
        }
    }
}

@Composable
private fun ShopBoosterCard(
    icon: ImageVector,
    name: String,
    desc: String,
    cost: Int,
    canAfford: Boolean,
    onBuy: () -> Unit,
    colors: GameColorScheme
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = colors.surfaceVariant.copy(alpha = 0.6f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF38BDF8).copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(22.dp))
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(text = name, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                Text(text = desc, fontSize = 11.5.sp, color = colors.textSecondary)
            }

            Button(
                onClick = onBuy,
                enabled = canAfford,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SnakeColors.GoldCoin,
                    disabledContainerColor = colors.surfaceVariant
                ),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "$cost 🪙",
                    color = if (canAfford) Color.Black else colors.textSecondary.copy(alpha = 0.5f),
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }
        }
    }
}
