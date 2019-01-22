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

package im.getsocial.demo;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.CallbackManager;
import com.vk.sdk.VKSdk;

import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnLongClick;
import im.getsocial.demo.dependencies.DependenciesContainer;
import im.getsocial.demo.dependencies.components.NotificationsManager;
import im.getsocial.demo.dialog.CurrentUserInfoDialog;
import im.getsocial.demo.dialog.ReferralDataDialog;
import im.getsocial.demo.dialog.UserInfoDialog;
import im.getsocial.demo.fragment.BaseFragment;
import im.getsocial.demo.fragment.ConsoleFragment;
import im.getsocial.demo.fragment.FriendsFragment;
import im.getsocial.demo.fragment.HasFragmentTag;
import im.getsocial.demo.fragment.HasTitle;
import im.getsocial.demo.fragment.RootFragment;
import im.getsocial.demo.plugin.FacebookSharePlugin;
import im.getsocial.demo.plugin.KakaoInvitePlugin;
import im.getsocial.demo.plugin.VKInvitePlugin;
import im.getsocial.demo.ui.PickActionView;
import im.getsocial.demo.ui.UserInfoView;
import im.getsocial.demo.utils.CompatibilityUtils;
import im.getsocial.demo.utils.Console;
import im.getsocial.demo.utils.NotificationContext;
import im.getsocial.demo.utils.NotificationHandler;
import im.getsocial.demo.utils.SimpleLogger;
import im.getsocial.sdk.Callback;
import im.getsocial.sdk.GetSocial;
import im.getsocial.sdk.GetSocialException;
import im.getsocial.sdk.actions.Action;
import im.getsocial.sdk.pushnotifications.ActionButton;
import im.getsocial.sdk.actions.ActionDataKeys;
import im.getsocial.sdk.actions.ActionListener;
import im.getsocial.sdk.actions.ActionTypes;
import im.getsocial.sdk.invites.FetchReferralDataCallback;
import im.getsocial.sdk.invites.InviteChannelIds;
import im.getsocial.sdk.invites.ReferralData;
import im.getsocial.sdk.pushnotifications.Notification;
import im.getsocial.sdk.pushnotifications.NotificationContent;
import im.getsocial.sdk.pushnotifications.NotificationListener;
import im.getsocial.sdk.pushnotifications.NotificationStatus;
import im.getsocial.sdk.pushnotifications.NotificationsSummary;
import im.getsocial.sdk.pushnotifications.SendNotificationPlaceholders;
import im.getsocial.sdk.usermanagement.OnUserChangedListener;
import im.getsocial.sdk.usermanagement.PublicUser;

public class MainActivity extends AppCompatActivity implements BaseFragment.ActivityListener, OnUserChangedListener, ActionListener, NotificationHandler {

	protected SimpleLogger _log;

	private static final String KEY_APP_SESSION = "GetSocial_AppSession_Key";

	protected final CallbackManager _facebookCallbackManager = CallbackManager.Factory.create();
	protected VKInvitePlugin _vkInvitePlugin;

	private ViewContainer _viewContainer;
	private final Map<String, String> _demoAppSessionData = new HashMap<>();

	private DependenciesContainer _dependenciesContainer;

	@Override
	protected void onCreate(@Nullable Bundle savedInstanceState) {
		_dependenciesContainer = new DependenciesContainer() {

			private NotificationsManager _notificationsManager = new NotificationsManager(getApplicationContext());

			@Override
			public NotificationsManager notificationsManager() {
				return _notificationsManager;
			}

			@Override
			public ActionListener actionListener() {
				return MainActivity.this;
			}

			@Override
			public NotificationHandler notificationHandler() {
				return MainActivity.this;
			}
		};
		super.onCreate(savedInstanceState);
		_log = new SimpleLogger(this, getClass().getSimpleName());

		setContentView(R.layout.activity_main);
		_viewContainer = new ViewContainer(this);

		if (savedInstanceState == null) {
			addRootFragment();
		}

		_vkInvitePlugin = new VKInvitePlugin(this);

		setupGetSocial();
	}

