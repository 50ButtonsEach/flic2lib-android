package io.flic.flic2libandroid;

public class FallDetectionConfig {
    public static final int FULL_SCALE_SELECTION_2G = 0;
    public static final int FULL_SCALE_SELECTION_4G = 1;
    public static final int FULL_SCALE_SELECTION_8G = 2;
    public static final int FULL_SCALE_SELECTION_16G = 3;

    final int lowGThresholdMg;
    final int lowGDurationMs;
    final int highGTimeoutMs;
    final int highGThresholdMg;
    final int highGTimeWindowMs;
    final int postEventRecordDurationMs;
    final int fullScaleSelection;

    /**
     * Fall detection config.
     *
     * <p>For use with {@link Flic2Button#enableFallDetection(FallDetectionConfig, boolean, EnableFallDetectionCallback)}.</p>
     *
     * <p>See the Fall Detection documentation how these parameters should typically be configured.</p>
     *
     * @param lowGThresholdMg The acceleration magnitude threshold, in mg, used to enter the low-G state.
     *                        The average acceleration magnitude must remain below this threshold for at least {@code lowGDurationMs} before the device starts listening for an impact.
     * @param lowGDurationMs The minimum duration, in milliseconds, that the average acceleration magnitude must remain below {@code lowGThresholdMg} to enter the low-G state.
     * @param highGTimeoutMs The maximum time, in milliseconds, allowed between entering the low-G state and detecting the high-G impact event.
     *                       This allows a short transition window between the falling phase and the impact, so falls can still be detected when the impact is delayed or less abrupt.
     *                       Increasing this value can also increase the probability of false positives.
     *                       If no matching high-G event is detected within this timeout, the fall detection sequence is not considered a fall.
     * @param highGThresholdMg The acceleration magnitude threshold, in mg, used to detect the impact after the low-G state has been entered.
     * @param highGTimeWindowMs The high-G smoothing window, in milliseconds, used when evaluating the impact threshold.
     *                          The acceleration magnitude must remain at or above {@code highGThresholdMg} for this window, which filters out short spikes while still accepting impact-like thuds.
     * @param postEventRecordDurationMs The duration, in milliseconds, that accelerometer samples are recorded after a fall has been detected.
     *                                  When a high-G event is detected within {@code highGTimeoutMs}, the device sends a fall detection triggered event and continues recording for this duration before streaming the post-event data to the host.
     * @param fullScaleSelection The accelerometer full-scale range selection used while fall detection is active.
     *                           Valid values: {@link #FULL_SCALE_SELECTION_2G}, {@link #FULL_SCALE_SELECTION_4G}, {@link #FULL_SCALE_SELECTION_8G}, {@link #FULL_SCALE_SELECTION_16G}.
     */
    public FallDetectionConfig(int lowGThresholdMg, int lowGDurationMs, int highGTimeoutMs, int highGThresholdMg, int highGTimeWindowMs, int postEventRecordDurationMs, int fullScaleSelection) {
        this.lowGThresholdMg = Math.min(lowGThresholdMg, 65535);
        this.lowGDurationMs = Math.min(lowGDurationMs, 65535);
        this.highGTimeoutMs = Math.min(highGTimeoutMs, 65535);
        this.highGThresholdMg = Math.min(highGThresholdMg, 65535);
        this.highGTimeWindowMs = Math.min(highGTimeWindowMs, 65535);
        this.postEventRecordDurationMs = Math.min(postEventRecordDurationMs, 65535);
        this.fullScaleSelection = Math.min(Math.max(fullScaleSelection, 0), 3);
    }

    public int getLowGThresholdMg() {
        return lowGThresholdMg;
    }

    public int getLowGDurationMs() {
        return lowGDurationMs;
    }

    public int getHighGTimeoutMs() {
        return highGTimeoutMs;
    }

    public int getHighGThresholdMg() {
        return highGThresholdMg;
    }

    public int getHighGTimeWindowMs() {
        return highGTimeWindowMs;
    }

    public int getPostEventRecordDurationMs() {
        return postEventRecordDurationMs;
    }

    public int getFullScaleSelection() {
        return fullScaleSelection;
    }
}
