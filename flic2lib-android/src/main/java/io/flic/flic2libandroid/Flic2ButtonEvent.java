package io.flic.flic2libandroid;

import java.util.Objects;

/**
 * Flic 2 button event.
 *
 * <p>Emitted from {@link Flic2ButtonListener#onButtonEvent(Flic2Button, Flic2ButtonEvent)}.</p>
 */
public class Flic2ButtonEvent {
    /**
     * Up or down.
     *
     * <p>Triggered on every button down or release.</p>
     */
    public static final int EVENT_CLASS_UP_OR_DOWN = 0;

    /**
     * Click or hold.
     *
     * <p>Used if you want to distinguish between click and hold.</p>
     */
    public static final int EVENT_CLASS_CLICK_OR_HOLD = 1;

    /**
     * Single or double click.
     *
     * <p>Used if you want to distinguish between a single click and a double click.</p>
     */
    public static final int EVENT_CLASS_SINGLE_OR_DOUBLE_CLICK = 2;

    /**
     * Single or double click or hold.
     *
     * <p>Used if you want to distinguish between a single click, a double click and a hold.</p>
     */
    public static final int EVENT_CLASS_SINGLE_OR_DOUBLE_CLICK_OR_HOLD = 3;

    /**
     * The button was pressed.
     */
    public static final int EVENT_TYPE_UP = 0;

    /**
     * The button was released.
     */
    public static final int EVENT_TYPE_DOWN = 1;

    /**
     * The button was clicked, and was held for at most 1 seconds between press and release.
     */
    public static final int EVENT_TYPE_CLICK = 2;

    /**
     * The button was clicked once.
     */
    public static final int EVENT_TYPE_SINGLE_CLICK = 3;

    /**
     * The button was clicked twice. The time between the first and second press must be at most 0.5 seconds.
     */
    public static final int EVENT_TYPE_DOUBLE_CLICK = 4;

    /**
     * The button was held for at least 1 second.
     */
    public static final int EVENT_TYPE_HOLD = 5;

    /**
     * Button number for the only button on Flic 2 or the big button on Flic Duo.
     */
    public static final int BUTTON_NUMBER_BIG = 0;

    /**
     * Button number for the small button on Flic Duo.
     */
    public static final int BUTTON_NUMBER_SMALL = 1;

    /**
     * The Flic did not detect any gesture being performed.
     */
    public static final int GESTURE_NO_GESTURE = -1;

    /**
     * The Flic did detect that a gesture was being performed, but it was unrecognized.
     */
    public static final int GESTURE_UNRECOGNIZED_GESTURE = 0;

    /**
     * The Flic did detect a swipe left gesture.
     */
    public static final int GESTURE_LEFT = 1;

    /**
     * The Flic did detect a swipe right gesture.
     */
    public static final int GESTURE_RIGHT = 2;

    /**
     * The Flic did detect a swipe up gesture.
     */
    public static final int GESTURE_UP = 3;

    /**
     * The Flic detect a swipe down gesture.
     */
    public static final int GESTURE_DOWN = 4;

    private final byte eventClass;
    private final byte eventType;
    private final int eventCount;
    private final byte buttonNumber;
    private final boolean wasQueued;
    private final boolean lastQueued;
    private final long timestamp;
    private final byte x, y, z;
    private final byte gesture;
    private final boolean downAtLeastHalfASecond;

    Flic2ButtonEvent(int eventClass, int eventType, int eventCount, byte buttonNumber, boolean wasQueued, boolean lastQueued, long timestamp, byte x, byte y, byte z, byte gesture, boolean downAtLeastHalfASecond) {
        this.eventClass = (byte) eventClass;
        this.eventType = (byte) eventType;
        this.eventCount = eventCount;
        this.buttonNumber = buttonNumber;
        this.wasQueued = wasQueued;
        this.lastQueued = lastQueued;
        this.timestamp = timestamp;
        this.x = x;
        this.y = y;
        this.z = z;
        this.gesture = gesture;
        this.downAtLeastHalfASecond = downAtLeastHalfASecond;
    }

    Flic2ButtonEvent(int eventClass, int eventType, int eventCount, boolean wasQueued, boolean lastQueued, long timestamp) {
        this.eventClass = (byte) eventClass;
        this.eventType = (byte) eventType;
        this.eventCount = eventCount;
        this.buttonNumber = 0;
        this.wasQueued = wasQueued;
        this.lastQueued = lastQueued;
        this.timestamp = timestamp;
        this.x = 0;
        this.y = 0;
        this.z = 0;
        this.gesture = -1;
        this.downAtLeastHalfASecond = false;
    }

    /**
     * Button event class.
     *
     * <p>Each time the button is interacted with, one or more events will be sent.</p>
     *
     * <p>Usually an application only needs to listen to one event class.</p>
     *
     * <p>Since distinguishing between single and double click needs some waiting time after the first
     * click to detect if a second press will occur or not, single click events will be delayed for the
     * last two event classes but not for the first two, it is important to pick the right event class
     * for the use case.</p>
     *
     * <p>For a particular event class, only the specified button event types may be emitted.</p>
     *
     * @return The possible options are: {@link #EVENT_CLASS_UP_OR_DOWN}, {@link #EVENT_CLASS_CLICK_OR_HOLD},
     * {@link #EVENT_CLASS_SINGLE_OR_DOUBLE_CLICK} and {@link #EVENT_CLASS_SINGLE_OR_DOUBLE_CLICK_OR_HOLD}.
     */
    public int getEventClass() {
        return eventClass;
    }

