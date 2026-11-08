package com.snakeescape.logic

/**
 * Chế độ chơi của Snake Escape:
 * - CAMPAIGN: Chế độ cốt truyện / chiến dịch chuẩn gồm 200 màn với độ khó tăng dần.
 * - CHALLENGE: Chế độ thử thách đại kiện tướng (Grandmaster Challenge) với lưới lớn, mật độ rắn dày đặc,
 *              chuỗi phụ thuộc đan xen sâu, ít tim hơn và đòi hỏi tư duy logic nhiều bước.
 */
enum class GameMode {
    CAMPAIGN,
    CHALLENGE
}

/**
 * Tọa độ một ô trên lưới chơi.
 */
data class Cell(val x: Int, val y: Int) {
    operator fun plus(dir: Direction): Cell = Cell(x + dir.dx, y + dir.dy)
    operator fun minus(dir: Direction): Cell = Cell(x - dir.dx, y - dir.dy)

    fun directionTo(other: Cell): Direction? = when {
        other.x == x && other.y == y - 1 -> Direction.UP
        other.x == x && other.y == y + 1 -> Direction.DOWN
        other.x == x - 1 && other.y == y -> Direction.LEFT
        other.x == x + 1 && other.y == y -> Direction.RIGHT
        else -> null
    }
}

/**
 * Hướng di chuyển của đầu rắn.
 * Hệ tọa độ 2D:
 *  - (0, 0) ở góc trên bên trái
 *  - UP: y giảm (-1)
 *  - DOWN: y tăng (+1)
 *  - LEFT: x giảm (-1)
 *  - RIGHT: x tăng (+1)
 */
enum class Direction(val dx: Int, val dy: Int) {
    UP(0, -1),
    DOWN(0, 1),
    LEFT(-1, 0),
    RIGHT(1, 0);

    fun opposite(): Direction = when (this) {
        UP -> DOWN
        DOWN -> UP
        LEFT -> RIGHT
        RIGHT -> LEFT
    }
}

/**
 * Đại diện cho 1 con rắn trên bàn cờ.
 * @param id Định danh duy nhất của con rắn
 * @param direction Hướng đầu rắn đang nhìn
 * @param body Danh sách các đốt liên tiếp. body[0] luôn là ĐẦU (Head), body.last() là ĐUÔI (Tail)
 * @param colorId Mã màu/skin của rắn
 */
data class Snake(
    val id: Int,
    val direction: Direction,
    val body: List<Cell>,
    val colorId: Int = 0
) {
    init {
        require(body.isNotEmpty()) { "Snake body must not be empty" }
    }

    val head: Cell get() = body.first()
    val tail: Cell get() = body.last()
    val length: Int get() = body.size

    /**
     * Dịch chuyển rắn 1 bước:
     * - Đầu tiến 1 ô theo hướng [direction].
     * - Thân đi theo đúng quỹ đạo của đầu (mỗi đốt k lấy vị trí cũ của đốt k-1).
     */
    fun step(): Snake {
        val newHead = head + direction
        val newBody = ArrayList<Cell>(body.size).apply {
            add(newHead)
            for (i in 0 until body.size - 1) {
                add(body[i])
            }
        }
        return copy(body = newBody)
    }

    /**
     * Kiểm tra xem toàn bộ các đốt của rắn đã nằm ngoài biên lưới chưa.
     */
    fun isCompletelyOutside(width: Int, height: Int): Boolean {
        return body.none { it.x in 0 until width && it.y in 0 until height }
    }
}

/**
 * Trạng thái bàn cờ và toàn bộ màn chơi.
 * @param width Chiều rộng lưới
 * @param height Chiều cao lưới
 * @param snakes Danh sách các con rắn hiện có trên bàn cờ
 * @param lockedCells Tập các ô bị khóa vĩnh viễn (tạo hình dạng sao, tròn, chướng ngại)
 * @param hearts Số tim còn lại (mặc định 3, trong Challenge có thể là 1 hoặc 2)
 * @param history Ngăn xếp lịch sử các GameState trước đó dùng cho chức năng Undo
 */
data class GameState(
    val width: Int,
    val height: Int,
    val snakes: List<Snake>,
    val lockedCells: Set<Cell> = emptySet(),
    val hearts: Int = 3,
    val history: List<GameState> = emptyList()
) {
    fun isInside(cell: Cell): Boolean = cell.x in 0 until width && cell.y in 0 until height

    fun findSnake(snakeId: Int): Snake? = snakes.find { it.id == snakeId }

    fun isLocked(cell: Cell): Boolean = cell in lockedCells

    fun isWon(): Boolean = snakes.isEmpty() && hearts > 0

    fun isGameOver(): Boolean = hearts <= 0

    fun undo(): GameState = history.lastOrNull() ?: this
}

/**
 * Kết quả sau khi thử di chuyển một con rắn.
 */
sealed class MoveResult {
    abstract val state: GameState
    abstract val snakeId: Int

    val isEscaped: Boolean get() = this is Escaped
    val isBlocked: Boolean get() = this is Blocked

    data class Escaped(
        override val state: GameState,
        override val snakeId: Int,
        val escapedSnake: Snake
    ) : MoveResult()

    data class Blocked(
        override val state: GameState,
        override val snakeId: Int,
        val reason: String
    ) : MoveResult()
}
