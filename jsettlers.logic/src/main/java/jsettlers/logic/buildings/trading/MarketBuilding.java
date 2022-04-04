/*******************************************************************************
 * Copyright (c) 2016 - 2018
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

import jsettlers.common.buildings.EBuildingType;
import jsettlers.common.position.ShortPoint2D;
import jsettlers.logic.buildings.IBuildingsGrid;
import jsettlers.logic.buildings.stack.IRequestStack;
import jsettlers.logic.buildings.stack.IStackSizeSupplier;
import jsettlers.logic.movable.cargo.DonkeyMovable;
import jsettlers.logic.player.Player;
import jsettlers.logic.trading.TradeManager;

/**
 *
 * @author Andreas Eberle
 *
 */
public class MarketBuilding extends TradingBuilding {

	private static final long serialVersionUID = -2068624609186914142L;

	public MarketBuilding(EBuildingType type, Player player, ShortPoint2D position, IBuildingsGrid buildingsGrid) {
		super(type, player, position, buildingsGrid);
	}

	@Override
	protected ShortPoint2D getWaypointsStartPosition() {
		return super.pos;
	}


	@Override
	public boolean isSeaTrading() {
		return false;
	}

	@Override
	protected void killedEvent() {
		super.killedEvent();
	}

	@Override
	public ShortPoint2D getPickUpPosition() {
		return getDoor();
	}

	@Override
	protected TradeManager getTradeManager() {
		return getPlayer().getLandTradeManager();
	}

	@Override
	protected int getTradersForMaterial() {
		return (int)Math.ceil(getStacks().stream().mapToDouble(IStackSizeSupplier::getStackSize).sum() / DonkeyMovable.CARGO_COUNT);
	}
}
