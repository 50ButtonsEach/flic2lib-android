package io.flic.flic2libandroid;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import javax.crypto.Mac;

/**
 * Flic 2 button class.
 *
 * <p>This class represents a paired Flic 2 button.</p>
 *
 * <p>This class cannot be instantiated directly. Instead get an object using
 * {@link Flic2Manager#startScan(Flic2ScanCallback)} or {@link Flic2Manager#getButtons()}</p>
 */
public class Flic2Button {
    /**
     * The button is in the disconnected state. No connection attempt is currently in progress.
     * Call {@link #connect()} if you would like to establish a connection as soon as the button
     * comes in range and advertises.
     */
    public static final int CONNECTION_STATE_DISCONNECTED = 0;

    /**
     * The manager is waiting for the button to come in range and advertise, at which time it should automatically connect.
     * Note that this state is also possible when Bluetooth is turned off, in which case it means
     * the manager will wait for Bluetooth to become turned on and then connect.
     */
    public static final int CONNECTION_STATE_CONNECTING = 1;

    /**
     * The button is connected, but the verification is not yet complete.
     */
    public static final int CONNECTION_STATE_CONNECTED_STARTING = 2;

    /**
     * The button is connected, the verification is done and button events can now arrive.
     */
    public static final int CONNECTION_STATE_CONNECTED_READY = 3;

    private static final int ADV_SETTINGS_CONFIGURED_NO = 0;
    private static final int ADV_SETTINGS_CONFIGURED_YES_NOT_PERSISTENTLY = 1;
    private static final int ADV_SETTINGS_CONFIGURED_YES_WITHOUT_ALWAYS_RECONNECT = 2;
    private static final int ADV_SETTINGS_CONFIGURED_YES_WITH_ALWAYS_RECONNECT = 3;

    private static void log(String s) {
        //Log.d("Flic2Button", s);
    }

    static class PairingData {
        public int identifier;
        public byte[] key;

        public PairingData(int identifier, byte[] key) {
            this.identifier = identifier;
            this.key = key;
        }
    }

    Flic2Manager manager;
    String bdAddr;
    Boolean addressType;
    String uuid;
    String serialNumber;
    volatile String name;
    long nameTimestampUtcMs;
    short autoDisconnectTime = 511;
    PairingData pairingData;
    int firmwareVersion;
    long nextFirmwareCheckTimestamp;
    boolean unpaired;
    int advSettingsConfigured;
    int bootId;
    int[] eventCount = new int[2];
    long readyTimestamp;
    Float lastKnownBatteryVoltage;
    Long lastKnownBatteryTimestampUtcMs;

    Flic2Manager.FlicGattCallback currentGattCb;
    boolean isConnected;
    boolean wantConnected;
    Runnable disconnectRunnable;
    Runnable retryConnectRunnable;

    private SafeIterableList<Flic2ButtonListener> listeners = new SafeIterableList<>();
    final Flic2ButtonListener listener = new Flic2ButtonListener() {
        @Override
        public void onConnect(Flic2Button button) {
            for (Flic2ButtonListener listener : listeners) {
                listener.onConnect(button);
            }
        }

        @Override
        public void onDisconnect(Flic2Button button) {
            for (Flic2ButtonListener listener : listeners) {
                listener.onDisconnect(button);
            }
        }

        @Override
        public void onReady(Flic2Button button, long timestamp) {
            for (Flic2ButtonListener listener : listeners) {
                listener.onReady(button, timestamp);
            }
        }

        @Override
        public void onUnpaired(Flic2Button button) {
            for (Flic2ButtonListener listener : listeners) {
                listener.onUnpaired(button);
            }
        }

        @Override
        public void onFailure(Flic2Button button, int errorCode, int subCode) {
            for (Flic2ButtonListener listener : listeners) {
                listener.onFailure(button, errorCode, subCode);
            }
        }

        @Override
        public void onButtonUpOrDown(Flic2Button button, boolean wasQueued, boolean lastQueued, long timestamp, boolean isUp, boolean isDown) {
            for (Flic2ButtonListener listener : listeners) {
                listener.onButtonUpOrDown(button, wasQueued, lastQueued, timestamp, isUp, isDown);
            }
        }

        @Override
        public void onButtonClickOrHold(Flic2Button button, boolean wasQueued, boolean lastQueued, long timestamp, boolean isClick, boolean isHold) {
            for (Flic2ButtonListener listener : listeners) {
                listener.onButtonClickOrHold(button, wasQueued, lastQueued, timestamp, isClick, isHold);
            }
        }

        @Override
        public void onButtonSingleOrDoubleClick(Flic2Button button, boolean wasQueued, boolean lastQueued, long timestamp, boolean isSingleClick, boolean isDoubleClick) {
            for (Flic2ButtonListener listener : listeners) {
                listener.onButtonSingleOrDoubleClick(button, wasQueued, lastQueued, timestamp, isSingleClick, isDoubleClick);
            }
        }

        @Override
        public void onButtonSingleOrDoubleClickOrHold(Flic2Button button, boolean wasQueued, boolean lastQueued, long timestamp, boolean isSingleClick, boolean isDoubleClick, boolean isHold) {
            for (Flic2ButtonListener listener : listeners) {
                listener.onButtonSingleOrDoubleClickOrHold(button, wasQueued, lastQueued, timestamp, isSingleClick, isDoubleClick, isHold);
            }
        }

        @Override
        public void onButtonEvent(Flic2Button button, Flic2ButtonEvent event) {
            for (Flic2ButtonListener listener : listeners) {
                if (event.getButtonNumber() == 0) {
                    switch (event.getEventClass()) {
                        case Flic2ButtonEvent.EVENT_CLASS_UP_OR_DOWN:
                            listener.onButtonUpOrDown(button, event.getWasQueued(), event.isLastQueued(), event.getTimestamp(),
                                    event.getEventType() == Flic2ButtonEvent.EVENT_TYPE_UP,
                                    event.getEventType() == Flic2ButtonEvent.EVENT_TYPE_DOWN);
                            break;
                        case Flic2ButtonEvent.EVENT_CLASS_CLICK_OR_HOLD:
                            listener.onButtonClickOrHold(button, event.getWasQueued(), event.isLastQueued(), event.getTimestamp(),
                                    event.getEventType() == Flic2ButtonEvent.EVENT_TYPE_CLICK,
                                    event.getEventType() == Flic2ButtonEvent.EVENT_TYPE_HOLD);
                            break;
                        case Flic2ButtonEvent.EVENT_CLASS_SINGLE_OR_DOUBLE_CLICK:
                            listener.onButtonSingleOrDoubleClick(button, event.getWasQueued(), event.isLastQueued(), event.getTimestamp(),
                                    event.getEventType() == Flic2ButtonEvent.EVENT_TYPE_SINGLE_CLICK,
                                    event.getEventType() == Flic2ButtonEvent.EVENT_TYPE_DOUBLE_CLICK);
                            break;
                        case Flic2ButtonEvent.EVENT_CLASS_SINGLE_OR_DOUBLE_CLICK_OR_HOLD:
                            listener.onButtonSingleOrDoubleClickOrHold(button, event.getWasQueued(), event.isLastQueued(), event.getTimestamp(),
                                    event.getEventType() == Flic2ButtonEvent.EVENT_TYPE_SINGLE_CLICK,
                                    event.getEventType() == Flic2ButtonEvent.EVENT_TYPE_DOUBLE_CLICK,
                                    event.getEventType() == Flic2ButtonEvent.EVENT_TYPE_HOLD);
                            break;
                    }
                }

                listener.onButtonEvent(button, event);
            }
        }

        @Override
        public void onNameUpdated(Flic2Button button, String newName) {
            for (Flic2ButtonListener listener : listeners) {
                listener.onNameUpdated(button, newName);
            }
        }

        @Override
        public void onFirmwareVersionCheckComplete(Flic2Button button, boolean checkSuccess, boolean hasNewVersion) {
            for (Flic2ButtonListener listener : listeners) {
                listener.onFirmwareVersionCheckComplete(button, checkSuccess, hasNewVersion);
            }
        }

        @Override
        public void onFirmwareVersionUpdated(Flic2Button button, int newVersion) {
            for (Flic2ButtonListener listener : listeners) {
                listener.onFirmwareVersionUpdated(button, newVersion);
            }
        }

        @Override
        public void onBatteryLevelUpdated(Flic2Button button, BatteryLevel level) {
            for (Flic2ButtonListener listener : listeners) {
                listener.onBatteryLevelUpdated(button, level);
            }
        }

        @Override
        public void onAllQueuedButtonEventsProcessed(Flic2Button button) {
            for (Flic2ButtonListener listener : listeners) {
                listener.onAllQueuedButtonEventsProcessed(button);
            }
        }

        @Override
        public void onDuoPushTwistNotification(FlicDuoPushTwistNotificationButtonInfo[] buttonInfo, int angleDiff) {
            for (Flic2ButtonListener listener : listeners) {
                listener.onDuoPushTwistNotification(buttonInfo, angleDiff);
            }
        }

        @Override
        public void onAccelerometerStreamingData(AccelerometerDataPoint[] dataPoints) {
            for (Flic2ButtonListener listener : listeners) {
                listener.onAccelerometerStreamingData(dataPoints);
            }
        }

        @Override
        public void onFallDetectionUpdated(FallDetectionEvent event) {
            for (Flic2ButtonListener listener : listeners) {
                listener.onFallDetectionUpdated(event);
            }
        }
    };

    Flic2Button(Flic2Manager manager, String bdAddr) {
        this.manager = manager;
        this.bdAddr = bdAddr;
    }

    /**
     * Returns the Bluetooth Device address of the button.
     *
     * @return the Bluetooth device address
     */
    public String getBdAddr() {
        return bdAddr;
    }

    /**
     * Puts the button object in the connect state.
     *
     * <p>Attempts to connect to the button. If Bluetooth is off, the button is not
     * available, due to either being out of range or not currently advertising, then
     * the button will be connected once it becomes available since this
     * call will not time out, also called a pending connection.</p>
     *
     * <p>If the connection drops, a reconnection attempt will be made automatically.</p>
     *
     * <p>This state is exited by calling the {@link #disconnectOrAbortPendingConnection()} method.</p>
     *
     * <p>If the button already is in this state, or {@link #isUnpaired()} is true, this method does nothing.</p>
     *
     * <p>When targeting and running on Android 12 or higher, the BLUETOOTH_CONNECT runtime permission is required.</p>
     */
    public void connect() {
        manager.connectButton(this);
    }

    /**
     * Puts the button object in the disconnect state.
     *
     * <p>If the connection is established, it is disconnected and the {@link Flic2ButtonListener#onDisconnect} callback will later be run.
     * Else it aborts a pending connection request, if there is one.</p>
     */
    public void disconnectOrAbortPendingConnection() {
        manager.disconnectButton(this, false);
    }

    /**
     * Gets the connection state of this button.
     *
     * <p>Returns one of {@link #CONNECTION_STATE_DISCONNECTED}, {@link #CONNECTION_STATE_CONNECTING},
     * {@link #CONNECTION_STATE_CONNECTED_STARTING} and {@link #CONNECTION_STATE_CONNECTED_READY}.</p>
     *
     * <p>To get the expected results, run this on the thread that is associated with the
     * manager's Handler.</p>
     *
     * @return the connection state
     */
    public int getConnectionState() {
        if (!wantConnected) {
            return CONNECTION_STATE_DISCONNECTED;
        }
        if (!isConnected) {
            return CONNECTION_STATE_CONNECTING;
        }
        Flic2Manager.FlicGattCallback cb = currentGattCb;
        if (cb != null) {
            Session session = cb.getSession();
            if (session != null && session.isEstablished()) {
                return CONNECTION_STATE_CONNECTED_READY;
            }
        }
        return CONNECTION_STATE_CONNECTED_STARTING;
    }

    /**
     * Adds a button event listener.
     *
     * <p>The processing order of events will be the same order as the listeners are added.
     * If a listener is added while a callback is currently executing, the new listener will also be called.</p>
     *
     * <p>If the listener is already added, this method does nothing.</p>
     *
     * <p>NOTE: remember to remove the listener when appropriate.
     * If you add a listener in an onCreate method for example in an Activity,
     * remember to always remove it in the corresponding onDestroy method.</p>
     *
     * @param listener the listener
     */
    public void addListener(final Flic2ButtonListener listener) {
        if (listener == null) {
            throw new NullPointerException();
        }
        manager.runOnHandlerThread(new Runnable() {
            @Override
            public void run() {
                listeners.add(listener);
            }
        });
    }

    /**
     * Removes a button event listener.
     *
     * <p>If a listener is removed while a callback is currently executing, the removed listener will not be called.</p>
     *
     * @param listener the listener
     */
    public void removeListener(final Flic2ButtonListener listener) {
        manager.runOnHandlerThread(new Runnable() {
            @Override
            public void run() {
                listeners.remove(listener);
            }
        });
    }

    void clearListeners() {
        manager.runOnHandlerThread(new Runnable() {
            @Override
            public void run() {
                listeners.clear();
            }
        });
    }

    /**
     * Returns the name stored on the button's memory (latest known if disconnected).
     *
     * @return the name, might be an empty string if not set
     */
    public String getName() {
        return name;
    }

