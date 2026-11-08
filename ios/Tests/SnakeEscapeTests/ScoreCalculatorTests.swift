import XCTest
@testable import SnakeEscape

final class ScoreCalculatorTests: XCTestCase {

    func testBaseScoreIncreasesWithLevel() {
        let lv1 = ScoringConfig.getBaseScore(level: 1)
        let lv10 = ScoringConfig.getBaseScore(level: 10)
        let lv50 = ScoringConfig.getBaseScore(level: 50)

        XCTAssertEqual(lv1, 110)
        XCTAssertEqual(lv10, 200)
        XCTAssertEqual(lv50, 600)
        XCTAssertTrue(lv50 > lv10)
        XCTAssertTrue(lv10 > lv1)
    }

    func testSpeedMultiplierFastVsSlow() {
        let limit = 180

        let fastMult = ScoringConfig.getSpeedMultiplier(timeElapsedSeconds: 0.0, timeLimitSeconds: limit)
        XCTAssertEqual(fastMult, 2.0, accuracy: 0.001)

        let midMult = ScoringConfig.getSpeedMultiplier(timeElapsedSeconds: 90.0, timeLimitSeconds: limit)
        XCTAssertEqual(midMult, 1.25, accuracy: 0.001)

        let slowMult = ScoringConfig.getSpeedMultiplier(timeElapsedSeconds: 180.0, timeLimitSeconds: limit)
        XCTAssertEqual(slowMult, 0.5, accuracy: 0.001)

        XCTAssertTrue(fastMult > midMult)
        XCTAssertTrue(midMult > slowMult)
    }

    func testFastSolverScoresHigherThanSlowSolver() {
        let level = 5
        let limit = 180

        let fast = ScoreCalculator.calculate(level: level, solveTimeSeconds: 15.0, timeLimitSeconds: limit)
        let slow = ScoreCalculator.calculate(level: level, solveTimeSeconds: 170.0, timeLimitSeconds: limit)

        XCTAssertTrue(fast.finalScore > slow.finalScore)
    }

    func testDeductionsForBoosterUsageAndHeartsLost() {
        let level = 3
        let limit = 180
        let time = 30.0

        let clean = ScoreCalculator.calculate(level: level, solveTimeSeconds: time, timeLimitSeconds: limit)
        let penalized = ScoreCalculator.calculate(
            level: level,
            solveTimeSeconds: time,
            timeLimitSeconds: limit,
            hintsUsed: 1, // -25
            gridVisionsUsed: 1, // -25
            heartsLost: 1 // -20
        )

        XCTAssertEqual(clean.finalScore - penalized.finalScore, 70)
        XCTAssertEqual(penalized.hintPenalty, 25)
        XCTAssertEqual(penalized.gridVisionPenalty, 25)
        XCTAssertEqual(penalized.heartPenalty, 20)
    }

    func testScoreNeverDropsBelowZero() {
        let result = ScoreCalculator.calculate(
            level: 1,
            solveTimeSeconds: 180.0,
            timeLimitSeconds: 180,
            hintsUsed: 10,
            gridVisionsUsed: 10,
            heartsLost: 2
        )
        XCTAssertTrue(result.finalScore >= 0)
    }
}
