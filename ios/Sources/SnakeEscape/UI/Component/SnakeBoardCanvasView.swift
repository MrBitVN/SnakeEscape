import SwiftUI

/// Trạng thái của con rắn đang thực hiện hoạt hình trượt thoát ra ngoài bàn cờ.
public struct EscapingSnakeAnimationState: Equatable {
    public let snake: Snake
    public let polyline: [CGPoint] // Điểm mốc polyline theo đơn vị Cell [Tail -> ... -> Head -> Exit]
    public var progress: CGFloat   // Quãng đường đã đi (theo đơn vị đốt)

    public init(snake: Snake, polyline: [CGPoint], progress: CGFloat = 0) {
        self.snake = snake
        self.polyline = polyline
        self.progress = progress
    }
}

/// SwiftUI Canvas hiển thị bàn cờ Snake Escape:
/// - Tự động co dãn theo kích thước màn hình
/// - Vẽ thân rắn bo tròn nối liền, mắt tinh tế theo hướng
/// - Hoạt hình trượt mượt mà
/// - Hiệu ứng rung lắc và dấu X nháy đỏ khi bị chặn
/// - Hỗ trợ Pinch-to-zoom và Pan trên lưới lớn
/// - Hỗ trợ Dark Mode & Light Mode
public struct SnakeBoardCanvasView: View {
    public let state: GameState
    public let colors: GameColorScheme
    public let shakingSnakeId: Int?
    public let shakeProgress: CGFloat
    public let blockedCell: Cell?
    public let blockedAlpha: CGFloat
    public let hintSnakeId: Int?
    public let isGridVisionActive: Bool
    public let escapingSnake: EscapingSnakeAnimationState?
    public let onSnakeTapped: (Int) -> Void

    @State private var scale: CGFloat = 1.0
    @State private var lastScale: CGFloat = 1.0
    @State private var panOffset: CGSize = .zero
    @State private var lastPanOffset: CGSize = .zero

    public init(
        state: GameState,
        colors: GameColorScheme,
        shakingSnakeId: Int? = nil,
        shakeProgress: CGFloat = 0,
        blockedCell: Cell? = nil,
        blockedAlpha: CGFloat = 0,
        hintSnakeId: Int? = nil,
        isGridVisionActive: Bool = false,
        escapingSnake: EscapingSnakeAnimationState? = nil,
        onSnakeTapped: @escaping (Int) -> Void
    ) {
        self.state = state
        self.colors = colors
        self.shakingSnakeId = shakingSnakeId
        self.shakeProgress = shakeProgress
        self.blockedCell = blockedCell
        self.blockedAlpha = blockedAlpha
        self.hintSnakeId = hintSnakeId
        self.isGridVisionActive = isGridVisionActive
        self.escapingSnake = escapingSnake
        self.onSnakeTapped = onSnakeTapped
    }