    /**
     * Sets a new name of the button.
     *
     * <p>This sets a new name of the button. The new name will be written to the button's internal memory.
     * If the button is disconnected it will sync when it later connects.</p>
     *
     * <p>Assuming this method is called on the manager's handler's thread, the name returned by
     * {@link #getName()} will immediately reflect the newly set name.</p>
     *
     * <p>If {@link #isUnpaired()} is true however, this method will do nothing.</p>
     *
     * @param name a name which will be truncated so that it takes up at most 23 bytes (after UTF-8 conversion), must not be null
     */
    public void setName(String name) {
        if (name == null) {
            throw new IllegalArgumentException("name is null");
        }
        byte[] asUtf8 = name.getBytes(StandardCharsets.UTF_8);
        if (asUtf8.length > 23) {
            name = new String(asUtf8, 0, Utils.truncateUTF8StringToMaxLenBytes(asUtf8, 23), StandardCharsets.UTF_8);
        }
        final String finalName = name;
        manager.runOnHandlerThread(new Runnable() {
            @Override
            public void run() {
                if (unpaired) {
                    return;
                }

                Flic2Button.this.nameTimestampUtcMs = System.currentTimeMillis();
                Flic2Button.this.name = finalName;
                manager.database.updateName(Flic2Button.this);
                if (Flic2Button.this.isConnected) {
                    Session s = Flic2Button.this.currentGattCb.getSession();
                    if (s != null && s.isEstablished() && s.gotInitialButtonEvents) {
                        s.sendSetName();
                    }
                }
            }
        });
    }

    /**
     * Sets how long the connection should stay alive.
     *
     * <p>This value controls when the button should automatically disconnect due to inactivity.
     * Use this to preserve battery life, but it might affect latency.</p>
     *
     * <p>A value of 511 means infinite. Otherwise the button will automatically disconnect after
     * the specified number of seconds.</p>
     *
     * <p>The value is not persistent between app restarts, so if this feature is used this method
     * should be called after a new button has been paired as well as upon app start.</p>
     *
     * @param numSeconds number of seconds, between 40 and 511
     */
    public void setAutoDisconnectTime(final int numSeconds) {
        if (numSeconds < 40 || numSeconds > 511) {
            throw new IllegalArgumentException("numSeconds must be between 40 and 511");
        }

        manager.runOnHandlerThread(new Runnable() {
            @Override
            public void run() {
                if (unpaired) {
                    return;
                }

                if (Flic2Button.this.autoDisconnectTime != numSeconds) {
                    Flic2Button.this.autoDisconnectTime = (short)numSeconds;
                    if (Flic2Button.this.isConnected) {
                        Session s = Flic2Button.this.currentGattCb.getSession();
                        if (s != null && s.isEstablished()) {
                            s.sendSetAutoDisconnectTime();
                        }
                    }
                }
            }
        });
    }

    /**
     * Enables the Push-Twist feature for Flic Duo.
     *
     * <p>This feature must be re-enabled every time the button reconnects
     * after {@link Flic2ButtonListener#onReady(Flic2Button, long)} has been received.</p>
     *
     * @param bigButton If it should be enabled for the big button.
     * @param smallButton If it should be enabled for the small button.
     */
    public void enableDuoPushTwist(boolean bigButton, boolean smallButton) {
        byte mask = (byte) ((bigButton ? 1 : 0) | (smallButton ? 2 : 0));

        manager.runOnHandlerThread(new Runnable() {
            @Override
            public void run() {
                if (unpaired) {
                    return;
                }

                if (Flic2Button.this.isConnected) {
                    Session s = Flic2Button.this.currentGattCb.getSession();
                    if (s != null && s.isEstablished() && s.isDuo) {
                        if (s.duoPushTwistMask != mask) {
                            s.duoPushTwistMask = mask;
                            s.sendEnablePushTwistInd();
                        }
                    }
                }
            }
        });
    }

    /**
     * Gets the firmware version.
     *
     * @return the firmware version
     */
    public int getFirmwareVersion() {
        return firmwareVersion;
    }

    /**
     * Gets the uuid of the button.
     *
     * This is just a 32 characters long hex string.
     *
     * @return unique uuid
     */
    public String getUuid() {
        return uuid;
    }

    /**
     * Gets the serial number of the button
     *
     * @return serial number
     */
    public String getSerialNumber() {
        return serialNumber;
    }

    /**
     * Checks if a button is unpaired.
     *
     * <p>This value becomes true when {@link Flic2Manager#forgetButton(Flic2Button)} or
     * {@link Flic2ButtonListener#onUnpaired(Flic2Button)} is called.</p>
     *
     * <p>At that point this object becomes "dead" and {@link #connect()} will do nothing.
     * Pair the button again with {@link Flic2Manager#startScan(Flic2ScanCallback)} to get a new object.</p>
     *
     * @return true if unpaired
     */
    public boolean isUnpaired() {
        return unpaired;
    }

    /**
     * Gets the button's timestamp when it became ready.
     *
     * <p>This is the same value that was latest sent in {@link Flic2ButtonListener#onReady(Flic2Button, long)}.
     * Might return an unexpected result if called from another thread than the manager's handler's thread if the button just reconnected.</p>
     *
     * @return the timestamp in milliseconds since the button booted
     */
    public long getReadyTimestamp() {
        return readyTimestamp;
    }

    /**
     * Gets press count.
     *
     * <p>This property specifies how many times the button has been
     * toggled at any given time since boot. This will register all down events
     * as well as the up events, which means that if you want to
     * know how many times it has been clicked then you have to
     * divide this number by two. Also, this property will always
     * contain the last known registered value, meaning that if
     * the button has been pressed while not being within proximity
     * then the property will of course not be up to date. It will
     * be updated as soon as the button connects the next time.</p>
     *
     * @return The press count
     */
    public int getPressCount() {
        return (eventCount[0] + 1) / 2;
    }

    /**
     * Gets press count for Flic Duo.
     *
     * <p>This property specifies how many times the button has been
     * toggled at any given time since boot. This will register all down events
     * as well as the up events, which means that if you want to
     * know how many times it has been clicked then you have to
     * divide this number by two. Also, this property will always
     * contain the last known registered value, meaning that if
     * the button has been pressed while not being within proximity
     * then the property will of course not be up to date. It will
     * be updated as soon as the button connects the next time.</p>
     *
     * @return The press count
     * @param buttonNumber 0 is for the big button and 1 is for the small button on Flic Duo
     */
    public int getPressCount(int buttonNumber) {
        if (buttonNumber < 0 || buttonNumber > 1) {
            throw new IllegalArgumentException("buttonNumber out of range");
        }
        return (eventCount[buttonNumber] + 1) / 2;
    }

    /**
     * Gets the last known battery level.
     *
     * <p>This method returns a cached value.</p>
     *
     * <p>It can return {@code null} in case the button was rebooted and the value
     * has not yet been fetched.</p>
     *
     * @return Battery level info or {@code null} if unknown
     */
    public BatteryLevel getLastKnownBatteryLevel() {
        if (lastKnownBatteryVoltage != null) {
            return new BatteryLevel(lastKnownBatteryVoltage, lastKnownBatteryTimestampUtcMs);
        } else {
            return null;
        }
    }

    /**
     * Sets a new hid midi config.
     *
     * <p>The button must have firmware version at least version 9, and the button must be connected and ready.</p>
     *
     * @param config   The config blob
     * @param callback Will be called when the operation completes (no callback if button disconnects before completion)
     */
    public void setHidMidiConfig(final byte[] config, final SetHidMidiConfigCallback callback) {
        if (callback == null) {
            throw new IllegalArgumentException("callback is null");
        }
        manager.handler.post(new Runnable() {
            @Override
            public void run() {
                if (Flic2Button.this.isConnected) {
                    Session s = Flic2Button.this.currentGattCb.getSession();
                    if (s != null && s.isEstablished()) {
                        if (Flic2Button.this.firmwareVersion >= 9) {
                            s.setHidMidiConfig(config, callback);
                        } else {
                            callback.onResult(SetHidMidiConfigCallback.INCOMPATIBLE_FIRMWARE_VERSION);
                        }
                        return;
                    }
                }
                callback.onResult(SetHidMidiConfigCallback.NOT_READY);
            }
        });
    }

    /**
     * Gets the current hid midi config.
     *
     * <p>The button must have firmware version at least version 9, and the button must be connected and ready.</p>
     *
     * @param callback Will be called when the operation completes (no callback if button disconnects before completion)
     */
    public void getHidMidiConfig(final GetHidMidiConfigCallback callback) {
        if (callback == null) {
            throw new IllegalArgumentException("callback is null");
        }
        manager.runOnHandlerThread(new Runnable() {
            @Override
            public void run() {
                if (Flic2Button.this.isConnected) {
                    Session s = Flic2Button.this.currentGattCb.getSession();
                    if (s != null && s.isEstablished()) {
                        if (Flic2Button.this.firmwareVersion >= 9) {
                            s.getHidMidiConfig(callback);
                        } else {
                            callback.onResult(GetHidMidiConfigCallback.INCOMPATIBLE_FIRMWARE_VERSION, null);
                        }
                        return;
                    }
                }
                callback.onResult(GetHidMidiConfigCallback.NOT_READY, null);
            }
        });
    }

    /**
     * Triggers an immediate firmware update.
     *
     * <p>If the button is ready and is responding, the callback {@link Flic2ButtonListener#onFirmwareVersionCheckComplete(Flic2Button, boolean, boolean)}
     * will be called shortly after this function is called. If a new version is to be updated, {@link Flic2ButtonListener#onFirmwareVersionUpdated(Flic2Button, int)}
     * will then be called when the update completes successfully.</p>
     *
     * <p>Note that a firmware update check will be performed once per 24 hours, even if this method is not called.</p>
     */
    public void triggerFirmwareUpdate() {
        manager.runOnHandlerThread(new Runnable() {
            @Override
            public void run() {
                if (Flic2Button.this.isConnected) {
                    Session s = Flic2Button.this.currentGattCb.getSession();
                    if (s != null && s.isEstablished()) {
                        nextFirmwareCheckTimestamp = Long.MIN_VALUE;
                        currentGattCb.getSession().checkFirmwareTimer();
                    }
                }
            }
        });
    }

    /**
     * Plays a series of notes on Flic Duo's buzzer.
     *
     * <p>The button must be connected and ready. If a previous sequence is already in progress playing, that one will be aborted.</p>
     *
     * <p>Minimum firmware version: 20.</p>
     *
     * @param notes The maximum number of notes is 30.
     * @throws IllegalArgumentException If the number of notes is more than 30.
     */
    public void playBuzzerSound(BuzzerNote[] notes) {
        if (notes.length > 30) {
            throw new IllegalArgumentException("Too many notes (" + notes.length + "), max: 30");
        }
        manager.runOnHandlerThread(new Runnable() {
            @Override
            public void run() {
                if (Flic2Button.this.isConnected) {
                    Session s = Flic2Button.this.currentGattCb.getSession();
                    if (s != null && s.isEstablished()) {
                        currentGattCb.getSession().playBuzzerSound(notes);
                    }
                }
            }
        });
    }

    /**
     * Enables accelerometer streaming for Flic Duo.
     *
     * <p>Samples will be delivered through the {@link Flic2ButtonListener#onAccelerometerStreamingData(AccelerometerDataPoint[])} method.</p>
     *
     * <p>Minimum firmware version: 20.</p>
     *
     * @param config The configuration that the accelerometer will use.
     * @param callback Called when the accelerometer streaming has been enabled in the Flic Duo or an error occurred.
     *                 The callback might be executed as an inner call by this method.
     *                 The callback may not execute if the link disconnects or another call to this or the disable method occurs before the callback has executed.
     */
    public void enableAccelerometerStreaming(AccelerometerStreamingConfig config, EnableAccelerometerStreamingCallback callback) {
        EnableAccelerometerStreamingCallback cb = callback != null ? callback : new EnableAccelerometerStreamingCallback() {
            @Override
            public void onResult(int result) {
            }
        };
        manager.runOnHandlerThread(new Runnable() {
            @Override
            public void run() {
                if (Flic2Button.this.isConnected) {
                    Session s = Flic2Button.this.currentGattCb.getSession();
                    if (s != null && s.isEstablished()) {
                        currentGattCb.getSession().configureAccelerometerStreaming(config, cb);
                        return;
                    }
                }
                cb.onResult(EnableAccelerometerStreamingCallback.NOT_READY);
            }
        });
    }

    /**
     * Disables accelerometer streaming.
     */
    public void disableAccelerometerStreaming() {
        manager.runOnHandlerThread(new Runnable() {
            @Override
            public void run() {
                if (Flic2Button.this.isConnected) {
                    Session s = Flic2Button.this.currentGattCb.getSession();
                    if (s != null && s.isEstablished()) {
                        currentGattCb.getSession().disableAccelerometerStreaming();
                    }
                }
            }
        });
    }

