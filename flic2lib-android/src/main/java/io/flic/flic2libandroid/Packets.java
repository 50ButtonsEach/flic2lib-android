package io.flic.flic2libandroid;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

abstract class TxPacket {
    public static final int FULL_VERIFY_REQUEST_1 = 0;
    public static final int FULL_VERIFY_REQUEST_2_WITH_APP_TOKEN = 1;
    public static final int FULL_VERIFY_REQUEST_2_WITHOUT_APP_TOKEN = 2;
    public static final int FULL_VERIFY_ABORT_IND = 3;
    public static final int TEST_IF_REALLY_UNPAIRED_REQUEST = 4;
    public static final int QUICK_VERIFY_REQUEST = 5;
    public static final int FORCE_BT_DISCONNECT_IND = 6;
    public static final int BLE_SECURITY_REQUEST_IND = 7;
    public static final int GET_FIRMWARE_VERSION_REQUEST = 8;

    public static final int DISCONNECT_VERIFIED_LINK_IND = 9;
    public static final int SET_NAME_REQUEST = 10;
    public static final int GET_NAME_REQUEST = 11;
    public static final int SET_CONNECTION_PARAMETERS_IND = 12;
    public static final int START_API_TIMER_IND = 13;
    public static final int PING_RESPONSE = 14;
    public static final int INIT_BUTTON_EVENTS_REQUEST = 15;
    public static final int ACK_BUTTON_EVENTS = 16;
    public static final int START_FIRMWARE_UPDATE_REQUEST = 17;
    public static final int FIRMWARE_UPDATE_DATA_IND = 18;
    public static final int SET_AUTO_DISCONNECT_TIME_IND = 19;
    public static final int GET_BATTERY_LEVEL_REQUEST = 20;

    public static final int INIT_BUTTON_EVENTS_LIGHT_REQUEST = 23;
    public static final int SET_ADV_PARAMETERS_REQUEST = 27;


    protected abstract void write(Writer w);

    protected static class Writer {
        private byte[] buf;
        private int pos;
        private int bitpos;
        public Writer() {
            buf = new byte[256];
            pos = 1;
        }
        public void opcode(int opcode) {
            buf[0] = (byte)opcode;
        }
        public void bool(boolean b) {
            buf[pos++] = b ? (byte)1 : (byte)0;
        }
        public void b(int b) {
            buf[pos++] = (byte)b;
        }
        public void bits(long value, int width) {
            value &= (1L << width) - 1;
            while (width > 8) {
                bits(value, 8);
                value >>>= 8;
                width -= 8;
            }
            if (width == 0) {
                return;
            }
            if (bitpos == 0) {
                buf[pos++] = (byte)value;
                bitpos += width;
            } else {
                buf[pos - 1] |= value << bitpos;
                if (bitpos + width > 8) {
                    buf[pos++] = (byte)(value >>> (8 - bitpos));
                    bitpos = bitpos + width - 8;
                } else {
                    bitpos += width;
                }
            }
            if (bitpos == 8) {
                bitpos = 0;
            }
        }
        public void bitsPadding(int width) {
            bits(0, width);
        }
        public void bitBool(boolean b) {
            bits(b ? 1 : 0, 1);
        }
        public void s(int s) {
            buf[pos++] = (byte)s;
            buf[pos++] = (byte)(s >> 8);
        }
        public void i(int i) {
            buf[pos++] = (byte)i;
            buf[pos++] = (byte)(i >> 8);
            buf[pos++] = (byte)(i >> 16);
            buf[pos++] = (byte)(i >> 24);
        }
        public void l(long l) {
            i((int)l);
            i((int)(l >> 32));
        }
        public void ba(byte[] ba) {
            System.arraycopy(ba, 0, buf, pos, ba.length);
            pos += ba.length;
        }
        public void str(String str) {
            ba(str.getBytes(StandardCharsets.UTF_8));
        }
        public byte[] toBytes() {
            return Arrays.copyOf(buf, pos);
        }
    }

    public byte[] getBytes() {
        Writer w = new Writer();
        write(w);
        return w.toBytes();
    }

    static class FullVerifyRequest1 extends TxPacket {
        int tmpId;

        FullVerifyRequest1(int tmpId) {
            this.tmpId = tmpId;
        }

        @Override
        protected void write(Writer w) {
            w.opcode(FULL_VERIFY_REQUEST_1);
            w.i(tmpId);
        }
    }

