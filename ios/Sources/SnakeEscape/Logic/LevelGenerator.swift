import Foundation

/// Bộ sinh màn chơi (Level Generator) bảo đảm 100% LUÔN GIẢI ĐƯỢC.
///
/// Thuật toán Sinh Ngược (Backward Generation) kết hợp Đan Xen Sâu (Deep Interlocking):
/// - Bắt đầu từ lưới rỗng (kèm các ô Locked của ShapeMask).
/// - Lần lượt sinh từng con rắn: Rắn thứ k được đặt vào lưới sao cho đường thoát thẳng
///   của nó hoàn toàn thông thoáng đối với các ô trống hiện tại.
/// - Ưu tiên các con rắn vắt ngang qua đường thoát của các con rắn đặt trước để gia tăng
///   độ sâu cây phụ thuộc (dependency tree depth), biến màn chơi thành ma trận nan giải.
/// - Nhờ đó, thứ tự gỡ ngược lại luôn là một lời giải hợp lệ 100%.
public enum LevelGenerator {

    /// Sinh màn chơi theo số thứ tự level, seed (mặc định seed = level) và chế độ chơi (Campaign / Challenge).
    public static func generate(level: Int, seed: Int64? = nil, mode: GameMode = .campaign) -> GameState {
        let actualSeed = seed ?? Int64(level)
        return Engine(level: level, seed: actualSeed, mode: mode).generate()
    }

    private final class Engine {
        let level: Int
        let seed: Int64
        let mode: GameMode
        var random: JavaRandom
        let config: LevelConfig

        init(level: Int, seed: Int64, mode: GameMode) {
            self.level = level
            self.seed = seed
            self.mode = mode
            self.random = JavaRandom(seed: seed)
            self.config = LevelConfig.fromLevel(level, mode: mode)
        }

        func generate() -> GameState {
            let width = config.width
            let height = config.height
            let lockedCells = generateMask(maskType: config.maskType, width: width, height: height)

            var placedSnakes: [Snake] = []
            var occupiedCells = lockedCells

            let maxAttemptsPerSnake = 240

            for _ in 1...config.targetSnakes {
                var bestCandidate: Snake? = nil
                var bestInterlockScore = -1

                for attempt in 1...maxAttemptsPerSnake {
                    // 1. Tìm các ô còn trống
                    var freeCells: [Cell] = []
                    for x in 0..<width {
                        for y in 0..<height {
                            let c = Cell(x, y)
                            if !occupiedCells.contains(c) {
                                freeCells.append(c)
                            }
                        }
                    }
                    if freeCells.isEmpty { break }

                    let headIndex = random.nextInt(freeCells.count)
                    let head = freeCells[headIndex]

                    // 2. Thử các hướng di chuyển của đầu
                    var directions = Direction.allCases
                    random.shuffle(&directions)

                    for dir in directions {
                        // Kiểm tra tia thoát thẳng từ head theo hướng dir có thông thoáng không
                        if !isRayClear(head: head, dir: dir, width: width, height: height, occupiedCells: occupiedCells) {
                            continue
                        }

                        // 3. Nuôi thân rắn từ head lùi vào trong lưới
                        let targetLength: Int
                        if config.maxLength > config.minLength {
                            targetLength = config.minLength + random.nextInt(config.maxLength - config.minLength + 1)
                        } else {
                            targetLength = config.minLength
                        }

                        guard let body = growSnakeBody(
                            head: head,
                            headDir: dir,
                            targetLength: targetLength,
                            width: width,
                            height: height,
                            occupiedCells: occupiedCells,
                            bendChance: config.bendChance
                        ) else {
                            continue
                        }

                        let candidateSnake = Snake(
                            id: placedSnakes.count + 1,
                            body: body,
                            direction: dir,
                            colorId: placedSnakes.count % 8
                        )

                        // 4. Kiểm chứng tính hợp lệ của tia thoát bằng chính Engine
                        let testState = GameState(
                            width = width,
                            height = height,
                            snakes: placedSnakes + [candidateSnake],
                            lockedCells: lockedCells,
                            hearts: config.hearts
                        )

                        if SnakeEscapeEngine.canEscape(state: testState, snakeId: candidateSnake.id) {
                            let interlockScore: Int = {
                                if placedSnakes.isEmpty { return 0 }
                                return placedSnakes.filter { prev in
                                    candidateSnake.body.contains { cell in
                                        isCellOnRay(head: prev.head, dir: prev.direction, cell: cell)
                                    }
                                }.count
                            }()

                            if interlockScore > bestInterlockScore {
                                bestInterlockScore = interlockScore
                                bestCandidate = candidateSnake
                                if interlockScore >= 2 {
                                    break
                                }
                            }
                        }
                    }

                    if bestCandidate != nil && (bestInterlockScore >= 1 || attempt > 30 || placedSnakes.isEmpty) {
                        break
                    }
                }

                if let candidate = bestCandidate {
                    placedSnakes.append(candidate)
                    for c in candidate.body {
                        occupiedCells.insert(c)
                    }
                }
            }

            return GameState(
                width: width,
                height: height,
                snakes: placedSnakes,
                lockedCells: lockedCells,
                hearts: config.hearts
            )
        }