    /**
     * Enables fall detection for Flic Duo.
     *
     * <p>Fall detection continuously monitors the built-in accelerometer for the pattern of a fall: a low-G event, indicating falling, followed by a high-G impact.</p>
     *
     * <p>When the configured conditions are met, an event is reported through {@link Flic2ButtonListener#onFallDetectionUpdated(FallDetectionEvent)}, where accelerometer data for the fall event is delivered.</p>
     *
     * <p>Fall detection is only kept active as long as the Flic is connected and the session is kept alive. The recommendation is to call this method at every {@link Flic2ButtonListener#onReady(Flic2Button, long)}.</p>
     *
     * <p>Minimum firmware version: 20.</p>
     *
     * <p>See <a href="https://github.com/50ButtonsEach/flic2-documentation/wiki/Fall-Detection-Documentation">the fall detection documentation page</a> for more information.</p>
     *
     * @param config The thresholds and timing used to detect a fall.
     * @param alwaysReconnect If {@code true}, also enables always-reconnect advertising.
     *                        This is recommended for fall detection because a fall can be detected without a button press, while a button press is normally the signal that causes the Duo to advertise and reconnect after a lost connection.
     *                        When set to {@code true}, the implementation acts as if {@link #setAlwaysReconnect(boolean, SetAlwaysReconnectCallback)} is executed with an empty callback before the call to this method.
     *                        If {@code false}, the always reconnect setting will not be touched.
     * @param callback        Called when the fall detection has been enabled in the Flic Duo or an error occurred. The callback might be executed as an inner call by this method. The callback may not execute if the link disconnects or another call to this or the disable method occurs before the callback has executed.
     */
    public void enableFallDetection(FallDetectionConfig config, boolean alwaysReconnect, EnableFallDetectionCallback callback) {
        EnableFallDetectionCallback cb = callback != null ? callback : new EnableFallDetectionCallback() {
            @Override
            public void onResult(int result) {
            }
        };
        manager.runOnHandlerThread(new Runnable() {
            @Override
            public void run() {
                if (Flic2Button.this.isConnected) {
                    Session s = Flic2Button.this.currentGattCb.getSession();
                    if (s != null && s.isEstablished()) {
                        currentGattCb.getSession().enableFallDetection(config, alwaysReconnect, cb);
                        return;
                    }
                }
                cb.onResult(EnableFallDetectionCallback.NOT_READY);
            }
        });
    }

    /**
     * Disables fall detection.
     *
     * @param disableAlwaysReconnect If {@code true}, disables always-reconnect advertising after disabling fall detection
     *                               (the implementation acts as if {@link #setAlwaysReconnect(boolean, SetAlwaysReconnectCallback)}
     *                               is executed after the call to this method).
     *                               If {@code false}, this setting is not touched and the callback parameter is not used.
     * @param callback               See {@link #setAlwaysReconnect(boolean, SetAlwaysReconnectCallback)}.
     */
    public void disableFallDetection(boolean disableAlwaysReconnect, SetAlwaysReconnectCallback callback) {
        SetAlwaysReconnectCallback cb = callback != null ? callback : new SetAlwaysReconnectCallback() {
            @Override
            public void onResult(int result) {
            }
        };
        manager.runOnHandlerThread(new Runnable() {
            @Override
            public void run() {
                if (Flic2Button.this.isConnected) {
                    Session s = Flic2Button.this.currentGattCb.getSession();
                    if (s != null && s.isEstablished()) {
                        currentGattCb.getSession().disableFallDetection(disableAlwaysReconnect, cb);
                        return;
                    }
                }
                if (disableAlwaysReconnect) {
                    cb.onResult(SetAlwaysReconnectCallback.NOT_READY);
                }
            }
        });
    }

    /**
     * Sets whether the button should always reconnect when disconnected for Flic Duo.
     *
     * <p>If set to {@code true}, the button will always try to reconnect regardless if it has anything to report.
     * This can be useful if you want to monitor battery levels even if the button is left unused for a long time but might have negative impact on battery performance.</p>
     *
     * <p>If set to {@code false} it typically only reconnects if pressed or lost connection.</p>
     *
     * <p>This setting is persisted on the Flic device.</p>
     *
     * <p>Minimum firmware version: 20.</p>
     *
     * @param alwaysReconnect See above.
     * @param callback The callback might be executed as an inner call by this method.
     *                 The callback may not execute if the link disconnects or another call to this method (or another method that acts as if this method was called) occurs before the callback has executed.
     */
    public void setAlwaysReconnect(boolean alwaysReconnect, SetAlwaysReconnectCallback callback) {
        SetAlwaysReconnectCallback cb = callback != null ? callback : new SetAlwaysReconnectCallback() {
            @Override
            public void onResult(int result) {
            }
        };
        manager.runOnHandlerThread(new Runnable() {
            @Override
            public void run() {
                if (Flic2Button.this.isConnected) {
                    Session s = Flic2Button.this.currentGattCb.getSession();
                    if (s != null && s.isEstablished()) {
                        currentGattCb.getSession().setAlwaysReconnect(alwaysReconnect, cb);
                        return;
                    }
                }
                cb.onResult(SetAlwaysReconnectCallback.NOT_READY);
            }
        });
    }

    /**
     * Exports the pairing key.
     *
     * <p>This is to be used in case a button pairing should be imported into Flic Hub SDK or Flic Device Manager.</p>
     *
     * @return Pairing key
     */
    public byte[] exportPairingKey() {
        if (pairingData == null) {
            return null;
        }
        return Utils.concatArrays(Utils.intToBytes(pairingData.identifier), pairingData.key);
    }

    /**
     * Checks whether this device is a Flic Duo.
     *
     * @return {@code true} if it is a Flic Duo, otherwise {@code false}.
     */
    public boolean isFlicDuo() {
        return serialNumber.startsWith("D");
    }

    /**
     * Gets a string representation.
     *
     * <p>Should only be used for debug purposes.</p>
     *
     * @return A string representing the button
     */
    @Override
    public String toString() {
        return getBdAddr();
    }

    class Session {
        private static final int STATE_WAIT_FULL_VERIFY1 = 0;
        private static final int STATE_BONDING = 1;
        private static final int STATE_WAIT_FULL_VERIFY2 = 2;
        private static final int STATE_WAIT_QUICK_VERIFY = 3;
        private static final int STATE_SESSION_ESTABLISHED = 4;
        private static final int STATE_WAIT_FULL_VERIFY1_TEST_UNPAIRED = 5;
        private static final int STATE_WAIT_TEST_IF_REALLY_UNPAIRED_RESPONSE = 6;
        private static final int STATE_FAILED = 7;
        private static final int STATE_ENDED = 8;

        private static final int FW_UPDATE_STATE_IDLE = 0;
        private static final int FW_UPDATE_STATE_GETTING_BUTTON_VERSION = 1;
        private static final int FW_UPDATE_STATE_DOWNLOADING_FIRMWARE = 2;
        private static final int FW_UPDATE_STATE_STARTING_UPDATE = 3;
        private static final int FW_UPDATE_STATE_PERFORMING_UPDATE = 4;
        private static final int FW_UPDATE_STATE_DONE = 5;

        private static final int SIGNATURE_LENGTH = 5;

        private boolean onL2CAP;
        private SessionCallback sessionCallback;
        private int mtu;
        private boolean txInProgress;
        private Queue<byte[]> txQueue = new LinkedList<>();
        private Queue<TxPacket> requestQueue = new LinkedList<>();
        private int numRequestsPending;

        private int state;
        private int tmpId = Utils.secureRandom.nextInt();
        private int connId;
        private boolean useQuickVerify;
        private byte[] pendingRxPacket;
        private byte[] myPublicKey;
        private byte[] clientRandomBytes;
        private byte[] fullVerifySharedSecret;
        private boolean tmpBdAddressType;
        private long rxCounter;
        private long txCounter;
        private byte[] qvClientRandomBytes;
        private int[] chaskeyKeys;

        private boolean isDuo;
        private boolean hasProcessedEndOfQueueMarker;
        private long lastTimestamp;

        private byte duoPushTwistMask;

        private Runnable firmwareCheckTimerRunnable;
        private int firmwareUpdateState;
        private byte[] firmwareUpdateData;
        private int firmwareUpdateSentPos;
        private int firmwareUpdateAckPos;

        private boolean gotInitialButtonEvents;

        List<Integer> pendingAdvSettings = new LinkedList<>();
        SetAlwaysReconnectCallback pendingAlwaysReconnectCallback;

        private boolean nameRequestPending;
        private boolean shouldResendNameRequest;

        private Runnable batteryCheckTimerRunnable;

        private SetHidMidiConfigCallback setHidMidiConfigCallback;
        private byte[] getHidMidiBuffer;
        private GetHidMidiConfigCallback getHidMidiConfigCallback;

        boolean forceButtonValidationOfAppCredentials;
        byte[] appCredential;

        int[] twistPosition = new int[2];

        int accelerometerStreamingNumPendingRequests;
        EnableAccelerometerStreamingCallback enableAccelerometerStreamingCallback;
        byte accelerometerStreamingScale;

        int fallDetectionNumPendingRequests;
        EnableFallDetectionCallback enableFallDetectionCallback;
        FallDetectionCollector fallDetectionCollector;
        byte fallDetectionScale;

        Session(boolean onL2CAP, SessionCallback sessionCallback) {
            this.onL2CAP = onL2CAP;
            this.sessionCallback = sessionCallback;
        }

        public void end() {
            state = STATE_ENDED;
            if (firmwareCheckTimerRunnable != null) {
                manager.handler.removeCallbacks(firmwareCheckTimerRunnable);
                firmwareCheckTimerRunnable = null;
            }
            if (batteryCheckTimerRunnable != null) {
                manager.handler.removeCallbacks(batteryCheckTimerRunnable);
                batteryCheckTimerRunnable = null;
            }
        }

        public boolean isEstablished() {
            return state == STATE_SESSION_ESTABLISHED;
        }

        public boolean isBonding() {
            return state == STATE_BONDING;
        }

        public void tx(byte[] data) {
            if (!txInProgress) {
                txInProgress = true;
                sessionCallback.tx(data);
            } else {
                txQueue.add(data);
            }
        }

        public void txDone() {
            if (txQueue.isEmpty()) {
                txInProgress = false;
            } else {
                sessionCallback.tx(txQueue.remove());
            }
        }

        private void sendPacket(byte[] data) {
            if (!onL2CAP) {
                if (mtu >= 3 + 1 + data.length) {
                    byte[] p = new byte[1 + data.length];
                    p[0] = (byte) connId;
                    System.arraycopy(data, 0, p, 1, data.length);
                    tx(p);
                } else {
                    for (int i = 0; i < data.length; i += mtu - 4) {
                        byte[] p = new byte[1 + Math.min(mtu - 4, data.length - i)];
                        p[0] = (byte) (connId | (i + mtu - 4 < data.length ? 128 : 0));
                        System.arraycopy(data, i, p, 1, p.length - 1);
                        tx(p);
                    }
                }
            } else {
                tx(data);
            }
        }

        private void sendUnsignedPacket(TxPacket packet) {
            sendPacket(packet.getBytes());
        }

        private byte[] calcSignature(byte[] packet, boolean dirIsToButton) {
            return Flic2Crypto.chaskeyWithDirAndPacketCounter(chaskeyKeys, dirIsToButton ? 1 : 0, dirIsToButton ? txCounter++ : rxCounter++, packet);
        }

        private void sendSignedPacket(TxPacket packet) {
            byte[] pkt = packet.getBytes();
            sendPacket(Utils.concatArrays(pkt, calcSignature(pkt, true)));
        }

        private void tryDequeueRequestQueue() {
            while (!requestQueue.isEmpty() && numRequestsPending < 2) {
                sendSignedPacket(requestQueue.remove());
                ++numRequestsPending;
            }
        }

        private void sendSignedRequest(TxPacket packet) {
            requestQueue.add(packet);
            tryDequeueRequestQueue();
        }

        private void responseReceived() {
            --numRequestsPending;
            tryDequeueRequestQueue();
        }

        private void sendFullVerify() {
            sendUnsignedPacket(new TxPacket.FullVerifyRequest1(tmpId));
            state = STATE_WAIT_FULL_VERIFY1;
        }

        private void sendQuickVerify() {
            TxPacket.QuickVerifyRequest req = new TxPacket.QuickVerifyRequest();
            req.tmpId = tmpId;
            req.pairingId = Flic2Button.this.pairingData.identifier;
            req.random = new byte[7];
            Utils.secureRandom.nextBytes(req.random);
            qvClientRandomBytes = req.random;
            req.encryptionVariant = 0;
            req.signatureVariant = 0;
            req.supportsDuo = true;
            sendUnsignedPacket(req);
            state = STATE_WAIT_QUICK_VERIFY;
        }

        public void start(int mtu) {
            this.mtu = mtu;
            if (pairingData == null) {
                sendFullVerify();
            } else {
                useQuickVerify = true;
                sendQuickVerify();
            }
        }

        private void sendInit() {
            if (!isDuo) {
                TxPacket.InitButtonEventsLightRequest req = new TxPacket.InitButtonEventsLightRequest();
                req.bootId = bootId;
                req.eventCount = eventCount[0];
                req.autoDisconnectTime = Flic2Button.this.autoDisconnectTime;
                req.maxQueuedPackets = useQuickVerify ? 31 : 0;
                req.maxQueuedPacketsAge = 0xfffff;
                sendSignedRequest(req);
            } else {
                TxPacket.InitButtonEventsDuoLightRequest req = new TxPacket.InitButtonEventsDuoLightRequest();
                req.bootId = bootId;
                req.eventCount = eventCount;
                req.autoDisconnectTime = Flic2Button.this.autoDisconnectTime;
                req.maxQueuedPackets = useQuickVerify ? 31 : 0;
                req.maxQueuedPacketsAge = 0xfffff;
                sendSignedRequest(req);
            }
        }

