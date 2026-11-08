import SwiftUI

/// Dialog Tạm Dừng (Pause) - Không có nút Restart
public struct PauseDialogView: View {
    public let colors: GameColorScheme
    public let strings: AppStrings
    public let onResume: () -> Void
    public let onSettings: () -> Void
    public let onAbandonRun: () -> Void

    public var body: some View {
        ZStack {
            Color.black.opacity(0.6)
                .ignoresSafeArea()

            VStack(spacing: 16) {
                Text(strings.pause.uppercased())
                    .font(.system(size: 22, weight: .bold))
                    .foregroundColor(colors.textPrimary)

                VStack(spacing: 10) {
                    Button(action: onResume) {
                        Text(strings.resume)
                            .font(.system(size: 15, weight: .bold))
                            .foregroundColor(.white)
                            .frame(maxWidth: .infinity)
                            .frame(height: 46)
                            .background(Color(red: 0.06, green: 0.73, blue: 0.51))
                            .clipShape(RoundedRectangle(cornerRadius: 12))
                    }

                    Button(action: onSettings) {
                        Text(strings.settings)
                            .font(.system(size: 15, weight: .semibold))
                            .foregroundColor(colors.textPrimary)
                            .frame(maxWidth: .infinity)
                            .frame(height: 46)
                            .background(colors.surfaceVariant)
                            .clipShape(RoundedRectangle(cornerRadius: 12))
                    }

                    Button(action: onAbandonRun) {
                        Text(strings.abandonRun)
                            .font(.system(size: 15, weight: .bold))
                            .foregroundColor(Color(red: 0.94, green: 0.27, blue: 0.27))
                            .frame(maxWidth: .infinity)
                            .frame(height: 46)
                            .background(Color(red: 0.94, green: 0.27, blue: 0.27).opacity(0.12))
                            .clipShape(RoundedRectangle(cornerRadius: 12))
                    }
                }
            }
            .padding(24)
            .background(colors.surface)
            .clipShape(RoundedRectangle(cornerRadius: 24))
            .shadow(radius: 20)
            .padding(.horizontal, 32)
        }
    }
}

/// Dialog Chiến Thắng (Win) - Không có nút Replay
public struct WinDialogView: View {
    public let level: Int
    public let scoreBreakdown: LevelScoreBreakdown?
    public let runScore: Int64
    public let colors: GameColorScheme
    public let strings: AppStrings
    public let onNextLevel: () -> Void
    public let onHome: () -> Void

    public var body: some View {
        ZStack {
            Color.black.opacity(0.65)
                .ignoresSafeArea()

            VStack(spacing: 16) {
                Text(strings.levelCleared)
                    .font(.system(size: 24, weight: .heavy))
                    .foregroundColor(Color(red: 0.06, green: 0.73, blue: 0.51))

                Text("\(strings.levelPrefix) \(level)")
                    .font(.system(size: 16, weight: .semibold))
                    .foregroundColor(colors.textSecondary)

                if let breakdown = scoreBreakdown {
                    VStack(spacing: 6) {
                        HStack {
                            Text("Base Score")
                                .font(.system(size: 13))
                                .foregroundColor(colors.textSecondary)
                            Spacer()
                            Text("\(breakdown.baseScore)")
                                .font(.system(size: 13, weight: .bold))
                        }
                        HStack {
                            Text("Speed Multiplier")
                                .font(.system(size: 13))
                                .foregroundColor(colors.textSecondary)
                            Spacer()
                            Text(String(format: "%.2fx", breakdown.speedMultiplier))
                                .font(.system(size: 13, weight: .bold))
                                .foregroundColor(Color(red: 0.22, green: 0.74, blue: 0.97))
                        }
                        let pen = breakdown.hintPenalty + breakdown.gridVisionPenalty + breakdown.heartPenalty
                        if pen > 0 {
                            HStack {
                                Text("Penalties")
                                    .font(.system(size: 13))
                                    .foregroundColor(Color(red: 0.94, green: 0.27, blue: 0.27))
                                Spacer()
                                Text("-\(pen)")
                                    .font(.system(size: 13, weight: .bold))
                                    .foregroundColor(Color(red: 0.94, green: 0.27, blue: 0.27))
                            }
                        }
                        Divider().background(colors.gridLine)
                        HStack {
                            Text("Level Points")
                                .font(.system(size: 14, weight: .bold))
                            Spacer()
                            Text("+\(breakdown.finalScore)")
                                .font(.system(size: 15, weight: .heavy))
                                .foregroundColor(Color(red: 1.0, green: 0.84, blue: 0.31))
                        }
                        HStack {
                            Text("Run Total Score")
                                .font(.system(size: 14, weight: .bold))
                            Spacer()
                            Text("\(runScore)")
                                .font(.system(size: 16, weight: .black))
                                .foregroundColor(Color(red: 1.0, green: 0.7, blue: 0.0))
                        }
                    }
                    .padding(12)
                    .background(colors.surfaceVariant.opacity(0.6))
                    .clipShape(RoundedRectangle(cornerRadius: 14))
                }

                HStack(spacing: 12) {
                    Button(action: onHome) {
                        Text(strings.home)
                            .font(.system(size: 15, weight: .semibold))
                            .foregroundColor(colors.textSecondary)
                            .frame(maxWidth: .infinity)
                            .frame(height: 46)
                            .background(colors.surfaceVariant)
                            .clipShape(RoundedRectangle(cornerRadius: 12))
                    }

                    Button(action: onNextLevel) {
                        Text(strings.nextLevel)
                            .font(.system(size: 15, weight: .bold))
                            .foregroundColor(.white)
                            .frame(maxWidth: .infinity)
                            .frame(height: 46)
                            .background(Color(red: 0.06, green: 0.73, blue: 0.51))
                            .clipShape(RoundedRectangle(cornerRadius: 12))
                    }
                }
                .padding(.top, 8)
            }
            .padding(24)
            .background(colors.surface)
            .clipShape(RoundedRectangle(cornerRadius: 24))
            .shadow(radius: 20)
            .padding(.horizontal, 30)
        }
    }
}

