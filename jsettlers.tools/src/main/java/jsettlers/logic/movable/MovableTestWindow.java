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
package jsettlers.logic.movable;

import java.io.IOException;

import jsettlers.TestToolUtils;
import jsettlers.common.action.EMoveToType;
import jsettlers.common.action.MoveToAction;
import jsettlers.common.ai.EPlayerType;
import jsettlers.common.material.EMaterialType;
import jsettlers.common.menu.IMapInterfaceConnector;
import jsettlers.common.movable.EMovableType;
import jsettlers.common.player.ECivilisation;
import jsettlers.input.SelectionSet;
import jsettlers.logic.constants.MatchConstants;
import jsettlers.logic.movable.interfaces.ILogicMovable;
import jsettlers.logic.movable.testmap.MovableTestsMap;
import jsettlers.logic.player.Player;
import jsettlers.logic.player.Team;
import jsettlers.logic.timer.RescheduleTimer;
import jsettlers.main.swing.lookandfeel.JSettlersLookAndFeelExecption;
import jsettlers.main.swing.resources.SwingResourceLoader;
import jsettlers.network.synchronic.timer.NetworkTimer;
import jsettlers.shared.ShortPoint2D;

public class MovableTestWindow {
	private static final Player PLAYER_0 = new Player((byte) 0, new Team((byte) 0), (byte) 1, EPlayerType.HUMAN, ECivilisation.ROMAN);
	private final ILogicMovable movable;

	public static void main(String args[]) throws InterruptedException, JSettlersLookAndFeelExecption, IOException, SwingResourceLoader.ResourceSetupException {
		new MovableTestWindow();
	}

	private MovableTestWindow() throws InterruptedException, JSettlersLookAndFeelExecption, IOException {
		MatchConstants.init(new NetworkTimer(true), 1000);
		MatchConstants.clock().startExecution();
		RescheduleTimer.schedule(MatchConstants.clock());

		MovableTestsMap grid = new MovableTestsMap(100, 100, PLAYER_0);
		IMapInterfaceConnector connector = TestToolUtils.openTestWindow(grid);

		movable = Movable.createMovable(EMovableType.PIONEER, PLAYER_0, new ShortPoint2D(49, 50), grid.getMovableGrid());
		movable.setSelected(true);

		connector.setSelection(new SelectionSet(movable));

		connector.addListener(action -> {
			switch (action.getActionType()) {
			case MOVE_TO:
				movable.moveTo(((MoveToAction) action).getPosition(), ((MoveToAction) action).getMoveToType());
				break;
			case SPEED_FASTER:
				MatchConstants.clock().multiplyGameSpeed(1.2f);
				break;
			case SPEED_SLOWER:
				MatchConstants.clock().multiplyGameSpeed(1 / 1.2f);
				break;
			case FAST_FORWARD:
				MatchConstants.clock().fastForward();
				break;
			default:
				break;
			}
		});

		grid.getMovableGrid().dropMaterial(new ShortPoint2D(40, 40), EMaterialType.PLANK, true, false);
		grid.getMovableGrid().dropMaterial(new ShortPoint2D(60, 60), EMaterialType.STONE, true, false);

		Movable.createMovable(EMovableType.BEARER, PLAYER_0, new ShortPoint2D(30, 30), grid.getMovableGrid());
		Movable.createMovable(EMovableType.BEARER, PLAYER_0, new ShortPoint2D(31, 31), grid.getMovableGrid());
		Movable.createMovable(EMovableType.BEARER, PLAYER_0, new ShortPoint2D(32, 32), grid.getMovableGrid());
		Movable.createMovable(EMovableType.BEARER, PLAYER_0, new ShortPoint2D(33, 33), grid.getMovableGrid());

		Movable.createMovable(EMovableType.BEARER, PLAYER_0, new ShortPoint2D(50, 50), grid.getMovableGrid());

		{// test automatic distribution of many movables next to each other
			for (int x = 30; x < 40; x++) {
				for (int y = 80; y < 90; y++) {
					Movable.createMovable(EMovableType.BEARER, PLAYER_0, new ShortPoint2D(x, y), grid.getMovableGrid());
				}
			}
		}

		{
			Thread.sleep(3000L);
			// circle of three movables blocking each others path
			ILogicMovable m1 = Movable.createMovable(EMovableType.PIONEER, PLAYER_0, new ShortPoint2D(50, 64), grid.getMovableGrid());
			ILogicMovable m2 = Movable.createMovable(EMovableType.PIONEER, PLAYER_0, new ShortPoint2D(51, 65), grid.getMovableGrid());
			ILogicMovable m3 = Movable.createMovable(EMovableType.PIONEER, PLAYER_0, new ShortPoint2D(50, 64), grid.getMovableGrid());

			m1.moveTo(new ShortPoint2D(52, 65), EMoveToType.DEFAULT);
			m2.moveTo(new ShortPoint2D(49, 63), EMoveToType.DEFAULT);
			m3.moveTo(new ShortPoint2D(50, 66), EMoveToType.DEFAULT);
		}
	}
}
