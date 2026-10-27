package io.github.moazemad1.volumeboost;

import android.app.Activity;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Off, +3, +6 or +9 dB, picked with the remote. */
public class MainActivity extends Activity {
    private TextView status;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setBackgroundColor(0xff111111);
        status = new TextView(this);
        status.setTextSize(28);
        status.setTextColor(0xffffffff);
        status.setGravity(Gravity.CENTER);
        root.addView(status);
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER);
        for (int db = 0; db <= BoostService.MAX_DB; db += 3) {
            final int level = db;
            Button btn = new Button(this);
            btn.setText(db == 0 ? "Off" : "+" + db + " dB");
            btn.setTextSize(22);
            btn.setAllCaps(false);   // "dB", not "DB"
            btn.setOnClickListener(v -> {
                BoostService.apply(this, level);
                status.postDelayed(this::show, 500);
            });
            row.addView(btn);
        }
        root.addView(row);
        TextView note = new TextView(this);
        note.setText("Start at +3 dB. If voices crackle or buzz, go one step down.");
        note.setTextColor(0xffaaaaaa);
        note.setTextSize(16);
        note.setGravity(Gravity.CENTER);
        root.addView(note);
        setContentView(root);
        show();
    }

    private void show() {
        int db = BoostService.prefs(this).getInt("db", 0);
        status.setText(db == 0 ? "Volume Boost is off"
                : "Volume Boost: +" + db + " dB" + (BoostService.running ? " (running)" : " (not running: pick it again)"));
    }
}
