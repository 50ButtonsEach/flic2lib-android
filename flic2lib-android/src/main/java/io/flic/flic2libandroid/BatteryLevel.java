package io.flic.flic2libandroid;

/**
 * Battery level.
 *
 * @see Flic2Button#getLastKnownBatteryLevel()
 * @see Flic2ButtonListener#onBatteryLevelUpdated(BatteryLevel)
 */
public class BatteryLevel {
    private float voltage;
    private long timestampUtcMs;

    BatteryLevel(float voltage, long timestampUtcMs) {
        this.voltage = voltage;
        this.timestampUtcMs = timestampUtcMs;
    }

    /**
     * Gets the battery voltage.
     *
     * @return The voltage in Volt
     */
    public float getVoltage() {
        return voltage;
    }

    /**
     * Gets the battery level, in estimated percentage.
     *
     * <p>This uses the voltage from {@link #getVoltage()}
     * and estimates a percentage left.</p>
     *
     * <p>The estimated percentage is derived from the voltage, but is a very rough estimate.
     * The interval between 50 and 100 especially is very inaccurate.</p>
     *
     * @return A number between 0 and 100
     */
    public int getEstimatedPercentage() {
        return batteryVoltageToEstimatedPercentage(voltage);
    }

    /**
     * Gets the timestamp of the battery level.
     *
     * <p>This method returns the Unix timestamp in milliseconds when the
     * battery level was measured.</p>
     *
     * @return The timestamp
     */
    public long getTimestampUtcMs() {
        return timestampUtcMs;
    }

    private static Integer batteryVoltageToEstimatedPercentage(float voltage) {
        int mvolt = (int)(voltage * 1000);
        int percentage;
        if (mvolt >= 3000) {
            percentage = 100;
        } else if (mvolt >= 2900) {
            percentage = 42 + (mvolt - 2900) * 58 / 100;
        } else if (mvolt >= 2740) {
            percentage = 18 + (mvolt - 2740) * 24 / 160;
        } else if (mvolt >= 2440) {
            percentage = 6 + (mvolt - 2440) * 12 / 300;
        } else if (mvolt >= 2100) {
            percentage = (mvolt - 2100) * 6 / 340;
        } else {
            percentage = 0;
        }
        return percentage;
    }
}