        private void sendConnParamsUpdate(int connIntervalMin, int connIntervalMax, int slaveLatency, int supervisionTimeout) {
            TxPacket.SetConnectionParametersInd ind = new TxPacket.SetConnectionParametersInd();
            ind.intvMin = (short)connIntervalMin;
            ind.intvMax = (short)connIntervalMax;
            ind.latency = (short)slaveLatency;
            ind.timeout = (short)supervisionTimeout;
            sendSignedPacket(ind);
        }

        private void sendAdvSettings(boolean alwaysReconnect) {
            TxPacket.SetAdvParametersRequest req = new TxPacket.SetAdvParametersRequest();
            req.isActive = true;
            req.removeOtherPairingsAdvSettings = false;
            req.advInterval0 = 64;
            req.advInterval1 = 1636;
            req.timeoutSeconds = 86400;
            req.withShortRange = true;
            req.withLongRange = false;
            req.alwaysReconnect = alwaysReconnect;
            sendSignedRequest(req);
            pendingAdvSettings.add(firmwareVersion < 19 ? ADV_SETTINGS_CONFIGURED_YES_NOT_PERSISTENTLY :
                    alwaysReconnect ? ADV_SETTINGS_CONFIGURED_YES_WITH_ALWAYS_RECONNECT : ADV_SETTINGS_CONFIGURED_YES_WITHOUT_ALWAYS_RECONNECT);
        }

        private void sendAdvSettingsIfNeeded() {
            if (firmwareVersion >= 6 && advSettingsConfigured == ADV_SETTINGS_CONFIGURED_NO && pendingAdvSettings.isEmpty()) {
                sendAdvSettings(false);
            }
        }

        private void initialSetName() {
            if (Flic2Button.this.nameTimestampUtcMs == 0) {
                sendSignedRequest(new TxPacket.GetNameRequest());
            } else {
                TxPacket.SetNameRequest req = new TxPacket.SetNameRequest(Flic2Button.this.nameTimestampUtcMs, false, Flic2Button.this.name);
                sendSignedRequest(req);
            }
            nameRequestPending = true;
        }

        private void sendSetName() {
            if (!nameRequestPending) {
                TxPacket.SetNameRequest req = new TxPacket.SetNameRequest(Flic2Button.this.nameTimestampUtcMs, true, Flic2Button.this.name);
                sendSignedRequest(req);
                nameRequestPending = true;
            } else {
                shouldResendNameRequest = true;
            }
        }

        private void sendSetAutoDisconnectTime() {
            sendSignedPacket(new TxPacket.SetAutoDisconnectTimeInd(Flic2Button.this.autoDisconnectTime));
        }

        private void sendEnablePushTwistInd() {
            sendSignedPacket(new TxPacket.EnablePushTwistInd(new boolean[] {(duoPushTwistMask & 1) != 0, (duoPushTwistMask & 2) != 0}));
        }

        private void setHidMidiConfig(byte[] data, SetHidMidiConfigCallback callback) {
            if (setHidMidiConfigCallback != null) {
                callback.onResult(SetHidMidiConfigCallback.ALREADY_IN_PROGRESS);
                return;
            }
            setHidMidiConfigCallback = callback;
            for (int i = 0; i < data.length; i += 120) {
                sendSignedPacket(new TxPacket.SetHidMidiConfigDataInd(Arrays.copyOfRange(data, i, Math.min(i + 120, data.length))));
            }
            sendSignedRequest(new TxPacket.SetHidMidiConfigApplyRequest());
        }

        private void getHidMidiConfig(GetHidMidiConfigCallback callback) {
            if (getHidMidiConfigCallback != null) {
                callback.onResult(GetHidMidiConfigCallback.ALREADY_IN_PROGRESS, null);
                return;
            }
            getHidMidiConfigCallback = callback;
            sendSignedRequest(new TxPacket.GetHidMidiConfigRequest());
        }

        private void onGotName(String name) {
            nameRequestPending = false;
            if (shouldResendNameRequest) {
                shouldResendNameRequest = false;
                sendSetName();
                return;
            }
            if (!name.equals(Flic2Button.this.name)) {
                onNameUpdated(name);
            } else {
                Flic2Button.this.nameTimestampUtcMs = 0;
                manager.database.updateName(Flic2Button.this);
            }
        }

        private void onNameUpdated(String name) {
            Flic2Button.this.nameTimestampUtcMs = 0;
            Flic2Button.this.name = name;
            manager.database.updateName(Flic2Button.this);
            listener.onNameUpdated(Flic2Button.this, name);
        }

        private void checkFirmwareTimer() {
            if (firmwareUpdateState != FW_UPDATE_STATE_IDLE) {
                return;
            }

            long now = System.currentTimeMillis();
            if (now >= nextFirmwareCheckTimestamp) {
                firmwareUpdateState = FW_UPDATE_STATE_GETTING_BUTTON_VERSION;
                sendSignedRequest(new TxPacket.GetFirmwareVersionRequest());
            } else {
                if (firmwareCheckTimerRunnable != null) {
                    manager.handler.removeCallbacks(firmwareCheckTimerRunnable);
                }
                long timeLeft = nextFirmwareCheckTimestamp - now;
                manager.handler.postDelayed(firmwareCheckTimerRunnable = new Runnable() {
                    @Override
                    public void run() {
                        firmwareCheckTimerRunnable = null;
                        checkFirmwareTimer();
                    }
                }, timeLeft);
            }
        }

        private void onGotFirmwareVersion(int oldVersion, final int newVersion) {
            firmwareUpdateState = FW_UPDATE_STATE_DOWNLOADING_FIRMWARE;
            new Thread(new Runnable() {
                @Override
                public void run() {
                    final Utils.FirmwareCheckResult result = Utils.firmwareCheck(manager.context, Flic2Button.this.uuid, newVersion);
                    manager.runOnHandlerThread(new Runnable() {
                        @Override
                        public void run() {
                            if (state != STATE_SESSION_ESTABLISHED || firmwareUpdateState != FW_UPDATE_STATE_DOWNLOADING_FIRMWARE) {
                                return;
                            }
                            if (result.data != null && result.data.length < 1000) {
                                result.data = null;
                                result.nextCheckInMinutes = 24 * 60;
                            }
                            if (result.data == null) {
                                firmwareUpdateState = FW_UPDATE_STATE_IDLE;
                                Flic2Button.this.nextFirmwareCheckTimestamp = System.currentTimeMillis() + (long)result.nextCheckInMinutes * 60 * 1000;
                                manager.database.updateFirmwareCheckTimestamp(Flic2Button.this);
                                checkFirmwareTimer();
                            } else {
                                if (!isDuo) {
                                    byte[] iv = Arrays.copyOf(result.data, 8);
                                    byte[] data = Arrays.copyOfRange(result.data, 8, result.data.length);
                                    firmwareUpdateState = FW_UPDATE_STATE_IDLE;
                                    performFirmwareUpdate(data, iv);
                                } else {
                                    byte[] header = Arrays.copyOf(result.data, 76);
                                    byte[] data = Arrays.copyOfRange(result.data, 76, result.data.length);
                                    firmwareUpdateState = FW_UPDATE_STATE_IDLE;
                                    performFirmwareUpdate(data, header);
                                }
                            }
                            boolean checkSuccess = result.alreadyUpdated || result.data != null;
                            boolean hasNewVersion = result.data != null;
                            listener.onFirmwareVersionCheckComplete(Flic2Button.this, checkSuccess, hasNewVersion);
                        }
                    });
                }
            }).start();
            if (oldVersion != newVersion) {
                listener.onFirmwareVersionUpdated(Flic2Button.this, newVersion);
            }
        }

        private void afterInitialButtonEventsReceived() {
            gotInitialButtonEvents = true;
            sendAdvSettingsIfNeeded();
            initialSetName();
            sendConnParamsUpdate(80, 90, 17, 800);
            if (useQuickVerify) {
                sendBatteryLevelRequest();
            } else {
                sendBatteryLevelRequestDelayed();
            }
            manager.handler.postDelayed(firmwareCheckTimerRunnable = new Runnable() {
                @Override
                public void run() {
                    firmwareCheckTimerRunnable = null;
                    checkFirmwareTimer();
                }
            }, useQuickVerify ? 0 : 30000);
        }

        private void sendBatteryLevelRequest() {
            sendSignedRequest(new TxPacket.GetBatteryLevelRequest());
        }

        private void sendBatteryLevelRequestDelayed() {
            manager.handler.postDelayed(batteryCheckTimerRunnable = new Runnable() {
                @Override
                public void run() {
                    batteryCheckTimerRunnable = null;
                    sendBatteryLevelRequest();
                }
            }, 3 * 60 * 60 * 1000);
        }

        private void playBuzzerSound(BuzzerNote[] notes) {
            if (!isDuo || firmwareVersion < 20) {
                return;
            }
            TxPacket.PlayBuzzerSoundRequestItem[] items = new TxPacket.PlayBuzzerSoundRequestItem[notes.length];
            for (int i = 0; i < notes.length; i++) {
                float hz = notes[i].getHz();
                int halfPeriodLen = 0;
                if (hz > 0) {
                    float halfPeriodLenFloat = 500000.0f / hz;
                    if (halfPeriodLenFloat > 65535.0f) {
                        halfPeriodLenFloat = 65535.0f;
                    }
                    halfPeriodLen = Math.round(halfPeriodLenFloat);
                }
                items[i] = new TxPacket.PlayBuzzerSoundRequestItem(halfPeriodLen, notes[i].getDuration());
            }
            sendSignedRequest(new TxPacket.PlayBuzzerSoundRequest(items));
        }

        private void configureAccelerometerStreaming(AccelerometerStreamingConfig config, EnableAccelerometerStreamingCallback callback) {
            if (!isDuo) {
                callback.onResult(EnableAccelerometerStreamingCallback.NOT_SUPPORTED);
                return;
            }
            if (firmwareVersion < 20) {
                callback.onResult(EnableAccelerometerStreamingCallback.FIRMWARE_UPDATE_NEEDED);
                return;
            }
            TxPacket.ConfigureAccelerometerStreamingRequest r = new TxPacket.ConfigureAccelerometerStreamingRequest();
            r.lowPowerMode = (byte)config.lowPowerMode;
            r.mode = (byte)config.mode;
            r.outputDataRate = (byte)config.outputDataRate;
            r.bandwidthFilter = (byte)config.bandwidthFilter;
            r.fullScaleSelection = (byte)config.fullScaleSelection;
            r.filterDatatypeSelection = (byte)config.filterDatatypeSelection;
            r.lowNoise = config.lowNoise;
            r.highPassRefMode = config.highPassRefMode;
            r.onlyWhilePressed = config.onlyWhilePressed;
            r.samplesPerBurst = (byte)config.samplesPerBurst;
            sendSignedRequest(r);
            ++accelerometerStreamingNumPendingRequests;
            enableAccelerometerStreamingCallback = callback;
            accelerometerStreamingScale = (byte)config.fullScaleSelection;
        }

        private void disableAccelerometerStreaming() {
            if (isDuo && firmwareVersion >= 20) {
                sendSignedRequest(new TxPacket.DisableAccelerometerStreamingRequest());
                ++accelerometerStreamingNumPendingRequests;
            }
        }

        private void enableFallDetection(FallDetectionConfig config, boolean alwaysReconnect, EnableFallDetectionCallback callback) {
            if (!isDuo) {
                callback.onResult(EnableFallDetectionCallback.NOT_SUPPORTED);
                return;
            }
            if (firmwareVersion < 20) {
                callback.onResult(EnableFallDetectionCallback.FIRMWARE_UPDATE_NEEDED);
                return;
            }
            if (alwaysReconnect) {
                boolean correctNow = advSettingsConfigured == ADV_SETTINGS_CONFIGURED_YES_WITH_ALWAYS_RECONNECT;
                boolean correctLastInQueue = !pendingAdvSettings.isEmpty() && pendingAdvSettings.get(pendingAdvSettings.size() - 1) == ADV_SETTINGS_CONFIGURED_YES_WITH_ALWAYS_RECONNECT;
                if ((pendingAdvSettings.isEmpty() && !correctNow) || (!pendingAdvSettings.isEmpty() && !correctLastInQueue)) {
                    sendAdvSettings(true);
                }
                pendingAlwaysReconnectCallback = null;
            }
            TxPacket.ConfigureFallDetectionRequest r = new TxPacket.ConfigureFallDetectionRequest(
                    (short)config.lowGThresholdMg,
                    (short)config.lowGDurationMs,
                    (short)config.highGTimeoutMs,
                    (short)config.highGThresholdMg,
                    (short)config.highGTimeWindowMs,
                    (short)config.postEventRecordDurationMs,
                    (byte)config.fullScaleSelection);
            sendSignedRequest(r);
            ++fallDetectionNumPendingRequests;
            enableFallDetectionCallback = callback;
            fallDetectionScale = (byte)config.fullScaleSelection;
        }

        private void disableFallDetection(boolean disableAlwaysReconnect, SetAlwaysReconnectCallback callback) {
            if (isDuo && firmwareVersion >= 20) {
                sendSignedRequest(new TxPacket.CancelFallDetectionRequest());
                ++fallDetectionNumPendingRequests;
            }
            if (disableAlwaysReconnect) {
                setAlwaysReconnect(false, callback);
            }
        }

