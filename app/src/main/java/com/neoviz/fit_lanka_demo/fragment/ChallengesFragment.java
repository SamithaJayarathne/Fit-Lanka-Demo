package com.neoviz.fit_lanka_demo.fragment;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.neoviz.fit_lanka_demo.adapter.ChallengeAdapter;
import com.neoviz.fit_lanka_demo.databinding.FragmentChallengesBinding;
import com.neoviz.fit_lanka_demo.model.Challenge;
import com.neoviz.fit_lanka_demo.model.User;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ChallengesFragment extends Fragment {

    private FragmentChallengesBinding binding;
    private FirebaseFirestore firestore;
    private User currentUser;
    private String userId;
    private ChallengeAdapter adapter;
    private List<Challenge> challengeList = new ArrayList<>();
    private int todaySteps;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentChallengesBinding.inflate(inflater, container, false);
        firestore = FirebaseFirestore.getInstance();
        userId = FirebaseAuth.getInstance().getUid();

        // SharedPreferences වලින් අද පියවර ගණන ගන්නවා
        SharedPreferences prefs = requireActivity().getSharedPreferences("StepPrefs", Context.MODE_PRIVATE);
        todaySteps = prefs.getInt("currentSteps", 0);

        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // මේ මෙතඩ් දෙක මෙතනදී අනිවාර්යයෙන්ම Call විය යුතුයි
        setupRecyclerView();
        loadUserDataAndChallenges();
    }

    private void setupRecyclerView() {
        binding.challengesRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
    }

    private void loadUserDataAndChallenges() {
        if (userId == null) return;

        firestore.collection("users").document(userId).get().addOnSuccessListener(snapshot -> {
            currentUser = snapshot.toObject(User.class);
            if (currentUser != null) {
                fetchDailyChallenges();
            }
        });
    }

    private void fetchDailyChallenges() {
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        firestore.collection("challenges")
                .whereEqualTo("date", today)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    challengeList.clear();
                    for (DocumentSnapshot ds : querySnapshot) {
                        challengeList.add(ds.toObject(Challenge.class));
                    }

                    if (challengeList.isEmpty()) {
                        binding.tvNoChallenge.setVisibility(View.VISIBLE);
                    } else {
                        binding.tvNoChallenge.setVisibility(View.GONE);
                        // Adapter එකට method reference එකක් ලෙස onClaimClick එක දෙනවා
                        adapter = new ChallengeAdapter(challengeList, todaySteps,
                                currentUser.getCompletedChallenges(), this::onClaimClick);
                        binding.challengesRecyclerView.setAdapter(adapter);
                    }
                });
    }

    private void onClaimClick(Challenge challenge) {
        String challengeId = challenge.getDate() + "_" + challenge.getTargetSteps();
        DocumentReference userRef = firestore.collection("users").document(userId);

        firestore.runTransaction(transaction -> {
            DocumentSnapshot snapshot = transaction.get(userRef);
            User user = snapshot.toObject(User.class);

            if (user == null) return null;

            Map<String, Boolean> completed = user.getCompletedChallenges();
            if (completed.containsKey(challengeId)) return null;

            double newFitCoins = user.getFitCoins() + challenge.getRewardCoins();
            completed.put(challengeId, true);

            transaction.update(userRef, "fitCoins", newFitCoins);
            transaction.update(userRef, "completedChallenges", completed);
            return null;

        }).addOnSuccessListener(aVoid -> {
            Toast.makeText(getContext(), "Success! +" + challenge.getRewardCoins() + " Fit-Coins", Toast.LENGTH_SHORT).show();
            loadUserDataAndChallenges(); // UI එක Refresh කරනවා
        }).addOnFailureListener(e -> {
            Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}