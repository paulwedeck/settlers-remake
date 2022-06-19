/*******************************************************************************
 * Copyright (c) 2015 - 2018
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

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;

import java.util.function.Consumer;

import jsettlers.ai.highlevel.AiExecutor;
import jsettlers.common.CommitInfo;
import jsettlers.common.CommonConstants;
import jsettlers.common.logging.MultiplexingOutputStream;
import jsettlers.common.map.IGraphicsGrid;
import jsettlers.common.menu.EGameError;
import jsettlers.common.menu.EProgressState;
import jsettlers.common.menu.IMapInterfaceConnector;
import jsettlers.common.menu.IStartedGame;
import jsettlers.common.menu.IStartingGame;
import jsettlers.common.menu.IStartingGameListener;
import jsettlers.common.player.IInGamePlayer;
import jsettlers.common.resources.ResourceManager;
import jsettlers.common.statistics.IGameTimeProvider;
import jsettlers.input.GuiInterface;
import jsettlers.input.IGameStoppable;
import jsettlers.input.PlayerState;
import jsettlers.logic.buildings.Building;
import jsettlers.logic.buildings.trading.HarborBuilding;
import jsettlers.logic.buildings.trading.MarketBuilding;
import jsettlers.logic.constants.MatchConstants;
import jsettlers.logic.map.grid.MainGrid;
import jsettlers.logic.map.loading.IGameCreator;
import jsettlers.logic.map.loading.IGameCreator.MainGridWithUiSettings;
import jsettlers.logic.map.loading.MapLoadException;
import jsettlers.logic.map.loading.MapLoader;
import jsettlers.logic.movable.MovableManager;
import jsettlers.logic.player.InitialGameState;
import jsettlers.logic.player.PlayerSetting;
import jsettlers.logic.timer.RescheduleTimer;
import jsettlers.main.replay.ReplayUtils;
import jsettlers.network.client.OfflineNetworkConnector;
import jsettlers.network.client.interfaces.INetworkConnector;

/**
 * This class can start a Thread that loads and sets up a game and wait's for its termination.
 *
 * @author Andreas Eberle
 */
