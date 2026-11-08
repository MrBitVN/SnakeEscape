import Foundation

/// Engine xử lý toàn bộ logic và quy tắc di chuyển của Snake Escape:
/// - Thuần Swift (không import UI/AppKit/UIKit)
/// - Kiểm tra va chạm, thoát lưới, tính toán vết chân
/// - DFS giải bài toán (isSolvable) với memoization
/// - Gợi ý nước đi (hint) và hoàn tác (undo)
public enum SnakeEscapeEngine {

    /// Kiểm tra xem con rắn có thể thoát ra khỏi lưới một cách an toàn hay không.
    /// Rắn sẽ tiến theo hướng đầu, thân đi theo đúng quỹ đạo.
    /// Trả về false nếu:
    /// - Va vào ô bị khóa (lockedCells)
    /// - Va vào thân của con rắn khác
    /// - Tự cắn vào thân của chính mình (chưa kịp rời khỏi vị trí đó)
    public static func canEscape(state: GameState, snakeId: Int) -> Bool {
        guard let snake = state.findSnake(id: snakeId) else { return false }

        var otherSnakeCells = Set<Cell>()
        for s in state.snakes where s.id != snakeId {
            for c in s.body {
                otherSnakeCells.insert(c)
            }
        }

        var currentSnake = snake
        let maxSteps = state.width + state.height + snake.length + 2

        for _ in 1...maxSteps {
            currentSnake = currentSnake.step()
            let head = currentSnake.head

            // Nếu đầu vẫn nằm trong lưới chơi
            if state.isInside(head) {
                // 1. Va chạm ô bị khóa
                if state.isLocked(head) {
                    return false
                }

                // 2. Va chạm với rắn khác
                if otherSnakeCells.contains(head) {
                    return false
                }

                // 3. Tự va chạm với thân của chính mình (các đốt từ index 1 trở đi)
                let bodyWithoutHead = currentSnake.body.dropFirst()
                if bodyWithoutHead.contains(head) {
                    return false
                }
            }

            // Nếu toàn bộ rắn đã rời khỏi lưới thành công
            if currentSnake.isCompletelyOutside(width: state.width, height: state.height) {
                return true
            }
        }

        return false
    }

    /// Tạo danh sách các trạng thái của rắn qua từng bước dịch chuyển cho đến khi thoát hoàn toàn.
    public static func simulateSnakeEscapeSteps(snake: Snake, width: Int, height: Int) -> [Snake] {
        var steps = [snake]
        var current = snake
        let maxSteps = width + height + snake.length + 2

        for _ in 1...maxSteps {
            current = current.step()
            steps.append(current)
            if current.isCompletelyOutside(width: width, height: height) {
                break
            }
        }
        return steps
    }

    /// Thao tác chạm vào rắn:
    /// - Nếu rắn thoát được: Rắn rời lưới, giữ nguyên số tim, lưu snapshot vào history.
    /// - Nếu bị chặn: Rắn đứng yên, trừ 1 tim, lưu snapshot vào history (để có thể undo).
    public static func tryMove(state: GameState, snakeId: Int) -> MoveResult {
        guard let snake = state.findSnake(id: snakeId) else {
            return .blocked(state: state, snakeId: snakeId, reason: "Snake with id \(snakeId) not found")
        }

        if state.hearts <= 0 {
            return .blocked(state: state, snakeId: snakeId, reason: "No hearts left")
        }

        let historySnapshot = state.copy(history: [])
        let updatedHistory = state.history + [historySnapshot]

        if canEscape(state: state, snakeId: snakeId) {
            let newState = state.copy(
                snakes: state.snakes.filter { $0.id != snakeId },
                history: updatedHistory
            )
            return .escaped(state: newState, snakeId: snakeId, escapedSnake: snake)
        } else {
            let newState = state.copy(
                hearts: max(0, state.hearts - 1),
                history: updatedHistory
            )
            return .blocked(state: newState, snakeId: snakeId, reason: "Path is blocked by obstacle or other snake")
        }
    }

    /// Hoàn tác nước đi gần nhất.
    public static func undo(state: GameState) -> GameState {
        state.history.last ?? state
    }

    /// Kiểm tra điều kiện thắng: Toàn bộ rắn đã thoát hết và vẫn còn ít nhất 1 tim.
    public static func isWon(state: GameState) -> Bool {
        state.snakes.isEmpty && state.hearts > 0
    }

    /// Kiểm tra level có giải được hay không bằng thuật toán DFS / Backtracking.
    /// Trả về true nếu tồn tại ít nhất 1 chuỗi thoát đưa đến chiến thắng.
    public static func isSolvable(state: GameState) -> Bool {
        var deadEnds = Set<Set<Int>>()

        func solveDfs(current: GameState) -> Bool {
            if isWon(state: current) { return true }
            if current.hearts <= 0 { return false }

            let remainingIds = Set(current.snakes.map { $0.id })
            if deadEnds.contains(remainingIds) { return false }

            let escapableSnakes = current.snakes.filter { canEscape(state: current, snakeId: $0.id) }
            if escapableSnakes.isEmpty {
                deadEnds.insert(remainingIds)
                return false
            }

            // Ưu tiên thử các con rắn sinh sau trước (theo thứ tự LIFO của backward generation)
            for snake in escapableSnakes.sorted(by: { $0.id > $1.id }) {
                let nextState = current.copy(
                    snakes: current.snakes.filter { $0.id != snake.id }
                )
                if solveDfs(current: nextState) {
                    return true
                }
            }

            deadEnds.insert(remainingIds)
            return false
        }

        return solveDfs(current: state)
    }

    /// Gợi ý nước đi tiếp theo:
    /// Trả về ID của con rắn có thể thoát ngay lập tức.
    /// Ưu tiên con rắn nằm trên nhánh đường dẫn tới chiến thắng.
    public static func hint(state: GameState) -> Int? {
        let escapableSnakes = state.snakes.filter { canEscape(state: state, snakeId: $0.id) }
        if escapableSnakes.isEmpty { return nil }

        for snake in escapableSnakes {
            let nextState = state.copy(
                snakes: state.snakes.filter { $0.id != snake.id }
            )
            if isSolvable(state: nextState) {
                return snake.id
            }
        }

        return escapableSnakes.first?.id
    }
}
