/*******************************************************************************
 * Copyright (c) 2015
 * <p/>
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"),
 * to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 * <p/>
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 * <p/>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 *******************************************************************************/
package jsettlers.integration.ai;

import static org.junit.Assert.fail;

import org.junit.Ignore;
import org.junit.Test;

import jsettlers.common.map.MapLoadException;
import jsettlers.testutils.TestUtils;
import jsettlers.ai.highlevel.AiStatistics;
import jsettlers.common.CommonConstants;
import jsettlers.common.ai.EPlayerType;
import jsettlers.common.buildings.EBuildingType;
import jsettlers.common.logging.StatisticsStopWatch;
import jsettlers.common.menu.IStartedGame;
import jsettlers.common.player.ECivilisation;
import jsettlers.input.PlayerState;
import jsettlers.logic.constants.MatchConstants;
import jsettlers.logic.map.loading.MapLoader;
import jsettlers.logic.map.loading.list.MapList;
import jsettlers.logic.player.PlayerSetting;
import jsettlers.main.JSettlersGame;
import jsettlers.main.replay.ReplayUtils;
import jsettlers.network.client.OfflineNetworkConnector;
import jsettlers.testutils.map.MapUtils;

/**
 * @author codingberlin
 */
public class AiDifficultiesIT {
	public static final int MINUTES = 1000 * 60;
	public static final int JUMP_FORWARD = 2 * MINUTES;

	static {
		CommonConstants.ENABLE_CONSOLE_LOGGING = true;

		TestUtils.setupTempResourceManager();
	}

	@Test
	public void easyShouldConquerVeryEasy() throws MapLoadException {
		holdBattleBetween(EPlayerType.AI_EASY, EPlayerType.AI_VERY_EASY, 70 * MINUTES);
	}

	@Test
	@Ignore // currently disabled as the difficulty levels still need to be balanced.
	public void hardShouldConquerEasy() throws MapLoadException {
		holdBattleBetween(EPlayerType.AI_HARD, EPlayerType.AI_EASY, 70 * MINUTES);
	}

	@Test
	public void veryHardShouldConquerHard() throws MapLoadException {
		holdBattleBetween(EPlayerType.AI_VERY_HARD, EPlayerType.AI_HARD, 82 * MINUTES);
	}

	@Test
	public void veryHardShouldProduceCertainAmountOfSoldiersWithin85Minutes() throws MapLoadException {
		PlayerSetting[] playerSettings = new PlayerSetting[4];
		playerSettings[0] = new PlayerSetting(true, EPlayerType.AI_VERY_HARD, ECivilisation.ROMAN, (byte) 0);
		playerSettings[1] = new PlayerSetting(false, (byte) -1);
		playerSettings[2] = new PlayerSetting(false, (byte) -1);
		playerSettings[3] = new PlayerSetting(false, (byte) -1);
		JSettlersGame.GameRunner startingGame = createStartingGame(playerSettings);
		IStartedGame startedGame = ReplayUtils.waitForGameStartup(startingGame);

		MatchConstants.clock().fastForwardTo(85 * MINUTES);

		ReplayUtils.awaitShutdown(startedGame);

		short expectedMinimalProducedSoldiers = 300;
		short producedSoldiers = startingGame.getMainGrid().getPartitionsGrid().getPlayer(0).getEndgameStatistic().getAmountOfProducedSoldiers();
		if (producedSoldiers < expectedMinimalProducedSoldiers) {
			fail("AI_VERY_HARD was not able to produce " + expectedMinimalProducedSoldiers + " within 90 minutes.\nOnly " + producedSoldiers + " "
					+ "soldiers were produced. Some code changes make the AI weaker.");
		}
		ensureRuntimePerformance("to apply rules", startingGame.getAiExecutor().getApplyRulesStopWatch(), 50, 2500);
		ensureRuntimePerformance("tp update statistics", startingGame.getAiExecutor().getUpdateStatisticsStopWatch(), 50, 2500);
	}

