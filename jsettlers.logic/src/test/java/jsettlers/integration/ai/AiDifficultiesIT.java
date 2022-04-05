/*******************************************************************************
 * Copyright (c) 2016 - 2017
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"),
 * to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 *******************************************************************************/
package jsettlers.integration.ai;

import jsettlers.logic.constants.Constants;
import jsettlers.logic.map.loading.MapLoader;
import jsettlers.logic.utils.MapUtils;

import org.junit.Test;

import jsettlers.common.CommonConstants;
import jsettlers.common.ai.EPlayerType;
import jsettlers.common.menu.IStartedGame;
import jsettlers.common.player.ECivilisation;
import jsettlers.logic.constants.MatchConstants;
import jsettlers.logic.map.loading.MapLoadException;
import jsettlers.logic.player.PlayerSetting;
import jsettlers.main.JSettlersGame;
import jsettlers.main.replay.ReplayUtils;
import jsettlers.logic.utils.TestUtils;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static jsettlers.integration.ai.AiTestUtils.*;

/**
 * @author codingberlin
 */
@RunWith(Parameterized.class)
public class AiDifficultiesIT {

	static {
		CommonConstants.ENABLE_CONSOLE_LOGGING = true;
		Constants.FOG_OF_WAR_DEFAULT_ENABLED = false;

		TestUtils.setupTempResourceManager();
	}


	@Parameterized.Parameters(name = "{index}: {0}")
	public static Object[] civilisations() {
		return ECivilisation.VALUES;
	}

	private final ECivilisation civilisation;

	public AiDifficultiesIT(ECivilisation civilisation) {
		this.civilisation = civilisation;
	}

	@Test
	public void easyShouldConquerVeryEasy() throws MapLoadException {
		holdBattleBetween(EPlayerType.AI_EASY, EPlayerType.AI_VERY_EASY, civilisation, 90 * MINUTES, MapUtils.getSpezialSumpf());
	}

	@Test
	public void hardShouldConquerEasy() throws MapLoadException {
		holdBattleBetween(EPlayerType.AI_HARD, EPlayerType.AI_EASY, civilisation, 75 * MINUTES, MapUtils.getSpezialSumpf());
	}

	@Test
	public void veryHardShouldConquerHard() throws MapLoadException {
		holdBattleBetween(EPlayerType.AI_VERY_HARD, EPlayerType.AI_HARD, civilisation, 75 * MINUTES, MapUtils.getSpezialSumpf());
	}

	@Test
	public void veryHardShouldProduceCertainAmountOfSoldiersWithin90Minutes() throws MapLoadException {
		byte playerId = (byte) 0;
		PlayerSetting[] playerSettings = getDefaultPlayerSettings(12);
		playerSettings[playerId] = new PlayerSetting(EPlayerType.AI_VERY_HARD, civilisation, playerId);

		JSettlersGame.GameRunner startingGame = createStartingGame(playerSettings, MapUtils.getSpezialSumpf());
		IStartedGame startedGame = ReplayUtils.waitForGameStartup(startingGame);

		MatchConstants.clock().fastForwardTo(90 * MINUTES);

		short expectedMinimalProducedSoldiers = 760;
		short producedSoldiers = startingGame.getMainGrid().getPartitionsGrid().getPlayer(0).getEndgameStatistic().getAmountOfProducedSoldiers();
		if (producedSoldiers < expectedMinimalProducedSoldiers) {
			stopAndFail("AI_VERY_HARD was not able to produce " + expectedMinimalProducedSoldiers + " soldiers within 90 minutes.\nOnly " + producedSoldiers
					+ " soldiers were produced. Some code changes make the AI weaker.", startedGame, startingGame.getMainGrid(), playerId);
		} else {
			System.out.println("The ai produced " + producedSoldiers + " soldiers.");
		}
		ReplayUtils.awaitShutdown(startedGame);
		ensureRuntimePerformance("to apply light rules", startingGame.getAiExecutor().getApplyLightRulesStopWatch(), 20, 250);
		ensureRuntimePerformance("to apply heavy rules", startingGame.getAiExecutor().getApplyHeavyRulesStopWatch(), 200, 2500);
		ensureRuntimePerformance("to update statistics", startingGame.getAiExecutor().getUpdateStatisticsStopWatch(), 100, 2500);
	}
}
