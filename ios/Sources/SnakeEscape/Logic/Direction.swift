import Foundation

/// 4 hướng di chuyển của đầu rắn: UP, DOWN, LEFT, RIGHT.
public enum Direction: String, CaseIterable, Codable, CustomStringConvertible {
    case up = "UP"
    case down = "DOWN"
    case left = "LEFT"
    case right = "RIGHT"

    public var dx: Int {
        switch self {
        case .left: return -1
        case .right: return 1
        case .up, .down: return 0
        }
    }

    public var dy: Int {
        switch self {
        case .up: return -1
        case .down: return 1
        case .left, .right: return 0
        }
    }

    public var opposite: Direction {
        switch self {
        case .up: return .down
        case .down: return .up
        case .left: return .right
        case .right: return .left
        }
    }

    public var description: String {
        rawValue
    }
}
