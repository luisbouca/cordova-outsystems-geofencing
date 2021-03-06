package com.outsystems.geofencing;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.icu.text.DateFormat;
import android.icu.text.SimpleDateFormat;
import android.icu.util.TimeZone;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.JobIntentService;
import androidx.core.app.NotificationCompat;
import androidx.core.app.TaskStackBuilder;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingEvent;

import $appid.MainActivity;
import $appid.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class GeofenceTransitionsJobIntentService extends JobIntentService {

    private static final int JOB_ID = 573;
    private final String TAG = "GeofencePluginService";
    private final String sharedPreferencesDB = "com.outsystems.geofencing.geofence";
    private final String TOSEND = "toSend";
    private final String SENT = "sent";
    private static final String CHANNEL_ID = "channel_01";
    private static final String CHANNEL_ID2 = "channel_02";

    /**
     * Convenience method for enqueuing work in to this service.
     */
    public static void enqueueWork(Context context, Intent intent){
        enqueueWork(context,GeofenceTransitionsJobIntentService.class, JOB_ID,intent);
    }

    @Override
    protected void onHandleWork(@NonNull Intent intent) {


        GeofencingEvent geofencingEvent = GeofencingEvent.fromIntent(intent);
        if (geofencingEvent.hasError()){
            Log.e(TAG, String.valueOf(geofencingEvent.getErrorCode()));
            return;
        }
        // Get the transition type.
        int geofenceTransition = geofencingEvent.getGeofenceTransition();

        // Test that the reported transition was of interest.
        if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER ||
                geofenceTransition == Geofence.GEOFENCE_TRANSITION_EXIT) {

            sendNotification("New Geofence detected","",CHANNEL_ID);
            // Get the geofences that were triggered. A single event can trigger multiple geofences.
            List<Geofence> triggeringGeofences = geofencingEvent.getTriggeringGeofences();
            try {
                for (Geofence geofence : triggeringGeofences) {
                    SharedPreferences preferences = getApplicationContext().getSharedPreferences(sharedPreferencesDB, Context.MODE_PRIVATE);
                    JSONObject fence = new JSONObject(preferences.getString(geofence.getRequestId(), ""));
                    int action = 0;
                    if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_EXIT) {
                        action = 1;
                    }
                    fence.put("Latitude", geofencingEvent.getTriggeringLocation().getLatitude());
                    fence.put("Longitude", geofencingEvent.getTriggeringLocation().getLongitude());

                    registerRequest(fence, geofence.getRequestId(), action);
                }
            }catch (JSONException e){
                e.printStackTrace();
            }

            // Get the transition details as a String.
            String geofenceTransitionDetails = getGeofenceTransitionDetails(geofenceTransition,
                    triggeringGeofences);

            Log.i(TAG, geofenceTransitionDetails);
            try {
                sendApiRequest();
            } catch (JSONException e) {
                e.printStackTrace();
            }
        } else {
            sendNotification("New Location","New location retrieved!",CHANNEL_ID2);
            // Log the error.
            Log.e(TAG, "geofence transition invalid type");
        }
    }


    /**
     * Gets transition details and returns them as a formatted string.
     *
     * @param geofenceTransition    The ID of the geofence transition.
     * @param triggeringGeofences   The geofence(s) triggered.
     * @return                      The transition details formatted as String.
     */
    private String getGeofenceTransitionDetails(int geofenceTransition,List<Geofence> triggeringGeofences){
        String geofenceTransitionString = getTransitionString(geofenceTransition);

        // Get the Ids of each geofence that was triggered.
        List<String> triggeringGeofencesIdsList = new ArrayList<>();
        for (Geofence geofence : triggeringGeofences) {
            triggeringGeofencesIdsList.add(geofence.getRequestId());
        }
        String triggeringGeofencesIdsString = TextUtils.join(", ",  triggeringGeofencesIdsList);

        return geofenceTransitionString +" : "+ triggeringGeofencesIdsString;
    }
    private void registerRequest(JSONObject newFence,String id,int fenceAction){
        SharedPreferences preferences = getApplicationContext().getSharedPreferences(sharedPreferencesDB,Context.MODE_PRIVATE);
        SharedPreferences.Editor preferencesEditor = preferences.edit();
        try {
            JSONArray fences = new JSONArray(preferences.getString(TOSEND, "[]"));
            DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            df.setTimeZone(TimeZone.getTimeZone("UTC"));//2021-08-21T09:20:32
            String utcDateTime = df.format(new Date());
            utcDateTime = utcDateTime.replace(" ","T");
            newFence.put("Datetime",utcDateTime);
            newFence.put("FenceAction", fenceAction);
            if (fenceAction == 1){
                newFence.put("Tag","Fora de fence");
            }else{
                newFence.put("Tag", id);
            }
            fences.put(newFence);
            preferencesEditor.putString(TOSEND, fences.toString());
            preferencesEditor.apply();
            sendNotification("New Geofence added to send",fences.toString(), CHANNEL_ID);
        }catch (JSONException e){
            e.printStackTrace();
        }
    }
    private void sendApiRequest() throws JSONException{
        SharedPreferences preferences = getApplicationContext().getSharedPreferences(sharedPreferencesDB,Context.MODE_PRIVATE);
        SharedPreferences.Editor preferencesEditor = preferences.edit();
        JSONArray fences = new JSONArray(preferences.getString(TOSEND,"[]"));
        preferencesEditor.putString(SENT,preferences.getString(TOSEND,"[]"));
        preferencesEditor.putString(TOSEND,"[]");
        preferencesEditor.apply();
        OkHttpClient client = new OkHttpClient();
        RequestBody body = RequestBody.create(
                MediaType.parse("application/json"), fences.toString());

        String Url = preferences.getString("Url","");
        String AppId = preferences.getString("AppId","");
        String Key = preferences.getString("Key","");
        Request request = new Request.Builder()
                .url(Url)
                .addHeader("X-Contacts-AppId",AppId)
                .addHeader("X-Contacts-Key",Key)
                .post(body)
                .build();

        sendNotification("Geofence Sent",fences.toString(), CHANNEL_ID);

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                e.getStackTrace();

                sendNotification("Geofence Sent error"," ", CHANNEL_ID);
                try {
                    if (!preferences.getString(TOSEND, "").equals("")) {
                        JSONArray tojoin = new JSONArray(preferences.getString(SENT, "[]"));
                        JSONArray joined = new JSONArray(preferences.getString(TOSEND, "[]"));
                        for (int i = 0; i<tojoin.length();i++) {
                            joined.put(tojoin.getJSONObject(i));
                        }
                        preferencesEditor.putString(TOSEND, joined.toString());
                    } else {
                        preferencesEditor.putString(TOSEND, preferences.getString(SENT, "[]"));
                    }
                    preferencesEditor.putString(SENT,"[]");
                    preferencesEditor.apply();
                }catch (JSONException e1){
                    e1.printStackTrace();
                }
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                try {

                    sendNotification("Geofence Sent response", String.valueOf(response.code()), CHANNEL_ID);
                    if (response.code() != 200) {
                        if (!preferences.getString(TOSEND, "").equals("")) {
                            JSONArray tojoin = new JSONArray(preferences.getString(SENT, ""));
                            JSONArray joined = new JSONArray(preferences.getString(TOSEND, ""));
                            for (int i = 0; i < tojoin.length(); i++) {
                                joined.put(tojoin.getJSONObject(i));
                            }
                            preferencesEditor.putString(TOSEND, joined.toString());
                        } else {
                            preferencesEditor.putString(TOSEND, preferences.getString(SENT, ""));
                        }
                    }
                    preferencesEditor.putString(SENT, "");
                    preferencesEditor.apply();
                }catch (JSONException e){
                    e.printStackTrace();
                }
            }
        });
    }

    /**
     * Maps geofence transition types to their human-readable equivalents.
     *
     * @param transitionType    A transition type constant defined in Geofence
     * @return                  A String indicating the type of transition
     */
    private String getTransitionString(int transitionType) {
        switch (transitionType) {
            case Geofence.GEOFENCE_TRANSITION_EXIT :
                return "Exited";
            case Geofence.GEOFENCE_TRANSITION_ENTER :
                return "Entered";
            default:
                return "unknown";
        }
    }

    private void sendNotification(String notificationTitle, String notificationDetails, String channelId) {
        SharedPreferences preferences = getApplicationContext().getSharedPreferences(sharedPreferencesDB,Context.MODE_PRIVATE);
        Boolean isDebug = preferences.getBoolean("isDebug", false);
        if (!isDebug){
            return;
        }
        // Get an instance of the Notification manager
        NotificationManager mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        // Android O requires a Notification Channel.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = getString(R.string.app_name);
            // Create the channel for the notification
            NotificationChannel mChannel =
                    new NotificationChannel(channelId, name, NotificationManager.IMPORTANCE_DEFAULT);

            // Set the Notification Channel for the Notification Manager.
            mNotificationManager.createNotificationChannel(mChannel);
        }

        // Create an explicit content Intent that starts the main Activity.
        Intent notificationIntent = new Intent(getApplicationContext(), MainActivity.class);

        // Construct a task stack.
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);

        // Add the main Activity to the task stack as the parent.
        stackBuilder.addParentStack(MainActivity.class);

        // Push the content Intent onto the stack.
        stackBuilder.addNextIntent(notificationIntent);

        // Get a PendingIntent containing the entire back stack.
        PendingIntent notificationPendingIntent =
                stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);

        // Get a notification builder that's compatible with platform versions >= 4
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);

        // Define the notification settings.
        builder.setSmallIcon(R.mipmap.ic_launcher)
                // In a real app, you may want to use a library like Volley
                // to decode the Bitmap.
                .setLargeIcon(BitmapFactory.decodeResource(getResources(),
                        R.mipmap.ic_launcher))
                .setColor(Color.RED)
                .setContentTitle(notificationTitle)
                .setContentText(notificationDetails)
                .setContentIntent(notificationPendingIntent);

        // Set the Channel ID for Android O.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder.setChannelId(CHANNEL_ID); // Channel ID
        }

        // Dismiss notification once the user touches it.
        builder.setAutoCancel(true);

        // Issue the notification
        mNotificationManager.notify(0, builder.build());
    }
}
