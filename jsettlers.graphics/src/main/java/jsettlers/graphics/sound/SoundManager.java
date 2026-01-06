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
package jsettlers.graphics.sound;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Random;

import go.graphics.UIPoint;
import go.graphics.sound.ISoundDataRetriever;
import go.graphics.sound.SoundPlayer;
import jsettlers.common.CommonConstants;
import jsettlers.common.position.ShortPoint2D;
import jsettlers.common.sound.ESoundType;
import jsettlers.common.utils.FileUtils;
import jsettlers.graphics.map.MapDrawContext;
import jsettlers.graphics.image.reader.bytereader.ByteReader;
import jsettlers.graphics.map.ScreenPosition;


/*
 * This class manages reading and playing of the sound file.
 * <p>
 * Some known sounds:
 * <p>
 *
 * 0 lumberjack
 * 1 (6 times): bricklayer <br>
 * 2 (3 times): digger <br>
 * 3 (twice): stonecutter <br>
 * 4: plant tree?
 * 5: sawmiller <br>
 * 6/7 smith <br>
 * 8/9 and 12: farmer <br>
 * 13: fire
 * 14: dying pig <br>
 * 15/16/17: fisherman
 * 20: dockyard
 * 21: healer
 * 24: geologist
 * 25: bowman
 * 30: sword Soldier <br>
 * 31/32 (soldier ?) <br>
 * 33 Bowman <br>
 * 34 Pikeman
 * 35 soldier killed <br>
 * 36: falling tree <br>
 * 37/38 molten metal
 * 39: pigs <br>
 * 40/41: donkey <br>
 * 42: wind/mill: 5s <br>
 * 43/44: distillery
 * 45: charcoal burner coughing
 * 46-50 UI sounds
 * 48: decrease UI sound
 * 49: increase UI sound
 * 51: trigger for building destruction
 * 52: set distribution of goods
 * 54: pause construction
 * 56: lock storage <br>
 * 57: move action
 * 58-59: notification sounds<br>
 * 61 GUI click sound
 * 62: Ui klick <br>
 * 63-66: GUI click sounds
 * 67: desert
 * 68, 68b: Sea <br>
 * 69, 69b: Bird <br>
 * 70, 70b: Bird
 * 71: Water (river) <br>
 * 72 (+ alternaitves): moor <br>
 * 73: wind <br>
 * 74: crazy wind <br>
 * 75 (3 times): thunder <br>
 * 76 (2 times): rain <br>
 * 80: You are beeing attacked <br>
 * 81: Mill, <br>
 * 82: older mill, <br>
 * 83: even older mill <br>
 * 84: catapult <br>
 * 85: Arrow shooting <br>
 * 86 -90: canon shooting <br>
 * 91: fire <br>
 * 92: small fire on wood or building <br>
 * 93: collapsing building
 * 95: set worker area
 * 100-109: Attacked (same sound?) ? <br>
 * 110: announcement of missing tool
 * 111: 112: gong, <br>
 * 113: (4 times): amazone killed
 * 116: refused center of work displacement
 * 117: bees
 *
 * @author michael
 */
public class SoundManager {

	private static final String SOUND_FILE_NAME = "Siedler3_00.dat";

	private static final int SOUND_META_LENGTH = 16;
	private static final int SOUND_FILE_START = 0x24;
	private static final int SEQUENCE_N = 118;

	private static final byte[] SOUND_FILE_MAGIC = new byte[] {
			0x44,
			0x15,
			0x01,
			0x00,
			0x02,
			0x00,
			0x00,
			0x00,
			0x00,
			0x00,
			0x00,
			0x00,
			0x1C,
			0x00,
			0x00,
			0x00
	};

	private static File lookupPath;

	private final SoundPlayer soundPlayer;
	public final Random random = new Random();

	/**
	 * The start positions of all the playable sounds.
	 */
	private int[][] soundStarts;
	private boolean initializing = false;
	private MapDrawContext map = null;
    private ScreenPosition screen = null;

    /**
	 * Creates a new sound manager.
	 *
	 * @param soundPlayer
	 * 		The soundPlayer to play sounds at.
	 */
	public SoundManager(SoundPlayer soundPlayer) {
		this.soundPlayer = soundPlayer;
		initialize();
	}

