/*
*    	Copyright 2015-2017 GetSocial B.V.
*
*	Licensed under the Apache License, Version 2.0 (the "License");
*	you may not use this file except in compliance with the License.
*	You may obtain a copy of the License at
*
*    	http://www.apache.org/licenses/LICENSE-2.0
*
*	Unless required by applicable law or agreed to in writing, software
*	distributed under the License is distributed on an "AS IS" BASIS,
*	WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
*	See the License for the specific language governing permissions and
*	limitations under the License.
*/

package im.getsocial.demo.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;
import butterknife.BindView;
import butterknife.ButterKnife;
import im.getsocial.demo.R;

import java.util.List;

public class MenuAdapter extends RecyclerView.Adapter<MenuAdapter.MenuViewHolder> {

	private final List<MenuItem> _listData;

	public MenuAdapter(List<MenuItem> listData) {
		_listData = listData;
	}

	@Override
	public MenuViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
		View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_menu, parent, false);
		return new MenuViewHolder(view);
	}

	@Override
	public void onBindViewHolder(MenuViewHolder holder, int position) {
		holder.update(_listData.get(position));
	}

	@Override
	public int getItemCount() {
		return _listData.size();
	}

	static class MenuViewHolder extends RecyclerView.ViewHolder {

		final View _view;
		MenuItem.Action _action;

		@BindView(R.id.textViewTitle)
		TextView _textViewTitle;
		@BindView(R.id.textViewSubtitle)
		TextView _textViewSubtitle;

		MenuViewHolder(View itemView) {
			super(itemView);
			_view = itemView;
			ButterKnife.bind(this, itemView);
			_action = null;
			_view.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					if (_action != null) {
						_action.execute();
					}
				}
			});
		}

		void update(final MenuItem menuItem) {
			_textViewTitle.setText(menuItem.getTitle());

			final boolean hasSubtitle = menuItem.hasSubtitle();
			_textViewSubtitle.setText(hasSubtitle ? menuItem.getSubtitle() : null);
			_textViewSubtitle.setVisibility(hasSubtitle ? View.VISIBLE : View.GONE);
			
			final EnabledCheck enabledCheck = menuItem.getEnabledCheck();
			final boolean isEnabled = enabledCheck == null || enabledCheck.isOptionEnabled();
			_view.setEnabled(isEnabled);
			_textViewTitle.setEnabled(isEnabled);

			_action = menuItem.getAction();
		}
	}
}