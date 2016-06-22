package com.pruebas.allanflores.beaconsbeta;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.webkit.WebView;
import android.widget.TextView;
import android.widget.Toast;

import com.kontakt.sdk.android.ble.configuration.ActivityCheckConfiguration;
import com.kontakt.sdk.android.ble.configuration.ScanPeriod;
import com.kontakt.sdk.android.ble.configuration.scan.ScanMode;
import com.kontakt.sdk.android.ble.connection.OnServiceReadyListener;
import com.kontakt.sdk.android.ble.device.BeaconRegion;
import com.kontakt.sdk.android.ble.filter.ibeacon.IBeaconFilters;
import com.kontakt.sdk.android.ble.manager.ProximityManager;
import com.kontakt.sdk.android.ble.manager.ProximityManagerContract;
import com.kontakt.sdk.android.ble.manager.listeners.IBeaconListener;
import com.kontakt.sdk.android.ble.manager.listeners.ScanStatusListener;
import com.kontakt.sdk.android.ble.manager.listeners.simple.SimpleIBeaconListener;
import com.kontakt.sdk.android.ble.manager.listeners.simple.SimpleScanStatusListener;
import com.kontakt.sdk.android.cloud.IKontaktCloud;
import com.kontakt.sdk.android.cloud.KontaktCloud;
import com.kontakt.sdk.android.cloud.exception.KontaktCloudException;
import com.kontakt.sdk.android.cloud.response.paginated.Actions;
import com.kontakt.sdk.android.common.KontaktSDK;
import com.kontakt.sdk.android.common.log.LogLevel;
import com.kontakt.sdk.android.common.model.Action;
import com.kontakt.sdk.android.common.profile.IBeaconDevice;
import com.kontakt.sdk.android.common.profile.IBeaconRegion;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {

    private ProximityManagerContract proximityManager;
    private TextView textView;
    private WebView webView;
    private IKontaktCloud kontaktCloud;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        KontaktSDK.initialize(this).setDebugLoggingEnabled(BuildConfig.DEBUG).setLogLevelEnabled(LogLevel.DEBUG, true);;
        setContentView(R.layout.activity_main);
        //textView = (TextView) findViewById(R.id.layout1);
        webView = (WebView)findViewById(R.id.webView);

        proximityManager = new ProximityManager(this);
        configureProximityManager();
        configureListeners();
        configureSpaces();
        configureFilters();


        kontaktCloud = KontaktCloud.newInstance();

        ActionBeacon task = new ActionBeacon();
        task.execute();


    }

    private class ActionBeacon extends AsyncTask<Void, Void, Void> {

        String url = null;

        @Override
        protected Void doInBackground(Void... params) {

            Log.i("Allan", "ActionBeacon: " + "doInBackgroind");
            // write service code here
            try {
                Actions a = kontaktCloud.actions().fetch().forDevices("Rj5g").execute();
                Log.i("Mira ACTION SIZE-------", String.valueOf(a.getContent().size()));
                url = displayActions(a.getContent(), Action.Type.CONTENT);
                url = displayActions(a.getContent(), Action.Type.BROWSER);
            } catch (IOException e) {
                e.printStackTrace();
            } catch ( KontaktCloudException e) {
                Log.i("KontaktException-------", e.toString());
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            webView.loadUrl(url);
            Log.i("Allan", "ActionBeacon: " + "onPostExecute");

        }
    }

    private String displayActions(List<Action> actionsList, Action.Type targetType){
        Action targetAction = null;

        for(Action action : actionsList){
            if(action.getType() == targetType) {
                targetAction = action;
                break;
            }
        }

        if(targetAction == null)
            return null;

        String url = null;
        if(targetAction.getType() == Action.Type.CONTENT){
            url = targetAction.getContent().getContentUrl();
        }else if(targetAction.getType() == Action.Type.BROWSER){
            url = targetAction.getUrl();
        }

        if(TextUtils.isEmpty(url)){
            return null;
        }

        showToast(url);

        return url;
    }

    private void configureProximityManager() {
        proximityManager.configuration()
                .scanMode(ScanMode.LOW_LATENCY)
                .scanPeriod(ScanPeriod.create(TimeUnit.SECONDS.toMillis(10), TimeUnit.SECONDS.toMillis(20)))
                .activityCheckConfiguration(ActivityCheckConfiguration.MINIMAL)
                .monitoringEnabled(true);
    }

    private void configureListeners() {
        proximityManager.setIBeaconListener(createIBeaconListener());
        proximityManager.setScanStatusListener(createScanStatusListener());
    }

    private void configureSpaces() {
        IBeaconRegion region = new BeaconRegion.Builder()
                .setIdentifier("All my iBeacons")
                .setProximity(UUID.fromString("123e4567-e89b-12d3-a456-426655440000"))
                .build();

        proximityManager.spaces().iBeaconRegion(region);
    }

    private void configureFilters() {
        proximityManager.filters().iBeaconFilter(IBeaconFilters.newDeviceNameFilter("JonSnow"));
    }

    @Override
    protected void onStart() {
        super.onStart();
        startScanning();
    }

    @Override
    protected void onStop() {
        proximityManager.stopScanning();
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        proximityManager.disconnect();
        proximityManager = null;
        super.onDestroy();
    }

    private void startScanning() {
        proximityManager.connect(new OnServiceReadyListener() {
            @Override
            public void onServiceReady() {
                proximityManager.startScanning();
            }
        });
    }

    private void showToast(final String message) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private IBeaconListener createIBeaconListener() {
        return new SimpleIBeaconListener() {
            @Override
            public void onIBeaconDiscovered(IBeaconDevice ibeacon, IBeaconRegion region) {
                Log.i("Sample", "IBeacon discovered! UniqueID: " + ibeacon.getUniqueId());
                Log.i("Allan", "IBEACON: " + ibeacon.describeContents());
            }
        };
    }

    private ScanStatusListener createScanStatusListener() {
        return new SimpleScanStatusListener() {
            @Override
            public void onScanStart() {
                showToast("Scanning started");
            }

            @Override
            public void onScanStop() {
                showToast("Scanning stopped");
            }
        };
    }
}

