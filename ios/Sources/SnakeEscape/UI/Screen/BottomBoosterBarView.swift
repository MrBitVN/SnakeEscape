import SwiftUI

public struct BottomBoosterBarView: View {
    public let hintCount: Int
    public let gridVisionCount: Int
    public let isGridVisionActive: Bool
    public let colors: GameColorScheme
    public let strings: AppStrings
    public let onHintClick: () -> Void
    public let onGridVisionClick: () -> Void

    public init(
        hintCount: Int,
        gridVisionCount: Int,
        isGridVisionActive: Bool,
        colors: GameColorScheme,
        strings: AppStrings,
        onHintClick: @escaping () -> Void,
        onGridVisionClick: @escaping () -> Void
    ) {
        self.hintCount = hintCount
        self.gridVisionCount = gridVisionCount
        self.isGridVisionActive = isGridVisionActive
        self.colors = colors
        self.strings = strings
        self.onHintClick = onHintClick
        self.onGridVisionClick = onGridVisionClick
    }

    public var body: some View {
        HStack(spacing: 48) {
            Spacer()

            BoosterButton(
                iconName: "lightbulb.fill",
                label: strings.boosterHint,
                count: hintCount,
                isActive: false,
                colors: colors,
                onClick: onHintClick
            )

            BoosterButton(
                iconName: "eye.fill",
                label: strings.boosterGridVision,
                count: gridVisionCount,
                isActive: isGridVisionActive,
                colors: colors,
                onClick: onGridVisionClick
            )

            Spacer()
        }
        .padding(.horizontal, 24)
        .padding(.vertical, 10)
        .background(Color(red: 0.35, green: 0.11, blue: 0.53))
        .shadow(color: Color.black.opacity(0.12), radius: 3, y: -2)
    }
}
