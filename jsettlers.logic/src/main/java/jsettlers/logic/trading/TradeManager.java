package jsettlers.logic.trading;

import jsettlers.logic.buildings.ITradeBuilding;
import jsettlers.logic.map.grid.partition.manager.datastructures.PredicatedPositionableList;
import jsettlers.logic.timer.IScheduledTimerable;
import jsettlers.logic.timer.RescheduleTimer;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

public class TradeManager implements IScheduledTimerable, Serializable {

	private static final int RESCHEDULE_DELAY = 100;

	private final PredicatedPositionableList<ITrader> freeTraders = new PredicatedPositionableList<>();

	private final Set<ITradeBuilding> tradeBuildings = new HashSet<>();

	public void registerTrader(ITrader newTrader) {
		freeTraders.insert(newTrader);
	}

	public void issueTransportTask(ITrader mov, ITradeBuilding target) {
		mov.moveGoods(new TransportationRequest(target, this, mov));
	}

	void finishedTask(ITrader trader) {
		freeTraders.insert(trader);
	}

	public void removeTrader(ITrader trader, TransportationRequest currentRequest) {
		freeTraders.remove(trader);
		if(currentRequest != null) {
			currentRequest.receivedGoods();
		}
	}

	public void registerTradeBuilding(ITradeBuilding tradingBuilding) {
		tradeBuildings.add(tradingBuilding);
	}

	public void removeTradeBuilding(ITradeBuilding tradingBuilding) {
		tradeBuildings.remove(tradingBuilding);
	}

	public void scheduleTasks() {
		RescheduleTimer.add(this, 1);
	}

	@Override
	public int timerEvent() {
		for(ITradeBuilding tradeBuilding : tradeBuildings) {
			while(tradeBuilding.needsMoreTraders() && !freeTraders.isEmpty()) {
				ITrader newTrader = freeTraders.removeObjectNextTo(tradeBuilding.getPickUpPosition(), trader -> trader.mightReachPosition(tradeBuilding.getPickUpPosition()));
				if(newTrader == null || !newTrader.canReachPosition(tradeBuilding.getPickUpPosition())) break;

				issueTransportTask(newTrader, tradeBuilding);
			}
		}

		return RESCHEDULE_DELAY;
	}

	@Override
	public void kill() {
		System.err.println("TradeManagers should never be killed!");
	}
}