import SwiftUI

public struct SnakeEscapeGameView: View {
    @StateObject private var viewModel = GameViewModel()
    @Environment(\.colorScheme) private var systemColorScheme

    private let soundManager = SoundManager.shared
    private let hapticManager = HapticManager.shared

    // Animation states
    @State private var escapingSnake: EscapingSnakeAnimationState? = nil
    @State private var shakeProgress: CGFloat = 0.0
    @State private var blockedAlpha: CGFloat = 0.0

    public init() {}

    private var currentStrings: AppStrings {
        StringsHelper.getStrings(for: "EN")
    }

    private var currentColors: GameColorScheme {
        return SnakeColors.lightScheme
    }

    public var body: some View {
        let state = viewModel.uiState
        let strings = currentStrings
        let colors = currentColors

        ZStack {
            colors.background
                .ignoresSafeArea()

            VStack(spacing: 0) {
                // 1. Top Bar (Matching Image 2 with runScore, countdown timer, hearts)
                TopGameBarView(
                    level: state.currentLevel,
                    hearts: state.levelHearts,
                    maxHearts: ScoringConfig.initialLevelHearts,
                    runScore: state.runScore,
                    timeSeconds: state.timeRemainingSeconds,
                    escapedCount: state.escapedSnakeCount,
                    totalCount: state.initialSnakeCount,
                    canUndo: !state.gameState.history.isEmpty,
                    colors: colors,
                    strings: strings,
                    onPauseClick: {
                        soundManager.playTap()
                        hapticManager.tap()
                        viewModel.pauseGame()
                    },
                    onUndoClick: {
                        soundManager.playTap()
                        hapticManager.tap()
                        viewModel.undo()
                    }
                )

                // 2. Main Board Canvas
                SnakeBoardCanvasView(
                    state: state.gameState,
                    colors: colors,
                    shakingSnakeId: state.shakingSnakeId,
                    shakeProgress: shakeProgress,
                    blockedCell: state.blockedCell,
                    blockedAlpha: blockedAlpha,
                    hintSnakeId: state.hintSnakeId,
                    isGridVisionActive: state.isGridVisionActive,
                    escapingSnake: escapingSnake,
                    onSnakeTapped: { snakeId in
                        handleSnakeTap(snakeId: snakeId)
                    }
                )
                .frame(maxWidth: .infinity, maxHeight: .infinity)

                // 3. Bottom Booster Bar: ONLY Hint and Grid Vision, centered
                BottomBoosterBarView(
                    hintCount: state.hintBoosterCount,
                    gridVisionCount: state.gridVisionBoosterCount,
                    isGridVisionActive: state.isGridVisionActive,
                    colors: colors,
                    strings: strings,
                    onHintClick: {
                        soundManager.playTap()
                        hapticManager.tap()
                        viewModel.useHint()
                    },
                    onGridVisionClick: {
                        soundManager.playTap()
                        hapticManager.tap()
                        viewModel.useGridVision()
                    }
                )
            }

            // Dialog Tạm dừng (Pause) - No restart button
            if state.gameStatus == .paused && !state.isSettingsOpen {
                PauseDialogView(
                    colors: colors,
                    strings: strings,
                    onResume: {
                        soundManager.playTap()
                        viewModel.resumeGame()
                    },
                    onSettings: {
                        soundManager.playTap()
                        viewModel.openSettings()
                    },
                    onAbandonRun: {
                        soundManager.playTap()
                        viewModel.abandonCurrentRun()
                    }
                )
            }

            // Dialog Chiến Thắng (Win) - Displays score breakdown & total score
            if state.isWon && escapingSnake == nil {
                WinDialogView(
                    level: state.currentLevel,
                    scoreBreakdown: state.lastScoreBreakdown,
                    runScore: state.runScore,
                    colors: colors,
                    strings: strings,
                    onNextLevel: {
                        soundManager.playTap()
                        viewModel.nextLevel()
                    },
                    onHome: {
                        soundManager.playTap()
                        viewModel.startNewRun()
                    }
                )
            }

            // Màn Tổng Kết Run (Run Summary Dialog) - Khi mất 3 tim trong 1 màn
            if state.isGameOver && escapingSnake == nil, let summary = state.runSummary {
                let userRank = viewModel.leaderboardRepository.leaderboard.first(where: {
                    $0.playerName.caseInsensitiveCompare(state.playerName) == .orderedSame
                })?.rank ?? 0

                RunSummaryDialogView(
                    summary: summary,
                    currentRank: userRank,
                    colors: colors,
                    strings: strings,
                    onStartNewRun: {
                        soundManager.playTap()
                        viewModel.startNewRun()
                    },
                    onHome: {
                        soundManager.playTap()
                        viewModel.startNewRun()
                    }
                )
            }
        }
        .sheet(isPresented: Binding(
            get: { viewModel.uiState.isSettingsOpen },
            set: { if !$0 { viewModel.closeSettings() } }
        )) {
            SettingsSheet(
                soundEnabled: state.soundEnabled,
                musicEnabled: state.musicEnabled,
                hapticEnabled: state.hapticEnabled,
                themeMode: state.themeMode,
                language: "EN",
                colors: colors,
                strings: strings,
                onSoundToggle: { viewModel.setSoundEnabled($0) },
                onMusicToggle: { viewModel.setMusicEnabled($0) },
                onHapticToggle: { viewModel.setHapticEnabled($0) },
                onThemeSelect: { viewModel.setThemeMode($0) },
                onLanguageSelect: { _ in },
                onClose: { viewModel.closeSettings() }
            )
        }
    }

    private func handleSnakeTap(snakeId: Int) {
        guard escapingSnake == nil else { return }

        soundManager.playTap()
        hapticManager.tap()

        viewModel.onSnakeTapped(
            snakeId: snakeId,
            onEscaped: { snake in
                soundManager.playEscape()
                hapticManager.escape()

                let exitDist = max(viewModel.uiState.gameState.width, viewModel.uiState.gameState.height) + snake.length + 2
                var pathCells: [Cell] = []
                for cell in snake.body.reversed() {
                    pathCells.append(cell)
                }
                var forward = snake.head + snake.direction
                for _ in 1...exitDist {
                    pathCells.append(forward)
                    forward = forward + snake.direction
                }

                let animState = EscapingSnakeAnimationState(
                    snake: snake,
                    pathCells: pathCells,
                    progress: 0.0
                )
                self.escapingSnake = animState

                withAnimation(.linear(duration: 0.28 + Double(snake.length) * 0.045)) {
                    self.escapingSnake?.progress = CGFloat(exitDist)
                }

                DispatchQueue.main.asyncAfter(deadline: .now() + 0.35 + Double(snake.length) * 0.045) {
                    self.escapingSnake = nil
                }
            },
            onBlocked: { snake, cell in
                soundManager.playBlocked()
                hapticManager.blocked()

                withAnimation(.easeInOut(duration: 0.35)) {
                    self.shakeProgress = 1.0
                }
                DispatchQueue.main.asyncAfter(deadline: .now() + 0.35) {
                    self.shakeProgress = 0.0
                    self.viewModel.clearShakeAndBlocked()
                }

                withAnimation(.easeInOut(duration: 0.5)) {
                    self.blockedAlpha = 1.0
                }
                DispatchQueue.main.asyncAfter(deadline: .now() + 0.5) {
                    self.blockedAlpha = 0.0
                }
            }
        )
    }
}