    public var body: some View {
        GeometryReader { geometry in
            let size = geometry.size
            let padding: CGFloat = 20
            let baseCellSize = min(
                (size.width - 2 * padding) / CGFloat(state.width),
                (size.height - 2 * padding) / CGFloat(state.height)
            )
            let cellSize = baseCellSize * scale
            let gridW = cellSize * CGFloat(state.width)
            let gridH = cellSize * CGFloat(state.height)
            let originX = (size.width - gridW) / 2 + panOffset.width
            let originY = (size.height - gridH) / 2 + panOffset.height

            ZStack {
                colors.background
                    .ignoresSafeArea()

                Canvas { context, _ in
                    // 1. Vẽ các ô của bàn cờ (Grid Tiles)
                    drawGridTiles(
                        context: context,
                        originX: originX,
                        originY: originY,
                        cellSize: cellSize
                    )

                    // 2. Vẽ hiệu ứng gợi ý Hint
                    if let hintId = hintSnakeId, let hintSnake = state.findSnake(id: hintId) {
                        drawHintGlow(
                            context: context,
                            snake: hintSnake,
                            originX: originX,
                            originY: originY,
                            cellSize: cellSize
                        )
                    }

                    // 2.5. Vẽ hiệu ứng Grid Vision (Laser dẫn đường)
                    if isGridVisionActive {
                        drawGridVision(
                            context: context,
                            originX: originX,
                            originY: originY,
                            cellSize: cellSize
                        )
                    }

                    // 3. Vẽ các con rắn đang đứng yên hoặc rung lắc
                    for snake in state.snakes {
                        let isShaking = (snake.id == shakingSnakeId)
                        let shakeOffset: CGSize
                        if isShaking {
                            let perpX = -CGFloat(snake.direction.dy)
                            let perpY = CGFloat(snake.direction.dx)
                            let magnitude = sin(shakeProgress * 4 * .pi) * (1 - shakeProgress) * (cellSize * 0.2)
                            shakeOffset = CGSize(width: perpX * magnitude, height: perpY * magnitude)
                        } else {
                            shakeOffset = .zero
                        }

                        drawSnake(
                            context: context,
                            snake: snake,
                            originX: originX + shakeOffset.width,
                            originY: originY + shakeOffset.height,
                            cellSize: cellSize,
                            isDizzy: isShaking
                        )
                    }

                    // 4. Vẽ con rắn đang trượt thoát ra ngoài
                    if let escaping = escapingSnake {
                        drawEscapingSnake(
                            context: context,
                            escapingAnim: escaping,
                            originX: originX,
                            originY: originY,
                            cellSize: cellSize
                        )
                    }

                    // 5. Vẽ hiệu ứng va chạm: Ô bị chặn nháy đỏ + dấu X
                    if let blocked = blockedCell, blockedAlpha > 0 {
                        drawBlockedCell(
                            context: context,
                            cell: blocked,
                            alpha: blockedAlpha,
                            originX: originX,
                            originY: originY,
                            cellSize: cellSize
                        )
                    }
                }
                .contentShape(Rectangle())
                .gesture(
                    SimultaneousGesture(
                        MagnificationGesture()
                            .onChanged { value in
                                scale = (lastScale * value).clamped(to: 0.7...3.5)
                            }
                            .onEnded { _ in
                                lastScale = scale
                            },
                        DragGesture(minimumDistance: 0)
                            .onChanged { value in
                                if abs(value.translation.width) > 6 || abs(value.translation.height) > 6 {
                                    panOffset = CGSize(
                                        width: lastPanOffset.width + value.translation.width,
                                        height: lastPanOffset.height + value.translation.height
                                    )
                                }
                            }
                            .onEnded { value in
                                if abs(value.translation.width) <= 6 && abs(value.translation.height) <= 6 {
                                    // Coi như Tap nếu khoảng cách kéo nhỏ
                                    handleTap(at: value.location, originX: originX, originY: originY, cellSize: cellSize)
                                } else {
                                    lastPanOffset = panOffset
                                }
                            }
                    )
                )
                .onTapGesture(count: 2) {
                    // Double tap: Khôi phục kích thước và vị trí ban đầu
                    withAnimation(.spring()) {
                        scale = 1.0
                        lastScale = 1.0
                        panOffset = .zero
                        lastPanOffset = .zero
                    }
                }
            }
        }
    }

    private func handleTap(at location: CGPoint, originX: CGFloat, originY: CGFloat, cellSize: CGFloat) {
        let col = Int((location.x - originX) / cellSize)
        let row = Int((location.y - originY) / cellSize)

        if col >= 0 && col < state.width && row >= 0 && row < state.height {
            let clickedCell = Cell(col, row)
            if let snake = state.snakes.first(where: { $0.body.contains(clickedCell) }) {
                onSnakeTapped(snake.id)
            }
        }
    }

    // MARK: - Drawing Routines

    private func drawGridTiles(context: GraphicsContext, originX: CGFloat, originY: CGFloat, cellSize: CGFloat) {
        for col in 0..<state.width {
            for row in 0..<state.height {
                let cell = Cell(col, row)
                let cellRect = CGRect(
                    x: originX + CGFloat(col) * cellSize + 2,
                    y: originY + CGFloat(row) * cellSize + 2,
                    width: cellSize - 4,
                    height: cellSize - 4
                )
                let cornerRadius = cellSize * 0.16

                if state.isLocked(cell) {
                    // Ô bị khoá (vật cản / mask hình tròn, tim, sao)
                    let path = Path(roundedRect: cellRect, cornerRadius: cornerRadius)
                    context.fill(path, with: .color(colors.cellLocked))
                    context.stroke(path, with: .color(colors.cellLockedBorder), lineWidth: 1.5)
                } else {
                    // Ô chơi thông thường
                    let path = Path(roundedRect: cellRect, cornerRadius: cornerRadius)
                    context.fill(path, with: .color(colors.cellEmpty))
                }
            }
        }
    }

