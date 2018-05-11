package eventflit.example;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.firebase.messaging.RemoteMessage;
import com.eventflit.android.EventflitAndroid;
import com.eventflit.android.EventflitAndroidOptions;
import com.eventflit.android.notifications.ManifestValidator;
import com.eventflit.android.notifications.PushNotificationRegistration;
import com.eventflit.android.notifications.fcm.FCMPushNotificationReceivedListener;
import com.eventflit.android.notifications.gcm.GCMPushNotificationReceivedListener;
import com.eventflit.android.notifications.tokens.PushNotificationRegistrationListener;
import com.eventflit.android.notifications.interests.InterestSubscriptionChangeListener;
import com.eventflit.client.connection.ConnectionEventListener;
import com.eventflit.client.connection.ConnectionState;
import com.eventflit.client.connection.ConnectionStateChange;

import java.util.List;

public class MainActivity extends Activity {

    private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;
    private static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        EventflitAndroidOptions options = new EventflitAndroidOptions();

        EventflitAndroid eventflit = new EventflitAndroid("YOUR_APP_KEY", options);


        if (checkPlayServices()) {
            String defaultSenderId = getString(R.string.gcm_defaultSenderId);
            PushNotificationRegistration nativeEventflit = eventflit.nativeEventflit();
            nativeEventflit.setFCMListener(new FCMPushNotificationReceivedListener() {
                @Override
                public void onMessageReceived(RemoteMessage remoteMessage) {

                }
            });


            nativeEventflit.subscribe("donuts", new InterestSubscriptionChangeListener() {
                @Override
                public void onSubscriptionChangeSucceeded() {
                    System.out.println("DONUT SUCCEEDED W000HOOO!!!");
                }

                @Override
                public void onSubscriptionChangeFailed(int statusCode, String response) {
                    System.out.println("What a disgrace: received " + statusCode + " with" + response);
                }
            });


            PushNotificationRegistrationListener regListener = new PushNotificationRegistrationListener() {
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
            };

            //FCM example:
            //Use this if using Eventflit's FCMMessagingService - this is not used if you have your custom service to receive notifications
            nativeEventflit.setFCMListener(new FCMPushNotificationReceivedListener() {
                @Override
                public void onMessageReceived(RemoteMessage remoteMessage) {
                    String message = remoteMessage.getNotification().getBody();
                    Log.d(TAG, "EVENTFLIT!!!");
                    Log.d(TAG, "From: " + remoteMessage.getFrom());
                    Log.d(TAG, "Message: " + message);                }
            });

            try {
                nativeEventflit.registerFCM(getApplicationContext(), regListener);
            } catch (ManifestValidator.InvalidManifestException e) {
                e.printStackTrace();
            }


            //GCM example:
            nativeEventflit.setGCMListener(new GCMPushNotificationReceivedListener() {
                @Override
                public void onMessageReceived(String from, Bundle data) {
                    String message = data.getString("message");
                    Log.d(TAG, "EVENTFLIT!!!");
                    Log.d(TAG, "From: " + from);
                    Log.d(TAG, "Message: " + message);
                }
            });
            
            try {
                nativeEventflit.registerGCM(this, defaultSenderId, regListener);

            } catch (ManifestValidator.InvalidManifestException e) {
                e.printStackTrace();
            }
        }
    }

    private boolean checkPlayServices() {
    /**
     * Check the device to make sure it has the Google Play Services APK. If
     * it doesn't, display a dialog that allows users to download the APK from
     * the Google Play Store or enable it in the device's system settings.
     */
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
}
