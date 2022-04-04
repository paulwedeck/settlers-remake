package jsettlers.algorithms.terraform;

import java.util.List;

import java.util.function.BiConsumer;
import java.util.function.Function;
import jsettlers.common.landscape.ELandscapeType;
import jsettlers.common.position.ShortPoint2D;

public final class LandscapeEditor {
	private final Function<ShortPoint2D, ELandscapeType> getLandscape;
	private final BiConsumer<ShortPoint2D, ELandscapeType> setLandscape;

	public LandscapeEditor(Function<ShortPoint2D, ELandscapeType> getLandscape, BiConsumer<ShortPoint2D, ELandscapeType> setLandscape) {
		this.getLandscape = getLandscape;
		this.setLandscape = setLandscape;
	}

	public void fill(ELandscapeType type, List<ShortPoint2D> points) {
		fill(type, null, points);
	}

	private void fill(ELandscapeType type, ELandscapeType ignore, List<ShortPoint2D> points) {
		boolean is_root = true;
		int size = points.size();
		for(int i = 0; i != size; i++) {
			ShortPoint2D pt = points.get(i);
			if(!type.isRoot(getLandscape.apply(pt))) {
				is_root = false;
				break;
			}
		}

		if(!is_root) {
			fill(type.getDirectRoot(), type, points);
			replaceDirect(type.getDirectRoot(), type, points);
		}

		replaceDown(type, type, ignore, points);
	}

	private void replaceDown(ELandscapeType from, ELandscapeType to, ELandscapeType ignore, List<ShortPoint2D> points) {
		for(ELandscapeType directChild : from.getDirectChildren()) {
			if(ignore != null && directChild == ignore) continue;

			replaceDown(directChild, from, null, points);
		}
		replaceDirect(from, to, points);
	}

	private void replaceDirect(ELandscapeType from, ELandscapeType to, List<ShortPoint2D> points) {
		int size = points.size();
		for(int i = 0; i != size; i++) {
			ShortPoint2D pt = points.get(i);
			if(getLandscape.apply(pt) == from) setLandscape.accept(pt, to);
		}
	}
}
