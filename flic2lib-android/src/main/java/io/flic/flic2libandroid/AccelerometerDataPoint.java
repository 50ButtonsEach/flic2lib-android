package io.flic.flic2libandroid;

import java.util.Objects;

/**
 * Accelerometer data point.
 *
 * <p>See {@link Flic2Button#enableAccelerometerStreaming(AccelerometerStreamingConfig, EnableAccelerometerStreamingCallback)} and
 * {@link Flic2Button#enableFallDetection(FallDetectionConfig, boolean, EnableFallDetectionCallback)}.</p>
 */
public class AccelerometerDataPoint {
    private final short x, y, z;
    private final byte scale;

    AccelerometerDataPoint(short x, short y, short z, byte scale) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.scale = scale;
    }

    /**
     * X coordinate.
     *
     * @return The x coordinate in units of 1 g.
     */
    public float getX() {
        return x / (16393.44f / (1 << scale));
    }

    /**
     * Y coordinate.
     *
     * @return The y coordinate in units of 1 g.
     */
    public float getY() {
        return y / (16393.44f / (1 << scale));
    }

    /**
     * Z coordinate.
     *
     * @return The z coordinate in units of 1 g.
     */
    public float getZ() {
        return z / (16393.44f / (1 << scale));
    }

    @Override
    public String toString() {
        return "AccelerometerDataPoint{" +
                "x=" + getX() +
                ", y=" + getY() +
                ", z=" + getZ() +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        AccelerometerDataPoint that = (AccelerometerDataPoint) o;
        return getX() == that.getX() && getY() == that.getY() && getZ() == that.getZ();
    }

    @Override
    public int hashCode() {
        return Objects.hash(getX(), getY(), getZ());
    }
}
