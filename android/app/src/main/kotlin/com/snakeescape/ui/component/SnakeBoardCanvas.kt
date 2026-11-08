package com.snakeescape.ui.component

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import com.snakeescape.logic.*
import com.snakeescape.ui.theme.GameColorScheme
import com.snakeescape.ui.theme.SnakeColors
import kotlin.math.*

/**
 * Trạng thái của con rắn đang thực hiện hoạt hình trượt thoát ra khỏi bàn cờ.
 */
data class EscapingSnakeAnim(
    val snake: Snake,
    val polyline: List<Offset>,
    val animatable: Animatable<Float, AnimationVector1D>
)

/**
 * Jetpack Compose Canvas hiển thị bàn cờ Snake Escape cao cấp:
 * - Đồ họa 3D mềm mại: Bàn cờ viền phát sáng, các ô dập nổi xúc giác (tactile bevel).
 * - Rắn 3D sống động: Đổ bóng mềm nhiều lớp, dải sống lưng phản quang (spine highlight),
 *   đuôi vuốt nhọn tự nhiên (tapered tail), chi tiết đầu sống động (lỗ mũi, má hồng, mắt có hồn).
 * - Hiệu ứng va chạm: Sóng xung kích lan tỏa (shockwave ripple) + dấu X đỏ cảnh báo.
 * - Hỗ trợ Pinch-to-zoom và Pan trên lưới lớn, duy trì 60 FPS mượt mà.
 */
