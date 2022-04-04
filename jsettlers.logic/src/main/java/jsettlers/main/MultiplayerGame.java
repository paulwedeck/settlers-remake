/*******************************************************************************
 * Copyright (c) 2015 - 2017
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
package jsettlers.main;

import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import jsettlers.common.ai.EPlayerType;
import jsettlers.common.menu.ENetworkMessage;
import jsettlers.common.menu.IChatMessageListener;
import jsettlers.common.menu.IJoinPhaseMultiplayerGameConnector;
import jsettlers.common.menu.IJoiningGame;
import jsettlers.common.menu.IJoiningGameListener;
import jsettlers.common.menu.IMapDefinition;
import jsettlers.common.menu.IMultiplayerListener;
import jsettlers.common.menu.IMultiplayerPlayer;
import jsettlers.common.menu.IMultiplayerSlot;
import jsettlers.common.menu.IOpenMultiplayerGameInfo;
import jsettlers.common.player.ECivilisation;
import jsettlers.common.utils.collections.ChangingList;
import jsettlers.logic.map.loading.MapLoader;
import jsettlers.logic.map.loading.list.MapList;
import jsettlers.logic.player.InitialGameState;
import jsettlers.logic.player.PlayerSetting;
import jsettlers.main.datatypes.MultiplayerPlayer;
import jsettlers.main.datatypes.MultiplayerSlot;
import jsettlers.network.NetworkConstants;
import jsettlers.network.client.interfaces.INetworkClient;
import jsettlers.network.client.receiver.IPacketReceiver;
import jsettlers.network.common.packets.ChatMessagePacket;
import jsettlers.network.common.packets.MapInfoPacket;
import jsettlers.network.common.packets.MatchInfoPacket;
import jsettlers.network.common.packets.MatchInfoUpdatePacket;
import jsettlers.network.common.packets.MatchStartPacket;
import jsettlers.network.common.packets.PlayerInfoPacket;
import jsettlers.network.common.packets.SlotInfoPacket;
import jsettlers.network.server.match.EPlayerState;

/**
 * 
 * @author Andreas Eberle
 * 
 */
public class MultiplayerGame {

	private final AsyncNetworkClientConnector networkClientFactory;
	private final ChangingList<IMultiplayerPlayer> playersList = new ChangingList<>();
	private final ChangingList<IMultiplayerSlot> slotList = new ChangingList<>();
	private INetworkClient networkClient;

	private IJoiningGameListener joiningGameListener;
	private IMultiplayerListener multiplayerListener;
	private IChatMessageListener chatMessageListener;
	private boolean iAmTheHost = false;
	private int maxPlayers;

	public MultiplayerGame(AsyncNetworkClientConnector networkClientFactory) {
		this.networkClientFactory = networkClientFactory;
	}

	public IJoiningGame join(final String matchId) {
		new Thread("joinGameThread") {
			@Override
			public void run() {
				networkClient = networkClientFactory.getNetworkClient();
				networkClient.joinMatch(matchId, generateMatchStartedListener(), generateMatchInfoUpdatedListener(), generateChatMessageReceiver());
			}
		}.start();
		return generateJoiningGame();
	}

	public IJoiningGame openNewGame(final IOpenMultiplayerGameInfo gameInfo) {
		iAmTheHost = true;
		new Thread("openNewGameThread") {
			@Override
			public void run() {
				networkClient = networkClientFactory.getNetworkClient();

				IMapDefinition mapDefintion = gameInfo.getMapDefinition();
				MapInfoPacket mapInfo = new MapInfoPacket(mapDefintion.getMapId(), mapDefintion.getMapName(), "", "", mapDefintion.getMaxPlayers());

				networkClient.openNewMatch(gameInfo.getMatchName(), gameInfo.getMaxPlayers(), mapInfo, 4711L, generateMatchStartedListener(),
						generateMatchInfoUpdatedListener(), generateChatMessageReceiver());
			}
		}.start();
		return generateJoiningGame();
	}

