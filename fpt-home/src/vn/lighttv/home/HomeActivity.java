package vn.lighttv.home;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public final class HomeActivity extends Activity {
    private static final String[] LABELS = {"YouTube", "FPT Play", "TV360"};
    private static final String[] PACKAGES = {
            "com.google.android.youtube.tv",
            "net.fptplay.ottbox",
            "com.viettel.tv360.tv"
    };

    private StageView stage;
    private View firstTile;
    private boolean focusedOnce;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        stage = findViewById(R.id.stage);

        LinearLayout row = findViewById(R.id.row);
        final View[] tiles = new View[PACKAGES.length];
        for (int i = 0; i < PACKAGES.length; i++) {
            tiles[i] = addTile(row, LABELS[i], PACKAGES[i], i);
        }
        firstTile = tiles[0];
        View settings = findViewById(R.id.settings);

        for (int i = 0; i < tiles.length; i++) {
            View tile = tiles[i];
            tile.setNextFocusUpId(tile.getId());
            tile.setNextFocusDownId(settings.getId());
            tile.setNextFocusLeftId(i == 0 ? tile.getId() : tiles[i - 1].getId());
            tile.setNextFocusRightId(i == tiles.length - 1 ? tile.getId() : tiles[i + 1].getId());
        }
        settings.setNextFocusUpId(tiles[1].getId());
        settings.setNextFocusDownId(settings.getId());
        settings.setNextFocusLeftId(settings.getId());
        settings.setNextFocusRightId(settings.getId());
        settings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(HomeActivity.this, SettingsActivity.class));
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        stage.setBackdrop(Backgrounds.load(this));
        stage.start();
    }

    @Override
    protected void onPause() {
        stage.stop();
        super.onPause();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus && !focusedOnce && firstTile != null) {
            focusedOnce = true;
            firstTile.requestFocus();
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && isTaskRoot()) {
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    private View addTile(LinearLayout row, final String label, final String packageName, final int index) {
        View tile = getLayoutInflater().inflate(R.layout.tile, row, false);
        tile.setId(View.generateViewId());
        tile.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    stage.setFocusBias(index - 1f);
                }
            }
        });
        ImageView icon = tile.findViewById(R.id.icon);
        TextView name = tile.findViewById(R.id.label);
        TextView state = tile.findViewById(R.id.state);
        name.setText(label);
        tile.setContentDescription(label);

        boolean installed = isInstalled(packageName);
        if (installed) {
            try {
                icon.setImageDrawable(getPackageManager().getApplicationIcon(packageName));
            } catch (PackageManager.NameNotFoundException ignored) {
                installed = false;
            }
        }
        if (!installed) {
            icon.setVisibility(View.GONE);
            state.setText(R.string.missing);
        }
        final boolean canOpen = installed;
        tile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!canOpen) {
                    Toast.makeText(HomeActivity.this, label + " chưa được cài.", Toast.LENGTH_SHORT).show();
                    return;
                }
                openPackage(packageName, label);
            }
        });
        row.addView(tile);
        return tile;
    }

    private boolean isInstalled(String packageName) {
        try {
            getPackageManager().getPackageInfo(packageName, 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    private void openPackage(String packageName, String label) {
        PackageManager manager = getPackageManager();
        Intent intent = manager.getLeanbackLaunchIntentForPackage(packageName);
        if (intent == null) {
            intent = manager.getLaunchIntentForPackage(packageName);
        }
        if (intent == null) {
            Toast.makeText(this, "Không mở được " + label + ".", Toast.LENGTH_SHORT).show();
            return;
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "Không mở được " + label + ".", Toast.LENGTH_SHORT).show();
        }
    }

}