@Composable
fun SnakeBoardCanvas(
    state: GameState,
    onSnakeTapped: (snakeId: Int) -> Unit,
    colors: GameColorScheme,
    modifier: Modifier = Modifier,
    gameMode: GameMode = GameMode.CAMPAIGN,
    shakingSnakeId: Int? = null,
    shakeProgressProvider: () -> Float = { 0f },
    blockedCell: Cell? = null,
    blockedAlphaProvider: () -> Float = { 0f },
    hintSnakeId: Int? = null,
    isGridVisionActive: Boolean = false,
    escapingSnake: EscapingSnakeAnim? = null
) {
    // Zoom & Pan state
    var scale by remember { mutableFloatStateOf(1f) }
    var panOffset by remember { mutableStateOf(Offset.Zero) }

    fun resetTransform() {
        scale = 1f
        panOffset = Offset.Zero
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .pointerInput(state.width, state.height, scale, panOffset) {
                detectTransformGestures { _, pan, zoom, _ ->
                    scale = (scale * zoom).coerceIn(0.65f, 3.8f)
                    panOffset += pan
                }
            }
            .pointerInput(state.width, state.height, scale, panOffset, state.snakes) {
                detectTapGestures(
                    onDoubleTap = { resetTransform() },
                    onTap = { tapOffset ->
                        val padding = 24f
                        val baseCellSize = min(
                            (size.width - 2 * padding) / state.width,
                            (size.height - 2 * padding) / state.height
                        )
                        val cellSize = baseCellSize * scale
                        val gridW = cellSize * state.width
                        val gridH = cellSize * state.height
                        val originX = (size.width - gridW) / 2f + panOffset.x
                        val originY = (size.height - gridH) / 2f + panOffset.y

                        val col = ((tapOffset.x - originX) / cellSize).toInt()
                        val row = ((tapOffset.y - originY) / cellSize).toInt()

                        if (col in 0 until state.width && row in 0 until state.height) {
                            val clickedCell = Cell(col, row)
                            val clickedSnake = state.snakes.find { it.body.contains(clickedCell) }
                            if (clickedSnake != null) {
                                onSnakeTapped(clickedSnake.id)
                            }
                        }
                    }
                )
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val padding = 24f
            val baseCellSize = min(
                (size.width - 2 * padding) / state.width,
                (size.height - 2 * padding) / state.height
            )
            val cellSize = baseCellSize * scale
            val gridW = cellSize * state.width
            val gridH = cellSize * state.height
            val originX = (size.width - gridW) / 2f + panOffset.x
            val originY = (size.height - gridH) / 2f + panOffset.y

            val shakeProgress = shakeProgressProvider()
            val blockedAlpha = blockedAlphaProvider()

            // 0. Viền khung bao quanh toàn bộ bàn cờ (Board Ambient Frame)
            val frameMargin = cellSize * 0.18f
            drawRoundRect(
                color = colors.boardBackground,
                topLeft = Offset(originX - frameMargin, originY - frameMargin),
                size = Size(gridW + 2 * frameMargin, gridH + 2 * frameMargin),
                cornerRadius = CornerRadius(cellSize * 0.35f)
            )
            // Viền phát sáng theo chế độ chơi
            val borderGlowColor = if (gameMode == GameMode.CHALLENGE) {
                SnakeColors.ChallengeFlame.copy(alpha = 0.5f)
            } else {
                Color(0xFF10B981).copy(alpha = 0.35f)
            }
            drawRoundRect(
                color = borderGlowColor,
                topLeft = Offset(originX - frameMargin, originY - frameMargin),
                size = Size(gridW + 2 * frameMargin, gridH + 2 * frameMargin),
                cornerRadius = CornerRadius(cellSize * 0.35f),
                style = Stroke(width = 2.5f)
            )

            // 1. Vẽ các ô của bàn cờ (Grid Tiles dập nổi tactile)
            for (col in 0 until state.width) {
                for (row in 0 until state.height) {
                    val cell = Cell(col, row)
                    val cellLeft = originX + col * cellSize
                    val cellTop = originY + row * cellSize
                    val isLocked = state.isLocked(cell)
                    val tileMargin = cellSize * 0.05f
                    val tileSize = cellSize - 2 * tileMargin
                    val cornerRadius = CornerRadius(cellSize * 0.22f)

                    if (isLocked) {
                        // Ô chướng ngại vật / Cutout: Phong cách Obsidian huyền bí có viền kim loại
                        drawRoundRect(
                            color = colors.cellLocked,
                            topLeft = Offset(cellLeft + tileMargin, cellTop + tileMargin),
                            size = Size(tileSize, tileSize),
                            cornerRadius = cornerRadius
                        )
                        drawRoundRect(
                            color = colors.cellLockedBorder,
                            topLeft = Offset(cellLeft + tileMargin, cellTop + tileMargin),
                            size = Size(tileSize, tileSize),
                            cornerRadius = cornerRadius,
                            style = Stroke(width = 1.8f)
                        )
                        // Biểu tượng kim cương ở tâm ô bị khóa
                        val midX = cellLeft + cellSize / 2f
                        val midY = cellTop + cellSize / 2f
                        val dSize = cellSize * 0.16f
                        val diamondPath = Path().apply {
                            moveTo(midX, midY - dSize)
                            lineTo(midX + dSize, midY)
                            lineTo(midX, midY + dSize)
                            lineTo(midX - dSize, midY)
                            close()
                        }
                        drawPath(diamondPath, color = colors.cellLockedBorder.copy(alpha = 0.45f))
                    } else {
                        // Ô chơi thông thường dập nổi xúc giác
                        drawRoundRect(
                            color = colors.cellEmpty,
                            topLeft = Offset(cellLeft + tileMargin, cellTop + tileMargin),
                            size = Size(tileSize, tileSize),
                            cornerRadius = cornerRadius
                        )
                        // Viền sáng vi tế cạnh trên
                        drawRoundRect(
                            color = Color.White.copy(alpha = if (colors.isDark) 0.04f else 0.4f),
                            topLeft = Offset(cellLeft + tileMargin, cellTop + tileMargin),
                            size = Size(tileSize, tileSize),
                            cornerRadius = cornerRadius,
                            style = Stroke(width = 1f)
                        )
                        // Lúm đồng tiền xúc giác ở tâm mỗi ô trống (Tactile center dimple)
                        drawCircle(
                            color = colors.gridLine.copy(alpha = 0.22f),
                            radius = cellSize * 0.05f,
                            center = Offset(cellLeft + cellSize / 2f, cellTop + cellSize / 2f)
                        )
                    }
                }
            }

            // 2. Vẽ hiệu ứng gợi ý Hint
            if (hintSnakeId != null) {
                val hintSnake = state.findSnake(hintSnakeId)
                if (hintSnake != null) {
                    for (cell in hintSnake.body) {
                        val cx = originX + (cell.x + 0.5f) * cellSize
                        val cy = originY + (cell.y + 0.5f) * cellSize
                        drawCircle(
                            color = SnakeColors.HintGlow.copy(alpha = 0.38f),
                            radius = cellSize * 0.55f,
                            center = Offset(cx, cy)
                        )
                    }
                }
            }

            // 2.5. Vẽ hiệu ứng Grid Vision
            if (isGridVisionActive) {
                for (snake in state.snakes) {
                    val headCenter = Offset(
                        originX + (snake.head.x + 0.5f) * cellSize,
                        originY + (snake.head.y + 0.5f) * cellSize
                    )
                    val canEscape = SnakeEscapeEngine.canEscape(state, snake.id)
                    if (canEscape) {
                        val exitDist = max(state.width, state.height) + 2
                        val exitPoint = Offset(
                            headCenter.x + snake.direction.dx * exitDist * cellSize,
                            headCenter.y + snake.direction.dy * exitDist * cellSize
                        )
                        drawLine(
                            color = Color(0xFF00E676),
                            start = headCenter,
                            end = exitPoint,
                            strokeWidth = 3.8f,
                            cap = StrokeCap.Round,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(16f, 14f))
                        )
                        drawCircle(
                            color = Color(0xFF00E676),
                            radius = cellSize * 0.18f,
                            center = headCenter
                        )
                    } else {
                        var check = snake.head + snake.direction
                        while (state.isInside(check)) {
                            val isOtherSnake = state.snakes.filter { it.id != snake.id }.any { it.body.contains(check) }
                            val isSelf = snake.body.contains(check)
                            val isLocked = state.isLocked(check)
                            if (isOtherSnake || isSelf || isLocked) break
                            check = check + snake.direction
                        }
                        val targetCenter = Offset(
                            originX + (check.x + 0.5f) * cellSize,
                            originY + (check.y + 0.5f) * cellSize
                        )
                        drawLine(
                            color = Color(0xFFFF5252).copy(alpha = 0.8f),
                            start = headCenter,
                            end = targetCenter,
                            strokeWidth = 2.8f,
                            cap = StrokeCap.Round,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f))
                        )
                    }
                }
            }

            // 3. Vẽ các con rắn đang đứng yên hoặc rung lắc
            for (snake in state.snakes) {
                val isShaking = (snake.id == shakingSnakeId)
                val shakeOffset = if (isShaking) {
                    val perpX = -snake.direction.dy.toFloat()
                    val perpY = snake.direction.dx.toFloat()
                    val magnitude = sin(shakeProgress * 4f * Math.PI.toFloat()) * (1f - shakeProgress) * (cellSize * 0.22f)
                    Offset(perpX * magnitude, perpY * magnitude)
                } else {
                    Offset.Zero
                }

                drawSnake(
                    snake = snake,
                    originX = originX + shakeOffset.x,
                    originY = originY + shakeOffset.y,
                    cellSize = cellSize,
                    isDizzy = isShaking
                )
            }

            // 4. Vẽ con rắn đang thực hiện hoạt hình trượt thoát ra ngoài
            if (escapingSnake != null) {
                drawEscapingSnake(
                    escapingAnim = escapingSnake,
                    originX = originX,
                    originY = originY,
                    cellSize = cellSize
                )
            }

            // 5. Vẽ hiệu ứng va chạm: Sóng xung kích (shockwave ripple) + ô bị chặn nháy đỏ + dấu X
            if (blockedCell != null && blockedAlpha > 0f) {
                val cx = originX + (blockedCell.x + 0.5f) * cellSize
                val cy = originY + (blockedCell.y + 0.5f) * cellSize
                val halfSize = cellSize * 0.36f

                // Sóng xung kích hình tròn lan tỏa ra ngoài
                val rippleRadius = cellSize * (0.35f + (1f - blockedAlpha) * 0.45f)
                drawCircle(
                    color = SnakeColors.BlockedRed.copy(alpha = blockedAlpha * 0.65f),
                    radius = rippleRadius,
                    center = Offset(cx, cy),
                    style = Stroke(width = 3.5f)
                )

                // Lớp phủ đỏ phát sáng
                drawRoundRect(
                    color = SnakeColors.BlockedGlow.copy(alpha = blockedAlpha * 0.6f),
                    topLeft = Offset(cx - halfSize * 1.2f, cy - halfSize * 1.2f),
                    size = Size(halfSize * 2.4f, halfSize * 2.4f),
                    cornerRadius = CornerRadius(cellSize * 0.22f)
                )

                // Dấu X màu đỏ cảnh báo
                val strokeW = (cellSize * 0.14f).coerceAtLeast(3.5f)
                drawLine(
                    color = SnakeColors.BlockedRed.copy(alpha = blockedAlpha),
                    start = Offset(cx - halfSize, cy - halfSize),
                    end = Offset(cx + halfSize, cy + halfSize),
                    strokeWidth = strokeW,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = SnakeColors.BlockedRed.copy(alpha = blockedAlpha),
                    start = Offset(cx + halfSize, cy - halfSize),
                    end = Offset(cx - halfSize, cy + halfSize),
                    strokeWidth = strokeW,
                    cap = StrokeCap.Round
                )
            }
        }
    }
}

