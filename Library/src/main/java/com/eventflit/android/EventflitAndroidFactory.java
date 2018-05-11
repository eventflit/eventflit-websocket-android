package com.eventflit.android;

import android.content.Context;
import android.os.Looper;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.SyncHttpClient;
import com.eventflit.android.notifications.PlatformType;
import com.eventflit.android.notifications.interests.InterestSubscriptionChange;
import com.eventflit.android.notifications.interests.InterestSubscriptionChangeListener;
import com.eventflit.android.notifications.interests.Subscription;
import com.eventflit.android.notifications.interests.SubscriptionChangeHandler;
import com.eventflit.android.notifications.interests.SubscriptionManager;
import com.eventflit.android.notifications.tokens.InvalidClientIdHandler;
import com.eventflit.android.notifications.tokens.RegistrationListenerStack;
import com.eventflit.android.notifications.tokens.TokenRegistry;
import com.eventflit.android.notifications.tokens.TokenUpdateHandler;
import com.eventflit.android.notifications.tokens.TokenUploadHandler;

import cz.msebera.android.httpclient.entity.StringEntity;

/**
 * Created by jamiepatel on 12/07/2016.
 */
public class EventflitAndroidFactory {
    public SubscriptionChangeHandler newSubscriptionChangeHandler(Subscription subscription) {
        return new SubscriptionChangeHandler(subscription);
    }

    public AsyncHttpClient newHttpClient() {
        AsyncHttpClient client;
        if (Looper.myLooper() == Looper.getMainLooper()) {
            client = new AsyncHttpClient();
        } else {
            client = new SyncHttpClient();
        }
        
        client.addHeader("X-Eventflit-Library", "eventflit-websocket-android " + BuildConfig.VERSION_NAME);
        return client;
    }

    public TokenUploadHandler newTokenUploadHandler(Context context, RegistrationListenerStack listenerStack) {
        return new TokenUploadHandler(context, listenerStack);
    }

    public TokenUpdateHandler newTokenUpdateHandler(String cachedId, StringEntity retryParams, Context context, RegistrationListenerStack listenerStack, InvalidClientIdHandler invalidClientIdHandler) {
        return new TokenUpdateHandler(cachedId, retryParams, context, listenerStack, invalidClientIdHandler);
    }

    public SubscriptionManager newSubscriptionManager(
            String clientId,
            Context context,
            String appKey,
            EventflitAndroidOptions options
            ) {
        return new SubscriptionManager(clientId, context, appKey, options, this);
    }

    public TokenRegistry newTokenRegistry(
            String appKey, RegistrationListenerStack listenerStack,
            Context context, PlatformType platformType,
            EventflitAndroidOptions options) {
        return new TokenRegistry(appKey, listenerStack, context, platformType, options, this);
    }
}