    static class FullVerifyRequest2Base extends TxPacket {
        byte[] ecdhPublicKey;
        byte[] randomBytes;
        int signatureVariant;
        int encryptionVariant;
        boolean mustValidateAppToken;
        byte[] verifier;

        @Override
        protected void write(Writer w) {
            w.ba(ecdhPublicKey);
            w.ba(randomBytes);
            w.bits(signatureVariant, 3);
            w.bits(encryptionVariant, 3);
            w.bitBool(mustValidateAppToken);
            w.bitsPadding(1);
        }
    }

    static class FullVerifyRequest2WithoutAppToken extends FullVerifyRequest2Base {
        @Override
        protected void write(Writer w) {
            w.opcode(FULL_VERIFY_REQUEST_2_WITHOUT_APP_TOKEN);
            super.write(w);
            w.ba(verifier);
        }
    }

    static class FullVerifyRequest2WithAppToken extends FullVerifyRequest2Base {
        byte[] encryptedAppToken;

        @Override
        protected void write(Writer w) {
            w.opcode(FULL_VERIFY_REQUEST_2_WITH_APP_TOKEN);
            super.write(w);
            w.ba(encryptedAppToken);
            w.ba(verifier);
        }
    }

    static class QuickVerifyRequest extends TxPacket {
        byte[] random; // 7 bytes
        int signatureVariant;
        int encryptionVariant;
        int tmpId;
        int pairingId;

        @Override
        protected void write(Writer w) {
            w.opcode(QUICK_VERIFY_REQUEST);
            w.ba(random);
            w.bits(signatureVariant, 3);
            w.bits(encryptionVariant, 3);
            w.bitsPadding(2);
            w.i(tmpId);
            w.i(pairingId);
        }
    }

    static class TestIfReallyUnpairedRequest extends TxPacket {
        byte[] ecdhPublicKey;
        byte[] randomBytes;
        int pairingId;
        byte[] pairingToken;

        @Override
        protected void write(Writer w) {
            w.opcode(TEST_IF_REALLY_UNPAIRED_REQUEST);
            w.ba(ecdhPublicKey);
            w.ba(randomBytes);
            w.i(pairingId);
            w.ba(pairingToken);
        }
    }

    static class InitButtonEventsLightRequest extends TxPacket {
        int eventCount;
        int bootId;
        int autoDisconnectTime;
        int maxQueuedPackets;
        int maxQueuedPacketsAge;

        @Override
        protected void write(Writer w) {
            w.opcode(INIT_BUTTON_EVENTS_LIGHT_REQUEST);
            w.i(eventCount);
            w.i(bootId);
            w.bits(autoDisconnectTime, 9);
            w.bits(maxQueuedPackets, 5);
            w.bits(maxQueuedPacketsAge, 20);
            w.bitsPadding(6);
        }
    }

    static class AckButtonEvents extends TxPacket {
        int eventCount;

        public AckButtonEvents(int eventCount) {
            this.eventCount = eventCount;
        }

        @Override
        protected void write(Writer w) {
            w.opcode(ACK_BUTTON_EVENTS);
            w.i(eventCount);
        }
    }

    static class SetConnectionParametersInd extends TxPacket {
        short intvMin;
        short intvMax;
        short latency;
        short timeout;

        @Override
        protected void write(Writer w) {
            w.opcode(SET_CONNECTION_PARAMETERS_IND);
            w.s(intvMin);
            w.s(intvMax);
            w.s(latency);
            w.s(timeout);
        }
    }

    static class GetFirmwareVersionRequest extends TxPacket {
        @Override
        protected void write(Writer w) {
            w.opcode(GET_FIRMWARE_VERSION_REQUEST);
        }
    }

    static class DisconnectVerifiedLinkInd extends TxPacket {
        @Override
        protected void write(Writer w) {
            w.opcode(DISCONNECT_VERIFIED_LINK_IND);
        }
    }

    static class SetNameRequest extends TxPacket {
        long timestampUtcMs;
        boolean forceUpdate;
        String name;

        SetNameRequest(long timestampUtcMs, boolean forceUpdate, String name) {
            this.timestampUtcMs = timestampUtcMs;
            this.forceUpdate = forceUpdate;
            this.name = name;
        }

        @Override
        protected void write(Writer w) {
            w.opcode(SET_NAME_REQUEST);
            w.bits(timestampUtcMs, 47);
            w.bits(forceUpdate ? 1 : 0, 1);
            w.str(name);
        }
    }

    static class PingResponse extends TxPacket {
        @Override
        protected void write(Writer w) {
            w.opcode(PING_RESPONSE);
        }
    }