/**
 * Vẽ một con rắn hoàn chỉnh với đồ họa 3D mềm mại, đổ bóng và dải sống lưng phản quang.
 */
private fun DrawScope.drawSnake(
    snake: Snake,
    originX: Float,
    originY: Float,
    cellSize: Float,
    isDizzy: Boolean = false
) {
    val theme = SnakeColors.getTheme(snake.colorId)
    val radius = cellSize * 0.38f
    val shadowOffset = Offset(cellSize * 0.04f, cellSize * 0.05f)

    // 0. Đổ bóng mềm nhiều tầng phía dưới thân rắn (Soft Drop Shadow)
    for (i in 0 until snake.body.size - 1) {
        val c1 = snake.body[i]
        val c2 = snake.body[i + 1]
        val p1 = Offset(originX + (c1.x + 0.5f) * cellSize, originY + (c1.y + 0.5f) * cellSize) + shadowOffset
        val p2 = Offset(originX + (c2.x + 0.5f) * cellSize, originY + (c2.y + 0.5f) * cellSize) + shadowOffset
        drawLine(
            color = Color.Black.copy(alpha = 0.3f),
            start = p1,
            end = p2,
            strokeWidth = radius * 2.1f,
            cap = StrokeCap.Round
        )
    }

    // 1. Thân chính: Các đoạn thân nối liền bo tròn
    for (i in 0 until snake.body.size - 1) {
        val c1 = snake.body[i]
        val c2 = snake.body[i + 1]
        val p1 = Offset(originX + (c1.x + 0.5f) * cellSize, originY + (c1.y + 0.5f) * cellSize)
        val p2 = Offset(originX + (c2.x + 0.5f) * cellSize, originY + (c2.y + 0.5f) * cellSize)

        drawLine(
            color = theme.body,
            start = p1,
            end = p2,
            strokeWidth = radius * 2f,
            cap = StrokeCap.Round
        )

        // Họa tiết vảy da rắn (Serpentine scale texture rib rings như trong ảnh mẫu)
        val midPoint = (p1 + p2) / 2f
        drawCircle(
            color = theme.patternColor,
            radius = radius * 0.45f,
            center = midPoint,
            style = Stroke(width = radius * 0.22f)
        )
    }

    // 2. Vẽ khớp tròn tại mỗi đốt kèm vân vảy
    for (i in snake.body.indices) {
        val cell = snake.body[i]
        val p = Offset(originX + (cell.x + 0.5f) * cellSize, originY + (cell.y + 0.5f) * cellSize)
        // Nếu là đuôi, vẽ vuốt nhọn (tapered tail)
        val r = if (i == snake.body.size - 1) radius * 0.65f else radius
        drawCircle(
            color = theme.body,
            radius = r,
            center = p
        )
        if (i > 0 && i < snake.body.size - 1) {
            drawCircle(
                color = theme.patternColor,
                radius = r * 0.4f,
                center = p,
                style = Stroke(width = r * 0.2f)
            )
        }
    }

    // 3. Dải sống lưng phản quang (3D Tubular Spine Highlight)
    for (i in 0 until snake.body.size - 1) {
        val c1 = snake.body[i]
        val c2 = snake.body[i + 1]
        val p1 = Offset(originX + (c1.x + 0.5f) * cellSize, originY + (c1.y + 0.5f) * cellSize)
        val p2 = Offset(originX + (c2.x + 0.5f) * cellSize, originY + (c2.y + 0.5f) * cellSize)

        drawLine(
            color = Color.White.copy(alpha = 0.32f),
            start = p1,
            end = p2,
            strokeWidth = radius * 0.42f,
            cap = StrokeCap.Round
        )
    }

    // 4. Chi tiết đầu rắn: Mắt, lỗ mũi, má hồng, trạng thái va chạm
    val headCell = snake.head
    val headCenter = Offset(
        originX + (headCell.x + 0.5f) * cellSize,
        originY + (headCell.y + 0.5f) * cellSize
    )

    drawSnakeFace(
        headCenter = headCenter,
        direction = snake.direction,
        radius = radius,
        eyeColor = theme.eye,
        isDizzy = isDizzy
    )
}