	private void holdBattleBetween(EPlayerType expectedWinner, EPlayerType expectedLooser, int maximumTimeToWin) throws MapLoadException {
		PlayerSetting[] playerSettings = new PlayerSetting[4];
		playerSettings[0] = new PlayerSetting(true, expectedLooser, ECivilisation.ROMAN, (byte) 0);
		playerSettings[1] = new PlayerSetting(true, expectedWinner, ECivilisation.ROMAN, (byte) 1);
		playerSettings[2] = new PlayerSetting(false, (byte) -1);
		playerSettings[3] = new PlayerSetting(false, (byte) -1);

		JSettlersGame.GameRunner startingGame = createStartingGame(playerSettings);
		IStartedGame startedGame = ReplayUtils.waitForGameStartup(startingGame);
		AiStatistics aiStatistics = new AiStatistics(startingGame.getMainGrid());

		int targetGameTime = 0;
		do {
			targetGameTime += JUMP_FORWARD;
			MatchConstants.clock().fastForwardTo(targetGameTime);
			aiStatistics.updateStatistics();
			if (aiStatistics.getNumberOfBuildingTypeForPlayer(EBuildingType.TOWER, (byte) 1) == 0) {
				stopAndFail(expectedWinner + " was defeated by " + expectedLooser, startedGame);
			}
			if (MatchConstants.clock().getTime() > maximumTimeToWin) {
				MapLoader savegame = MapUtils.saveMainGrid(startingGame.getMainGrid(), new PlayerState[]{new PlayerState((byte) 0, null), new PlayerState((byte) 1, null)});
				System.out.println("Saved game at: " + savegame.getListedMap().getFile());
				stopAndFail(expectedWinner + " was not able to defeat " + expectedLooser + " within " + (maximumTimeToWin / 60000)
						+ " minutes.\nIf the AI code was changed in a way which makes the " + expectedLooser + " stronger with the sideeffect that "
						+ "the " + expectedWinner + " needs more time to win you could make the " + expectedWinner + " stronger, too, or increase "
						+ "the maximumTimeToWin.", startedGame);
			}
		} while (aiStatistics.getNumberOfBuildingTypeForPlayer(EBuildingType.TOWER, (byte) 0) > 0);
		System.out.println("The battle between " + expectedWinner + " and " + expectedLooser + " took " + (MatchConstants.clock().getTime() / 60000) +
				" minutes.");
		ReplayUtils.awaitShutdown(startedGame);

		ensureRuntimePerformance("to apply rules", startingGame.getAiExecutor().getApplyRulesStopWatch(), 50, 3000);
		ensureRuntimePerformance("tp update statistics", startingGame.getAiExecutor().getUpdateStatisticsStopWatch(), 50, 2500);
	}

	private void ensureRuntimePerformance(String description, StatisticsStopWatch stopWatch, long median, int max) {
		System.out.println(description + ": " + stopWatch);
		if (stopWatch.getMedian() > median) {
			fail(description + "'s median is higher than " + median + ". It was " + stopWatch.getMedian() + ".\nSomething in the code changed which "
					+ "caused the AI to have a worse runtime performance.");
		}
		if (stopWatch.getMax() > max) {
			fail(description + "'s max is higher than " + max + ". It was " + stopWatch.getMax() + ".\nSomething in the code changed which "
					+ "caused the AI to have a worse runtime performance.");
		}
	}

	private JSettlersGame.GameRunner createStartingGame(PlayerSetting[] playerSettings) throws MapLoadException {
		MapLoader mapCreator = MapUtils.getMountainlake();
		JSettlersGame game = new JSettlersGame(mapCreator, 2L, new OfflineNetworkConnector(), (byte) 0, playerSettings);
		return (JSettlersGame.GameRunner) game.start();
	}

	private void stopAndFail(String reason, IStartedGame startedGame) {
		ReplayUtils.awaitShutdown(startedGame);
		fail(reason);
	}

}