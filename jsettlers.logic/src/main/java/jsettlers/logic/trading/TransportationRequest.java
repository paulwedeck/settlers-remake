package jsettlers.logic.trading;

import jsettlers.common.position.ShortPoint2D;
import jsettlers.logic.buildings.ITradeBuilding;

import java.io.Serializable;
import java.util.Iterator;

public class TransportationRequest implements Serializable {

	private static final long serialVersionUID = 5289692132463521081L;
	private final ITradeBuilding tradeBuilding;
	private final Iterator<ShortPoint2D> waypoints;
	private final TradeManager parent;
	private final ITrader subject;
	private boolean goingToBuilding = true;

	public TransportationRequest(ITradeBuilding tradeBuilding, TradeManager parent, ITrader subject) {
		this.tradeBuilding = tradeBuilding;
		this.parent = parent;
		this.subject = subject;

		tradeBuilding.addApproachingTrader();

		waypoints = tradeBuilding.getWaypointsIterator();
	}

	public ShortPoint2D getStart() {
		return tradeBuilding.getPickUpPosition();
	}

	public boolean isActive() {
		return !goingToBuilding || tradeBuilding.needsTrader();
	}

	public boolean hasNextWaypoint() {
		return waypoints.hasNext();
	}

	public ShortPoint2D nextWaypoint() {
		return waypoints.next();
	}

	public ITradeBuilding getBuilding() {
		return tradeBuilding;
	}

	public void receivedGoods() {
		assert goingToBuilding;
		goingToBuilding = false;
		tradeBuilding.removeApproachingTrader();
	}

	public void finishTask() {
		if(goingToBuilding) {
			tradeBuilding.removeApproachingTrader();
		}
		parent.finishedTask(subject);
	}
}
