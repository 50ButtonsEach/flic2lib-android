![Flic Logo Black](https://user-images.githubusercontent.com/2717016/70526105-1bbaa200-1b49-11ea-9aa0-49e7959300c3.png)

[![javadoc](https://javadoc.io/badge2/io.flic/flic2lib-android/javadoc.svg)](https://javadoc.io/doc/io.flic/flic2lib-android)

# Flic2 lib for Android

The official library for Flic2 on Android.

The library is hosted at JitPack and can be included in your Android app by entering the following in your `build.gradle` file:

    dependencies {
        implementation 'com.github.50ButtonsEach:flic2lib-android:1.+'
    }

If you have not already done so, include the JitPack repository in your root `build.gradle` file:

    allprojects {
        repositories {
            ...
            maven { url 'https://jitpack.io' }
        }
    }



## License

See [License.txt](License.txt).

# API documentation

See [![javadoc](https://javadoc.io/badge2/io.flic/flic2lib-android/javadoc.svg)](https://javadoc.io/doc/io.flic/flic2lib-android).

# Tutorial

First you need to add the library to your project, by following the instructions above.

All imported classes are in the package `io.flic.flic2libandroid`. There are two classes of particular interest, `Flic2Manager` and `Flic2Button`. The singleton class `Flic2Manager` keeps track of all the buttons and `Flic2Button` represents a single button.

The manager must first be initialized with the application's `Context` as well as a `Handler`, that defines which thread the library will run on, including all callbacks. To do that, it's recommended to initialize the manager in the `Application` class of your app. If you don't already have one, make a subclass of [Application](https://developer.android.com/reference/android/app/Application) and specify it in your Android manifest:

```java
public class Flic2SampleApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        // Initialize the Flic2 manager to run on the same thread as the current thread (the main thread)
        Flic2Manager manager = Flic2Manager.initAndGetInstance(getApplicationContext(), new Handler());

        // use manager later on
    }
}
```

```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    package="flic.io.flic2androidsample">
    <application
        android:name=".Flic2SampleApplication"
        ...>

        ...
    </application>
</manifest>
```

Once the manager is initialized, it can always be retrieved again using `Flic2Manager.getInstance()`.

## Scanning for new buttons

To be able to scan for new buttons, the method `startScan` of the manager is used. It will automatically scan, connect and pair a button. The progress is delivered using callbacks. If everything succeeds, you will be given a new `Flic2Button` object.

```java
Flic2Manager.getInstance().startScan(new Flic2ScanCallback() {
    @Override
    public void onDiscoveredAlreadyPairedButton(Flic2Button button) {
        // Found an already paired button. Try another button.
    }

    @Override
    public void onDiscovered(String bdAddr) {
        // Found Flic2, now connecting...
    }

    @Override
    public void onConnected() {
        // Connected. Now pairing...
    }

    @Override
    public void onComplete(int result, int subCode, Flic2Button button) {
        if (result == Flic2ScanCallback.RESULT_SUCCESS) {
            // Success!
            // The button object can now be used
        } else {
            // Failed
        }
    }
});
```

If the scan times out or otherwise fails for some reason, `onComplete` will be called with an error code as result, defined in `Flic2ScanCallback` or `Flic2ButtonListener`.

Before scanning however, we need to acquire some runtime permission by asking the user. This is due to a requirement of the Android platform in order to scan for/connect to Bluetooth Low Energy devices.
If targeting and running on Android 12 or higher, `Manifest.permission.BLUETOOTH_SCAN` and `Manifest.permission.BLUETOOTH_CONNECT` are required. Otherwise `Manifest.permission.ACCESS_FINE_LOCATION` is required.
In your activity, use the following code:

```java
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
    if (Build.VERSION.SDK_INT < 31 || getApplicationInfo().targetSdkVersion < 31) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            return;
        }
    } else {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED)
        {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT}, 1);
            return;
        }
    }
}
// if this line is reached, the permission was already granted and we can now start scan
```

The `requestPermissions` call will open a popup where the user must press Allow. Android will then call the activity's `onRequestPermissionsResult` method, so we need to implement that:

```java
@Override
public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    if (requestCode == 1) {
        if (Build.VERSION.SDK_INT < 31 || getApplicationInfo().targetSdkVersion < 31) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // now startScan can be safely called
            } else {
                Toast.makeText(getApplicationContext(), "Scanning needs Location permission, which you have rejected", Toast.LENGTH_SHORT).show();
            }
        } else {
            if (grantResults.length >= 2 && grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                // now startScan can be safely called
            } else {
                Toast.makeText(getApplicationContext(), "Scanning needs permissions for finding nearby devices, which you have rejected", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
```

If the permissions are not granted when calling `startScan` on the manager, an `SecurityException` will be thrown.

## Interacting with buttons
Once we have a `Flic2Button` object, we can start listen to events. Events are delivered to a `Flic2ButtonListener` object that you should extend and override the desired methods. Each button object can have multiple listener objects and they are added and removed by the `addListener` and `removeListener` methods, respectively.

The `Flic2ButtonListener` can both be used to detect connectivity change events as well as different kinds of click events. For example, to show a toast when the button is pressed:

```java
button.addListener(new Flic2ButtonListener() {
    @Override
    public void onButtonUpOrDown(Flic2Button button, boolean wasQueued, boolean lastQueued, long timestamp, boolean isUp, boolean isDown) {
        if (isDown) {
            Toast.makeText(getApplicationContext(), "Button " + button + " was pressed", Toast.LENGTH_SHORT).show();
        }
    }
});
```

There are other kinds of listeners if you instead want to distinguish click, double click and hold. See the API documentation for more information.

If the button was pressed while it was disconnected, the button events will be sent when it later connects (the latest that could fit on the internal memory). Sometimes it's not desired to get old events. In that case the formula `wasQueued && button.getReadyTimestamp() - timestamp > 15000` can be used in the event callback to detect if the event was older than 15 seconds.

The button's desired connectivity state is set using the methods `connect` and `disconnectOrAbortPendingConnection`. Once `connect` is called, the library will try to make sure the button is connected whenever it is in range and Bluetooth is turned on. If the connection gets dropped for any reason, or Bluetooth is toggled off and on, the library will automatically attempt to re-establish the connection. When you want the button to stay disconnected, call `disconnectOrAbortPendingConnection`.

The button listener has the events `onConnected`, `onReady` and `onDisconnected` that are called when the connectivity state changes. The `onConnected` and `onDisconnected` callbacks represent the Bluetooth connectivity, while `onReady` is called shortly after `onConnected` when the library has completed some initial transactions, such as configuring the communication channel and verified the pairing.

The callback `onUnpaired` should also be implemented, which will be called if the button has been factory reset. From that point the button is removed and detached from the library, and the button object cannot be used anymore. To add it back, the user must re-scan the button.

To get a list of all paired buttons (usually at application startup), use the method `getButtons` on the manager object. All paired buttons are saved to disk in an internal database file, so the pairings remain even if the Android device is restarted. When the application starts, the buttons are always in the disconnected state, and you need to explicitly call `connect` on the buttons you want connected. With a newly paired button (as delivered by `onComplete`) however, you don't need to call `connect` since it is already connected at that point.

## API versioning and manifest permissions

The way Android handles Bluetooth permissions has changed in API 31 (Android 12). Previously, `ACCESS_FINE_LOCATION` (runtime permission) was required to scan new Bluetooth devices and `BLUETOOTH` and `BLUETOOTH_ADMIN` (install-time permissions) were required to pair and connect to devices. The `BLUETOOTH` and `BLUETOOTH_ADMIN` permissions are entirely removed in API 31 and have been replaced by `BLUETOOTH_CONNECT`. The `ACCESS_FINE_LOCATION` permission is not used for Bluetooth scanning anymore but is still used for GPS for example.

When targeting API 31 or higher, this library will automatically insert the required permissions into the application's manifest so that it works on both older devices as well as those running Android 12 or higher. Unfortunately, the XML structure is not expressive enough to automatically handle situations when your app has different requirements, such as targeting a lower API version than 31 or using location permissions for other situations. Therefore you might need to change the manifest as described below.

The options below need the `xmlns:tools="http://schemas.android.com/tools"` attribute added to the `<manifest>` tag in order to work.

When targeting API 30 (Android 11) or lower, the following lines must be added directly inside the `<manifest>` tag:

```xml
<uses-permission android:name="android.permission.BLUETOOTH" tools:remove="maxSdkVersion" />
<uses-permission android:name="android.permission.BLUETOOTH_ADMIN" tools:remove="maxSdkVersion" />
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" tools:remove="maxSdkVersion" />
<uses-permission android:name="android.permission.BLUETOOTH_SCAN" tools:node="remove" />
<uses-permission android:name="android.permission.BLUETOOTH_CONNECT" tools:node="remove" />
```

When targeting API 31 or higher and the app also uses Bluetooth scanning for location purposes (such as scanning for Bluetooth "beacons"), add the following line directly inside the `<manifest>` tag:

```xml
<uses-permission android:name="android.permission.BLUETOOTH_SCAN" tools:remove="android:usesPermissionFlags" />
```

When targeting API 31 or higher and the app needs the `ACCESS_FINE_LOCATION` permission for any purpose other than Bluetooth scanning, add the following line directly inside the `<manifest>` tag:

```xml
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" tools:remove="maxSdkVersion" />
```

## Upgrading to or using API 31 (Android 12)

When targeting API 31 or later, in order to connect to Bluetooth devices (such as Flic 2), the runtime permission `BLUETOOTH_CONNECT` must have been granted. If this permission is not granted, the `connect` method on a `Flic2Button` will throw a `SecurityException`. This can happen in particular when the app is updated when previously targeting API 30 or lower, or when the user revokes the app's permission in the system settings. The possibility of these situations must be taken into account when upgrading the target API for your app. One idea is to show a dialog on app startup if the `BLUETOOTH_CONNECT` permission is not granted, and ask the user to grant it at that point.

# Background execution

It is common that apps using Flic buttons want to be able to receive button events even when the app is not visible on the screen, or the screen is turned off. Usually Android kills the app process after inactivity or when it wants to reclaim memory. The official way of avoiding the process to be killed is to use a [Foreground Service](https://developer.android.com/guide/components/services#Foreground). A foreground service can be written like this:

```java
public class Flic2SampleService extends Service {
    private static final int SERVICE_NOTIFICATION_ID = 123;
    private final String NOTIFICATION_CHANNEL_ID = "Notification_Channel_Flic2SampleService";
    private final CharSequence NOTIFICATION_CHANNEL_NAME = "Flic2Sample";

    @Override
    public void onCreate() {
        super.onCreate();

        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_CANCEL_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel mChannel = new NotificationChannel(NOTIFICATION_CHANNEL_ID, NOTIFICATION_CHANNEL_NAME, NotificationManager.IMPORTANCE_LOW);
            NotificationManager notificationManager = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.createNotificationChannel(mChannel);
        }

        Notification notification = new NotificationCompat.Builder(this.getApplicationContext(), NOTIFICATION_CHANNEL_ID)
                .setContentTitle("Flic2Sample")
                .setContentText("Flic2Sample")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentIntent(contentIntent)
                .setOngoing(true)
                .build();
        startForeground(SERVICE_NOTIFICATION_ID, notification);
    }

    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not implemented");
    }
}
```

If we want the foreground service to always run as soon as the application starts, we can start it in the `onCreate` method of our `Application` class:

```java
// To prevent the application process from being killed while the app is running in the background, start a Foreground Service
ContextCompat.startForegroundService(getApplicationContext(), new Intent(getApplicationContext(), Flic2SampleService.class));
```

It also needs to be added to the manifest, inside the `<application>` section:

```xml
<service
    android:name=".Flic2SampleService"
    android:enabled="true"
    android:exported="false">
</service>
```

Also a permission needs to be added directly inside the `<manifest>` tag:

```xml
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
```

Note that the service class itself doesn't need to do anything. As long as it is running it will keep the rest of the process, including the Flic2 library, to stay alive.

Another idea would be to only run your Foreground Service when you have at least one button you want to stay connected, but requires slightly more code.

In order to automatically start the application after boot of the device, as well as when the app has been updated, we need to implement two broadcast receivers. In the service class (or anywhere else appropriate), implement the following to handlers:

```java
public static class BootUpReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        // The Application class's onCreate has already been called at this point, which is what we want
    }
}

public static class UpdateReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        // The Application class's onCreate has already been called at this point, which is what we want
    }
}
```

And register the receivers in the manifest inside the `<application>` tag:

```xml
<receiver
    android:name=".Flic2SampleService$BootUpReceiver"
    android:enabled="true"
    android:exported="false"
    android:permission="android.permission.RECEIVE_BOOT_COMPLETED">
    <intent-filter>
        <action android:name="android.intent.action.BOOT_COMPLETED" />
        <category android:name="android.intent.category.DEFAULT" />
    </intent-filter>
</receiver>

<receiver
    android:name=".Flic2SampleService$UpdateReceiver"
    android:enabled="true"
    android:exported="false">
    <intent-filter>
        <action android:name="android.intent.action.PACKAGE_REPLACED" />
        <data
            android:path="[YOUR PACKAGE NAME HERE]"
            android:scheme="package" />
        <category android:name="android.intent.category.LAUNCHER" />
    </intent-filter>
</receiver>
```

We also need to declare the permission directly inside the `<manifest>` tag:

```xml
<uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />
```

The receivers don't need to do anything, since their only intention is to start the app process. When the app process starts, the `Application` class's `onCreate` method is always called first before any other components are created.
