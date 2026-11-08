import XCTest
@testable import SnakeEscape

final class GameSessionTests: XCTestCase {

    func testRunStartsAtLevel1WithZeroScoreAndThreeHearts() {
        let session = GameSession()
        let state = session.startNewRun()

        XCTAssertEqual(state.currentLevel, 1)
        XCTAssertEqual(state.accumulatedScore, 0)
        XCTAssertEqual(state.totalTimeMs, 0)
        XCTAssertEqual(state.levelsCleared, 0)
        XCTAssertEqual(state.levelHearts, 3)
        XCTAssertTrue(state.isLevelInProgress)
    }

    func testLevelFailureDecrementsHeartAndAllowsRetry() {
        let session = GameSession()
        session.startNewRun()

        let res1 = session.onLevelFailure(playerName: "Tester")
        if case .canRetry(let hearts) = res1 {
            XCTAssertEqual(hearts, 2)
            XCTAssertEqual(session.state.levelHearts, 2)
        } else {
            XCTFail("Expected canRetry on first failure")
        }

        let res2 = session.onLevelFailure(playerName: "Tester")
        if case .canRetry(let hearts) = res2 {
            XCTAssertEqual(hearts, 1)
            XCTAssertEqual(session.state.levelHearts, 1)
        } else {
            XCTFail("Expected canRetry on second failure")
        }
    }

    func testLosingThreeHeartsEndsRun() {
        let session = GameSession()
        session.startNewRun()

        _ = session.onLevelFailure(playerName: "Tester")
        _ = session.onLevelFailure(playerName: "Tester")
        let res3 = session.onLevelFailure(playerName: "Tester")

        if case .runOver(let summary) = res3 {
            XCTAssertEqual(summary.playerName, "Tester")
            XCTAssertEqual(summary.levelsCleared, 0)
            XCTAssertEqual(summary.totalScore, 0)
            XCTAssertFalse(session.state.isLevelInProgress)
        } else {
            XCTFail("Expected runOver on 3rd failure")
        }

        let newRun = session.startNewRun()
        XCTAssertEqual(newRun.currentLevel, 1)
        XCTAssertEqual(newRun.accumulatedScore, 0)
        XCTAssertEqual(newRun.levelHearts, 3)
    }

    func testEnteringNewLevelResetsHeartsToThreeAndTimer() {
        let session = GameSession()
        session.startNewRun()

        _ = session.onLevelFailure(playerName: "Tester")
        XCTAssertEqual(session.state.levelHearts, 2)

        _ = session.onLevelSuccess(solveTimeSeconds: 20.0, timeLimitSeconds: 180, playerName: "Tester")
        XCTAssertEqual(session.state.levelsCleared, 1)

        let lv2 = session.enterLevel(2)
        XCTAssertEqual(lv2.levelHearts, 3, "Entering new level MUST reset hearts to 3")
        XCTAssertEqual(lv2.heartsLostInLevel, 0)
        XCTAssertEqual(lv2.hintsUsedInLevel, 0)
        XCTAssertEqual(lv2.gridVisionsUsedInLevel, 0)
    }

    func testMidGameExitPenalty() {
        let session = GameSession()
        session.startNewRun()
        XCTAssertTrue(session.state.isLevelInProgress)

        let penalty = session.applyMidGameExitPenalty(playerName: "Cheater")
        XCTAssertNotNil(penalty)
        if case .canRetry(let hearts) = penalty {
            XCTAssertEqual(hearts, 2)
            XCTAssertEqual(session.state.levelHearts, 2)
        } else {
            XCTFail("Expected canRetry penalty")
        }
    }
}
