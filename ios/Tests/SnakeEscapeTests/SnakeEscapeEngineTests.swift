import XCTest
@testable import SnakeEscape

final class SnakeEscapeEngineTests: XCTestCase {

    /// 1. Test rắn đi thẳng thoát ra khỏi lưới an toàn
    func testStraightEscape() {
        // Rắn tại (2, 2) hướng UP trên bàn cờ 5x5 không có vật cản
        let snake = Snake(
            id: 1,
            body: [Cell(2, 2), Cell(2, 3), Cell(2, 4)],
            direction: .up
        )
        let state = GameState(width: 5, height: 5, snakes: [snake])

        XCTAssertTrue(SnakeEscapeEngine.canEscape(state: state, snakeId: 1))

        let result = SnakeEscapeEngine.tryMove(state: state, snakeId: 1)
        guard case let .escaped(newState, snakeId, _) = result else {
            XCTFail("Expected .escaped, got \(result)")
            return
        }

        XCTAssertEqual(snakeId, 1)
        XCTAssertTrue(newState.snakes.isEmpty)
        XCTAssertEqual(newState.hearts, 3)
        XCTAssertTrue(SnakeEscapeEngine.isWon(state: newState))
    }

    /// 2. Test rắn bị chặn bởi thân con rắn khác
    func testBlockedByOtherSnake() {
        // Snake 1 tại (2, 2) hướng UP
        // Snake 2 nằm ngang tại (2, 1) chặn ngay phía trước mặt Snake 1
        let snake1 = Snake(id: 1, body: [Cell(2, 2), Cell(2, 3)], direction: .up)
        let snake2 = Snake(id: 2, body: [Cell(2, 1), Cell(1, 1)], direction: .left)
        let state = GameState(width: 5, height: 5, snakes: [snake1, snake2], hearts: 3)

        XCTAssertFalse(SnakeEscapeEngine.canEscape(state: state, snakeId: 1))

        let result = SnakeEscapeEngine.tryMove(state: state, snakeId: 1)
        guard case let .blocked(newState, blockedId, _) = result else {
            XCTFail("Expected .blocked, got \(result)")
            return
        }

        XCTAssertEqual(blockedId, 1)
        XCTAssertEqual(newState.snakes.count, 2)
        XCTAssertEqual(newState.hearts, 2) // Mất 1 tim
    }

    /// 3. Test rắn bị chặn bởi ô bị khoá (Locked cell)
    func testBlockedByLockedCell() {
        let snake = Snake(id: 1, body: [Cell(2, 2), Cell(2, 3)], direction: .up)
        let state = GameState(
            width: 5,
            height: 5,
            snakes: [snake],
            lockedCells: [Cell(2, 1)]
        )

        XCTAssertFalse(SnakeEscapeEngine.canEscape(state: state, snakeId: 1))
    }

    /// 4. Test thân rắn đi theo đúng đường cong / quỹ đạo của đầu
    func testBodyFollowsHeadPath() {
        // Rắn hình chữ L: Head (2, 2), (2, 3), Tail (3, 3), hướng UP
        let snake = Snake(
            id: 1,
            body: [Cell(2, 2), Cell(2, 3), Cell(3, 3)],
            direction: .up
        )

        let steps = SnakeEscapeEngine.simulateSnakeEscapeSteps(snake: snake, width: 5, height: 5)
        // Step 0: [ (2,2), (2,3), (3,3) ]
        XCTAssertEqual(steps[0].body, [Cell(2, 2), Cell(2, 3), Cell(3, 3)])

        // Step 1: Head tiến tới (2, 1), đốt 1 tới (2, 2), đốt 2 tới (2, 3)
        XCTAssertEqual(steps[1].body, [Cell(2, 1), Cell(2, 2), Cell(2, 3)])

        // Step 2: Head tiến tới (2, 0), đốt 1 tới (2, 1), đốt 2 tới (2, 2)
        XCTAssertEqual(steps[2].body, [Cell(2, 0), Cell(2, 1), Cell(2, 2)])

        // Step 3: Head ra ngoài (2, -1)
        XCTAssertEqual(steps[3].head, Cell(2, -1))
        XCTAssertFalse(steps[3].isCompletelyOutside(width: 5, height: 5))

        // Bước cuối cùng: Toàn bộ rời lưới
        XCTAssertTrue(steps.last!.isCompletelyOutside(width: 5, height: 5))
    }

