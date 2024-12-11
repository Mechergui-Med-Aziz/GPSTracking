package com.example.lab09;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.IBinder;
import android.telephony.PhoneStateListener;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;
import android.widget.Toast;

import androidx.annotation.Nullable;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class TrackingService extends Service {

    private LocationManager locationManager;
    private TelephonyManager telephonyManager;
    private MyPhoneStateListener phoneStateListener;
    private FileWriter writer;
    private SimpleDateFormat dateFormat;
    private Location lastLocation;
    private int lastSignalStrength = 0;

    @Override
    public void onCreate() {
        super.onCreate();
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        phoneStateListener = new MyPhoneStateListener();
        dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());

        try {
            File file = new File(getExternalFilesDir(null), "LogTracking.csv");
            writer = new FileWriter(file, true);
            writer.append("Date;Latitude;Longitude;Altitude;dbm;BatteryLevel\n");
            writer.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startTracking();
        return START_STICKY;
    }

    private void startTracking() {
        try {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
            telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_SIGNAL_STRENGTHS);
        } catch (SecurityException e) {
            e.printStackTrace();
        }
    }

    private final LocationListener locationListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            if (shouldLogLocation(location)) {
                logData(location);
                lastLocation = location;
            }
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {}

        @Override
        public void onProviderEnabled(String provider) {}

        @Override
        public void onProviderDisabled(String provider) {}
    };

    private boolean shouldLogLocation(Location newLocation) {
        if (lastLocation == null) return true;
        float distance = newLocation.distanceTo(lastLocation);
        return distance > 10; // Log if moved more than 10 meters
    }

    private void logData(Location location) {
        try {
            String date = dateFormat.format(new Date());
            double latitude = location.getLatitude();
            double longitude = location.getLongitude();
            double altitude = location.getAltitude();
            int batteryLevel = getBatteryLevel();

            String logLine = String.format(Locale.US, "%s;%.6f;%.6f;%.1f;%d;%d\n",
                    date, latitude, longitude, altitude, lastSignalStrength, batteryLevel);
            writer.append(logLine);
            writer.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private int getBatteryLevel() {
        BatteryManager batteryManager = (BatteryManager) getSystemService(BATTERY_SERVICE);
        return batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
    }

    private class MyPhoneStateListener extends PhoneStateListener {
        @Override
        public void onSignalStrengthsChanged(SignalStrength signalStrength) {
            lastSignalStrength = signalStrength.getGsmSignalStrength() * 2 - 113; // Convert to dBm
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        locationManager.removeUpdates(locationListener);
        telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_NONE);
        try {
            if (writer != null) {
                writer.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Affichage du chemin du fichier
        File logFileLocation = new File(getExternalFilesDir(null), "LogTracking.csv");
        String filePath = logFileLocation.getAbsolutePath();

        // Afficher dans la console
        System.out.println("Fichier de tracking enregistré à : " + filePath);

        // Afficher un Toast sur l'écran
        Toast.makeText(this, "Fichier de tracking enregistré à : " + filePath, Toast.LENGTH_LONG).show();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}


