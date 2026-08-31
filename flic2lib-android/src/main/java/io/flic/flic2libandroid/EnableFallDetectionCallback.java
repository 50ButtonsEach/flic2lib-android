package io.flic.flic2libandroid;

public interface EnableFallDetectionCallback {
    int SUCCESS = 0;
    int INVALID_CONFIG = 1;
    int BUSY = 2;
    int NOT_READY = -1;
    int NOT_SUPPORTED = -2;
    int FIRMWARE_UPDATE_NEEDED = -3;

    /**
     * Result.
     *
     * <p>One of {@link #SUCCESS}, {@link #INVALID_CONFIG}, {@link #BUSY}, {@link #NOT_READY}, {@link #NOT_SUPPORTED}, {@link #FIRMWARE_UPDATE_NEEDED}. Treat any other result code as a generic error.</p>
     *
     * @param result The result
     */
    void onResult(int result);
}
