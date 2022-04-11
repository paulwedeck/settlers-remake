/*
 * Copyright (c) 2015 - 2017
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
 */
package jsettlers.logic.buildings.military.occupying;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import jsettlers.algorithms.path.IPathCalculatable;
import jsettlers.algorithms.path.Path;
import jsettlers.algorithms.path.dijkstra.DijkstraAlgorithm.DijkstraContinuableRequest;
import jsettlers.common.CommonConstants;
import jsettlers.common.buildings.BuildingVariant;
import jsettlers.common.buildings.EBuildingType;
import jsettlers.common.buildings.IBuilding;
import jsettlers.common.buildings.IBuildingOccupier;
import jsettlers.common.buildings.OccupierPlace;
import jsettlers.common.map.shapes.FreeMapArea;
import jsettlers.common.map.shapes.MapCircle;
import jsettlers.common.mapobject.EMapObjectType;
import jsettlers.common.mapobject.IAttackableTowerMapObject;
import jsettlers.common.movable.EMovableType;
import jsettlers.common.movable.ESoldierClass;
import jsettlers.common.movable.ESoldierType;
import jsettlers.common.movable.IGraphicsMovable;
import jsettlers.common.player.ECivilisation;
import jsettlers.common.player.IPlayer;
import jsettlers.common.position.ShortPoint2D;
import jsettlers.common.utils.collections.map.ArrayListMap;
import jsettlers.common.menu.messages.SimpleMessage;
import jsettlers.logic.buildings.Building;
import jsettlers.logic.buildings.IBuildingsGrid;
import jsettlers.logic.constants.Constants;
import jsettlers.logic.constants.MatchConstants;
import jsettlers.logic.movable.interfaces.IAttackable;
import jsettlers.logic.movable.interfaces.IAttackableMovable;
import jsettlers.logic.movable.interfaces.ILogicMovable;
import jsettlers.logic.movable.interfaces.ISoldierMovable;
import jsettlers.logic.objects.StandardMapObject;
import jsettlers.logic.player.Player;

/**
 * This is a tower building that can request soldiers and let them defend the building.
 *
 * @author Andreas Eberle
 */
public class OccupyingBuilding extends Building implements IBuilding.IOccupied, IPathCalculatable, IOccupyableBuilding, Serializable {
	private static final long serialVersionUID = 5267249978497095473L;

	private static final int TIMER_PERIOD = 500;

	private final LinkedList<OccupierPlace> emptyPlaces = new LinkedList<>();
	private final SoldierRequests searchedSoldiers = new SoldierRequests();
	private final Map<ISoldierMovable, SoldierRequest> comingSoldiers = new ArrayListMap<>();
	private final LinkedList<TowerOccupier> sortedOccupiers = new LinkedList<>();
	private final LinkedList<TowerOccupier> toBeReleasedOccupiers = new LinkedList<>();
	private final ECivilisation civilisation;

	private DijkstraContinuableRequest dijkstraRequest;

	private boolean occupiedArea;
	private float doorHealth = 50f;
	private boolean inFight = false;
	private AttackableTowerMapObject attackableTowerObject = null;

	public OccupyingBuilding(EBuildingType type, Player player, ShortPoint2D position, IBuildingsGrid buildingsGrid) {
		super(type, player, position, buildingsGrid);
		civilisation = player.getCivilisation();

		initSoldierRequests();
	}

	@Override
	public BuildingVariant getBuildingVariant() {
		return type.getVariant(civilisation);
	}

	private void initSoldierRequests() {
		final OccupierPlace[] occupierPlaces = getBuildingVariant().getOccupierPlaces();
		if (occupierPlaces.length > 0) {
			emptyPlaces.addAll(Arrays.asList(occupierPlaces));
			requestSoldier(ESoldierClass.INFANTRY);
		}
	}

	@Override
	protected final int constructionFinishedEvent() {
		setAttackableTowerObject(true);
		return TIMER_PERIOD + MatchConstants.random().nextInt(200); // adding random prevents simultaneous scan after map creation
	}

	private void setAttackableTowerObject(boolean set) {
		if (set) {
			attackableTowerObject = new AttackableTowerMapObject(this);
			super.grid.getMapObjectsManager().addAttackableTowerObject(getDoor(), attackableTowerObject);
		} else {
			super.grid.getMapObjectsManager().removeMapObjectType(getDoor().x, getDoor().y, EMapObjectType.ATTACKABLE_TOWER);
			attackableTowerObject = null;
		}
	}

