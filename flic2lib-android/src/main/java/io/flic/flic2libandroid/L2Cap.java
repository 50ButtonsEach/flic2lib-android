package io.flic.flic2libandroid;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Build;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

class L2CapUtils {
    private static volatile boolean TESTED_TYPE;
    private static final int TYPE_L2CAP_LE = 4;

    private static Field getField(Class<?> klass, String name) throws NoSuchFieldException {
        Field f = klass.getDeclaredField(name);
        f.setAccessible(true);
        return f;
    }

    @TargetApi(Build.VERSION_CODES.P)
    public static BluetoothSocket getSocket(BluetoothDevice device) throws IOException {
        int psm = 0xfc;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return device.createInsecureL2capChannel(psm);
        }

        try {
            Method createInsecureRfcommSocket = BluetoothDevice.class.getDeclaredMethod("createInsecureRfcommSocket", int.class);
            BluetoothSocket socket = (BluetoothSocket) createInsecureRfcommSocket.invoke(device, 7); // May throw IOException wrapped in an InvocationTargetException if Bluetooth if turned off

            Field mPortField = getField(BluetoothSocket.class, "mPort");
            mPortField.set(socket, psm);

            Class<?> unsafeClass = Class.forName("sun.misc.Unsafe");
            Field f2 = getField(unsafeClass, "theUnsafe");
            Object theUnsafe = f2.get(null);

            if (!TESTED_TYPE) {
                Method createScoSocket = BluetoothDevice.class.getDeclaredMethod("createScoSocket");
                BluetoothSocket scoSocket = (BluetoothSocket) createScoSocket.invoke(device); // May throw IOException wrapped in an InvocationTargetException if Bluetooth if turned off
                int typeSco = (int) unsafeClass.getMethod("getInt", Object.class, long.class).invoke(theUnsafe, scoSocket, 72L);
                int typeRfcomm = (int) unsafeClass.getMethod("getInt", Object.class, long.class).invoke(theUnsafe, socket, 72L);
                if (typeSco == BluetoothSocket.TYPE_SCO && typeRfcomm == BluetoothSocket.TYPE_RFCOMM) {
                    // We are then confident we found the right field offset
                    TESTED_TYPE = true;
                } else {
                    return null;
                }
            }

            unsafeClass.getMethod("putInt", Object.class, long.class, int.class).invoke(theUnsafe, socket, 72L, TYPE_L2CAP_LE);
            return socket;
        } catch (InvocationTargetException e) {
            if (e.getCause() instanceof IOException) {
                throw (IOException)e.getCause(); // Bluetooth is off
            } else {
                return null;
            }
        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | NoSuchFieldException e) {
            return null;
        }
    }
}
