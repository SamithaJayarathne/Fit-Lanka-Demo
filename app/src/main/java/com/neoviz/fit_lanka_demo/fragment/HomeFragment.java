package com.neoviz.fit_lanka_demo.fragment;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.neoviz.fit_lanka_demo.databinding.FragmentHomeBinding;
import com.neoviz.fit_lanka_demo.model.User;
import com.neoviz.fit_lanka_demo.util.AppUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private SharedPreferences prefs;
    private List<User.DailySteps> historyData = new ArrayList<>();

    // Prefs වල අගය වෙනස් වන විට UI එක සහ Chart එක update කිරීම
    private final SharedPreferences.OnSharedPreferenceChangeListener prefsListener = (sharedPrefs, key) -> {
        if ("currentSteps".equals(key)) {
            int steps = sharedPrefs.getInt("currentSteps", 0);
            if (isAdded() && binding != null) {
                updateUI(steps);
                // පියවර වෙනස් වෙද්දී chart එකේ අද දවසත් update කරනවා
                loadChartWithRealData(historyData, steps);
            }
        }
    };

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        prefs = requireActivity().getSharedPreferences("StepPrefs", Context.MODE_PRIVATE);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        checkPermissions();

        int currentSteps = prefs.getInt("currentSteps", 0);
        updateUI(currentSteps);

        fetchUserDataFromFirestore(currentSteps);
    }

    private void checkPermissions() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACTIVITY_RECOGNITION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(), new String[]{Manifest.permission.ACTIVITY_RECOGNITION}, 1);
        }
    }

    private void fetchUserDataFromFirestore(int currentSteps) {
        if (auth.getCurrentUser() == null) return;

        db.collection("users").document(auth.getCurrentUser().getUid())
                .get().addOnSuccessListener(doc -> {
                    if (binding == null || !isAdded()) return;

                    User user = doc.toObject(User.class);
                    if (user != null) {
                        int target = user.getDailyStepTarget() > 0 ? user.getDailyStepTarget() : 5000;
                        binding.progressBar.setMax(target);

                        // ඉතිහාස දත්ත (History) save කරගන්නවා පස්සේ පාවිච්චි කරන්න
                        if (user.getDailySteps() != null) {
                            this.historyData = user.getDailySteps();
                        }
                        loadChartWithRealData(historyData, currentSteps);
                    }
                });
    }

    private void updateUI(int stepsCount) {
        if (binding == null) return;
        binding.tvStepCount.setText(String.valueOf(stepsCount));
        binding.progressBar.setProgress(stepsCount);

        double distance = (stepsCount * AppUtils.METER_PER_STEP) / 1000.0;
        binding.tvDistance.setText(String.format(Locale.getDefault(), "%.2f km", distance));

        double calories = stepsCount * AppUtils.CALORIES_PER_STEP;
        binding.tvCalories.setText(String.format(Locale.getDefault(), "%.0f kcal", calories));
    }

    private void loadChartWithRealData(List<User.DailySteps> history, int todaySteps) {
        if (binding == null || !isAdded()) return;

        ArrayList<BarEntry> entries = new ArrayList<>();
        float[] stepsArray = new float[7];

        // කලින් දින 6 ක දත්ත පිරවීම
        if (history != null && !history.isEmpty()) {
            int historySize = history.size();
            for (int i = 0; i < 6; i++) {
                int index = historySize - 1 - i;
                if (index >= 0) {
                    stepsArray[5 - i] = (float) history.get(index).getStepsCount();
                } else {
                    stepsArray[5 - i] = 0f;
                }
            }
        }

        // 7 වෙනි Bar එකට (අද දවස) දැනට තියෙන Steps දානවා
        stepsArray[6] = (float) todaySteps;

        for (int i = 0; i < 7; i++) {
            entries.add(new BarEntry(i, stepsArray[i]));
        }

        BarDataSet dataSet = new BarDataSet(entries, "Weekly Activity");
        dataSet.setColor(Color.parseColor("#57F4FA"));
        dataSet.setDrawValues(false);

        BarData barData = new BarData(dataSet);
        barData.setBarWidth(0.5f);

        binding.stepChart.setData(barData);

        // Chart එක refresh වෙන්න මේවා අනිවාර්යයි
        binding.stepChart.notifyDataSetChanged();

        // UI එක ලස්සනට තියාගන්න Configurations
        binding.stepChart.getDescription().setEnabled(false);
        binding.stepChart.getLegend().setEnabled(false);
        binding.stepChart.getAxisRight().setEnabled(false);
        binding.stepChart.getAxisLeft().setAxisMinimum(0f); // බින්දුවෙන් පටන් ගන්න

        XAxis xAxis = binding.stepChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f);

        binding.stepChart.invalidate(); // Chart එක රී-ඩ්‍රෝ කරනවා
    }

    @Override
    public void onResume() {
        super.onResume();
        prefs.registerOnSharedPreferenceChangeListener(prefsListener);
        updateUI(prefs.getInt("currentSteps", 0));
        loadChartWithRealData(historyData, prefs.getInt("currentSteps", 0));
    }

    @Override
    public void onPause() {
        super.onPause();
        prefs.unregisterOnSharedPreferenceChangeListener(prefsListener);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}