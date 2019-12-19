package io.flic.flic2libandroid;

import android.content.Context;
import android.content.pm.PackageManager;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

class Utils {
    static SecureRandom secureRandom = new SecureRandom();

    static MessageDigest createSha256() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    static Mac createHmacSha256(byte[] key) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key, "HmacSHA256"));
            return mac;
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException(e);
        }
    }

    static byte[] concatArrays(byte[] a, byte[] b, int offset) {
        byte[] arr = new byte[a.length + b.length - offset];
        System.arraycopy(a, 0, arr, 0, a.length);
        System.arraycopy(b, offset, arr, a.length, b.length - offset);
        return arr;
    }

    static byte[] concatArrays(byte[] a, byte[] b) {
        return concatArrays(a, b, 0);
    }

    private static final char[] hexArray = "0123456789ABCDEF".toCharArray();
    public static String bytesToHex(byte[] bytes) {
        if (bytes == null) {
            return null;
        }
        char[] hexChars = new char[bytes.length * 2];
        for (int i = 0; i < bytes.length; i++) {
            int v = bytes[i] & 0xff;
            hexChars[i << 1] = hexArray[v >>> 4];
            hexChars[(i << 1) + 1] = hexArray[v & 0x0f];
        }
        return new String(hexChars);
    }

    static String bdAddrBytesToString(byte[] bytes) {
        String str = bytesToHex(bytes);
        return str.substring(10, 12) + ':' + str.substring(8, 10) + ':' + str.substring(6, 8) +
                ':' + str.substring(4, 6) + ':' + str.substring(2, 4) + ':' + str.substring(0, 2);
    }

    static int bytesToInt(byte[] buf) {
        int pos = 0;
        return (buf[pos++] & 0xff) | ((buf[pos++] & 0xff) << 8) | ((buf[pos++] & 0xff) << 16) | ((buf[pos++] & 0xff) << 24);
    }

    static byte[] intToBytes(int i) {
        return new byte[] {(byte)i, (byte)(i >> 8), (byte)(i >> 16), (byte)(i >> 24)};
    }

    private static byte[] downloadFirmware(String urlString) throws IOException {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(urlString);
            conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(60 * 1000);
            conn.setReadTimeout(60 * 1000);
            if (conn.getResponseCode() != 200) {
                throw new IOException("Not OK status");
            }
            InputStream inputStream = conn.getInputStream();
            byte[] buf = new byte[256 * 1024];
            int pos = 0;
            for (;;) {
                int nread = inputStream.read(buf, pos, buf.length - pos);
                if (nread == -1) {
                    break;
                }
                pos += nread;
                if (pos == buf.length) {
                    throw new IOException("Firmware too large");
                }
            }
            return Arrays.copyOf(buf, pos);
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    static Pair<byte[], Integer> firmwareCheck(Context context, String uuid, int currentVersion) {
        String packageName = context.getPackageName();
        String packageVersion;
        try {
            packageVersion = context.getPackageManager().getPackageInfo(packageName, 0).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            packageVersion = null;
        }
        JSONObject reqObj = new JSONObject();
        try {
            reqObj.put("uuid", uuid);
            reqObj.put("current_version", currentVersion);
            reqObj.put("platform", "Android");
            reqObj.put("package_name", packageName);
            reqObj.put("package_version", packageVersion);
            reqObj.put("lib_version", BuildConfig.VERSION_NAME);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
        String postData = reqObj.toString();

        URL url;
        try {
            url = new URL("https://api.flic.io/api/v1/buttons/versions/firmware2");
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }

        String firmwareDownloadUrl;
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setConnectTimeout(60 * 1000);
            conn.setReadTimeout(60 * 1000);
            try (OutputStream os = conn.getOutputStream()) {
                os.write(postData.getBytes());
            }
            int responseCode = conn.getResponseCode();
            if (responseCode != 200) {
                return new Pair<>(null, 60);
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                StringBuilder sb = new StringBuilder();
                int ch;
                while ((ch = reader.read()) != -1) {
                    sb.append((char) ch);
                }
                String response = sb.toString();
                if (response.equals("null")) {
                    // Already latest version
                    return new Pair<>(null, 24 * 60);
                }
                try {
                    JSONObject obj = new JSONObject(response);
                    //String firmwareVersion = obj.getString("firmware_version");
                    firmwareDownloadUrl = obj.getString("firmware_download_url");
                } catch (JSONException e) {
                    return new Pair<>(null, 24 * 60);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            return new Pair<>(null, 120);
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }

        try {
            return new Pair<>(downloadFirmware(firmwareDownloadUrl), 20);
        } catch (IOException e) {
            e.printStackTrace();
            return new Pair<>(null, 20);
        }
    }

    static class Pair<A, B> {
        A a;
        B b;

        public Pair(A a, B b) {
            this.a = a;
            this.b = b;
        }
    }
}
