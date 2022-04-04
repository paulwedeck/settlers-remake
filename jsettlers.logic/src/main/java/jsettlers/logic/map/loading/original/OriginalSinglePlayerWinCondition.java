/*******************************************************************************
 * Copyright (c) 2020
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
package jsettlers.logic.map.loading.original;

import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.BitSet;
import java.util.Set;
import java.util.function.Supplier;

import jsettlers.common.buildings.EBuildingType;
import jsettlers.common.player.EWinState;
import jsettlers.common.position.ShortPoint2D;
import jsettlers.logic.buildings.Building;
import jsettlers.logic.constants.MatchConstants;
import jsettlers.logic.map.grid.MainGrid;
import jsettlers.logic.map.loading.WinLoseHandler;
import jsettlers.logic.map.loading.original.data.OriginalDestroyBuildingsWinCondition;
import jsettlers.logic.map.loading.original.data.OriginalProduceGoodsWinCondition;
import jsettlers.logic.map.loading.original.data.OriginalSurviveDurationWinCondition;
import jsettlers.logic.player.Player;

public class OriginalSinglePlayerWinCondition extends WinLoseHandler implements Serializable {
	private static final long serialVersionUID = 1;

	private static final byte MAIN_PLAYER = 0;
	private static final byte MAIN_TEAM = 0;

	private BitSet killToWin = new BitSet();
	private Set<ShortPoint2D> conquerToWin = Set.of();
	private Set<OriginalProduceGoodsWinCondition> produceToWin = Set.of();
	private Set<OriginalSurviveDurationWinCondition> surviveToWin = Set.of();
	private Set<OriginalDestroyBuildingsWinCondition> destroyToWin = Set.of();

	private transient Set<Supplier<Boolean>> winConditions;
	private transient Set<Supplier<Boolean>> loseConditions;

	private void readObject(ObjectInputStream ois) {
		try {
			if (ois != null) ois.defaultReadObject();
		} catch (Exception ex) {
			ex.printStackTrace();
		}

		winConditions = Set.of(this::checkKillCondition, this::checkDestroyCondition, this::checkConquerCondition, this::checkSurviveWinCondition, this::checkProduceCondition);
		loseConditions = Set.of(this::checkSurviveLoseCondition);
	}

	public OriginalSinglePlayerWinCondition(MainGrid mainGrid) {
		super(mainGrid);
		readObject(null);
	}

	public void killPlayersToWin(BitSet killToWin) {
		this.killToWin = killToWin;
	}

	public void destroyAllBuildingsToWin(Set<OriginalDestroyBuildingsWinCondition> destroyToWin) {
		this.destroyToWin = destroyToWin;
	}

	public void conquerPositionsToWin(Set<ShortPoint2D> conquerToWin) {
		this.conquerToWin = conquerToWin;
	}

	public void surviveDurationToWin(Set<OriginalSurviveDurationWinCondition> surviveToWin) {
		this.surviveToWin = surviveToWin;
	}

	public void produceGoodsToWin(Set<OriginalProduceGoodsWinCondition> produceToWin) {
		this.produceToWin = produceToWin;
	}

	@Override
	public void updateWinLose() {
		if(killToWin.isEmpty() && destroyToWin.isEmpty() && conquerToWin.isEmpty() && surviveToWin.isEmpty() && produceToWin.isEmpty()) {
			// not all maps set win condition, the default one is just to kill all enemies
			playerStream().filter(player -> player.getTeamId() != MAIN_TEAM).forEach(player -> killToWin.set(player.getPlayerId()));
		}


		EWinState initMainState = players[MAIN_PLAYER].getWinState();

		// Update defeated status
		defeatDeadPlayers();

		// player can't win or lose if he already won
		if(initMainState == EWinState.WON) return;

		for(Supplier<Boolean> loseCondition : loseConditions) {
			if(loseCondition.get()) {
				// if player lost by any condition
				players[MAIN_PLAYER].setWinState(EWinState.LOST);
				break;
			}
		}

		// player lost
		if(players[MAIN_PLAYER].getWinState() == EWinState.LOST) {
			playerStream().filter(player -> player.getTeamId() != MAIN_TEAM)
					.forEach(player -> player.setWinState(EWinState.WON));

			playerStream().map(Player::getTeamId).distinct().filter(teamId -> teamId!=MAIN_TEAM).forEach(mainGrid::disableFogOfWar);
			return;
		}


		for(Supplier<Boolean> winCondition : winConditions) {
			if(!winCondition.get()) {
				// all conditions must be met to win
				return;
			}
		}

		// player won
		players[MAIN_PLAYER].setWinState(EWinState.WON);
		mainGrid.disableFogOfWar(MAIN_TEAM);

		playerStream().filter(player -> player.getTeamId() != MAIN_TEAM)
				.forEach(player -> player.setWinState(EWinState.LOST));
	}

	private boolean checkKillCondition() {
		if(killToWin == null) return true;

		for(int i = 0; i < players.length; i++) {
			// test failed if the player is alive but supposed to be dead
			if(players[i] != null && players[i].getWinState() == EWinState.UNDECIDED && killToWin.get(i)) return false;
		}

		return true;
	}

	private boolean checkDestroyCondition() {
		for(OriginalDestroyBuildingsWinCondition destroyBuildings : destroyToWin) {
			EBuildingType buildingType = destroyBuildings.getBuildingType();
			byte playerId = destroyBuildings.getPlayerId();

			// test failed if buildings of type by player exist
			if(Building.getAllBuildings().stream()
					.filter(building -> building.getPlayer().playerId == playerId)
					.anyMatch(building -> building.getBuildingVariant().isVariantOf(buildingType))) return false;
		}

		return true;
	}

	private boolean checkConquerCondition() {
		for(ShortPoint2D conquerPoint : conquerToWin) {
			if(mainGrid.getPartitionsGrid().getPlayerIdAt(conquerPoint.x, conquerPoint.y) != MAIN_PLAYER) return false;
		}

		return true;
	}

	private boolean checkSurviveLoseCondition() {
		int time = MatchConstants.clock().getTime();
		for(OriginalSurviveDurationWinCondition surviveTime : surviveToWin) {
			byte playerId = surviveTime.getPlayerId();

			// ???
			if(playerId >= players.length || players[playerId] == null) continue;

			if(players[playerId].getWinState() == EWinState.LOST && time < surviveTime.getTime()) return true;
		}

		return false;
	}

	private boolean checkSurviveWinCondition() {
		int time = MatchConstants.clock().getTime();
		for(OriginalSurviveDurationWinCondition surviveTime : surviveToWin) {
			byte playerId = surviveTime.getPlayerId();

			// ???
			if(playerId >= players.length || players[playerId] == null) continue;

			// we wouldn't be here if player has lost /  target player died
			if(time < surviveTime.getTime()) return false;
		}

		return true;
	}

	private boolean checkProduceCondition() {
		// TODO implement production statistics
		return true;
	}
}