	private void changePlayerTo(ShortPoint2D attackerPos) {
		assert sortedOccupiers.isEmpty() : "there cannot be any occupiers in the tower when changing the player.";

		ILogicMovable attacker = super.grid.getMovable(attackerPos);
		Player newPlayer = attacker.getPlayer();

		setAttackableTowerObject(false);
		super.showFlag(false);

		resetSoldierSearch();

		super.setPlayer(newPlayer);

		if (occupiedArea) { // free the area if it had been occupied.
			super.grid.changePlayerOfTower(super.pos, newPlayer, getGroundArea());
		} else {
			occupyAreaIfNeeded();
		}

		ISoldierMovable attackingSoldier = (ISoldierMovable)attacker;
		initSoldierRequests();
		attackingSoldier.moveToTower(this);
		comingSoldiers.put(attackingSoldier, searchedSoldiers.removeOne(attackingSoldier.getMovableType().getSoldierType()));

		doorHealth = 0.1f;
		inFight = false;
		setOccupied(false);

		super.showFlag(true);
		setAttackableTowerObject(true);
	}

	private FreeMapArea getGroundArea() {
		return new FreeMapArea(super.pos, getBuildingVariant().getProtectedTiles());
	}

	private void resetSoldierSearch() {
		dijkstraRequest = null;
		searchedSoldiers.clear();
		emptyPlaces.clear();
		comingSoldiers.clear();
	}

	@Override
	protected final void appearedEvent() {
		occupyAreaIfNeeded();
	}

	@Override
	protected final EMapObjectType getFlagType() {
		return EMapObjectType.FLAG_DOOR;
	}

	@Override
	protected final int subTimerEvent() {
		increaseDoorHealth();
		releaseNextSoldierIfNeeded();
		searchSoldiersIfNeeded();
		return TIMER_PERIOD;
	}

	private void increaseDoorHealth() {
		if (doorHealth < 1 && !inFight) {
			doorHealth = Math.min(1, doorHealth + Constants.TOWER_DOOR_REGENERATION);
		}
	}

	private void releaseNextSoldierIfNeeded() {
		if (!toBeReleasedOccupiers.isEmpty()) {
			ILogicMovable movableAtDoor = grid.getMovable(super.getDoor());
			if (movableAtDoor == null) {
				TowerOccupier releasedOccupier = toBeReleasedOccupiers.pop();
				sortedOccupiers.remove(releasedOccupier);
				emptyPlaces.add(releasedOccupier.place);
				ISoldierMovable soldier = releasedOccupier.soldier;
				soldier.leaveTower(super.getDoor());
			} else {
				movableAtDoor.leavePosition();
			}
		}
	}

	private void searchSoldiersIfNeeded() {
		if (!searchedSoldiers.isEmpty()) {
			if (dijkstraRequest == null) {
				dijkstraRequest = new DijkstraContinuableRequest(this, super.pos.x, super.pos.y, (short) 1, Constants.TOWER_SEARCH_SOLDIERS_RADIUS);
			}

			dijkstraRequest.setSearchTypes(searchedSoldiers.getRequestedSearchTypes());

			Path path = super.grid.getDijkstra().find(dijkstraRequest);
			if (path != null) {
				ILogicMovable soldierMovable = super.grid.getMovable(path.getTargetPosition());
				if (soldierMovable instanceof ISoldierMovable) {
					ISoldierMovable soldier = (ISoldierMovable)soldierMovable;
					if(soldier.moveToTower(this)) {
						SoldierRequest soldierRequest = searchedSoldiers.removeOne(soldier.getMovableType().getSoldierType());
						comingSoldiers.put(soldier, soldierRequest);
						dijkstraRequest.reset();
					} // else soldier wasn't able to take the job to go to this building
				} // else { soldier wasn't at the position
			}
		}
	}

	@Override
	protected final void killedEvent() {
		setSelected(false);

		if (occupiedArea) {
			freeArea();

			int idx = 0;
			FreeMapArea buildingArea = super.getBuildingArea();
			for (TowerOccupier curr : sortedOccupiers) {
				addInformableMapObject(curr, false);// if curr is a bowman, this removes the informable map object.

				curr.getMovable().leaveTower(buildingArea.get(idx));
				idx++;
			}

			sortedOccupiers.clear();
			if(!inFight) setOccupied(false);
		}

		if (attackableTowerObject != null && attackableTowerObject.currDefender != null) {
			attackableTowerObject.currDefender.soldier.leaveTower(attackableTowerObject.getPosition());
		}

		setAttackableTowerObject(false);
	}

