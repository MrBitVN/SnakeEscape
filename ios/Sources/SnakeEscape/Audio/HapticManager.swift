import Foundation
#if os(iOS)
import UIKit
#endif

public final class HapticManager {

    public static let shared = HapticManager()

    public var isHapticEnabled: Bool = true

    #if os(iOS)
    private let lightImpact = UIImpactFeedbackGenerator(style: .light)
    private let mediumImpact = UIImpactFeedbackGenerator(style: .medium)
    private let notification = UINotificationFeedbackGenerator()
    #endif

    public init() {
        #if os(iOS)
        lightImpact.prepare()
        mediumImpact.prepare()
        notification.prepare()
        #endif
    }

    public func tap() {
        guard isHapticEnabled else { return }
        #if os(iOS)
        lightImpact.impactOccurred()
        #endif
    }

    public func escape() {
        guard isHapticEnabled else { return }
        #if os(iOS)
        mediumImpact.impactOccurred()
        #endif
    }

    public func blocked() {
        guard isHapticEnabled else { return }
        #if os(iOS)
        notification.notificationOccurred(.error)
        #endif
    }

    public func win() {
        guard isHapticEnabled else { return }
        #if os(iOS)
        notification.notificationOccurred(.success)
        #endif
    }
}
