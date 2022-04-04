package jsettlers.common.utils.collections.set;

import java.io.Serializable;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ArrayListSet<E> extends AbstractSet<E> implements Serializable {

	private static final long serialVersionUID = 4735006144281753889L;
	private final List<E> list = new ArrayList<>();

	@Override
	public Iterator<E> iterator() {
		return list.iterator();
	}

	@Override
	public boolean add(E e) {
		if(contains(e)) {
			return false;
		}

		list.add(e);
		return true;
	}

	@Override
	public int size() {
		return list.size();
	}
}
