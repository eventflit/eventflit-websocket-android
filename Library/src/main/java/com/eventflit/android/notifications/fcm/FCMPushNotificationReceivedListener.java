package com.eventflit.android.notifications.fcm;

import com.google.firebase.messaging.RemoteMessage;

/**
 * Created by jamiepatel on 10/08/2016.
 */

public interface FCMPushNotificationReceivedListener {
    void onMessageReceived(RemoteMessage remoteMessage);
}
