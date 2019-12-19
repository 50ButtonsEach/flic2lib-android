package io.flic.flic2libandroid;
import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanSettings;
import android.os.Build;
import android.os.ParcelUuid;
import android.util.Log;
import android.util.SparseArray;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.UUID;

abstract class ScanWrapper {
    private static final String TAG = "ScanWrapper";

    public abstract void startScan(BluetoothAdapter adapter, UUID uuid, Callback cb);
    public abstract void stopScan(BluetoothAdapter adapter, Callback cb);

    public static ScanWrapper INSTANCE = Build.VERSION.SDK_INT >= 21 ? new LollipopScanWrapper() : new PreLollipopScanWrapper();

    public static abstract class Callback {
        private Object mAndroidCb;

        public abstract void onScanResult(int callbackType, final ScanResult result);
        public abstract void onScanFailed(final int errorCode);
    }

    public static class ScanResult {
        private BluetoothDevice mDevice;
        private SparseArray<byte[]> mManufacturerSpecificData;
        private byte[] mBytes;

        public BluetoothDevice getDevice() {
            return mDevice;
        }

        public byte[] getManufacturerSpecificData(int manufacturerId) {
            return mManufacturerSpecificData.get(manufacturerId);
        }

        public byte[] getBytes() {
            return mBytes;
        }
    }

    private static class PreLollipopScanWrapper extends ScanWrapper {
        public void startScan(BluetoothAdapter adapter, final UUID uuid, final Callback cb) {
            if (cb.mAndroidCb == null) {
                cb.mAndroidCb = new BluetoothAdapter.LeScanCallback() {
                    @Override
                    public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
                        ScanResult result = new ScanResult();
                        result.mDevice = device;
                        result.mManufacturerSpecificData = new SparseArray<>();
                        result.mBytes = scanRecord;
                        boolean foundUuid = false;
                        for (int offset = 0; offset < scanRecord.length - 1;) {
                            int len = scanRecord[offset++] & 0xff;
                            if (len > scanRecord.length - offset) {
                                break;
                            }
                            if (len > 0) {
                                int next = offset + len;
                                --len;
                                switch (scanRecord[offset++] & 0xff) {
                                    case 0x02: // Partial list of 16-bit uuids
                                    case 0x03: // Complete list of 16-bit uuids
                                    {
                                        for (int pos = offset; pos + 1 < next; pos += 2) {
                                            int uuid16 = (scanRecord[pos] & 0xff) | ((scanRecord[pos + 1] & 0xff) << 8);
                                            if (uuid.equals(UUID.fromString(String.format("%08x-0000-1000-8000-00805f9b34fb", uuid16)))) {
                                                foundUuid = true;
                                            }
                                        }
                                        break;
                                    }
                                    case 0x06: // Incomplete list of 128-bit uuids
                                    case 0x07: // Complete list of 128-bit uuids
                                    {
                                        for (int pos = offset; pos + 15 < next; pos += 16) {
                                            if (uuid.equals(UUID.fromString(String.format("%02x%02x%02x%02x-%02x%02x-%02x%02x-%02x%02x-%02x%02x%02x%02x%02x%02x",
                                                    scanRecord[pos+15],
                                                    scanRecord[pos+14],
                                                    scanRecord[pos+13],
                                                    scanRecord[pos+12],
                                                    scanRecord[pos+11],
                                                    scanRecord[pos+10],
                                                    scanRecord[pos+9],
                                                    scanRecord[pos+8],
                                                    scanRecord[pos+7],
                                                    scanRecord[pos+6],
                                                    scanRecord[pos+5],
                                                    scanRecord[pos+4],
                                                    scanRecord[pos+3],
                                                    scanRecord[pos+2],
                                                    scanRecord[pos+1],
                                                    scanRecord[pos])))) {
                                                foundUuid = true;
                                            }
                                        }
                                        break;
                                    }
                                    case 0xff: // Manufacturer specific data
                                        if (len >= 2) {
                                            int manufacturerId = (scanRecord[offset] & 0xff) | ((scanRecord[offset + 1] & 0xff) << 8);
                                            byte[] data = Arrays.copyOfRange(scanRecord, offset + 2, offset + len);
                                            result.mManufacturerSpecificData.put(manufacturerId, data);
                                        }
                                }
                                offset += len;
                            }
                        }

                        if (foundUuid) {
                            cb.onScanResult(1, result); // CALLBACK_TYPE_ALL_MATCHES
                        }
                    }
                };
            }
            adapter.startLeScan((BluetoothAdapter.LeScanCallback)cb.mAndroidCb);
        }

        public void stopScan(BluetoothAdapter adapter, Callback cb) {
            if (cb.mAndroidCb != null) {
                adapter.stopLeScan((BluetoothAdapter.LeScanCallback)cb.mAndroidCb);
            }
        }
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private static class LollipopScanWrapper extends ScanWrapper {
        BluetoothLeScanner scanner;

        private void initScanner(BluetoothAdapter adapter) {
            if (scanner == null) {
                scanner = adapter.getBluetoothLeScanner();
            }
        }

        public void startScan(BluetoothAdapter adapter, UUID uuid, final Callback cb) {
            if (cb.mAndroidCb == null) {
                cb.mAndroidCb = new ScanCallback() {
                    @Override
                    public void onScanResult(int callbackType, android.bluetooth.le.ScanResult result) {
                        ScanResult res = new ScanResult();
                        ScanRecord record = result.getScanRecord();
                        if (record != null) {
                            res.mDevice = result.getDevice();
                            res.mManufacturerSpecificData = record.getManufacturerSpecificData();
                            res.mBytes = record.getBytes();
                            cb.onScanResult(callbackType, res);
                        }
                    }

                    @Override
                    public void onScanFailed(int errorCode) {
                        cb.onScanFailed(errorCode);
                    }
                };
            }
            ArrayList<ScanFilter> filters = new ArrayList<>();
            filters.add(new ScanFilter.Builder().setServiceUuid(ParcelUuid.fromString(uuid.toString())).build());
            initScanner(adapter);
            try {
                if (scanner == null) {
                    throw new IllegalStateException("BT Adapter is not turned ON");
                }
                scanner.startScan(filters, new ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build(), (ScanCallback) cb.mAndroidCb);
            } catch (IllegalStateException e) {
                // BT Adapter is not turned ON
                Log.e(TAG, "BT Adapter is not turned ON");
            }
        }

        @Override
        public void stopScan(BluetoothAdapter adapter, Callback cb) {
            try {
                if (cb.mAndroidCb != null) {
                    initScanner(adapter);
                    if (scanner == null) {
                        throw new IllegalStateException("BT Adapter is not turned ON");
                    }
                    scanner.stopScan((ScanCallback) cb.mAndroidCb);
                }
            } catch (IllegalStateException e) {
                // BT Adapter is not turned ON
                Log.e(TAG, "BT Adapter is not turned ON");
            }
        }
    }
}
