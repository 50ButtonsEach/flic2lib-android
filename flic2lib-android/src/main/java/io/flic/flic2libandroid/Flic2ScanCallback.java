package io.flic.flic2libandroid;

/**
 * Flic 2 scan callbacks.
 */
public interface Flic2ScanCallback {
    /**
     * Indicates success.
     *
     * A button has been paired and added to the manager.
     * The button is currently connected and ready to use.
     */
    int RESULT_SUCCESS = 0;

    /**
     * Failure because a scan is already ongoing.
     */
    int RESULT_FAILED_ALREADY_RUNNING = 1;

    /**
     * Failure because Bluetooth is turned off.
     */
    int RESULT_FAILED_BLUETOOTH_OFF = 2;

    /**
     * Failure due to Android's BLE scan API sent us an error.
     *
     * Android's error code is set as subcode, see {@link android.bluetooth.le.ScanCallback}.
     */
    int RESULT_FAILED_SCAN_ERROR = 3;

    /**
     * Failure since no new buttons that were held down 6 seconds were found.
     */
    int RESULT_FAILED_NO_NEW_BUTTONS_FOUND = 4;

    /**
     * Failure since the button held down 6 seconds is already connected to another device.
     */
    int RESULT_FAILED_BUTTON_ALREADY_CONNECTED_TO_OTHER_DEVICE = 5;

    /**
     * Failure since the connection attempt timed out.
     *
     * This usually indicates an issue with the Android device's Bluetooth since it should connect if the device can be discovered.
     */
    int RESULT_FAILED_CONNECT_TIMED_OUT = 6;

    /**
     * Failure since the verification of the button timed out, after it connected.
     */
    int RESULT_FAILED_VERIFY_TIMED_OUT = 7;

    /**
     * Called when the user holds down a button that is already paired to the manager.
     *
     * This will be called at most one time per button per scan.
     *
     * @param button the button
     */
    void onDiscoveredAlreadyPairedButton(Flic2Button button);

    /**
     * Called when a new button is discovered that the user holds down for at least 6 seconds.
     *
     * This will be called at most once per scan.
     * The next callback will be {@link #onConnected()}, or {@link #onComplete(int, int, Flic2Button)} in case of failure.
     *
     * @param bdAddr the Bluetooth device address of the device
     */
    void onDiscovered(String bdAddr);

    /**
     * Called when the Bluetooth connection has been established to the previously discovered button.
     *
     * A pairing attempt to this button will now be initiated.
     * The next callback will be {@link #onComplete(int, int, Flic2Button)}.
     */
    void onConnected();

    /**
     * Called when the scanning has finished for any reason, unless scanning was explicitly stopped.
     *
     * @param result result code, see {@link Flic2ScanCallback} and {@link Flic2ButtonListener} for a list of codes.
     * @param subCode a subcode
     * @param button if {@link #RESULT_SUCCESS}, the button object that is now both connected and ready
     */
    void onComplete(int result, int subCode, Flic2Button button);
}
