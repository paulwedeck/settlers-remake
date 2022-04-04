package jsettlers.logic.trading;

import jsettlers.common.position.ILocatable;
import jsettlers.common.position.ShortPoint2D;

public interface ITrader extends ILocatable {

	void moveGoods(TransportationRequest transportRequest);
	void goToTradeBuilding(ShortPoint2D position);

	boolean canReachPosition(ShortPoint2D target);

}
