package com.outsystems.geofencing;

import android.Manifest;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.icu.text.DateFormat;
import android.icu.util.TimeZone;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingClient;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;

import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;

/**
 * This class echoes a string called from JavaScript.
 */
public class GeofencingPlugin extends CordovaPlugin {

    private static final int REQUEST_TURN_DEVICE_LOCATION_ON = 54;
    private final String TAG = "GeofencePluginService";
    private final String sharedPreferencesDB = "com.outsystems.geofencing.geofence";
    private GeofencingClient mGeofencingClient;
    private PendingIntent mGeofencePendingIntent = null;
    private Context mContext;
    private static final int permissionsRequestCode = 34;
    private CallbackContext mCallback;

    @Override
    protected void pluginInitialize() {
        mContext = cordova.getActivity();
        mGeofencingClient = LocationServices.getGeofencingClient(mContext);
        LocationRequest locationRequest = LocationRequest.create().setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder().addLocationRequest(locationRequest);

        // check if the client location settings are satisfied
        SettingsClient client = LocationServices.getSettingsClient(mContext);

        // create a location response that acts as a listener for the device location if enabled
        Task<LocationSettingsResponse> locationResponses = client.checkLocationSettings(builder.build());

        locationResponses.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                if (e instanceof ResolvableApiException) {
                    try {
                        ((ResolvableApiException) e).startResolutionForResult(
                                cordova.getActivity(), REQUEST_TURN_DEVICE_LOCATION_ON
                        );
                    } catch (IntentSender.SendIntentException sendIntentException) {
                        Log.d(TAG, "Error getting location settings resolution: "+sendIntentException.getMessage());
                    }
                } else {
                    Toast.makeText(mContext, "Enable your location", Toast.LENGTH_SHORT).show();
                }
            }
        });

        super.pluginInitialize();
    }

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        switch (action) {
            case "registerFence":
                addFence(args.getDouble(0), args.getDouble(1), (float) args.getDouble(2), args.getLong(3), args.getString(4),args.getString(5));
                callbackContext.success();
                return true;
            case "removeFences":
                removeFences();
                callbackContext.success();
                return true;
            case "checkPermission":
                Boolean hasAllPermissions = true;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    hasAllPermissions = cordova.hasPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION);
                }
                hasAllPermissions = hasAllPermissions && cordova.hasPermission(Manifest.permission.ACCESS_FINE_LOCATION);
                hasAllPermissions = hasAllPermissions && cordova.hasPermission(Manifest.permission.ACCESS_COARSE_LOCATION);
                callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK, hasAllPermissions));
                return true;
            case "requestPermission":
                mCallback = callbackContext;

                cordova.requestPermissions(this, permissionsRequestCode, new String[]{Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION});
                return true;
            case "setup":
                SharedPreferences preferences = cordova.getActivity().getApplicationContext().getSharedPreferences(sharedPreferencesDB,Context.MODE_PRIVATE);
                SharedPreferences.Editor preferencesEditor = preferences.edit();
                preferencesEditor.putString("Url",args.getString(0));
                preferencesEditor.putString("AppId",args.getString(1));
                preferencesEditor.putString("Key", args.getString(2));
                preferencesEditor.apply();
                callbackContext.success();
                return true;
            default:
                callbackContext.error("Action not mapped in plugin!");
                return false;
        }
    }

    @Override
    public void onRequestPermissionResult(int requestCode, String[] permissions, int[] grantResults) throws JSONException {
        super.onRequestPermissionResult(requestCode, permissions, grantResults);
        if (mCallback != null) {
            if (requestCode == permissionsRequestCode) {
                mGeofencingClient = LocationServices.getGeofencingClient(mContext);
                Boolean permissionsGranted = true;
                for (String permission : permissions) {
                    if (cordova.hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)||cordova.hasPermission(Manifest.permission.ACCESS_COARSE_LOCATION)){
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            cordova.requestPermission(this,permissionsRequestCode,Manifest.permission.ACCESS_BACKGROUND_LOCATION);
                            return;
                        }
                    }
                    permissionsGranted = permissionsGranted && cordova.hasPermission(permission);
                }
                mCallback.sendPluginResult(new PluginResult(PluginResult.Status.OK, permissionsGranted));
            }
        }
    }

    private void addFence(Double latitude, Double longitude, Float radiusInMeters, Long duration, String id,String policyNumber) {
        duration = (duration == 0L)? Geofence.NEVER_EXPIRE : duration;
        Geofence mGeofence = new Geofence.Builder()
                .setRequestId(id)
                .setCircularRegion(latitude, longitude, radiusInMeters)
                .setExpirationDuration(duration)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER | Geofence.GEOFENCE_TRANSITION_EXIT)
                .build();
        GeofencingRequest geofenceRequest = new GeofencingRequest.Builder()
                .addGeofence(mGeofence)
                .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
                .build();
        if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        SharedPreferences preferences = cordova.getActivity().getApplicationContext().getSharedPreferences(sharedPreferencesDB,Context.MODE_PRIVATE);
        SharedPreferences.Editor preferencesEditor = preferences.edit();
        try {
            JSONObject newFenceMPN = new JSONObject();
            newFenceMPN.put("MasterPolicyNumber", policyNumber);
            preferencesEditor.putString(id, newFenceMPN.toString());
            preferencesEditor.apply();
            JSONObject newFence = new JSONObject();
            newFence.put("lat",latitude);
            newFence.put("lon",longitude);
            newFence.put("radius",radiusInMeters.doubleValue());
            newFence.put("dur",duration);
            newFence.put("id",id);
            JSONArray fences = new JSONArray(preferences.getString("fences","[]"));
            fences.put(newFence);
            preferencesEditor.putString("fences",fences.toString());
        }catch (JSONException e){
            e.printStackTrace();
        }
        mGeofencingClient.addGeofences(geofenceRequest, getGeofencePendingIntent());
    }

    private PendingIntent getGeofencePendingIntent() {
        // Reuse the PendingIntent if we already have it.
        if (mGeofencePendingIntent != null) {
            return mGeofencePendingIntent;
        }
        Intent intent =new Intent(mContext, GeofenceReceiver.class);
        // We use FLAG_UPDATE_CURRENT so that we get the same pending intent back when calling
        // addGeofences() and removeGeofences().
        mGeofencePendingIntent = PendingIntent.getBroadcast(mContext, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        return mGeofencePendingIntent;
    }

    private void removeFences(){
        mGeofencingClient.removeGeofences(getGeofencePendingIntent());
    }
}
