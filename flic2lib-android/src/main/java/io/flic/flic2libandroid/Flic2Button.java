package io.flic.flic2libandroid;

import java.security.MessageDigest;
import java.util.Arrays;
import java.util.LinkedList;
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
     * Call {@link #connect()} if you would like to establish a connection as soon as the button comes in range.
     */
    public static final int CONNECTION_STATE_DISCONNECTED = 0;

    /**
     * The manager is waiting for the button to become in range, at which time it should automatically connect.
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
    boolean advSettingsConfigured;
    int bootId;
    int eventCount;
    long readyTimestamp;
    Float lastKnownBatteryVoltage;
    Long lastKnownBatteryTimestampUtcMs;

    Flic2Manager.FlicGattCallback currentGattCb;
    boolean isConnected;
    boolean wantConnected;
    Runnable disconnectRunnable;

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
        public void onNameUpdated(Flic2Button button, String newName) {
            for (Flic2ButtonListener listener : listeners) {
                listener.onNameUpdated(button, newName);
            }
        }

        @Override
        public void onFirmwareVersionUpdated(Flic2Button button, int newVersion) {
            for (Flic2ButtonListener listener : listeners) {
                listener.onFirmwareVersionUpdated(button, newVersion);
            }
        }

        @Override
        public void onBatteryLevelUpdated(BatteryLevel level) {
            for (Flic2ButtonListener listener : listeners) {
                listener.onBatteryLevelUpdated(level);
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
     * @param name a name of max 23 bytes (after UTF-8 conversion), must not be null
     */
    public void setName(final String name) {
        if (name == null) {
            throw new NullPointerException();
        }
        manager.runOnHandlerThread(new Runnable() {
            @Override
            public void run() {
                if (unpaired) {
                    return;
                }

                Flic2Button.this.nameTimestampUtcMs = System.currentTimeMillis();
                Flic2Button.this.name = name;
                manager.database.updateName(Flic2Button.this);
                if (Flic2Button.this.isConnected) {
                    Session s = Flic2Button.this.currentGattCb.getSession();
                    if (s != null && s.isEstablished()) {
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
        return (eventCount + 1) / 2;
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
        private static final int STATE_WAIT_FULL_VERIFY2 = 1;
        private static final int STATE_WAIT_QUICK_VERIFY = 2;
        private static final int STATE_SESSION_ESTABLISHED = 3;
        private static final int STATE_WAIT_FULL_VERIFY1_TEST_UNPAIRED = 4;
        private static final int STATE_WAIT_TEST_IF_REALLY_UNPAIRED_RESPONSE = 5;
        private static final int STATE_FAILED = 10;
        private static final int STATE_ENDED = 11;

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
        private byte[] fullVerifySharedSecret;
        private boolean tmpBdAddressType;
        private long rxCounter;
        private long txCounter;
        private byte[] qvClientRandomBytes;
        private int[] chaskeyKeys;

        private Runnable firmwareCheckTimerRunnable;
        private int firmwareUpdateState;
        private byte[] firmwareUpdateData;
        private int firmwareUpdateSentPos;
        private int firmwareUpdateAckPos;

        private boolean setNameRequestPending;
        private boolean shouldResendNameRequest;

        private Runnable batteryCheckTimerRunnable;

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
            TxPacket.InitButtonEventsLightRequest req = new TxPacket.InitButtonEventsLightRequest();
            req.bootId = bootId;
            req.eventCount = eventCount;
            req.autoDisconnectTime = Flic2Button.this.autoDisconnectTime;
            req.maxQueuedPackets = useQuickVerify ? 31 : 0;
            req.maxQueuedPacketsAge = 0xfffff;
            sendSignedRequest(req);
        }

        private void sendConnParamsUpdate(int connIntervalMin, int connIntervalMax, int slaveLatency, int supervisionTimeout) {
            TxPacket.SetConnectionParametersInd ind = new TxPacket.SetConnectionParametersInd();
            ind.intvMin = (short)connIntervalMin;
            ind.intvMax = (short)connIntervalMax;
            ind.latency = (short)slaveLatency;
            ind.timeout = (short)supervisionTimeout;
            sendSignedPacket(ind);
        }

        private void sendAdvSettingsIfNeeded() {
            if (firmwareVersion >= 6 && !advSettingsConfigured) {
                TxPacket.SetAdvParametersRequest req = new TxPacket.SetAdvParametersRequest();
                req.isActive = true;
                req.removeOtherPairingsAdvSettings = false;
                req.advInterval0 = 64;
                req.advInterval1 = 1636;
                req.timeoutSeconds = 86400;
                req.withShortRange = true;
                req.withLongRange = false;
                sendSignedRequest(req);
            }
        }

        private void initialSetName() {
            if (!setNameRequestPending) {
                TxPacket.SetNameRequest req = new TxPacket.SetNameRequest(Flic2Button.this.nameTimestampUtcMs, false, Flic2Button.this.name);
                sendSignedRequest(req);
                setNameRequestPending = true;
            }
        }

        private void sendSetName() {
            if (!setNameRequestPending) {
                TxPacket.SetNameRequest req = new TxPacket.SetNameRequest(Flic2Button.this.nameTimestampUtcMs, true, Flic2Button.this.name);
                sendSignedRequest(req);
                setNameRequestPending = true;
            } else {
                shouldResendNameRequest = true;
            }
        }

        private void sendSetAutoDisconnectTime() {
            sendSignedPacket(new TxPacket.SetAutoDisconnectTimeInd(Flic2Button.this.autoDisconnectTime));
        }

        private void onGotName(long timestampUtcMs, String name) {
            setNameRequestPending = false;
            if (shouldResendNameRequest) {
                shouldResendNameRequest = false;
                sendSetName();
                return;
            }
            if (!name.equals(Flic2Button.this.name)) {
                Flic2Button.this.nameTimestampUtcMs = 0;
                Flic2Button.this.name = name;
                manager.database.updateName(Flic2Button.this);
                listener.onNameUpdated(Flic2Button.this, name);
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
                    final Utils.Pair<byte[], Integer> result = Utils.firmwareCheck(manager.context, Flic2Button.this.uuid, newVersion);
                    manager.runOnHandlerThread(new Runnable() {
                        @Override
                        public void run() {
                            if (state != STATE_SESSION_ESTABLISHED || firmwareUpdateState != FW_UPDATE_STATE_DOWNLOADING_FIRMWARE) {
                                return;
                            }
                            if (result.a != null && result.a.length < 1000) {
                                result.a = null;
                                result.b = 24*60;
                            }
                            if (result.a == null) {
                                firmwareUpdateState = FW_UPDATE_STATE_IDLE;
                                Flic2Button.this.nextFirmwareCheckTimestamp = System.currentTimeMillis() + (long)result.b * 60 * 1000;
                                manager.database.updateFirmwareCheckTimestamp(Flic2Button.this);
                                checkFirmwareTimer();
                            } else {
                                byte[] iv = Arrays.copyOf(result.a, 8);
                                byte[] data = Arrays.copyOfRange(result.a, 8, result.a.length);
                                firmwareUpdateState = FW_UPDATE_STATE_IDLE;
                                performFirmwareUpdate(data, iv);
                            }
                        }
                    });
                }
            }).start();
            if (oldVersion != newVersion) {
                listener.onFirmwareVersionUpdated(Flic2Button.this, newVersion);
            }
        }

        private void afterInitialButtonEventsReceived() {
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

        private void firmwareUpdateContinue() {
            while (firmwareUpdateSentPos < firmwareUpdateData.length / 4 && firmwareUpdateSentPos - firmwareUpdateAckPos < 512) {
                int len = Math.min(firmwareUpdateData.length / 4 - firmwareUpdateSentPos, 30);
                len = Math.min(len, 512 - (firmwareUpdateSentPos - firmwareUpdateAckPos));
                sendSignedPacket(new TxPacket.FirmwareUpdateDataInd(Arrays.copyOfRange(firmwareUpdateData, firmwareUpdateSentPos * 4, (firmwareUpdateSentPos + len) * 4)));
                firmwareUpdateSentPos += len;
            }
        }

        public void performFirmwareUpdate(byte[] data, byte[] iv) {
            if (firmwareUpdateState != FW_UPDATE_STATE_IDLE) {
                return;
            }

            firmwareUpdateData = data;
            sendSignedRequest(new TxPacket.StartFirmwareUpdateRequest(data.length / 4, iv, 60));
            firmwareUpdateState = FW_UPDATE_STATE_STARTING_UPDATE;
        }

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
                    int i;
                    for (i = 0; i < 4; i++) {
                        p.signature[32] = (byte) ((p.signature[32] & ~3) | i);
                        if (Flic2Crypto.ed25519Verify(p.signature, msg)) {
                            break;
                        }
                    }
                    if (i == 4) {
                        // Report failure, invalid signature
                        state = STATE_FAILED;
                        listener.onFailure(Flic2Button.this, Flic2ButtonListener.FAILURE_GENUINE_CHECK_FAILED, Flic2ButtonListener.FAILURE_GENUINE_CHECK_FAILED_SUBCODE_INVALID_CERTIFICATE);
                        return;
                    }
                    byte[] fullVerifySecretKey = new byte[32];
                    Utils.secureRandom.nextBytes(fullVerifySecretKey);
                    byte[] myPublicKey = Flic2Crypto.curve25519Base(fullVerifySecretKey);
                    byte[] sharedSecret = Flic2Crypto.curve25519(p.publicKey, fullVerifySecretKey);
                    byte[] clientRandomBytes = new byte[8];
                    Utils.secureRandom.nextBytes(clientRandomBytes);
                    byte[] flags = new byte[1];
                    MessageDigest md = Utils.createSha256();
                    md.update(sharedSecret);
                    md.update((byte) i);
                    md.update(p.random);
                    md.update(clientRandomBytes);
                    md.update(flags);
                    fullVerifySharedSecret = md.digest();

                    if (state == STATE_WAIT_FULL_VERIFY1) {
                        sessionCallback.bond();

                        byte[] verifier = Arrays.copyOf(Utils.createHmacSha256(fullVerifySharedSecret).doFinal(new byte[]{'A', 'T'}), 16);

                        TxPacket.FullVerifyRequest2WithoutAppToken req = new TxPacket.FullVerifyRequest2WithoutAppToken();
                        req.ecdhPublicKey = myPublicKey;
                        req.randomBytes = clientRandomBytes;
                        req.verifier = verifier;
                        sendUnsignedPacket(req);

                        chaskeyKeys = Flic2Crypto.chaskeyGenerateSubkeys(Arrays.copyOf(Utils.createHmacSha256(fullVerifySharedSecret).doFinal(new byte[]{'S', 'K'}), 16));
                        state = STATE_WAIT_FULL_VERIFY2;
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
                        System.arraycopy(rsp.random, 0, data, 8, 8);
                        chaskeyKeys = Flic2Crypto.chaskeyGenerateSubkeys(Flic2Crypto.chaskey16Bytes(Flic2Crypto.chaskeyGenerateSubkeys(Flic2Button.this.pairingData.key), data));

                        if (!Arrays.equals(calcSignature(Arrays.copyOf(pktWithOpcode, pktWithOpcode.length - SIGNATURE_LENGTH), false), Arrays.copyOfRange(pkt, pkt.length - SIGNATURE_LENGTH, pkt.length))) {
                            state = STATE_FAILED;
                            listener.onFailure(Flic2Button.this, Flic2ButtonListener.FAILURE_QUICK_VERIFY_SIGNATURE_MISMATCH, 0);
                            return;
                        }

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
                        if (rsp.caresAboutAppCredentials) {
                            state = STATE_FAILED;
                            listener.onFailure(Flic2Button.this, Flic2ButtonListener.FAILURE_APP_CREDENTIALS_NOT_MATCHING_DENIED_BY_BUTTON, 0);
                            return;
                        } else {
                            if (manager.forceButtonValidationOfAppCredentials) {
                                state = STATE_FAILED;
                                listener.onFailure(Flic2Button.this, Flic2ButtonListener.FAILURE_APP_CREDENTIALS_NOT_MATCHING_DENIED_BY_APP, 0);
                                return;
                            }
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

                if (opcode == RxPacket.INIT_BUTTON_EVENTS_RESPONSE_WITH_BOOT_ID || opcode == RxPacket.INIT_BUTTON_EVENTS_RESPONSE_WITHOUT_BOOT_ID) {
                    responseReceived();
                    if (opcode == RxPacket.INIT_BUTTON_EVENTS_RESPONSE_WITHOUT_BOOT_ID) {
                        pkt = Utils.concatArrays(pkt, Utils.intToBytes(Flic2Button.this.bootId));
                    }
                    RxPacket.InitButtonEventsResponse rsp = new RxPacket.InitButtonEventsResponse(pkt);
                    boolean bootIdChanged = Flic2Button.this.bootId != rsp.bootId;
                    boolean eventCountChanged = Flic2Button.this.eventCount != rsp.eventCount;
                    Flic2Button.this.bootId = rsp.bootId;
                    Flic2Button.this.eventCount = rsp.eventCount;
                    if (eventCountChanged && !bootIdChanged) {
                        manager.database.updateEventCounter(Flic2Button.this);
                    } else if (bootIdChanged) {
                        Flic2Button.this.advSettingsConfigured = false;
                        if (useQuickVerify) {
                            Flic2Button.this.lastKnownBatteryVoltage = null;
                            Flic2Button.this.lastKnownBatteryTimestampUtcMs = null;
                        }
                        manager.database.updateBootIdAndEventCounter(Flic2Button.this);
                    }
                    if (!rsp.hasQueuedEvents) {
                        afterInitialButtonEventsReceived();
                    }
                    readyTimestamp = rsp.timestamp;
                    listener.onReady(Flic2Button.this, readyTimestamp);

                    // Battery level should be non-null when !useQuickVerify, but check as a precaution
                    if (!useQuickVerify && Flic2Button.this.lastKnownBatteryVoltage != null) {
                        listener.onBatteryLevelUpdated(new BatteryLevel(Flic2Button.this.lastKnownBatteryVoltage, Flic2Button.this.lastKnownBatteryTimestampUtcMs));
                    }
                    return;
                }

                if (opcode == RxPacket.BUTTON_NOTIFICATION) {
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
                        Flic2Button.this.eventCount = item.eventCount;
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
                            listener.onButtonUpOrDown(Flic2Button.this, item.wasQueued, item.wasQueuedLast, item.timestamp, true, false);
                            if (!wasHold) {
                                listener.onButtonClickOrHold(Flic2Button.this, item.wasQueued, item.wasQueuedLast, item.timestamp, true, false);
                                if (singleClick) {
                                    listener.onButtonSingleOrDoubleClickOrHold(Flic2Button.this, item.wasQueued, item.wasQueuedLast, item.timestamp, true, false, false);
                                }
                            }
                            if (singleClick)
                                listener.onButtonSingleOrDoubleClick(Flic2Button.this, item.wasQueued, item.wasQueuedLast, item.timestamp, true, false);
                            if (doubleClick) {
                                listener.onButtonSingleOrDoubleClick(Flic2Button.this, item.wasQueued, item.wasQueuedLast, item.timestamp, false, true);
                                listener.onButtonSingleOrDoubleClickOrHold(Flic2Button.this, item.wasQueued, item.wasQueuedLast, item.timestamp, false, true, false);
                            }
                        } else if (type == 1) {
                            // down
                            listener.onButtonUpOrDown(Flic2Button.this, item.wasQueued, item.wasQueuedLast, item.timestamp, false, true);
                        } else if (type == 2) {
                            // single click timeout
                            listener.onButtonSingleOrDoubleClick(Flic2Button.this, item.wasQueued, item.wasQueuedLast, item.timestamp, true, false);
                            listener.onButtonSingleOrDoubleClickOrHold(Flic2Button.this, item.wasQueued, item.wasQueuedLast, item.timestamp, true, false, false);
                        } else if (type == 3) {
                            // hold
                            listener.onButtonClickOrHold(Flic2Button.this, item.wasQueued, item.wasQueuedLast, item.timestamp, false, true);
                            if (!nextUpWillBeDoubleClick) {
                                listener.onButtonSingleOrDoubleClickOrHold(Flic2Button.this, item.wasQueued, item.wasQueuedLast, item.timestamp, false, false, true);
                            }
                        }
                        if ((type == 0 && (singleClick || doubleClick)) || type == 2) {
                            sendAck = true;
                        }
                        anyWasLastQueued |= item.wasQueuedLast;
                    }
                    manager.database.updateEventCounter(Flic2Button.this);
                    if (sendAck) {
                        sendSignedPacket(new TxPacket.AckButtonEvents(p.eventCounter));
                    }
                    if (anyWasLastQueued) {
                        afterInitialButtonEventsReceived();
                    }
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
                    if (firmwareUpdateAckPos == firmwareUpdateData.length / 4) {
                        // Done
                        log("FW update done");
                        // Button automatically reboots when disconnected after fw update
                        sendSignedPacket(new TxPacket.ForceBtDisconnectInd(true));
                        firmwareUpdateState = FW_UPDATE_STATE_DONE;
                        Flic2Button.this.nextFirmwareCheckTimestamp = System.currentTimeMillis() + 60*1000;
                        manager.database.updateFirmwareCheckTimestamp(Flic2Button.this);
                    } else if (firmwareUpdateAckPos == 0) {
                        firmwareUpdateData = null;
                        System.err.println("Invalid signature");
                        firmwareUpdateState = FW_UPDATE_STATE_IDLE;
                        Flic2Button.this.nextFirmwareCheckTimestamp = System.currentTimeMillis() + 24*60*60*1000;
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
                    listener.onBatteryLevelUpdated(new BatteryLevel(Flic2Button.this.lastKnownBatteryVoltage, Flic2Button.this.lastKnownBatteryTimestampUtcMs));
                    sendBatteryLevelRequestDelayed();
                    return;
                }

                if (opcode == RxPacket.SET_NAME_RESPONSE && pkt.length >= 6) {
                    responseReceived();
                    RxPacket.GetSetNameResponse rsp = new RxPacket.GetSetNameResponse(pkt);
                    log("Name: " + rsp.name);
                    onGotName(rsp.timestampUtcMs, rsp.name);
                    return;
                }

                if (opcode == RxPacket.NAME_UPDATED_NOTIFICATION) {
                    RxPacket.NameUpdatedNotification notification = new RxPacket.NameUpdatedNotification(pkt);
                    onNameUpdated(notification.name);
                    return;
                }

                if (opcode == RxPacket.SET_ADV_PARAMETERS_RESPONSE) {
                    responseReceived();
                    advSettingsConfigured = true;
                    manager.database.updateAdvSettingsConfigured(Flic2Button.this);
                    return;
                }
            } catch (RxPacket.UnexpectedEndOfPacketException ex) {
                log("Unexpected end of packet");
            }
        }
    }
}
