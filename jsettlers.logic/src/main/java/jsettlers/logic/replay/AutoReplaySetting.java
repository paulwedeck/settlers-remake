/*******************************************************************************
 * Copyright (c) 2016 - 2018
 * <p/>
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"),
 * to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 * <p/>
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 * <p/>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 *******************************************************************************/
package jsettlers.logic.replay;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;

import java.util.List;
import jsettlers.logic.map.loading.MapLoadException;
import jsettlers.logic.map.loading.MapLoader;
import jsettlers.logic.map.loading.list.MapList;
import jsettlers.logic.utils.MapUtils;
import jsettlers.main.replay.ReplayUtils.IReplayStreamProvider;

/**
 * Created by michael on 24.04.16.
 */
public class AutoReplaySetting {
	public static Collection<AutoReplaySetting> getDefaultSettings() {
		return List.of(
				// TODO currently broken
			//new AutoReplaySetting("fullproduction", 0, 10, 20, 40, 65, 90)
		);
	}

	private final String typeName;
	private final int[]  timeMinutes;

	private AutoReplaySetting(String typeName, int... timeMinutes) {
		this.typeName = typeName;
		this.timeMinutes = timeMinutes;
	}

	private String getTypeName() {
		return typeName;
	}

	public int[] getTimeMinutes() {
		return timeMinutes;
	}

	private String getPath(int index) {
		return getTypeName() + "/savegame-" + timeMinutes[index] + "m.zmap";
	}

	public MapLoader getMap() throws MapLoadException {
		return MapUtils.getMap(getClass(), getTypeName() + "/base.rmap");
	}

	private String getReplayName() {
		return getTypeName() + "/replay.log";
	}

	public IReplayStreamProvider getReplayFile() throws MapLoadException {
		return MapUtils.createReplayForResource(getClass(), getReplayName(), getMap());
	}

	public MapLoader getReferenceSavegame(int index) throws MapLoadException {
		String replayPath = getReplayPath(index);

		System.out.println("Using reference file: " + replayPath);
		return MapLoader.getLoaderForListedMap(new MapList.ListedResourceMap(replayPath));
	}

	public String getReplayPath(int index) {
		return "/" + getClass().getPackage().getName().replace('.', '/') + "/" + getPath(index);
	}

	@Override
	public String toString() {
		return "AutoReplaySetting{" +
			"typeName='" + typeName + '\'' +
			", timeMinutes=" + Arrays.toString(timeMinutes) +
			'}';
	}

	public void compareSaveGamesAndDelete(MapLoader[] actualSaveGames) throws MapLoadException, IOException, ClassNotFoundException {
		for (int i = 0; i < actualSaveGames.length; i++) {
			MapLoader actualSaveGame = actualSaveGames[i];
			MapLoader expectedSaveGame = getReferenceSavegame(i);

			MapUtils.compareMapFiles(expectedSaveGame, actualSaveGame);
			actualSaveGame.getListedMap().delete();
		}
	}
}
