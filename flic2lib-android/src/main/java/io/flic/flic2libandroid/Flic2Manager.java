package io.flic.flic2libandroid;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Application;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.UUID;

/**
 * A manager for Flic 2 buttons.
 *
 * <p>This class has a singleton instance that must be initialized before it can be retrieved.</p>
 *
 * <p>In the application's {@link Application} class, override {@link Application#onCreate()}
 * and call {@link #init(Context, Handler)}, passing {@code this} as context and {@code new Handler()}
 * as handler if you want to get all events on the main thread.</p>
 *
 * <p>After initialization, the singleton can betrieved using {@link #getInstance()}.
 * The utility method {@link #initAndGetInstance(Context, Handler)} can be used to combine the method calls.</p>
 *
 * <p>To pair a new Flic 2 button, first make sure the user has the ACCESS_FINE_LOCATION permission.
 * See https://developer.android.com/training/permissions/requesting for a permissions tutorial.
 * Then call {@link #startScan(Flic2ScanCallback)} and implement the callbacks.</p>
 *
 * <p>The manager has a SQLite database that stores all the paired buttons. If you automatically want
 * to connect to all already paired buttons upon app boot, call {@link #getButtons()} in the
 * {@link Application#onCreate()} method of your app followed by {@link Flic2Button#connect()}.</p>
 *
 * <p>It's recommended to register BOOT_COMPLETED and PACKAGE_REPLACED broadcast receivers in your application
 * in order to start the app when the phone boots or the app is updated so that button connection
 * attempts are initiated at those events.</p>
 *
 * <p>To keep the connections alive while an {@link Activity} is not running, you must have a
 * Foreground Service running in your app. Otherwise the app process may be killed at any time
 * and the Bluetooth connections are thereby terminated.</p>
 */
public class Flic2Manager {
    private static final String TAG = "Flic2Manager";

    static final UUID FLIC_SERVICE_UUID = UUID.fromString("00420000-8F59-4420-870D-84F3B617E493");
    static final UUID TX_CHAR_UUID = UUID.fromString("00420001-8F59-4420-870D-84F3B617E493");
    static final UUID RX_CHAR_UUID = UUID.fromString("00420002-8F59-4420-870D-84F3B617E493");

    static private Flic2Manager INSTANCE = new Flic2Manager();

    private final Object initializeLock = new Object();
    private boolean initialized;
    HandlerInterface handler;
    Context context;
    BluetoothAdapter adapter;
    BluetoothManager bluetoothManager;
    Flic2Database database;
    LoggerInterface logger;

    boolean forceButtonValidationOfAppCredentials;

    private final LinkedList<Flic2Button> allButtons = new LinkedList<>();

    private Flic2Manager() {
    }

