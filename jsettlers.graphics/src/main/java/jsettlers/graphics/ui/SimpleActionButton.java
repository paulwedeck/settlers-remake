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
package jsettlers.graphics.ui;

import jsettlers.common.images.ImageLink;
import jsettlers.common.action.EActionType;
import jsettlers.common.action.Action;
import jsettlers.common.sound.ESoundType;
import jsettlers.graphics.localization.Labels;

import java.util.Map;

public class SimpleActionButton extends Button {

    private final static Map<EActionType, ESoundType> triggerSoundMap = Map.of(
            EActionType.ASK_SET_WORK_AREA, ESoundType.UI_CLICK_GENERAL,
            EActionType.ASK_DESTROY, ESoundType.BUILDING_DESTRUCTION,
            EActionType.ASK_SET_DOCK, ESoundType.UI_INCREASE,
            EActionType.MAKE_FERRY, ESoundType.UI_CLICK_GENERAL,
            EActionType.MAKE_CARGO_SHIP, ESoundType.UI_CLICK_GENERAL,
            EActionType.SOLDIERS_ONE, ESoundType.UI_DECREASE_MAX,
            EActionType.SOLDIERS_ALL, ESoundType.UI_INCREASE_MAX
            // SOLDIERS_LESS and SOLDIERS_MORE are SoldierButtons
    );

    public SimpleActionButton(EActionType actionType, ImageLink image, ImageLink active) {
        super(new Action(actionType, triggerSoundMap.getOrDefault(actionType, null)), image, active, Labels.getName(actionType));
    }

	public SimpleActionButton(EActionType actionType, ImageLink image) {
		this(actionType, image, image);
	}

}
