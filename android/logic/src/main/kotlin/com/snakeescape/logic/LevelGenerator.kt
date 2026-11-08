package com.snakeescape.logic

import java.util.Random
import kotlin.math.*

/**
 * Loại mặt nạ hình dạng bàn cờ.
 */
enum class ShapeMask {
    NONE,
    CIRCLE,
    HEART,
    STAR,
    DIAMOND,
    MAZE,
    HELIX,
    SKULL
}

/**
 * Cấu hình tham số độ khó của một level.
 */
data class LevelConfig(
    val level: Int,
    val mode: GameMode,
    val width: Int,
    val height: Int,
    val targetSnakes: Int,
    val minLength: Int,
    val maxLength: Int,
    val bendChance: Double,
    val maskType: ShapeMask,
    val hearts: Int = 3,
    val timeLimitSeconds: Int = 90
) {
    companion object {
        fun fromLevel(level: Int, mode: GameMode = GameMode.CAMPAIGN): LevelConfig {
            return if (mode == GameMode.CAMPAIGN) {
                fromCampaignLevel(level)
            } else {
                fromChallengeLevel(level)
            }
        }

        private fun fromCampaignLevel(level: Int): LevelConfig {
            val mask = when {
                level <= 15 -> ShapeMask.NONE
                level <= 35 -> ShapeMask.DIAMOND
                level <= 70 -> ShapeMask.CIRCLE
                level <= 120 -> ShapeMask.HEART
                level <= 160 -> ShapeMask.STAR
                else -> ShapeMask.MAZE
            }

            val (w, h) = when {
                level <= 5 -> 5 to 5
                level <= 15 -> 6 to 6
                level <= 35 -> 7 to 7
                level <= 70 -> 8 to 8
                level <= 160 -> 9 to 9
                else -> 10 to 10
            }

            val snakes = when {
                level <= 5 -> 3 + (level - 1) / 2          // 3..5
                level <= 15 -> 5 + (level - 6) / 3         // 5..8
                level <= 35 -> 7 + (level - 16) / 7        // 7..9
                level <= 70 -> 9 + (level - 36) / 12       // 9..11
                level <= 120 -> 12 + (level - 71) / 16     // 12..15
                level <= 160 -> 14 + (level - 121) / 14    // 14..16
                else -> 16 + (level - 161) / 10            // 16..19
            }

            val minLen = when {
                level <= 10 -> 2
                else -> 3
            }

            val maxLen = when {
                level <= 5 -> 3
                level <= 20 -> 4
                level <= 60 -> 5
                else -> 6
            }

            val bend = when {
                level <= 5 -> 0.35
                level <= 15 -> 0.50
                level <= 35 -> 0.65
                level <= 70 -> 0.75
                level <= 120 -> 0.82
                else -> 0.86
            }

            val time = when {
                level <= 10 -> 180              // 03:00 (matching Image 2's ~03:00 timer)
                level <= 30 -> 180 + snakes * 3
                level <= 70 -> 190 + snakes * 4
                level <= 120 -> 210 + snakes * 4
                else -> 240 + snakes * 5
            }

            return LevelConfig(
                level = level,
                mode = GameMode.CAMPAIGN,
                width = w,
                height = h,
                targetSnakes = snakes,
                minLength = minLen,
                maxLength = maxLen,
                bendChance = bend,
                maskType = mask,
                hearts = 3,
                timeLimitSeconds = time
            )
        }

        private fun fromChallengeLevel(level: Int): LevelConfig {
            // Chế độ Thử Thách Siêu Cấp (Grandmaster Challenge 1..50)
            // Khó gấp nhiều lần màn thường: lưới lớn (10x10 đến 12x12), 16 đến 30 con rắn,
            // đan xen mật độ cao (> 80%), uốn lượn liên tục, ít tim và giới hạn thời gian gắt gao.
            val mask = when {
                level <= 10 -> ShapeMask.DIAMOND
                level <= 25 -> ShapeMask.HELIX
                level <= 40 -> ShapeMask.SKULL
                else -> ShapeMask.MAZE
            }

            val (w, h) = when {
                level <= 10 -> 10 to 10
                level <= 25 -> 11 to 11
                else -> 12 to 12
            }

            val snakes = when {
                level <= 10 -> 12 + (level - 1) / 2        // 12..16
                level <= 25 -> 16 + (level - 11) / 3       // 16..20
                level <= 40 -> 20 + (level - 26) / 3       // 20..24
                else -> 24 + (level - 41) / 2              // 24..28
            }

            val minLen = 3
            val maxLen = when {
                level <= 10 -> 6
                level <= 25 -> 7
                else -> 8
            }

            val bend = when {
                level <= 10 -> 0.82
                level <= 25 -> 0.88
                level <= 40 -> 0.92
                else -> 0.95
            }

            // Challenge chỉ có 2 tim (cấp 41..50 chỉ có 1 tim duy nhất!)
            val hearts = if (level >= 41) 1 else 2
            val time = when {
                level <= 10 -> 80
                level <= 25 -> 75
                level <= 40 -> 70
                else -> 65
            }

            return LevelConfig(
                level = level,
                mode = GameMode.CHALLENGE,
                width = w,
                height = h,
                targetSnakes = snakes,
                minLength = minLen,
                maxLength = maxLen,
                bendChance = bend,
                maskType = mask,
                hearts = hearts,
                timeLimitSeconds = time
            )
        }
    }
}