    private func drawHintGlow(context: GraphicsContext, snake: Snake, originX: CGFloat, originY: CGFloat, cellSize: CGFloat) {
        for cell in snake.body {
            let cx = originX + (CGFloat(cell.x) + 0.5) * cellSize
            let cy = originY + (CGFloat(cell.y) + 0.5) * cellSize
            let rect = CGRect(
                x: cx - cellSize * 0.52,
                y: cy - cellSize * 0.52,
                width: cellSize * 1.04,
                height: cellSize * 1.04
            )
            context.fill(Path(ellipseIn: rect), with: .color(SnakeColors.hintGlow.opacity(0.35)))
        }
    }

    private func drawGridVision(context: GraphicsContext, originX: CGFloat, originY: CGFloat, cellSize: CGFloat) {
        for snake in state.snakes {
            let headCenter = CGPoint(
                x: originX + (CGFloat(snake.head.x) + 0.5) * cellSize,
                y: originY + (CGFloat(snake.head.y) + 0.5) * cellSize
            )
            let canEscape = SnakeEscapeEngine.canEscape(state: state, snakeId: snake.id)

            if canEscape {
                let exitDist = CGFloat(max(state.width, state.height) + 2)
                let exitPoint = CGPoint(
                    x: headCenter.x + CGFloat(snake.direction.dx) * exitDist * cellSize,
                    y: headCenter.y + CGFloat(snake.direction.dy) * exitDist * cellSize
                )
                var path = Path()
                path.move(to: headCenter)
                path.addLine(to: exitPoint)

                let style = StrokeStyle(lineWidth: 3.5, lineCap: .round, dash: [14, 14])
                context.stroke(path, with: .color(Color(red: 0.0, green: 0.9, blue: 0.46)), style: style)

                let headCircle = CGRect(
                    x: headCenter.x - cellSize * 0.16,
                    y: headCenter.y - cellSize * 0.16,
                    width: cellSize * 0.32,
                    height: cellSize * 0.32
                )
                context.fill(Path(ellipseIn: headCircle), with: .color(Color(red: 0.0, green: 0.9, blue: 0.46)))
            } else {
                // Tia đỏ va chạm
                var check = snake.head + snake.direction
                while state.isInside(check) {
                    let isOtherSnake = state.snakes.filter { $0.id != snake.id }.contains { $0.body.contains(check) }
                    let isSelf = snake.body.contains(check)
                    let isLocked = state.isLocked(check)
                    if isOtherSnake || isSelf || isLocked { break }
                    check = check + snake.direction
                }
                let targetCenter = CGPoint(
                    x: originX + (CGFloat(check.x) + 0.5) * cellSize,
                    y: originY + (CGFloat(check.y) + 0.5) * cellSize
                )
                var path = Path()
                path.move(to: headCenter)
                path.addLine(to: targetCenter)

                let style = StrokeStyle(lineWidth: 2.5, lineCap: .round, dash: [8, 8])
                context.stroke(path, with: .color(Color(red: 1.0, green: 0.32, blue: 0.32).opacity(0.75)), style: style)
            }
        }
    }

