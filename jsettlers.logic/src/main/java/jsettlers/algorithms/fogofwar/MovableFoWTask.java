package jsettlers.algorithms.fogofwar;

import jsettlers.shared.ShortPoint2D;

public interface MovableFoWTask extends FoWTask {

	int getViewDistance();

	ShortPoint2D getFoWPosition();

	ShortPoint2D getOldFoWPosition();

	void setOldFoWPosition(ShortPoint2D position);

	boolean continueFoW();
}
