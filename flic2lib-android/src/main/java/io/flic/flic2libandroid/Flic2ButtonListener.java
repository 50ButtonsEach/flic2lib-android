package io.flic.flic2libandroid;

/**
 * Flic 2 button listener.
 *
 * <p>This class should be overridden in order to get callbacks.</p>
 *
 * <p>This class also defines error codes used in {@link #onFailure(Flic2Button, int, int)} and {@link Flic2ScanCallback#onComplete(int, int, Flic2Button)}.</p>
 */
public class Flic2ButtonListener {
    /**
     * Failure indicating that app credentials are not matching and the button therefore denied a pairing attempt to us.
     */
    public static final int FAILURE_APP_CREDENTIALS_NOT_MATCHING_DENIED_BY_BUTTON = 10;

    /**
     * Failure indicating that we denied the button's app credentials, since they don't match ours.
     */
    public static final int FAILURE_APP_CREDENTIALS_NOT_MATCHING_DENIED_BY_APP = 11;

    /**
     * Failure indicating the verification attempt didn't pass genuineness check.
     *
     * This will include a subcode.
     */
    public static final int FAILURE_GENUINE_CHECK_FAILED = 12;

    /**
     * Subcode indicating the button's certificate is for a different Bluetooth device address than the one we are connected to.
     */
    public static final int FAILURE_GENUINE_CHECK_FAILED_SUBCODE_UNEXPECTED_BD_ADDR = 0;

    /**
     * Subcode indicating the button's certificate wasn't properly signed by Shortcut Labs.
     */
    public static final int FAILURE_GENUINE_CHECK_FAILED_SUBCODE_INVALID_CERTIFICATE = 1;

    /**
     * Subcode indicating that the button couldn't validate our verifier.
     */
    public static final int FAILURE_GENUINE_CHECK_FAILED_SUBCODE_INVALID_VERIFIER = 2;

    /**
     * Subcode indicating that we couldn't verify data supposed to be signed by the button's certificate.
     */
    public static final int FAILURE_GENUINE_CHECK_FAILED_SUBCODE_INVALID_CALCULATED_SIGNATURE = 3;

    /**
     * Failure indicating that for an already paired button, verification of the button failed.
     */
    public static final int FAILURE_QUICK_VERIFY_SIGNATURE_MISMATCH = 13;

    /**
     * Failure indicating that a packet was not signed correctly.
     */
    public static final int FAILURE_PACKET_SIGNATURE_MISMATCH = 14;

    /**
     * Failure indicating that too many apps on this device are already communicating with the button.
     */
    public static final int FAILURE_TOO_MANY_APPS_CONNECTED = 15;

    /**
     * Failure indicating that the GATT DB didn't match the Flic 2 specification.
     *
     * <p>This might indicate a buggy Android Bluetooth stack.</p>
     */
    public static final int FAILURE_SERVICE_DISCOVERY_UNEXPECTED_GATT_DB = 16;

    /**
     * Failure indicating that Android couldn't initiate a connection attempt.
     *
     * <p>This usually happens when too many Bluetooth devices are connected to the device or due to some internal Android bug.
     * Subcode might be set to Android's status code.</p>
     */
    public static final int FAILURE_GATT_CONNECT_ANDROID_ERROR = 17;

    /**
     * Failure indicating that the button failed the full verify process and sent an unknown result code.
     *
     * <p>The raw unknown result code is included as subcode.</p>
     */
    public static final int FAILURE_FULL_VERIFY_FAILED_WITH_UNKNOWN_RESULT_CODE = 18;

    /**
     * Failure that can only happen during scan when the button is not in pairable mode.
     *
     * <p>This usually happens when the button was initially held down 6 seconds and had entered pairable mode,
     * but time has passed so that when we initiated the pairing attempt, the button had already exited pairable mode.</p>
     */
    public static final int FAILURE_BUTTON_NOT_IN_PAIRABLE_MODE = 50;


    /**
     * Connection event handler.
     *
     * <p>Called after a connection from the Flic button to the device has
     * completed. When ready, {@link #onReady(Flic2Button, long)} will be called.</p>
     *
     * @param button The {@link Flic2Button} that fired the event.
     */
    public void onConnect(Flic2Button button) {
    }

    /**
     * Connection on ready event handler.
     *
     * <p>Called after a connection from the Flic button to the device has
     * completed and the button is verified and ready to use.</p>
     *
     * <p>The timestamp is mainly used to calculate how old queued events are, that will be sent shortly after this callback.</p>
     *
     * @param button The {@link Flic2Button} that fired the event.
     * @param timestamp Timestamp in milliseconds since the button booted when the button became ready.
     */
    public void onReady(Flic2Button button, long timestamp) {
    }

    /**
     * Disconnection event handler.
     *
     * <p>Called after the Flic button has disconnected for any reason.
     * Unless {@link Flic2Button#disconnectOrAbortPendingConnection()} has been called,
     * a new connection attempt will automatically be made.</p>
     *
     * @param button The {@link Flic2Button} that fired the event.
     */
    public void onDisconnect(Flic2Button button) {
    }

    /**
     * Button unpaired handler.
     *
     * <p>Called when our pairing does not exist anymore in the button,
     * for example due to factory reset. The user has to scan and pair the button again.
     * The button is now also removed from the manager.</p>
     *
     * @param button The {@link Flic2Button} that fired the event.
     */
    public void onUnpaired(Flic2Button button) {
    }