	private void freeArea() {
		super.grid.freeAreaOccupiedByTower(super.pos);
	}

	@Override
	public final List<? extends IBuildingOccupier> getOccupiers() {
		return sortedOccupiers;
	}

	@Override
	public final boolean needsPlayersGround() { // soldiers don't need players ground.
		return false;
	}

	@Override
	public boolean isShip() {
		return false;
	}

	@Override
	public final OccupierPlace addSoldier(ISoldierMovable soldier) {
		SoldierRequest soldierRequest = comingSoldiers.remove(soldier);
		OccupierPlace place = soldierRequest.place;

		TowerOccupier towerOccupier = new TowerOccupier(place, soldier);
		addOccupier(towerOccupier);

		occupyAreaIfNeeded();

		soldier.setSelected(super.isSelected());

		addInformableMapObject(towerOccupier, true);

		return place;
	}

	private void addOccupier(TowerOccupier towerOccupier) {
		if(sortedOccupiers.isEmpty()) setOccupied(true);
		sortedOccupiers.add(towerOccupier);
		sortedOccupiers.sort(Comparator.comparingInt(occupier -> occupier.place.getSoldierClass().ordinal));
	}

	@Override
	public void removeSoldier(ISoldierMovable soldier) {
		TowerOccupier occupier = null;
		for (TowerOccupier currOccupier : sortedOccupiers) {
			if (currOccupier.soldier == soldier) {
				occupier = currOccupier;
				break;
			}
		}

		// if the soldier is not in the tower, just return
		if (occupier == null) {
			return;
		}

		// removeOne the soldier and dijkstraRequest a new one
		sortedOccupiers.remove(occupier);
		if(sortedOccupiers.isEmpty() && !inFight) setOccupied(false);
		emptyPlaces.add(occupier.place);
		requestSoldier(occupier.place.getSoldierClass());
	}

	private TowerOccupier removeSoldier() {
		TowerOccupier removedSoldier = sortedOccupiers.removeFirst();

		addInformableMapObject(removedSoldier, false);

		return removedSoldier;
	}

	/**
	 * Adds or removes the informable map object for the given soldier.
	 *
	 * @param soldier
	 * 		occipier of the tower
	 * @param add
	 * 		if true, the object is added<br>
	 * 		if false, the object is removed.
	 */
	private void addInformableMapObject(TowerOccupier soldier, boolean add) {
		if (soldier.place.getSoldierClass() == ESoldierClass.BOWMAN) {
			ShortPoint2D position = getTowerBowmanSearchPosition(soldier.place);

			if (add) {
				super.grid.getMapObjectsManager().addInformableMapObjectAt(position, soldier.getMovable());
			} else {
				super.grid.getMapObjectsManager().removeMapObjectType(position.x, position.y, EMapObjectType.INFORMABLE_MAP_OBJECT);
			}
		}
	}

	@Override
	public ShortPoint2D getTowerBowmanSearchPosition(OccupierPlace place) {
		ShortPoint2D pos = place.getPosition().calculatePoint(super.pos);
		// FIXME @Andreas Eberle introduce new field in the buildings xml file
		return new ShortPoint2D(pos.x + 3, pos.y + 6);
	}

	private void occupyAreaIfNeeded() {
		if (!occupiedArea) {
			MapCircle occupying = new MapCircle(super.pos, CommonConstants.TOWER_RADIUS);
			super.grid.occupyAreaByTower(super.getPlayer(), occupying, getGroundArea());
			occupiedArea = true;
		}
	}

	@Override
	public final void requestFailed(ISoldierMovable soldier) {
		SoldierRequest soldierRequest = comingSoldiers.remove(soldier);
		addSoldierToSearch(soldierRequest);
	}

	private void addSoldierToSearch(SoldierRequest soldierRequest) {
		searchedSoldiers.add(soldierRequest);
		if (dijkstraRequest != null) {
			dijkstraRequest.reset();
		}
	}

