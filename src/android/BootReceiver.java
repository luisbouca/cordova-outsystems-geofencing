package com.outsystems.geofencing;

import android.Manifest;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;

import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class BootReceiver extends BroadcastReceiver {

    private final String sharedPreferencesDB = "com.outsystems.geofencing.geofence";
    List<Geofence> geofenceList;
    @Override
    public void onReceive(Context context, Intent intent) {
        geofenceList = new ArrayList<>();
        SharedPreferences preferences = context.getApplicationContext().getSharedPreferences(sharedPreferencesDB,Context.MODE_PRIVATE);
        try {
            JSONArray fences = new JSONArray(preferences.getString("fences","[]"));
            for (int i = 0; i < fences.length(); i++) {
                JSONObject fence = fences.getJSONObject(i);
                addFence(fence.getDouble("lat"),fence.getDouble("lon"), (float) fence.getDouble("radius"),fence.getLong("dur"),fence.getString("id"));
            }
            registerFences(context);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void registerFences(Context context){
        GeofencingRequest.Builder geofenceRequestBuilder = new GeofencingRequest.Builder();
        for (Geofence mGeofence : geofenceList) {
            geofenceRequestBuilder.addGeofence(mGeofence);

        }
        GeofencingRequest geofenceRequest = geofenceRequestBuilder
                .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
                .build();
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }

        LocationServices.getGeofencingClient(context).addGeofences(geofenceRequest, getGeofencePendingIntent(context));
    }

    private void addFence(Double latitude, Double longitude, Float radiusInMeters, Long duration, String id) {
        duration = (duration == 0L)? Geofence.NEVER_EXPIRE : duration;
        Geofence mGeofence = new Geofence.Builder()
                .setRequestId(id)
                .setCircularRegion(latitude, longitude, radiusInMeters)
                .setExpirationDuration(duration)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER | Geofence.GEOFENCE_TRANSITION_EXIT)
                .build();
        geofenceList.add(mGeofence);
    }

    private PendingIntent getGeofencePendingIntent(Context context) {
        Intent intent =new Intent(context, GeofenceReceiver.class);
        // We use FLAG_UPDATE_CURRENT so that we get the same pending intent back when calling
        // addGeofences() and removeGeofences().
        return PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }
}