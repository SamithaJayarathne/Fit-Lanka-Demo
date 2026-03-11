package com.neoviz.fit_lanka_demo.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.neoviz.fit_lanka_demo.adapter.LeaderboardAdapter;
import com.neoviz.fit_lanka_demo.databinding.FragmentLeaderboardListBinding;
import com.neoviz.fit_lanka_demo.model.User;

import java.util.ArrayList;
import java.util.List;

public class LeaderboardListFragment extends Fragment {

    private FragmentLeaderboardListBinding binding;
    private String stepType;
    private List<User> userList = new ArrayList<>();
    private LeaderboardAdapter adapter;

    public static LeaderboardListFragment newInstance(String stepType) {
        LeaderboardListFragment fragment = new LeaderboardListFragment();
        Bundle args = new Bundle();
        args.putString("STEP_TYPE", stepType);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            stepType = getArguments().getString("STEP_TYPE");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentLeaderboardListBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        adapter = new LeaderboardAdapter(userList, stepType);
        binding.rvLeaderboard.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.rvLeaderboard.setAdapter(adapter);

        loadData();
    }

    private void loadData() {
        binding.progressBar.setVisibility(View.VISIBLE);

        FirebaseFirestore.getInstance().collection("users")
                .orderBy(stepType, Query.Direction.DESCENDING)
                .limit(50)
                .get()
                .addOnSuccessListener(qds -> {
                    userList.clear();
                    for (QueryDocumentSnapshot ds : qds) {
                        userList.add(ds.toObject(User.class));
                    }
                    adapter.notifyDataSetChanged();
                    binding.progressBar.setVisibility(View.GONE);
                })
                .addOnFailureListener(e -> {
                    binding.progressBar.setVisibility(View.GONE);
                    Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}