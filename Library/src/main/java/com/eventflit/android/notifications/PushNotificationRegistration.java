package com.eventflit.android.notifications;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.firebase.iid.FirebaseInstanceId;
import com.eventflit.android.EventflitAndroidFactory;
import com.eventflit.android.EventflitAndroidOptions;
import com.eventflit.android.notifications.fcm.FCMInstanceIDService;
import com.eventflit.android.notifications.fcm.FCMMessagingService;
import com.eventflit.android.notifications.fcm.FCMPushNotificationReceivedListener;
import com.eventflit.android.notifications.gcm.GCMPushNotificationReceivedListener;
import com.eventflit.android.notifications.gcm.EventflitGCMListenerService;
import com.eventflit.android.notifications.gcm.GCMRegistrationIntentService;
import com.eventflit.android.notifications.interests.InterestSubscriptionChange;
import com.eventflit.android.notifications.interests.InterestSubscriptionChangeListener;
import com.eventflit.android.notifications.interests.Subscription;
import com.eventflit.android.notifications.interests.SubscriptionManager;
import com.eventflit.android.notifications.tokens.InternalRegistrationProgressListener;
import com.eventflit.android.notifications.tokens.PushNotificationRegistrationListener;
import com.eventflit.android.notifications.tokens.RegistrationListenerStack;
import com.eventflit.android.notifications.tokens.TokenRegistry;

import org.json.JSONException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;


public class PushNotificationRegistration implements InternalRegistrationProgressListener {
    public static String GCM_CALLED_INTENT_FILTER = "__eventflit__gcm_called__received";
    public static String TOKEN_EXTRA_KEY = "token";

    private static final String TAG = "EventflitPushNotifReg";

    private final String appKey;
    private final EventflitAndroidOptions options;
    private final EventflitAndroidFactory factory;
    private final ManifestValidator manifestValidator;
    private SubscriptionManager subscriptionManager; // should only exist on successful registration with Eventflit
    private List<Subscription> pendingSubscriptions =
            Collections.synchronizedList(new ArrayList<Subscription>());

    public PushNotificationRegistration(
            String appKey,
            EventflitAndroidOptions options,
            EventflitAndroidFactory factory,
            ManifestValidator manifestValidator
    ) {
        this.appKey = appKey;
        this.options = options;
        this.factory = factory;
        this.manifestValidator = manifestValidator;
    }

    public void registerGCM(Context context, String defaultSenderId) throws ManifestValidator.InvalidManifestException {
        registerGCM(context, defaultSenderId, null);
    }

    /*
    Starts a EventflitRegistrationIntentService, which handles token receipts and updates from
    GCM
     */
    public void registerGCM(
            Context context,
            String defaultSenderId,
            final PushNotificationRegistrationListener registrationListener) throws ManifestValidator.InvalidManifestException {
        Log.d(TAG, "Registering for GCM notifications");

        manifestValidator.validateGCM(context);
        final Context applicationContext = context.getApplicationContext();

        BroadcastReceiver mRegistrationBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String token = intent.getStringExtra(TOKEN_EXTRA_KEY);
                if (token != null) {
                    final TokenRegistry tokenRegistry = newTokenRegistry(registrationListener, context, PlatformType.GCM);
                    try {
                        tokenRegistry.receive(token);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                } else {
                    if (registrationListener != null) {
                        registrationListener.onFailedRegistration(0, "Failed to get registration ID from GCM");
                    }
                }
            }
        };

        LocalBroadcastManager.
                getInstance(applicationContext).
                registerReceiver(mRegistrationBroadcastReceiver, new IntentFilter(GCM_CALLED_INTENT_FILTER));

        Intent intent = new Intent(applicationContext, GCMRegistrationIntentService.class);
        intent.putExtra("gcm_defaultSenderId", defaultSenderId);
        Log.d(TAG, "Starting registration intent service");
        applicationContext.startService(intent);

    }

    public void registerFCM(Context context, final PushNotificationRegistrationListener listener) throws ManifestValidator.InvalidManifestException {
        manifestValidator.validateFCM(context);

        TokenRegistry tokenRegistry = newTokenRegistry(listener, context, PlatformType.FCM);
        FCMInstanceIDService.setTokenRegistry(tokenRegistry);
        String token = FirebaseInstanceId.getInstance().getToken();

        if (token != null) {
            try {
                tokenRegistry.receive(token);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    public void registerFCM(Context context) throws ManifestValidator.InvalidManifestException {
        registerFCM(context, null);
    }

    private TokenRegistry newTokenRegistry(PushNotificationRegistrationListener customerListener, Context context, PlatformType platformType) {
        RegistrationListenerStack listenerStack = new RegistrationListenerStack();
        if (customerListener != null) {
            listenerStack.push(customerListener);
        }
        listenerStack.push(this);
        return factory.newTokenRegistry(appKey, listenerStack, context, platformType, options);
    }

    // Subscribes to an interest
    public void subscribe(String interest) {
        subscribe(interest, null);
    }

    public void subscribe(final String interest, final InterestSubscriptionChangeListener listener) {
        trySendSubscriptionChange(interest, InterestSubscriptionChange.SUBSCRIBE, listener);
    }

    // Subscribes to an interest
    public void unsubscribe(String interest) {
        unsubscribe(interest, null);
    }

    // Unsubscribes to an interest
    public void unsubscribe(final String interest, final InterestSubscriptionChangeListener listener) {
        trySendSubscriptionChange(interest, InterestSubscriptionChange.UNSUBSCRIBE, listener);
    }

    private void trySendSubscriptionChange(
            final String interest,
            final InterestSubscriptionChange change,
            final InterestSubscriptionChangeListener listener) {
        Log.d(TAG, "Trying to "+change+" to: " + interest);
        Subscription subscription = new Subscription(interest, change, listener);
        if (subscriptionManager != null) {
            subscriptionManager.sendSubscription(subscription);
        } else {
            pendingSubscriptions.add(subscription);
        }
    }

    /**
     * Sets the listener to execute when a notification is received
     * */
    public void setGCMListener(GCMPushNotificationReceivedListener listener) {
        EventflitGCMListenerService.setOnMessageReceivedListener(listener);
    }

    /**
     * Sets the FCM listener if you use Eventflit's FCMMessagingService in your manifest.
     * If you intend to use a different service to receive FCM notifications then this call does nothing and you need to handle the listener yourself.
     *
     * @param listener the listener to set.
     * */
    public void setFCMListener(FCMPushNotificationReceivedListener listener) {
        FCMMessagingService.setOnMessageReceivedListener(listener);
    }

    @Override
    public void onSuccessfulRegistration(String clientId, Context context) {
        subscriptionManager = factory.newSubscriptionManager(clientId, context, appKey, options);
        subscriptionManager.sendSubscriptions(pendingSubscriptions);
        pendingSubscriptions = Collections.synchronizedList(new ArrayList<Subscription>());
    }

    @Override
    public void onFailedRegistration(int statusCode, String reason) {
        for (Iterator<Subscription> iterator = pendingSubscriptions.iterator(); iterator.hasNext();){
            Subscription subscription = iterator.next();

            if(subscription.getListener() != null) {
                subscription.getListener().onSubscriptionChangeFailed(statusCode, reason);
            }

            iterator.remove();
        }
    }

}