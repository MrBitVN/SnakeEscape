package com.snakeescape.ui.screen

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.snakeescape.audio.HapticManager
import com.snakeescape.audio.SoundManager
import com.snakeescape.logic.*
import com.snakeescape.ui.*
import com.snakeescape.ui.component.EscapingSnakeAnim
import com.snakeescape.ui.component.SnakeBoardCanvas
import com.snakeescape.ui.dialog.*
import com.snakeescape.ui.theme.*
import kotlinx.coroutines.launch
import kotlin.math.max

@Composable
fun SnakeEscapeGameScreen(
    viewModel: GameViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val leaderboardEntries by viewModel.leaderboardFlow.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    // Audio & Haptics Managers
    val soundManager = remember { SoundManager(context) }
    val hapticManager = remember { HapticManager(context) }

    LaunchedEffect(uiState.soundEnabled) { soundManager.isSoundEnabled = uiState.soundEnabled }
    LaunchedEffect(uiState.musicEnabled) { soundManager.isMusicEnabled = uiState.musicEnabled }
    LaunchedEffect(uiState.hapticEnabled) { hapticManager.isHapticEnabled = uiState.hapticEnabled }

    LaunchedEffect(uiState.gameStatus, uiState.screenState) {
        if (uiState.screenState == ScreenState.GAMEPLAY && uiState.gameStatus == GameStatus.PLAYING) {
            soundManager.resumeMusic()
        } else {
            soundManager.pauseMusic()
        }
    }

    LaunchedEffect(uiState.isWon) {
        if (uiState.isWon) {
            soundManager.playWin()
            hapticManager.win()
        }
    }

    DisposableEffect(Unit) {
        onDispose { soundManager.release() }
    }

    val strings = LocalAppStrings.current
    val colors = SnakeColors.LavenderScheme

    var escapingSnakeAnim by remember { mutableStateOf<EscapingSnakeAnim?>(null) }
    val shakeProgress = remember { Animatable(0f) }
    val blockedAlpha = remember { Animatable(0f) }

    // Xử lý khi tap vào con rắn
    fun handleSnakeTap(snakeId: Int) {
        if (escapingSnakeAnim != null) return

        soundManager.playTap()
        hapticManager.tap()

        viewModel.onSnakeTapped(
            snakeId = snakeId,
            onEscaped = { snake ->
                soundManager.playEscape()
                hapticManager.escape()

                val exitDistance = max(uiState.gameState.width, uiState.gameState.height) + snake.length + 2
                val polyline = mutableListOf<Offset>()
                for (cell in snake.body.reversed()) {
                    polyline.add(Offset(cell.x.toFloat(), cell.y.toFloat()))
                }
                var forward = snake.head + snake.direction
                for (step in 1..exitDistance) {
                    polyline.add(Offset(forward.x.toFloat(), forward.y.toFloat()))
                    forward = forward + snake.direction
                }

                val animatable = Animatable(0f)
                val escapeAnim = EscapingSnakeAnim(
                    snake = snake,
                    polyline = polyline,
                    animatable = animatable
                )

                escapingSnakeAnim = escapeAnim

                coroutineScope.launch {
                    animatable.animateTo(
                        targetValue = exitDistance.toFloat(),
                        animationSpec = tween(
                            durationMillis = 280 + snake.length * 45,
                            easing = LinearEasing
                        )
                    )
                    escapingSnakeAnim = null
                }
            },
            onBlocked = { _, _ ->
                soundManager.playBlocked()
                hapticManager.blocked()

                coroutineScope.launch {
                    shakeProgress.snapTo(0f)
                    shakeProgress.animateTo(1f, animationSpec = tween(350))
                    viewModel.clearShakeAndBlocked()
                }

                coroutineScope.launch {
                    blockedAlpha.snapTo(1f)
                    blockedAlpha.animateTo(0f, animationSpec = tween(500))
                }
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (uiState.screenState == ScreenState.HOME) {
            HomeScreen(
                currentLevel = uiState.currentLevel,
                highestUnlockedLevel = uiState.highestUnlockedLevel,
                coins = uiState.coins,
                gameMode = uiState.gameMode,
                colors = colors,
                onPlayClick = {
                    soundManager.playTap()
                    viewModel.openGame()
                },
                onOpenLeaderboard = {
                    soundManager.playTap()
                    viewModel.openLeaderboard()
                },
                onOpenShop = {
                    soundManager.playTap()
                    viewModel.openShop()
                },
                onOpenSettings = {
                    soundManager.playTap()
                    viewModel.openSettings()
                },
                onClaimFreeCoins = {
                    viewModel.claimFreeCoins()
                },
                onToggleGameMode = {
                }
            )
        } else {
            // Màn chơi chính GAMEPLAY
            Scaffold(
                containerColor = colors.background,
                topBar = {
                    TopGameBarImage2(
                        level = uiState.currentLevel,
                        hearts = uiState.levelHearts,
                        maxHearts = ScoringConfig.INITIAL_LEVEL_HEARTS,
                        runScore = uiState.runScore,
                        timeSecondsProvider = { uiState.timeRemainingSeconds },
                        escapedCount = uiState.escapedSnakeCount,
                        totalCount = uiState.initialSnakeCount,
                        canUndo = uiState.gameState.history.isNotEmpty(),
                        colors = colors,
                        onPauseClick = { viewModel.pauseGame() },
                        onUndoClick = {
                            soundManager.playTap()
                            hapticManager.tap()
                            viewModel.undo()
                        }
                    )
                },
                bottomBar = {
                    // Thanh nút phía dưới CHỈ CÒN Hint và Grid Vision, căn giữa cân đối
                    BottomBoosterBar(
                        hintCount = uiState.hintBoosterCount,
                        gridVisionCount = uiState.gridVisionBoosterCount,
                        isGridVisionActive = uiState.isGridVisionActive,
                        colors = colors,
                        onHintClick = {
                            soundManager.playTap()
                            hapticManager.tap()
                            viewModel.useHint()
                        },
                        onGridVisionClick = {
                            soundManager.playTap()
                            hapticManager.tap()
                            viewModel.useGridVision()
                        }
                    )
                }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    // Canvas Game Board
                    SnakeBoardCanvas(
                        state = uiState.gameState,
                        onSnakeTapped = { snakeId -> handleSnakeTap(snakeId) },
                        colors = colors,
                        gameMode = uiState.gameMode,
                        shakingSnakeId = uiState.shakingSnakeId,
                        shakeProgressProvider = { shakeProgress.value },
                        blockedCell = uiState.blockedCell,
                        blockedAlphaProvider = { blockedAlpha.value },
                        hintSnakeId = uiState.hintSnakeId,
                        isGridVisionActive = uiState.isGridVisionActive,
                        escapingSnake = escapingSnakeAnim
                    )
                }
            }
        }

        // ================= DIALOGS =================
        // 1. Player Name Dialog (Check username & Atomic Reservation)
        if (uiState.isNameDialogOpen) {
            PlayerNameDialog(
                initialName = uiState.playerName,
                isMandatory = uiState.isNameDialogMandatory,
                errorMessage = uiState.nameDialogError,
                isLoading = uiState.isNameRegistering,
                onConfirm = { name -> viewModel.confirmPlayerName(name) },
                onDismiss = { viewModel.closeNameDialog() },
                colors = colors
            )
        }

        // 2. Leaderboard Dialog (Real-time snapshot top 50)
        if (uiState.isLeaderboardOpen) {
            LeaderboardDialog(
                leaderboardEntries = leaderboardEntries,
                currentUsername = uiState.playerName,
                colors = colors,
                onDismiss = { viewModel.closeLeaderboard() }
            )
        }

        // 3. Shop Dialog (Buy Boosters, Free Coins - No +30s)
        if (uiState.isShopOpen) {
            ShopDialog(
                coins = uiState.coins,
                onBuyBooster = { type -> viewModel.buyBooster(type) },
                onClaimFreeCoins = { viewModel.claimFreeCoins() },
                colors = colors,
                onDismiss = { viewModel.closeShop() }
            )
        }

        // 4. Settings Dialog
        if (uiState.isSettingsOpen) {
            SettingsDialog(
                soundEnabled = uiState.soundEnabled,
                onSoundToggle = { viewModel.setSoundEnabled(it) },
                musicEnabled = uiState.musicEnabled,
                onMusicToggle = { viewModel.setMusicEnabled(it) },
                hapticEnabled = uiState.hapticEnabled,
                onHapticToggle = { viewModel.setHapticEnabled(it) },
                themeMode = uiState.themeMode,
                onThemeSelect = { viewModel.setThemeMode(it) },
                language = uiState.language,
                onLanguageSelect = { },
                colors = colors,
                onDismiss = { viewModel.closeSettings() }
            )
        }

        // 5. Pause Dialog (Resume, Settings, Abandon Run - No Restart)
        if (uiState.gameStatus == GameStatus.PAUSED && !uiState.isSettingsOpen && !uiState.isLeaderboardOpen && !uiState.isShopOpen) {
            PauseDialog(
                colors = colors,
                onResume = { viewModel.resumeGame() },
                onSettings = { viewModel.openSettings() },
                onAbandonRun = { viewModel.abandonCurrentRun() }
            )
        }

        // 6. Win Dialog (Displays Cleared, Score Breakdown, Next Level - No Restart)
        if (uiState.isWon && escapingSnakeAnim == null) {
            WinDialog(
                level = uiState.currentLevel,
                scoreBreakdown = uiState.lastScoreBreakdown,
                runTotalScore = uiState.runScore,
                coinsEarned = uiState.earnedCoinsOnWin,
                colors = colors,
                onNextLevel = {
                    soundManager.playTap()
                    viewModel.nextLevel()
                },
                onHome = {
                    soundManager.playTap()
                    viewModel.backToHome()
                }
            )
        }

        // 7. Run Summary Dialog (Displayed when losing all 3 hearts in a level or abandoning run)
        if (uiState.isRunOver && uiState.runSummary != null && escapingSnakeAnim == null) {
            val userRank = leaderboardEntries.find { it.playerName.equals(uiState.playerName, ignoreCase = true) }?.rank ?: 0
            RunSummaryDialog(
                summary = uiState.runSummary!!,
                currentRank = userRank,
                colors = colors,
                onStartNewRun = {
                    soundManager.playTap()
                    viewModel.startNewRun()
                },
                onBackToHome = {
                    soundManager.playTap()
                    viewModel.backToHome()
                }
            )
        }

        // 8. Anti-cheat Mid-Game Penalty Notice
        if (uiState.midGamePenaltyNotice) {
            AlertDialog(
                onDismissRequest = { viewModel.dismissMidGamePenaltyNotice() },
                shape = RoundedCornerShape(20.dp),
                containerColor = colors.surface,
                title = {
                    Text("⚠️ Penalty Applied", fontWeight = FontWeight.Bold, color = SnakeColors.BlockedRed)
                },
                text = {
                    Text(strings.midGameExitNotice, color = colors.textPrimary, fontSize = 14.sp)
                },
                confirmButton = {
                    Button(
                        onClick = { viewModel.dismissMidGamePenaltyNotice() },
                        colors = ButtonDefaults.buttonColors(containerColor = SnakeColors.ButtonGreen)
                    ) {
                        Text("OK", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            )
        }
    }
}

/**
 * Top Bar matching Image 2 layout with live score and countdown timer.
 */
@Composable
private fun TopGameBarImage2(
    level: Int,
    hearts: Int,
    maxHearts: Int,
    runScore: Long,
    timeSecondsProvider: () -> Int,
    escapedCount: Int,
    totalCount: Int,
    canUndo: Boolean,
    colors: GameColorScheme,
    onPauseClick: () -> Unit,
    onUndoClick: () -> Unit
) {
    Surface(
        color = Color(0xFF581C87), // Royal Purple top bar (Image 2)
        tonalElevation = 6.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // LEFT: Blue square buttons [||] [↺]
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF0284C7),
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF38BDF8)),
                        modifier = Modifier
                            .size(42.dp)
                            .clickable { onPauseClick() }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("II", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Black)
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (canUndo) Color(0xFF0284C7) else Color(0xFF334155),
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, if (canUndo) Color(0xFF38BDF8) else Color(0xFF475569)),
                        modifier = Modifier
                            .size(42.dp)
                            .clickable(enabled = canUndo) { onUndoClick() }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Undo",
                                tint = if (canUndo) Color.White else Color.Gray,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }

                // CENTER: Cyan Card "Level X" + 3 Red Hearts (Image 2)
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFF0284C7),
                    border = androidx.compose.foundation.BorderStroke(2.dp, Color(0xFF38BDF8)),
                    modifier = Modifier.padding(horizontal = 4.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 5.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Level $level",
                            color = Color.White,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Black
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            for (i in 1..maxHearts) {
                                val active = (i <= hearts)
                                Text(
                                    text = if (active) "❤️" else "🖤",
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }

                // RIGHT: Escaped Counter [0/5] + Mascot Avatar
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "$escapedCount/$totalCount",
                        color = Color.White,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Black
                    )

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF7E22CE),
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFA855F7)),
                        modifier = Modifier.size(42.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("🐍", fontSize = 22.sp)
                        }
                    }
                }
            }

            // Sub-row: Run Score (left) & Floating Timer (right)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Score badge
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF3B1569),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF7E22CE))
                ) {
                    Text(
                        text = "Score: ${String.format("%,d", runScore)}",
                        color = Color(0xFFFFD54F),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                    )
                }

                // Floating Orange Timer Capsule (Image 2)
                val timeSeconds = timeSecondsProvider()
                val minutes = timeSeconds / 60
                val seconds = timeSeconds % 60
                val timeString = String.format("%02d:%02d", minutes, seconds)

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = SnakeColors.OrangeTimer,
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFFDBA74))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = timeString,
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "⏰", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

