package com.snakeescape.logic

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import kotlin.system.measureTimeMillis

class LevelGeneratorTest {

    /**
     * Test quan trọng nhất:
     * Sinh tự động 200 level liên tiếp (Level 1..200) và assert `isSolvable == true`
     * cho toàn bộ 100% các level.
     */
    @Test
    fun `generate 200 levels and assert all are solvable`() {
        val unsolvableLevels = mutableListOf<Int>()
        val totalLevels = 200

        val executionTime = measureTimeMillis {
            for (level in 1..totalLevels) {
                val state = LevelGenerator(level)

                // 1. Kiểm tra level có rắn hợp lệ
                assertFalse(
                    state.snakes.isEmpty(),
                    "Level $level không được rỗng rắn"
                )

                // 2. Kiểm tra không có 2 đốt nào trùng nhau trong cùng 1 rắn
                for (snake in state.snakes) {
                    val uniqueBody = snake.body.toSet()
                    assertEquals(
                        snake.body.size,
                        uniqueBody.size,
                        "Snake ${snake.id} ở level $level có đốt trùng lặp"
                    )
                }

                // 3. Kiểm tra tính giải được của toàn bộ 200 level
                val solvable = SnakeEscapeEngine.isSolvable(state)
                if (!solvable) {
                    unsolvableLevels.add(level)
                }
            }
        }

        println("Thời gian sinh và kiểm định 200 level: ${executionTime}ms (Trung bình: ${executionTime / 200.0}ms/level)")
        assertTrue(
            unsolvableLevels.isEmpty(),
            "Các level không giải được: $unsolvableLevels"
        )
    }

    /**
     * Test kiểm tra độ khó tăng dần theo level:
     * - Kích thước lưới tăng
     * - Số lượng rắn tăng
     * - Áp dụng các ShapeMask (NONE -> DIAMOND -> CIRCLE -> HEART -> STAR -> MAZE)
     */
    @Test
    fun `test difficulty progression and masks`() {
        // Level 5 (Tier 1): nhỏ, không mask
        val lvl5 = LevelGenerator.generate(5)
        assertEquals(5, lvl5.width)
        assertEquals(5, lvl5.height)
        assertTrue(lvl5.lockedCells.isEmpty(), "Level 5 không có ô khóa")
        assertTrue(lvl5.snakes.size in 3..6)

        // Level 25 (Tier 2): lưới 7x7, Mask DIAMOND
        val lvl25 = LevelGenerator.generate(25)
        assertEquals(7, lvl25.width)
        assertEquals(7, lvl25.height)
        assertTrue(lvl25.lockedCells.isNotEmpty(), "Level 25 có Mask Diamond")
        assertTrue(lvl25.snakes.size in 6..12)

        // Level 50 (Tier 3): lưới 8x8, Mask CIRCLE
        val lvl50 = LevelGenerator.generate(50)
        assertEquals(8, lvl50.width)
        assertEquals(8, lvl50.height)
        assertTrue(lvl50.lockedCells.isNotEmpty(), "Level 50 có Mask Circle")
        assertTrue(lvl50.snakes.size in 8..14)

        // Level 100 (Tier 4): lưới 9x9, Mask HEART
        val lvl100 = LevelGenerator.generate(100)
        assertEquals(9, lvl100.width)
        assertEquals(9, lvl100.height)
        assertTrue(lvl100.lockedCells.isNotEmpty(), "Level 100 có Mask Heart")
        assertTrue(lvl100.snakes.size in 11..18)

        // Level 180 (Tier 5): lưới 10x10, Mask MAZE
        val lvl180 = LevelGenerator.generate(180)
        assertEquals(10, lvl180.width)
        assertEquals(10, lvl180.height)
        assertTrue(lvl180.lockedCells.isNotEmpty(), "Level 180 có Mask Maze")
        println("Level 180 snakes count: ${lvl180.snakes.size}")
        assertTrue(lvl180.snakes.size >= 12, "Level 180 phải có ít nhất 12 con rắn (thực tế: ${lvl180.snakes.size})")

        // Thử gợi ý hint() trên level phức tạp
        val hintSnakeId = SnakeEscapeEngine.hint(lvl180)
        assertNotNull(hintSnakeId, "Level 180 phải có ít nhất 1 rắn có thể thoát ngay")
    }

    /**
     * Test chế độ Thử Thách Siêu Cấp (Challenge Mode):
     * - Độ khó vượt bậc (lưới 10x10 -> 12x12, rắn dài, uốn khúc cao, ít tim)
     * - Vẫn bảo đảm 100% có lời giải hợp lệ
     */
    @Test
    fun `test challenge mode levels are solvable`() {
        val sampleChallenges = listOf(1, 10, 20, 35, 50)
        for (c in sampleChallenges) {
            val challengeState = LevelGenerator.generate(level = c, mode = GameMode.CHALLENGE)
            assertTrue(challengeState.snakes.size >= 8, "Challenge $c phải có mật độ rắn cao (thực tế: ${challengeState.snakes.size})")
            val solvable = SnakeEscapeEngine.isSolvable(challengeState)
            assertTrue(solvable, "Challenge $c phải giải được!")
        }
    }
}
