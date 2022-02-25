/*******************************************************************************
 * Copyright (c) 2015
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"),
 * to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 *******************************************************************************/
package jsettlers.network.server;

import java.util.Timer;

import jsettlers.network.NetworkConstants;
import jsettlers.network.common.packets.ChatMessagePacket;
import jsettlers.network.common.packets.OpenNewMatchPacket;
import jsettlers.network.common.packets.TimeSyncPacket;
import jsettlers.network.infrastructure.channel.Channel;
import jsettlers.network.infrastructure.channel.reject.RejectPacket;
import jsettlers.network.server.db.IDBFacade;
import jsettlers.network.server.exceptions.NotAllPlayersReadyException;
import jsettlers.network.server.listeners.ChangeCivilisationPacketListener;
import jsettlers.network.server.listeners.ChangePlayerCountPacketListener;
import jsettlers.network.server.listeners.ChangePlayerTypePacketListener;
import jsettlers.network.server.listeners.ChangePositionPacketListener;
import jsettlers.network.server.listeners.ChangeTeamPacketListener;
import jsettlers.network.server.listeners.ChatMessageForwardingListener;
import jsettlers.network.server.listeners.IdentifyUserListener;
import jsettlers.network.server.listeners.ReadyStatePacketListener;
import jsettlers.network.server.listeners.ServerChannelClosedListener;
import jsettlers.network.server.listeners.StartFinishedSignalListener;
import jsettlers.network.server.listeners.TimeSyncForwardingListener;
import jsettlers.network.server.listeners.matches.JoinMatchListener;
import jsettlers.network.server.listeners.matches.LeaveMatchListener;
import jsettlers.network.server.listeners.matches.OpenNewMatchListener;
import jsettlers.network.server.listeners.matches.StartMatchListener;
import jsettlers.network.server.match.EPlayerState;
import jsettlers.network.server.match.Match;
import jsettlers.network.server.match.MatchesListSendingTimerTask;
import jsettlers.network.server.match.Player;

/**
 * This class is the central access point to the servers externally reachable functions.
 * 
 * @author Andreas Eberle
 * 
 */
public class ServerManager implements IServerManager {

	private final IDBFacade database;
	private final Timer sendMatchesListTimer = new Timer("SendMatchesListTimer", true);
	private final Timer matchesTaskDistributionTimer = new Timer("MatchesTaskDistributionTimer", true);
	private final MatchesListSendingTimerTask matchSendingTask;

	public ServerManager(IDBFacade db) {
		this.database = db;
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
		if (database.isAcceptedPlayer(player.getId())) {
			database.storePlayer(player);

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
			channel.registerListener(new StartFinishedSignalListener(this, player));
			channel.registerListener(new ChangeCivilisationPacketListener(this, player));
			channel.registerListener(new ChangePlayerTypePacketListener(this, player));
			channel.registerListener(new ChangePositionPacketListener(this, player));
			channel.registerListener(new ChangeTeamPacketListener(this, player));
			channel.registerListener(new ChangePlayerCountPacketListener(this, player));

			return true;
		} else {
			return false;
		}
	}

	@Override
	public void channelClosed(Player player) {
		if (player.isInMatch()) {
			try {
				player.leaveMatch();
			} catch (IllegalStateException e) {
				assert false : "This may never happen here!";
			}
		}

		database.removePlayer(player);
	}

	@Override
	public void createNewMatch(OpenNewMatchPacket matchInfo, Player player) {
		Match match = new Match(matchInfo.getMatchName(), matchInfo.getMaxPlayers(), matchInfo.getMapInfo(), player, matchInfo.getRandomSeed());
		database.storeMatch(match);

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
		Match match = player.getMatch();
		player.leaveMatch();
		database.checkMatch(match);
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
		Match match = database.getMatchById(matchId);
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

	private Match verifyAndGetMatch(Player player) {
		EPlayerState.assertState(player.getState(), EPlayerState.IN_MATCH);
		Match match = player.getMatch();

		if(match.getHost() != player) {
			throw new IllegalStateException("Player " + player + " is not the host of " + match + "!");
		}

		return match;
	}

	@Override
	public void setCivilisationForSlot(Player player, byte slot, byte civilisation) {
		try {
			Match match = verifyAndGetMatch(player);

			match.setSlotCivilisation(slot, civilisation);
		} catch (IllegalStateException e) {
			player.sendPacket(NetworkConstants.ENetworkKey.REJECT_PACKET,
					new RejectPacket(NetworkConstants.ENetworkMessage.INVALID_STATE_ERROR, NetworkConstants.ENetworkKey.CHANGE_CIVILISATION));
		}
	}

	@Override
	public void setPlayerTypeForSlot(Player player, byte slot, byte playerType) {
		try {
			Match match = verifyAndGetMatch(player);

			match.setSlotPlayerType(slot, playerType);
		} catch (IllegalStateException e) {
			player.sendPacket(NetworkConstants.ENetworkKey.REJECT_PACKET,
					new RejectPacket(NetworkConstants.ENetworkMessage.INVALID_STATE_ERROR, NetworkConstants.ENetworkKey.CHANGE_PLAYER_TYPE));
		}
	}

	@Override
	public void setPositionForSlot(Player player, byte slot, byte position) {
		try {
			Match match = verifyAndGetMatch(player);

			match.setSlotPosition(slot, position);
		} catch (IllegalStateException e) {
			player.sendPacket(NetworkConstants.ENetworkKey.REJECT_PACKET,
					new RejectPacket(NetworkConstants.ENetworkMessage.INVALID_STATE_ERROR, NetworkConstants.ENetworkKey.CHANGE_POSITION));
		}
	}

	@Override
	public void setTeamForSlot(Player player, byte slot, byte team) {
		try {
			Match match = verifyAndGetMatch(player);

			match.setSlotTeam(slot, team);
		} catch (IllegalStateException e) {
			player.sendPacket(NetworkConstants.ENetworkKey.REJECT_PACKET,
					new RejectPacket(NetworkConstants.ENetworkMessage.INVALID_STATE_ERROR, NetworkConstants.ENetworkKey.CHANGE_TEAM));
		}
	}

	@Override
	public void setPlayerCount(Player player, int playerCount) {
		try {
			Match match = verifyAndGetMatch(player);

			match.setPlayerCount(playerCount);
		} catch (IllegalStateException e) {
			player.sendPacket(NetworkConstants.ENetworkKey.REJECT_PACKET,
					new RejectPacket(NetworkConstants.ENetworkMessage.INVALID_STATE_ERROR, NetworkConstants.ENetworkKey.CHANGE_PLAYER_COUNT));
		}
	}

	@Override
	public void sendMatchesToPlayer(Player player) {
		matchSendingTask.sendMatchesTo(player);
	}

	@Override
	public void setStartFinished(Player player, boolean startFinished) {
		player.setStartFinished(startFinished);
	}

	public IDBFacade getDatabase() {
		return database;
	}
}