    /**
     * Failure handler.
     *
     * <p>Called when a the connection has been aborted. See {@link Flic2ButtonListener} for a list of error codes.</p>
     *
     * <p>A new attempt will be made again at some later point in time automatically, unless you call {@link Flic2Button#disconnectOrAbortPendingConnection()}.</p>
     *
     * @param button The {@link Flic2Button} that fired the event.
     * @param errorCode the error code
     * @param subCode the sub code
     */
    public void onFailure(Flic2Button button, int errorCode, int subCode) {
    }

    /**
     * Name updated handler.
     *
     * <p>Called when the name has been updated by another app.</p>
     *
     * @param button The {@link Flic2Button} that fired the event.
     * @param newName The new name.
     */
    public void onNameUpdated(Flic2Button button, String newName) {
    }

    /**
     * Firmware version was updated.
     *
     * <p>Called when the button has reported another version than was knew before.</p>
     *
     * @param button The {@link Flic2Button} that fired the event.
     * @param newVersion The new version.
     */
    public void onFirmwareVersionUpdated(Flic2Button button, int newVersion) {
    }

    /**
     * Battery level was updated.
     *
     * <p>Usually called shortly after {@link #onReady(Flic2Button, long)} and then every third hour,
     * as long as the button stays connected.</p>
     *
     * @param level A non-null {@link BatteryLevel} object
     */
    public void onBatteryLevelUpdated(BatteryLevel level) {
    }

    /**
     * Button up/down handler.
     *
     * <p>Called after the button has been pressed or released.</p>
     *
     * <p>One of isUp and isDown will be true, the other will be false.</p>
     *
     * @param button The {@link Flic2Button} that fired the event.
     * @param wasQueued true if the event happened before the connection was established
     * @param lastQueued true if this is the last queued event
     * @param timestamp when the event occurred in milliseconds, relative to button boot
     * @param isUp   true if it was released
     * @param isDown true if it was pressed
     */
    public void onButtonUpOrDown(Flic2Button button, boolean wasQueued, boolean lastQueued, long timestamp, boolean isUp, boolean isDown) {
    }

    /**
     * Button click/hold handler.
     *
     * <p>Used to distinguish between click and hold.</p>
     *
     * <p>Click will be fired when the button is released if it was pressed for maximum 1 second.
     * Otherwise, hold will be fired 1 second after the button was pressed. Click will then not be fired upon release.</p>
     *
     * <p>One of isClick and isHold will be true, the other will be false.</p>
     *
     * @param button  The {@link Flic2Button} that fired the event.
     * @param wasQueued true if the event happened before the connection was established
     * @param lastQueued true if this is the last queued event
     * @param timestamp when the event occurred in milliseconds, relative to button boot
     * @param isClick true if it was clicked
     * @param isHold  true if it was held
     */
    public void onButtonClickOrHold(Flic2Button button, boolean wasQueued, boolean lastQueued, long timestamp, boolean isClick, boolean isHold) {
    }

    /**
     * Button single/double click handler.
     *
     * <p>Used to distinguish between single and double click.</p>
     *
     * <p>Double click will be fired if the time between two button down events was at most 0.5 seconds.
     * The double click event will then be fired upon button release.
     * If the time was more than 0.5 seconds, a single click event will be fired;
     * either directly upon button release if the button was down for more than 0.5 seconds,
     * or after 0.5 seconds if the button was down for less than 0.5 seconds.</p>
     *
     * <p>One of isSingleClick and isDoubleClick will be true, the other will be false.</p>
     *
     * <p>Note: Three fast consecutive clicks means one double click and then one single click.
     * Four fast consecutive clicks means two double clicks.</p>
     *
     * @param button        The {@link Flic2Button} that fired the event.
     * @param wasQueued true if the event happened before the connection was established
     * @param lastQueued true if this is the last queued event
     * @param timestamp when the event occurred in milliseconds, relative to button boot
     * @param isSingleClick true if it was a single click
     * @param isDoubleClick true if it was a double click
     */
    public void onButtonSingleOrDoubleClick(Flic2Button button, boolean wasQueued, boolean lastQueued, long timestamp, boolean isSingleClick, boolean isDoubleClick) {
    }

    /**
     * Button single/double click handler.
     *
     * <p>Used to distinguish between single click, double click and hold.</p>
     *
     * <p>If the time between the first button down and button up event was more than 1 second,
     * a hold event will be fired.</p>
     *
     * <p>Else, double click will be fired if the time between two button down events was at most 0.5 seconds.
     * The double click event will then be fired upon button release.
     * If the time was more than 0.5 seconds, a single click event will be fired;
     * either directly upon button release if the button was down for more than 0.5 seconds,
     * or after 0.5 seconds if the button was down for less than 0.5 seconds.</p>
     *
     * <p>One of isHold, isSingleClick and isDoubleClick will be true, the other ones will be false.</p>
     *
     * <p>Note: Three fast consecutive clicks means one double click and then one single click.
     * Four fast consecutive clicks means two double clicks.</p>
     *
     * @param button        The {@link Flic2Button} that fired the event.
     * @param wasQueued true if the event happened before the connection was established
     * @param lastQueued true if this is the last queued event
     * @param timestamp when the event occurred in milliseconds, relative to button boot
     * @param isSingleClick true if it was a single click
     * @param isDoubleClick true if it was a double click
     * @param isHold        true if it was a hold
     */
    public void onButtonSingleOrDoubleClickOrHold(Flic2Button button, boolean wasQueued, boolean lastQueued, long timestamp, boolean isSingleClick, boolean isDoubleClick, boolean isHold) {
    }

}
