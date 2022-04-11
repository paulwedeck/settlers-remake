package jsettlers.algorithms.simplebehaviortree;


import java.io.Serializable;

import java.util.function.Function;
import jsettlers.common.position.ShortPoint2D;

@FunctionalInterface
public interface IShortPoint2DSupplier<T> extends Function<T, ShortPoint2D>, Serializable {
}