	private IJoiningGame generateJoiningGame() {
		return new IJoiningGame() {
			@Override
			public void setListener(IJoiningGameListener joiningGameListener) {
				MultiplayerGame.this.joiningGameListener = joiningGameListener;
				if (joiningGameListener != null && networkClient != null && networkClient.getState() == EPlayerState.IN_MATCH) {
					joiningGameListener.gameJoined(generateJoinPhaseGameConnector());
				}
			}

			@Override
			public void abort() {
				networkClient.leaveMatch();
			}
		};
	}

	private IPacketReceiver<ChatMessagePacket> generateChatMessageReceiver() {
		return packet -> {
			if (chatMessageListener != null) {
				chatMessageListener.chatMessageReceived(packet.getAuthorId(), packet.getMessage());
			}
		};
	}

	private IPacketReceiver<MatchStartPacket> generateMatchStartedListener() {
		return packet -> {
			updateLists(packet.getMatchInfo());

			MapLoader mapLoader = MapList.getDefaultList().getMapById(packet.getMatchInfo().getMapInfo().getId());
			long randomSeed = packet.getRandomSeed();
			PlayerSetting[] playerSettings = determinePlayerSettings();
			byte ownPlayerId = calculateOwnPlayerId();
			// TODO start resources
			InitialGameState initialGameState = new InitialGameState(ownPlayerId, playerSettings, randomSeed);

			JSettlersGame game = new JSettlersGame(mapLoader, networkClient.getNetworkConnector(), initialGameState);

			multiplayerListener.gameIsStarting(game.start());
		};
	}

	private PlayerSetting[] determinePlayerSettings() {
		Map<Byte, PlayerSetting> playerSettings = slotList.getItems().stream().sorted(Comparator.comparingInt(IMultiplayerSlot::getPosition))
				.map(slot -> {
					PlayerSetting playerSetting;
					if(iAmTheHost) {
						playerSetting = new PlayerSetting(slot.getType(), slot.getCivilisation(), slot.getTeam());
					} else {
						playerSetting = new PlayerSetting(EPlayerType.HUMAN, slot.getCivilisation(), slot.getTeam());
					}

					return Map.entry(slot.getPosition(), playerSetting);
				}).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
		return IntStream.range(0, maxPlayers).mapToObj(i -> playerSettings.computeIfAbsent((byte) i, v -> new PlayerSetting())).toArray(PlayerSetting[]::new);
	}

	private byte calculateOwnPlayerId() {
		String myId = networkClient.getPlayerInfo().getId();
		for (IMultiplayerSlot currSlot : slotList.getItems()) {
			if(currSlot.getPlayer() != null && currSlot.getPlayer().getId().equals(myId)) {
				return currSlot.getPosition();
			}
		}
		throw new RuntimeException("Wasn't able to find my id!");
	}

	private IPacketReceiver<MatchInfoUpdatePacket> generateMatchInfoUpdatedListener() {
		return packet -> {
			if (joiningGameListener != null) {
				joiningGameListener.gameJoined(generateJoinPhaseGameConnector());
				joiningGameListener = null;
			}

			updateLists(packet.getMatchInfo());
			if(packet.getUpdatedPlayer() != null) {
				receiveSystemMessage(new MultiplayerPlayer(packet.getUpdatedPlayer()), getNetworkMessageById(packet.getUpdateReason()));
			}
		};
	}

	void updateLists(MatchInfoPacket matchInfo) {
		maxPlayers = matchInfo.getMaxPlayers();
		List<IMultiplayerPlayer> players = new LinkedList<>();
		for (PlayerInfoPacket playerInfoPacket : matchInfo.getPlayers()) {
			players.add(new MultiplayerPlayer(playerInfoPacket));
		}
		playersList.setList(players);

		List<IMultiplayerSlot> slots = new LinkedList<>();
		SlotInfoPacket[] matchInfoSlots = matchInfo.getSlots();
		Iterator<IMultiplayerPlayer> playerIter = players.iterator();
		for (int i = 0, matchInfoSlotsLength = matchInfoSlots.length; i < matchInfoSlotsLength; i++) {
			SlotInfoPacket slotInfoPacket = matchInfoSlots[i];
			IMultiplayerSlot nextSlot;
			if(playerIter.hasNext()) {
				nextSlot = new MultiplayerSlot(slotInfoPacket, playerIter.next());
			} else {
				nextSlot = new MultiplayerSlot(slotInfoPacket);
			}
			slots.add(nextSlot);
		}
		slotList.setList(slots);
	}

