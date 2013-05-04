package networklib.client;

import java.io.IOException;

import networklib.NetworkConstants;
import networklib.channel.GenericDeserializer;
import networklib.channel.listeners.PacketChannelListener;
import networklib.client.receiver.IPacketReceiver;
import networklib.server.packets.MatchInfoPacket;

/**
 * 
 * @author Andreas Eberle
 * 
 */
public class JoinedMatchListener extends PacketChannelListener<MatchInfoPacket> {
	private final NetworkClient client;
	private final IPacketReceiver<MatchInfoPacket> listener;

	public JoinedMatchListener(NetworkClient client, IPacketReceiver<MatchInfoPacket> listener) {
		super(NetworkConstants.Keys.PLAYER_JOINED, new GenericDeserializer<MatchInfoPacket>(MatchInfoPacket.class));

		this.client = client;
		this.listener = listener;
	}

	@Override
	protected void receivePacket(int key, MatchInfoPacket deserialized) throws IOException {
		client.openedMatch(deserialized);
		listener.receivePacket(deserialized);
	}

}
