package com.neoviz.fit_lanka_demo.service;

import static android.content.pm.PackageManager.PERMISSION_GRANTED;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.neoviz.fit_lanka_demo.activity.HomeActivity;
import com.neoviz.fit_lanka_demo.R;
import com.neoviz.fit_lanka_demo.util.AppUtils;
import com.neoviz.fit_lanka_demo.util.StepUtils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class StepCounterService extends Service implements SensorEventListener {

    private static final String TAG = "StepCounterService";
    private static final String CHANNEL_ID = "StepCounterChannel";

    private SensorManager sensorManager;
    private Sensor stepSensor;
    private FirebaseFirestore db;
    private FirebaseAuth auth;

    private int stepsAtReset = 0;
    private int currentSteps = 0;
    private int lastSyncedSteps = 0;

    @Override
    public void onCreate() {
        super.onCreate();
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);

        if (stepSensor != null) {
            sensorManager.registerListener(this, stepSensor, SensorManager.SENSOR_DELAY_NORMAL);
        }

        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Android 14+ වලදී Permission එක නැතිව startForeground කලොත් Crash වෙනවා
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.FOREGROUND_SERVICE_HEALTH) != PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION) != PackageManager.PERMISSION_GRANTED) {

                Log.e(TAG, "Permissions not granted for Health FGS. Stopping service.");
                stopSelf();
                return START_NOT_STICKY;
            }
        }

        Intent notificationIntent = new Intent(this, HomeActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this,
                0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("FitLanka Active")
                .setContentText("Tracking your daily activity...")
                .setSmallIcon(R.drawable.gym)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .build();

        // මෙතනදී type එක හරියටම දෙන්න
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_HEALTH);
        } else {
            startForeground(1, notification);
        }

        return START_STICKY;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_STEP_COUNTER) {
            float totalStepsSinceBoot = event.values[0];

            checkDailyReset(totalStepsSinceBoot);

            SharedPreferences prefs = getSharedPreferences("StepPrefs", MODE_PRIVATE);
            stepsAtReset = prefs.getInt("stepsAtRest", Math.round(totalStepsSinceBoot));

            currentSteps = Math.round(totalStepsSinceBoot) - stepsAtReset;
            if (currentSteps < 0) currentSteps = 0;

            // Save for HomeFragment to read
            prefs.edit().putInt("currentSteps", currentSteps).apply();

            // Sync with Firestore (e.g., every 50 steps)
            if (Math.abs(currentSteps - lastSyncedSteps) >= 50) {
                syncToFirestore(currentSteps);
            }
        }
    }

    private void checkDailyReset(float totalStepsSinceBoot) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String currentDate = sdf.format(new Date());

        SharedPreferences prefs = getSharedPreferences("StepPrefs", MODE_PRIVATE);
        String lastDate = prefs.getString("lastDate", "");

        if (!currentDate.equals(lastDate)) {
            if (!lastDate.isEmpty() && currentSteps > 0) {
                archiveYesterdayData(lastDate, currentSteps);
            }

            stepsAtReset = Math.round(totalStepsSinceBoot);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putInt("stepsAtRest", stepsAtReset);
            editor.putString("lastDate", currentDate);
            editor.putInt("currentSteps", 0);
            editor.apply();

            currentSteps = 0;
            lastSyncedSteps = 0;
            syncToFirestore(0);
        }
    }

    private void syncToFirestore(int steps) {
        if (auth.getCurrentUser() == null) return;

        int diff = steps - lastSyncedSteps;
        if (diff <= 0 && steps != 0) return;

        String uid = auth.getCurrentUser().getUid();

        /// update data for leaderboard
        if(diff>0){
            StepUtils.updateLeaderboardSteps(auth.getCurrentUser().getUid(), diff);
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put("currentDaySteps", steps);
        if (diff > 0) {
            updates.put("allTimeSteps", FieldValue.increment(diff));
            updates.put("fitCoins", FieldValue.increment(diff * AppUtils.COINS_PER_STEP_PASSIVE));


        }

        db.collection("users").document(uid)
                .update(updates)
                .addOnSuccessListener(aVoid -> lastSyncedSteps = steps);
    }

    private void archiveYesterdayData(String date, int steps) {
        if (auth.getCurrentUser() == null) return;
        Map<String, Object> record = new HashMap<>();
        record.put("id", date);
        record.put("stepsCount", steps);
        record.put("timestamp", System.currentTimeMillis());
        db.collection("users").document(auth.getCurrentUser().getUid())
                .update("dailySteps", FieldValue.arrayUnion(record));
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Steps", NotificationManager.IMPORTANCE_LOW);
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) manager.createNotificationChannel(channel);
        }
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }

    @Override
    public void onDestroy() {
        super.onDestroy();
        sensorManager.unregisterListener(this);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}
}