	private ENetworkMessage getNetworkMessageById(NetworkConstants.ENetworkMessage errorMessageId) {
		switch (errorMessageId) {
		case INVALID_STATE_ERROR:
			return ENetworkMessage.INVALID_STATE_ERROR;
		case NO_LISTENER_FOUND:
			return ENetworkMessage.UNKNOWN_ERROR;
		case NOT_ALL_PLAYERS_READY:
			return ENetworkMessage.NOT_ALL_PLAYERS_READY;
		case PLAYER_JOINED:
			return ENetworkMessage.PLAYER_JOINED;
		case PLAYER_LEFT:
			return ENetworkMessage.PLAYER_LEFT;
		case UNAUTHORIZED:
			return ENetworkMessage.UNAUTHORIZED;
		case READY_STATE_CHANGED:
			return ENetworkMessage.READY_STATE_CHANGED;
		case UNKNOWN_ERROR:
		default:
			return ENetworkMessage.UNKNOWN_ERROR;
		}
	}

	void receiveSystemMessage(IMultiplayerPlayer author, ENetworkMessage networkMessage) {
		if (chatMessageListener != null) {
			chatMessageListener.systemMessageReceived(author, networkMessage);
		}
	}

	private IJoinPhaseMultiplayerGameConnector generateJoinPhaseGameConnector() {
		networkClient.registerRejectReceiver(packet -> {
			receiveSystemMessage(null, getNetworkMessageById(packet.getErrorMessageId()));
			System.out.println("Received reject packet: rejectedKey: " + packet.getRejectedKey() + " messageid: " + packet.getErrorMessageId());
		});

		return new IJoinPhaseMultiplayerGameConnector() {
			@Override
			public boolean startGame() {
				if (areAllPlayersReady()) {
					networkClient.startMatch();
					return true;
				} else {
					return false;
				}
			}

			@Override
			public void setReady(boolean ready) {
				networkClient.setReadyState(ready);
			}

			@Override
			public void setCivilisation(byte slot, ECivilisation civilisation) {
				networkClient.setCivilisation(slot, (byte) civilisation.ordinal);
			}

			@Override
			public void setTeam(byte slot, byte team) {
				networkClient.setTeam(slot, team);
			}

			@Override
			public void setType(byte slot, EPlayerType playerType) {
				networkClient.setType(slot, (byte) playerType.ordinal());
			}

			@Override
			public void setPosition(byte slot, byte position) {
				networkClient.setPosition(slot, position);
			}

			@Override
			public void setPlayerCount(int playerCount) {
				networkClient.setPlayerCount(playerCount);
			}

			@Override
			public void setMultiplayerListener(IMultiplayerListener multiplayerListener) {
				MultiplayerGame.this.multiplayerListener = multiplayerListener;
			}

			@Override
			public ChangingList<IMultiplayerPlayer> getPlayers() {
				return playersList;
			}

			@Override
			public ChangingList<IMultiplayerSlot> getSlots() {
				return slotList;
			}

			@Override
			public void abort() {
				networkClient.leaveMatch();
			}

			@Override
			public void setChatListener(IChatMessageListener chatMessageListener) {
				MultiplayerGame.this.chatMessageListener = chatMessageListener;
			}

			@Override
			public void sendChatMessage(String chatMessage) {
				networkClient.sendChatMessage(chatMessage);
			}

			private boolean areAllPlayersReady() {
				return playersList.getItems().stream().allMatch(IMultiplayerPlayer::isReady);
			}
		};
	}

}
