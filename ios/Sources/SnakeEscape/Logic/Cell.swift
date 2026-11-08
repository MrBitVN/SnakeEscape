import Foundation

/// Đại diện cho một toạ độ (x, y) trên bàn cờ lưới Snake Escape.
public struct Cell: Hashable, Equatable, Codable, CustomStringConvertible {
    public let x: Int
    public let y: Int

    public init(_ x: Int, _ y: Int) {
        self.x = x
        self.y = y
    }

    public var description: String {
        "(\(x), \(y))"
    }

    public static func + (lhs: Cell, rhs: Direction) -> Cell {
        Cell(lhs.x + rhs.dx, lhs.y + rhs.dy)
    }
}
