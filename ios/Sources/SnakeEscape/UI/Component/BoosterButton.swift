import SwiftUI

public struct BoosterButton: View {
    public let iconName: String
    public let label: String
    public let count: Int
    public let isActive: Bool
    public let colors: GameColorScheme
    public let onClick: () -> Void

    public init(
        iconName: String,
        label: String,
        count: Int,
        isActive: Bool,
        colors: GameColorScheme,
        onClick: @escaping () -> Void
    ) {
        self.iconName = iconName
        self.label = label
        self.count = count
        self.isActive = isActive
        self.colors = colors
        self.onClick = onClick
    }

    private var isEnabled: Bool {
        count != 0
    }

    public var body: some View {
        Button(action: onClick) {
            VStack(spacing: 4) {
                ZStack(alignment: .topTrailing) {
                    Circle()
                        .fill(isActive ? Color(red: 0.0, green: 0.9, blue: 0.46) : colors.surfaceVariant)
                        .frame(width: 52, height: 52)
                        .overlay(
                            Circle()
                                .stroke(isActive ? Color.white : colors.gridLine.opacity(0.5), lineWidth: isActive ? 2 : 1)
                        )
                        .overlay(
                            Image(systemName: iconName)
                                .font(.system(size: 22, weight: .bold))
                                .foregroundColor(
                                    isActive ? .black : (isEnabled ? (colors.isDark ? Color(red: 0.0, green: 0.9, blue: 0.75) : Color(red: 0.02, green: 0.59, blue: 0.41)) : colors.textSecondary.opacity(0.4))
                                )
                        )

                    if count >= 0 {
                        Text("\(count)")
                            .font(.system(size: 11, weight: .heavy))
                            .foregroundColor(.white)
                            .frame(width: 20, height: 20)
                            .background(count > 0 ? Color(red: 1.0, green: 0.16, blue: 0.52) : Color.gray)
                            .clipShape(Circle())
                            .offset(x: 4, y: -2)
                    }
                }

                Text(label)
                    .font(.system(size: 11, weight: .medium))
                    .foregroundColor(isEnabled ? colors.textPrimary : colors.textSecondary.opacity(0.4))
            }
        }
        .disabled(!isEnabled)
        .buttonStyle(.plain)
    }
}
