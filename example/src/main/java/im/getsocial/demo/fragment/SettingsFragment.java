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

package im.getsocial.demo.fragment;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.util.Log;
import im.getsocial.demo.R;
import im.getsocial.demo.Utils;
import im.getsocial.demo.adapter.EnabledCheck;
import im.getsocial.demo.adapter.MenuItem;
import im.getsocial.demo.adapter.TextGenerator;
import im.getsocial.demo.dependencies.DependenciesContainer;
import im.getsocial.demo.dependencies.components.Clipboard;
import im.getsocial.sdk.Callback;
import im.getsocial.sdk.CompletionCallback;
import im.getsocial.sdk.GetSocial;
import im.getsocial.sdk.GetSocialException;
import im.getsocial.sdk.consts.LanguageCodes;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SettingsFragment extends BaseListFragment {

	private Boolean _notificationsEnabled = null;

	private Clipboard _clipboard;

	public SettingsFragment() {
		GetSocial.User.isPushNotificationsEnabled(new Callback<Boolean>() {
			@Override
			public void onSuccess(Boolean result) {
				setNotificationsEnabled(result);
			}

			@Override
			public void onFailure(GetSocialException exception) {
				Log.e("Notifications", "Failed to get notifications status: " + exception);
			}
		});
	}

	@Override
	protected void inject(DependenciesContainer dependencies) {
		super.inject(dependencies);
		_clipboard = dependencies.clipboard();
	}

	private void setNotificationsEnabled(boolean isEnabled) {
		_notificationsEnabled = isEnabled;
		invalidateList();
	}

	@Override
	protected List<MenuItem> createListData() {
		List<MenuItem> listData = new ArrayList<>();

		listData.add(new MenuItem.Builder("Change Language")
				.withAction(new MenuItem.Action() {
					@Override
					public void execute() {
						changeLanguage();
					}
				})
				.withSubtitle(new TextGenerator() {
					@Override
					public String generateText() {
						return String.format("Current language: %s", LanguageCodes.all().get(GetSocial.getLanguage()));
					}
				})
				.build());

		listData.add(new MenuItem.Builder("Enable Custom Error Message")
				.withAction(() -> {
					Utils.setCustomErrorMesageEnabled(getContext(), true);
					invalidateList();
				})
				.withEnabledCheck(new EnabledCheck() {
					@Override
					public boolean isOptionEnabled() {
						return !Utils.isCustomErrorMesageEnabled(getContext());
					}
				}).build());

		listData.add(new MenuItem.Builder("Disable Custom Error Message")
				.withAction(() -> {
					Utils.setCustomErrorMesageEnabled(getContext(), false);
					invalidateList();
				})
				.withEnabledCheck(new EnabledCheck() {
					@Override
					public boolean isOptionEnabled() {
						return Utils.isCustomErrorMesageEnabled(getContext());
					}
				}).build());

		listData.add(new MenuItem.Builder("Enable Push Notifications")
				.withAction(new ChangeNotificationsEnabledAction())
				.withEnabledCheck(new EnabledCheck() {
					@Override
					public boolean isOptionEnabled() {
						return checkPushNotificationStatus(false);
					}
				}).build());

		listData.add(new MenuItem.Builder("Disable Push Notifications")
				.withAction(new ChangeNotificationsEnabledAction())
				.withEnabledCheck(new EnabledCheck() {
					@Override
					public boolean isOptionEnabled() {
						return checkPushNotificationStatus(true);
					}
				}).build());

		listData.add(MenuItem.builder("Copy Device ID")
						.withAction(new MenuItem.Action() {
							@Override
							public void execute() {
								String deviceId = GetSocial.Device.getIdentifier();
								Log.d("GETSOCIAL", "Device ID: " + deviceId);
								_clipboard.copy(deviceId);
							}
						})
						.build());

		return listData;
	}

	private boolean checkPushNotificationStatus(boolean pnStatus) {
		return _notificationsEnabled != null && _notificationsEnabled == pnStatus;
	}

	//region Presenter
	private void changeLanguage() {
		final Map<String, String> all = LanguageCodes.all();

		final String[] providers = all.values().toArray(new String[all.size()]);
		final String[] codes = all.keySet().toArray(new String[all.size()]);

		new AlertDialog.Builder(getContext())
				.setTitle(R.string.select_language)
				.setNegativeButton(android.R.string.cancel, null)
				.setItems(providers,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int which) {
								GetSocial.setLanguage(codes[which]);
								invalidateList();
							}
						}
				)
				.create()
				.show();
	}

	@Override
	public String getTitle() {
		return "Settings";
	}

	@Override
	public String getFragmentTag() {
		return "settings";
	}

	private class ChangeNotificationsEnabledAction implements MenuItem.Action {

		@Override
		public void execute() {
			final boolean shouldEnable = !_notificationsEnabled;
			GetSocial.User.setPushNotificationsEnabled(shouldEnable, new CompletionCallback() {
				@Override
				public void onSuccess() {
					setNotificationsEnabled(shouldEnable);
				}

				@Override
				public void onFailure(GetSocialException exception) {
					Log.e("Notifications", "Failed to set notifications status: " + exception);
				}
			});
		}
	}
	//endregion
}
