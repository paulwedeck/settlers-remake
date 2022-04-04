/*******************************************************************************
 * Copyright (c) 2017 - 2018
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"),
 * to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 * <p>
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 *******************************************************************************/
package jsettlers.logic.movable.interfaces;

import java.io.Serializable;

import jsettlers.algorithms.fogofwar.MovableFoWTask;
import jsettlers.algorithms.path.IPathCalculatable;
import jsettlers.common.movable.EEffectType;
import jsettlers.common.action.EMoveToType;
import jsettlers.common.movable.IGraphicsMovable;
import jsettlers.common.position.ILocatable;
import jsettlers.common.position.ShortPoint2D;
import jsettlers.input.IGuiMovable;
import jsettlers.logic.player.Player;
import jsettlers.logic.timer.IScheduledTimerable;

public interface ILogicMovable extends IScheduledTimerable, IPathCalculatable, IDebugable, Serializable, IGuiMovable, ILocatable, IGraphicsMovable, MovableFoWTask {
	void push(ILogicMovable pushingMovable);

	ShortPoint2D getPosition();

	void leavePosition();

	Player getPlayer();

	void moveTo(ShortPoint2D targetPosition, EMoveToType moveToType);
	void addEffect(EEffectType effect);

	void setPosition(ShortPoint2D to);


	/**
	 * Lets this movable stop or start its work.
	 *
	 * @param stop
	 * 		if true this selectable should stop working<br>
	 * 		if false, it should stop working.
	 */
	void stopOrStartWorking(boolean stop);
}
