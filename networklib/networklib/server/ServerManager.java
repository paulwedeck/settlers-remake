package networklib.server;

import java.util.Timer;

import networklib.NetworkConstants;
import networklib.common.packets.ChatMessagePacket;
import networklib.common.packets.OpenNewMatchPacket;
import networklib.common.packets.TimeSyncPacket;
import networklib.infrastructure.channel.Channel;
import networklib.infrastructure.channel.reject.RejectPacket;
import networklib.server.db.IDBFacade;
import networklib.server.exceptions.NotAllPlayersReadyException;
import networklib.server.listeners.ChatMessageForwardingListener;
import networklib.server.listeners.IdentifyUserListener;
import networklib.server.listeners.ReadyStatePacketListener;
import networklib.server.listeners.ServerChannelClosedListener;
import networklib.server.listeners.TimeSyncForwardingListener;
import networklib.server.listeners.matches.JoinMatchListener;
import networklib.server.listeners.matches.LeaveMatchListener;
import networklib.server.listeners.matches.OpenNewMatchListener;
import networklib.server.listeners.matches.StartMatchListener;
import networklib.server.match.Match;
import networklib.server.match.MatchesListSendingTimerTask;
import networklib.server.match.Player;

/**
 * This class is the central access point to the servers externally reachable functions.
 * 
 * @author Andreas Eberle
 * 
 */
public class ServerManager implements IServerManager {

	private final IDBFacade db;
	private final Timer sendMatchesListTimer = new Timer("SendMatchesListTimer", true);
	private final Timer matchesTaskDistributionTimer = new Timer("MatchesTaskDistributionTimer", true);
	private final MatchesListSendingTimerTask matchSendingTask;

	public ServerManager(IDBFacade db) {
		this.db = db;
		matchSendingTask = new MatchesListSendingTimerTask(db);
	}

	public synchronized void start() {
		sendMatchesListTimer.schedule(matchSendingTask, 0, NetworkConstants.Server.OPEN_MATCHES_SEND_INTERVAL_MS);
	}

	public synchronized void shutdown() {
		sendMatchesListTimer.cancel();
		matchesTaskDistributionTimer.cancel();
	}

	public void identifyNewChannel(Channel channel) {
		channel.registerListener(new IdentifyUserListener(channel, this));
	}

	@Override
	public boolean acceptNewPlayer(Player player) {
		if (db.isAcceptedPlayer(player.getId())) {
			db.storePlayer(player);

			Channel channel = player.getChannel();
			channel.removeListener(NetworkConstants.ENetworkKey.IDENTIFY_USER);

			channel.setChannelClosedListener(new ServerChannelClosedListener(this, player));
			channel.registerListener(new OpenNewMatchListener(this, player));
			channel.registerListener(new LeaveMatchListener(this, player));
			channel.registerListener(new StartMatchListener(this, player));
			channel.registerListener(new JoinMatchListener(this, player));
			channel.registerListener(new ChatMessageForwardingListener(this, player));
			channel.registerListener(new TimeSyncForwardingListener(this, player));
			channel.registerListener(new ReadyStatePacketListener(this, player));

			return true;
		} else {
			return false;
		}
	}

	@Override
	public void channelClosed(Player player) {
		db.removePlayer(player);

		if (player.isInMatch()) {
			try {
				player.leaveMatch();
			} catch (IllegalStateException e) {
				assert false : "This may never happen here!";
			}
		}
	}

	@Override
	public void createNewMatch(OpenNewMatchPacket matchInfo, Player player) {
		Match match = new Match(matchInfo.getMatchName(), matchInfo.getMaxPlayers(), matchInfo.getMapInfo(), matchInfo.getRandomSeed());
		db.storeMatch(match);

		joinMatch(match, player);
	}

	private void joinMatch(Match match, Player player) {
		try {
			player.joinMatch(match);
		} catch (IllegalStateException e) {
			e.printStackTrace();
			player.sendPacket(NetworkConstants.ENetworkKey.REJECT_PACKET,
					new RejectPacket(NetworkConstants.ENetworkMessage.INVALID_STATE_ERROR, NetworkConstants.ENetworkKey.REQUEST_OPEN_NEW_MATCH));
		}
	}

	@Override
	public void leaveMatch(Player player) {
		player.leaveMatch();
	}

	@Override
	public void startMatch(Player player) {
		try {
			player.startMatch(matchesTaskDistributionTimer);
		} catch (IllegalStateException e) {
			e.printStackTrace();
			player.sendPacket(NetworkConstants.ENetworkKey.REJECT_PACKET,
					new RejectPacket(NetworkConstants.ENetworkMessage.INVALID_STATE_ERROR, NetworkConstants.ENetworkKey.REQUEST_START_MATCH));
		} catch (NotAllPlayersReadyException e) {
			player.sendPacket(NetworkConstants.ENetworkKey.REJECT_PACKET,
					new RejectPacket(NetworkConstants.ENetworkMessage.NOT_ALL_PLAYERS_READY, NetworkConstants.ENetworkKey.REQUEST_START_MATCH));
		}
	}

	@Override
	public void forwardChatMessage(Player player, ChatMessagePacket packet) {
		try {
			player.forwardChatMessage(packet);
		} catch (IllegalStateException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void distributeTimeSync(Player player, TimeSyncPacket packet) {
		try {
			player.distributeTimeSync(packet);
		} catch (IllegalStateException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void joinMatch(String matchId, Player player) {
		Match match = db.getMatchById(matchId);
		try {
			player.joinMatch(match);
		} catch (IllegalStateException e) {
			player.sendPacket(NetworkConstants.ENetworkKey.REJECT_PACKET,
					new RejectPacket(NetworkConstants.ENetworkMessage.INVALID_STATE_ERROR, NetworkConstants.ENetworkKey.REQUEST_JOIN_MATCH));
		}
	}

	@Override
	public void setReadyStateForPlayer(Player player, boolean ready) {
		try {
			player.setReady(ready);
		} catch (IllegalStateException e) {
			player.sendPacket(NetworkConstants.ENetworkKey.REJECT_PACKET,
					new RejectPacket(NetworkConstants.ENetworkMessage.INVALID_STATE_ERROR, NetworkConstants.ENetworkKey.CHANGE_READY_STATE));
		}
	}

	@Override
	public void sendMatchesToPlayer(Player player) {
		matchSendingTask.sendMatchesTo(player);
	}

}
