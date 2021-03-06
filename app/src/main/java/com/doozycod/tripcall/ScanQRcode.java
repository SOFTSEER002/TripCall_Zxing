package com.doozycod.tripcall;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.Toast;

import com.doozycod.tripcall.helper.DBhelper;
import com.doozycod.tripcall.helper.DatabaseModel;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ScanQRcode extends AppCompatActivity implements LocationListener, View.OnClickListener {

    //View Objects
    boolean isNetworkEnabled = false;
    private ImageView buttonScan;
    Location location;
    double latitude;
    double longitude;
    LocationManager locationManager;
    DBhelper dBhelper;
    private IntentIntegrator qrScan;
    String QrCode;
    private static final long MIN_DISTANCE_CHANGE_FOR_UPDATES = 10;
    private static final long MIN_TIME_BW_UPDATES = 1000 * 60;
    List<DatabaseModel> databaseModels;
    String query = "";
    Cursor c;

    float distance;
    String diff = "";
    String origin_time;
    Intent intent;
    protected PowerManager.WakeLock mWakeLock;

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qrcode);
        dBhelper = new DBhelper(this);
        databaseModels = new ArrayList<>();


        databaseModels = dBhelper.getDataFromDb();
        hideStatusBar();
        getSupportActionBar().hide();
        buttonScan = findViewById(R.id.scan_code);
//      Checking that run is first time of the app or not
        Boolean isFirstRun = getSharedPreferences("PREFERENCE", MODE_PRIVATE)
                .getBoolean("isFirstRun", true);

//      Condition for Checking isFirstRun or not
        if (isFirstRun) {
            startActivity(new Intent(this, Splash.class));
        }
        getSharedPreferences("PREFERENCE", MODE_PRIVATE).edit()
                .putBoolean("isFirstRun", false).apply();
        qrScan = new IntentIntegrator(this);

        /* This code together with the one in onDestroy()
         * will make the screen be always on until this Activity gets destroyed. */
        final PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
//        Always Awake screen
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);



        buttonScan.setOnClickListener(this);
    }

    void hideStatusBar() {
        View decorView = getWindow().getDecorView();

        decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
                | View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY // hide status bar and nav bar after a short delay, or if the user interacts with the middle of the screen
        );
    }

    @Override
    protected void onResume() {
        hideStatusBar();
        super.onResume();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (result != null) {

            if (result.getContents() == null) {
                Toast.makeText(this, "Result Not Found", Toast.LENGTH_LONG).show();
            } else {

                try {
                    JSONObject obj = new JSONObject(result.getContents());
                    String QRCode = obj.getString("name");


                } catch (JSONException e) {
                    e.printStackTrace();

//                    Toast.makeText(this, result.getContents(), Toast.LENGTH_LONG).show();
                    QrCode = result.getContents();

                    if (QrCode != null) {
                        getLocation();

                    }
                }
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public void onClick(View view) {
        //initiating the qr code scan
//        getLocation();
        qrScan.initiateScan();

    }

    @TargetApi(Build.VERSION_CODES.O)
    private void getLocation() {
        try {
            locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 2000, 5, this);
            isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
            if (isNetworkEnabled) {

                locationManager.requestLocationUpdates(
                        LocationManager.NETWORK_PROVIDER,
                        MIN_TIME_BW_UPDATES,
                        MIN_DISTANCE_CHANGE_FOR_UPDATES, this);

                if (locationManager != null) {
                    location = locationManager
                            .getLastKnownLocation(LocationManager.NETWORK_PROVIDER);

                    if (location != null) {

                        latitude = location.getLatitude();
                        longitude = location.getLongitude();
                        String lat = String.valueOf(latitude);
                        String logi = String.valueOf(longitude);
                        if (QrCode != null) {
                            SQLiteDatabase db = dBhelper.getWritableDatabase();

                            try {

                                if (getCount() == 0) {

                                    //getting time
                                    Date date = new Date();
                                    String strDateFormat = "hh:mm:ss a";
                                    DateFormat dateFormat = new SimpleDateFormat(strDateFormat);
                                    origin_time = dateFormat.format(date);


                                    dBhelper.insertOrigin(QrCode, latitude, longitude, origin_time);
                                    startActivity(new Intent(ScanQRcode.this, ThankYou.class));

                                } else {

                                    //getting origin details
                                    String qrcode = c.getString(1);
                                    String origin_lat = c.getString(2);
                                    String origin_long = c.getString(3);
                                    String origin_time_db = c.getString(6);

                                    //getting desti details
                                    String des_lat = String.valueOf(latitude);
                                    String des_long = String.valueOf(longitude);
                                    //getting time for desti
                                    Date date = new Date();
                                    String strDateFormat = "hh:mm:ss a";
                                    DateFormat dateFormat = new SimpleDateFormat(strDateFormat);
                                    String desti_time = dateFormat.format(date);


                                    //getting diffrence

                                    Date date1 = dateFormat.parse(origin_time_db);
                                    Date date2 = dateFormat.parse(desti_time);
                                    long mills = date2.getTime() - date1.getTime();
                                    Log.v("Data1", "" + date1.getTime());
                                    Log.v("Data2", "" + date2.getTime());
                                    int hours = (int) (mills / (1000 * 60 * 60));
                                    int mins = (int) (mills / (1000 * 60)) % 60;

                                    diff = mins + " M"; // updated value every1 second


                                    showDistance(c.getString(2), c.getString(3), latitude, longitude, diff);
                                    dBhelper.delete(qrcode);


                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            } finally {

//                                Toast.makeText(this, diff, Toast.LENGTH_SHORT).show();

                            }

                        }

//
                    }
                }

            }
        } catch (SecurityException e) {
            e.printStackTrace();
        }
    }


    public void showDistance(String origin_lat, String origin_long, double dest_lat, double dest_long, String diff) {

        Location locationA = new Location("point A");

        locationA.setLatitude(Double.parseDouble(origin_lat));
        locationA.setLongitude(Double.parseDouble(origin_long));

        Location locationB = new Location("point B");

        locationB.setLatitude(dest_lat);
        locationB.setLongitude(dest_long);

        distance = locationA.distanceTo(locationB) / 1000;

        DecimalFormat df = new DecimalFormat("#.0000");
        String print = df.format(distance);

//        Toast.makeText(this, print + "", Toast.LENGTH_SHORT).show();

//        Bundle for parsing distance and Difference between travel time

        Bundle bundle = new Bundle();
        bundle.putString("distance", print);
        bundle.putString("diff", diff);

        intent = new Intent(ScanQRcode.this, TripSummary.class);
        intent.putExtras(bundle);

        startActivity(intent);
        finish();

    }

    public int getCount() {
        c = null;
        SQLiteDatabase db = null;
        try {
            db = dBhelper.getReadableDatabase();

            query = "select * from " + DBhelper.TABLE_NAME + " where qrcode = ?";

            c = db.rawQuery(query, new String[]{QrCode});
            if (c.moveToFirst()) {
                return c.getInt(0);
            }
            return 0;
        } finally {
            if (c != null) {
//                c.close();
            }
            if (db != null) {
//                db.close();
            }
        }
    }

    @Override
    public void onLocationChanged(Location location) {
//        Log.e(TAG, "Current Location: " + location.getLatitude() + ", " + location.getLongitude());
//        Toast.makeText(this, "Current Location: " + location.getLatitude() + ", " + location.getLongitude(), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {
        Toast.makeText(this, "Please Enable GPS and Internet", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onBackPressed() {
        finish();
        super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