    /**
     * Button event type within the specified event class.
     *
     * <p>For {@link #EVENT_CLASS_UP_OR_DOWN}, event type will be either {@link #EVENT_TYPE_UP} or {@link #EVENT_TYPE_DOWN}.</p>
     *
     * <p>For {@link #EVENT_CLASS_CLICK_OR_HOLD}, event type will be either {@link #EVENT_TYPE_CLICK} or {@link #EVENT_TYPE_HOLD}.</p>
     *
     * <p>For {@link #EVENT_CLASS_SINGLE_OR_DOUBLE_CLICK}, event type will be either {@link #EVENT_TYPE_SINGLE_CLICK} or {@link #EVENT_TYPE_DOUBLE_CLICK}.</p>
     *
     * <p>For {@link #EVENT_CLASS_SINGLE_OR_DOUBLE_CLICK_OR_HOLD}, event type will be either {@link #EVENT_TYPE_SINGLE_CLICK}, {@link #EVENT_TYPE_DOUBLE_CLICK} or {@link #EVENT_TYPE_HOLD}.</p>
     *
     * @return The event type.
     */
    public int getEventType() {
        return eventType;
    }

    /**
     * An event counter that starts at zero when the Flic boots and always increases.
     *
     * <p>This value divided by four indicates roughly how many times the button has been pressed and released.</p>
     *
     * <p>More specific, event_count mod 4 should be 1: down, 2: hold, 3: up, 0: single click timeout.</p>
     *
     * <p>Note: Flic Duo has one individual event counter per button.</p>
     */
    public int getEventCount() {
        return eventCount;
    }

    /**
     * Physical button number that was pressed.
     *
     * @return For Flic 2, always 0. For Flic Duo, the big button has number 0 and the small button has number 1. See {@link #BUTTON_NUMBER_BIG} and {@link #BUTTON_NUMBER_SMALL}.
     */
    public int getButtonNumber() {
        return buttonNumber;
    }

    /**
     * Indicates if this button event was queued, i.e. it was pressed some time ago before connection setup completed.
     *
     * @return true or false.
     */
    public boolean getWasQueued() {
        return wasQueued;
    }

    /**
     * Indicates that this button event was the last queued.
     *
     * @return true or false.
     */
    public boolean isLastQueued() {
        return lastQueued;
    }

    /**
     * Timestamp of event.
     *
     * <p>The difference between {@link Flic2Button#getReadyTimestamp()} and this value can be used to know the age of the event for a queued button event.</p>
     *
     * @return when the event occurred in milliseconds, relative to button boot.
     */
    public long getTimestamp() {
        return timestamp;
    }

    /**
     * The X axis accelerometer value.
     *
     * <p>The returned value is valid only for Flic Duo.</p>
     *
     * @return X.
     */
    public float getX() {
        return (float)(x / 64.036875);
    }

    /**
     * The Y axis accelerometer value.
     *
     * <p>The returned value is valid only for Flic Duo.</p>
     *
     * @return Y.
     */
    public float getY() {
        return (float)(y / 64.036875);
    }

    /**
     * The Z axis accelerometer value.
     *
     * <p>The returned value is valid only for Flic Duo.</p>
     *
     * @return Z.
     */
    public float getZ() {
        return (float)(z / 64.036875);
    }

    /**
     * Gesture recognition.
     *
     * <p>If multiple events emitted for the same button release, it will contain the same gesture.</p>
     *
     * <p>The returned value is valid only for Flic Duo.</p>
     *
     * @return One of {@link #GESTURE_NO_GESTURE}, {@link #GESTURE_UNRECOGNIZED_GESTURE}, {@link #GESTURE_LEFT}, {@link #GESTURE_RIGHT}, {@link #GESTURE_UP} or {@link #GESTURE_DOWN}.
     */
    public int getGesture() {
        return gesture;
    }

    /**
     * For events that are emitted as a response to a button release or hold, whether the button was pressed down at least half a second.
     *
     * <p>The returned value is valid only for Flic Duo.</p>
     *
     * @return true or false.
     */
    public boolean getDownAtLeastHalfASecond() {
        return downAtLeastHalfASecond;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Flic2ButtonEvent that = (Flic2ButtonEvent) o;
        return eventClass == that.eventClass && eventType == that.eventType && eventCount == that.eventCount && buttonNumber == that.buttonNumber && wasQueued == that.wasQueued && timestamp == that.timestamp && x == that.x && y == that.y && z == that.z && gesture == that.gesture && downAtLeastHalfASecond == that.downAtLeastHalfASecond;
    }

    @Override
    public int hashCode() {
        return Objects.hash(eventClass, eventType, eventCount, buttonNumber, wasQueued, timestamp, x, y, z, gesture, downAtLeastHalfASecond);
    }
}
