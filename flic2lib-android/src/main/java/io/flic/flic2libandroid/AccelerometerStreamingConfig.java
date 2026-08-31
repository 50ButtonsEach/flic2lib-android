package io.flic.flic2libandroid;

/**
 * Accelerometer streaming config.
 *
 * <p>See {@link Flic2Button#enableAccelerometerStreaming(AccelerometerStreamingConfig, EnableAccelerometerStreamingCallback)}.</p>
 */
public class AccelerometerStreamingConfig {
    public static final int FULL_SCALE_SELECTION_2G = 0;
    public static final int FULL_SCALE_SELECTION_4G = 1;
    public static final int FULL_SCALE_SELECTION_8G = 2;
    public static final int FULL_SCALE_SELECTION_16G = 3;

    final int lowPowerMode;
    final int mode;
    final int outputDataRate;
    final int bandwidthFilter;
    final int fullScaleSelection;
    final int filterDatatypeSelection;
    final boolean lowNoise;
    final boolean highPassRefMode;
    final int samplesPerBurst;
    final boolean onlyWhilePressed;

    /**
     * Accelerometer streaming config.
     *
     * <p>For use with {@link Flic2Button#enableAccelerometerStreaming(AccelerometerStreamingConfig, EnableAccelerometerStreamingCallback)}.</p>
     *
     * @param lowPowerMode Valid values: 0-3. See LIS2DW12 datasheet, table 31.
     * @param mode Valid values: 0-2. See LIS2DW12 datasheet, table 30.
     * @param outputDataRate Valid values: 1-9. See LIS2DW12 datasheet, table 29.
     * @param bandwidthFilter Valid values: 0-3. See LIS2DW12 datasheet, table 41.
     * @param fullScaleSelection Valid values: {@link #FULL_SCALE_SELECTION_2G}, {@link #FULL_SCALE_SELECTION_4G}, {@link #FULL_SCALE_SELECTION_8G}, {@link #FULL_SCALE_SELECTION_16G}.
     * @param filterDatatypeSelection 0: low-pass filter path selected, 1: high-pass filter path selected.
     * @param lowNoise Whether low-noise configuration is enabled.
     * @param highPassRefMode High-pass filter reference mode enabled.
     * @param samplesPerBurst Valid values: 1-32. Number of samples the accelerometer transfers per burst to the main chip of the Flic device. A higher value increases the power efficiency, but increases the latency.
     * @param onlyWhilePressed Whether {@link Flic2ButtonListener#onAccelerometerStreamingData(AccelerometerDataPoint[])} events should be emitted only while button is pressed.
     */
    public AccelerometerStreamingConfig(int lowPowerMode, int mode, int outputDataRate, int bandwidthFilter, int fullScaleSelection, int filterDatatypeSelection, boolean lowNoise, boolean highPassRefMode, int samplesPerBurst, boolean onlyWhilePressed) {
        this.lowPowerMode = lowPowerMode;
        this.mode = mode;
        this.outputDataRate = outputDataRate;
        this.bandwidthFilter = bandwidthFilter;
        this.fullScaleSelection = fullScaleSelection;
        this.filterDatatypeSelection = filterDatatypeSelection;
        this.lowNoise = lowNoise;
        this.highPassRefMode = highPassRefMode;
        this.samplesPerBurst = samplesPerBurst;
        this.onlyWhilePressed = onlyWhilePressed;
    }

    public int getLowPowerMode() {
        return lowPowerMode;
    }

    public int getMode() {
        return mode;
    }

    public int getOutputDataRate() {
        return outputDataRate;
    }

    public int getBandwidthFilter() {
        return bandwidthFilter;
    }

    public int getFullScaleSelection() {
        return fullScaleSelection;
    }

    public int getFilterDatatypeSelection() {
        return filterDatatypeSelection;
    }

    public boolean getLowNoise() {
        return lowNoise;
    }

    public boolean getHighPassRefMode() {
        return highPassRefMode;
    }

    public int getSamplesPerBurst() {
        return samplesPerBurst;
    }

    public boolean getOnlyWhilePressed() {
        return onlyWhilePressed;
    }
}
