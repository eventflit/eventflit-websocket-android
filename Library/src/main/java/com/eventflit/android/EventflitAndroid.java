package com.eventflit.android;

import com.google.firebase.iid.FirebaseInstanceIdService;
import com.eventflit.android.notifications.ManifestValidator;
import com.eventflit.android.notifications.PushNotificationRegistration;
import com.eventflit.client.Client;
import com.eventflit.client.Eventflit;
import com.eventflit.client.channel.Channel;
import com.eventflit.client.channel.ChannelEventListener;
import com.eventflit.client.channel.PresenceChannel;
import com.eventflit.client.channel.PresenceChannelEventListener;
import com.eventflit.client.channel.PrivateChannel;
import com.eventflit.client.channel.PrivateChannelEventListener;
import com.eventflit.client.connection.Connection;
import com.eventflit.client.connection.ConnectionEventListener;
import com.eventflit.client.connection.ConnectionState;

/**
 * Created by jamiepatel on 09/06/2016.
 */
public class EventflitAndroid implements Client {

    private final Eventflit eventflit;
    private final PushNotificationRegistration pushNotificationRegistration;

    public EventflitAndroid(final String appKey) {
        this(appKey, new EventflitAndroidOptions(), new EventflitAndroidFactory());
    }

    public EventflitAndroid(final String appKey, final EventflitAndroidOptions eventflitOptions) {
        this(appKey, eventflitOptions, new EventflitAndroidFactory());
    }

    private EventflitAndroid(final String appKey,
                          final EventflitAndroidOptions eventflitOptions, final EventflitAndroidFactory factory) {
        this.eventflit = new Eventflit(appKey, eventflitOptions);
        this.pushNotificationRegistration =
                new PushNotificationRegistration(appKey, eventflitOptions, factory, new ManifestValidator());

    }

    @Override
    public Connection getConnection() {
        return this.eventflit.getConnection();
    }

    @Override
    public void connect() {
        this.eventflit.connect();
    }

    @Override
    public void connect(ConnectionEventListener eventListener, ConnectionState... connectionStates) {
        this.eventflit.connect(eventListener, connectionStates);
    }

    @Override
    public void disconnect() {
        this.eventflit.disconnect();
    }

    @Override
    public Channel subscribe(String channelName) {
        return this.eventflit.subscribe(channelName);
    }

    @Override
    public Channel subscribe(String channelName, ChannelEventListener listener, String... eventNames) {
        return this.eventflit.subscribe(channelName, listener, eventNames);
    }

    @Override
    public PrivateChannel subscribePrivate(String channelName) {
        return this.eventflit.subscribePrivate(channelName);
    }

    @Override
    public PrivateChannel subscribePrivate(String channelName, PrivateChannelEventListener listener, String... eventNames) {
        return this.eventflit.subscribePrivate(channelName, listener, eventNames);
    }

    @Override
    public PresenceChannel subscribePresence(String channelName) {
        return this.eventflit.subscribePresence(channelName);
    }

    @Override
    public PresenceChannel subscribePresence(String channelName, PresenceChannelEventListener listener, String... eventNames) {
        return this.eventflit.subscribePresence(channelName, listener, eventNames);
    }

    @Override
    public void unsubscribe(String channelName) {
        this.eventflit.unsubscribe(channelName);
    }

    @Override
    public Channel getChannel(String channelName) {
        return this.eventflit.getChannel(channelName);
    }

    @Override
    public PrivateChannel getPrivateChannel(String channelName) {
        return this.eventflit.getPrivateChannel(channelName);
    }

    @Override
    public PresenceChannel getPresenceChannel(String channelName) {
        return this.eventflit.getPresenceChannel(channelName);
    }

    public PushNotificationRegistration nativeEventflit() {
        return this.pushNotificationRegistration;
    }

}
