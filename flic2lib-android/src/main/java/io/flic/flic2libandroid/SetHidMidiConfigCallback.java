package io.flic.flic2libandroid;

public interface SetHidMidiConfigCallback {
    int NOT_READY = -1;
    int ALREADY_IN_PROGRESS = -2;
    int INCOMPATIBLE_FIRMWARE_VERSION = -3;

    int OK = 0;
    int DATA_TOO_LONG = 1;
    int INVALID_CONFIG = 2;
    int NOT_STARTED = 3;
    int OTHER_CONN_WRITING = 4;
    int BUSY_FLASHING = 5;

    void onResult(int result);
}
