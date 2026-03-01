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
import com.neoviz.fit_lanka_demo.databinding.FragmentHomeBinding;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HomeFragment extends Fragment implements SensorEventListener {

    private static final String TAG = "HomeFragment";
    private static final String STEPS_AT_REST_KEY = "stepsAtRest";
    private static final String LAST_SAVED_DATE_KEY = "lastDate";
    private static final String PREF_CURRENT_STEPS = "currentSteps";
    private static final String DAILY_STEP_TARGET_KEY = "dailyStepTarget";
    private static final Double METER_PER_STEP = 0.762;
    private static final Double CALORIES_PER_STEP = 0.04;
    private FragmentHomeBinding binding;
    private SensorManager sensorManager;
    private Sensor stepSensor;
    private int stepsAtReset = 0;
    private int currentSteps = 0;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        SharedPreferences prefs = requireActivity().getSharedPreferences("StepPrefs", Context.MODE_PRIVATE);
        prefs.edit().putInt(DAILY_STEP_TARGET_KEY, 5000).apply();

        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Runtime Permission Check
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACTIVITY_RECOGNITION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(),
                    new String[]{Manifest.permission.ACTIVITY_RECOGNITION}, 1);
        }

        sensorManager = (SensorManager) requireActivity().getSystemService(Context.SENSOR_SERVICE);
        stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);

        // 1. කලින් තිබුණු දත්ත UI එකට ලෝඩ් කරනවා (සෙන්සර් එක වැඩ කරන්න කලින් පරණ ගාන පෙන්වන්න)
        SharedPreferences prefs = requireActivity().getSharedPreferences("StepPrefs", Context.MODE_PRIVATE);

        /// set daily target from pref
        Integer dailyTarget = prefs.getInt(DAILY_STEP_TARGET_KEY, 0);

        if (dailyTarget != null) {
            binding.progressBar.setMax(dailyTarget);
        } else {
            binding.progressBar.setMax(5000);
        }

        loadSavedData();
        loadChart();

        if (stepSensor == null) {
            binding.tvStepCount.setText("Sensor Not Found");
            Log.e(TAG, "Step Counter Not Available on this device");
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (stepSensor != null) {
            sensorManager.registerListener(this, stepSensor, SensorManager.SENSOR_DELAY_UI);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
        // ඇප් එක Pause වෙන වෙලාවෙත් අන්තිම පියවර ගාන සේව් කරමු
        saveCurrentSteps();
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_STEP_COUNTER) {
            float totalStepsSinceBoot = event.values[0];

            // 2. Daily Reset Logic
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            String currentDate = sdf.format(new Date());

            SharedPreferences prefs = requireActivity().getSharedPreferences("StepPrefs", Context.MODE_PRIVATE);
            String lastDate = prefs.getString(LAST_SAVED_DATE_KEY, "");

            if (!currentDate.equals(lastDate)) {
                stepsAtReset = Math.round(totalStepsSinceBoot);
                SharedPreferences.Editor editor = prefs.edit();
                editor.putInt(STEPS_AT_REST_KEY, stepsAtReset);
                editor.putString(LAST_SAVED_DATE_KEY, currentDate);
                editor.putInt(PREF_CURRENT_STEPS, 0); // අලුත් දවසක පියවර 0 කරන්න
                editor.apply();
                Log.d(TAG, "New Day! Steps Reset.");
            }

            // 3. පියවර ගණනය කර UI එකට යැවීම
            currentSteps = Math.round(totalStepsSinceBoot) - stepsAtReset;
            updateUI(currentSteps);

            // හැමවෙලේම පියවර ගාන සේව් කරන එක හොඳයි (App crash වුණොත් එහෙම)
            saveCurrentSteps();
        }
    }

    private void loadSavedData() {
        SharedPreferences prefs = requireActivity().getSharedPreferences("StepPrefs", Context.MODE_PRIVATE);
        stepsAtReset = prefs.getInt(STEPS_AT_REST_KEY, 0);
        currentSteps = prefs.getInt(PREF_CURRENT_STEPS, 0);

        // සෙන්සර් එකෙන් update එකක් එනකම් පරණ data පෙන්වනවා
        updateUI(currentSteps);
    }

    private void saveCurrentSteps() {
        SharedPreferences prefs = requireActivity().getSharedPreferences("StepPrefs", Context.MODE_PRIVATE);
        prefs.edit().putInt(PREF_CURRENT_STEPS, currentSteps).apply();
    }

    private void updateUI(int stepsCount) {
        if (binding != null) {
            binding.tvStepCount.setText(String.valueOf(stepsCount));
            binding.progressBar.setProgress(stepsCount);

            /// calculate distance
            double distance = (stepsCount * METER_PER_STEP) / 1000;
            binding.tvDistance.setText(String.format("%.2f km", distance));

            /// calculate calories
            double calories = stepsCount * CALORIES_PER_STEP;
            binding.tvCalories.setText(String.format("%.0f kcal", calories));

            /// update the current day stats of chart
            if (binding.stepChart.getData() != null && binding.stepChart.getData().getDataSetCount() >0) {

                BarDataSet set = (BarDataSet) binding.stepChart.getData().getDataSetByIndex(0);
                set.getEntryForIndex(6).setY(stepsCount);
                binding.stepChart.notifyDataSetChanged();
                binding.stepChart.invalidate();
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    /// dummy data for the chart
    private void loadChart() {
        List<BarEntry> entries = new ArrayList<>();

        entries.add(new BarEntry(0, 3200));
        entries.add(new BarEntry(1, 4100));
        entries.add(new BarEntry(2, 2500));
        entries.add(new BarEntry(3, 5600));
        entries.add(new BarEntry(4, 4800));
        entries.add(new BarEntry(5, 7780));
        entries.add(new BarEntry(6, currentSteps));

        BarDataSet dataSet = new BarDataSet(entries, "Weekly Steps");
        dataSet.setColor(Color.parseColor("#57F4FA"));
        dataSet.setValueTextColor(Color.GRAY);
        dataSet.setValueTextSize(10f);

        BarData barData = new BarData(dataSet);
        barData.setBarWidth(0.6f);

        binding.stepChart.setData(barData);

        binding.stepChart.getAxisLeft().setAxisMinimum(0f);
        binding.stepChart.getAxisRight().setEnabled(false);
        binding.stepChart.getXAxis().setPosition(com.github.mikephil.charting.components.XAxis.XAxisPosition.BOTTOM);
        binding.stepChart.getXAxis().setDrawGridLines(false);
        binding.stepChart.getDescription().setEnabled(false);

        binding.stepChart.animateY(1000);
        binding.stepChart.invalidate();
    }
}