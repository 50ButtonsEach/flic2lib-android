package io.flic.flic2libandroid;

interface SessionCallback {
    void tx(byte[] data);
    void bond();
    void restart(int afterMs);
    void pairingComplete();
    void unpaired();
}
