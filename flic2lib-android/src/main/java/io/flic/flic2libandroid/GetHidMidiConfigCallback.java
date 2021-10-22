package io.flic.flic2libandroid;

public interface GetHidMidiConfigCallback {
    int NOT_READY = -1;
    int ALREADY_IN_PROGRESS = -2;
    int INCOMPATIBLE_FIRMWARE_VERSION = -3;

    int OK = 0;
    int SOMEONE_ALREADY_WRITING = 1;
    int INTERRUPTED_BY_WRITER = 2;

    /**
     * Result callback.
     *
     * @param result Result code
     * @param data   The data blob, if a config exists ({@code null} otherwise)
     */
    void onResult(int result, byte[] data);
}
