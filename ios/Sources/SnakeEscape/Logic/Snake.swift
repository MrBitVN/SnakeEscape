import Foundation

/// Đại diện cho một con rắn trên bàn cờ:
/// - `body`: Danh sách các ô liên tiếp theo thứ tự [đầu (index 0) ... đuôi (index n-1)]
/// - `direction`: Hướng di chuyển của đầu
/// - `colorId`: Định danh bảng màu sắc pastel/neon
public struct Snake: Equatable, Hashable, Identifiable, Codable {
    public let id: Int
    public let body: [Cell]
    public let direction: Direction
    public let colorId: Int

    public init(id: Int, body: [Cell], direction: Direction, colorId: Int = 0) {
        precondition(!body.isEmpty, "Snake body cannot be empty")
        self.id = id
        self.body = body
        self.direction = direction
        self.colorId = colorId
    }

    public var head: Cell {
        body[0]
    }

    public var tail: Cell {
        body[body.count - 1]
    }

    public var length: Int {
        body.count
    }

    /// Dịch chuyển rắn 1 bước: Đầu tiến theo hướng `direction`, các đốt thân đi theo đúng vết chân đốt trước.
    public func step() -> Snake {
        let newHead = head + direction
        var newBody = [newHead]
        for i in 0..<(body.count - 1) {
            newBody.append(body[i])
        }
        return Snake(id: id, body: newBody, direction: direction, colorId: colorId)
    }

    /// Kiểm tra xem toàn bộ các đốt của rắn đã rời khỏi bàn cờ [width x height] chưa.
    public func isCompletelyOutside(width: Int, height: Int) -> Bool {
        for cell in body {
            if cell.x >= 0 && cell.x < width && cell.y >= 0 && cell.y < height {
                return false
            }
        }
        return true
    }
}
