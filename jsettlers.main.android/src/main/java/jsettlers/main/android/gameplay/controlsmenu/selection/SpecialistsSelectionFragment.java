/*
 * Copyright (c) 2017
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
 */

package jsettlers.main.android.gameplay.controlsmenu.selection;

import org.androidannotations.annotations.Click;
import org.androidannotations.annotations.EFragment;
import org.androidannotations.annotations.ViewById;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import jsettlers.common.action.ConvertAction;
import jsettlers.common.action.EActionType;
import jsettlers.common.movable.EMovableType;
import jsettlers.common.player.ECivilisation;
import jsettlers.graphics.map.draw.ECommonLinkType;
import jsettlers.graphics.map.draw.ImageLinkMap;
import jsettlers.main.android.R;
import jsettlers.main.android.core.controls.ActionControls;
import jsettlers.main.android.core.controls.ControlsResolver;
import jsettlers.main.android.core.resources.OriginalImageProvider;

/**
 * Created by tompr on 13/01/2017.
 */
@EFragment(R.layout.menu_selection_specialists)
public class SpecialistsSelectionFragment extends SelectionFragment {
	private static final EMovableType[] specialistTypes = new EMovableType[] {
			EMovableType.PIONEER,
			EMovableType.THIEF,
			EMovableType.GEOLOGIST,
	};

	public static SpecialistsSelectionFragment newInstance() {
		return new SpecialistsSelectionFragment_();
	}

	@ViewById(R.id.layout_specialists)
	LinearLayout specialistsLayout;

	ActionControls actionControls;

	@Override
	public void onActivityCreated(@Nullable Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		actionControls = new ControlsResolver(getActivity()).getActionControls();

		LayoutInflater layoutInflater = LayoutInflater.from(getActivity());

		for (EMovableType movableType : specialistTypes) {
			int count = getSelection().getMovableCount(movableType, null);

			if (count > 0) {
				View view = layoutInflater.inflate(R.layout.view_specialist, specialistsLayout, false);
				ImageView imageView = (ImageView) view.findViewById(R.id.image_view_specialist);
				TextView textView = (TextView) view.findViewById(R.id.text_view_specialist_count);

				ECivilisation civilisation = getSelection().get(0).getPlayer().getCivilisation();

				OriginalImageProvider.get(ImageLinkMap.get(civilisation, ECommonLinkType.SETTLER_GUI, movableType)).setAsImage(imageView);
				textView.setText(count + "");

				specialistsLayout.addView(view);
			}
		}
	}

	@Click(R.id.button_convert_carriers)
	void convertToCarriersClicked() {
		actionControls.fireAction(new ConvertAction(EMovableType.BEARER, Short.MAX_VALUE));
	}

	@Click(R.id.button_work_here)
	void workHereClicked() {
		actionControls.fireAction(EActionType.START_WORKING);
	}

	@Click(R.id.button_halt)
	void haltClicked() {
		actionControls.fireAction(EActionType.STOP_WORKING);
	}
}
