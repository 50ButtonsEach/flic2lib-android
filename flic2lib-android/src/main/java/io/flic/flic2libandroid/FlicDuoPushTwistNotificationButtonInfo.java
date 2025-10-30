package io.flic.flic2libandroid;

import java.util.Objects;

/**
 * Flic Duo push twist notification button info.
 *
 * <p>Emitted from {@link Flic2ButtonListener#onDuoPushTwistNotification(FlicDuoPushTwistNotificationButtonInfo[], int)}.</p>
 */
public class FlicDuoPushTwistNotificationButtonInfo {
    private final boolean pressed;
    private final boolean firstEvent;
    private final boolean pressedForAtLeastHalfASecond;

    FlicDuoPushTwistNotificationButtonInfo(boolean pressed, boolean firstEvent, boolean pressedForAtLeastHalfASecond) {
        this.pressed = pressed;
        this.firstEvent = firstEvent;
        this.pressedForAtLeastHalfASecond = pressedForAtLeastHalfASecond;
    }

    /**
     * Indicates whether this button is currently held down or not.
     *
     * @return true or false.
     */
    public boolean isPressed() {
        return pressed;
    }

    /**
     * Indicates whether this event is the first one since the button was pressed down.
     *
     * @return true or false.
     */
    public boolean isFirstEvent() {
        return firstEvent;
    }

    /**
     * Indicates whether the button has been held down for at least half a second.
     *
     * @return true or false.
     */
    public boolean isPressedForAtLeastHalfASecond() {
        return pressedForAtLeastHalfASecond;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        FlicDuoPushTwistNotificationButtonInfo that = (FlicDuoPushTwistNotificationButtonInfo) o;
        return pressed == that.pressed && firstEvent == that.firstEvent && pressedForAtLeastHalfASecond == that.pressedForAtLeastHalfASecond;
    }

    @Override
    public int hashCode() {
        return Objects.hash(pressed, firstEvent, pressedForAtLeastHalfASecond);
    }
}
