package io.github.moazemad1.volumeboost;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

/** The TV entry point. This firmware gives BOOT_COMPLETED only to its own system apps, so the launcher's
 *  "Open on start" opens this once per boot instead: it starts the boost at the saved level and closes
 *  with nothing on screen. Opened again while the boost runs, it shows the level screen. */
public class StartActivity extends Activity {
    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        int db = BoostService.prefs(this).getInt("db", 0);
        if (db > 0 && !BoostService.running) {
            BoostService.apply(this, db);
        } else {
            startActivity(new Intent(this, MainActivity.class));
        }
        finish();
        overridePendingTransition(0, 0);
    }
}
