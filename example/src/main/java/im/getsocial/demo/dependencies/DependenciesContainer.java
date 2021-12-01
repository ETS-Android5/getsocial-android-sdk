package im.getsocial.demo.dependencies;

import im.getsocial.demo.dependencies.components.NotificationsManager;
import im.getsocial.sdk.actions.ActionListener;
import im.getsocial.sdk.notifications.OnNotificationClickedListener;

public interface DependenciesContainer {

	NotificationsManager notificationsManager();

	ActionListener actionListener();

	OnNotificationClickedListener notificationHandler();

}
