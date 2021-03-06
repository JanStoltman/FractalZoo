package com.draabek.fractal;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;

import com.draabek.fractal.canvas.FractalCpuView;
import com.draabek.fractal.fractal.Fractal;
import com.draabek.fractal.fractal.FractalRegistry;
import com.draabek.fractal.gl.RenderImageView;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import java.io.InputStreamReader;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;

/*
import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.Thing;
import com.google.android.gms.common.api.GoogleApiClient;

*/

public class MainActivity extends AppCompatActivity {
    private static final String LOG_KEY = MainActivity.class.getName();
    public static final String CURRENT_FRACTAL_KEY = "current_fractal";
    public static final int CHOOSE_FRACTAL_CODE = 1;

    Map<Class<? extends FractalViewWrapper>, FractalViewWrapper> availableViews;
    private FractalViewWrapper currentView;
    private SharedPreferences prefs;
    private ProgressBar progressBar;

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Reader jsonReader = new InputStreamReader(this.getResources().openRawResource(R.raw.fractallist));
        JsonParser parser = new JsonParser();
        JsonElement fractalElement = parser.parse(jsonReader);
        JsonArray fractalArray = fractalElement.getAsJsonArray();
        FractalRegistry.getInstance().init(this, fractalArray);
        prefs = PreferenceManager.getDefaultSharedPreferences(this);

        //ugh
        FractalRegistry.getInstance().setCurrent(
                FractalRegistry.getInstance().get(prefs.getString(Utils.PREFS_CURRENT_FRACTAL_KEY, "Mandelbrot"))
        );
        setContentView(R.layout.activity_main);

        //Put views into map where key is the view class, this is then requested from the fractal
        RenderImageView renderImageView = (RenderImageView) findViewById(R.id.fractalGlView);
        availableViews = new HashMap<>();
        availableViews.put(renderImageView.getClass(), renderImageView);
        FractalCpuView cpuView = (FractalCpuView) findViewById(R.id.fractalCpuView);
        availableViews.put(cpuView.getClass(), cpuView);

        progressBar = (ProgressBar)findViewById(R.id.indeterminateBar);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        //noinspection ConstantConditions
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        unveilCorrectView(FractalRegistry.getInstance().getCurrent().getName());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (Utils.DEBUG) {
            Log.d(LOG_KEY, "onCreateOptionsMenu");
        }
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return super.onCreateOptionsMenu(menu);
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (Utils.DEBUG) {
            Log.d(LOG_KEY, "onOptionsItemSelected: " + item.getItemId());
        }
        switch (item.getItemId()) {
            case R.id.fractalList:
                if (Utils.DEBUG) {
                    Log.d(LOG_KEY, "Fractal list menu item pressed");
                }
                Intent intent = new Intent(this, FractalListActivity.class);
                startActivityForResult(intent, CHOOSE_FRACTAL_CODE);
                return true;
            case R.id.save:
                if (Utils.DEBUG) {
                    Log.d(LOG_KEY, "Save menu item pressed");
                }
                return attemptSave();
            case R.id.options:
                if (Utils.DEBUG) {
                    Log.d(LOG_KEY, "Options menu item pressed");
                }
                Intent intent2 = new Intent(this, FractalPreferenceActivity.class);
                startActivity(intent2);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
    public boolean attemptSave() {
        currentView.saveBitmap();
        return true;
    }

    /**
     * Make visible the correct view according to Fractal.getViewWrapper method
     * @param newFractal Name of the current fractal
     */
    private void unveilCorrectView(String newFractal) {
        Fractal f = FractalRegistry.getInstance().get(newFractal);
        if (f == null) {
            Log.e(this.getClass().getName(), String.format("Fractal %s not found", newFractal));
            f = FractalRegistry.getInstance().get("Mandelbrot");
        }
        assert f != null;
        if (currentView != null) currentView.setVisibility(View.GONE);
        Class<? extends FractalViewWrapper> requiredViewClass = f.getViewWrapper();
        FractalViewWrapper available = availableViews.get(requiredViewClass);
        if (available == null) {
            throw new RuntimeException("No appropriate view available");
        }
        currentView = available;
        FractalRegistry.getInstance().setCurrent(f);
        if (Utils.DEBUG) {
            Log.d(LOG_KEY, f.getName() + " is current");
        }
        currentView.setVisibility(View.VISIBLE);
        currentView.clear();
        currentView.setRenderListener(new RenderListener() {
            @Override
            public void onRenderRequested() {
                Log.i(this.getClass().getName(), String.format("Rendering requested on %s",
                        FractalRegistry.getInstance().getCurrent().getName()));
                progressBar.post(() -> progressBar.setVisibility(View.VISIBLE));

            }

            @Override
            public void onRenderComplete(long millis) {
                Log.i(this.getClass().getName(), String.format("Rendering complete in %d ms", millis));
                progressBar.post(() -> {
                    if (!currentView.isRendering())
                    progressBar.setVisibility(View.GONE);
                });
            }
    });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == CHOOSE_FRACTAL_CODE) {
            try {
                String pickedFractal = data.getStringExtra(CURRENT_FRACTAL_KEY);
                unveilCorrectView(pickedFractal);
            } catch (Exception e) {
                Log.e(LOG_KEY, "Exception on fractal switch");
                e.printStackTrace();
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onStop() {
        super.onStop();
    }
}