/**
 * Vẽ con rắn đang trượt dọc theo polyline thoát.
 */
private fun DrawScope.drawEscapingSnake(
    escapingAnim: EscapingSnakeAnim,
    originX: Float,
    originY: Float,
    cellSize: Float
) {
    val theme = SnakeColors.getTheme(escapingAnim.snake.colorId)
    val radius = cellSize * 0.38f
    val progress = escapingAnim.animatable.value

    val polyline = escapingAnim.polyline
    val snakeLength = escapingAnim.snake.length

    val segmentPoints = mutableListOf<Offset>()
    for (segIndex in 0 until snakeLength) {
        val targetDistance = progress + (snakeLength - 1 - segIndex)
        val pt = interpolatePolyline(polyline, targetDistance)
        segmentPoints.add(Offset(originX + (pt.x + 0.5f) * cellSize, originY + (pt.y + 0.5f) * cellSize))
    }

    // Đổ bóng trượt
    for (i in 0 until segmentPoints.size - 1) {
        drawLine(
            color = Color.Black.copy(alpha = 0.25f),
            start = segmentPoints[i] + Offset(cellSize * 0.04f, cellSize * 0.05f),
            end = segmentPoints[i + 1] + Offset(cellSize * 0.04f, cellSize * 0.05f),
            strokeWidth = radius * 2.1f,
            cap = StrokeCap.Round
        )
    }

    // Thân chính
    for (i in 0 until segmentPoints.size - 1) {
        drawLine(
            color = theme.body,
            start = segmentPoints[i],
            end = segmentPoints[i + 1],
            strokeWidth = radius * 2f,
            cap = StrokeCap.Round
        )
    }

    // Các đốt
    for (i in segmentPoints.indices) {
        val pt = segmentPoints[i]
        val r = if (i == segmentPoints.size - 1) radius * 0.75f else radius
        drawCircle(
            color = theme.body,
            radius = r,
            center = pt
        )
    }

    // Dải sống lưng
    for (i in 0 until segmentPoints.size - 1) {
        drawLine(
            color = Color.White.copy(alpha = 0.32f),
            start = segmentPoints[i],
            end = segmentPoints[i + 1],
            strokeWidth = radius * 0.42f,
            cap = StrokeCap.Round
        )
    }

    // Mặt rắn
    val headPoint = segmentPoints.first()
    val nextPoint = if (segmentPoints.size > 1) segmentPoints[1] else headPoint
    val forwardVector = headPoint - nextPoint
    val dir = when {
        forwardVector.x > 0.1f -> Direction.RIGHT
        forwardVector.x < -0.1f -> Direction.LEFT
        forwardVector.y > 0.1f -> Direction.DOWN
        forwardVector.y < -0.1f -> Direction.UP
        else -> escapingAnim.snake.direction
    }

    drawSnakeFace(
        headCenter = headPoint,
        direction = dir,
        radius = radius,
        eyeColor = theme.eye,
        isDizzy = false
    )
}

