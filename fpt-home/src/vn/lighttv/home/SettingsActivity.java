package vn.lighttv.home;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

public final class SettingsActivity extends Activity {
    private PhotoServer server;
    private TextView status;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        status = findViewById(R.id.status);
        TextView address = findViewById(R.id.address);
        String host = PhotoServer.address();
        if (host == null) {
            address.setText(R.string.no_wifi);
        } else {
            address.setText("http://" + host + ":" + PhotoServer.PORT);
        }

        findViewById(R.id.use_default).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Backgrounds.clear(SettingsActivity.this);
                status.setText(R.string.default_ready);
            }
        });
        findViewById(R.id.done).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        findViewById(R.id.done).requestFocus();
    }

    @Override
    protected void onStart() {
        super.onStart();
        server = new PhotoServer(this, new PhotoServer.Listener() {
            @Override
            public void onSaved() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        status.setText(R.string.photo_saved);
                    }
                });
            }
        });
        try {
            server.start();
        } catch (Exception ignored) {
            status.setText(R.string.server_failed);
        }
    }

    @Override
    protected void onStop() {
        if (server != null) {
            server.stop();
            server = null;
        }
        super.onStop();
    }
}
