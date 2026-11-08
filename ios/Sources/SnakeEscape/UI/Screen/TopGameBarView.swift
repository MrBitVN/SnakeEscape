import SwiftUI

public struct TopGameBarView: View {
    public let level: Int
    public let hearts: Int
    public let maxHearts: Int
    public let runScore: Int64
    public let timeSeconds: Int
    public let escapedCount: Int
    public let totalCount: Int
    public let canUndo: Bool
    public let colors: GameColorScheme
    public let strings: AppStrings
    public let onPauseClick: () -> Void
    public let onUndoClick: () -> Void

    public init(
        level: Int,
        hearts: Int,
        maxHearts: Int = 3,
        runScore: Int64 = 0,
        timeSeconds: Int,
        escapedCount: Int,
        totalCount: Int,
        canUndo: Bool,
        colors: GameColorScheme,
        strings: AppStrings,
        onPauseClick: @escaping () -> Void,
        onUndoClick: @escaping () -> Void
    ) {
        self.level = level
        self.hearts = hearts
        self.maxHearts = maxHearts
        self.runScore = runScore
        self.timeSeconds = timeSeconds
        self.escapedCount = escapedCount
        self.totalCount = totalCount
        self.canUndo = canUndo
        self.colors = colors
        self.strings = strings
        self.onPauseClick = onPauseClick
        self.onUndoClick = onUndoClick
    }

    public var body: some View {
        VStack(spacing: 6) {
            // Main Top Bar: Left buttons, Center Level Card + Hearts, Right Counter + Mascot
            HStack(spacing: 8) {
                // LEFT: [II] and [↺]
                HStack(spacing: 6) {
                    Button(action: onPauseClick) {
                        Text("II")
                            .font(.system(size: 16, weight: .black))
                            .foregroundColor(.white)
                            .frame(width: 38, height: 38)
                            .background(Color(red: 0.01, green: 0.52, blue: 0.78))
                            .overlay(RoundedRectangle(cornerRadius: 10).stroke(Color(red: 0.22, green: 0.74, blue: 0.97), lineWidth: 1.5))
                            .clipShape(RoundedRectangle(cornerRadius: 10))
                    }

                    Button(action: onUndoClick) {
                        Image(systemName: "arrow.counterclockwise")
                            .font(.system(size: 16, weight: .bold))
                            .foregroundColor(canUndo ? .white : .gray)
                            .frame(width: 38, height: 38)
                            .background(canUndo ? Color(red: 0.01, green: 0.52, blue: 0.78) : Color(red: 0.2, green: 0.25, blue: 0.33))
                            .overlay(RoundedRectangle(cornerRadius: 10).stroke(canUndo ? Color(red: 0.22, green: 0.74, blue: 0.97) : Color.gray.opacity(0.3), lineWidth: 1.5))
                            .clipShape(RoundedRectangle(cornerRadius: 10))
                    }
                    .disabled(!canUndo)
                }

                Spacer()

                // CENTER: Cyan Card "Level X" + 3 Red Hearts (matching Image 2)
                VStack(spacing: 2) {
                    Text("\(strings.levelPrefix) \(level)")
                        .font(.system(size: 16, weight: .black))
                        .foregroundColor(.white)

                    HStack(spacing: 3) {
                        ForEach(1...maxHearts, id: \.self) { i in
                            Text(i <= hearts ? "❤️" : "🖤")
                                .font(.system(size: 13))
                        }
                    }
                }
                .padding(.horizontal, 16)
                .padding(.vertical, 4)
                .background(Color(red: 0.01, green: 0.52, blue: 0.78))
                .overlay(RoundedRectangle(cornerRadius: 14).stroke(Color(red: 0.22, green: 0.74, blue: 0.97), lineWidth: 1.5))
                .clipShape(RoundedRectangle(cornerRadius: 14))

                Spacer()

                // RIGHT: Escaped Counter [x/y] + Snake Mascot
                HStack(spacing: 6) {
                    Text("\(escapedCount)/\(totalCount)")
                        .font(.system(size: 16, weight: .black))
                        .foregroundColor(.white)

                    Text("🐍")
                        .font(.system(size: 20))
                        .frame(width: 38, height: 38)
                        .background(Color(red: 0.49, green: 0.13, blue: 0.81))
                        .overlay(RoundedRectangle(cornerRadius: 10).stroke(Color(red: 0.66, green: 0.33, blue: 0.97), lineWidth: 1.5))
                        .clipShape(RoundedRectangle(cornerRadius: 10))
                }
            }

            // Sub-row: Run Score badge & Floating Timer Capsule
            HStack {
                Text("Score: \(runScore)")
                    .font(.system(size: 12, weight: .heavy))
                    .foregroundColor(Color(red: 1.0, green: 0.84, blue: 0.31))
                    .padding(.horizontal, 10)
                    .padding(.vertical, 3)
                    .background(Color(red: 0.23, green: 0.08, blue: 0.41))
                    .overlay(RoundedRectangle(cornerRadius: 10).stroke(Color(red: 0.49, green: 0.13, blue: 0.81), lineWidth: 1))
                    .clipShape(RoundedRectangle(cornerRadius: 10))

                Spacer()

                let mins = timeSeconds / 60
                let secs = timeSeconds % 60
                let timeString = String(format: "%02d:%02d", mins, secs)

                HStack(spacing: 4) {
                    Text(timeString)
                        .font(.system(size: 13, weight: .black))
                        .foregroundColor(.white)
                    Text("⏰")
                        .font(.system(size: 12))
                }
                .padding(.horizontal, 8)
                .padding(.vertical, 3)
                .background(Color(red: 0.96, green: 0.42, blue: 0.16))
                .overlay(RoundedRectangle(cornerRadius: 12).stroke(Color(red: 1.0, green: 0.73, blue: 0.45), lineWidth: 1.5))
                .clipShape(RoundedRectangle(cornerRadius: 12))
            }
        }
        .padding(.horizontal, 14)
        .padding(.vertical, 8)
        .background(Color(red: 0.35, green: 0.11, blue: 0.53))
        .shadow(color: Color.black.opacity(0.15), radius: 3, y: 2)
    }
}