/**
 * Bottom Booster Bar: Only Hint and Grid Vision, centered and balanced.
 */
@Composable
private fun BottomBoosterBar(
    hintCount: Int,
    gridVisionCount: Int,
    isGridVisionActive: Boolean,
    colors: GameColorScheme,
    onHintClick: () -> Unit,
    onGridVisionClick: () -> Unit
) {
    val strings = LocalAppStrings.current

    Surface(
        color = Color(0xFF581C87),
        tonalElevation = 6.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            BoosterButton(
                icon = Icons.Default.Star,
                label = strings.boosterHint,
                count = hintCount,
                isActive = false,
                colors = colors,
                onClick = onHintClick
            )

            Spacer(modifier = Modifier.width(48.dp))

            BoosterButton(
                icon = Icons.Default.Info,
                label = strings.boosterGridVision,
                count = gridVisionCount,
                isActive = isGridVisionActive,
                colors = colors,
                onClick = onGridVisionClick
            )
        }
    }
}

@Composable
private fun BoosterButton(
    icon: ImageVector,
    label: String,
    count: Int,
    isActive: Boolean,
    colors: GameColorScheme,
    onClick: () -> Unit
) {
    val isEnabled = (count != 0)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(horizontal = 8.dp)
    ) {
        Box(contentAlignment = Alignment.TopEnd) {
            FilledIconButton(
                onClick = onClick,
                enabled = isEnabled,
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = if (isActive) Color(0xFF00E676) else Color(0xFF3B1569),
                    disabledContainerColor = Color(0xFF3B1569).copy(alpha = 0.4f)
                ),
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .border(
                        width = if (isActive) 2.dp else 1.5.dp,
                        color = if (isActive) Color.White else Color(0xFF7E22CE),
                        shape = CircleShape
                    )
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = when {
                        isActive -> Color.Black
                        isEnabled -> Color(0xFF38BDF8)
                        else -> colors.textSecondary.copy(alpha = 0.4f)
                    },
                    modifier = Modifier.size(26.dp)
                )
            }

            if (count >= 0) {
                Box(
                    modifier = Modifier
                        .offset(x = 4.dp, y = (-2).dp)
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(if (count > 0) SnakeColors.ButtonGreen else Color.Gray),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "$count",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Text(
            text = label,
            color = if (isEnabled) Color.White else Color.Gray,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@Composable
private fun PauseDialog(
    colors: GameColorScheme,
    onResume: () -> Unit,
    onSettings: () -> Unit,
    onAbandonRun: () -> Unit
) {
    val strings = LocalAppStrings.current

    AlertDialog(
        onDismissRequest = onResume,
        shape = RoundedCornerShape(22.dp),
        containerColor = colors.surface,
        title = {
            Text(
                text = strings.pause.uppercase(),
                color = colors.textPrimary,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onResume,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = SnakeColors.ButtonGreen)
                ) {
                    Text(strings.resume, color = Color.White, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = onSettings,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.textPrimary)
                ) {
                    Text(strings.settings)
                }

                OutlinedButton(
                    onClick = onAbandonRun,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = SnakeColors.BlockedRed)
                ) {
                    Text(strings.abandonRun, color = SnakeColors.BlockedRed, fontWeight = FontWeight.Bold)
                }
            }
        },
        confirmButton = {}
    )
}