    private func drawSnake(context: GraphicsContext, snake: Snake, originX: CGFloat, originY: CGFloat, cellSize: CGFloat, isDizzy: Bool) {
        let theme = SnakeColors.getTheme(snake.colorId)
        let radius = cellSize * 0.38

        // 1. Vẽ các đoạn thân nối liền bo tròn
        for i in 0..<(snake.body.count - 1) {
            let c1 = snake.body[i]
            let c2 = snake.body[i + 1]
            let p1 = CGPoint(x: originX + (CGFloat(c1.x) + 0.5) * cellSize, y: originY + (CGFloat(c1.y) + 0.5) * cellSize)
            let p2 = CGPoint(x: originX + (CGFloat(c2.x) + 0.5) * cellSize, y: originY + (CGFloat(c2.y) + 0.5) * cellSize)

            var path = Path()
            path.move(to: p1)
            path.addLine(to: p2)
            context.stroke(path, with: .color(theme.body), style: StrokeStyle(lineWidth: radius * 2, lineCap: .round))
        }

        // 2. Vẽ khớp tròn tại mỗi đốt
        for cell in snake.body {
            let p = CGPoint(x: originX + (CGFloat(cell.x) + 0.5) * cellSize, y: originY + (CGFloat(cell.y) + 0.5) * cellSize)
            let rect = CGRect(x: p.x - radius, y: p.y - radius, width: radius * 2, height: radius * 2)
            context.fill(Path(ellipseIn: rect), with: .color(theme.body))
        }

        // 3. Vẽ mắt ở đầu rắn
        let headCenter = CGPoint(
            x: originX + (CGFloat(snake.head.x) + 0.5) * cellSize,
            y: originY + (CGFloat(snake.head.y) + 0.5) * cellSize
        )
        drawSnakeEyes(context: context, headCenter: headCenter, direction: snake.direction, radius: radius, eyeColor: theme.eye, isDizzy: isDizzy)
    }

    private func drawEscapingSnake(context: GraphicsContext, escapingAnim: EscapingSnakeAnimationState, originX: CGFloat, originY: CGFloat, cellSize: CGFloat) {
        let theme = SnakeColors.getTheme(escapingAnim.snake.colorId)
        let radius = cellSize * 0.38
        let progress = escapingAnim.progress
        let polyline = escapingAnim.polyline
        let snakeLength = escapingAnim.snake.length

        // Tính vị trí liên tục của từng đốt dọc theo polyline
        var segmentPoints: [CGPoint] = []
        for segIndex in 0..<snakeLength {
            let targetDistance = progress + CGFloat(snakeLength - 1 - segIndex)
            let pt = interpolatePolyline(polyline: polyline, distance: targetDistance)
            segmentPoints.append(CGPoint(x: originX + (pt.x + 0.5) * cellSize, y: originY + (pt.y + 0.5) * cellSize))
        }

        // Vẽ thân nối
        for i in 0..<(segmentPoints.count - 1) {
            var path = Path()
            path.move(to: segmentPoints[i])
            path.addLine(to: segmentPoints[i + 1])
            context.stroke(path, with: .color(theme.body), style: StrokeStyle(lineWidth: radius * 2, lineCap: .round))
        }

        // Vẽ từng đốt tròn
        for pt in segmentPoints {
            let rect = CGRect(x: pt.x - radius, y: pt.y - radius, width: radius * 2, height: radius * 2)
            context.fill(Path(ellipseIn: rect), with: .color(theme.body))
        }

        // Vẽ mắt ở đầu
        if let headPoint = segmentPoints.first {
            let nextPoint = segmentPoints.count > 1 ? segmentPoints[1] : headPoint
            let fwd = CGPoint(x: headPoint.x - nextPoint.x, y: headPoint.y - nextPoint.y)
            let dir: Direction
            if fwd.x > 0.1 { dir = .right }
            else if fwd.x < -0.1 { dir = .left }
            else if fwd.y > 0.1 { dir = .down }
            else if fwd.y < -0.1 { dir = .up }
            else { dir = escapingAnim.snake.direction }

            drawSnakeEyes(context: context, headCenter: headPoint, direction: dir, radius: radius, eyeColor: theme.eye, isDizzy: false)
        }
    }

    private func interpolatePolyline(polyline: [CGPoint], distance: CGFloat) -> CGPoint {
        guard !polyline.isEmpty else { return .zero }
        if distance <= 0 { return polyline.first! }

        var remaining = distance
        for i in 0..<(polyline.count - 1) {
            let p1 = polyline[i]
            let p2 = polyline[i + 1]
            let dx = p2.x - p1.x
            let dy = p2.y - p1.y
            let segLen = sqrt(dx * dx + dy * dy)
            if remaining <= segLen {
                let frac = segLen > 0.0001 ? remaining / segLen : 0
                return CGPoint(x: p1.x + dx * frac, y: p1.y + dy * frac)
            }
            remaining -= segLen
        }
        return polyline.last!
    }

