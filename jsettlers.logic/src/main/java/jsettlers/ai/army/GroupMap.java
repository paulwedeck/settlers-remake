package jsettlers.ai.army;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class GroupMap<G, M> {
	private final Map<G, Set<M>> groups = new HashMap<>();
	private final Map<M, G> reverse = new HashMap<>();

	public void setMember(M member, G group) {
		G oldGroup = reverse.get(member);
		if(oldGroup != null) {
			Set<M> oldGroupMembers = groups.get(oldGroup);
			oldGroupMembers.remove(member);
			if(oldGroupMembers.isEmpty()) {
				groups.remove(oldGroup);
			}
		}

		if(group != null) {
			getGroupMembers(group).add(member);
			reverse.put(member, group);
		} else {
			reverse.remove(member);
		}
	}

	public Set<M> getMembers(G group) {
		return Collections.unmodifiableSet(getGroupMembers(group));
	}

	public G getGroup(M member) {
		return reverse.get(member);
	}

	public Map<G, Set<M>> listGroups() {
		return Collections.unmodifiableMap(groups);
	}

	public Set<M> listMembers() {
		return Collections.unmodifiableSet(reverse.keySet());
	}

	public void removeGroup(G group) {
		groups.get(group).forEach(reverse::remove);
		groups.remove(group);
	}

	private Set<M> getGroupMembers(G group) {
		return groups.computeIfAbsent(group, g -> new HashSet<>());
	}

	public void removeMemberIf(Predicate<M> cond) {
		List<M> remove = reverse.keySet().stream().filter(cond).collect(Collectors.toList());

		remove.forEach(member -> setMember(member, null));
	}
}
