package jsettlers.common.map.partition;

public interface IProfessionSettings {

	boolean isBaererRatioFulfilled();

	boolean isDiggerRatioFulfilled();

	boolean isBricklayerRatioFulfilled();

	float getMinBearerRatio();

	float getCurrentBearerRatio();

	float getMaxDiggerRatio();

	float getCurrentDiggerRatio();

	float getMaxBricklayerRatio();

	float getCurrentBricklayerRatio();

	int getWorkerCount();
	
	int getBearerCount();

	int getDiggerCount();

	int getBricklayerCount();
}