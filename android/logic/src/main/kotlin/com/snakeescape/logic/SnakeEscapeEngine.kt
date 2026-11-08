package com.snakeescape.logic

object SnakeEscapeEngine {

    /**
     * Kiểm tra xem con rắn có thể trượt thoát ra khỏi lưới một cách an toàn hay không.
     * Rắn luôn trượt thẳng tiến theo hướng tự nhiên của đầu (từ đốt cổ tới đầu),
     * thân rắn uốn lượn đi theo đúng dấu chân của đầu.
     * Trả về false nếu va chạm với:
     * - Ô bị khóa (lockedCells)
     * - Thân của con rắn khác (otherSnakeCells)
     * - Tự cắn vào thân của chính mình
     */
    fun canEscape(state: GameState, snakeId: Int): Boolean {
        val snake = state.findSnake(snakeId) ?: return false
        val otherSnakeCells = state.snakes
            .filter { it.id != snakeId }
            .flatMap { it.body }
            .toSet()

        var currentSnake = snake
        val maxSteps = state.width + state.height + snake.length + 2

        for (step in 1..maxSteps) {
            currentSnake = currentSnake.step()
            val head = currentSnake.head

            // Nếu đầu vẫn nằm trong lưới chơi
            if (state.isInside(head)) {
                // 1. Va chạm ô bị khóa
                if (state.isLocked(head)) return false

                // 2. Va chạm với rắn khác
                if (head in otherSnakeCells) return false

                // 3. Tự va chạm với thân của chính mình
                if (currentSnake.body.drop(1).contains(head)) return false
            }

            // Nếu toàn bộ rắn đã rời khỏi lưới thành công
            if (currentSnake.isCompletelyOutside(state.width, state.height)) {
                return true
            }
        }

        return false
    }

    /**
     * Tạo danh sách các trạng thái của rắn qua từng bước dịch chuyển cho đến khi thoát hoàn toàn.
     * Thân rắn uốn lượn đi theo đúng dấu chân của đầu.
     */
    fun simulateSnakeEscapeSteps(snake: Snake, width: Int, height: Int): List<Snake> {
        val steps = mutableListOf(snake)
        var current = snake
        val maxSteps = width + height + snake.length + 2

        for (i in 1..maxSteps) {
            current = current.step()
            steps.add(current)
            if (current.isCompletelyOutside(width, height)) {
                break
            }
        }
        return steps
    }

    /**
     * Thao tác chạm vào rắn:
     * - Nếu rắn thoát được: Rắn rời lưới, giữ nguyên số tim, lưu snapshot vào history.
     * - Nếu bị chặn: Rắn đứng yên, trừ 1 tim, lưu snapshot vào history (để có thể undo lại tim).
     */
    fun tryMove(state: GameState, snakeId: Int): MoveResult {
        val snake = state.findSnake(snakeId)
            ?: return MoveResult.Blocked(state, snakeId, "Snake with id $snakeId not found")

        if (state.hearts <= 0) {
            return MoveResult.Blocked(state, snakeId, "No hearts left")
        }

        val historySnapshot = state.copy(history = emptyList())
        val updatedHistory = state.history + historySnapshot

        return if (canEscape(state, snakeId)) {
            val newState = state.copy(
                snakes = state.snakes.filter { it.id != snakeId },
                history = updatedHistory
            )
            MoveResult.Escaped(newState, snakeId, snake)
        } else {
            val newState = state.copy(
                hearts = maxOf(0, state.hearts - 1),
                history = updatedHistory
            )
            MoveResult.Blocked(newState, snakeId, "Path is blocked by obstacle or other snake")
        }
    }

    /**
     * Hoàn tác nước đi gần nhất.
     * Phục hồi lại trạng thái trước đó (con rắn vừa thoát hoặc tim vừa mất).
     */
    fun undo(state: GameState): GameState {
        return state.history.lastOrNull() ?: state
    }

    /**
     * Kiểm tra điều kiện thắng:
     * Toàn bộ rắn đã thoát hết và vẫn còn ít nhất 1 tim.
     */
    fun isWon(state: GameState): Boolean {
        return state.snakes.isEmpty() && state.hearts > 0
    }

    /**
     * Kiểm tra level có giải được hay không bằng thuật toán DFS / Backtracking.
     * Trả về true nếu tồn tại ít nhất 1 chuỗi thoát đưa đến chiến thắng.
     */
    fun isSolvable(state: GameState): Boolean {
        val deadEnds = mutableSetOf<Set<Int>>()

        fun solveDfs(current: GameState): Boolean {
            if (isWon(current)) return true
            if (current.hearts <= 0) return false

            val remainingIds = current.snakes.map { it.id }.toSet()
            if (remainingIds in deadEnds) return false

            val escapableSnakes = current.snakes.filter { canEscape(current, it.id) }
            if (escapableSnakes.isEmpty()) {
                deadEnds.add(remainingIds)
                return false
            }

            // Ưu tiên thử các con rắn sinh sau trước (theo thứ tự LIFO của backward generation)
            // để DFS tìm ngay ra nghiệm nhanh nhất mà không cần quay lui nhiều.
            for (snake in escapableSnakes.sortedByDescending { it.id }) {
                val nextState = current.copy(
                    snakes = current.snakes.filter { it.id != snake.id }
                )
                if (solveDfs(nextState)) {
                    return true
                }
            }

            deadEnds.add(remainingIds)
            return false
        }

        return solveDfs(state)
    }

    /**
     * Gợi ý nước đi tiếp theo:
     * Trả về ID của con rắn có thể thoát ngay lập tức.
     * Ưu tiên con rắn nằm trên nhánh đường dẫn tới chiến thắng (nếu level giải được).
     */
    fun hint(state: GameState): Int? {
        val escapableSnakes = state.snakes.filter { canEscape(state, it.id) }
        if (escapableSnakes.isEmpty()) return null

        // Ưu tiên chọn rắn dẫn tới chiến thắng
        for (snake in escapableSnakes) {
            val nextState = state.copy(
                snakes = state.snakes.filter { it.id != snake.id }
            )
            if (isSolvable(nextState)) {
                return snake.id
            }
        }

        // Nếu không có nhánh thắng (đang ở thế kẹt), trả về bất kỳ con nào thoát được ngay
        return escapableSnakes.first().id
    }
}