    private void initialize(Context context, HandlerInterface handler, LoggerInterface logger) {
        synchronized (initializeLock) {
            if (!initialized) {
                this.context = context.getApplicationContext();
                this.handler = handler;
                this.database = new Flic2Database(this.context);
                this.logger = logger;

                this.adapter = BluetoothAdapter.getDefaultAdapter();
                bluetoothManager = (BluetoothManager) this.context.getSystemService(Context.BLUETOOTH_SERVICE);
                this.context.registerReceiver(bluetoothBroadcastReceiver, new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));
                this.context.registerReceiver(bondStateBroadcastReceiver, new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED));
                for (Flic2Button button : database.getButtons(this)) {
                    allButtons.add(button);
                }
                initialized = true;
                log("initialized");
            }
        }
    }

    /**
     * Initializes the library.
     *
     * <p>The handler defines what thread the library will run on. All callbacks will run on this thread.
     * For most method calls to the library, the commands will be posted to the handler if they
     * are invoked from another thread than the handler's thread.</p>
     *
     * @param context An Android context.
     * @param handler The handler defines what thread the library will run on.
     */
    public static void init(Context context, Handler handler) {
        INSTANCE.initialize(context, new AndroidHandler(handler), null);
    }

    /**
     * Initializes the library.
     *
     * <p>The handler defines what thread the library will run on. All callbacks will run on this thread.
     * For most method calls to the library, the commands will be posted to the handler if they
     * are invoked from another thread than the handler's thread.</p>
     *
     * <p>This method takes a custom handler type and should only be used if you have a different
     * thread scheduler.</p>
     *
     * @param context An Android context.
     * @param handler The handler defines what thread the library will run on.
     * @param logger A logger for debug purposes, can be null.
     */
    public static void init(Context context, HandlerInterface handler, LoggerInterface logger) {
        INSTANCE.initialize(context, handler, logger);
    }

    /**
     * Gets the singleton instance of this library.
     *
     * @return the singleton
     * @throws IllegalStateException if not initialized yet
     */
    public static Flic2Manager getInstance() {
        synchronized (INSTANCE.initializeLock) {
            if (!INSTANCE.initialized) {
                throw new IllegalStateException("Not initialized");
            }
        }
        return INSTANCE;
    }

    /**
     * Initializes the library and returns the singleton instance.
     *
     * @see #init(Context, Handler)
     *
     * @param context An Android context.
     * @param handler The handler defines what thread the library will run on.
     * @return the singleton
     */
    public static Flic2Manager initAndGetInstance(Context context, Handler handler) {
        init(context, handler);
        return INSTANCE;
    }

    /**
     * Set logger.
     *
     * @param logger the logger
     */
    public void setLogger(LoggerInterface logger) {
        this.logger = logger;
    }

    void log(String action) {
        log(null, action, (String)null);
    }

    void log(String bdAddr, String action) {
        log(bdAddr, action, (String)null);
    }

    void log(String bdAddr, String action, byte[] text) {
        LoggerInterface logger = this.logger;
        if (logger != null) {
            logger.log(bdAddr, action, text == null ? null : Utils.bytesToHex(text));
        }
    }

    void log(String bdAddr, String action, int code) {
        LoggerInterface logger = this.logger;
        if (logger != null) {
            logger.log(bdAddr, action, "" + code);
        }
    }

    void log(String bdAddr, String action, String text)  {
        LoggerInterface logger = this.logger;
        if (logger != null) {
            logger.log(bdAddr, action, text);
        }
    }

    void runOnHandlerThread(Runnable r) {
        if (!handler.currentThreadIsHandlerThread()) {
            handler.post(r);
        } else {
            r.run();
        }
    }

    private BroadcastReceiver bluetoothBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
            //Log.d("Flic2Manager", "StateChange: " + state);
            log("bt change", state + " " + adapter.isEnabled());
            handler.post(new Runnable() {
                @Override
                public void run() {
                    // In any case, first clean up all devices
                    ArrayList<Flic2Button> buttonsToDisconnect = new ArrayList<>();
                    for (Flic2Button button : allButtons) {
                        if (disconnectGatt(button)) {
                            buttonsToDisconnect.add(button);
                        }
                    }
                    for (Flic2Button button : buttonsToDisconnect) {
                        button.listener.onDisconnect(button);
                    }

                    if (state != BluetoothAdapter.STATE_ON) {
                        if (currentScanState != SCAN_STATE_IDLE) {
                            if (currentScanState == SCAN_STATE_SCANNING) {
                                ScanWrapper.INSTANCE.stopScan(adapter, scanCallback);
                            }
                            if (currentScanState == SCAN_STATE_CONNECTING || currentScanState == SCAN_STATE_VERIFYING) {
                                disconnectGatt(currentScanButton);
                            }
                            cleanupScan().onComplete(Flic2ScanCallback.RESULT_FAILED_BLUETOOTH_OFF, 0, null);
                        }
                    } else {
                        // Turned on, so recreate GATT objects for devices we want connected
                        for (Flic2Button button : allButtons) {
                            if (button.wantConnected) {
                                normalConnect(button);
                            }
                        }
                    }
                }
            });
        }
    };

    private BroadcastReceiver bondStateBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            int newState = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, 0);
            int oldState = intent.getIntExtra(BluetoothDevice.EXTRA_PREVIOUS_BOND_STATE, 0);

            if (device != null) {
                log(device.getAddress(), "bond state change", oldState + " -> " + newState);
            }
        }
    };

    private static final int SCAN_STATE_IDLE = 0;
    private static final int SCAN_STATE_SCANNING = 1;
    private static final int SCAN_STATE_CONNECTING = 2;
    private static final int SCAN_STATE_VERIFYING = 3;

    private int currentScanState;
    private int currentScanCount;
    private Flic2ScanCallback currentFlic2ScanCallback;
    private HashSet<String> alreadyPairedButtonsFoundDuringScan = new HashSet<>();
    private Runnable stopScanRunnable;
    private Runnable stopConnectAttemptRunnable;
    private Runnable stopVerifyAttemptRunnable;
    private Flic2Button currentScanButton;
    private ScanWrapper.Callback scanCallback = new ScanWrapper.Callback() {
        @Override
        public void onScanResult(int callbackType, final ScanWrapper.ScanResult result) {
            log(result.getDevice().toString(), "sr", result.getBytes());
            runOnHandlerThread(new Runnable() {
                @Override
                public void run() {
                    if (currentScanState != SCAN_STATE_SCANNING) {
                        return;
                    }
                    String address = result.getDevice().getAddress();
                    for (Flic2Button button : allButtons) {
                        if (address.equals(button.bdAddr)) {
                            if (alreadyPairedButtonsFoundDuringScan.add(address)) {
                                currentFlic2ScanCallback.onDiscoveredAlreadyPairedButton(button);
                            }
                            return;
                        }
                    }

                    byte[] msd = result.getManufacturerSpecificData(0x30f);
                    if (msd != null && msd.length >= 5) {
                        boolean isConnected = (msd[4] & 2) != 0;
                        if (isConnected) {
                            if (bluetoothManager.getConnectionState(adapter.getRemoteDevice(address), BluetoothProfile.GATT) != BluetoothProfile.STATE_CONNECTED) {
                                ScanWrapper.INSTANCE.stopScan(adapter, scanCallback);
                                log("already connected to other device");
                                cleanupScan().onComplete(Flic2ScanCallback.RESULT_FAILED_BUTTON_ALREADY_CONNECTED_TO_OTHER_DEVICE, 0, null);
                                return;
                            }
                        }
                    }

                    // Found a button, so stop scan and connect to the button
                    ScanWrapper.INSTANCE.stopScan(adapter, scanCallback);
                    handler.removeCallbacks(stopScanRunnable);
                    stopScanRunnable = null;

                    currentScanButton = new Flic2Button(Flic2Manager.this, address);
                    currentScanButton.wantConnected = true;
                    currentScanButton.addListener(new Flic2ButtonListener() {
                        @Override
                        public void onConnect(final Flic2Button button) {
                            if (currentScanState == SCAN_STATE_CONNECTING) {
                                handler.removeCallbacks(stopConnectAttemptRunnable);
                                stopConnectAttemptRunnable = null;

                                stopVerifyAttemptRunnable = new Runnable() {
                                    @Override
                                    public void run() {
                                        stopVerifyAttemptRunnable = null;
                                        disconnectGatt(button);
                                        log("scan verify timeout");
                                        cleanupScan().onComplete(Flic2ScanCallback.RESULT_FAILED_VERIFY_TIMED_OUT, 0, null);
                                    }
                                };
                                handler.postDelayed(stopVerifyAttemptRunnable, 31000);
                                currentScanState = SCAN_STATE_VERIFYING;
                                currentFlic2ScanCallback.onConnected();
                            }
                        }

                        @Override
                        public void onFailure(Flic2Button button, int errorCode, int subCode) {
                            log(button.bdAddr, "onFailure", currentScanState + " " + errorCode + " " + subCode);
                            if (currentScanState == SCAN_STATE_VERIFYING) {
                                disconnectGatt(button);
                                cleanupScan().onComplete(errorCode, subCode, null);
                            }
                        }
                    });
                    connectGatt(currentScanButton);
                    currentScanState = SCAN_STATE_CONNECTING;
                    stopConnectAttemptRunnable = new Runnable() {
                        @Override
                        public void run() {
                            stopConnectAttemptRunnable = null;
                            disconnectGatt(currentScanButton);
                            log("scan connect time out");
                            cleanupScan().onComplete(Flic2ScanCallback.RESULT_FAILED_CONNECT_TIMED_OUT, 0, null);
                        }
                    };
                    handler.postDelayed(stopConnectAttemptRunnable, 31000);

                    currentFlic2ScanCallback.onDiscovered(address);
                }
            });
        }

        @Override
        public void onScanFailed(final int errorCode) {
            log("scan failed", "code " + errorCode);
            runOnHandlerThread(new Runnable() {
                @Override
                public void run() {
                    if (currentScanState == SCAN_STATE_SCANNING) {
                        ScanWrapper.INSTANCE.stopScan(adapter, scanCallback); // Just in case
                        cleanupScan().onComplete(Flic2ScanCallback.RESULT_FAILED_SCAN_ERROR, errorCode, null);
                    }
                }
            });
        }
    };

    /**
     * Converts an error code to a string representation.
     *
     * <p>This is either a result code passed to the scan callback {@link Flic2ScanCallback#onComplete(int, int, Flic2Button)}
     * or a failure code passed to the callback {@link Flic2ButtonListener#onFailure(Flic2Button, int, int)}.</p>
     *
     * @param code an error code
     * @return a non-null string
     */
    public static String errorCodeToString(int code) {
        for (Field field : Flic2ScanCallback.class.getDeclaredFields()) {
            try {
                if (field.getType() == int.class && field.getInt(null) == code) {
                    return field.getName();
                }
            } catch (IllegalAccessException e) {
            }
        }
        for (Field field : Flic2ButtonListener.class.getDeclaredFields()) {
            try {
                if (field.getType() == int.class && field.getInt(null) == code) {
                    return field.getName();
                }
            } catch (IllegalAccessException e) {
            }
        }
        return "UNKNOWN (" + code + ")";
    }

    private Flic2ScanCallback cleanupScan() {
        //Log.d(TAG, "Cleaning up scan, " + currentFlic2ScanCallback + ", " + stopScanRunnable);
        Flic2ScanCallback cb = currentFlic2ScanCallback;
        currentFlic2ScanCallback = null;
        if (stopScanRunnable != null) {
            handler.removeCallbacks(stopScanRunnable);
            stopScanRunnable = null;
        }
        if (stopConnectAttemptRunnable != null) {
            handler.removeCallbacks(stopConnectAttemptRunnable);
            stopConnectAttemptRunnable = null;
        }
        if (stopVerifyAttemptRunnable != null) {
            handler.removeCallbacks(stopVerifyAttemptRunnable);
            stopVerifyAttemptRunnable = null;
        }
        currentScanState = SCAN_STATE_IDLE;
        currentScanButton = null;
        return cb;
    }
    private void pairingComplete() {
        Flic2Button button = currentScanButton;
        synchronized (allButtons) {
            allButtons.add(button);
        }
        button.clearListeners();
        currentScanState = SCAN_STATE_IDLE;
        cleanupScan().onComplete(Flic2ScanCallback.RESULT_SUCCESS, 0, button);
    }
    private void continueScan() {
        log("cont scan");
        ScanWrapper.INSTANCE.startScan(adapter, FLIC_SERVICE_UUID, scanCallback);
        stopScanRunnable = new Runnable() {
            @Override
            public void run() {
                stopScanRunnable = null;
                log("stop scan");
                ScanWrapper.INSTANCE.stopScan(adapter, scanCallback);
                if (++currentScanCount < 3) {
                    continueScan();
                } else {
                    cleanupScan().onComplete(Flic2ScanCallback.RESULT_FAILED_NO_NEW_BUTTONS_FOUND, 0, null);
                }
            }
        };
        handler.postDelayed(stopScanRunnable, 10000);
    }

    @TargetApi(Build.VERSION_CODES.M)
    private void checkScanPermission() {
        if (context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            throw new SecurityException("ACCESS_FINE_LOCATION not granted. Please call `Activity.requestPermissions(String[], int)` first.");
        }
    }

    /**
     * Starts a button scan.
     *
     * <p>This starts a scan for Flic 2 buttons.
     * Please ask the user for ACCESS_FINE_LOCATION first (see https://developer.android.com/training/permissions/requesting).</p>
     *
     * <p>The progress will be published through the callback.</p>
     *
     * <p>If there already is a scan ongoing, the callback is invoked with {@link Flic2ScanCallback#RESULT_FAILED_ALREADY_RUNNING}.</p>
     *
     * @param flic2ScanCallback callback object
     * @throws SecurityException if ACCESS_FINE_LOCATION is not granted before this call
     */
    public void startScan(final Flic2ScanCallback flic2ScanCallback) {
        log("u start scan");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            checkScanPermission();
        }
        handler.post(new Runnable() {
            @Override
            public void run() {
                if (currentScanState != SCAN_STATE_IDLE) {
                    flic2ScanCallback.onComplete(Flic2ScanCallback.RESULT_FAILED_ALREADY_RUNNING, 0, null);
                    return;
                }
                if (!adapter.isEnabled()) {
                    flic2ScanCallback.onComplete(Flic2ScanCallback.RESULT_FAILED_BLUETOOTH_OFF, 0, null);
                    return;
                }
                currentScanCount = 0;
                currentScanState = SCAN_STATE_SCANNING;
                currentFlic2ScanCallback = flic2ScanCallback;
                alreadyPairedButtonsFoundDuringScan.clear();
                continueScan();
            }
        });
    }

    /**
     * Stops the currently running scan.
     *
     * <p>Assuming this method is called from the same thread as the handler's thread, the pairing attempt
     * is immediately aborted and the {@link Flic2ScanCallback#onComplete(int, int, Flic2Button)}
     * callback will NOT be called.</p>
     */
    public void stopScan() {
        log("u stop scan");
        runOnHandlerThread(new Runnable() {
            @Override
            public void run() {
                if (currentScanState == SCAN_STATE_SCANNING) {
                    ScanWrapper.INSTANCE.stopScan(adapter, scanCallback);
                    handler.removeCallbacks(stopScanRunnable);
                    stopScanRunnable = null;
                }
                if (currentScanState == SCAN_STATE_CONNECTING || currentScanState == SCAN_STATE_VERIFYING) {
                    currentScanButton.wantConnected = false;
                    disconnectGatt(currentScanButton);
                    currentScanButton = null;
                }
                cleanupScan();
            }
        });
    }

    private BluetoothGatt connectGatt(BluetoothDevice bluetoothDevice, BluetoothGattCallback cb) {
        log(bluetoothDevice.getAddress(), "c");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            BluetoothGatt gatt;
            gatt = bluetoothDevice.connectGatt(context, true, cb, BluetoothDevice.TRANSPORT_LE);
            return gatt;
        }

        // Fix for https://issuetracker.google.com/issues/36995652
        // "Race condition in BluetoothGatt when using BluetoothDevice#connectGatt"
        try {
            boolean reflectionError = false;

            Method method = adapter.getClass().getDeclaredMethod("getBluetoothManager");
            method.setAccessible(true);
            Object managerService = method.invoke(adapter);

            // IGatt iGatt = managerService.getBluetoothGatt();
            Method gattMethod = managerService.getClass().getDeclaredMethod("getBluetoothGatt");
            gattMethod.setAccessible(true);
            Object iGatt = gattMethod.invoke(managerService);

            if (iGatt == null) {
                return null;
            }

            // BluetoothGatt gatt = new BluetoothGatt(context, iGatt, bluetoothDevice, TRANSPORT_AUTO);
            @SuppressWarnings("unchecked")
            Constructor<BluetoothGatt> constructor = (Constructor<BluetoothGatt>)BluetoothGatt.class.getDeclaredConstructors()[0];
            constructor.setAccessible(true);
            BluetoothGatt gatt = null;
            Class<?>[] args = constructor.getParameterTypes();
            if (args.length == 4) {
                if (args[2].equals(Integer.TYPE)) {
                    gatt = constructor.newInstance(iGatt, bluetoothDevice, 2, 1);
                } else {
                    gatt = constructor.newInstance(context, iGatt, bluetoothDevice, 2);
                }
            } else if (args.length == 3) {
                if (args[2].equals(Integer.TYPE)) {
                    gatt = constructor.newInstance(iGatt, bluetoothDevice, 2);
                } else {
                    gatt = constructor.newInstance(context, iGatt, bluetoothDevice);
                }
            } else {
                reflectionError = true;
            }
            if (!reflectionError) {
                // gatt.mAutoConnect = true;
                Field mAutoConnectField = gatt.getClass().getDeclaredField("mAutoConnect");
                mAutoConnectField.setAccessible(true);
                mAutoConnectField.setBoolean(gatt, true);

                // gatt.connect(true, cb);
                Method connectMethod = gatt.getClass().getDeclaredMethod("connect", Boolean.class, BluetoothGattCallback.class);
                connectMethod.setAccessible(true);
                boolean response = (Boolean) connectMethod.invoke(gatt, true, cb);
                if (!response) {
                    gatt.close();
                }
                return gatt;
            }
        } catch (NoSuchMethodException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | InstantiationException | NoSuchFieldException e) {
            e.printStackTrace();
        }

        BluetoothGatt gatt;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            gatt = bluetoothDevice.connectGatt(context, true, cb, BluetoothDevice.TRANSPORT_LE);
        } else {
            gatt = bluetoothDevice.connectGatt(context, true, cb);
        }
        return gatt;
    }

    private void connectGatt(Flic2Button button) {
        BluetoothDevice device = adapter.getRemoteDevice(button.bdAddr);
        FlicGattCallback cb = new FlicGattCallback(button);
        BluetoothGatt gatt = connectGatt(device, cb);
        if (gatt == null) {
            // If we came here, most likely Bluetooth was just recently turned off, which is handled by the broadcast receiver later
            log(button.bdAddr, "gatt null");
            if (adapter.isEnabled()) {
                // Usually null is never returned if the Bluetooth was turned on
                if (button.disconnectRunnable != null) {
                    button.disconnectRunnable.run();
                }
                button.listener.onFailure(button, Flic2ButtonListener.FAILURE_GATT_CONNECT_ANDROID_ERROR, 0);
            }
            return;
        }
        cb.gatt = gatt;
        button.currentGattCb = cb;
        cb.createdTime = SystemClock.uptimeMillis();
    }

    private boolean disconnectGatt(Flic2Button button) {
        log(button.bdAddr, "d", button.currentGattCb != null ? "not null" : "null");
        if (button.currentGattCb != null) {
            if (SystemClock.uptimeMillis() - button.currentGattCb.createdTime < 300) {
                // To avoid race condition in BluetoothGatt if we disconnect immediately after connect
                try {
                    Thread.sleep(300);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            if (button.currentGattCb.socket != null) {
                try {
                    log(button.bdAddr, "l2cap close");
                    button.currentGattCb.socket.close();
                } catch (IOException e) {
                }
            }
            button.currentGattCb.gatt.disconnect(); // Shouldn't be needed but do it anyway to workaround some buggy phones
            button.currentGattCb.gatt.close();
            button.currentGattCb.cleanup();
            button.currentGattCb = null;
        }
        if (button.isConnected) {
            button.isConnected = false;
            return true;
        }
        return false;
    }

    private void normalConnect(Flic2Button button) {
        connectGatt(button);
    }

    void connectButton(final Flic2Button button) {
        log(button.bdAddr, "u c");
        runOnHandlerThread(new Runnable() {
            @Override
            public void run() {
                if (button.wantConnected || button.unpaired) {
                    return;
                }
                button.wantConnected = true;
                if (adapter.isEnabled()) {
                    normalConnect(button);
                }
            }
        });
    }
    void disconnectButton(final Flic2Button button, final boolean forget) {
        log(button.bdAddr, "u d", forget ? "f" : "");
        runOnHandlerThread(new Runnable() {
            @Override
            public void run() {
                Runnable forgetRunnable = new Runnable() {
                    @Override
                    public void run() {
                        button.listener.onUnpaired(button);
                    }
                };
                if (forget) {
                    button.unpaired = true;
                }
                if (!button.wantConnected) {
                    if (forget) {
                        handler.post(forgetRunnable);
                    }
                    return;
                }
                button.wantConnected = false;
                if (disconnectGatt(button)) {
                    button.disconnectRunnable = new Runnable() {
                        @Override
                        public void run() {
                            button.disconnectRunnable = null;
                            button.listener.onDisconnect(button);
                            if (forget) {
                                button.listener.onUnpaired(button);
                            }
                        }
                    };
                    handler.post(button.disconnectRunnable);
                } else if (forget) {
                    handler.post(forgetRunnable);
                }
            }
        });
    }

    /**
     * Forgets a paired button.
     *
     * <p>Removes a button from the list of paired buttons.
     * If it's in the connected state, it is disconnected.</p>
     *
     * <p>If this button already is removed, this method does nothing.</p>
     *
     * <p>If the button was connected, {@link Flic2ButtonListener#onDisconnect(Flic2Button)} will be called.
     * In any case, {@link Flic2ButtonListener#onUnpaired(Flic2Button)} will finally be called.</p>
     *
     * <p>From now on {@link Flic2Button#connect()} will do nothing.</p>
     *
     * @param button the button to forget
     */
    public void forgetButton(final Flic2Button button) {
        synchronized (allButtons) {
            if (!allButtons.remove(button)) {
                return;
            }
        }
        runOnHandlerThread(new Runnable() {
            @Override
            public void run() {
                database.deleteButton(button);
            }
        });
        disconnectButton(button, true);
    }

    /**
     * Gets a cloned list of all paired buttons.
     *
     * @return the list
     */
    public List<Flic2Button> getButtons() {
        synchronized (allButtons) {
            return (List<Flic2Button>) allButtons.clone();
        }
    }

    /**
     * Gets a button by it's Bluetooth device address.
     *
     * The button must already be paired, otherwise {@code null} is returned.
     *
     * @param bdAddr The Bluetooth device address of the button
     * @return the button object or {@code null} if it's not paired
     */
    public Flic2Button getButtonByBdAddr(String bdAddr) {
        bdAddr = bdAddr.toUpperCase();
        synchronized (allButtons) {
            for (Flic2Button button : allButtons) {
                if (button.bdAddr.equals(bdAddr)) {
                    return button;
                }
            }
        }
        return null;
    }

    class FlicGattCallback extends BluetoothGattCallback {
        private static final int STATE_IDLE = 0;
        private static final int STATE_WAIT_MTU = 1;
        private static final int STATE_WAIT_SERVICE_DISCOVERY = 2;
        private static final int STATE_WAIT_L2CAP_CONNECT = 3;
        private static final int STATE_RUNNING_GATT = 4;
        private static final int STATE_RUNNING_L2CAP = 5;

        Flic2Button button;
        long createdTime;

        FlicGattCallback(Flic2Button button) {
            this.button = button;
        }

        private BluetoothGatt gatt;
        private int state;
        private long disconnectCount;
        private int mtu;

        private BluetoothGattCharacteristic txChar, rxChar;
        private Flic2Button.Session session;
        private Runnable restartRunnable;
        private final Queue<Utils.Pair<Flic2Button.Session, byte[]>> txQueue = new LinkedList<>();
        private final Queue<Utils.Pair<Flic2Button.Session, byte[]>> l2CapTxQueue = new LinkedList<>();

        private BluetoothSocket socket;

        Flic2Button.Session getSession() {
            return session;
        }

        void cleanup() {
            if (session != null) {
                session.end();
            }
            if (restartRunnable != null) {
                handler.removeCallbacks(restartRunnable);
                restartRunnable = null;
            }
        }

        private void start(final boolean onL2CAP) {
            if (onL2CAP) {
                mtu = 128;
            }
            state = onL2CAP ? STATE_RUNNING_L2CAP : STATE_RUNNING_GATT;

            if (session != null) {
                session.end();
            }
            session = button.new Session(onL2CAP, new SessionCallback() {
                @Override
                public void tx(byte[] data) {
                    //Log.d("Flic2Manager", "Writing data " + Utils.bytesToHex(data));
                    if (button.currentGattCb != FlicGattCallback.this) {
                        return;
                    }
                    if (!onL2CAP) {
                        boolean wasEmpty = txQueue.isEmpty();
                        txQueue.add(new Utils.Pair<>(session, data));
                        if (wasEmpty) {
                            txChar.setValue(data);
                            log(button.bdAddr, "wg", data);
                            gatt.writeCharacteristic(txChar);
                        }
                    } else {
                        synchronized (l2CapTxQueue) {
                            l2CapTxQueue.add(new Utils.Pair<>(session, data));
                            l2CapTxQueue.notify();
                        }
                    }
                }

                @Override
                public void bond() {
                    log(button.bdAddr, "bond");
                    gatt.getDevice().createBond();
                }

                @Override
                public void restart(int afterMs) {
                    if (button.currentGattCb != FlicGattCallback.this) {
                        // Can be disconnected in previous callback
                        return;
                    }

                    if (onL2CAP) {
                        log(button.bdAddr, "l2cap restart close " + afterMs);
                        try {
                            socket.close();
                        } catch (IOException e) {
                        }
                        synchronized (l2CapTxQueue) {
                            l2CapTxQueue.clear();
                        }
                        // When reader thread ends, it will notify the write thread
                    }

                    restartRunnable = new Runnable() {
                        @Override
                        public void run() {
                            restartRunnable = null;
                            if (!onL2CAP) {
                                start(false);
                            } else {
                                startUsingL2CAP();
                            }
                        }
                    };
                    handler.postDelayed(restartRunnable, afterMs);
                }


                @Override
                public void pairingComplete() {
                    if (currentScanState == SCAN_STATE_VERIFYING && currentScanButton == button) {
                        log(button.bdAddr, "pc");
                        Flic2Manager.this.pairingComplete();
                    }
                }

                @Override
                public void unpaired() {
                    log(button.bdAddr, "unpaired");
                    disconnectGatt(button);
                    button.wantConnected = false;
                    synchronized (allButtons) {
                        allButtons.remove(button);
                    }
                    database.deleteButton(button);
                }
            });
            session.start(mtu);
        }

        private void startUsingGatt() {
            mtu = 23;
            state = STATE_WAIT_SERVICE_DISCOVERY;
            log(button.bdAddr, "sd");
            gatt.discoverServices();
        }

        private void startUsingL2CAP() {
            try {
                socket = L2CapUtils.getSocket(gatt.getDevice());
                if (socket == null) {
                    startUsingGatt();
                    return;
                }
            } catch (IOException e) {
                // Adapter not enabled anymore
                return;
            }
            state = STATE_WAIT_L2CAP_CONNECT;
            final long thisDisconnectCount = disconnectCount;
            final BluetoothSocket currentSocket = socket;
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        currentSocket.connect();
                    } catch (IOException | SecurityException e) {
                        log(button.bdAddr, "l2cap failed", e.getMessage());
                        e.printStackTrace();
                        try {
                            currentSocket.close();
                        } catch (IOException e1) {
                        }
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                if (button.currentGattCb != FlicGattCallback.this || thisDisconnectCount != disconnectCount) {
                                    return;
                                }
                                startUsingGatt();
                            }
                        });
                        return;
                    }
                    //Log.d(TAG, "l2cap socket connected");
                    log(button.bdAddr, "l2cap connected");

                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            while (true) {
                                final Utils.Pair<Flic2Button.Session, byte[]> item;
                                synchronized (l2CapTxQueue) {
                                    while (l2CapTxQueue.isEmpty() && currentSocket.isConnected()) {
                                        try {
                                            l2CapTxQueue.wait();
                                        } catch (InterruptedException e) {
                                            Thread.currentThread().interrupt();
                                        }
                                    }
                                    if (!currentSocket.isConnected()) {
                                        break;
                                    }
                                    item = l2CapTxQueue.remove();
                                }
                                //Log.d(TAG, "l2cap socket write");
                                log(button.bdAddr, "wl", item.b);
                                try {
                                    currentSocket.getOutputStream().write(item.b);
                                } catch (IOException e) {
                                    break;
                                }
                                handler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        if (button.currentGattCb != FlicGattCallback.this || thisDisconnectCount != disconnectCount) {
                                            return;
                                        }
                                        item.a.txDone();
                                    }
                                });
                            }
                            log(button.bdAddr, "l2cap wdone");
                            //Log.d(TAG, "l2cap socket write thread ended");
                        }
                    }).start();

                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            if (button.currentGattCb != FlicGattCallback.this || thisDisconnectCount != disconnectCount) {
                                return;
                            }
                            start(true);
                        }
                    });

                    byte[] packet = new byte[128];
                    while (true) {
                        if (!currentSocket.isConnected()) {
                            break;
                        }
                        int nread;
                        try {
                            nread = currentSocket.getInputStream().read(packet);
                            //Log.d(TAG, "l2cap read returned " + nread);
                            if (nread == -1) {
                                break;
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                            break;
                        }
                        final byte[] p = Arrays.copyOf(packet, nread);
                        log(button.bdAddr, "r", p);
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                if (button.currentGattCb != FlicGattCallback.this || thisDisconnectCount != disconnectCount || !currentSocket.isConnected()) {
                                    return;
                                }
                                if (state == STATE_RUNNING_L2CAP) {
                                    session.onData(p);
                                }
                            }
                        });
                    }
                    synchronized (l2CapTxQueue) {
                        try {
                            currentSocket.close();
                        } catch (IOException e) {
                        }
                        l2CapTxQueue.notify();
                    }
                    log(button.bdAddr, "l2cap done");
                    //Log.d(TAG, "l2cap socket thread ended");
                }
            }).start();
        }

        @Override
        public void onConnectionStateChange(final BluetoothGatt gatt, final int status, final int newState) {
            log(button.bdAddr, "csc", status + " " + newState);
            handler.post(new Runnable() {
                @Override
                public void run() {
                    //Log.d("Flic2Manager", "onConnectionStateChange " + status + " " + newState);
                    if (button.currentGattCb != FlicGattCallback.this) {
                        return;
                    }
                    if (newState == BluetoothGatt.STATE_CONNECTED) {
                        if (status == BluetoothGatt.GATT_SUCCESS) {
                            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
                                startUsingGatt();
                            } else {
                                startUsingL2CAP();
                            }
                            button.isConnected = true;
                            button.listener.onConnect(button);
                        } else {
                            // Should only happen in Android 4.4 and lower
                            button.listener.onFailure(button, Flic2ButtonListener.FAILURE_GATT_CONNECT_ANDROID_ERROR, status);
                        }
                    } else if (newState == BluetoothGatt.STATE_DISCONNECTED) {
                        state = STATE_IDLE;
                        ++disconnectCount;
                        if (session != null) {
                            session.end();
                        }
                        if (restartRunnable != null) {
                            handler.removeCallbacks(restartRunnable);
                            restartRunnable = null;
                        }
                        if (button.isConnected) {
                            button.isConnected = false;
                            button.listener.onDisconnect(button);
                        } else {
                            // Common error status codes:
                            // 128: No resources to open a new connection
                            // 257: can't Register GATT client, MAX client reached: 32
                            button.listener.onFailure(button, Flic2ButtonListener.FAILURE_GATT_CONNECT_ANDROID_ERROR, status);
                        }
                    }
                }
            });
        }

        @Override
        public void onServicesDiscovered(final BluetoothGatt gatt, final int status) {
            log(button.bdAddr, "sdcmpl", status);
            handler.post(new Runnable() {
                @Override
                public void run() {
                    //Log.d("Flic2Manager", "onServicesDiscovered: " + status);
                    if (button.currentGattCb != FlicGattCallback.this) {
                        return;
                    }
                    if (state == STATE_WAIT_SERVICE_DISCOVERY) {
                        BluetoothGattService flicService = null;
                        for (BluetoothGattService service : gatt.getServices()) {
                            if (service.getUuid().equals(FLIC_SERVICE_UUID)) {
                                flicService = service;
                            }
                        }
                        if (flicService != null) {
                            txChar = flicService.getCharacteristic(TX_CHAR_UUID);
                            rxChar = flicService.getCharacteristic(RX_CHAR_UUID);
                        }

                        if (txChar == null || rxChar == null) {
                            // failed service discovery
                            StringBuilder sb = new StringBuilder();
                            for (BluetoothGattService s : gatt.getServices()) {
                                sb.append("s").append(s.getUuid()).append(' ');
                                for (BluetoothGattCharacteristic c : s.getCharacteristics()) {
                                    sb.append("c").append(c.getUuid()).append(' ');
                                    for (BluetoothGattDescriptor d : c.getDescriptors()) {
                                        sb.append("d").append(d.getUuid()).append(' ');
                                    }
                                }
                            }

                            Log.e("Flic2Manager", "service discovery found incorrect services: " + flicService + " " + txChar + " " + rxChar + " " + sb);
                            log(button.bdAddr, "incorrect", flicService + " " + txChar + " " + rxChar);
                            if (button.pairingData == null) {
                                try {
                                    gatt.getClass().getMethod("refresh", new Class[0]).invoke(gatt, new Object[0]);
                                } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                                    e.printStackTrace();
                                }
                            }
                            button.listener.onFailure(button, Flic2ButtonListener.FAILURE_SERVICE_DISCOVERY_UNEXPECTED_GATT_DB, 0);
                            return;
                        }
                        txChar.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
                        gatt.setCharacteristicNotification(rxChar, true);

                        if (mtu != 23) {
                            start(false);
                        } else {
                            state = STATE_WAIT_MTU;
                            log(button.bdAddr, "mtuReq");
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                gatt.requestMtu(517);
                            } else {
                                onMtuChanged(gatt, 23, 0);
                            }
                        }
                    }
                }
            });
        }

        @Override
        public void onCharacteristicWrite(final BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, final int status) {
            log(button.bdAddr, "wcmpl", status);
            handler.post(new Runnable() {
                @Override
                public void run() {
                    //Log.d("Flic2Manager", "onCharacteristicWrite: " + status);
                    if (button.currentGattCb != FlicGattCallback.this) {
                        return;
                    }
                    if (status == 0) {
                        Utils.Pair<Flic2Button.Session, byte[]> item = txQueue.remove();
                        if (!txQueue.isEmpty()) {
                            byte[] value = txQueue.peek().b;
                            log(button.bdAddr, "wgq", value);
                            txChar.setValue(value);
                            gatt.writeCharacteristic(txChar);
                        }
                        item.a.txDone();
                    }
                }
            });
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic) {
            final byte[] value = characteristic.getValue();
            log(button.bdAddr, "notify", value);
            handler.post(new Runnable() {
                @Override
                public void run() {
                    //Log.d("Flic2Manager", "Got data " + Utils.bytesToHex(value));
                    if (button.currentGattCb != FlicGattCallback.this) {
                        return;
                    }
                    if (characteristic.getUuid().equals(RX_CHAR_UUID) && state == STATE_RUNNING_GATT) {
                        session.onData(value);
                    }
                }
            });
        }

        @Override
        public void onMtuChanged(final BluetoothGatt gatt, final int mtu, final int status) {
            log(button.bdAddr, "mtu", mtu + " " + status);
            handler.post(new Runnable() {
                @Override
                public void run() {
                    //Log.d("Flic2Manager", "onMtuChanged: " + mtu + " " + status);
                    if (button.currentGattCb != FlicGattCallback.this) {
                        return;
                    }
                    FlicGattCallback.this.mtu = mtu;
                    if (state == STATE_WAIT_MTU) {
                        start(false);
                    }
                }
            });
        }
    }
}