        private void sendFallDetectionUpdatesIfNeeded() {
            if (fallDetectionCollector == null) {
                return;
            }

            boolean preFallComplete = fallDetectionCollector.numSamplesReceived >= fallDetectionCollector.phase0NumSamples;
            boolean postFallComplete = fallDetectionCollector.numSamplesReceived >= fallDetectionCollector.phase0NumSamples + fallDetectionCollector.phase1NumSamples;

            if (preFallComplete && !fallDetectionCollector.didSendPreFallDataCollected) {
                fallDetectionCollector.didSendPreFallDataCollected = true;
                listener.onFallDetectionUpdated(fallDetectionCollector.getEvent(FallDetectionEvent.STATE_PRE_FALL_DATA_COLLECTED));
            }

            if (postFallComplete && fallDetectionCollector != null) {
                listener.onFallDetectionUpdated(fallDetectionCollector.getEvent(FallDetectionEvent.STATE_COMPLETED));
                fallDetectionCollector = null;
            }
        }

        private void setAlwaysReconnect(boolean alwaysReconnect, SetAlwaysReconnectCallback callback) {
            if (!isDuo) {
                callback.onResult(SetAlwaysReconnectCallback.NOT_SUPPORTED);
                return;
            }
            if (firmwareVersion < 20) {
                callback.onResult(SetAlwaysReconnectCallback.FIRMWARE_UPDATE_NEEDED);
                return;
            }
            int target = alwaysReconnect ? ADV_SETTINGS_CONFIGURED_YES_WITH_ALWAYS_RECONNECT : ADV_SETTINGS_CONFIGURED_YES_WITHOUT_ALWAYS_RECONNECT;
            boolean correctNow = advSettingsConfigured == target;
            boolean correctLastInQueue = !pendingAdvSettings.isEmpty() && pendingAdvSettings.get(pendingAdvSettings.size() - 1) == target;
            if ((pendingAdvSettings.isEmpty() && !correctNow) || (!pendingAdvSettings.isEmpty() && !correctLastInQueue)) {
                sendAdvSettings(alwaysReconnect);
                pendingAlwaysReconnectCallback = callback;
            } else if (pendingAdvSettings.isEmpty()) {
                callback.onResult(SetAlwaysReconnectCallback.SUCCESS);
            } else {
                pendingAlwaysReconnectCallback = callback;
            }
        }

        private void firmwareUpdateContinue() {
            if (!isDuo) {
                while (firmwareUpdateSentPos < firmwareUpdateData.length / 4 && firmwareUpdateSentPos - firmwareUpdateAckPos < 512) {
                    int len = Math.min(firmwareUpdateData.length / 4 - firmwareUpdateSentPos, 30);
                    len = Math.min(len, 512 - (firmwareUpdateSentPos - firmwareUpdateAckPos));
                    sendSignedPacket(new TxPacket.FirmwareUpdateDataInd(Arrays.copyOfRange(firmwareUpdateData, firmwareUpdateSentPos * 4, (firmwareUpdateSentPos + len) * 4)));
                    firmwareUpdateSentPos += len;
                }
            } else {
                int limit = 12 * 110;
                while (firmwareUpdateSentPos < firmwareUpdateData.length && firmwareUpdateSentPos - firmwareUpdateAckPos < limit) {
                    int len = Math.min(firmwareUpdateData.length - firmwareUpdateSentPos, 110);
                    len = Math.min(len, limit - (firmwareUpdateSentPos - firmwareUpdateAckPos));
                    sendSignedPacket(new TxPacket.FirmwareUpdateDataDuoInd(Arrays.copyOfRange(firmwareUpdateData, firmwareUpdateSentPos, firmwareUpdateSentPos + len)));
                    firmwareUpdateSentPos += len;
                }
            }
        }

        public void performFirmwareUpdate(byte[] data, byte[] header) {
            if (firmwareUpdateState != FW_UPDATE_STATE_IDLE) {
                return;
            }

            firmwareUpdateData = data;
            if (!isDuo) {
                sendSignedRequest(new TxPacket.StartFirmwareUpdateRequest(data.length / 4, header, 60));
            } else {
                sendSignedRequest(new TxPacket.StartFirmwareUpdateDuoRequest(data.length, header, 2));
            }
            firmwareUpdateState = FW_UPDATE_STATE_STARTING_UPDATE;
        }

        public void onBondComplete() {
            if (state == STATE_BONDING) {
                if (appCredential == null) {
                    byte[] verifier = Arrays.copyOf(Utils.createHmacSha256(fullVerifySharedSecret).doFinal(new byte[]{'A', 'T'}), 16);

                    TxPacket.FullVerifyRequest2WithoutAppToken req = new TxPacket.FullVerifyRequest2WithoutAppToken();
                    req.ecdhPublicKey = myPublicKey;
                    req.randomBytes = clientRandomBytes;
                    req.mustValidateAppToken = forceButtonValidationOfAppCredentials;
                    req.supportsDuo = true;
                    req.verifier = verifier;
                    sendUnsignedPacket(req);
                } else {
                    byte[] appToken = Utils.createHmacSha256(fullVerifySharedSecret).doFinal(new byte[]{'a', 'p', 'p'});
                    for (int i = 0; i < 16; i++) {
                        appToken[i] ^= appCredential[i];
                    }
                    appToken = Arrays.copyOf(appToken, 16);

                    Mac verifierMac = Utils.createHmacSha256(fullVerifySharedSecret);
                    verifierMac.update(new byte[]{'A', 'T'});
                    verifierMac.update(appToken);
                    byte[] verifier = Arrays.copyOf(verifierMac.doFinal(), 16);

                    TxPacket.FullVerifyRequest2WithAppToken req = new TxPacket.FullVerifyRequest2WithAppToken();
                    req.ecdhPublicKey = myPublicKey;
                    req.randomBytes = clientRandomBytes;
                    req.encryptedAppToken = appToken;
                    req.mustValidateAppToken = forceButtonValidationOfAppCredentials;
                    req.supportsDuo = true;
                    req.verifier = verifier;
                    sendUnsignedPacket(req);
                }

                chaskeyKeys = Flic2Crypto.chaskeyGenerateSubkeys(Arrays.copyOf(Utils.createHmacSha256(fullVerifySharedSecret).doFinal(new byte[]{'S', 'K'}), 16));
                state = STATE_WAIT_FULL_VERIFY2;
            }
        }

