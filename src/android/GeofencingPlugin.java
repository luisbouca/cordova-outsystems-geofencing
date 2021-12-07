package com.outsystems.geofencing;

import android.Manifest;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.icu.text.DateFormat;
import android.icu.util.TimeZone;
import android.os.Build;

import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingClient;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;

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

    private final String TAG = "GeofencePluginService";
    private final String sharedPreferencesDB = "com.luisbouca.test.geofence";
    private GeofencingClient mGeofencingClient;
    private PendingIntent mGeofencePendingIntent = null;
    private Context mContext;
    private static final int permissionsRequestCode = 34;
    private CallbackContext mCallback;

    @Override
    protected void pluginInitialize() {
        mContext = cordova.getActivity();
        mGeofencingClient = LocationServices.getGeofencingClient(mContext);
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
                .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER | GeofencingRequest.INITIAL_TRIGGER_EXIT)
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
            JSONObject newFence = new JSONObject();
            newFence.put("MasterPolicyNumber", policyNumber);
            preferencesEditor.putString(id, newFence.toString());
            preferencesEditor.apply();
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