    /// 5. Test tính năng Undo: khôi phục rắn và tim
    func testUndoMoves() {
        let snake1 = Snake(id: 1, body: [Cell(0, 0)], direction: .up)
        let snake2 = Snake(id: 2, body: [Cell(2, 2), Cell(2, 3)], direction: .up)
        let state0 = GameState(
            width: 5,
            height: 5,
            snakes: [snake1, snake2],
            lockedCells: [Cell(2, 1)],
            hearts: 3
        )

        // Nước 1: Snake 1 thoát
        guard case let .escaped(state1, _, _) = SnakeEscapeEngine.tryMove(state: state0, snakeId: 1) else {
            XCTFail(); return
        }
        XCTAssertEqual(state1.snakes.count, 1)

        // Nước 2: Snake 2 bị chặn -> mất 1 tim
        guard case let .blocked(state2, _, _) = SnakeEscapeEngine.tryMove(state: state1, snakeId: 2) else {
            XCTFail(); return
        }
        XCTAssertEqual(state2.hearts, 2)

        // Undo 1: Phục hồi tim về 3
        let undo1 = SnakeEscapeEngine.undo(state: state2)
        XCTAssertEqual(undo1.hearts, 3)
        XCTAssertEqual(undo1.snakes.count, 1)

        // Undo 2: Phục hồi lại Snake 1
        let undo2 = SnakeEscapeEngine.undo(state: undo1)
        XCTAssertEqual(undo2.snakes.count, 2)
        XCTAssertEqual(undo2.snakes.map { $0.id }.sorted(), [1, 2])
    }

    /// 6. Test quan hệ phụ thuộc thứ tự thoát (Topological Order) & Hint
    func testTopologicalDependencyAndHint() {
        // Snake 1 tại (2, 1) hướng RIGHT -> thoát ra biên x=4
        // Snake 2 tại (2, 2) hướng UP -> bị chặn bởi Snake 1 tại (2, 1)
        let snake1 = Snake(id: 1, body: [Cell(2, 1)], direction: .right)
        let snake2 = Snake(id: 2, body: [Cell(2, 2)], direction: .up)
        let state = GameState(width: 5, height: 5, snakes: [snake1, snake2])

        // Snake 2 bị chặn, Snake 1 thoát được
        XCTAssertFalse(SnakeEscapeEngine.canEscape(state: state, snakeId: 2))
        XCTAssertTrue(SnakeEscapeEngine.canEscape(state: state, snakeId: 1))

        // Hint phải gợi ý Snake 1
        XCTAssertEqual(SnakeEscapeEngine.hint(state: state), 1)

        // Giải được bằng DFS
        XCTAssertTrue(SnakeEscapeEngine.isSolvable(state: state))
    }

    /// 7. Test chu trình bế tắc (Deadlock Cycle) không thể giải
    func testDeadlockCycle() {
        // 4 con rắn chặn đầu nhau theo vòng tròn: UP -> RIGHT -> DOWN -> LEFT -> UP
        let s1 = Snake(id: 1, body: [Cell(2, 2)], direction: .up)     // Đầu hướng tới (2, 1)
        let s2 = Snake(id: 2, body: [Cell(2, 1)], direction: .right)  // Đầu hướng tới (3, 1)
        let s3 = Snake(id: 3, body: [Cell(3, 1)], direction: .down)   // Đầu hướng tới (3, 2)
        let s4 = Snake(id: 4, body: [Cell(3, 2)], direction: .left)   // Đầu hướng tới (2, 2)

        let state = GameState(width: 5, height: 5, snakes: [s1, s2, s3, s4])

        // Không con nào thoát được
        XCTAssertFalse(SnakeEscapeEngine.canEscape(state: state, snakeId: 1))
        XCTAssertFalse(SnakeEscapeEngine.canEscape(state: state, snakeId: 2))
        XCTAssertFalse(SnakeEscapeEngine.canEscape(state: state, snakeId: 3))
        XCTAssertFalse(SnakeEscapeEngine.canEscape(state: state, snakeId: 4))

        XCTAssertFalse(SnakeEscapeEngine.isSolvable(state: state))
        XCTAssertNil(SnakeEscapeEngine.hint(state: state))
    }
}