        private func isRayClear(
            head: Cell,
            dir: Direction,
            width: Int,
            height: Int,
            occupiedCells: Set<Cell>
        ) -> Bool {
            var current = head + dir
            while current.x >= 0 && current.x < width && current.y >= 0 && current.y < height {
                if occupiedCells.contains(current) {
                    return false
                }
                current = current + dir
            }
            return true
        }

        private func isCellOnRay(head: Cell, dir: Direction, cell: Cell) -> Bool {
            var current = head + dir
            while abs(current.x - head.x) <= 15 && abs(current.y - head.y) <= 15 {
                if current == cell { return true }
                current = current + dir
            }
            return false
        }

        private func growSnakeBody(
            head: Cell,
            headDir: Direction,
            targetLength: Int,
            width: Int,
            height: Int,
            occupiedCells: Set<Cell>,
            bendChance: Double
        ) -> [Cell]? {
            var body = [head]
            var bodySet = Set<Cell>([head])
            var current = head
            var currentGrowthDir = headDir.opposite

            while body.count < targetLength {
                var candidates: [(Cell, Direction)] = []

                for growthDir in Direction.allCases {
                    if growthDir == currentGrowthDir.opposite { continue }
                    let nextCell = current + growthDir
                    if nextCell.x >= 0 && nextCell.x < width && nextCell.y >= 0 && nextCell.y < height {
                        if !occupiedCells.contains(nextCell) && !bodySet.contains(nextCell) {
                            candidates.append((nextCell, growthDir))
                        }
                    }
                }

                if candidates.isEmpty {
                    return nil
                }

                let straightCandidate = candidates.first { $0.1 == currentGrowthDir }
                let chosen: (Cell, Direction)
                if let straight = straightCandidate, random.nextDouble() >= bendChance {
                    chosen = straight
                } else {
                    let idx = random.nextInt(candidates.count)
                    chosen = candidates[idx]
                }

                current = chosen.0
                currentGrowthDir = chosen.1
                body.append(current)
                bodySet.insert(current)
            }

            return body
        }

        private func generateMask(maskType: ShapeMask, width: Int, height: Int) -> Set<Cell> {
            var locked = Set<Cell>()
            let cx = Double(width - 1) / 2.0
            let cy = Double(height - 1) / 2.0
            let radius = Double(min(width, height)) / 2.0

            switch maskType {
            case .none:
                break

            case .circle:
                for x in 0..<width {
                    for y in 0..<height {
                        let dx = Double(x) - cx
                        let dy = Double(y) - cy
                        let distSq = dx * dx + dy * dy
                        if abs(dx) <= 0.6 || abs(dy) <= 0.6 { continue }

                        if distSq > (radius + 0.1) * (radius + 0.1) {
                            locked.insert(Cell(x, y))
                        }
                    }
                }

            case .heart:
                let midX = width / 2
                if height >= 6 {
                    locked.insert(Cell(midX, 0))
                    if width % 2 == 0 { locked.insert(Cell(midX - 1, 0)) }
                }
                locked.insert(Cell(0, 0))
                locked.insert(Cell(width - 1, 0))

                let bottomCutRows = max(1, height / 4)
                for r in 0..<bottomCutRows {
                    let y = height - 1 - r
                    let cutCols = bottomCutRows - r
                    for x in 0..<cutCols {
                        locked.insert(Cell(x, y))
                        locked.insert(Cell(width - 1 - x, y))
                    }
                }

            case .star:
                let cornerCut = max(1, min(width, height) / 3)
                for x in 0..<cornerCut {
                    for y in 0..<cornerCut {
                        if x + y < cornerCut + 1 {
                            locked.insert(Cell(x, y))
                            locked.insert(Cell(width - 1 - x, y))
                            locked.insert(Cell(x, height - 1 - y))
                            locked.insert(Cell(width - 1 - x, height - 1 - y))
                        }
                    }
                }

            case .diamond:
                for x in 0..<width {
                    for y in 0..<height {
                        let distManhattan = abs(Double(x) - cx) + abs(Double(y) - cy)
                        if abs(Double(x) - cx) <= 0.6 || abs(Double(y) - cy) <= 0.6 { continue }
                        if distManhattan > radius + 1.8 {
                            locked.insert(Cell(x, y))
                        }
                    }
                }

            case .maze:
                var x = 2
                while x < width - 2 {
                    var y = 2
                    while y < height - 2 {
                        if abs(Double(x) - cx) > 0.8 || abs(Double(y) - cy) > 0.8 {
                            locked.insert(Cell(x, y))
                        }
                        y += 2
                    }
                    x += 2
                }

            case .helix:
                let stepCount = max(1, min(width, height) / 4)
                for s in 0..<stepCount {
                    locked.insert(Cell(s, s + 1))
                    locked.insert(Cell(width - 1 - s, height - 2 - s))
                    locked.insert(Cell(width - 2 - s, s))
                    locked.insert(Cell(s + 1, height - 1 - s))
                }

            case .skull:
                locked.insert(Cell(0, height - 1))
                locked.insert(Cell(1, height - 1))
                locked.insert(Cell(width - 1, height - 1))
                locked.insert(Cell(width - 2, height - 1))
                if width >= 8 && height >= 8 {
                    let eyeY = Int(Double(height) * 0.35)
                    let eyeX1 = Int(Double(width) * 0.28)
                    let eyeX2 = width - 1 - eyeX1
                    locked.insert(Cell(eyeX1, eyeY))
                    locked.insert(Cell(eyeX2, eyeY))
                }
            }

            return locked
        }
    }
}
