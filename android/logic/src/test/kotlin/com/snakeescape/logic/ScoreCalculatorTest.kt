package com.snakeescape.logic

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class ScoreCalculatorTest {

    @Test
    fun testBaseScoreIncreasesWithLevel() {
        val scoreLv1 = ScoringConfig.getBaseScore(1)
        val scoreLv10 = ScoringConfig.getBaseScore(10)
        val scoreLv50 = ScoringConfig.getBaseScore(50)

        assertEquals(110, scoreLv1)
        assertEquals(200, scoreLv10)
        assertEquals(600, scoreLv50)
        assertTrue(scoreLv50 > scoreLv10)
        assertTrue(scoreLv10 > scoreLv1)
    }

    @Test
    fun testSpeedMultiplierFastVsSlow() {
        val timeLimit = 180

        // Solve instantly (0 seconds) -> multiplier 2.0
        val fastMult = ScoringConfig.getSpeedMultiplier(0.0, timeLimit)
        assertEquals(2.0, fastMult, 0.001)

        // Solve at half time (90 seconds) -> multiplier 1.25
        val midMult = ScoringConfig.getSpeedMultiplier(90.0, timeLimit)
        assertEquals(1.25, midMult, 0.001)

        // Solve at time limit (180 seconds) -> multiplier 0.5
        val slowMult = ScoringConfig.getSpeedMultiplier(180.0, timeLimit)
        assertEquals(0.5, slowMult, 0.001)

        assertTrue(fastMult > midMult)
        assertTrue(midMult > slowMult)
    }

    @Test
    fun testFastSolverGetsHigherScoreThanSlowSolver() {
        val level = 5
        val limit = 180

        val fastResult = ScoreCalculator.calculate(
            level = level,
            solveTimeSeconds = 15.0,
            timeLimitSeconds = limit
        )

        val slowResult = ScoreCalculator.calculate(
            level = level,
            solveTimeSeconds = 170.0,
            timeLimitSeconds = limit
        )

        assertTrue(
            fastResult.finalScore > slowResult.finalScore,
            "Fast solver ($fastResult) must score higher than slow solver ($slowResult)"
        )
    }

    @Test
    fun testDeductionsForHintsAndGridVisionAndHearts() {
        val level = 3
        val limit = 180
        val solveTime = 30.0

        val cleanResult = ScoreCalculator.calculate(
            level = level,
            solveTimeSeconds = solveTime,
            timeLimitSeconds = limit,
            hintsUsed = 0,
            gridVisionsUsed = 0,
            heartsLost = 0
        )

        val penalizedResult = ScoreCalculator.calculate(
            level = level,
            solveTimeSeconds = solveTime,
            timeLimitSeconds = limit,
            hintsUsed = 1, // -25
            gridVisionsUsed = 1, // -25
            heartsLost = 1 // -20
        )

        assertEquals(70, cleanResult.finalScore - penalizedResult.finalScore)
        assertEquals(25, penalizedResult.hintPenalty)
        assertEquals(25, penalizedResult.gridVisionPenalty)
        assertEquals(20, penalizedResult.heartPenalty)
    }

    @Test
    fun testScoreNeverNegative() {
        val result = ScoreCalculator.calculate(
            level = 1,
            solveTimeSeconds = 180.0,
            timeLimitSeconds = 180,
            hintsUsed = 10,
            gridVisionsUsed = 10,
            heartsLost = 2
        )
        assertTrue(result.finalScore >= 0, "Score must not be negative")
    }
}