/// Màn Tổng Kết Run (Run Summary Dialog)
public struct RunSummaryDialogView: View {
    public let summary: RunSummary
    public let currentRank: Int
    public let colors: GameColorScheme
    public let strings: AppStrings
    public let onStartNewRun: () -> Void
    public let onHome: () -> Void

    public var body: some View {
        ZStack {
            Color.black.opacity(0.7)
                .ignoresSafeArea()

            VStack(spacing: 16) {
                // Trophy icon
                ZStack {
                    Circle()
                        .fill(LinearGradient(colors: [Color(red: 1.0, green: 0.84, blue: 0.31), Color(red: 1.0, green: 0.6, blue: 0.0)], startPoint: .top, endPoint: .bottom))
                        .frame(width: 64, height: 64)
                    Image(systemName: "trophy.fill")
                        .font(.system(size: 32))
                        .foregroundColor(.white)
                }

                Text(strings.runOver.uppercased())
                    .font(.system(size: 22, weight: .black))
                    .foregroundColor(colors.textPrimary)

                Text(summary.playerName)
                    .font(.system(size: 16, weight: .bold))
                    .foregroundColor(Color(red: 0.22, green: 0.74, blue: 0.97))

                VStack(spacing: 10) {
                    summaryRow(label: strings.levelsClearedLabel, value: "\(summary.levelsCleared)")
                    Divider().background(colors.gridLine.opacity(0.5))
                    summaryRow(label: strings.totalScoreLabel, value: "\(summary.totalScore)", valueColor: Color(red: 1.0, green: 0.7, blue: 0.0), isBold: true)
                    Divider().background(colors.gridLine.opacity(0.5))
                    let mins = (summary.totalTimeMs / 1000) / 60
                    let secs = (summary.totalTimeMs / 1000) % 60
                    summaryRow(label: strings.totalTimeLabel, value: String(format: "%02d:%02d", mins, secs))
                    Divider().background(colors.gridLine.opacity(0.5))
                    summaryRow(label: strings.currentRankLabel, value: currentRank > 0 ? "#\(currentRank)" : "--", valueColor: Color(red: 0.0, green: 0.9, blue: 0.46), isBold: true)
                }
                .padding(14)
                .background(colors.surfaceVariant.opacity(0.6))
                .clipShape(RoundedRectangle(cornerRadius: 16))

                VStack(spacing: 10) {
                    Button(action: onStartNewRun) {
                        HStack {
                            Image(systemName: "play.fill")
                            Text(strings.playAgain)
                        }
                        .font(.system(size: 16, weight: .heavy))
                        .foregroundColor(.white)
                        .frame(maxWidth: .infinity)
                        .frame(height: 50)
                        .background(Color(red: 0.06, green: 0.73, blue: 0.51))
                        .clipShape(RoundedRectangle(cornerRadius: 14))
                    }

                    Button(action: onHome) {
                        Text(strings.home)
                            .font(.system(size: 15, weight: .bold))
                            .foregroundColor(colors.textPrimary)
                            .frame(maxWidth: .infinity)
                            .frame(height: 44)
                            .background(colors.surfaceVariant)
                            .clipShape(RoundedRectangle(cornerRadius: 14))
                    }
                }
                .padding(.top, 4)
            }
            .padding(24)
            .background(colors.surface)
            .clipShape(RoundedRectangle(cornerRadius: 26))
            .shadow(radius: 24)
            .padding(.horizontal, 28)
        }
    }

    private func summaryRow(label: String, value: String, valueColor: Color? = nil, isBold: Bool = false) -> some View {
        HStack {
            Text(label)
                .font(.system(size: 14))
                .foregroundColor(colors.textSecondary)
            Spacer()
            Text(value)
                .font(.system(size: 15, weight: isBold ? .black : .bold))
                .foregroundColor(valueColor ?? colors.textPrimary)
        }
    }
}