    private func drawSnakeEyes(context: GraphicsContext, headCenter: CGPoint, direction: Direction, radius: CGFloat, eyeColor: Color, isDizzy: Bool) {
        let eyeRadius = radius * 0.32
        let pupilRadius = eyeRadius * 0.5

        let perpX = -CGFloat(direction.dy)
        let perpY = CGFloat(direction.dx)
        let fwdOffset = radius * 0.42
        let latOffset = radius * 0.48

        let eye1Center = CGPoint(
            x: headCenter.x + CGFloat(direction.dx) * fwdOffset + perpX * latOffset,
            y: headCenter.y + CGFloat(direction.dy) * fwdOffset + perpY * latOffset
        )
        let eye2Center = CGPoint(
            x: headCenter.x + CGFloat(direction.dx) * fwdOffset - perpX * latOffset,
            y: headCenter.y + CGFloat(direction.dy) * fwdOffset - perpY * latOffset
        )

        if isDizzy {
            // Mắt chữ X khi bị chặn
            let xSize = eyeRadius * 1.1
            for ec in [eye1Center, eye2Center] {
                var p1 = Path()
                p1.move(to: CGPoint(x: ec.x - xSize, y: ec.y - xSize))
                p1.addLine(to: CGPoint(x: ec.x + xSize, y: ec.y + xSize))
                p1.move(to: CGPoint(x: ec.x + xSize, y: ec.y - xSize))
                p1.addLine(to: CGPoint(x: ec.x - xSize, y: ec.y + xSize))
                context.stroke(p1, with: .color(eyeColor), style: StrokeStyle(lineWidth: 2.5, lineCap: .round))
            }
        } else {
            // Lòng trắng mắt
            for ec in [eye1Center, eye2Center] {
                let rect = CGRect(x: ec.x - eyeRadius, y: ec.y - eyeRadius, width: eyeRadius * 2, height: eyeRadius * 2)
                context.fill(Path(ellipseIn: rect), with: .color(.white))
            }

            // Con ngươi
            let pupilShift = eyeRadius * 0.35
            for ec in [eye1Center, eye2Center] {
                let pc = CGPoint(x: ec.x + CGFloat(direction.dx) * pupilShift, y: ec.y + CGFloat(direction.dy) * pupilShift)
                let rect = CGRect(x: pc.x - pupilRadius, y: pc.y - pupilRadius, width: pupilRadius * 2, height: pupilRadius * 2)
                context.fill(Path(ellipseIn: rect), with: .color(eyeColor))

                // Catchlight
                let clRadius = pupilRadius * 0.35
                let clRect = CGRect(x: pc.x - clRadius * 0.6 - clRadius, y: pc.y - clRadius * 0.6 - clRadius, width: clRadius * 2, height: clRadius * 2)
                context.fill(Path(ellipseIn: clRect), with: .color(.white.opacity(0.9)))
            }
        }
    }

    private func drawBlockedCell(context: GraphicsContext, cell: Cell, alpha: CGFloat, originX: CGFloat, originY: CGFloat, cellSize: CGFloat) {
        let cx = originX + (CGFloat(cell.x) + 0.5) * cellSize
        let cy = originY + (CGFloat(cell.y) + 0.5) * cellSize
        let halfSize = cellSize * 0.35

        let glowRect = CGRect(x: cx - halfSize * 1.2, y: cy - halfSize * 1.2, width: halfSize * 2.4, height: halfSize * 2.4)
        context.fill(Path(roundedRect: glowRect, cornerRadius: cellSize * 0.2), with: .color(SnakeColors.blockedGlow.opacity(alpha * 0.6)))

        // Dấu X đỏ cảnh báo
        let strokeW = max(3.0, cellSize * 0.12)
        var xPath = Path()
        xPath.move(to: CGPoint(x: cx - halfSize, y: cy - halfSize))
        xPath.addLine(to: CGPoint(x: cx + halfSize, y: cy + halfSize))
        xPath.move(to: CGPoint(x: cx + halfSize, y: cy - halfSize))
        xPath.addLine(to: CGPoint(x: cx - halfSize, y: cy + halfSize))
        context.stroke(xPath, with: .color(SnakeColors.blockedRed.opacity(alpha)), style: StrokeStyle(lineWidth: strokeW, lineCap: .round))
    }
}

private extension Comparable {
    func clamped(to limits: ClosedRange<Self>) -> Self {
        min(max(self, limits.lowerBound), limits.upperBound)
    }
}
