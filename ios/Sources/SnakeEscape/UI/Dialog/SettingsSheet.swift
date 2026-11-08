import SwiftUI

public struct SettingsSheet: View {
    @Binding public var soundEnabled: Bool
    @Binding public var musicEnabled: Bool
    @Binding public var hapticEnabled: Bool
    @Binding public var themeMode: String
    @Binding public var language: String
    public let colors: GameColorScheme
    public let strings: AppStrings
    public let onDismiss: () -> Void

    public init(
        soundEnabled: Binding<Bool>,
        musicEnabled: Binding<Bool>,
        hapticEnabled: Binding<Bool>,
        themeMode: Binding<String>,
        language: Binding<String>,
        colors: GameColorScheme,
        strings: AppStrings,
        onDismiss: @escaping () -> Void
    ) {
        self._soundEnabled = soundEnabled
        self._musicEnabled = musicEnabled
        self._hapticEnabled = hapticEnabled
        self._themeMode = themeMode
        self._language = language
        self.colors = colors
        self.strings = strings
        self.onDismiss = onDismiss
    }

    public var body: some View {
        VStack(spacing: 20) {
            // Header
            HStack {
                Text(strings.settings)
                    .font(.system(size: 22, weight: .bold))
                    .foregroundColor(colors.textPrimary)

                Spacer()

                Button(action: onDismiss) {
                    Image(systemName: "xmark.circle.fill")
                        .font(.system(size: 26))
                        .foregroundColor(colors.textSecondary.opacity(0.8))
                }
            }

            VStack(spacing: 12) {
                // Sound FX Toggle
                ToggleRow(
                    icon: "speaker.wave.2.fill",
                    title: strings.soundEffects,
                    isOn: $soundEnabled,
                    colors: colors
                )

                // Music Toggle
                ToggleRow(
                    icon: "music.note",
                    title: strings.music,
                    isOn: $musicEnabled,
                    colors: colors
                )

                // Haptic Toggle
                ToggleRow(
                    icon: "iphone.radiowaves.left.and.right",
                    title: strings.haptics,
                    isOn: $hapticEnabled,
                    colors: colors
                )
            }

            Divider()
                .background(colors.gridLine.opacity(0.5))

            // Theme Selector
            VStack(alignment: .leading, spacing: 8) {
                Text(strings.theme)
                    .font(.system(size: 14, weight: .semibold))
                    .foregroundColor(colors.textSecondary)

                HStack(spacing: 8) {
                    ThemeSegmentButton(title: strings.themeSystem, value: "SYSTEM", current: $themeMode, colors: colors)
                    ThemeSegmentButton(title: strings.themeDark, value: "DARK", current: $themeMode, colors: colors)
                    ThemeSegmentButton(title: strings.themeLight, value: "LIGHT", current: $themeMode, colors: colors)
                }
            }

            Divider()
                .background(colors.gridLine.opacity(0.5))

            // Language Selector
            VStack(alignment: .leading, spacing: 8) {
                Text(strings.language)
                    .font(.system(size: 14, weight: .semibold))
                    .foregroundColor(colors.textSecondary)

                HStack(spacing: 8) {
                    ThemeSegmentButton(title: strings.langVietnamese, value: "VI", current: $language, colors: colors)
                    ThemeSegmentButton(title: strings.langEnglish, value: "EN", current: $language, colors: colors)
                }
            }

            Spacer()

            Button(action: onDismiss) {
                Text(strings.close)
                    .font(.system(size: 16, weight: .bold))
                    .foregroundColor(.white)
                    .frame(maxWidth: .infinity)
                    .frame(height: 48)
                    .background(Color(red: 0.06, green: 0.73, blue: 0.51))
                    .clipShape(RoundedRectangle(cornerRadius: 12))
            }
        }
        .padding(24)
        .background(colors.surface)
    }
}

private struct ToggleRow: View {
    let icon: String
    let title: String
    @Binding var isOn: Bool
    let colors: GameColorScheme

    var body: some View {
        HStack {
            Image(systemName: icon)
                .font(.system(size: 18))
                .foregroundColor(Color(red: 0.06, green: 0.73, blue: 0.51))
                .frame(width: 28)

            Text(title)
                .font(.system(size: 15, weight: .medium))
                .foregroundColor(colors.textPrimary)

            Spacer()

            Toggle("", isOn: $isOn)
                .labelsHidden()
                .tint(Color(red: 0.06, green: 0.73, blue: 0.51))
        }
        .padding(.horizontal, 14)
        .padding(.vertical, 10)
        .background(colors.surfaceVariant.opacity(0.6))
        .clipShape(RoundedRectangle(cornerRadius: 12))
    }
}

private struct ThemeSegmentButton: View {
    let title: String
    let value: String
    @Binding var current: String
    let colors: GameColorScheme

    var isSelected: Bool {
        current.caseInsensitiveCompare(value) == .orderedSame
    }

    var body: some View {
        Button(action: {
            current = value
        }) {
            Text(title)
                .font(.system(size: 13, weight: isSelected ? .bold : .regular))
                .foregroundColor(isSelected ? .white : colors.textSecondary)
                .frame(maxWidth: .infinity)
                .frame(height: 38)
                .background(isSelected ? Color(red: 0.06, green: 0.73, blue: 0.51) : colors.surfaceVariant)
                .clipShape(RoundedRectangle(cornerRadius: 10))
        }
        .buttonStyle(.plain)
    }
}