	protected void addRootFragment() {
		addContentFragment(new RootFragment());
	}

	@Override
	public void onSaveInstanceState(Bundle outState, PersistableBundle outPersistentState) {
		outState.putSerializable(KEY_APP_SESSION, new HashMap<>(_demoAppSessionData));

		super.onSaveInstanceState(outState, outPersistentState);
	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);

		if (savedInstanceState.containsKey(KEY_APP_SESSION)) {
			Map<String, String> map = (Map<String, String>) savedInstanceState.getSerializable(KEY_APP_SESSION);
			_demoAppSessionData.putAll(map);
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		_facebookCallbackManager.onActivityResult(requestCode, resultCode, data);
		VKSdk.onActivityResult(requestCode, resultCode, data, _vkInvitePlugin);
	}

	@Override
	public void onBackPressed() {
		if (getSupportFragmentManager().getBackStackEntryCount() == 1) {
			finish();
		} else {
			super.onBackPressed();
		}
	}

	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);

		// setIntent is needed to call in case your Activity launch mode is set to
		// singleTop or singleTask, so in GetSocial SDK gets the
		// last instance of Intent that brought Activity to foreground
		setIntent(intent);
	}

	@Override
	protected void onResume() {
		super.onResume();
		_viewContainer.updateView();
	}

	@Nullable
	private Fragment getActiveFragment() {
		if (getSupportFragmentManager().getBackStackEntryCount() == 0) {
			return null;
		}
		String tag = getSupportFragmentManager().getBackStackEntryAt(getSupportFragmentManager().getBackStackEntryCount() - 1).getName();

		return getSupportFragmentManager().findFragmentByTag(tag);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.menu_main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == R.id.action_console) {
			openConsole();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	private void openConsole() {
		addContentFragment(new ConsoleFragment());
	}

	protected void setupGetSocial() {
		Console.logInfo(getDemoAppInfo());
		GetSocial.setNotificationListener(new NotificationListener() {

			public boolean onNotificationReceived(Notification notification, boolean wasClicked) {
				return handleNotification(notification, NotificationContext.clicked(wasClicked));
			}
		});

		GetSocial.User.setOnUserChangedListener(this);
		GetSocial.whenInitialized(new Runnable() {
			@Override
			public void run() {
				invalidateUi();
				GetSocial.getReferralData(new FetchReferralDataCallback() {
					@Override
					public void onSuccess(@Nullable ReferralData referralData) {
						final String textToDisplay = referralData == null ?  "No referral data." : "Referral data received: [ " + referralData + " ]";
						_log.logInfo(textToDisplay);
						showReferralData(textToDisplay);
					}

					@Override
					public void onFailure(GetSocialException e) {
						_log.logErrorAndToast("Failed to get referral data: " + e.getMessage());
					}
				});
			}
		});
		registerCustomInvitesChannelPlugins();
	}

	@Override
	public boolean handleNotification(final Notification notification, NotificationContext context) {
		if (context.getAction() == null) {
			if (PickActionView.ADD_FRIEND.equals(notification.getAction().getType())) {
				showAddFriendDialog(notification);
				return true;
			}
			if (!context.wasClicked()) {
				Toast.makeText(MainActivity.this, notification.getText(), Toast.LENGTH_SHORT).show();
				return true;
			}

			return handleAction(notification.getAction());
		} else {
			return handleCustomAction(notification, context.getAction());
		}
	}

	private void showAddFriendDialog(final Notification notification) {
		final AlertDialog.Builder builder = new AlertDialog.Builder(this)
				.setTitle(notification.getTitle())
				.setMessage(notification.getText());
		for (final ActionButton actionButton : notification.getActionButtons()) {
			final String actionId = actionButton.getId();
			final DialogInterface.OnClickListener onClickListener = new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					dialog.dismiss();
					handleCustomAction(notification, actionId);
				}
			};
			if (ActionButton.CONSUME_ACTION.equals(actionId)) {
				builder.setPositiveButton(actionButton.getTitle(), onClickListener);
			} else if (ActionButton.IGNORE_ACTION.equals(actionId)) {
				builder.setNegativeButton(actionButton.getTitle(), onClickListener);
			}
		}
		builder.setNeutralButton("Dismiss", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
			}
		}).create().show();
	}

	private boolean handleCustomAction(Notification notification, String actionId) {
		final Action action = notification.getAction();
		if (actionId.equals(ActionButton.CONSUME_ACTION)) {
			if (PickActionView.ADD_FRIEND.equals(action.getType())) {
				final String userId = action.getData().get(PickActionView.KEY_USER_ID);
				final String userName = action.getData().get(PickActionView.KEY_USER_NAME);
				addFriend(userId, userName);
			} else {
				GetSocial.processAction(action);
			}
			_dependenciesContainer.notificationsManager().setStatus(notification.getId(), NotificationStatus.CONSUMED);
		} else {
			_dependenciesContainer.notificationsManager().setStatus(notification.getId(), NotificationStatus.IGNORED);
		}
		return false;
	}

	private void addFriend(final String userId, final String userName) {
		GetSocial.User.addFriend(userId, new Callback<Integer>() {
			@Override
			public void onSuccess(Integer result) {
				notifyFriend(userId);
				Toast.makeText(MainActivity.this, userName + " is now your friend!", Toast.LENGTH_SHORT).show();
			}

			@Override
			public void onFailure(GetSocialException exception) {
				Toast.makeText(MainActivity.this, "Failed to add " + userName + ", error: " + exception.getMessage(), Toast.LENGTH_SHORT).show();
			}
		});
	}

	private void notifyFriend(String userId) {
		GetSocial.User.sendNotification(Collections.singletonList(userId), NotificationContent.notificationWithText(SendNotificationPlaceholders.CustomText.SENDER_DISPLAY_NAME + " accepted your friend request!"), new Callback<NotificationsSummary>() {
			@Override
			public void onSuccess(NotificationsSummary result) {
				//
			}

			@Override
			public void onFailure(GetSocialException exception) {
				//
			}
		});
	}

	protected void registerCustomInvitesChannelPlugins() {
		GetSocial.registerInviteChannelPlugin(InviteChannelIds.KAKAO, new KakaoInvitePlugin());
		GetSocial.registerInviteChannelPlugin(InviteChannelIds.FACEBOOK, new FacebookSharePlugin(this, _facebookCallbackManager));
		GetSocial.registerInviteChannelPlugin(InviteChannelIds.VK, _vkInvitePlugin);
	}

	private void showUserInfoDialog(String userId) {
		GetSocial.getUserById(userId, new Callback<PublicUser>() {
			@Override
			public void onSuccess(PublicUser publicUser) {
				UserInfoDialog.show(getSupportFragmentManager(), publicUser);
			}

			@Override
			public void onFailure(GetSocialException e) {
				_log.logErrorAndToast("Failed to get user: " + e.getMessage());
			}
		});
	}

	protected String getDemoAppInfo() {
		return String.format(Locale.getDefault(), "GetSocial Android Demo\nSDK v%s. Build v%d", GetSocial.getSdkVersion(), BuildConfig.VERSION_CODE);
	}

	private void showUserDetails() {
		if (GetSocial.isInitialized()) {
			CurrentUserInfoDialog.show(getSupportFragmentManager());
		}
	}

	private void showReferralData(String referralData) {
		ReferralDataDialog.showReferralData(getSupportFragmentManager(), referralData);
	}

	private void copyUserIdToClipboard() {
		final String userId = GetSocial.User.getId();

		ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
		ClipData clip = ClipData.newPlainText("GetSocial User ID", userId);
		clipboard.setPrimaryClip(clip);
		vibrate();
		Toast.makeText(this, "Copied " + userId + " to clipboard.", Toast.LENGTH_LONG).show();
	}

	private void vibrate() {
		Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

		if (vibrator == null) {
			return;
		}
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			vibrator.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE));
		} else {
			vibrator.vibrate(500);
		}
	}

	private RootFragment findRootFragment() {
		return findFragment("root");
	}

	private <T> T findFragment(String tag) {
		return (T) getSupportFragmentManager().findFragmentByTag(tag);
	}

	//region ActivityListener

	@Override
	public void invalidateUi() {
		_viewContainer.updateView();
	}

	@Override
	public void addContentFragment(Fragment fragment) {
		String tag = "";
		if (fragment instanceof HasFragmentTag) {
			tag = ((HasFragmentTag) fragment).getFragmentTag();
		}
		getSupportFragmentManager().beginTransaction()
				.replace(R.id.content, fragment, tag)
				.addToBackStack(tag)
				.commitAllowingStateLoss();
	}

	@Override
	public void putSessionValue(String key, @Nullable String value) {
		_demoAppSessionData.put(key, value);
	}

	@Override
	@Nullable
	public String getSessionValue(String key) {
		return _demoAppSessionData.get(key);
	}

	@Override
	public DependenciesContainer dependencies() {
		return _dependenciesContainer;
	}
	//endregion

	//region GetSocial listeners

	@Override
	public void onUserChanged() {
		_viewContainer._userInfoView.updateView();
		GetSocial.User.getFriendsCount(new Callback<Integer>() {
			@Override
			public void onSuccess(Integer friendsCount) {
				putSessionValue(FriendsFragment.KEY_FRIENDS_COUNT, String.valueOf(friendsCount));
				RootFragment rootFragment = findRootFragment();
				if (rootFragment != null) {
					rootFragment.invalidateList();
				}
			}

			@Override
			public void onFailure(GetSocialException exception) {
				Console.logError(exception.getLocalizedMessage());
			}
		});
		dependencies().notificationsManager().sync();
	}

	@Override
	public boolean handleAction(Action action) {
		if (action.getType().equals(ActionTypes.OPEN_PROFILE)) {
			final String userId = action.getData().get(ActionDataKeys.OpenProfile.USER_ID);
			showUserInfoDialog(userId);
			return true;
		} else if (action.getType().equals(ActionTypes.CUSTOM)) {
			_log.logInfo("Received custom action:" + action.getData());
			return true;
		}
		return false;
	}

	//endregion

	class ViewContainer {

		@BindView(R.id.toolbar)
		Toolbar _toolbar;
		@BindView(R.id.textViewVersion)
		TextView _versionTextView;
		@BindView(R.id.userInfoView)
		UserInfoView _userInfoView;

		private Context _context;

		public ViewContainer(Activity activity) {
			_context = activity;
			ButterKnife.bind(this, activity);

			setSupportActionBar(_toolbar);

			getSupportFragmentManager().addOnBackStackChangedListener(
					new FragmentManager.OnBackStackChangedListener() {
						public void onBackStackChanged() {
							updateView();
						}
					});
		}

		@OnClick(R.id.userInfoView)
		void onUserInfoClicked() {
			showUserDetails();
		}

		@OnLongClick(R.id.userInfoView)
		boolean onUserInfoLongClicked() {
			copyUserIdToClipboard();
			return true;
		}

		@SuppressLint("ResourceAsColor")
		private void updateView() {
			_versionTextView.setText(getDemoAppInfo());

			if (getSupportFragmentManager().getBackStackEntryCount() == 1) {
				_toolbar.setNavigationIcon(null);
				_userInfoView.setVisibility(View.VISIBLE);
				_userInfoView.updateView();
			} else {
				_userInfoView.setVisibility(View.GONE);

				Fragment fragment = getActiveFragment();
				if (fragment != null && fragment instanceof HasTitle) {
					HasTitle hasTitle = (HasTitle) fragment;
					_toolbar.setTitle(hasTitle.getTitle());
				}
				
				Drawable navigationIcon = CompatibilityUtils.getDrawable(_context, R.drawable.ic_menu_back);
				Drawable wrappedNavigationIcon = DrawableCompat.wrap(navigationIcon);
				DrawableCompat.setTint(wrappedNavigationIcon, getResources().getColor(R.color.primary_text));
				_toolbar.setNavigationIcon(navigationIcon);

				_toolbar.setNavigationOnClickListener(
						new View.OnClickListener() {
							@Override
							public void onClick(View view) {
								onBackPressed();
							}
						}
				);
			}
		}
	}
}