	@Override
	public final ShortPoint2D getPosition(ISoldierMovable soldier) {
		for (TowerOccupier curr : sortedOccupiers) {
			if (curr.getMovable() == soldier) {
				return curr.place.getPosition().calculatePoint(super.pos);
			}
		}
		return null;
	}

	@Override
	public final void setSelected(boolean selected) {
		super.setSelected(selected);
		for (TowerOccupier curr : sortedOccupiers) {
			curr.getMovable().setSelected(selected);
		}

		if (attackableTowerObject != null && attackableTowerObject.currDefender != null) {
			attackableTowerObject.currDefender.getMovable().setSelected(selected);
		}
	}

	@Override
	public void towerDefended(ISoldierMovable soldier) {
		inFight = false;
		if(sortedOccupiers.isEmpty()) setOccupied(false);
		if (attackableTowerObject.currDefender == null) {
			System.err.println("ERROR: WHAT? No defender in a defended tower!");
		} else {
			TowerOccupier towerOccupier = new TowerOccupier(attackableTowerObject.currDefender.place, soldier);
			addOccupier(towerOccupier);
			attackableTowerObject.currDefender = null;
			addInformableMapObject(towerOccupier, true);
		}
		doorHealth = 0.1f;
	}

	@Override
	public int getSearchedSoldiers(ESoldierClass soldierClass) {
		return searchedSoldiers.getCount(soldierClass);
	}

	@Override
	public int getComingSoldiers(ESoldierClass soldierClass) {
		int numberOfComingSoldiers = 0;
		for (Map.Entry<ISoldierMovable, SoldierRequest> comingSoldier : comingSoldiers.entrySet()) {
			if (comingSoldier.getValue().isOfTypeOrClass(soldierClass)) {
				numberOfComingSoldiers++;
			}
		}
		return numberOfComingSoldiers;
	}

	@Override
	public void requestSoldier(ISoldierMovable soldier) {
		ESoldierClass soldierClass = soldier.getMovableType().getSoldierClass();
		OccupierPlace emptyPlace = getEmptyPlaceForSoldierClass(soldierClass);

		if (emptyPlace == null) {
			return;
		}

		soldier.moveToTower(this);
		comingSoldiers.put(soldier, new SoldierRequest(soldierClass, emptyPlace));
	}

	public void requestFullSoldiers() {
		for (OccupierPlace emptyPlace : emptyPlaces) {
			addSoldierToSearch(new SoldierRequest(emptyPlace.getSoldierClass(), emptyPlace));
		}
		emptyPlaces.clear();
	}

	public boolean isSetToBeFullyOccupied() {
		return emptyPlaces.isEmpty();
	}

	public void requestSoldier(ESoldierType soldierType) {
		OccupierPlace emptyPlace = getEmptyPlaceForSoldierClass(soldierType.soldierClass);
		if (emptyPlace != null) {
			emptyPlaces.remove(emptyPlace);
			addSoldierToSearch(new SoldierRequest(soldierType, emptyPlace));
		}
	}

	private void requestSoldier(ESoldierClass soldierClass) {
		OccupierPlace emptyPlace = getEmptyPlaceForSoldierClass(soldierClass);
		if (emptyPlace != null) {
			emptyPlaces.remove(emptyPlace);
			addSoldierToSearch(new SoldierRequest(soldierClass, emptyPlace));
		}
	}

	public void releaseSoldiers() {
		toBeReleasedOccupiers.clear();
		toBeReleasedOccupiers.addAll(sortedOccupiers); // release all but first occupier
		toBeReleasedOccupiers.removeFirst();

		emptyPlaces.addAll(searchedSoldiers.getPlaces());
		searchedSoldiers.clear();

		for (Map.Entry<ISoldierMovable, SoldierRequest> commingSoldierEntry : comingSoldiers.entrySet()) {
			commingSoldierEntry.getKey().leaveTower(super.getDoor());
			emptyPlaces.add(commingSoldierEntry.getValue().place);
		}
		comingSoldiers.clear();
	}