	/**
	 * Reads the start indexes of the sounds.
	 *
	 * @param reader
	 * 		The reader to read from.
	 * @return An array of start indexes for each sound and it's variants.
	 * @throws IOException
	 * 		If the file could not be read.
	 */
	protected static int[][] getSoundStarts(ByteReader reader) throws IOException {
		int[] sequenceHeaderStarts = new int[SEQUENCE_N];
		for (int i = 0; i < SEQUENCE_N; i++) {
			sequenceHeaderStarts[i] = reader.read32();
		}

		int[][] playerIds = new int[SEQUENCE_N][];
		for (int i = 0; i < SEQUENCE_N; i++) {
			reader.skipTo(sequenceHeaderStarts[i]);
			int alternativeCount = reader.read32();
			int[] starts = new int[alternativeCount];
			for (int j = 0; j < alternativeCount; j++) {
				starts[j] = reader.read32();
			}

			playerIds[i] = starts;
		}
		return playerIds;
	}

	/**
	 * Opens the sound file.
	 *
	 * @return The file reader.
	 * @throws IOException
	 * 		If the file could not be opened,
	 */
	protected static ByteReader openSoundFile() throws IOException {
		File sndFile = getSoundFile();

		if (sndFile == null) {
			throw new IOException("Sound file not found.");
		}

		RandomAccessFile randomAccessFile = new RandomAccessFile(sndFile, "r");
		ByteReader reader = new ByteReader(randomAccessFile);

		reader.assumeToRead(SOUND_FILE_MAGIC);

		reader.skipTo(SOUND_FILE_START);
		return reader;
	}

	private static File getSoundFile() {
		return FileUtils.getFileByNameIgnoringCase(lookupPath, SOUND_FILE_NAME);
	}

	/**
	 * Plays a given sound.
	 *
	 * @param soundType
	 * 		The sound to play.
	 * @param volume
	 * 		The volume
	 */
	public void playSound(ESoundType soundType, float volume) {
		initialize();

		if (soundStarts != null && soundType != null && soundType.ordinal() < SEQUENCE_N) {
			int[] alternatives = soundStarts[soundType.ordinal()];
			if (alternatives != null && alternatives.length > 0) {
				int rand = random.nextInt(alternatives.length);
				soundPlayer.playSound(alternatives[rand], volume, volume);
			}
		}
	}

	public void playSound(ESoundType soundType, float volume, ShortPoint2D position) {
		playSound(soundType, volume, position.x, position.y, false);
	}
    public void playSound(ESoundType soundType, float volume, ShortPoint2D position, boolean playInFog) {
        playSound(soundType, volume, position.x, position.y, playInFog);
    }