@Composable
private fun WinDialog(
    level: Int,
    scoreBreakdown: LevelScoreBreakdown?,
    runTotalScore: Long,
    coinsEarned: Int,
    colors: GameColorScheme,
    onNextLevel: () -> Unit,
    onHome: () -> Unit
) {
    val strings = LocalAppStrings.current

    AlertDialog(
        onDismissRequest = {},
        shape = RoundedCornerShape(26.dp),
        containerColor = colors.surface,
        title = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = strings.levelCleared,
                    color = SnakeColors.ButtonGreen,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = "${strings.levelPrefix} $level",
                    color = colors.textSecondary,
                    fontSize = 15.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Score breakdown card
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = colors.surfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (scoreBreakdown != null) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Base Score", fontSize = 13.sp, color = colors.textSecondary)
                                Text("${scoreBreakdown.baseScore}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Speed Multiplier", fontSize = 13.sp, color = colors.textSecondary)
                                Text(String.format("%.2fx", scoreBreakdown.speedMultiplier), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8))
                            }
                            if (scoreBreakdown.hintPenalty > 0 || scoreBreakdown.gridVisionPenalty > 0 || scoreBreakdown.heartPenalty > 0) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Penalties", fontSize = 13.sp, color = SnakeColors.BlockedRed)
                                    Text("-${scoreBreakdown.hintPenalty + scoreBreakdown.gridVisionPenalty + scoreBreakdown.heartPenalty}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = SnakeColors.BlockedRed)
                                }
                            }
                            Divider(color = colors.gridLine.copy(alpha = 0.5f), thickness = 0.8.dp)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Level Points", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                                Text("+${scoreBreakdown.finalScore}", fontSize = 15.sp, fontWeight = FontWeight.Black, color = Color(0xFFFFD54F))
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Total Run Score", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                            Text(String.format("%,d", runTotalScore), fontSize = 16.sp, fontWeight = FontWeight.Black, color = Color(0xFFFFB300))
                        }
                    }
                }

                // Coins earned
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text("🪙 +$coinsEarned Coins", fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = SnakeColors.GoldCoin)
                }
            }
        },
        confirmButton = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onHome) {
                    Text("🏠 ${strings.home}", color = colors.textSecondary)
                }

                Button(
                    onClick = onNextLevel,
                    colors = ButtonDefaults.buttonColors(containerColor = SnakeColors.ButtonGreen)
                ) {
                    Text(strings.nextLevel, color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    )
}
