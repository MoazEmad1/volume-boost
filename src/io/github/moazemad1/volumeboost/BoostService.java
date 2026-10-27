package io.github.moazemad1.volumeboost;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ServiceInfo;
import android.media.audiofx.LoudnessEnhancer;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

/** Holds one LoudnessEnhancer on audio session 0 (the global output mix) at the saved gain. From adb, without
 *  opening the app over what's playing (only DUMP holders, such as the adb shell, may start it):
 *  adb shell am start-foreground-service -n io.github.moazemad1.volumeboost/.BoostService --ei db 6 */
public class BoostService extends Service {
    static final String TAG = "VolumeBoost";
    /** Hard cap: past +9 dB the limiter mostly squashes the sound and works the small speaker harder;
     *  +12 is here on trial (2026-10-27). */
    static final int MAX_DB = 12;
    private LoudnessEnhancer enhancer;
    /** True while the effect is on, for the app's screen. */
    static volatile boolean running;

    static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences("boost", MODE_PRIVATE);
    }

    static void apply(Context c, int db) {
        db = Math.max(0, Math.min(MAX_DB, db));
        prefs(c).edit().putInt("db", db).apply();
        Intent i = new Intent(c, BoostService.class);
        if (db == 0) c.stopService(i);
        else c.startForegroundService(i);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        NotificationManager nm = getSystemService(NotificationManager.class);
        nm.createNotificationChannel(new NotificationChannel("boost", "Volume Boost", NotificationManager.IMPORTANCE_MIN));
        if (intent != null && intent.hasExtra("db")) {
            prefs(this).edit().putInt("db", Math.max(0, Math.min(MAX_DB, intent.getIntExtra("db", 0)))).apply();
        }
        int db = prefs(this).getInt("db", 0);
        Notification n = new Notification.Builder(this, "boost")
                .setSmallIcon(android.R.drawable.ic_lock_silent_mode_off)
                .setContentTitle("Volume Boost +" + db + " dB").build();
        try {
            if (Build.VERSION.SDK_INT >= 34) startForeground(1, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
            else startForeground(1, n);
        } catch (RuntimeException e) {
            // Android 12+ refuses a foreground start from the background (a fresh install started from adb, or
            // a restart after a crash). Stop instead of crashing in a restart loop; opening the app starts it.
            Log.w(TAG, "not allowed to run in the foreground now: " + e);
            stopSelf();
            return START_NOT_STICKY;
        }
        if (db == 0) {
            stopSelf();
            return START_NOT_STICKY;
        }
        try {
            if (enhancer == null) enhancer = new LoudnessEnhancer(0);
            enhancer.setTargetGain(db * 100);   // millibels
            enhancer.setEnabled(true);
            running = enhancer.getEnabled();
            Log.i(TAG, "boost on: +" + db + " dB, enabled=" + running);
        } catch (RuntimeException e) {
            Log.w(TAG, "LoudnessEnhancer on session 0 refused: " + e);
            stopSelf();
        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        running = false;
        if (enhancer != null) {
            enhancer.setEnabled(false);
            enhancer.release();
            enhancer = null;
        }
        Log.i(TAG, "boost off");
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
