package com.eventflit.android.notifications.tokens;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.eventflit.android.EventflitAndroidFactory;
import com.eventflit.android.EventflitAndroidOptions;
import com.eventflit.android.notifications.PlatformType;

import org.json.JSONException;
import org.json.JSONObject;

import cz.msebera.android.httpclient.entity.StringEntity;

/**
 * Created by jamiepatel on 12/08/2016.
 */

public class TokenRegistry implements InvalidClientIdHandler, InternalRegistrationProgressListener {
    static final String EVENTFLIT_PUSH_CLIENT_ID_PREFIX = "__eventflit__client__key__";
    private static final String TAG = "TokenRegistry";

    private final String appKey;
    private final Context context;
    private RegistrationListenerStack listenerStack;
    private final PlatformType platformType;
    private final EventflitAndroidOptions options;
    private final EventflitAndroidFactory factory;

    public TokenRegistry(String appKey, RegistrationListenerStack listenerStack, Context context, PlatformType platformType, EventflitAndroidOptions options, EventflitAndroidFactory factory) {
        this.appKey = appKey;
        this.context = context;
        this.platformType = platformType;
        this.options = options;
        this.factory = factory;

        listenerStack.push(this); // add our listener to the stack of registration listeners
        this.listenerStack = listenerStack;
    }

    public void receive(String token) throws JSONException {
        Log.d(TAG, "Received token for " + platformType.toString() + ": " + token);
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        String cachedId = preferences.getString(sharedPreferencesKey(appKey), null);
        if (cachedId == null) {
            upload(token);
        } else {
            update(token, cachedId);
        }
    }

    private void upload(String token) throws JSONException {
        StringEntity params = createRegistrationJSON(token);
        upload(params);
    }

    private void upload(StringEntity params) {
        String url = options.buildNotificationURL(
            "/device/app/" + appKey + "/fcm"
        );
        AsyncHttpClient client = factory.newHttpClient();
        JsonHttpResponseHandler handler = factory.newTokenUploadHandler(context, listenerStack);
        client.post(context, url, params, "application/json", handler);
    }

    private void update(String token, String cachedId) throws JSONException {
        String url = options.buildNotificationURL(
            "/device/app/" + appKey + "/fcm/" + cachedId + "/token"
        );
        AsyncHttpClient client = factory.newHttpClient();
        StringEntity params = createRegistrationJSON(token);

        AsyncHttpResponseHandler handler = factory.newTokenUpdateHandler(
                cachedId, params, context, listenerStack, this
        );
        client.put(context, url, params, "application/json", handler);
    }

    private StringEntity createRegistrationJSON(String token) throws JSONException {
        JSONObject params = new JSONObject();
        params.put("platformType", platformType.toString());
        params.put("token", token);
        params.put("appKey", appKey);
        return new StringEntity(params.toString(), "UTF-8");
    }

    private String sharedPreferencesKey(String appKey) {
        return EVENTFLIT_PUSH_CLIENT_ID_PREFIX + platformType.toString() + "__" + appKey;
    }

    @Override
    public void onInvalidClientId(StringEntity params) {
        upload(params);
    }

    @Override
    public void onSuccessfulRegistration(String clientId, Context context) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        preferences.edit().putString(sharedPreferencesKey(appKey), clientId).apply();
    }

    @Override
    public void onFailedRegistration(int statusCode, String reason) {}

}