private fun interpolatePolyline(polyline: List<Offset>, distance: Float): Offset {
    if (polyline.isEmpty()) return Offset.Zero
    if (distance <= 0f) return polyline.first()

    var remaining = distance
    for (i in 0 until polyline.size - 1) {
        val p1 = polyline[i]
        val p2 = polyline[i + 1]
        val segLen = sqrt((p2.x - p1.x).pow(2) + (p2.y - p1.y).pow(2))
        if (remaining <= segLen) {
            val fraction = if (segLen > 0.0001f) remaining / segLen else 0f
            return Offset(
                p1.x + (p2.x - p1.x) * fraction,
                p1.y + (p2.y - p1.y) * fraction
            )
        }
        remaining -= segLen
    }

    return polyline.last()
}

/**
 * Vẽ khuôn mặt chi tiết và biểu cảm đáng yêu cho rắn:
 * - Lỗ mũi nhỏ phía chóp đầu
 * - Má hồng ngọt ngào hai bên
 * - Mắt to tròn với lòng trắng, con ngươi nhìn theo hướng di chuyển, và đốm sáng catchlight
 * - Khi bị chặn: Mắt biến thành chữ X kèm giọt mồ hôi hoạt hình
 */
private fun DrawScope.drawSnakeFace(
    headCenter: Offset,
    direction: Direction,
    radius: Float,
    eyeColor: Color,
    isDizzy: Boolean = false
) {
    val eyeRadius = radius * 0.33f
    val pupilRadius = eyeRadius * 0.52f

    val perpX = -direction.dy.toFloat()
    val perpY = direction.dx.toFloat()

    val forwardOffset = radius * 0.42f
    val lateralOffset = radius * 0.48f

    // 1. Hai lỗ mũi nhỏ ở chóp mũi (Snout & Nostrils)
    val snoutDist = radius * 0.85f
    val nostrilOffset = radius * 0.22f
    val nostril1 = Offset(headCenter.x + direction.dx * snoutDist + perpX * nostrilOffset, headCenter.y + direction.dy * snoutDist + perpY * nostrilOffset)
    val nostril2 = Offset(headCenter.x + direction.dx * snoutDist - perpX * nostrilOffset, headCenter.y + direction.dy * snoutDist - perpY * nostrilOffset)
    drawCircle(color = eyeColor.copy(alpha = 0.4f), radius = radius * 0.07f, center = nostril1)
    drawCircle(color = eyeColor.copy(alpha = 0.4f), radius = radius * 0.07f, center = nostril2)

    // 2. Hai vệt má hồng đáng yêu (Cute Blush)
    val blushForward = -direction.dx * (radius * 0.1f)
    val blushSide = radius * 0.65f
    val blush1 = Offset(headCenter.x + blushForward + perpX * blushSide, headCenter.y + (-direction.dy * radius * 0.1f) + perpY * blushSide)
    val blush2 = Offset(headCenter.x + blushForward - perpX * blushSide, headCenter.y + (-direction.dy * radius * 0.1f) - perpY * blushSide)
    drawCircle(color = Color(0xFFFF4081).copy(alpha = 0.38f), radius = radius * 0.2f, center = blush1)
    drawCircle(color = Color(0xFFFF4081).copy(alpha = 0.38f), radius = radius * 0.2f, center = blush2)

    // 3. Mắt
    val eye1Center = Offset(
        headCenter.x + direction.dx * forwardOffset + perpX * lateralOffset,
        headCenter.y + direction.dy * forwardOffset + perpY * lateralOffset
    )
    val eye2Center = Offset(
        headCenter.x + direction.dx * forwardOffset - perpX * lateralOffset,
        headCenter.y + direction.dy * forwardOffset - perpY * lateralOffset
    )

    if (isDizzy) {
        val xSize = eyeRadius * 1.15f
        for (ec in listOf(eye1Center, eye2Center)) {
            drawLine(
                color = eyeColor,
                start = Offset(ec.x - xSize, ec.y - xSize),
                end = Offset(ec.x + xSize, ec.y + xSize),
                strokeWidth = 2.8f,
                cap = StrokeCap.Round
            )
            drawLine(
                color = eyeColor,
                start = Offset(ec.x + xSize, ec.y - xSize),
                end = Offset(ec.x - xSize, ec.y + xSize),
                strokeWidth = 2.8f,
                cap = StrokeCap.Round
            )
        }
        // Giọt mồ hôi choáng váng phía trên
        val sweatCenter = Offset(headCenter.x - direction.dx * radius * 0.5f + perpX * radius * 0.5f, headCenter.y - direction.dy * radius * 0.5f + perpY * radius * 0.5f)
        drawCircle(color = Color(0xFF40C4FF), radius = radius * 0.15f, center = sweatCenter)
    } else {
        // Đôi mắt to hoạt hình 3D (Cartoon Googly Eyes như trong ảnh mẫu)
        val bigEyeRadius = eyeRadius * 1.12f
        val irisRadius = bigEyeRadius * 0.68f
        val blackPupilRadius = irisRadius * 0.62f
        val catchlightRadius = blackPupilRadius * 0.45f

        for (ec in listOf(eye1Center, eye2Center)) {
            // Viền nhẹ mắt
            drawCircle(color = Color(0x33000000), radius = bigEyeRadius + 1f, center = ec)
            // Lòng trắng mắt (Sclera)
            drawCircle(color = Color.White, radius = bigEyeRadius, center = ec)
            // Mống mắt màu xanh biển cartoon (Sky Blue Iris)
            drawCircle(color = Color(0xFF38BDF8), radius = irisRadius, center = ec)
            // Con ngươi đen tròn (Deep Black Pupil)
            drawCircle(color = Color(0xFF0F172A), radius = blackPupilRadius, center = ec)
            // Đốm sáng phản quang hoạt hình (White Catchlight glint)
            val glintPos = Offset(ec.x - blackPupilRadius * 0.35f, ec.y - blackPupilRadius * 0.35f)
            drawCircle(color = Color.White, radius = catchlightRadius, center = glintPos)
        }
    }
}
