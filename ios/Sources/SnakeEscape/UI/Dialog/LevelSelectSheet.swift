import SwiftUI

public struct LevelSelectSheet: View {
    public let currentMode: GameMode
    public let currentLevel: Int
    public let highestUnlockedLevel: Int
    public let highestUnlockedChallengeLevel: Int
    public let colors: GameColorScheme
    public let strings: AppStrings
    public let onSelectLevel: (Int, GameMode) -> Void
    public let onDismiss: () -> Void

    @State private var selectedMode: GameMode

    public init(
        currentMode: GameMode = .campaign,
        currentLevel: Int,
        highestUnlockedLevel: Int,
        highestUnlockedChallengeLevel: Int,
        colors: GameColorScheme,
        strings: AppStrings,
        onSelectLevel: @escaping (Int, GameMode) -> Void,
        onDismiss: @escaping () -> Void
    ) {
        self.currentMode = currentMode
        self.currentLevel = currentLevel
        self.highestUnlockedLevel = highestUnlockedLevel
        self.highestUnlockedChallengeLevel = highestUnlockedChallengeLevel
        self.colors = colors
        self.strings = strings
        self.onSelectLevel = onSelectLevel
        self.onDismiss = onDismiss
        _selectedMode = State(initialValue: currentMode)
    }

    private let columns = [
        GridItem(.adaptive(minimum: 60, maximum: 76), spacing: 10)
    ]

    public var body: some View {
        VStack(spacing: 14) {
            // Header
            HStack {
                Text(strings.selectLevel.uppercased())
                    .font(.system(size: 20, weight: .black))
                    .foregroundColor(colors.textPrimary)

                Spacer()

                Button(action: onDismiss) {
                    Image(systemName: "xmark.circle.fill")
                        .font(.system(size: 26))
                        .foregroundColor(colors.textSecondary.opacity(0.8))
                }
            }

            // Mode Selector Segmented Tabs
            HStack(spacing: 8) {
                // Campaign Tab
                Button(action: { selectedMode = .campaign }) {
                    Text("🛡️ Chiến Dịch")
                        .font(.system(size: 13, weight: .bold))
                        .foregroundColor(selectedMode == .campaign ? .white : colors.textSecondary)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 10)
                        .background(
                            selectedMode == .campaign ? Color(red: 0.06, green: 0.73, blue: 0.51) : Color.clear
                        )
                        .cornerRadius(10)
                }

                // Challenge Tab
                Button(action: { selectedMode = .challenge }) {
                    Text("⚡ Thử Thách")
                        .font(.system(size: 13, weight: .bold))
                        .foregroundColor(selectedMode == .challenge ? .white : colors.textSecondary)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 10)
                        .background(
                            selectedMode == .challenge ? Color(red: 1.0, green: 0.16, blue: 0.52) : Color.clear
                        )
                        .cornerRadius(10)
                }
            }
            .padding(4)
            .background(colors.surfaceVariant.opacity(0.7))
            .cornerRadius(12)

            let totalLevels = (selectedMode == .campaign) ? 200 : 50
            let highestUnlocked = (selectedMode == .campaign) ? highestUnlockedLevel : highestUnlockedChallengeLevel

            HStack {
                Text(selectedMode == .challenge ? "Mê cung nan giải cực khó" : strings.escapedLabel)
                    .font(.system(size: 12, weight: .semibold))
                    .foregroundColor(selectedMode == .challenge ? Color(red: 1.0, green: 0.4, blue: 0.2) : colors.textSecondary)
                Spacer()
                Text("\(highestUnlocked) / \(totalLevels)")
                    .font(.system(size: 13, weight: .bold))
                    .foregroundColor(selectedMode == .challenge ? Color(red: 1.0, green: 0.84, blue: 0.0) : Color(red: 0.06, green: 0.73, blue: 0.51))
            }
            .padding(.horizontal, 4)

            // Grid levels
            ScrollView {
                LazyVGrid(columns: columns, spacing: 10) {
                    ForEach(1...totalLevels, id: \.self) { level in
                        let isUnlocked = (level <= highestUnlocked)
                        let isCurrent = (selectedMode == currentMode && level == currentLevel)

                        Button(action: {
                            if isUnlocked {
                                onSelectLevel(level, selectedMode)
                            }
                        }) {
                            ZStack {
                                RoundedRectangle(cornerRadius: 14)
                                    .fill(
                                        isCurrent ? (selectedMode == .challenge ? Color(red: 1.0, green: 0.16, blue: 0.52) : Color(red: 0.06, green: 0.73, blue: 0.51)) :
                                        (isUnlocked ? (selectedMode == .challenge ? Color(red: 1.0, green: 0.16, blue: 0.52).opacity(0.2) : colors.surfaceVariant) :
                                        (colors.isDark ? Color(red: 0.05, green: 0.08, blue: 0.14) : Color(red: 0.88, green: 0.90, blue: 0.94)))
                                    )
                                    .overlay(
                                        RoundedRectangle(cornerRadius: 14)
                                            .stroke(
                                                isCurrent ? Color.white :
                                                (isUnlocked && selectedMode == .challenge ? Color(red: 1.0, green: 0.84, blue: 0.0).opacity(0.5) : colors.gridLine.opacity(0.4)),
                                                lineWidth: isCurrent ? 2 : 1
                                            )
                                    )

                                if isUnlocked {
                                    VStack(spacing: 2) {
                                        Text("\(level)")
                                            .font(.system(size: 16, weight: .bold))
                                            .foregroundColor(isCurrent ? .white : colors.textPrimary)

                                        if level < highestUnlocked {
                                            Image(systemName: "star.fill")
                                                .font(.system(size: 10))
                                                .foregroundColor(selectedMode == .challenge ? Color(red: 1.0, green: 0.84, blue: 0.0) : Color(red: 1.0, green: 0.84, blue: 0.0))
                                        }
                                    }
                                } else {
                                    Image(systemName: "lock.fill")
                                        .font(.system(size: 15))
                                        .foregroundColor(colors.textSecondary.opacity(0.35))
                                }
                            }
                            .frame(height: 56)
                        }
                        .disabled(!isUnlocked)
                        .buttonStyle(.plain)
                    }
                }
                .padding(.vertical, 4)
            }
        }
        .padding(20)
        .background(colors.surface)
    }
}
