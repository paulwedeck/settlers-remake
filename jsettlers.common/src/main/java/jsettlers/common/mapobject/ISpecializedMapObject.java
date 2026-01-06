package jsettlers.common.mapobject;

import jsettlers.common.sound.ESoundType;

public interface ISpecializedMapObject {
	int getAnimation();
	ESoundType getSound();
    boolean isPlayInFog();
}
