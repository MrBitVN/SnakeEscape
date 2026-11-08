package com.snakeescape.logic

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class SnakeEscapeTest {

    /**
     * Test 1: Rắn thoát thẳng ra khỏi lưới an toàn
     */
    @Test
    fun `test snake escapes straight`() {
        // Lưới 5x5, Rắn 1 nằm ở hàng y=2, đầu tại (2, 2), hướng RIGHT
        val snake1 = Snake(
            id = 1,
            direction = Direction.RIGHT,
            body = listOf(Cell(2, 2), Cell(1, 2), Cell(0, 2))
        )
        val state = GameState(
            width = 5,
            height = 5,
            snakes = listOf(snake1),
            hearts = 3
        )

        // Kiểm tra canEscape
        assertTrue(SnakeEscapeEngine.canEscape(state, snake1.id))

        // Thực hiện tryMove
        val result = SnakeEscapeEngine.tryMove(state, snake1.id)
        assertTrue(result is MoveResult.Escaped, "Rắn phải thoát thành công")

        val escapedResult = result as MoveResult.Escaped
        assertEquals(snake1.id, escapedResult.snakeId)
        assertTrue(escapedResult.state.snakes.isEmpty(), "Bàn cờ không còn rắn nào")
        assertEquals(3, escapedResult.state.hearts, "Số tim giữ nguyên 3")
        assertTrue(SnakeEscapeEngine.isWon(escapedResult.state), "Đạt điều kiện thắng")
    }

    /**
     * Test 2: Rắn bị chặn bởi rắn khác hoặc ô khóa
     */
    @Test
    fun `test snake is blocked by other snake`() {
        // Rắn 1: đầu tại (1, 2), hướng RIGHT (chuẩn bị đi vào x=2, y=2)
        val snake1 = Snake(
            id = 1,
            direction = Direction.RIGHT,
            body = listOf(Cell(1, 2), Cell(0, 2))
        )
        // Rắn 2: nằm chắn dọc ở x=2, từ y=1 đến y=3
        val snake2 = Snake(
            id = 2,
            direction = Direction.DOWN,
            body = listOf(Cell(2, 3), Cell(2, 2), Cell(2, 1))
        )

        val state = GameState(
            width = 5,
            height = 5,
            snakes = listOf(snake1, snake2),
            hearts = 3
        )

        // Rắn 1 bị Rắn 2 chặn
        assertFalse(SnakeEscapeEngine.canEscape(state, snake1.id))

        // Khi tap Rắn 1: thất bại, mất 1 tim
        val result = SnakeEscapeEngine.tryMove(state, snake1.id)
        assertTrue(result is MoveResult.Blocked, "Nước đi phải bị Blocked")

        val blockedResult = result as MoveResult.Blocked
        assertEquals(2, blockedResult.state.hearts, "Số tim phải giảm từ 3 xuống 2")
        assertEquals(2, blockedResult.state.snakes.size, "Hai con rắn vẫn còn trên bàn cờ")
    }

    @Test
    fun `test snake is blocked by locked cell`() {
        val snake = Snake(
            id = 1,
            direction = Direction.UP,
            body = listOf(Cell(2, 2), Cell(2, 3))
        )
        // Ô (2, 1) phía trước đầu bị LOCKED
        val state = GameState(
            width = 5,
            height = 5,
            snakes = listOf(snake),
            lockedCells = setOf(Cell(2, 1)),
            hearts = 3
        )

        assertFalse(SnakeEscapeEngine.canEscape(state, snake.id))
        val result = SnakeEscapeEngine.tryMove(state, snake.id)
        assertTrue(result is MoveResult.Blocked)
        assertEquals(2, result.state.hearts)
    }

    /**
     * Test 3: Thân rắn đi theo đúng quỹ đạo của đầu (Body trail following)
     */
    @Test
    fun `test snake body follows exact head path`() {
        // Tạo rắn uốn khúc chữ L:
        // Đầu tại (2, 2) hướng UP
        // Đốt 1 tại (2, 3)
        // Đốt 2 (đuôi) tại (1, 3)
        val initialSnake = Snake(
            id = 1,
            direction = Direction.UP,
            body = listOf(Cell(2, 2), Cell(2, 3), Cell(1, 3))
        )

        // Bước 1:
        // - Đầu mới: (2, 2) + UP = (2, 1)
        // - Đốt 1 mới: vị trí cũ của đầu = (2, 2)
        // - Đốt 2 mới: vị trí cũ của đốt 1 = (2, 3)
        val step1 = initialSnake.step()
        assertEquals(Cell(2, 1), step1.body[0], "Đầu phải tiến lên (2, 1)")
        assertEquals(Cell(2, 2), step1.body[1], "Đốt 1 phải chiếm vị trí cũ của đầu (2, 2)")
        assertEquals(Cell(2, 3), step1.body[2], "Đốt 2 phải chiếm vị trí cũ của đốt 1 (2, 3)")

        // Bước 2:
        // - Đầu mới: (2, 1) + UP = (2, 0)
        // - Đốt 1 mới: (2, 1)
        // - Đốt 2 mới: (2, 2)
        val step2 = step1.step()
        assertEquals(Cell(2, 0), step2.body[0], "Đầu phải tiến lên (2, 0)")
        assertEquals(Cell(2, 1), step2.body[1], "Đốt 1 phải lên (2, 1)")
        assertEquals(Cell(2, 2), step2.body[2], "Đốt 2 phải lên (2, 2)")

        // Mô phỏng trọn vẹn quá trình trườn ra ngoài
        val steps = SnakeEscapeEngine.simulateSnakeEscapeSteps(initialSnake, width = 5, height = 5)
        assertTrue(steps.last().isCompletelyOutside(5, 5), "Bước cuối cùng rắn phải ra hoàn toàn khỏi bàn cờ")

        // Kiểm tra tính liên tục của quỹ đạo: Ở mọi bước i > 0, đốt k của bước i
        // luôn trùng với đốt k-1 của bước i-1
        for (i in 1 until steps.size) {
            val prevSnake = steps[i - 1]
            val currSnake = steps[i]
            for (k in 1 until initialSnake.length) {
                assertEquals(
                    prevSnake.body[k - 1],
                    currSnake.body[k],
                    "Đốt $k ở bước $i phải đúng bằng đốt ${k - 1} ở bước ${i - 1}"
                )
            }
        }
    }

    /**
     * Test 4: Chức năng Undo (Hoàn tác nước đi)
     */
    @Test
    fun `test undo moves`() {
        val snake1 = Snake(1, Direction.RIGHT, listOf(Cell(1, 1), Cell(0, 1)))
        val snake2 = Snake(2, Direction.DOWN, listOf(Cell(3, 1), Cell(3, 0)))

        var state = GameState(
            width = 5,
            height = 5,
            snakes = listOf(snake1, snake2),
            hearts = 3
        )

        // 1. Cho Rắn 2 thoát thành công
        val escapeResult = SnakeEscapeEngine.tryMove(state, snake2.id)
        assertTrue(escapeResult.isEscaped)
        state = escapeResult.state
        assertEquals(1, state.snakes.size, "Còn 1 rắn trên bàn cờ")

        // Undo: Khôi phục lại Rắn 2
        state = SnakeEscapeEngine.undo(state)
        assertEquals(2, state.snakes.size, "Rắn 2 đã được khôi phục")
        assertNotNull(state.findSnake(snake2.id))

        // 2. Thử nước đi va chạm: Cho Rắn 1 bị chặn
        val blockedStateWithWall = state.copy(lockedCells = setOf(Cell(2, 1)))
        val blockedResult = SnakeEscapeEngine.tryMove(blockedStateWithWall, snake1.id)
        assertTrue(blockedResult.isBlocked)
        assertEquals(2, blockedResult.state.hearts, "Mất 1 tim còn 2")

        // Undo: Khôi phục lại tim
        val restoredState = SnakeEscapeEngine.undo(blockedResult.state)
        assertEquals(3, restoredState.hearts, "Tim đã được phục hồi về 3")
    }

    /**
     * Test 5: isSolvable & Hint với chuỗi phụ thuộc (Topological dependency)
     */
    @Test
    fun `test isSolvable and hint`() {
        // Chuỗi: Rắn 1 bị Rắn 2 chặn; Rắn 2 tự do thoát
        // Rắn 1: đầu tại (1, 2) hướng RIGHT, cần đi qua (2, 2)
        val snake1 = Snake(1, Direction.RIGHT, listOf(Cell(1, 2), Cell(0, 2)))
        // Rắn 2: chiếm (2, 2), đầu tại (2, 3) hướng DOWN (thoát tự do)
        val snake2 = Snake(2, Direction.DOWN, listOf(Cell(2, 3), Cell(2, 2)))

        val state = GameState(
            width = 4,
            height = 4,
            snakes = listOf(snake1, snake2),
            hearts = 3
        )

        // Level này giải được
        assertTrue(SnakeEscapeEngine.isSolvable(state), "Level phải giải được")

        // Hint phải gợi ý Rắn 2 (rắn đang có đường thoát)
        val suggestedId = SnakeEscapeEngine.hint(state)
        assertEquals(2, suggestedId, "Hint phải trả về Rắn 2")

        // Thoát Rắn 2
        val step1 = SnakeEscapeEngine.tryMove(state, snake2.id)
        assertTrue(step1.isEscaped)

        // Sau khi Rắn 2 thoát, Rắn 1 giải phóng đường và thoát được
        assertTrue(SnakeEscapeEngine.canEscape(step1.state, snake1.id))
        val step2 = SnakeEscapeEngine.tryMove(step1.state, snake1.id)
        assertTrue(step2.isEscaped)
        assertTrue(SnakeEscapeEngine.isWon(step2.state))
    }

    /**
     * Test 6: Deadlock pinwheel cycle (Không thể giải được)
     */
    @Test
    fun `test unsolvable deadlock pinwheel`() {
        // 4 con rắn khóa vòng tròn lẫn nhau:
        // 1 chặn 2, 2 chặn 3, 3 chặn 4, 4 chặn 1
        val s1 = Snake(1, Direction.RIGHT, listOf(Cell(1, 1), Cell(0, 1))) // Muốn đi qua (2, 1) - bị s2 chặn
        val s2 = Snake(2, Direction.DOWN, listOf(Cell(2, 1), Cell(2, 0)))  // Muốn đi qua (2, 2) - bị s3 chặn
        val s3 = Snake(3, Direction.LEFT, listOf(Cell(2, 2), Cell(3, 2)))  // Muốn đi qua (1, 2) - bị s4 chặn
        val s4 = Snake(4, Direction.UP, listOf(Cell(1, 2), Cell(1, 3)))    // Muốn đi qua (1, 1) - bị s1 chặn

        val deadlockState = GameState(
            width = 4,
            height = 4,
            snakes = listOf(s1, s2, s3, s4),
            hearts = 3
        )

        assertFalse(SnakeEscapeEngine.isSolvable(deadlockState), "Thế cờ deadlock không thể giải được")
        assertNull(SnakeEscapeEngine.hint(deadlockState), "Không có gợi ý vì không ai thoát được")
    }
}
