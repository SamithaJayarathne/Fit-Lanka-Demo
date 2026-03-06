package com.neoviz.fit_lanka_demo.fragment;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.neoviz.fit_lanka_demo.databinding.FragmentWorkoutBinding;

import java.util.HashMap;
import java.util.Map;

public class WorkoutFragment extends Fragment implements SensorEventListener, OnMapReadyCallback {

    private FragmentWorkoutBinding binding;
    private SensorManager sensorManager;
    private Sensor stepSensor;

    private FirebaseFirestore db;
    private FirebaseAuth auth;

    private boolean isWorkoutActive = false;
    private int stepsAtStart = 0;
    private int currentWorkoutSteps = 0;
    private static final double WORKOUT_COIN_VALUE = 0.1;
    private GoogleMap mMap;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentWorkoutBinding.inflate(inflater, container, false);
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        // Initialize Map
        binding.workoutMapView.onCreate(savedInstanceState);
        binding.workoutMapView.getMapAsync(this);

        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        sensorManager = (SensorManager) requireActivity().getSystemService(Context.SENSOR_SERVICE);
        stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);

        binding.btnStartWorkout.setOnClickListener(v -> startWorkout());
        binding.btnStopWorkout.setOnClickListener(v -> stopWorkout());

        binding.btnStopWorkout.setVisibility(View.GONE);
    }

    private void startWorkout() {
        if (stepSensor == null) {
            Toast.makeText(getContext(), "Sensor not found!", Toast.LENGTH_SHORT).show();
            return;
        }

        isWorkoutActive = true;
        stepsAtStart = 0;
        currentWorkoutSteps = 0;

        binding.btnStartWorkout.setVisibility(View.GONE);
        binding.btnStopWorkout.setVisibility(View.VISIBLE);
        binding.tvWorkoutSteps.setText("0");

        Toast.makeText(getContext(), "Workout Started!", Toast.LENGTH_LONG).show();
    }

    private void stopWorkout() {
        isWorkoutActive = false;
        binding.btnStopWorkout.setEnabled(false);
        updateWorkoutToFirestore(currentWorkoutSteps);
    }

    private void updateWorkoutToFirestore(int totalSteps) {
        if (auth.getCurrentUser() == null || totalSteps <= 0) {
            resetUI();
            return;
        }

        double earnedCoins = totalSteps * WORKOUT_COIN_VALUE;
        String uid = auth.getCurrentUser().getUid();

        Map<String, Object> updates = new HashMap<>();
        updates.put("currentDaySteps", FieldValue.increment(totalSteps));
        updates.put("allTimeSteps", FieldValue.increment(totalSteps));
        updates.put("fitCoins", FieldValue.increment(earnedCoins));

        db.collection("users").document(uid)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "Workout Saved! Earned: " + earnedCoins + " Coins", Toast.LENGTH_SHORT).show();
                    resetUI();
                })
                .addOnFailureListener(e -> {
                    Log.e("Workout", "Update failed", e);
                    if (binding != null) binding.btnStopWorkout.setEnabled(true);
                });
    }

    private void resetUI() {
        if (binding != null) {
            binding.btnStartWorkout.setVisibility(View.VISIBLE);
            binding.btnStopWorkout.setVisibility(View.GONE);
            binding.btnStopWorkout.setEnabled(true);
            binding.tvWorkoutSteps.setText("0");
        }
        currentWorkoutSteps = 0;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (isWorkoutActive && event.sensor.getType() == Sensor.TYPE_STEP_COUNTER) {
            int totalStepsSinceBoot = (int) event.values[0];
            if (stepsAtStart == 0) stepsAtStart = totalStepsSinceBoot;
            currentWorkoutSteps = totalStepsSinceBoot - stepsAtStart;
            if (binding != null) binding.tvWorkoutSteps.setText(String.valueOf(currentWorkoutSteps));
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    // --- Map Lifecycle Management ---

    @Override
    public void onResume() {
        super.onResume();
        if (binding != null) binding.workoutMapView.onResume();
        if (stepSensor != null) {
            sensorManager.registerListener(this, stepSensor, SensorManager.SENSOR_DELAY_UI);
        }
    }

    @Override
    public void onPause() {
        if (binding != null) binding.workoutMapView.onPause();
        super.onPause();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        if (binding != null) binding.workoutMapView.onLowMemory();
    }

    @Override
    public void onDestroyView() {
        // 1. Map එක මුලින්ම Destroy කරන්න
        if (binding != null) {
            binding.workoutMapView.onDestroy();
        }
        // 2. Sensor එක Unregister කරන්න
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }
        // 3. Super call එක අන්තිමට
        super.onDestroyView();
        // 4. Binding එක null කරන්න
        binding = null;
    }

    // මචං, මම onDestroy() එක අයින් කළා. මොකද onDestroyView එකේදී අපි ඔක්කොම clean කරනවා.
    // Fragment එකක View එකට අදාළ දේවල් අයින් කරන්න ඕනේ onDestroyView එකේදී.

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);
        }
    }
}