import XCTest
@testable import SnakeEscape

final class LevelGeneratorTests: XCTestCase {

    /// Assert 200 màn chơi liên tiếp (Level 1..200) sinh ra đều LUÔN GIẢI ĐƯỢC 100%
    func test200LevelsAreAllSolvable() {
        var totalSolveTime: TimeInterval = 0

        for level in 1...200 {
            let start = Date()
            let state = LevelGenerator.generate(level: level, seed: Int64(level), mode: .campaign)
            let genTime = Date().timeIntervalSince(start)

            XCTAssertFalse(state.snakes.isEmpty, "Level \(level) must have at least 1 snake")

            let solveStart = Date()
            let solvable = SnakeEscapeEngine.isSolvable(state: state)
            let solveTime = Date().timeIntervalSince(solveStart)
            totalSolveTime += (genTime + solveTime)

            XCTAssertTrue(
                solvable,
                "Level \(level) with \(state.snakes.count) snakes on \(state.width)x\(state.height) grid must be solvable!"
            )
        }

        print("Successfully generated and solved 200 consecutive levels in \(String(format: "%.3f", totalSolveTime))s")
    }

    /// Kiểm tra tính tăng dần độ khó và các loại mặt nạ hình học
    func testDifficultyProgressionAndMasks() {
        // Level 5: Lưới 5x5, không mask
        let lvl5 = LevelGenerator.generate(level: 5, mode: .campaign)
        XCTAssertEqual(lvl5.width, 5)
        XCTAssertEqual(lvl5.height, 5)
        XCTAssertTrue(lvl5.lockedCells.isEmpty)
        XCTAssertTrue((3...6).contains(lvl5.snakes.count))

        // Level 50: Lưới 8x8, Circle Mask
        let lvl50 = LevelGenerator.generate(level: 50, mode: .campaign)
        XCTAssertEqual(lvl50.width, 8)
        XCTAssertEqual(lvl50.height, 8)
        XCTAssertFalse(lvl50.lockedCells.isEmpty)

        // Level 100: Lưới 9x9, Heart Mask
        let lvl100 = LevelGenerator.generate(level: 100, mode: .campaign)
        XCTAssertEqual(lvl100.width, 9)
        XCTAssertEqual(lvl100.height, 9)
        XCTAssertFalse(lvl100.lockedCells.isEmpty)

        // Level 180: Lưới 10x10, Maze Mask
        let lvl180 = LevelGenerator.generate(level: 180, mode: .campaign)
        XCTAssertEqual(lvl180.width, 10)
        XCTAssertEqual(lvl180.height, 10)
        XCTAssertFalse(lvl180.lockedCells.isEmpty)
        XCTAssertTrue(lvl180.snakes.count >= 10)
    }

    /// Kiểm tra chế độ Thử Thách Siêu Cấp (Grandmaster Challenge)
    func testChallengeModeLevels() {
        for challengeLvl in [1, 10, 25, 45, 50] {
            let state = LevelGenerator.generate(level: challengeLvl, mode: .challenge)
            XCTAssertTrue(state.width >= 10, "Lưới Challenge phải từ 10x10 trở lên")
            XCTAssertTrue(state.snakes.count >= 10, "Số rắn Challenge phải nhiều")
            let solvable = SnakeEscapeEngine.isSolvable(state: state)
            XCTAssertTrue(solvable, "Challenge level \(challengeLvl) phải giải được!")
        }
    }
}
