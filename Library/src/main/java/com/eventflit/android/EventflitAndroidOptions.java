package com.eventflit.android;

import android.util.Log;

import com.eventflit.client.EventflitOptions;

/**
 * Created by jamiepatel on 15/07/2016.
 */

public class EventflitAndroidOptions extends EventflitOptions {
    private static final String NOTIFICATION_API_PREFIX = "client_api";
    private static final String NOTIFICATION_API_VERSION = "v1";

    private String notificationHost = "nativepushclient-cluster1.eventflit.com";
    private boolean notificationEncrypted = true;

    public String buildNotificationURL(String path) {
        String scheme = notificationEncrypted ? "https://" : "http://";
        return scheme + notificationHost + "/" + NOTIFICATION_API_PREFIX + "/" + NOTIFICATION_API_VERSION + path;
    }

    public String getNotificationHost() {
        return notificationHost;
    }

    public void setNotificationHost(String notificationHost) {
        this.notificationHost = notificationHost;
    }

    public boolean isNotificationEncrypted() {
        return notificationEncrypted;
    }

    public void setNotificationEncrypted(boolean notificationEncrypted) {
        this.notificationEncrypted = notificationEncrypted;
    }
}
