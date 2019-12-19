package io.flic.flic2libandroid;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

class Flic2Database extends SQLiteOpenHelper {
    private static final int DB_VERSION = 2;
    private static final String DB_NAME = "flic2_database";

    public Flic2Database(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onConfigure(SQLiteDatabase db) {
        setWriteAheadLoggingEnabled(true);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        onUpgrade(db, 0, DB_VERSION);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 1) {
            db.execSQL("CREATE TABLE buttons (" +
                    "bd_addr TEXT NOT NULL, " +
                    "address_type INTEGER NOT NULL, " +
                    "uuid TEXT UNIQUE NOT NULL, " +
                    "serial_number TEXT NOT NULL, " +
                    "name TEXT, " +
                    "name_timestamp_utc_ms INTEGER NOT NULL DEFAULT 0, " +
                    "pairing_data BLOB NOT NULL, " +
                    "boot_id INTEGER, " +
                    "event_counter INTEGER NOT NULL DEFAULT 0, " +
                    "firmware_version INTEGER NOT NULL DEFAULT 0, " +
                    "next_firmware_check_timestamp INTEGER NOT NULL DEFAULT 0, " +
                    "adv_settings_configured INTEGER NOT NULL DEFAULT 0, " +
                    "UNIQUE(bd_addr, address_type))");
        }
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE buttons ADD COLUMN last_known_battery_voltage REAL");
            db.execSQL("ALTER TABLE buttons ADD COLUMN last_known_battery_timestamp_utc_ms INTEGER");
        }
    }

    public void addButton(Flic2Button button) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("bd_addr", button.bdAddr);
        values.put("address_type", button.addressType ? 1 : 0);
        values.put("uuid", button.uuid);
        values.put("serial_number", button.serialNumber);
        values.put("name", button.name);
        values.put("name_timestamp_utc_ms", button.nameTimestampUtcMs);
        values.put("firmware_version", button.firmwareVersion);
        values.put("next_firmware_check_timestamp", button.nextFirmwareCheckTimestamp);
        values.put("pairing_data", Utils.concatArrays(Utils.intToBytes(button.pairingData.identifier), button.pairingData.key));
        if (button.eventCount != 0) {
            values.put("boot_id", button.bootId);
            values.put("event_counter", button.eventCount);
        }
        values.put("last_known_battery_voltage", button.lastKnownBatteryVoltage);
        values.put("last_known_battery_timestamp_utc_ms", button.lastKnownBatteryTimestampUtcMs);
        db.replace("buttons", null, values);
    }

    public void deleteButton(Flic2Button button) {
        getWritableDatabase().delete("buttons", "bd_addr = ? AND address_type = ?", new String[]{button.bdAddr, button.addressType ? "1" : "0"});
    }

    private void update(Flic2Button button, ContentValues values) {
        getWritableDatabase().update("buttons", values, "bd_addr = ? AND address_type = ?", new String[]{button.bdAddr, button.addressType ? "1" : "0"});
    }

    public void updateBootIdAndEventCounter(Flic2Button button) {
        ContentValues values = new ContentValues();
        values.put("boot_id", button.bootId);
        values.put("event_counter", button.eventCount);
        values.put("adv_settings_configured", button.advSettingsConfigured ? 1 : 0);
        values.put("last_known_battery_voltage", button.lastKnownBatteryVoltage);
        values.put("last_known_battery_timestamp_utc_ms", button.lastKnownBatteryTimestampUtcMs);

        update(button, values);
    }

    public void updateEventCounter(Flic2Button button) {
        ContentValues values = new ContentValues();
        values.put("event_counter", button.eventCount);

        update(button, values);
    }

    public void updateAdvSettingsConfigured(Flic2Button button) {
        ContentValues values = new ContentValues();
        values.put("adv_settings_configured", button.advSettingsConfigured ? 1 : 0);

        update(button, values);
    }

    public void updateFirmwareVersion(Flic2Button button) {
        ContentValues values = new ContentValues();
        values.put("firmware_version", button.firmwareVersion);

        update(button, values);
    }

    public void updateFirmwareCheckTimestamp(Flic2Button button) {
        ContentValues values = new ContentValues();
        values.put("next_firmware_check_timestamp", button.nextFirmwareCheckTimestamp);

        update(button, values);
    }

    public void updateName(Flic2Button button) {
        ContentValues values = new ContentValues();
        values.put("name", button.name);
        values.put("name_timestamp_utc_ms", button.nameTimestampUtcMs);

        update(button, values);
    }

    public void updateBatteryLevel(Flic2Button button) {
        ContentValues values = new ContentValues();
        values.put("last_known_battery_voltage", button.lastKnownBatteryVoltage);
        values.put("last_known_battery_timestamp_utc_ms", button.lastKnownBatteryTimestampUtcMs);

        update(button, values);
    }

    public List<Flic2Button> getButtons(Flic2Manager manager) {
        SQLiteDatabase db = getWritableDatabase();
        Cursor query = db.query("buttons", null, null, null, null, null, null);
        ArrayList<Flic2Button> buttons = new ArrayList<>();
        query.moveToFirst();
        if (!query.isAfterLast()) {
            do {
                Flic2Button button = new Flic2Button(manager, query.getString(query.getColumnIndex("bd_addr")));
                button.addressType = query.getInt(query.getColumnIndex("address_type")) != 0;
                button.uuid = query.getString(query.getColumnIndex("uuid"));
                button.serialNumber = query.getString(query.getColumnIndex("serial_number"));
                button.name = query.getString(query.getColumnIndex("name"));
                button.nameTimestampUtcMs = query.getLong(query.getColumnIndex("name_timestamp_utc_ms"));
                byte[] pairingData = query.getBlob(query.getColumnIndex("pairing_data"));
                button.pairingData = new Flic2Button.PairingData(Utils.bytesToInt(pairingData), Arrays.copyOfRange(pairingData, 4, 20));
                button.firmwareVersion = query.getInt(query.getColumnIndex("firmware_version"));
                button.nextFirmwareCheckTimestamp = query.getLong(query.getColumnIndex("next_firmware_check_timestamp"));
                button.bootId = query.isNull(query.getColumnIndex("boot_id")) ? 0 : query.getInt(query.getColumnIndex("boot_id"));
                button.eventCount = query.getInt(query.getColumnIndex("event_counter"));
                button.advSettingsConfigured = query.getInt(query.getColumnIndex("adv_settings_configured")) != 0;
                button.lastKnownBatteryVoltage = query.isNull(query.getColumnIndex("last_known_battery_voltage")) ? null : query.getFloat(query.getColumnIndex("last_known_battery_voltage"));
                button.lastKnownBatteryTimestampUtcMs = query.isNull(query.getColumnIndex("last_known_battery_timestamp_utc_ms")) ? null : query.getLong(query.getColumnIndex("last_known_battery_timestamp_utc_ms"));
                buttons.add(button);
            } while (query.moveToNext());
        }
        query.close();
        return buttons;
    }
}
