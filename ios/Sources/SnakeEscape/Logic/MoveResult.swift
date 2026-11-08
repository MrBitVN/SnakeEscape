import Foundation

/// Kết quả của thao tác tryMove khi người chơi tap vào một con rắn.
public enum MoveResult: Equatable {
    case escaped(state: GameState, snakeId: Int, escapedSnake: Snake)
    case blocked(state: GameState, snakeId: Int, reason: String)
}
