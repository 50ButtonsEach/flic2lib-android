package io.flic.flic2libandroid;

public class BuzzerNote {
    private final float hz;
    private final int duration;

    /**
     * Initializes a buzzer note.
     *
     * @param hz The note pitch in hz. 0 indicates silence.
     * @param duration The note duration in ms, max 65535.
     */
    public BuzzerNote(float hz, int duration) {
        if (duration > 65535) {
            throw new IllegalArgumentException("duration is out of range (must be at most 65535)");
        }
        this.hz = hz;
        this.duration = duration;
    }

    public float getHz() {
        return hz;
    }

    public int getDuration() {
        return duration;
    }
}