	/**
	 * Plays a given sound at a given coordinate
	 *
	 * @param soundType
	 * 		The sound to play
	 * @param volume
	 * 		The volume
	 * @param x
	 * 		The x coordinate of the sound
	 * @param y
	 * 		The y coordinate of the sound
     * 	@param playInFog
     * 	    Whether to play the sound even if the position is in fog of war.
	 */
	public void playSound(ESoundType soundType, float volume, int x, int y, boolean playInFog) {
		if (map == null || (map.getVisibleStatus(x, y) <= CommonConstants.FOG_OF_WAR_EXPLORED && !playInFog)) { // only play sounds when fog of war level is higher than explored
			return;
		}

		initialize();

		if (soundStarts != null && soundType != null && soundType.ordinal() < SEQUENCE_N && screen != null) {
			int[] alternatives = soundStarts[soundType.ordinal()];
			if (alternatives != null && alternatives.length > 0) {
				int rand = random.nextInt(alternatives.length);;

                UIPoint soundScreenPos = map.getScreenPosition(x, y);
                float zoom = this.screen.getZoom();

                float width = this.screen.getWidth() * zoom;
                float height = this.screen.getHeight() * zoom;

                // Between 0 and 1, where 0 is left/bottom of the screen, 1 is right/top of the screen
                float screenX = (float)soundScreenPos.getX() / width;
                float screenY = (float)soundScreenPos.getY() / height;

                float zeroDistance = 1.0f;
                float fullDistance = 0.25f;
                float screenXCenter = 0.5f;
                float screenYCenter = 0.5f;

                float distance = (float) Math.sqrt((screenX - screenXCenter) * (screenX - screenXCenter) + (screenY - screenYCenter) * (screenY - screenYCenter));
                float distanceVolume = 1;
                if (distance > fullDistance) {
                	if (distance >= zeroDistance) {
                		distanceVolume = 0;
                	} else {
                		distanceVolume = (zeroDistance - distance) / (zeroDistance - fullDistance);
                	}
                }

                // Shift sound to left or right ear. 0 means both ears have full volume, 1 means only right ear has volume, -1 means only left ear has volume
                // We want to shift only to max -0.25/+0.25
                float shift = (screenX - screenXCenter) / screenXCenter * 0.25f;
                float leftVolume = 1;
                float rightVolume = 1;
                if (shift > 0) {
                	leftVolume = 1 - shift;
                } else if (shift < 0) {
                	rightVolume = 1 + shift;
                }

                // Zoom level volume. It should fade to 0.05 when zoomed out to minimum zoom
                float minZoomForFull = CommonConstants.MAXIMUM_ZOOM - (CommonConstants.MAXIMUM_ZOOM - CommonConstants.MINIMUM_ZOOM) / 2f;
                float minZoomVolume = 0.05f;
                float zoomVolume = 1;
                if (zoom < minZoomForFull) {
                	zoomVolume = minZoomVolume + (zoom - CommonConstants.MINIMUM_ZOOM) / (minZoomForFull - CommonConstants.MINIMUM_ZOOM) * (1 - minZoomVolume);
                }

                leftVolume *= distanceVolume * zoomVolume * volume;
                rightVolume *= distanceVolume * zoomVolume * volume;

				soundPlayer.playSound(alternatives[rand], leftVolume, rightVolume);
			}
		}
	}

	private void initialize() {
		synchronized (this) {
			if (initializing) {
				return;
			}
			initializing = true;
		}
		new Thread(() -> {
			try {
				loadSounds();
			} catch (Throwable e) {
				e.printStackTrace();
			}
		}, "sound loader").start();
	}

	private void loadSounds() throws IOException {
		ByteReader reader = openSoundFile();
		this.soundStarts = getSoundStarts(reader);
		soundPlayer.setSoundDataRetriever(new SoundDataRetriever(reader));
	}

	/**
	 * Sets the sound file lookup path.
	 *
	 * @param lookupPath
	 * 		The file path.
	 */
	public static void setLookupPath(File lookupPath) {
		SoundManager.lookupPath = lookupPath;
	}

	/**
	 * This class wraps an open {@link ByteReader} to a {@link ISoundDataRetriever}.
	 *
	 * @author Michael Zangl
	 */
	private static class SoundDataRetriever implements ISoundDataRetriever {
		private final ByteReader reader;

		/**
		 * Create a new {@link SoundDataRetriever}.
		 *
		 * @param reader
		 * 		The byte reader.
		 */
		SoundDataRetriever(ByteReader reader) {
			this.reader = reader;
		}

		@Override
		public synchronized short[] getSoundData(int soundStart) throws IOException {
			return SoundManager.getSoundData(reader, soundStart);
		}
	}

	/**
	 * Reads the sound data from a byte reader.
	 *
	 * @param reader
	 * 		The reader to read.
	 * @param start
	 * 		The sound start position.
	 * @return The read sound data.
	 * @throws IOException
	 * 		If that sound could not be read.
	 */
	protected static short[] getSoundData(ByteReader reader, int start) throws IOException {
		reader.skipTo(start);

		int length = reader.read32() / 2 - SOUND_META_LENGTH;
		reader.read32();
		reader.read32(); // mostly 22050
		reader.read32(); // mostly 44100
		reader.read32();

		return loadSound(reader, length);
	}

	private static short[] loadSound(ByteReader reader, int length) throws IOException {
		if (length < 0) {
			return new short[0];
		}
		short[] data = new short[length];
		for (int i = 0; i < length; i++) {
			data[i] = (short) reader.read16signed();
		}

		return data;
	}

	public void setMap(MapDrawContext map) {
		this.map = map;
        this.screen = map.getScreen();
	}
}
