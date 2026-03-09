package com.neoviz.fit_lanka_demo.fragment;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.neoviz.fit_lanka_demo.databinding.FragmentHomeBinding;
import com.neoviz.fit_lanka_demo.model.User;
import com.neoviz.fit_lanka_demo.util.Utils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class HomeFragment extends Fragment implements SensorEventListener {

    private static final String TAG = "HomeFragment";
    private static final String STEPS_AT_REST_KEY = "stepsAtRest";
    private static final String LAST_SAVED_DATE_KEY = "lastDate";
    private static final String PREF_CURRENT_STEPS = "currentSteps";

    private FragmentHomeBinding binding;
    private SensorManager sensorManager;
    private Sensor stepSensor;

    private FirebaseFirestore db;
    private FirebaseAuth auth;

    private int stepsAtReset = 0;
    private int currentSteps = 0;
    private int lastSyncedSteps = 0;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Permission Check
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACTIVITY_RECOGNITION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(), new String[]{Manifest.permission.ACTIVITY_RECOGNITION}, 1);
        }

        sensorManager = (SensorManager) requireActivity().getSystemService(Context.SENSOR_SERVICE);
        stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);

        loadSavedDataFromPrefs();
        fetchUserDataFromFirestore();

        if (stepSensor == null) {
            binding.tvStepCount.setText("No Sensor Found");
        }
    }

    private void fetchUserDataFromFirestore() {
        if (auth.getCurrentUser() == null) return;
        db.collection("users").document(auth.getCurrentUser().getUid())
                .get().addOnSuccessListener(doc -> {
                    User user = doc.toObject(User.class);
                    if (user != null) {
                        int target = user.getDailyStepTarget() > 0 ? user.getDailyStepTarget() : 5000;
                        binding.progressBar.setMax(target);

                        loadChartWithRealData(user.getDailySteps(), currentSteps);
                    }
                });
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_STEP_COUNTER) {
            float totalStepsSinceBoot = event.values[0];

            // 1. Check for Daily Reset & Archive
            checkAndHandleDailyReset(totalStepsSinceBoot);

            // 2. Calculate Current Day Steps
            currentSteps = Math.round(totalStepsSinceBoot) - stepsAtReset;
            if (currentSteps < 0) currentSteps = 0;

            updateUI(currentSteps);
            saveCurrentStepsToLocal();

            // 3. Sync to Firestore every 50 steps
            if (Math.abs(currentSteps - lastSyncedSteps) >= 50) {
                syncStepsToFirestore(currentSteps);
            }
        }
    }

    private void syncStepsToFirestore(int steps) {
        if (auth.getCurrentUser() == null) return;

        int diff = steps - lastSyncedSteps;
        if (diff <= 0) return;

        double earnedCoins = diff * Utils.COINS_PER_STEP_PASSIVE;

        Map<String, Object> updates = new HashMap<>();
        updates.put("currentDaySteps", steps);
        updates.put("allTimeSteps", FieldValue.increment(diff));
        updates.put("fitCoins", FieldValue.increment(earnedCoins));

        db.collection("users").document(auth.getCurrentUser().getUid())
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    lastSyncedSteps = steps;
                    Log.d(TAG, "Steps & Coins synced!");
                });
    }

    private void checkAndHandleDailyReset(float totalStepsSinceBoot) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String currentDate = sdf.format(new Date());

        SharedPreferences prefs = requireActivity().getSharedPreferences("StepPrefs", Context.MODE_PRIVATE);
        String lastDate = prefs.getString(LAST_SAVED_DATE_KEY, "");

        if (!currentDate.equals(lastDate) && !lastDate.isEmpty()) {
            // දවස මාරු වෙලා, පරණ දවසේ පියවර archive කරමු
            archiveYesterdaySteps(lastDate, currentSteps);

            // Reset local variables for the new day
            stepsAtReset = Math.round(totalStepsSinceBoot);
            currentSteps = 0;
            lastSyncedSteps = 0;

            SharedPreferences.Editor editor = prefs.edit();
            editor.putInt(STEPS_AT_REST_KEY, stepsAtReset);
            editor.putString(LAST_SAVED_DATE_KEY, currentDate);
            editor.putInt(PREF_CURRENT_STEPS, 0);
            editor.apply();

            // Firestore එකෙත් දවසේ පියවර 0 කරන්න
            if (auth.getCurrentUser() != null) {
                db.collection("users").document(auth.getCurrentUser().getUid())
                        .update("currentDaySteps", 0);
            }
        }
    }

    // දිනපතා වාර්තාව archive කිරීමේ අලුත් මෙතඩ් එක
    private void archiveYesterdaySteps(String date, int finalSteps) {
        if (auth.getCurrentUser() == null || finalSteps <= 0) return;

        User.DailySteps historyRecord = User.DailySteps.builder()
                .id(date)
                .stepsCount(finalSteps)
                .timestamp(System.currentTimeMillis())
                .build();

        db.collection("users").document(auth.getCurrentUser().getUid())
                .update("dailySteps", FieldValue.arrayUnion(historyRecord))
                .addOnSuccessListener(aVoid -> Log.d(TAG, "History Archived: " + date))
                .addOnFailureListener(e -> Log.e(TAG, "History Archive Failed", e));
    }

    private void updateUI(int stepsCount) {
        if (binding != null) {
            binding.tvStepCount.setText(String.valueOf(stepsCount));
            binding.progressBar.setProgress(stepsCount);

            double distance = (stepsCount * Utils.METER_PER_STEP) / 1000;
            binding.tvDistance.setText(String.format(Locale.getDefault(), "%.2f km", distance));

            double calories = stepsCount * Utils.CALORIES_PER_STEP;
            binding.tvCalories.setText(String.format(Locale.getDefault(), "%.0f kcal", calories));

            if (binding.stepChart.getData() != null && binding.stepChart.getData().getDataSetCount() > 0) {
                BarDataSet set = (BarDataSet) binding.stepChart.getData().getDataSetByIndex(0);
                set.getEntryForIndex(6).setY(stepsCount);
                binding.stepChart.notifyDataSetChanged();
                binding.stepChart.invalidate();
            }
        }
    }

    private void loadSavedDataFromPrefs() {
        SharedPreferences prefs = requireActivity().getSharedPreferences("StepPrefs", Context.MODE_PRIVATE);
        stepsAtReset = prefs.getInt(STEPS_AT_REST_KEY, 0);
        currentSteps = prefs.getInt(PREF_CURRENT_STEPS, 0);
        lastSyncedSteps = currentSteps;
        updateUI(currentSteps);
    }

    private void saveCurrentStepsToLocal() {
        SharedPreferences prefs = requireActivity().getSharedPreferences("StepPrefs", Context.MODE_PRIVATE);
        prefs.edit().putInt(PREF_CURRENT_STEPS, currentSteps).apply();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (sensorManager != null) sensorManager.unregisterListener(this);
        syncStepsToFirestore(currentSteps);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (stepSensor != null) {
            sensorManager.registerListener(this, stepSensor, SensorManager.SENSOR_DELAY_UI);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private void loadChartWithRealData(List<User.DailySteps> history, int todaySteps) {
        List<BarEntry> entries = new ArrayList<>();

        // 1. කලින් දවස් 6 සඳහා 0 දාමු (දත්ත නැති වුණොත් පෙන්වන්න)
        float[] stepsArray = new float[7];

        if (history != null && !history.isEmpty()) {
            // අන්තිම දවස් 6 දත්ත ගමු (History එකේ අන්තිම ටික)
            int historySize = history.size();
            int count = 0;
            for (int i = historySize - 1; i >= 0 && count < 6; i--) {
                // අන්තිම දවස් ටික පිටුපස සිට ඉදිරියට (index 5, 4, 3...)
                stepsArray[5 - count] = (float) history.get(i).getStepsCount();
                count++;
            }
        }

        // 2. අන්තිම index එකට (6) අද දවසේ පියවර ගාන දාමු
        stepsArray[6] = (float) todaySteps;

        // 3. Entries ලිස්ට් එක හදමු
        for (int i = 0; i < 7; i++) {
            entries.add(new BarEntry(i, stepsArray[i]));
        }

        // 4. Chart එක Update කරමු
        BarDataSet dataSet = new BarDataSet(entries, "Weekly Steps");
        dataSet.setColor(Color.parseColor("#57F4FA"));
        dataSet.setValueTextColor(Color.GRAY);
        dataSet.setValueTextSize(10f);

        BarData barData = new BarData(dataSet);
        barData.setBarWidth(0.6f);

        binding.stepChart.setData(barData);
        binding.stepChart.animateY(1000);
        binding.stepChart.invalidate(); // Refresh chart
    }
}