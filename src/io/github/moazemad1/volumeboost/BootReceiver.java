package io.github.moazemad1.volumeboost;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context c, Intent intent) {
        int db = BoostService.prefs(c).getInt("db", 0);
        if (db > 0) BoostService.apply(c, db);
    }
}
