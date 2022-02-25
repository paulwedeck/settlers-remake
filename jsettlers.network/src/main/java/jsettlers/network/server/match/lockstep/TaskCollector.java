package jsettlers.network.server.match.lockstep;

import jsettlers.network.NetworkConstants;
import jsettlers.network.infrastructure.channel.GenericDeserializer;
import jsettlers.network.infrastructure.channel.IChannelListener;
import jsettlers.network.infrastructure.channel.listeners.PacketChannelListener;
import jsettlers.network.infrastructure.channel.packet.Packet;
import jsettlers.network.server.packets.ServersideTaskPacket;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class TaskCollector {

	private final Object taskListLock = new Object();
	private List<ServersideTaskPacket> currTasksList = new LinkedList<>();

	/**
	 *
	 * @return
	 */
	public List<ServersideTaskPacket> getAndResetTasks() {
		List<ServersideTaskPacket> temp;
		synchronized (taskListLock) {
			temp = currTasksList;
			currTasksList = new LinkedList<>();
		}
		return temp;
	}

	public IChannelListener createListener(Set<Byte> validIds) {
		return new TaskCollectingListener(validIds);
	}

	/**
	 * This listener collects {@link Packet}s for the {@link NetworkConstants}.Keys.SYNCHRONOUS_TASK key and adds them to a list. The elements can then be
	 * removed from the list to be send to the clients as batch.
	 *
	 * @author Andreas Eberle
	 *
	 */
	public class TaskCollectingListener extends PacketChannelListener<ServersideTaskPacket> {

		private final Set<Byte> validIds;

		private TaskCollectingListener(Set<Byte> validIds) {
			super(NetworkConstants.ENetworkKey.SYNCHRONOUS_TASK, new GenericDeserializer<>(ServersideTaskPacket.class));
			this.validIds = validIds;
		}

		@Override
		protected void receivePacket(NetworkConstants.ENetworkKey key, ServersideTaskPacket deserialized) {
			synchronized (taskListLock) {
				currTasksList.add(deserialized);
			}
		}
	}
}
