import Foundation

/// Trạng thái toàn cục của màn chơi tại một thời điểm:
/// - Kích thước lưới W x H
/// - Danh sách các con rắn hiện có trên bàn cờ
/// - Tập hợp các ô bị khóa (obstacle cutouts tạo hình)
/// - Số tim còn lại (mặc định 3)
/// - Lịch sử các bước trước (phục vụ chức năng Undo)
public struct GameState: Equatable, Codable {
    public let width: Int
    public let height: Int
    public let snakes: [Snake]
    public let lockedCells: Set<Cell>
    public let hearts: Int
    public let history: [GameState]

    public init(
        width: Int,
        height: Int,
        snakes: [Snake],
        lockedCells: Set<Cell> = [],
        hearts: Int = 3,
        history: [GameState] = []
    ) {
        self.width = width
        self.height = height
        self.snakes = snakes
        self.lockedCells = lockedCells
        self.hearts = hearts
        self.history = history
    }

    public func isInside(_ cell: Cell) -> Bool {
        cell.x >= 0 && cell.x < width && cell.y >= 0 && cell.y < height
    }

    public func isLocked(_ cell: Cell) -> Bool {
        lockedCells.contains(cell)
    }

    public func findSnake(id: Int) -> Snake? {
        snakes.first { $0.id == id }
    }

    public func snakeAt(_ cell: Cell) -> Snake? {
        snakes.first { $0.body.contains(cell) }
    }

    public func copy(
        width: Int? = nil,
        height: Int? = nil,
        snakes: [Snake]? = nil,
        lockedCells: Set<Cell>? = nil,
        hearts: Int? = nil,
        history: [GameState]? = nil
    ) -> GameState {
        GameState(
            width: width ?? self.width,
            height: height ?? self.height,
            snakes: snakes ?? self.snakes,
            lockedCells: lockedCells ?? self.lockedCells,
            hearts: hearts ?? self.hearts,
            history: history ?? self.history
        )
    }
}
