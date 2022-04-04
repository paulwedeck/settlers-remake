package jsettlers.logic.movable.cargo;

import jsettlers.common.material.EMaterialType;
import jsettlers.common.movable.EMovableType;
import jsettlers.common.movable.IGraphicsCargoShip;
import jsettlers.common.position.ShortPoint2D;
import jsettlers.common.utils.mutables.MutableBoolean;
import jsettlers.logic.buildings.ITradeBuilding;
import jsettlers.logic.trading.TradeManager;
import jsettlers.logic.constants.Constants;
import jsettlers.logic.movable.Movable;
import jsettlers.logic.movable.interfaces.AbstractMovableGrid;
import jsettlers.logic.player.Player;

public class CargoShipMovable extends CargoMovable implements IGraphicsCargoShip {
	private static final short WAYPOINT_SEARCH_RADIUS = 50;

	public static final int CARGO_STACKS = 3;
	private static final long serialVersionUID = -2110193970372443265L;

	private final EMaterialType[]	cargoType	= new EMaterialType[CARGO_STACKS];
	private final int[]				cargoCount	= new int[CARGO_STACKS];

	public CargoShipMovable(AbstractMovableGrid grid, ShortPoint2D position, Player player, Movable movable) {
		super(grid, EMovableType.CARGO_SHIP, position, player, movable);
	}

	private boolean checkStackNumber(int stack) {
		return stack >= 0 && stack < CARGO_STACKS;
	}

	@Override
	public EMaterialType getCargoType(int stack) {
		return this.cargoType[stack];
	}

	@Override
	public int getCargoCount(int stack) {
		return this.cargoCount[stack];
	}

	@Override
	public int getNumberOfCargoStacks() {
		return CARGO_STACKS;
	}

	private void setCargoCount(int count, int stack) {
		if (checkStackNumber(stack)) {
			this.cargoCount[stack] = count;
			if (this.cargoCount[stack] < 0) {
				this.cargoCount[stack] = 0;
			} else if (this.cargoCount[stack] > 8) {
				this.cargoCount[stack] = 8;
			}
		}
	}

	private void setCargoType(EMaterialType cargo, int stack) {
		this.cargoType[stack] = cargo;
	}

	@Override
	protected boolean loadUp(ITradeBuilding tradeBuilding) {
		MutableBoolean loaded = new MutableBoolean(false);

		for (int stackIndex = 0; stackIndex < CargoShipMovable.CARGO_STACKS; stackIndex++) {
			if (getCargoCount(stackIndex) > 0) {
				continue;
			}

			final int finalStackIndex = stackIndex;

			tradeBuilding.tryToTakeMaterial(Constants.STACK_SIZE).ifPresent(materialTypeWithCount -> {
				setCargoType(materialTypeWithCount.materialType, finalStackIndex);
				setCargoCount(materialTypeWithCount.count, finalStackIndex);

				loaded.value = true;
			});
		}

		return loaded.value;
	}

	protected void dropMaterialIfPossible() {
		for (int stack = 0; stack < CargoShipMovable.CARGO_STACKS; stack++) {
			int cargoCount = getCargoCount(stack);
			EMaterialType material = getCargoType(stack);
			while (cargoCount > 0) {
				grid.dropMaterial(position, material, true, true);
				cargoCount--;
			}
			setCargoCount(0, stack);
		}
	}

	@Override
	protected TradeManager getTradeManager() {
		return player.getSeaTradeManager();
	}

	@Override
	public short getWaypointSearchRadius() {
		return WAYPOINT_SEARCH_RADIUS;
	}
}