	public void releaseSoldier(ESoldierType soldierType) {
		SoldierRequest removedRequest = searchedSoldiers.removeOne(soldierType);
		if (removedRequest != null) {
			emptyPlaces.add(removedRequest.place);
			return;
		}

		for (Map.Entry<ISoldierMovable, SoldierRequest> commingSoldierEntry : comingSoldiers.entrySet()) {
			if (commingSoldierEntry.getValue().isOfTypeOrClass(soldierType)) {
				commingSoldierEntry.getKey().leaveTower(super.getDoor());
				emptyPlaces.add(commingSoldierEntry.getValue().place);
				comingSoldiers.remove(commingSoldierEntry.getKey());
				return;
			}
		}

		if (sortedOccupiers.size() - toBeReleasedOccupiers.size() > 1) { // always keep one soldier inside
			for (TowerOccupier occupier : sortedOccupiers) {
				if (occupier.soldier.getMovableType().getSoldierType() == soldierType && !toBeReleasedOccupiers.contains(occupier)) {
					toBeReleasedOccupiers.add(occupier);
					return;
				}
			}
		}
	}

	private OccupierPlace getEmptyPlaceForSoldierClass(ESoldierClass soldierClass) {
		for (OccupierPlace place : emptyPlaces) {
			if (place.getSoldierClass() == soldierClass) {
				return place;
			}
		}

		return null;
	}

	private static class AttackableTowerMapObject extends StandardMapObject implements IAttackable, IAttackableTowerMapObject {
		private static final long serialVersionUID = -2172130724577350091L;
		private OccupyingBuilding occupyingBuilding;
		private TowerOccupier currDefender;

		AttackableTowerMapObject(OccupyingBuilding occupyingBuilding) {
			super(EMapObjectType.ATTACKABLE_TOWER, false, occupyingBuilding.getPlayer());
			this.occupyingBuilding = occupyingBuilding;
		}

		@Override
		public ShortPoint2D getPosition() {
			return occupyingBuilding.getDoor();
		}

		@Override
		public void receiveHit(float strength, ShortPoint2D attackerPos, IPlayer attackingPlayer) {
			if (occupyingBuilding.isDestroyed()) {
				return; // building is destroyed => do nothing
			}

			ILogicMovable attacker = occupyingBuilding.grid.getMovable(attackerPos);
			if (attacker != null && attacker.getPlayer() == occupyingBuilding.getPlayer()) {
				return; // this can happen directly after the tower changed its player
			}

			if (occupyingBuilding.doorHealth > 0) {
				occupyingBuilding.doorHealth -= strength / Constants.DOOR_HIT_RESISTENCY_FACTOR;

				if (occupyingBuilding.doorHealth <= 0) {
					occupyingBuilding.doorHealth = 0;
					occupyingBuilding.inFight = true;
					occupyingBuilding.setOccupied(true);

					occupyingBuilding.grid.getMapObjectsManager().addSelfDeletingMapObject(getPosition(), EMapObjectType.GHOST, Constants.GHOST_PLAY_DURATION, getPlayer());

					pullNewDefender(attackerPos);
				}
			} else if (currDefender != null) {
				IAttackableMovable movable = currDefender.getMovable();
				movable.receiveHit(strength, attackerPos, attackingPlayer);

				if (!movable.isAlive()) {
					occupyingBuilding.emptyPlaces.add(currDefender.place); // dijkstraRequest a new soldier.
					occupyingBuilding.requestSoldier(currDefender.place.getSoldierClass());

					pullNewDefender(attackerPos);
				}
			}

			occupyingBuilding.getPlayer().showMessage(SimpleMessage.attacked(attackingPlayer.getPlayerId(), attackerPos));
		}

		private void pullNewDefender(ShortPoint2D attackerPos) {
			if (occupyingBuilding.sortedOccupiers.isEmpty()) {
				currDefender = null;
				occupyingBuilding.changePlayerTo(attackerPos);
			} else {
				currDefender = occupyingBuilding.removeSoldier();
				currDefender.getMovable().defendTowerAt();
			}
		}

		@Override
		public boolean isAlive() {
			return occupyingBuilding.doorHealth > 0 || currDefender != null && currDefender.getMovable().isAlive();
		}

		@Override
		public boolean isAttackable() {
			return true;
		}

		@Override
		public IGraphicsMovable getMovable() {
			return currDefender == null ? null : currDefender.getMovable();
		}

		@Override
		public EMovableType getMovableType() {
			assert false : "This should never have been called";
			return EMovableType.SWORDSMAN_L1;
		}

		@Override
		public void informAboutAttackable(IAttackable attackable) {
		}

		@Override
		public boolean isTower() {
			return true;
		}
	}
}