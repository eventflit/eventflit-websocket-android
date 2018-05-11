# Eventflit Android Library

eventflit-websocket-android is a wrapper library around [eventflit-websocket-java](https://github.com/eventflit/eventflit-websocket-java). Whereas the underlying library is a purely Java library, this library has interaction with Android APIs. As a result, we can provide a better experience for mobile developers. We can also use Eventflit's new BETA feature: native push notifications.

This README will only cover library-specific features. In order to get the core documentation, please visit the README of [eventflit-websocket-java](https://github.com/eventflit/eventflit-websocket-java).

**Please note that this library is still in beta and may not be ready in a production environment. As this library is still pre-1.0, expect breaking changes. Feel free to raise an issue about any bugs you find.**

## Installation

You can install the library via Gradle. First add these dependencies to your `$PROJECT_ROOT/app/build.gradle`:

```groovy
dependencies {
  // for GCM
  compile 'com.google.android.gms:play-services-gcm:9.8.0' // This version if often updated by Google Play Services. 

  // for FCM
  compile 'com.google.firebase:firebase-messaging:9.8.0'
  compile 'com.google.firebase:firebase-core:9.8.0'

  compile 'com.eventflit:eventflit-websocket-android:0.1.0'
}

// for GCM and FCM
apply plugin: 'com.google.gms.google-services'
```

In your project-level `build.gradle` add:

```groovy
buildscript {
  dependencies {
    classpath 'com.google.gms:google-services:3.0.0'
  }
}
```

## Push Notifications

### GCM

This feature requires some set up on your behalf. See [our guide to setting up push notifications for Android](https://docs.eventflit.com/push_notifications/android) for a friendly introduction.

* That you have an app (a.k.a. project) on the Google Developers Console
* That you have a Server API Key which you have uploaded to the Eventflit dashboard
* That you have a [valid configuration file](https://developers.google.com/cloud-messaging/android/client#get-config) for Google services in your `app/` directory

Add to your `AndroidManifest.xml` the following:

```xml
<manifest>
  <!-- ... -->

  <!-- GCM permissions -->
  <uses-permission android:name="com.google.android.c2dm.permission.RECEIVE" />
  <uses-permission android:name="android.permission.WAKE_LOCK" />

  <application>
    <!-- ... -->

    <!-- <application> -->
    <!-- Eventflit's GCM listeners and services -->
    <receiver
        android:name="com.google.android.gms.gcm.GcmReceiver"
        android:exported="true"
        android:permission="com.google.android.c2dm.permission.SEND" >
        <intent-filter>
            <action android:name="com.google.android.c2dm.intent.RECEIVE" />
            <category android:name="gcm.play.android.samples.com.gcmquickstart" />
        </intent-filter>
    </receiver>

    <service
        android:name="com.eventflit.android.notifications.gcm.EventflitGCMListenerService"
        android:exported="false" >
        <intent-filter>
            <action android:name="com.google.android.c2dm.intent.RECEIVE" />
        </intent-filter>
    </service>

    <service
        android:name="com.eventflit.android.notifications.gcm.GCMInstanceIDListenerService"
        android:exported="false">
        <intent-filter>
            <action android:name="com.google.android.gms.iid.InstanceID"/>
        </intent-filter>
    </service>

    <service
        android:name="com.eventflit.android.notifications.gcm.GCMRegistrationIntentService"
        android:exported="false">
    </service>

    <!-- ... -->
  </application>

  <!-- ... -->
</manifest>
```

Eventflit's GCM listeners and services above allow the library to handle incoming tokens and keep state synced with our servers.

### FCM

To start with, you will need to [add Firebase to your project](https://firebase.google.com/docs/android/setup). Then, in your application manifest, you need to register these services:

```xml
<application>
  <service
      android:name="com.eventflit.android.notifications.fcm.FCMMessagingService">
      <intent-filter>
          <action android:name="com.google.firebase.MESSAGING_EVENT"/>
      </intent-filter>
  </service>

  <service
      android:name="com.eventflit.android.notifications.fcm.FCMInstanceIDService">
      <intent-filter>
          <action android:name="com.google.firebase.INSTANCE_ID_EVENT"/>
      </intent-filter>
  </service>
</application>
```

### Registering Your Device With Eventflit

You can start registering for push notifications in an `Activity` or any other valid `Context`. You will need to check Google Play Services availability on the device, with a function such as:

```java
public class MainActivity extends AppCompatActivity {
    private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;
    private static final String TAG = "yourtag";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
      if (playServicesAvailable()) {
        // ... set up Eventflit push notifications
      } else {
        // ... log error, or handle gracefully
      }
    }

    private boolean playServicesAvailable() {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        int resultCode = apiAvailability.isGooglePlayServicesAvailable(this);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (apiAvailability.isUserResolvableError(resultCode)) {
                apiAvailability.getErrorDialog(this, resultCode, PLAY_SERVICES_RESOLUTION_REQUEST)
                        .show();
            } else {
                Log.i(TAG, "This device is not supported.");
                finish();
            }
            return false;
        }
        return true;
    }

    // ...
}
```


Assuming that Google Play services are available, you can then register for push notifications.

#### GCM

Expand your `onCreate` handler to instantiate a `EventflitAndroid`, get the native push notification object from it, and register using the sender ID fetched from your `google-services.json` file:

```java
public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
      if (playServicesAvailable()) {
        EventflitAndroidOptions options = new EventflitAndroidOptions().setCluster(<eventflit_app_cluster>);
        EventflitAndroid eventflit = new EventflitAndroid(<eventflit_api_key>);
        PushNotificationRegistration nativeEventflit = eventflit.nativeEventflit();
        String defaultSenderId = getString(R.string.gcm_defaultSenderId); // fetched from your google-services.json
        nativeEventflit.registerGCM(this, defaultSenderId);
      } else {
        // ... log error, or handle gracefully
      }
    }
    // ...
}
```

Having called `register` this will start an `IntentService` under the hood that uploads the device token to Eventflit.

#### FCM

For Firebase Cloud Messaging, instead of `registerGCM` we call `registerFCM`, passing in the context:

```java
nativeEventflit.registerFCM(this);
```

#### Listening For Registration Progress

To get progress updates on your registration to GCM or FCM, you can optionally pass a `PushNotificationRegistrationListener`:

```java
PushNotificationRegistrationListener listener = new PushNotificationRegistrationListener() {
    @Override
    public void onSuccessfulRegistration() {
        System.out.println("REGISTRATION SUCCESSFUL!!! YEEEEEHAWWWWW!");

    }

    @Override
    public void onFailedRegistration(int statusCode, String response) {
        System.out.println(
                "A real sad day. Registration failed with code " + statusCode +
                        " " + response
        );
    }
}

// GCM
nativeEventflit.registerGCM(this, defaultSenderId, listener);

// FCM
nativeEventflit.registerFCM(this, listener);
```

### Receiving Notifications

Eventflit has a concept of `interests` which clients can subscribe to. Whenever your server application sends a notification to an interest, subscribed clients will receive those notifications.

Subscribing to an interest is simply a matter of calling:

```java
PushNotificationRegistration nativeEventflit = eventflit.nativeEventflit();
nativeEventflit.subscribe("kittens"); // the client is interested in kittens
```

To unsubscribe to an interest:

```java
nativeEventflit.unsubscribe("kittens"); // we are no longer interested in kittens
```

You can also keep track of the state of your subscriptions or un-subscriptions by passing an optional `InterestSubscriptionChangeListener`:


```java
nativeEventflit.subscribe("kittens", new InterestSubscriptionChangeListener() {
    @Override
    public void onSubscriptionChangeSucceeded() {
        System.out.println("Success! I love kittens!");
    }

    @Override
    public void onSubscriptionChangeFailed(int statusCode, String response) {
        System.out.println(":(: received " + statusCode + " with" + response);
    }
});
```

If you wish to set a custom callback for when GCM notifications come in:

```java
nativeEventflit.setGCMListener(new GCMPushNotificationReceivedListener() {
    @Override
    public void onMessageReceived(String from, Bundle data) {
      // do something magical 🔮
    }
});
```

For FCM:

```java
nativeEventflit.setFCMListener(new FCMPushNotificationReceivedListener() {
    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
      // do something magical 🔮
    }
});
```

### Configuring the notifications client

#### Setting the host of Eventflit's notifications server

```java
EventflitAndroidOptions options = new EventflitAndroidOptions();
options.setCluster(<eventflit_app_cluster>);
options.setNotificationHost("yolo.io");

EventflitAndroid eventflit = new EventflitAndroid("key", options);
```

#### Using SSL

The client uses SSL by default. To unset it:

```java
EventflitAndroidOptions options = new EventflitAndroidOptions();
options.setCluster(<eventflit_app_cluster>);
options.setNotificationEncrypted(false);
EventflitAndroid eventflit = new EventflitAndroid("key", options);
```
