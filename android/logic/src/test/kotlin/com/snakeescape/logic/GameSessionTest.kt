package com.snakeescape.logic

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class GameSessionTest {

    @Test
    fun testRunStartsAtLevel1WithZeroScoreAndThreeHearts() {
        val session = GameSession()
        val state = session.startNewRun()

        assertEquals(1, state.currentLevel)
        assertEquals(0L, state.accumulatedScore)
        assertEquals(0L, state.totalTimeMs)
        assertEquals(0, state.levelsCleared)
        assertEquals(3, state.levelHearts)
        assertTrue(state.isLevelInProgress)
    }

    @Test
    fun testLevelFailureDecrementsHeartAndAllowsRetry() {
        val session = GameSession()
        session.startNewRun()

        // 1st failure
        val result1 = session.onLevelFailure(playerName = "Tester")
        assertTrue(result1 is LevelFailureResult.CanRetry)
        assertEquals(2, (result1 as LevelFailureResult.CanRetry).heartsRemaining)
        assertEquals(2, session.state.levelHearts)

        // 2nd failure
        val result2 = session.onLevelFailure(playerName = "Tester")
        assertTrue(result2 is LevelFailureResult.CanRetry)
        assertEquals(1, (result2 as LevelFailureResult.CanRetry).heartsRemaining)
        assertEquals(1, session.state.levelHearts)
    }

    @Test
    fun testLosingThreeHeartsEndsRun() {
        val session = GameSession()
        session.startNewRun()

        // Fail 3 times in this level
        session.onLevelFailure(playerName = "Tester")
        session.onLevelFailure(playerName = "Tester")
        val result3 = session.onLevelFailure(playerName = "Tester")

        assertTrue(result3 is LevelFailureResult.RunOver, "3rd failure must terminate the run")
        val runOver = result3 as LevelFailureResult.RunOver
        assertEquals("Tester", runOver.summary.playerName)
        assertEquals(0, runOver.summary.levelsCleared)
        assertEquals(0L, runOver.summary.totalScore)
        assertFalse(session.state.isLevelInProgress)

        // Next run must restart from level 1 with 0 score
        val newRun = session.startNewRun()
        assertEquals(1, newRun.currentLevel)
        assertEquals(0L, newRun.accumulatedScore)
        assertEquals(3, newRun.levelHearts)
    }

    @Test
    fun testNewLevelResetsHeartsToThreeAndTimer() {
        val session = GameSession()
        session.startNewRun()

        // Suppose user failed once on level 1, leaving 2 hearts
        session.onLevelFailure(playerName = "Tester")
        assertEquals(2, session.state.levelHearts)

        // Succeed level 1
        session.onLevelSuccess(solveTimeSeconds = 20.0, timeLimitSeconds = 180, playerName = "Tester")
        assertEquals(1, session.state.levelsCleared)
        assertTrue(session.state.accumulatedScore > 0)

        // Enter level 2
        val lv2State = session.enterLevel(2)
        assertEquals(3, lv2State.levelHearts, "Entering new level MUST reset hearts to 3")
        assertEquals(0, lv2State.heartsLostInLevel)
        assertEquals(0, lv2State.hintsUsedInLevel)
        assertEquals(0, lv2State.gridVisionsUsedInLevel)
    }

    @Test
    fun testMidGameExitPenaltyDeductsHeart() {
        val session = GameSession()
        session.startNewRun()
        assertTrue(session.state.isLevelInProgress)

        // Player closed the app mid-level
        val penalty = session.applyMidGameExitPenalty(playerName = "CheatTester")
        assertNotNull(penalty)
        assertTrue(penalty is LevelFailureResult.CanRetry)
        assertEquals(2, (penalty as LevelFailureResult.CanRetry).heartsRemaining)
        assertEquals(2, session.state.levelHearts)

        // Exit two more times -> terminates run
        session.applyMidGameExitPenalty(playerName = "CheatTester")
        val finalPenalty = session.applyMidGameExitPenalty(playerName = "CheatTester")
        assertTrue(finalPenalty is LevelFailureResult.RunOver)
    }
}
