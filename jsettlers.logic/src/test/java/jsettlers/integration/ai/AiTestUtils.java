package jsettlers.integration.ai;

import java.util.Arrays;
import java.util.Locale;
import java.util.concurrent.Executors;
import jsettlers.ai.highlevel.AiStatistics;
import jsettlers.common.ai.EPlayerType;
import jsettlers.common.logging.StatisticsStopWatch;
import jsettlers.common.menu.IStartedGame;
import jsettlers.common.player.ECivilisation;
import jsettlers.logic.constants.MatchConstants;
import jsettlers.logic.map.grid.MainGrid;
import jsettlers.logic.map.loading.MapLoadException;
import jsettlers.logic.map.loading.MapLoader;
import jsettlers.logic.player.InitialGameState;
import jsettlers.logic.player.PlayerSetting;
import jsettlers.logic.utils.MapUtils;
import jsettlers.main.JSettlersGame;
import jsettlers.main.replay.ReplayUtils;
import jsettlers.network.client.OfflineNetworkConnector;

import org.junit.Assert;

public class AiTestUtils {
	static final int MINUTES = 1000 * 60;
	static final int JUMP_FORWARD = 2 * MINUTES;
	static final String LOW_PERFORMANCE_FAILURE_MESSAGE = "%s's %s is higher than %d. It was %d\nSome code change caused the AI to have a worse runtime performance.";

	public static void holdBattleBetween(EPlayerType expectedWinner, EPlayerType expectedLooser, ECivilisation civilisation, int maximumTimeToWin, MapLoader map) throws MapLoadException {
		byte expectedWinnerSlotId = 9;
		byte expectedLooserSlotId = 7;
		PlayerSetting[] playerSettings = getDefaultPlayerSettings(12);
		playerSettings[expectedWinnerSlotId] = new PlayerSetting(expectedWinner, civilisation, (byte) 0);
		playerSettings[expectedLooserSlotId] = new PlayerSetting(expectedLooser, civilisation, (byte) 1);

		JSettlersGame.GameRunner startingGame = createStartingGame(playerSettings, map);
		IStartedGame startedGame = ReplayUtils.waitForGameStartup(startingGame);
		AiStatistics aiStatistics = new AiStatistics(startingGame.getMainGrid(), Executors.newWorkStealingPool());

		int targetGameTime = 0;
		do {
			targetGameTime += JUMP_FORWARD;
			MatchConstants.clock().fastForwardTo(targetGameTime);
			aiStatistics.updateStatistics();
			if (!aiStatistics.isAlive(expectedWinnerSlotId)) {
				stopAndFail(expectedWinner + " was defeated by " + expectedLooser, startedGame, startingGame.getMainGrid(), expectedWinnerSlotId);
			}
			if (MatchConstants.clock().getTime() > maximumTimeToWin) {
				stopAndFail(expectedWinner + " was not able to defeat " + expectedLooser + " within " + (maximumTimeToWin / 60000)
						+ " minutes.\nIf the AI code was changed in a way which makes the " + expectedLooser + " stronger with the sideeffect that "
						+ "the " + expectedWinner + " needs more time to win you could make the " + expectedWinner + " stronger, too, or increase "
						+ "the maximumTimeToWin.", startedGame, startingGame.getMainGrid(), expectedWinnerSlotId);
			}
		} while (aiStatistics.isAlive(expectedLooserSlotId));
		System.out.println("The battle between " + expectedWinner + " and " + expectedLooser + " took " + (MatchConstants.clock().getTime() / 60000) +
				" minutes.");
		ReplayUtils.awaitShutdown(startedGame);

		ensureRuntimePerformance("to apply light rules", startingGame.getAiExecutor().getApplyLightRulesStopWatch(), 20, 300);
		ensureRuntimePerformance("to apply heavy rules", startingGame.getAiExecutor().getApplyHeavyRulesStopWatch(), 200, 3000);
		ensureRuntimePerformance("to update statistics", startingGame.getAiExecutor().getUpdateStatisticsStopWatch(), 100, 2500);
	}

	public static void ensureRuntimePerformance(String description, StatisticsStopWatch stopWatch, long median, int max) {
		System.out.println(description + ": " + stopWatch);
		if (stopWatch.getMedian() > median) {
			String medianText = String.format(Locale.ENGLISH, LOW_PERFORMANCE_FAILURE_MESSAGE, description, "median", median, stopWatch.getMedian());
			System.out.println(medianText);
			Assert.fail(medianText);
		}
		if (stopWatch.getMax() > max) {
			String maxText = String.format(Locale.ENGLISH, LOW_PERFORMANCE_FAILURE_MESSAGE, description, "max", max, stopWatch.getMax());
			System.out.println(maxText);
			Assert.fail(maxText);
		}
	}

	public static JSettlersGame.GameRunner createStartingGame(PlayerSetting[] playerSettings, MapLoader map) throws MapLoadException {
		byte playerId = 0;
		for (byte i = 0; i < playerSettings.length; i++) {
			if (playerSettings[i].isAvailable()) {
				playerId = i;
				break;
			}
		}

		JSettlersGame game = new JSettlersGame(map, new OfflineNetworkConnector(), new InitialGameState(playerId, playerSettings, 1L));
		return (JSettlersGame.GameRunner) game.start();
	}

	public static void stopAndFail(String reason, IStartedGame startedGame, MainGrid mainGrid, byte playerId) {
		MapLoader savegame = MapUtils.saveMainGrid(mainGrid, playerId, null);
		System.out.println("Saved game at: " + savegame.getListedMap().getFile());

		ReplayUtils.awaitShutdown(startedGame);
		Assert.fail(reason);
	}

	public static PlayerSetting[] getDefaultPlayerSettings(int numberOfPlayers) {
		PlayerSetting[] playerSettings = new PlayerSetting[numberOfPlayers];
		Arrays.fill(playerSettings, 0, numberOfPlayers, new PlayerSetting());
		return playerSettings;
	}
}