/**
 * Bộ sinh màn chơi (Level Generator) bảo đảm 100% LUÔN GIẢI ĐƯỢC.
 *
 * Thuật toán Sinh Ngược (Backward Generation) nâng cao:
 * - Đan xen tầng phụ thuộc sâu (Deep Topological Interlocking):
 *   Khi đặt rắn mới, ưu tiên đặt vắt ngang qua đường thoát thẳng của các con rắn sinh trước đó.
 * - Nhờ vậy tạo thành mê cung logic nhiều tầng: chỉ 1-2 con rắn có thể thoát lúc đầu,
 *   người chơi phải quan sát và tính toán đường đi theo đúng thứ tự logic chặt chẽ.
 */
object LevelGenerator {

    operator fun invoke(level: Int, seed: Long = level.toLong(), mode: GameMode = GameMode.CAMPAIGN): GameState {
        return generate(level, seed, mode)
    }

    fun generate(level: Int, seed: Long = level.toLong(), mode: GameMode = GameMode.CAMPAIGN): GameState {
        return Engine(level, seed, mode).generate()
    }

    private class Engine(
        val level: Int,
        val seed: Long,
        val mode: GameMode
    ) {
        private val random = Random(seed xor if (mode == GameMode.CHALLENGE) 0x5555AAAA5555AAAL else 0L)
        private val config = LevelConfig.fromLevel(level, mode)

        fun generate(): GameState {
            val width = config.width
            val height = config.height
            val lockedCells = generateMask(config.maskType, width, height)

            val placedSnakes = mutableListOf<Snake>()
            val occupiedCells = lockedCells.toMutableSet()

            val maxAttemptsPerSnake = if (mode == GameMode.CHALLENGE) 220 else 180

            for (snakeIdx in 1..config.targetSnakes) {
                var bestCandidate: Snake? = null
                var bestInterlockScore = -1

                for (attempt in 1..maxAttemptsPerSnake) {
                    val freeCells = mutableListOf<Cell>()
                    for (x in 0 until width) {
                        for (y in 0 until height) {
                            val c = Cell(x, y)
                            if (c !in occupiedCells) {
                                freeCells.add(c)
                            }
                        }
                    }
                    if (freeCells.isEmpty()) break

                    val head = freeCells[random.nextInt(freeCells.size)]
                    val directions = Direction.entries.toMutableList().apply { shuffle(random) }

                    for (dir in directions) {
                        if (!isRayClear(head, dir, width, height, occupiedCells)) {
                            continue
                        }

                        val targetLength = if (config.maxLength > config.minLength) {
                            config.minLength + random.nextInt(config.maxLength - config.minLength + 1)
                        } else {
                            config.minLength
                        }

                        val body = growSnakeBody(
                            head = head,
                            headDir = dir,
                            targetLength = targetLength,
                            width = width,
                            height = height,
                            occupiedCells = occupiedCells,
                            bendChance = config.bendChance
                        ) ?: continue

                        val candidateSnake = Snake(
                            id = placedSnakes.size + 1,
                            direction = dir,
                            body = body,
                            colorId = placedSnakes.size % 8
                        )

                        val testState = GameState(
                            width = width,
                            height = height,
                            snakes = placedSnakes + candidateSnake,
                            lockedCells = lockedCells,
                            hearts = config.hearts
                        )

                        if (SnakeEscapeEngine.canEscape(testState, candidateSnake.id)) {
                            val interlockScore = if (placedSnakes.isEmpty()) {
                                0
                            } else {
                                placedSnakes.count { prevSnake ->
                                    candidateSnake.body.any { cell -> isCellOnRay(prevSnake.head, prevSnake.direction, cell) }
                                }
                            }

                            if (interlockScore > bestInterlockScore) {
                                bestInterlockScore = interlockScore
                                bestCandidate = candidateSnake
                                if (interlockScore >= 2) {
                                    break
                                }
                            }
                        }
                    }

                    if (bestCandidate != null && (bestInterlockScore >= 1 || attempt > 30 || placedSnakes.isEmpty())) {
                        break
                    }
                }

                if (bestCandidate != null) {
                    placedSnakes.add(bestCandidate)
                    occupiedCells.addAll(bestCandidate.body)
                }
            }

            return GameState(
                width = width,
                height = height,
                snakes = placedSnakes,
                lockedCells = lockedCells,
                hearts = config.hearts
            )
        }

        private fun isRayClear(
            head: Cell,
            dir: Direction,
            width: Int,
            height: Int,
            occupiedCells: Set<Cell>
        ): Boolean {
            var current = head + dir
            while (current.x in 0 until width && current.y in 0 until height) {
                if (current in occupiedCells) {
                    return false
                }
                current = current + dir
            }
            return true
        }

        private fun isCellOnRay(rayOrigin: Cell, rayDir: Direction, target: Cell): Boolean {
            val dx = target.x - rayOrigin.x
            val dy = target.y - rayOrigin.y
            if (rayDir.dx != 0 && dy == 0) {
                return (dx * rayDir.dx) > 0
            }
            if (rayDir.dy != 0 && dx == 0) {
                return (dy * rayDir.dy) > 0
            }
            return false
        }

        private fun growSnakeBody(
            head: Cell,
            headDir: Direction,
            targetLength: Int,
            width: Int,
            height: Int,
            occupiedCells: Set<Cell>,
            bendChance: Double
        ): List<Cell>? {
            val body = mutableListOf(head)
            val bodySet = mutableSetOf(head)
            var current = head
            var currentGrowthDir = headDir.opposite()

            for (segment in 1 until targetLength) {
                val candidates = mutableListOf<Pair<Cell, Direction>>()

                for (dir in Direction.entries) {
                    val next = current + dir
                    if (next.x in 0 until width && next.y in 0 until height) {
                        if (next !in occupiedCells && next !in bodySet) {
                            if (segment == 1 && next == head + headDir) {
                                continue
                            }
                            candidates.add(next to dir)
                        }
                    }
                }

                if (candidates.isEmpty()) {
                    if (body.size >= config.minLength) {
                        return body
                    }
                    return null
                }

                val straightCandidate = candidates.find { it.second == currentGrowthDir }
                val chosen = if (straightCandidate != null && random.nextDouble() >= bendChance) {
                    straightCandidate
                } else {
                    candidates[random.nextInt(candidates.size)]
                }

                current = chosen.first
                currentGrowthDir = chosen.second
                body.add(current)
                bodySet.add(current)
            }

            return body
        }

        private fun generateMask(maskType: ShapeMask, width: Int, height: Int): Set<Cell> {
            val locked = mutableSetOf<Cell>()
            val cx = (width - 1) / 2.0
            val cy = (height - 1) / 2.0
            val radius = min(width, height) / 2.0

            when (maskType) {
                ShapeMask.NONE -> {}
                ShapeMask.CIRCLE -> {
                    for (x in 0 until width) {
                        for (y in 0 until height) {
                            val dx = x - cx
                            val dy = y - cy
                            val distSq = dx * dx + dy * dy
                            if (abs(dx) <= 0.6 || abs(dy) <= 0.6) continue
                            if (distSq > (radius + 0.1) * (radius + 0.1)) {
                                locked.add(Cell(x, y))
                            }
                        }
                    }
                }
                ShapeMask.HEART -> {
                    val midX = width / 2
                    if (height >= 6) {
                        locked.add(Cell(midX, 0))
                        if (width % 2 == 0) locked.add(Cell(midX - 1, 0))
                    }
                    locked.add(Cell(0, 0))
                    locked.add(Cell(width - 1, 0))

                    val bottomCutRows = max(1, height / 4)
                    for (r in 0 until bottomCutRows) {
                        val y = height - 1 - r
                        val cutCols = bottomCutRows - r
                        for (x in 0 until cutCols) {
                            locked.add(Cell(x, y))
                            locked.add(Cell(width - 1 - x, y))
                        }
                    }
                }
                ShapeMask.STAR -> {
                    val cornerCut = max(1, min(width, height) / 3)
                    for (x in 0 until cornerCut) {
                        for (y in 0 until cornerCut) {
                            if (x + y < cornerCut + 1) {
                                locked.add(Cell(x, y))
                                locked.add(Cell(width - 1 - x, y))
                                locked.add(Cell(x, height - 1 - y))
                                locked.add(Cell(width - 1 - x, height - 1 - y))
                            }
                        }
                    }
                }
                ShapeMask.DIAMOND -> {
                    // Cắt 4 góc chéo tạo hình thoi gọn gàng, giữ lòng bàn cờ rộng
                    for (x in 0 until width) {
                        for (y in 0 until height) {
                            val distManhattan = abs(x - cx) + abs(y - cy)
                            if (abs(x - cx) <= 0.6 || abs(y - cy) <= 0.6) continue
                            if (distManhattan > radius + 1.8) {
                                locked.add(Cell(x, y))
                            }
                        }
                    }
                }
                ShapeMask.MAZE -> {
                    // Mê cung đối xứng: Các trụ đá cố định phân luồng di chuyển
                    for (x in 2 until width - 2 step 2) {
                        for (y in 2 until height - 2 step 2) {
                            if (abs(x - cx) > 0.8 || abs(y - cy) > 0.8) {
                                locked.add(Cell(x, y))
                            }
                        }
                    }
                }
                ShapeMask.HELIX -> {
                    // Xoắn ốc 4 góc bậc thang
                    val stepCount = max(1, min(width, height) / 4)
                    for (s in 0 until stepCount) {
                        locked.add(Cell(s, s + 1))
                        locked.add(Cell(width - 1 - s, height - 2 - s))
                        locked.add(Cell(width - 2 - s, s))
                        locked.add(Cell(s + 1, height - 1 - s))
                    }
                }
                ShapeMask.SKULL -> {
                    // Khóa 2 góc dưới và 2 hốc mắt
                    locked.add(Cell(0, height - 1))
                    locked.add(Cell(1, height - 1))
                    locked.add(Cell(width - 1, height - 1))
                    locked.add(Cell(width - 2, height - 1))
                    if (width >= 8 && height >= 8) {
                        val eyeY = (height * 0.35).toInt()
                        val eyeX1 = (width * 0.28).toInt()
                        val eyeX2 = width - 1 - eyeX1
                        locked.add(Cell(eyeX1, eyeY))
                        locked.add(Cell(eyeX2, eyeY))
                    }
                }
            }

            return locked
        }
    }
}

fun generateLevel(level: Int, seed: Long = level.toLong(), mode: GameMode = GameMode.CAMPAIGN): GameState =
    LevelGenerator(level, seed, mode)