    static class StartFirmwareUpdateRequest extends TxPacket {
        int len;
        byte[] iv;
        int statusInterval;

        public StartFirmwareUpdateRequest(int len, byte[] iv, int statusInterval) {
            this.len = len;
            this.iv = iv;
            this.statusInterval = statusInterval;
        }

        @Override
        protected void write(Writer w) {
            w.opcode(START_FIRMWARE_UPDATE_REQUEST);
            w.s(len);
            w.ba(iv);
            w.s(statusInterval);
        }
    }

    static class FirmwareUpdateDataInd extends TxPacket {
        byte[] chunk;

        public FirmwareUpdateDataInd(byte[] chunk) {
            this.chunk = chunk;
        }

        @Override
        protected void write(Writer w) {
            w.opcode(FIRMWARE_UPDATE_DATA_IND);
            w.ba(chunk);
        }
    }

    static class SetAutoDisconnectTimeInd extends TxPacket {
        int autoDisconnectTime;

        public SetAutoDisconnectTimeInd(int autoDisconnectTime) {
            this.autoDisconnectTime = autoDisconnectTime;
        }

        @Override
        protected void write(Writer w) {
            w.opcode(SET_AUTO_DISCONNECT_TIME_IND);
            w.bits(autoDisconnectTime, 9);
            w.bitsPadding(7);
        }
    }

    static class GetBatteryLevelRequest extends TxPacket {
        @Override
        protected void write(Writer w) {
            w.opcode(GET_BATTERY_LEVEL_REQUEST);
        }
    }

    static class ForceBtDisconnectInd extends TxPacket {
        boolean restartAdv;

        public ForceBtDisconnectInd(boolean restartAdv) {
            this.restartAdv = restartAdv;
        }

        @Override
        protected void write(Writer w) {
            w.opcode(FORCE_BT_DISCONNECT_IND);
            w.bool(restartAdv);
        }
    }

    static class SetAdvParametersRequest extends TxPacket {
        boolean isActive;
        boolean removeOtherPairingsAdvSettings;
        boolean withShortRange;
        boolean withLongRange;
        short advInterval0;
        short advInterval1;
        int timeoutSeconds;

        @Override
        protected void write(Writer w) {
            w.opcode(SET_ADV_PARAMETERS_REQUEST);
            w.bool(isActive);
            w.bool(removeOtherPairingsAdvSettings);
            w.bool(withShortRange);
            w.bool(withLongRange);
            w.s(advInterval0);
            w.s(advInterval1);
            w.i(timeoutSeconds);
        }
    }
}

abstract class RxPacket {
    public static final int FULL_VERIFY_RESPONSE_1 = 0;
    public static final int FULL_VERIFY_RESPONSE_2 = 1;
    public static final int NO_LOGICAL_CONNECTION_SLOTS = 2;
    public static final int FULL_VERIFY_FAIL_RESPONSE = 3;
    public static final int TEST_IF_REALLY_UNPAIRED_RESPONSE = 4;
    public static final int GET_FIRMWARE_VERSION_RESPONSE = 5;
    public static final int QUICK_VERIFY_NEGATIVE_RESPONSE = 6;
    public static final int PAIRING_FINISHED_IND = 7;

    public static final int QUICK_VERIFY_RESPONSE = 8;
    public static final int DISCONNECT_VERIFIED_LINK = 9;

    public static final int INIT_BUTTON_EVENTS_RESPONSE_WITH_BOOT_ID = 10;
    public static final int INIT_BUTTON_EVENTS_RESPONSE_WITHOUT_BOOT_ID = 11;
    public static final int BUTTON_NOTIFICATION = 12;
    public static final int API_TIMER_NOTIFICATION = 13;
    public static final int NAME_UPDATED_NOTIFICATION = 14;
    public static final int PING_REQUEST = 15;
    public static final int GET_NAME_RESPONSE = 16;
    public static final int SET_NAME_RESPONSE = 17;
    public static final int START_FIRMWARE_UPDATE_RESPONSE = 18;
    public static final int FIRMWARE_UPDATE_NOTIFICATION = 19;
    public static final int GET_BATTERY_LEVEL_RESPONSE = 20;

    public static final int SET_ADV_PARAMETERS_RESPONSE = 25;

    static class UnexpectedEndOfPacketException extends Exception {

    }

