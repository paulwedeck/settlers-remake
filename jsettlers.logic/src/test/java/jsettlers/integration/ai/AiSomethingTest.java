package jsettlers.integration.ai;

import java.io.File;
import jsettlers.common.CommonConstants;
import jsettlers.common.ai.EPlayerType;
import jsettlers.common.menu.IStartedGame;
import jsettlers.common.player.ECivilisation;
import jsettlers.logic.constants.Constants;
import jsettlers.logic.constants.MatchConstants;
import jsettlers.logic.map.loading.MapLoadException;
import jsettlers.logic.map.loading.MapLoader;
import jsettlers.logic.map.loading.list.DirectoryMapLister;
import jsettlers.logic.player.PlayerSetting;
import jsettlers.main.JSettlersGame;
import jsettlers.main.replay.ReplayUtils;
import jsettlers.network.client.OfflineNetworkConnector;
import jsettlers.logic.utils.TestUtils;

import org.junit.Test;
import static jsettlers.integration.ai.AiTestUtils.MINUTES;
import static jsettlers.integration.ai.AiTestUtils.createStartingGame;
import static jsettlers.integration.ai.AiTestUtils.ensureRuntimePerformance;
import static jsettlers.integration.ai.AiTestUtils.getDefaultPlayerSettings;
import static jsettlers.integration.ai.AiTestUtils.stopAndFail;

public class AiSomethingTest {
/*
	static {
		CommonConstants.ENABLE_CONSOLE_LOGGING = true;
		Constants.FOG_OF_WAR_DEFAULT_ENABLED = false;

		TestUtils.setupTempResourceManager();
	}

	public static JSettlersGame.GameRunner createStartingGame(PlayerSetting[] playerSettings, MapLoader map) throws MapLoadException {
		byte playerId = 0;
		for (byte i = 0; i < playerSettings.length; i++) {
			if (playerSettings[i].isAvailable()) {
				playerId = i;
				break;
			}
		}

		JSettlersGame game = new JSettlersGame(map, 1L, new OfflineNetworkConnector(), playerId, playerSettings);
		return (JSettlersGame.GameRunner) game.start();
	}

	@Test
	public void testDoesAiDoSomething() throws MapLoadException {
		byte playerIdA = (byte) 0;
		//byte playerIdB = (byte) 1;
		//byte playerIdC = (byte) 2;
		PlayerSetting[] playerSettings = getDefaultPlayerSettings(6);
		playerSettings[playerIdA] = new PlayerSetting(EPlayerType.AI_VERY_HARD, ECivilisation.ROMAN, playerIdA);
		//playerSettings[playerIdB] = new PlayerSetting(EPlayerType.AI_VERY_HARD, ECivilisation.ROMAN, playerIdB);
		//playerSettings[playerIdC] = new PlayerSetting(EPlayerType.AI_VERY_HARD, ECivilisation.AMAZON, playerIdB);

		JSettlersGame.GameRunner startingGame = createStartingGame(playerSettings, MapLoader.getLoaderForListedMap(new DirectoryMapLister.ListedMapFile(new File("/home/paul/projects/settlers-remake/maps/release/500-6-6_players_fun.rmap"))));
		IStartedGame startedGame = ReplayUtils.waitForGameStartup(startingGame);

		MatchConstants.clock().fastForwardTo(90 * MINUTES);

		short expectedMinimalProducedSoldiers = 1000;
		short producedSoldiers = startingGame.getMainGrid().getPartitionsGrid().getPlayer(playerIdA).getEndgameStatistic().getAmountOfProducedSoldiers();
		//short producedSoldiersB = startingGame.getMainGrid().getPartitionsGrid().getPlayer(playerIdB).getEndgameStatistic().getAmountOfProducedSoldiers();
		//short producedSoldiersC = startingGame.getMainGrid().getPartitionsGrid().getPlayer(playerIdC).getEndgameStatistic().getAmountOfProducedSoldiers();
		if (producedSoldiers < expectedMinimalProducedSoldiers) {
			stopAndFail("AI_VERY_HARD was not able to produce " + expectedMinimalProducedSoldiers + " soldiers within 90 minutes.\nOnly " + producedSoldiers + "/"// + producedSoldiersB + "/" + producedSoldiersC
					+ " soldiers were produced. Some code changes make the AI weaker.", startedGame, startingGame.getMainGrid(), playerIdA);
		} else {
			System.out.println("The ai produced " + producedSoldiers + "/" +/* producedSoldiersB + "/" + producedSoldiersC +* " soldiers.");
		}
		ReplayUtils.awaitShutdown(startedGame);
		ensureRuntimePerformance("to apply light rules", startingGame.getAiExecutor().getApplyLightRulesStopWatch(), 20, 250);
		ensureRuntimePerformance("to apply heavy rules", startingGame.getAiExecutor().getApplyHeavyRulesStopWatch(), 200, 2500);
		ensureRuntimePerformance("to update statistics", startingGame.getAiExecutor().getUpdateStatisticsStopWatch(), 100, 2500);
	}*/
}