public class JSettlersGame {
	private static final SimpleDateFormat LOG_DATE_FORMATTER = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US);
	private final Object stopMutex = new Object();

	private final IGameCreator mapCreator;
	private final INetworkConnector networkConnector;
	private final boolean multiplayer;
	private final DataInputStream replayFileInputStream;

	private final GameRunner gameRunner;
	private final InitialGameState initialGameState;

	private boolean started = false;
	private boolean stopped = false;
	private boolean shutdownFinished;

	private PrintStream systemErrorStream;
	private PrintStream systemOutStream;

	private JSettlersGame(IGameCreator mapCreator, INetworkConnector networkConnector, InitialGameState initialGameState,
			boolean controlAll, boolean multiplayer, DataInputStream replayFileInputStream) {
		configureLogging(mapCreator);

		this.initialGameState = initialGameState;

		System.out.println("OS version: " + System.getProperty("os.name") + " " + System.getProperty("os.arch") + " "
				+ System.getProperty("os.version"));
		System.out.println("Java version: " + System.getProperty("java.vendor") + " " + System.getProperty("java.version"));
		System.out.println("JSettlers version: " + CommitInfo.COMMIT_HASH_SHORT + " " + CommitInfo.BUILD_TIME);
		System.out.println("JSettlersGame(): initialGameState: " + initialGameState + " multiplayer: " + multiplayer + " mapCreator: " + mapCreator);

		if (mapCreator == null) {
			throw new IllegalArgumentException("No mapCreator provided (mapCreator==null).");
		}

		this.mapCreator = mapCreator;
		this.networkConnector = networkConnector;
		this.multiplayer = multiplayer;
		this.replayFileInputStream = replayFileInputStream;

		MatchConstants.ENABLE_ALL_PLAYER_FOG_OF_WAR = controlAll;
		MatchConstants.ENABLE_ALL_PLAYER_SELECTION = controlAll;
		MatchConstants.ENABLE_FOG_OF_WAR_DISABLING = controlAll;
		MatchConstants.ENABLE_DEBUG_COLORS = controlAll;

		this.gameRunner = new GameRunner();
	}

	/**
	 * @param mapCreator
	 * @param networkConnector
	 */
	public JSettlersGame(IGameCreator mapCreator, INetworkConnector networkConnector, InitialGameState initialGameState) {
		this(mapCreator, networkConnector, initialGameState, CommonConstants.CONTROL_ALL, true, null);
	}

	/**
	 * Creates a new {@link JSettlersGame} object with an {@link OfflineNetworkConnector}.
	 *
	 * @param mapCreator
	 */
	public JSettlersGame(IGameCreator mapCreator, InitialGameState initialGameState) {
		this(mapCreator, new OfflineNetworkConnector(), initialGameState, CommonConstants.CONTROL_ALL, false, null);
	}

	public static JSettlersGame loadFromReplayFile(ReplayUtils.IReplayStreamProvider loadableReplayFile, INetworkConnector networkConnector, ReplayStartInformation replayStartInformation)
			throws MapLoadException {
		try {
			DataInputStream replayFileInputStream = new DataInputStream(loadableReplayFile.openStream());
			replayStartInformation.deserialize(replayFileInputStream);

			MapLoader mapCreator = loadableReplayFile.getMap(replayStartInformation);
			return new JSettlersGame(mapCreator, networkConnector, replayStartInformation.getReplayableGameState(), true, false, replayFileInputStream);
		} catch (IOException e) {
			throw new MapLoadException("Could not deserialize " + loadableReplayFile, e);
		}
	}

	/**
	 * Starts the game in a new thread. Returns immediately.
	 *
	 * @return
	 */
	public synchronized IStartingGame start() {
		if (!started) {
			started = true;
			new Thread(null, gameRunner, "GameThread", 16 * 1024 * 1024).start();
		}
		return gameRunner;
	}

	public void stop() {
		synchronized (stopMutex) {
			stopped = true;
			stopMutex.notifyAll();
		}
	}

	protected OutputStream createReplayWriteStream() throws IOException {
		final String replayFilename = getLogFile(mapCreator, "_replay.log");
		return ResourceManager.writeUserFile(replayFilename);
	}

	public class GameRunner implements Runnable, IStartingGame, IStartedGame, IGameStoppable {
		private IStartingGameListener startingGameListener;
		private MainGrid mainGrid;
		private GameTimeProvider gameTimeProvider;
		private EProgressState progressState;
		private float progress;
		private Consumer<IStartedGame> exitListener;
		private boolean gameRunning;
		private AiExecutor aiExecutor;

		@Override
		public void run() {
			try {
				if (startingGameListener != null) {
					startingGameListener.startingLoadingGame();
				}
				updateProgressListener(EProgressState.LOADING, 0.1f);

				clearState();
				MatchConstants.init(networkConnector.getGameClock(), initialGameState.getRandomSeed());
				try {
					MatchConstants.clock().setReplayLogStream(createReplayFileStream());
				} catch (IOException e) {
					// TODO: log that we do not have write access to resources.
					System.out.println("Cannot write jsettlers.integration.replay file.");
				}

				updateProgressListener(EProgressState.LOADING_MAP, 0.3f);

				MainGridWithUiSettings gridWithUiState = mapCreator.loadMainGrid(initialGameState.getPlayerSettings(), initialGameState.getStartResources());
				mainGrid = gridWithUiState.getMainGrid();
				PlayerState playerState = gridWithUiState.getPlayerState(initialGameState.getPlayerId());

				RescheduleTimer.schedule(MatchConstants.clock()); // schedule timer

				updateProgressListener(EProgressState.LOADING_IMAGES, 0.7f);
				gameTimeProvider = new GameTimeProvider(MatchConstants.clock());

				mainGrid.initForPlayer(initialGameState.getPlayerId(), playerState.getFogOfWar());
				mainGrid.startThreads();

				waitForStartingGameListener();
				startingGameListener.waitForPreloading();

				updateProgressListener(EProgressState.WAITING_FOR_OTHER_PLAYERS, 0.98f);

				if (replayFileInputStream != null) {
					MatchConstants.clock().loadReplayLogFromStream(replayFileInputStream);
				}

				networkConnector.setStartFinished(true);
				waitForAllPlayersStartFinished(networkConnector);

				final IMapInterfaceConnector connector = startingGameListener.preLoadFinished(this);
				GuiInterface guiInterface = new GuiInterface(connector, MatchConstants.clock(), networkConnector.getTaskScheduler(),
						mainGrid.getGuiInputGrid(), this, initialGameState.getPlayerId(), multiplayer);
				connector.loadUIState(playerState.getUiState()); // This is required after the GuiInterface instantiation so that
				// ConstructionMarksThread has it's mapArea variable initialized via the EActionType.SCREEN_CHANGE event.

				aiExecutor = new AiExecutor(initialGameState.getPlayerSettings(), mainGrid, networkConnector.getTaskScheduler());
				networkConnector.getGameClock().schedule(aiExecutor, (short) 1000);

				MatchConstants.clock().startExecution(); // WARNING: GAME CLOCK IS STARTED!
				// NO CONFIGURATION AFTER THIS POINT! =================================
				gameRunning = true;

				startingGameListener.startFinished();

				synchronized (stopMutex) {
					while (!stopped) {
						try {
							stopMutex.wait();
						} catch (InterruptedException e) {
						}
					}
				}

				networkConnector.shutdown();
				mainGrid.stopThreads();
				connector.shutdown();
				guiInterface.stop();
				clearState();

				System.setErr(systemErrorStream);
				System.setOut(systemOutStream);

			} catch (MapLoadException e) {
				e.printStackTrace();
				reportFail(EGameError.MAPLOADING_ERROR, e);
			} catch (Exception e) {
				e.printStackTrace();
				reportFail(EGameError.UNKNOWN_ERROR, e);
			} finally {
				shutdownFinished = true;
				if (exitListener != null) {
					exitListener.accept(this);
				}
			}
		}

		public AiExecutor getAiExecutor() {
			return aiExecutor;
		}

		private DataOutputStream createReplayFileStream() throws IOException {
			DataOutputStream replayFileStream = new DataOutputStream(createReplayWriteStream());

			ReplayStartInformation replayInfo = new ReplayStartInformation(mapCreator.getMapName(), mapCreator.getMapId(), initialGameState);
			replayInfo.serialize(replayFileStream);
			replayFileStream.flush();

			return replayFileStream;
		}

		/**
		 * Waits until the {@link #startingGameListener} has been set.
		 */
		private void waitForStartingGameListener() {
			while (startingGameListener == null) {
				try {
					Thread.sleep(5L);
				} catch (InterruptedException e) {
				}
			}
		}

		private void waitForAllPlayersStartFinished(INetworkConnector networkConnector) {
			while (!networkConnector.haveAllPlayersStartFinished()) {
				try {
					Thread.sleep(5L);
				} catch (InterruptedException e) {
				}
			}
		}

		private void updateProgressListener(EProgressState progressState, float progress) {
			this.progressState = progressState;
			this.progress = progress;

			if (startingGameListener != null) {
				startingGameListener.startProgressChanged(progressState, progress);
			}
		}

		private void reportFail(EGameError gameError, Exception e) {
			if (startingGameListener != null)
				startingGameListener.startFailed(gameError, e);
		}

		// METHODS of IStartingGame
		// ====================================================
		@Override
		public void setListener(IStartingGameListener startingGameListener) {
			this.startingGameListener = startingGameListener;
			if (startingGameListener != null)
				startingGameListener.startProgressChanged(progressState, progress);
		}

		// METHODS of IStartedGame
		// ======================================================
		@Override
		public IGraphicsGrid getMap() {
			return mainGrid.getGraphicsGrid();
		}

		@Override
		public IGameTimeProvider getGameTimeProvider() {
			return gameTimeProvider;
		}

		@Override
		public IInGamePlayer getInGamePlayer() {
			return mainGrid.getPartitionsGrid().getPlayer(initialGameState.getPlayerId());
		}

		@Override
		public IInGamePlayer[] getAllInGamePlayers() {
			return mainGrid.getPartitionsGrid().getPlayers();
		}

		@Override
		public boolean isShutdownFinished() {
			return shutdownFinished;
		}

		@Override
		public boolean isMultiplayerGame() {
			return multiplayer;
		}

		@Override
		public void stopGame() {
			stop();
		}

		@Override
		public void setGameExitListener(Consumer<IStartedGame> exitListener) {
			this.exitListener = exitListener;
		}

		@Override
		public boolean isStartupFinished() {
			return gameRunning;
		}

		public MainGrid getMainGrid() {
			return mainGrid;
		}
	}

        /**
         * Configures logging for the given mapcreator.
         * 
         * Depending on CommonConstants.ENABLE_CONSOLE_LOGGING the logging
         * will go to the logfile only or to console and logfile. The logfile
         * will be calculated using getLogFile(mapcreator, "_out.log").
         * 
         * @param mapcreator the map for which to set the logfile
         */
	private void configureLogging(final IGameCreator mapcreator) {
		try {
			systemErrorStream = System.err;
			systemOutStream = System.out;

			OutputStream logStream;
			OutputStream errStream;
                        String logfile = getLogFile(mapcreator, "_out.log");
			OutputStream logFileStream = ResourceManager.writeUserFile(logfile);
			if(CommonConstants.ENABLE_CONSOLE_LOGGING) {
                                System.out.println("Duplicating stdout and stderr to "+new File(logfile).getAbsolutePath());
				logStream = new MultiplexingOutputStream(System.out, logFileStream);
				errStream = new MultiplexingOutputStream(System.err, logFileStream);
			} else {
                                System.out.println("Redirecting stdout and stderr to "+new File(logfile).getAbsolutePath());
				logStream = logFileStream;
				errStream = logFileStream;
			}

			System.setOut(new PrintStream(logStream));
			System.setErr(new PrintStream(errStream));
		} catch (IOException ex) {
			throw new RuntimeException("Error setting up logging.", ex);
		}
	}

        /**
         * Calculates the logfile based on map and date.
         * 
         * @param mapcreator The map used
         * @param suffix the suffix for the logfile
         * @return a relative path to the desired logfile
         */
	private static String getLogFile(IGameCreator mapcreator, String suffix) {
		final String dateAndMap = getLogDateFormatter().format(new Date()) + "_" + mapcreator.getMapName().replace(" ", "_");
		final String logFolder = "logs/" + dateAndMap + "/";

		return logFolder + dateAndMap + suffix;
	}

	private static DateFormat getLogDateFormatter() {
		return LOG_DATE_FORMATTER;
	}

	public static void clearState() {
		RescheduleTimer.stopAndClear();
		MovableManager.resetState();
		Building.clearState();
		MatchConstants.clearState();
	}
}