    protected static class Reader {
        private byte[] buf;
        private int pos;
        private int bitpos;
        Reader(byte[] arr) {
            buf = arr;
        }
        int left() throws UnexpectedEndOfPacketException {
            int left = buf.length - pos;
            if (left < 0) {
                throw new UnexpectedEndOfPacketException();
            }
            return left;
        }
        boolean bool() throws UnexpectedEndOfPacketException {
            if (pos >= buf.length) {
                throw new UnexpectedEndOfPacketException();
            }
            return buf[pos++] != 0;
        }
        int b() throws UnexpectedEndOfPacketException {
            if (pos >= buf.length) {
                throw new UnexpectedEndOfPacketException();
            }
            return buf[pos++] & 0xff;
        }
        long bits(int width) throws UnexpectedEndOfPacketException {
            if (width > 8) {
                return bits(8) | (bits(width - 8) << 8);
            }
            if (bitpos == 0) {
                if (pos >= buf.length) {
                    throw new UnexpectedEndOfPacketException();
                }
                int val = (buf[pos++] & 0xff) & ((1 << width) - 1);
                if (width != 8) {
                    bitpos = width;
                }
                return val;
            } else {
                int val = ((buf[pos - 1] & 0xff) >> bitpos) & ((1 << width) - 1);
                if (bitpos + width > 8) {
                    if (pos >= buf.length) {
                        throw new UnexpectedEndOfPacketException();
                    }
                    val |= ((buf[pos++] & 0xff) << (8 - bitpos)) & ((1 << width) - 1);
                    bitpos -= 8;
                }
                bitpos += width;
                if (bitpos == 8) {
                    bitpos = 0;
                }
                return val;
            }
        }
        void bitsPadding(int width) throws UnexpectedEndOfPacketException {
            bits(width);
        }
        boolean bitBool() throws UnexpectedEndOfPacketException {
            return bits(1) != 0;
        }
        int s() throws UnexpectedEndOfPacketException {
            if (pos + 2 > buf.length) {
                throw new UnexpectedEndOfPacketException();
            }
            return (buf[pos++] & 0xff) | ((buf[pos++] & 0xff) << 8);
        }
        int i() throws UnexpectedEndOfPacketException {
            if (pos + 4 > buf.length) {
                throw new UnexpectedEndOfPacketException();
            }
            return (buf[pos++] & 0xff) | ((buf[pos++] & 0xff) << 8) | ((buf[pos++] & 0xff) << 16) | ((buf[pos++] & 0xff) << 24);
        }
        long l() throws UnexpectedEndOfPacketException {
            return (i() & 0xffffffffL) | ((long)i() << 32);
        }
        byte[] ba(int len) throws UnexpectedEndOfPacketException {
            if (pos + len > buf.length) {
                throw new UnexpectedEndOfPacketException();
            }
            byte[] ret = Arrays.copyOfRange(buf, pos, pos + len);
            pos += len;
            return ret;
        }
        String str(int len) throws UnexpectedEndOfPacketException {
            return new String(ba(len), StandardCharsets.UTF_8);
        }
        void skip(int len) throws UnexpectedEndOfPacketException {
            if (pos + len > buf.length) {
                throw new UnexpectedEndOfPacketException();
            }
            pos += len;
        }
    }

    protected Reader r;

    protected RxPacket(byte[] arr) {
        r = new Reader(arr);
    }

    static class FullVerifyResponse1 extends RxPacket {
        int tmpId;
        byte[] signature;
        byte[] bdAddr;
        boolean bdAddrType;
        byte[] publicKey;
        byte[] random;

        FullVerifyResponse1(byte[] arr) throws UnexpectedEndOfPacketException {
            super(arr);
            tmpId = r.i();
            signature = r.ba(64);
            bdAddr = r.ba(6);
            bdAddrType = r.bool();
            publicKey = r.ba(32);
            random = r.ba(8);
            r = null;
        }
    }

    static class FullVerifyResponse2 extends RxPacket {
        boolean appCredentialsMatch;
        boolean caresAboutAppCredentials;
        byte[] buttonUuid;
        String name;
        int firmwareVersion;
        int batteryLevel;
        String serialNumber;

        FullVerifyResponse2(byte[] arr) throws UnexpectedEndOfPacketException {
            super(arr);
            appCredentialsMatch = r.bitBool();
            caresAboutAppCredentials = r.bitBool();
            r.bitsPadding(6);
            buttonUuid = r.ba(16);
            int nameLen = r.b();
            if (nameLen < 0 || nameLen > 23) {
                nameLen = 0;
            }
            name = nameLen == 0 ? "" : r.str(nameLen);
            r.skip(23 - nameLen);
            firmwareVersion = r.i();
            batteryLevel = r.s();
            serialNumber = r.str(11);
        }
    }

