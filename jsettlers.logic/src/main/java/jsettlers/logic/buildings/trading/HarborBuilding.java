/*******************************************************************************
 * Copyright (c) 2017 - 2018
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
package jsettlers.logic.buildings.trading;

import jsettlers.common.action.SetTradingWaypointAction;
import jsettlers.common.buildings.EBuildingType;
import jsettlers.common.material.EMaterialType;
import jsettlers.common.position.ShortPoint2D;
import jsettlers.logic.DockPosition;
import jsettlers.logic.buildings.IBuildingsGrid;
import jsettlers.logic.buildings.IDockBuilding;
import jsettlers.logic.buildings.stack.IRequestStack;
import jsettlers.logic.constants.Constants;
import jsettlers.logic.movable.cargo.CargoShipMovable;
import jsettlers.logic.player.Player;
import jsettlers.logic.trading.TradeManager;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * @author Rudolf Polzer
 */
public class HarborBuilding extends TradingBuilding implements IDockBuilding {

	private static final long serialVersionUID = -8477261512687011185L;
	private DockPosition dockPosition = null;

	public HarborBuilding(EBuildingType type, Player player, ShortPoint2D position, IBuildingsGrid buildingsGrid) {
		super(type, player, position, buildingsGrid);
	}


	@Override
	public ShortPoint2D getWaypointsStartPosition() {
		return dockPosition != null ? dockPosition.getWaterPosition() : null;
	}


	@Override
	public boolean isSeaTrading() {
		return true;
	}

	@Override
	protected void killedEvent() {
		super.killedEvent();
		removeDock();
	}

	@Override
	public ShortPoint2D getPickUpPosition() {
		return getWaypointsStartPosition();
	}

	@Override
	protected boolean isWaypointFulfillingPreconditions(SetTradingWaypointAction.EWaypointType waypointType, ShortPoint2D position) {
		return waypointType != SetTradingWaypointAction.EWaypointType.DESTINATION || grid.isCoastReachable(position);
	}

	@Override
	public void setDock(ShortPoint2D requestedDockPosition) {
		DockPosition newDockPosition = findValidDockPosition(requestedDockPosition);
		if (newDockPosition == null) {
			return;
		}

		if (isSelected()) {
			drawWaypointLine(false);
		}
		removeDock();

		dockPosition = newDockPosition;
		grid.setDock(dockPosition, this.getPlayer());

		if (isSelected()) {
			drawWaypointLine(true);
		}
	}

	@Override
	public boolean canDockBePlaced(ShortPoint2D requestedDockPosition) {
		return findValidDockPosition(requestedDockPosition) != null;
	}

	private DockPosition findValidDockPosition(ShortPoint2D requestedDockPosition) {
		return grid.findValidDockPosition(requestedDockPosition, pos, IDockBuilding.MAXIMUM_DOCKYARD_DISTANCE);
	}

	public DockPosition getDock() {
		return this.dockPosition;
	}

	private void removeDock() {
		if (this.dockPosition == null) {
			return;
		}
		this.grid.removeDock(this.dockPosition);
		this.dockPosition = null;
	}

	@Override
	protected int getTradersForMaterial() {
		Map<EMaterialType, Integer> amountPerMaterial = new HashMap<>();
		for(IRequestStack stack : getStacks()) {
			amountPerMaterial.put(stack.getMaterialType(), stack.getStackSize());
		}
		double uniqueTransportStacks = amountPerMaterial.values().stream().mapToDouble(i -> i).map(i -> Math.ceil(i / Constants.STACK_SIZE)).sum();

		return (int) Math.ceil(uniqueTransportStacks / CargoShipMovable.CARGO_STACKS);
	}

	@Override
	protected TradeManager getTradeManager() {
		return getPlayer().getSeaTradeManager();
	}
}