        @SuppressLint("MissingPermission")
        public void onData(byte[] value) {
            try {
                int packetConnId;
                if (!onL2CAP) {
                    if (value.length < 2) {
                        return;
                    }
                    packetConnId = value[0] & 0x1f;
                    boolean newlyAssigned = (value[0] & (1 << 5)) != 0;
                    boolean lastFragment = (value[0] & (1 << 7)) == 0;
                    if ((packetConnId != 0 && packetConnId != connId && !newlyAssigned) || (newlyAssigned && connId != 0)) {
                        // To another app
                        return;
                    }

                    if (pendingRxPacket != null) {
                        if (pendingRxPacket.length + (value.length - 1) > 128) {
                            // Invalid packet, drop
                            pendingRxPacket = null;
                            return;
                        }
                        pendingRxPacket = Utils.concatArrays(pendingRxPacket, value, 1);
                    } else {
                        pendingRxPacket = Arrays.copyOfRange(value, 1, value.length);
                    }

                    if (!lastFragment) {
                        return;
                    }
                } else {
                    packetConnId = -1;
                    pendingRxPacket = value;
                }

                byte[] pktWithOpcode = pendingRxPacket;
                pendingRxPacket = null;

                int opcode = pktWithOpcode[0] & 0xff;
                byte[] pkt = Arrays.copyOfRange(pktWithOpcode, 1, pktWithOpcode.length);
                log("Opcode " + opcode);

                if (opcode == RxPacket.NO_LOGICAL_CONNECTION_SLOTS && (state == STATE_WAIT_FULL_VERIFY1 || state == STATE_WAIT_FULL_VERIFY1_TEST_UNPAIRED || state == STATE_WAIT_QUICK_VERIFY)) {
                    RxPacket.NoLogicalConnectionSlots p = new RxPacket.NoLogicalConnectionSlots(pkt);
                    for (int i = 0; i < p.tmpIds.length; i++) {
                        if (p.tmpIds[i] == tmpId) {
                            state = STATE_FAILED;
                            listener.onFailure(Flic2Button.this, Flic2ButtonListener.FAILURE_TOO_MANY_APPS_CONNECTED, 0);
                            sessionCallback.restart(30000);
                            return;
                        }
                    }
                    return;
                }

                if (opcode == RxPacket.FULL_VERIFY_RESPONSE_1 && (state == STATE_WAIT_FULL_VERIFY1 || state == STATE_WAIT_FULL_VERIFY1_TEST_UNPAIRED)) {
                    RxPacket.FullVerifyResponse1 p = new RxPacket.FullVerifyResponse1(pkt);

                    if (tmpId != p.tmpId) {
                        return;
                    }

                    connId = packetConnId;

                    if (!Utils.bdAddrBytesToString(p.bdAddr).equals(bdAddr) || (addressType != null && addressType != p.bdAddrType)) {
                        state = STATE_FAILED;
                        listener.onFailure(Flic2Button.this, Flic2ButtonListener.FAILURE_GENUINE_CHECK_FAILED, Flic2ButtonListener.FAILURE_GENUINE_CHECK_FAILED_SUBCODE_UNEXPECTED_BD_ADDR);
                        return;
                    }
                    tmpBdAddressType = p.bdAddrType;

                    byte[] msg = new byte[39];
                    System.arraycopy(p.bdAddr, 0, msg, 0, 6);
                    msg[6] = (byte) (p.bdAddrType ? 1 : 0);
                    System.arraycopy(p.publicKey, 0, msg, 7, 32);
                    int i = Flic2Crypto.ed25519Verify(p.signature, msg);
                    if (i < 0) {
                        // Report failure, invalid signature
                        state = STATE_FAILED;
                        listener.onFailure(Flic2Button.this, Flic2ButtonListener.FAILURE_GENUINE_CHECK_FAILED, Flic2ButtonListener.FAILURE_GENUINE_CHECK_FAILED_SUBCODE_INVALID_CERTIFICATE);
                        return;
                    }
                    byte[] fullVerifySecretKey = new byte[32];
                    Utils.secureRandom.nextBytes(fullVerifySecretKey);
                    myPublicKey = Flic2Crypto.curve25519Base(fullVerifySecretKey);
                    byte[] sharedSecret = Flic2Crypto.curve25519(p.publicKey, fullVerifySecretKey);
                    clientRandomBytes = new byte[8];
                    Utils.secureRandom.nextBytes(clientRandomBytes);
                    byte[] flags = new byte[1];
                    if (state == STATE_WAIT_FULL_VERIFY1) {
                        flags[0] = (byte)(1 << 7); // supports duo
                        if (forceButtonValidationOfAppCredentials) {
                            flags[0] |= 1 << 6;
                        }
                    }
                    MessageDigest md = Utils.createSha256();
                    md.update(sharedSecret);
                    md.update((byte) i);
                    md.update(p.random);
                    md.update(clientRandomBytes);
                    md.update(flags);
                    fullVerifySharedSecret = md.digest();

                    if (state == STATE_WAIT_FULL_VERIFY1) {
                        if (!p.isInPublicMode) {
                            state = STATE_FAILED;
                            listener.onFailure(Flic2Button.this, Flic2ButtonListener.FAILURE_BUTTON_NOT_IN_PAIRABLE_MODE, 0);
                            return;
                        }
                        state = STATE_BONDING;
                        if (manager.adapter.getRemoteDevice(bdAddr).getBondState() != BluetoothDevice.BOND_BONDED) {
                            sessionCallback.bond();
                        } else {
                            onBondComplete();
                        }
                    } else {
                        Mac hmac = Utils.createHmacSha256(fullVerifySharedSecret);
                        hmac.update(new byte[]{'P', 'T'});
                        hmac.update(Utils.intToBytes(Flic2Button.this.pairingData.identifier));
                        hmac.update(Flic2Button.this.pairingData.key);
                        byte[] pairingToken = Arrays.copyOf(hmac.doFinal(), 16);

                        TxPacket.TestIfReallyUnpairedRequest req = new TxPacket.TestIfReallyUnpairedRequest();
                        req.ecdhPublicKey = myPublicKey;
                        req.randomBytes = clientRandomBytes;
                        req.pairingId = Flic2Button.this.pairingData.identifier;
                        req.pairingToken = pairingToken;
                        sendUnsignedPacket(req);

                        state = STATE_WAIT_TEST_IF_REALLY_UNPAIRED_RESPONSE;
                    }
                    return;
                }

                if (state == STATE_WAIT_QUICK_VERIFY) {
                    if (opcode == RxPacket.QUICK_VERIFY_RESPONSE && pkt.length >= 12 + SIGNATURE_LENGTH) {
                        RxPacket.QuickVerifyResponse rsp = new RxPacket.QuickVerifyResponse(pkt);
                        if (rsp.tmpId != tmpId) {
                            // To another app
                            return;
                        }

                        connId = packetConnId;

                        byte[] data = new byte[16];
                        System.arraycopy(qvClientRandomBytes, 0, data, 0, 7);
                        data[7] = 0; // encryption and signature variant
                        data[7] |= (byte)(1 << 6); // supports duo
                        System.arraycopy(rsp.random, 0, data, 8, 8);
                        chaskeyKeys = Flic2Crypto.chaskeyGenerateSubkeys(Flic2Crypto.chaskey16Bytes(Flic2Crypto.chaskeyGenerateSubkeys(Flic2Button.this.pairingData.key), data));

                        if (!Arrays.equals(calcSignature(Arrays.copyOf(pktWithOpcode, pktWithOpcode.length - SIGNATURE_LENGTH), false), Arrays.copyOfRange(pkt, pkt.length - SIGNATURE_LENGTH, pkt.length))) {
                            state = STATE_FAILED;
                            listener.onFailure(Flic2Button.this, Flic2ButtonListener.FAILURE_QUICK_VERIFY_SIGNATURE_MISMATCH, 0);
                            return;
                        }

                        isDuo = rsp.isDuo;

                        state = STATE_SESSION_ESTABLISHED;
                        sendInit();
                        return;
                    }

                    if (opcode == RxPacket.QUICK_VERIFY_NEGATIVE_RESPONSE) {
                        RxPacket.QuickVerifyNegativeResponse rsp = new RxPacket.QuickVerifyNegativeResponse(pkt);
                        if (rsp.tmpId != tmpId) {
                            // To another app
                            return;
                        }

                        sendFullVerify();
                        state = STATE_WAIT_FULL_VERIFY1_TEST_UNPAIRED;
                        return;
                    }
                }

                if (packetConnId == 0) {
                    return;
                }

                if (opcode == RxPacket.FULL_VERIFY_RESPONSE_2 && state == STATE_WAIT_FULL_VERIFY2 && pkt.length >= 17 + SIGNATURE_LENGTH) {
                    if (!Arrays.equals(calcSignature(Arrays.copyOf(pktWithOpcode, pktWithOpcode.length - SIGNATURE_LENGTH), false), Arrays.copyOfRange(pkt, pkt.length - SIGNATURE_LENGTH, pkt.length))) {
                        state = STATE_FAILED;
                        listener.onFailure(Flic2Button.this, Flic2ButtonListener.FAILURE_GENUINE_CHECK_FAILED, Flic2ButtonListener.FAILURE_GENUINE_CHECK_FAILED_SUBCODE_INVALID_CALCULATED_SIGNATURE);
                        return;
                    }
                    RxPacket.FullVerifyResponse2 rsp = new RxPacket.FullVerifyResponse2(pkt);
                    if (!rsp.appCredentialsMatch) {
                        if (forceButtonValidationOfAppCredentials) {
                            state = STATE_FAILED;
                            listener.onFailure(Flic2Button.this, Flic2ButtonListener.FAILURE_APP_CREDENTIALS_NOT_MATCHING_DENIED_BY_APP, 0);
                            return;
                        } else if (rsp.caresAboutAppCredentials) {
                            state = STATE_FAILED;
                            listener.onFailure(Flic2Button.this, Flic2ButtonListener.FAILURE_APP_CREDENTIALS_NOT_MATCHING_DENIED_BY_BUTTON, 0);
                            return;
                        } else {
                            // ok
                        }
                    }

                    byte[] pk = Utils.createHmacSha256(fullVerifySharedSecret).doFinal(new byte[]{'P', 'K'});
                    int pairingIdentifier = Utils.bytesToInt(pk);
                    byte[] pairingKey = Arrays.copyOfRange(pk, 4, 20);

                    Flic2Button.this.uuid = Utils.bytesToHex(rsp.buttonUuid).toLowerCase();
                    Flic2Button.this.serialNumber = rsp.serialNumber;
                    Flic2Button.this.firmwareVersion = rsp.firmwareVersion;
                    Flic2Button.this.name = rsp.name;
                    Flic2Button.this.pairingData = new PairingData(pairingIdentifier, pairingKey);
                    Flic2Button.this.addressType = tmpBdAddressType;
                    Flic2Button.this.lastKnownBatteryVoltage = rsp.batteryLevel * 3.6f / 1024.0f;
                    Flic2Button.this.lastKnownBatteryTimestampUtcMs = System.currentTimeMillis();
                    manager.database.addButton(Flic2Button.this);

                    isDuo = rsp.isDuo;

                    state = STATE_SESSION_ESTABLISHED;
                    sendInit();

                    sessionCallback.pairingComplete();

                    return;
                }

                if (opcode == RxPacket.FULL_VERIFY_FAIL_RESPONSE && state == STATE_WAIT_FULL_VERIFY2 && pkt.length >= 1) {
                    RxPacket.FullVerifyFailResponse rsp = new RxPacket.FullVerifyFailResponse(pkt);
                    state = STATE_FAILED;
                    if (rsp.reason == RxPacket.FullVerifyFailResponse.NOT_IN_PUBLIC_MODE) {
                        listener.onFailure(Flic2Button.this, Flic2ButtonListener.FAILURE_BUTTON_NOT_IN_PAIRABLE_MODE, 0);
                    } else if (rsp.reason == RxPacket.FullVerifyFailResponse.INVALID_VERIFIER) {
                        listener.onFailure(Flic2Button.this, Flic2ButtonListener.FAILURE_GENUINE_CHECK_FAILED, Flic2ButtonListener.FAILURE_GENUINE_CHECK_FAILED_SUBCODE_INVALID_VERIFIER);
                    } else {
                        listener.onFailure(Flic2Button.this, Flic2ButtonListener.FAILURE_FULL_VERIFY_FAILED_WITH_UNKNOWN_RESULT_CODE, rsp.reason);
                    }
                    return;
                }

                if (opcode == RxPacket.TEST_IF_REALLY_UNPAIRED_RESPONSE && state == STATE_WAIT_TEST_IF_REALLY_UNPAIRED_RESPONSE) {
                    RxPacket.TestIfReallyUnpairedResponse rsp = new RxPacket.TestIfReallyUnpairedResponse(pkt);

                    state = STATE_FAILED;

                    Mac hmac = Utils.createHmacSha256(fullVerifySharedSecret);
                    hmac.update(new byte[]{'P', 'T'});
                    hmac.update(Utils.intToBytes(Flic2Button.this.pairingData.identifier));
                    hmac.update(Flic2Button.this.pairingData.key);
                    byte[] pairingToken = Arrays.copyOf(hmac.doFinal(), 16);

                    hmac = Utils.createHmacSha256(fullVerifySharedSecret);
                    hmac.update(new byte[]{'N', 'E'});
                    hmac.update(pairingToken);
                    byte[] ne = Arrays.copyOf(hmac.doFinal(), 16);

                    if (Arrays.equals(ne, rsp.result)) {
                        log("Pairing was not found in button, removing button...");
                        Flic2Button.this.pairingData = null;
                        Flic2Button.this.unpaired = true;
                        connId = 0;
                        sessionCallback.unpaired();
                        listener.onUnpaired(Flic2Button.this);
                    } else {
                        hmac = Utils.createHmacSha256(fullVerifySharedSecret);
                        hmac.update(new byte[]{'E', 'X'});
                        hmac.update(pairingToken);
                        byte[] ex = Arrays.copyOf(hmac.doFinal(), 16);
                        boolean exMatch = Arrays.equals(ex, rsp.result);
                        log("Unexpected negative response: ex = " + exMatch);
                        if (!exMatch) {
                            listener.onFailure(Flic2Button.this, Flic2ButtonListener.FAILURE_GENUINE_CHECK_FAILED, Flic2ButtonListener.FAILURE_GENUINE_CHECK_FAILED_SUBCODE_INVALID_CALCULATED_SIGNATURE);
                        } else {
                            // Strange, shouldn't happen
                        }
                    }
                    return;
                }

                if (state != STATE_SESSION_ESTABLISHED) {
                    // Unknown opcode for this state
                    return;
                }

                if (pkt.length < SIGNATURE_LENGTH) {
                    // Invalid packet
                    return;
                }

                if (!Arrays.equals(calcSignature(Arrays.copyOf(pktWithOpcode, pktWithOpcode.length - SIGNATURE_LENGTH), false), Arrays.copyOfRange(pkt, pkt.length - SIGNATURE_LENGTH, pkt.length))) {
                    sendSignedPacket(new TxPacket.DisconnectVerifiedLinkInd());
                    state = STATE_FAILED;
                    listener.onFailure(Flic2Button.this, Flic2ButtonListener.FAILURE_PACKET_SIGNATURE_MISMATCH, 0);
                    sessionCallback.restart(5000);
                    return;
                }
                pkt = Arrays.copyOf(pkt, pkt.length - SIGNATURE_LENGTH);

                if (opcode == RxPacket.INIT_BUTTON_EVENTS_RESPONSE_WITH_BOOT_ID || opcode == RxPacket.INIT_BUTTON_EVENTS_RESPONSE_WITHOUT_BOOT_ID || opcode == RxPacket.INIT_BUTTON_EVENTS_DUO_RESPONSE_WITH_BOOT_ID || opcode == RxPacket.INIT_BUTTON_EVENTS_DUO_RESPONSE_WITHOUT_BOOT_ID) {
                    responseReceived();
                    if (opcode == RxPacket.INIT_BUTTON_EVENTS_RESPONSE_WITHOUT_BOOT_ID || opcode == RxPacket.INIT_BUTTON_EVENTS_DUO_RESPONSE_WITHOUT_BOOT_ID) {
                        pkt = Utils.concatArrays(pkt, Utils.intToBytes(Flic2Button.this.bootId));
                    }
                    RxPacket.InitButtonEventsDuoResponse rsp;
                    if (opcode == RxPacket.INIT_BUTTON_EVENTS_RESPONSE_WITH_BOOT_ID || opcode == RxPacket.INIT_BUTTON_EVENTS_RESPONSE_WITHOUT_BOOT_ID) {
                        RxPacket.InitButtonEventsResponse r = new RxPacket.InitButtonEventsResponse(pkt);
                        rsp = new RxPacket.InitButtonEventsDuoResponse(r);
                    } else {
                        rsp = new RxPacket.InitButtonEventsDuoResponse(pkt);
                    }
                    boolean bootIdChanged = Flic2Button.this.bootId != rsp.bootId;
                    boolean eventCountChanged = Flic2Button.this.eventCount[0] != rsp.eventCount[0] || Flic2Button.this.eventCount[1] != rsp.eventCount[1];
                    Flic2Button.this.bootId = rsp.bootId;
                    Flic2Button.this.eventCount = rsp.eventCount;
                    if (eventCountChanged && !bootIdChanged) {
                        manager.database.updateEventCounter(Flic2Button.this);
                    } else if (bootIdChanged) {
                        if (Flic2Button.this.advSettingsConfigured == ADV_SETTINGS_CONFIGURED_YES_NOT_PERSISTENTLY) {
                            Flic2Button.this.advSettingsConfigured = ADV_SETTINGS_CONFIGURED_NO;
                        }
                        if (useQuickVerify) {
                            Flic2Button.this.lastKnownBatteryVoltage = null;
                            Flic2Button.this.lastKnownBatteryTimestampUtcMs = null;
                        }
                        manager.database.updateBootIdAndEventCounter(Flic2Button.this);
                    }
                    if (!rsp.hasQueuedEvents) {
                        hasProcessedEndOfQueueMarker = true;
                        afterInitialButtonEventsReceived();
                    }
                    readyTimestamp = rsp.timestamp;
                    listener.onReady(Flic2Button.this, readyTimestamp);

                    // Battery level should be non-null when !useQuickVerify, but check as a precaution
                    if (!useQuickVerify && Flic2Button.this.lastKnownBatteryVoltage != null) {
                        listener.onBatteryLevelUpdated(Flic2Button.this, new BatteryLevel(Flic2Button.this.lastKnownBatteryVoltage, Flic2Button.this.lastKnownBatteryTimestampUtcMs));
                    }

                    if (!rsp.hasQueuedEvents) {
                        listener.onAllQueuedButtonEventsProcessed(Flic2Button.this);
                    }
                    return;
                }

                if (opcode == RxPacket.BUTTON_EVENT_NOTIFICATION && !isDuo) {
                    RxPacket.ButtonEventNotification p = new RxPacket.ButtonEventNotification(pkt);
                    boolean sendAck = false;
                    boolean anyWasLastQueued = false;
                    int ec = p.eventCounter;
                    p.items[p.items.length - 1].eventCount = ec;
                    for (int i = p.items.length - 2; i >= 0; i--) {
                        // counter mod 4 should be 1: down, 2: hold, 3: up, 0: single click timeout
                        RxPacket.ButtonEventNotificationItem item = p.items[i];
                        int m4 = ec % 4;
                        if (m4 == 0 || m4 == 2) {
                            --ec;
                        } else {
                            int type = item.eventEncoded & 3;
                            if ((item.eventEncoded >> 3) != 0) {
                                type = 0;
                            }
                            if (m4 == 1) { // down
                                if (type == 2) {
                                    // single click timeout
                                    --ec;
                                } else {
                                    // should be up (type should be 0)
                                    ec -= 2;
                                }
                            } else { // up
                                if (type == 3) {
                                    // hold
                                    --ec;
                                } else {
                                    // should be down (type should be 1)
                                    ec -= 2;
                                }
                            }
                        }
                        item.eventCount = ec;
                    }
                    for (RxPacket.ButtonEventNotificationItem item : p.items) {
                        Flic2Button.this.eventCount[0] = item.eventCount;
                        int type = item.eventEncoded & 3;
                        boolean wasHold = false;
                        boolean singleClick = false;
                        boolean doubleClick = false;
                        boolean nextUpWillBeDoubleClick = false;
                        if ((item.eventEncoded >> 3) != 0) {
                            // Button up
                            type = 0;
                            wasHold = (item.eventEncoded & 4) != 0;
                            singleClick = (item.eventEncoded & 2) != 0 && (item.eventEncoded & 1) == 0;
                            doubleClick = (item.eventEncoded & 2) != 0 && (item.eventEncoded & 1) != 0;
                        } else if (item.eventEncoded == 7) {
                            nextUpWillBeDoubleClick = true;
                        }

                        if (type == 0) {
                            // up
                            listener.onButtonEvent(Flic2Button.this, new Flic2ButtonEvent(Flic2ButtonEvent.EVENT_CLASS_UP_OR_DOWN, Flic2ButtonEvent.EVENT_TYPE_UP, ec, item.wasQueued, item.wasQueuedLast && wasHold && !singleClick && !doubleClick, item.timestamp));
                            if (!wasHold) {
                                listener.onButtonEvent(Flic2Button.this, new Flic2ButtonEvent(Flic2ButtonEvent.EVENT_CLASS_CLICK_OR_HOLD, Flic2ButtonEvent.EVENT_TYPE_CLICK, ec, item.wasQueued, item.wasQueuedLast && !singleClick && !doubleClick, item.timestamp));
                                if (singleClick) {
                                    listener.onButtonEvent(Flic2Button.this, new Flic2ButtonEvent(Flic2ButtonEvent.EVENT_CLASS_SINGLE_OR_DOUBLE_CLICK_OR_HOLD, Flic2ButtonEvent.EVENT_TYPE_SINGLE_CLICK, ec, item.wasQueued, false, item.timestamp));
                                }
                            }
                            if (singleClick) {
                                listener.onButtonEvent(Flic2Button.this, new Flic2ButtonEvent(Flic2ButtonEvent.EVENT_CLASS_SINGLE_OR_DOUBLE_CLICK, Flic2ButtonEvent.EVENT_TYPE_SINGLE_CLICK, ec, item.wasQueued, item.wasQueuedLast, item.timestamp));
                            }
                            if (doubleClick) {
                                listener.onButtonEvent(Flic2Button.this, new Flic2ButtonEvent(Flic2ButtonEvent.EVENT_CLASS_SINGLE_OR_DOUBLE_CLICK, Flic2ButtonEvent.EVENT_TYPE_DOUBLE_CLICK, ec, item.wasQueued, false, item.timestamp));
                                listener.onButtonEvent(Flic2Button.this, new Flic2ButtonEvent(Flic2ButtonEvent.EVENT_CLASS_SINGLE_OR_DOUBLE_CLICK_OR_HOLD, Flic2ButtonEvent.EVENT_TYPE_DOUBLE_CLICK, ec, item.wasQueued, item.wasQueuedLast, item.timestamp));
                            }
                        } else if (type == 1) {
                            // down
                            listener.onButtonEvent(Flic2Button.this, new Flic2ButtonEvent(Flic2ButtonEvent.EVENT_CLASS_UP_OR_DOWN, Flic2ButtonEvent.EVENT_TYPE_DOWN, ec, item.wasQueued, item.wasQueuedLast, item.timestamp));
                        } else if (type == 2) {
                            // single click timeout
                            listener.onButtonEvent(Flic2Button.this, new Flic2ButtonEvent(Flic2ButtonEvent.EVENT_CLASS_SINGLE_OR_DOUBLE_CLICK, Flic2ButtonEvent.EVENT_TYPE_SINGLE_CLICK, ec, item.wasQueued, false, item.timestamp));
                            listener.onButtonEvent(Flic2Button.this, new Flic2ButtonEvent(Flic2ButtonEvent.EVENT_CLASS_SINGLE_OR_DOUBLE_CLICK_OR_HOLD, Flic2ButtonEvent.EVENT_TYPE_SINGLE_CLICK, ec, item.wasQueued, item.wasQueuedLast, item.timestamp));
                        } else if (type == 3) {
                            // hold
                            listener.onButtonEvent(Flic2Button.this, new Flic2ButtonEvent(Flic2ButtonEvent.EVENT_CLASS_CLICK_OR_HOLD, Flic2ButtonEvent.EVENT_TYPE_HOLD, ec, item.wasQueued, item.wasQueuedLast && !nextUpWillBeDoubleClick, item.timestamp));
                            if (!nextUpWillBeDoubleClick) {
                                listener.onButtonEvent(Flic2Button.this, new Flic2ButtonEvent(Flic2ButtonEvent.EVENT_CLASS_SINGLE_OR_DOUBLE_CLICK_OR_HOLD, Flic2ButtonEvent.EVENT_TYPE_HOLD, ec, item.wasQueued, item.wasQueuedLast, item.timestamp));
                            }
                        }
                        if ((type == 0 && (singleClick || doubleClick)) || type == 2) {
                            sendAck = true;
                        }
                        if (item.wasQueuedLast) {
                            listener.onAllQueuedButtonEventsProcessed(Flic2Button.this);
                        }
                        anyWasLastQueued |= item.wasQueuedLast;
                    }
                    manager.database.updateEventCounter(Flic2Button.this);
                    if (sendAck) {
                        sendSignedPacket(new TxPacket.AckButtonEventsInd(p.eventCounter));
                    }
                    if (anyWasLastQueued) {
                        afterInitialButtonEventsReceived();
                    }
                    return;
                }

                if (opcode == RxPacket.BUTTON_EVENT_DUO_NOTIFICATION && isDuo) {
                    RxPacket.Reader reader = new RxPacket.Reader(pkt);
                    boolean[] gotEventCount = new boolean[2];

                    boolean sendAck = false;

                    while (reader.left() > 1) {
                        byte buttonNumber = (byte) reader.bits(1);

                        if (!gotEventCount[buttonNumber]) {
                            int eventCounterDelta = (int) reader.bits(1);
                            if (eventCounterDelta == 1 && reader.bitBool()) {
                                int numBits = new int[]{2, 4, 8, 32}[(int) reader.bits(2)];
                                eventCounterDelta = (int) reader.bits(numBits);
                            }
                            ++eventCounterDelta;
                            eventCount[buttonNumber] += eventCounterDelta;
                            gotEventCount[buttonNumber] = true;
                        } else {
                            ++eventCount[buttonNumber];
                        }

                        long timestampDelta = reader.bits(new int[]{8, 10, 13, 16, 24, 32, 40, 48}[(int) reader.bits(3)]);
                        lastTimestamp += timestampDelta;

                        boolean wasQueued = false;
                        boolean thisWasLastQueued = false;

                        if (!hasProcessedEndOfQueueMarker) {
                            if (!reader.bitBool()) {
                                // This event does not mark end of queue
                                wasQueued = true;
                            } else {
                                hasProcessedEndOfQueueMarker = true;
                                if (!reader.bitBool()) {
                                    // This event is the last event queued
                                    wasQueued = true;
                                    thisWasLastQueued = true;
                                } else {
                                    // The last event queued had to be discarded; this event is the first non-queued event
                                    listener.onAllQueuedButtonEventsProcessed(Flic2Button.this);
                                }
                                afterInitialButtonEventsReceived();
                            }
                        }

                        boolean theDoubleClickWasAlsoAHold = false, nextUpWillBeDoubleClick = false;

                        int type = (int) reader.bits(3);
                        if (type <= 4) {
                            // Up
                            if (type == 4) {
                                theDoubleClickWasAlsoAHold = reader.bitBool();
                            }
                        } else if (type == 7) {
                            // Hold
                            nextUpWillBeDoubleClick = reader.bitBool();
                        }

                        byte gestureData = -1;

                        if (type <= 4 || type == 6) {
                            boolean withGesture = reader.bitBool();
                            if (withGesture) {
                                // With gesture
                                boolean gestureRecognized = reader.bitBool();
                                if (gestureRecognized) {
                                    gestureData = (byte) (reader.bits(2) + 1);
                                    String[] gestures = new String[]{"LEFT", "RIGHT", "UP", "DOWN"};
                                } else {
                                    gestureData = 0;
                                }
                            }
                        }

                        byte x = (byte) reader.bits(8);
                        byte y = (byte) reader.bits(8);
                        byte z = (byte) reader.bits(8);

                        if (type <= 5 && eventCount[buttonNumber] % 2 == 0) {
                            // Up or down was not preceded by hold or single click timeout
                            ++eventCount[buttonNumber];
                        }

                        int ec = eventCount[buttonNumber];
                        if (type <= 4) {
                            // Up
                            boolean wasHold = type == 2 || (type == 4 && theDoubleClickWasAlsoAHold);
                            boolean doubleClick = type == 3 || type == 4;
                            boolean singleClick = type == 1 || type == 2;
                            boolean downAtLeastHalfASecond = type == 1 || type == 2 || type == 4;

                            if (singleClick || doubleClick) {
                                sendAck = true;
                            }

                            listener.onButtonEvent(Flic2Button.this, new Flic2ButtonEvent(Flic2ButtonEvent.EVENT_CLASS_UP_OR_DOWN, Flic2ButtonEvent.EVENT_TYPE_UP, ec, buttonNumber, wasQueued, false, lastTimestamp, x, y, z, gestureData, downAtLeastHalfASecond));
                            if (!wasHold) {
                                listener.onButtonEvent(Flic2Button.this, new Flic2ButtonEvent(Flic2ButtonEvent.EVENT_CLASS_CLICK_OR_HOLD, Flic2ButtonEvent.EVENT_TYPE_CLICK, ec, buttonNumber, wasQueued, thisWasLastQueued && !singleClick && !doubleClick, lastTimestamp, x, y, z, gestureData, downAtLeastHalfASecond));
                                if (singleClick) {
                                    listener.onButtonEvent(Flic2Button.this, new Flic2ButtonEvent(Flic2ButtonEvent.EVENT_CLASS_SINGLE_OR_DOUBLE_CLICK_OR_HOLD, Flic2ButtonEvent.EVENT_TYPE_SINGLE_CLICK, ec, buttonNumber, wasQueued, false, lastTimestamp, x, y, z, gestureData, downAtLeastHalfASecond));
                                }
                            }
                            if (singleClick) {
                                listener.onButtonEvent(Flic2Button.this, new Flic2ButtonEvent(Flic2ButtonEvent.EVENT_CLASS_SINGLE_OR_DOUBLE_CLICK, Flic2ButtonEvent.EVENT_TYPE_SINGLE_CLICK, ec, buttonNumber, wasQueued, thisWasLastQueued, lastTimestamp, x, y, z, gestureData, downAtLeastHalfASecond));
                            } else if (doubleClick) {
                                listener.onButtonEvent(Flic2Button.this, new Flic2ButtonEvent(Flic2ButtonEvent.EVENT_CLASS_SINGLE_OR_DOUBLE_CLICK, Flic2ButtonEvent.EVENT_TYPE_DOUBLE_CLICK, ec, buttonNumber, wasQueued, false, lastTimestamp, x, y, z, gestureData, downAtLeastHalfASecond));
                                listener.onButtonEvent(Flic2Button.this, new Flic2ButtonEvent(Flic2ButtonEvent.EVENT_CLASS_SINGLE_OR_DOUBLE_CLICK_OR_HOLD, Flic2ButtonEvent.EVENT_TYPE_DOUBLE_CLICK, ec, buttonNumber, wasQueued, thisWasLastQueued, lastTimestamp, x, y, z, gestureData, downAtLeastHalfASecond));
                            }
                        } else if (type == 5) {
                            // Down
                            listener.onButtonEvent(Flic2Button.this, new Flic2ButtonEvent(Flic2ButtonEvent.EVENT_CLASS_UP_OR_DOWN, Flic2ButtonEvent.EVENT_TYPE_DOWN, ec, buttonNumber, wasQueued, thisWasLastQueued, lastTimestamp, x, y, z, gestureData, false));
                        } else if (type == 6) {
                            // Single click timeout
                            sendAck = true;

                            listener.onButtonEvent(Flic2Button.this, new Flic2ButtonEvent(Flic2ButtonEvent.EVENT_CLASS_SINGLE_OR_DOUBLE_CLICK, Flic2ButtonEvent.EVENT_TYPE_SINGLE_CLICK, ec, buttonNumber, wasQueued, false, lastTimestamp, x, y, z, gestureData, false));
                            listener.onButtonEvent(Flic2Button.this, new Flic2ButtonEvent(Flic2ButtonEvent.EVENT_CLASS_SINGLE_OR_DOUBLE_CLICK_OR_HOLD, Flic2ButtonEvent.EVENT_TYPE_SINGLE_CLICK, ec, buttonNumber, wasQueued, thisWasLastQueued, lastTimestamp, x, y, z, gestureData, false));
                        } else {
                            // Hold
                            listener.onButtonEvent(Flic2Button.this, new Flic2ButtonEvent(Flic2ButtonEvent.EVENT_CLASS_CLICK_OR_HOLD, Flic2ButtonEvent.EVENT_TYPE_HOLD, ec, buttonNumber, wasQueued, thisWasLastQueued && !nextUpWillBeDoubleClick, lastTimestamp, x, y, z, gestureData, true));
                            if (!nextUpWillBeDoubleClick) {
                                listener.onButtonEvent(Flic2Button.this, new Flic2ButtonEvent(Flic2ButtonEvent.EVENT_CLASS_SINGLE_OR_DOUBLE_CLICK_OR_HOLD, Flic2ButtonEvent.EVENT_TYPE_HOLD, ec, buttonNumber, wasQueued, thisWasLastQueued, lastTimestamp, x, y, z, gestureData, true));
                            }
                        }

                        if (thisWasLastQueued) {
                            listener.onAllQueuedButtonEventsProcessed(Flic2Button.this);
                        }
                    }

                    manager.database.updateEventCounter(Flic2Button.this);

                    if (sendAck) {
                        sendSignedPacket(new TxPacket.AckButtonEventsDuoInd(eventCount));
                    }

                    return;
                }

                if (opcode == RxPacket.PUSH_TWIST_DATA_NOTIFICATION && pkt.length >= 5) {
                    RxPacket.PushTwistDataNotification p = new RxPacket.PushTwistDataNotification(pkt);
                    for (int i = 0; i < 2; i++) {
                        if (p.buttonsPressed[i]) {
                            twistPosition[i] += p.angleDiff;
                        }
                    }
                    listener.onDuoPushTwistNotification(new FlicDuoPushTwistNotificationButtonInfo[] {
                            new FlicDuoPushTwistNotificationButtonInfo(p.buttonsPressed[0], p.isFirstEvent[0], p.buttonsPressedForAtLeastHalfASecond[0]),
                            new FlicDuoPushTwistNotificationButtonInfo(p.buttonsPressed[1], p.isFirstEvent[1], p.buttonsPressedForAtLeastHalfASecond[1])
                    }, p.angleDiff);
                    return;
                }

                if (opcode == RxPacket.PING_REQUEST) {
                    sendSignedPacket(new TxPacket.PingResponse());
                    return;
                }

                if (opcode == RxPacket.GET_FIRMWARE_VERSION_RESPONSE && pkt.length >= 4 && firmwareUpdateState == FW_UPDATE_STATE_GETTING_BUTTON_VERSION) {
                    responseReceived();
                    RxPacket.GetFirmwareVersionResponse rsp = new RxPacket.GetFirmwareVersionResponse(pkt);
                    int oldVersion = Flic2Button.this.firmwareVersion;
                    int newVersion = rsp.version;
                    if (oldVersion != newVersion) {
                        Flic2Button.this.firmwareVersion = newVersion;
                        manager.database.updateFirmwareVersion(Flic2Button.this);
                    }
                    log("Firmware version: " + newVersion);
                    onGotFirmwareVersion(oldVersion, newVersion);
                    return;
                }

                if (opcode == RxPacket.START_FIRMWARE_UPDATE_RESPONSE && pkt.length >= 4 && firmwareUpdateState == FW_UPDATE_STATE_STARTING_UPDATE) {
                    responseReceived();
                    RxPacket.StartFirmwareUpdateResponse rsp = new RxPacket.StartFirmwareUpdateResponse(pkt);
                    int startPos = rsp.startPos;
                    if (startPos < 0) {
                        // -1: invalid parameters
                        // -2: busy
                        firmwareUpdateState = FW_UPDATE_STATE_IDLE;
                        Flic2Button.this.nextFirmwareCheckTimestamp = System.currentTimeMillis() + 10*60*1000;
                        manager.database.updateFirmwareCheckTimestamp(Flic2Button.this);
                        checkFirmwareTimer();
                    } else {
                        firmwareUpdateSentPos = startPos;
                        firmwareUpdateAckPos = startPos;
                        firmwareUpdateState = FW_UPDATE_STATE_PERFORMING_UPDATE;
                        firmwareUpdateContinue();
                    }
                    return;
                }

                if (opcode == RxPacket.FIRMWARE_UPDATE_NOTIFICATION && pkt.length >= 4 && firmwareUpdateState == FW_UPDATE_STATE_PERFORMING_UPDATE) {
                    RxPacket.FirmwareUpdateNotification notification = new RxPacket.FirmwareUpdateNotification(pkt);
                    firmwareUpdateAckPos = notification.pos;
                    if (firmwareUpdateAckPos == firmwareUpdateData.length / (isDuo ? 1 : 4)) {
                        // Done
                        log("FW update done");
                        // Button automatically reboots when disconnected after fw update
                        sendSignedPacket(new TxPacket.ForceBtDisconnectInd(true));
                        firmwareUpdateState = FW_UPDATE_STATE_DONE;
                        Flic2Button.this.nextFirmwareCheckTimestamp = System.currentTimeMillis() + 5 * 1000;
                        manager.database.updateFirmwareCheckTimestamp(Flic2Button.this);
                    } else if (firmwareUpdateAckPos == 0) {
                        firmwareUpdateData = null;
                        System.err.println("Invalid signature");
                        firmwareUpdateState = FW_UPDATE_STATE_IDLE;
                        Flic2Button.this.nextFirmwareCheckTimestamp = System.currentTimeMillis() + 24 * 60 * 60 * 1000;
                        manager.database.updateFirmwareCheckTimestamp(Flic2Button.this);
                        checkFirmwareTimer();
                    } else {
                        firmwareUpdateContinue();
                    }
                    return;
                }

                if (opcode == RxPacket.GET_BATTERY_LEVEL_RESPONSE && pkt.length >= 2) {
                    responseReceived();
                    RxPacket.GetBatteryLevelResponse rsp = new RxPacket.GetBatteryLevelResponse(pkt);
                    Flic2Button.this.lastKnownBatteryVoltage = rsp.level * 3.6f / 1024.0f;
                    Flic2Button.this.lastKnownBatteryTimestampUtcMs = System.currentTimeMillis();
                    manager.database.updateBatteryLevel(Flic2Button.this);
                    log("Battery level: " + rsp.level);
                    listener.onBatteryLevelUpdated(Flic2Button.this, new BatteryLevel(Flic2Button.this.lastKnownBatteryVoltage, Flic2Button.this.lastKnownBatteryTimestampUtcMs));
                    sendBatteryLevelRequestDelayed();
                    return;
                }

                if (opcode == RxPacket.SET_NAME_RESPONSE && pkt.length >= 6) {
                    responseReceived();
                    RxPacket.GetSetNameResponse rsp = new RxPacket.GetSetNameResponse(pkt);
                    log("Name: " + rsp.name);
                    onGotName(rsp.name);
                    return;
                }

                if (opcode == RxPacket.GET_NAME_RESPONSE && pkt.length >= 6) {
                    responseReceived();
                    RxPacket.GetSetNameResponse rsp = new RxPacket.GetSetNameResponse(pkt);
                    log("Got name: " + rsp.name);
                    onGotName(rsp.name);
                    return;
                }

                if (opcode == RxPacket.NAME_UPDATED_NOTIFICATION) {
                    if (Flic2Button.this.nameTimestampUtcMs == 0) {
                        RxPacket.NameUpdatedNotification notification = new RxPacket.NameUpdatedNotification(pkt);
                        if (!notification.name.equals(Flic2Button.this.name)) {
                            onNameUpdated(notification.name);
                        }
                    }
                    return;
                }

                if (opcode == RxPacket.SET_ADV_PARAMETERS_RESPONSE) {
                    responseReceived();
                    advSettingsConfigured = pendingAdvSettings.remove(0);
                    manager.database.updateAdvSettingsConfigured(Flic2Button.this);
                    if (pendingAdvSettings.isEmpty()) {
                        SetAlwaysReconnectCallback cb = pendingAlwaysReconnectCallback;
                        if (cb != null) {
                            pendingAlwaysReconnectCallback = null;
                            cb.onResult(SetAlwaysReconnectCallback.SUCCESS);
                        }
                    }
                    return;
                }

                if (opcode == RxPacket.SET_HID_MIDI_CONFIG_APPLY_RESPONSE && pkt.length >= 1) {
                    responseReceived();
                    RxPacket.SetHidMidiConfigApplyResponse rsp = new RxPacket.SetHidMidiConfigApplyResponse(pkt);
                    if (setHidMidiConfigCallback != null) {
                        SetHidMidiConfigCallback callback = setHidMidiConfigCallback;
                        setHidMidiConfigCallback = null;
                        callback.onResult(rsp.result);
                    }
                    return;
                }

                if (opcode == RxPacket.GET_HID_MIDI_CONFIG_DATA_IND) {
                    RxPacket.GetHidMidiConfigDataInd data = new RxPacket.GetHidMidiConfigDataInd(pkt);
                    if (getHidMidiConfigCallback != null) {
                        if (getHidMidiBuffer == null) {
                            getHidMidiBuffer = data.data;
                        } else if (getHidMidiBuffer.length + data.data.length < 1024) {
                            getHidMidiBuffer = Utils.concatArrays(getHidMidiBuffer, data.data);
                        }
                    }
                    return;
                }

                if (opcode == RxPacket.GET_HID_MIDI_CONFIG_RESPONSE && pkt.length >= 1) {
                    responseReceived();
                    RxPacket.GetHidMidiConfigDataResponse rsp = new RxPacket.GetHidMidiConfigDataResponse(pkt);
                    if (getHidMidiConfigCallback != null) {
                        GetHidMidiConfigCallback callback = getHidMidiConfigCallback;
                        byte[] data = getHidMidiBuffer;
                        getHidMidiConfigCallback = null;
                        getHidMidiBuffer = null;
                        callback.onResult(rsp.result, rsp.result == 0 ? data : null);
                    }
                    return;
                }

                if (opcode == RxPacket.CONFIGURE_ACCELEROMETER_STREAMING_RESPONSE && pkt.length >= 1) {
                    --accelerometerStreamingNumPendingRequests;
                    responseReceived();
                    if (accelerometerStreamingNumPendingRequests == 0) {
                        RxPacket.ConfigureAccelerometerStreamingResponse rsp = new RxPacket.ConfigureAccelerometerStreamingResponse(pkt);
                        EnableAccelerometerStreamingCallback callback = enableAccelerometerStreamingCallback;
                        enableAccelerometerStreamingCallback = null;
                        callback.onResult(rsp.result);
                    }
                    return;
                }

                if (opcode == RxPacket.DISABLE_ACCELEROMETER_STREAMING_RESPONSE) {
                    --accelerometerStreamingNumPendingRequests;
                    responseReceived();
                    return;
                }

                if (opcode == RxPacket.ACCELEROMETER_STREAMING_NOTIFICATION) {
                    if (accelerometerStreamingNumPendingRequests == 0) {
                        RxPacket.AccelerometerStreamingNotification notification = new RxPacket.AccelerometerStreamingNotification(pkt);
                        AccelerometerDataPoint[] array = new AccelerometerDataPoint[notification.samples.length / 3];
                        for (int i = 0; i < array.length; i++) {
                            array[i] = new AccelerometerDataPoint(notification.samples[3 * i], notification.samples[3 * i + 1], notification.samples[3 * i + 2], accelerometerStreamingScale);
                        }
                        listener.onAccelerometerStreamingData(array);
                    }
                    return;
                }

                if (opcode == RxPacket.PLAY_BUZZER_SOUND_RESPONSE) {
                    responseReceived();
                    return;
                }

                if (opcode == RxPacket.CONFIGURE_FALL_DETECTION_RESPONSE && pkt.length >= 1) {
                    --fallDetectionNumPendingRequests;
                    responseReceived();
                    RxPacket.ConfigureFallDetectionResponse rsp = new RxPacket.ConfigureFallDetectionResponse(pkt);
                    if (fallDetectionNumPendingRequests == 0) {
                        EnableFallDetectionCallback callback = enableFallDetectionCallback;
                        enableFallDetectionCallback = null;
                        callback.onResult(rsp.result);
                    }
                    return;
                }

                if (opcode == RxPacket.FALL_DETECTION_TRIGGERED_NOTIFICATION && pkt.length >= 8) {
                    RxPacket.FallDetectionTriggeredNotification notification = new RxPacket.FallDetectionTriggeredNotification(pkt);
                    if (fallDetectionNumPendingRequests == 0) {
                        FallDetectionCollector collector = new FallDetectionCollector();
                        collector.phase0SampleRate = notification.phase0SampleRate;
                        collector.phase0NumSamples = notification.phase0NumSamples;
                        collector.phase1SampleRate = notification.phase1SampleRate;
                        collector.phase1NumSamples = notification.phase1NumSamples;
                        collector.samples = new AccelerometerDataPoint[notification.phase0NumSamples + notification.phase1NumSamples];
                        collector.addSamples(notification.samples, fallDetectionScale);
                        fallDetectionCollector = collector;
                        listener.onFallDetectionUpdated(collector.getEvent(FallDetectionEvent.STATE_TRIGGERED));
                        sendFallDetectionUpdatesIfNeeded();
                    }
                    return;
                }

                if (opcode == RxPacket.FALL_DETECTION_SAMPLES_NOTIFICATION) {
                    RxPacket.FallDetectionSamplesNotification notification = new RxPacket.FallDetectionSamplesNotification(pkt);
                    if (fallDetectionNumPendingRequests == 0 && fallDetectionCollector != null) {
                        fallDetectionCollector.addSamples(notification.samples, fallDetectionScale);
                        sendFallDetectionUpdatesIfNeeded();
                    }
                    return;
                }

                if (opcode == RxPacket.CANCEL_FALL_DETECTION_RESPONSE) {
                    --fallDetectionNumPendingRequests;
                    responseReceived();
                    return;
                }
            } catch (RxPacket.UnexpectedEndOfPacketException ex) {
                log("Unexpected end of packet");
            }
        }
    }
}