    static class FullVerifyFailResponse extends RxPacket {
        static final int INVALID_VERIFIER = 0;
        static final int NOT_IN_PUBLIC_MODE = 1;

        int reason;

        protected FullVerifyFailResponse(byte[] arr) throws UnexpectedEndOfPacketException {
            super(arr);
            reason = r.b();
        }
    }

    static class TestIfReallyUnpairedResponse extends RxPacket {
        byte[] result;

        TestIfReallyUnpairedResponse(byte[] arr) throws UnexpectedEndOfPacketException {
            super(arr);
            result = r.ba(16);
        }
    }

    static class NoLogicalConnectionSlots extends RxPacket {
        int[] tmpIds;

        NoLogicalConnectionSlots(byte[] arr) throws UnexpectedEndOfPacketException {
            super(arr);
            tmpIds = new int[r.left() / 4];
            for (int i = 0; i < tmpIds.length; i++) {
                tmpIds[i] = r.i();
            }
        }
    }

    static class QuickVerifyResponse extends RxPacket {
        byte[] random;
        int tmpId;

        QuickVerifyResponse(byte[] arr) throws UnexpectedEndOfPacketException {
            super(arr);
            random = r.ba(8);
            tmpId = r.i();
        }
    }

    static class QuickVerifyNegativeResponse extends RxPacket {
        int tmpId;

        QuickVerifyNegativeResponse(byte[] arr) throws UnexpectedEndOfPacketException {
            super(arr);
            tmpId = r.i();
        }
    }

    static class InitButtonEventsResponse extends RxPacket {
        boolean hasQueuedEvents;
        long timestamp;
        int eventCount;
        int bootId;

        InitButtonEventsResponse(byte[] arr) throws UnexpectedEndOfPacketException {
            super(arr);
            hasQueuedEvents = r.bitBool();
            timestamp = r.bits(47) * 1000 / 32768;
            eventCount = r.i();
            bootId = r.i();
        }
    }

    static class NameUpdatedNotification extends RxPacket {
        String name;

        NameUpdatedNotification(byte[] arr) throws UnexpectedEndOfPacketException {
            super(arr);
            name = r.str(r.left());
        }
    }

    static class GetSetNameResponse extends RxPacket {
        long timestampUtcMs;
        String name;

        GetSetNameResponse(byte[] arr) throws UnexpectedEndOfPacketException {
            super(arr);
            timestampUtcMs = r.bits(48);
            name = r.str(r.left());
        }
    }

    static class ButtonEventNotificationItem {
        long timestamp;
        int eventEncoded;
        boolean wasQueued;
        boolean wasQueuedLast;
        int eventCount; // Not in original packet
    }

    static class ButtonEventNotification extends RxPacket {
        int eventCounter;
        ButtonEventNotificationItem[] items;

        ButtonEventNotification(byte[] arr) throws UnexpectedEndOfPacketException {
            super(arr);
            eventCounter = r.i();
            items = new ButtonEventNotificationItem[r.left() / 7];
            if (items.length == 0) {
                throw new UnexpectedEndOfPacketException();
            }
            for (int i = 0; i < items.length; i++) {
                ButtonEventNotificationItem item = new ButtonEventNotificationItem();
                item.timestamp = r.bits(48) * 1000 / 32768;
                item.eventEncoded = (int)r.bits(4);
                item.wasQueued = r.bitBool();
                item.wasQueuedLast = r.bitBool();
                r.bitsPadding(2);
                items[i] = item;
            }
        }
    }

    static class GetFirmwareVersionResponse extends RxPacket {
        int version;

        GetFirmwareVersionResponse(byte[] arr) throws UnexpectedEndOfPacketException {
            super(arr);
            version = r.i();
        }
    }

    static class StartFirmwareUpdateResponse extends RxPacket {
        int startPos;

        StartFirmwareUpdateResponse(byte[] arr) throws UnexpectedEndOfPacketException {
            super(arr);
            startPos = r.i();
        }
    }

    static class FirmwareUpdateNotification extends RxPacket {
        int pos;

        FirmwareUpdateNotification(byte[] arr) throws UnexpectedEndOfPacketException {
            super(arr);
            pos = r.i();
        }
    }

    static class GetBatteryLevelResponse extends RxPacket {
        int level;

        GetBatteryLevelResponse(byte[] arr) throws UnexpectedEndOfPacketException {
            super(arr);
            level = r.s();
        }
    }
}


