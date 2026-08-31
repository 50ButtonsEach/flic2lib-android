package io.flic.flic2libandroid;

import java.util.List;

public class FallDetectionEvent {
    public static final int STATE_TRIGGERED = 0;
    public static final int STATE_PRE_FALL_DATA_COLLECTED = 1;
    public static final int STATE_COMPLETED = 2;

    private final int state;
    private final int preFallSampleRate;
    private final int preFallExpectedSampleCount;
    private final List<AccelerometerDataPoint> preFallAccelerometerData;
    private final int postFallSampleRate;
    private final int postFallExpectedSampleCount;
    private final List<AccelerometerDataPoint> postFallAccelerometerData;

    FallDetectionEvent(int state, int preFallSampleRate, int preFallExpectedSampleCount, List<AccelerometerDataPoint> preFallAccelerometerData, int postFallSampleRate, int postFallExpectedSampleCount, List<AccelerometerDataPoint> postFallAccelerometerData) {
        this.state = state;
        this.preFallSampleRate = preFallSampleRate;
        this.preFallExpectedSampleCount = preFallExpectedSampleCount;
        this.preFallAccelerometerData = preFallAccelerometerData;
        this.postFallSampleRate = postFallSampleRate;
        this.postFallExpectedSampleCount = postFallExpectedSampleCount;
        this.postFallAccelerometerData = postFallAccelerometerData;
    }

    /**
     * State of the fall detection event.
     *
     * <p>See {@link Flic2ButtonListener#onFallDetectionUpdated(FallDetectionEvent)} for more information.</p>
     *
     * @return One of {@link #STATE_TRIGGERED}, {@link #STATE_PRE_FALL_DATA_COLLECTED} and {@link #STATE_COMPLETED}.
     */
    public int getState() {
        return state;
    }

    /**
     * Pre-fall sample rate.
     *
     * @return The sample rate in Hz for the pre-fall accelerometer data.
     */
    public int getPreFallSampleRate() {
        return preFallSampleRate;
    }

    /**
     * Pre-fall expected sample count.
     *
     * @return The number of expected sample count in the pre-fall accelerometer data list, once the {@link #STATE_PRE_FALL_DATA_COLLECTED} has been emitted.
     */
    public int getPreFallExpectedSampleCount() {
        return preFallExpectedSampleCount;
    }

    /**
     * Pre-fall accelerometer data.
     *
     * @return A list of data points using the sample rate {@link #getPreFallSampleRate()}.
     */
    public List<AccelerometerDataPoint> getPreFallAccelerometerData() {
        return preFallAccelerometerData;
    }

    /**
     * Post-fall sample rate.
     *
     * @return A sample rate in Hz for the post-fall accelerometer data.
     */
    public int getPostFallSampleRate() {
        return postFallSampleRate;
    }

    /**
     * Post-fall expected sample count.
     *
     * @return The number of expected sample count in the post-fall accelerometer data list, once the {@link #STATE_COMPLETED} has been emitted.
     */
    public int getPostFallExpectedSampleCount() {
        return postFallExpectedSampleCount;
    }

    /**
     * Post-fall accelerometer data.
     *
     * @return A list of data points using the sample rate {@link #getPostFallSampleRate()}.
     */
    public List<AccelerometerDataPoint> getPostFallAccelerometerData() {
        return postFallAccelerometerData;
    }
}
