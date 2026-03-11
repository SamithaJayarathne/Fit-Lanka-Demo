package com.neoviz.fit_lanka_demo.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.neoviz.fit_lanka_demo.databinding.ItemChallengeBinding;
import com.neoviz.fit_lanka_demo.model.Challenge;
import java.util.List;
import java.util.Map;

public class ChallengeAdapter extends RecyclerView.Adapter<ChallengeAdapter.ViewHolder> {

    private List<Challenge> challengeList;
    private int userCurrentSteps;
    private Map<String, Boolean> completedChallenges;
    private OnClaimClickListener onClaimClickListener;

    public interface OnClaimClickListener {
        void onClaimClick(Challenge challenge);
    }

    public ChallengeAdapter(List<Challenge> challengeList, int userCurrentSteps,
                            Map<String, Boolean> completedChallenges, OnClaimClickListener listener) {
        this.challengeList = challengeList;
        this.userCurrentSteps = userCurrentSteps;
        this.completedChallenges = completedChallenges;
        this.onClaimClickListener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemChallengeBinding binding = ItemChallengeBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Challenge challenge = challengeList.get(position);
        holder.binding.challengeTitle.setText(challenge.getTitle());
        holder.binding.challengeDescription.setText(challenge.getDescription());
        holder.binding.challengeReward.setText(String.format("Reward: %.2f Fit-Coins", challenge.getRewardCoins()));

        int targetSteps = challenge.getTargetSteps();
        holder.binding.challengeProgress.setMax(targetSteps);
        holder.binding.challengeProgress.setProgress(Math.min(userCurrentSteps, targetSteps));
        holder.binding.challengeProgressSteps.setText(userCurrentSteps + " / " + targetSteps + " steps");

        String challengeId = challenge.getDate() + "_" + challenge.getTargetSteps();
        boolean isCompleted = false;
        if (completedChallenges != null) {
            isCompleted = completedChallenges.containsKey(challengeId);
        }

        // UI Logic
        if (isCompleted) {
            holder.binding.challengeClaimBtn.setEnabled(false);
            holder.binding.challengeClaimBtn.setText("Completed");
        } else if (userCurrentSteps >= targetSteps) {
            holder.binding.challengeClaimBtn.setEnabled(true);
            holder.binding.challengeClaimBtn.setText("Claim Reward");
            // බටන් එකට Listener එක දානවා
            holder.binding.challengeClaimBtn.setOnClickListener(v -> {
                if (onClaimClickListener != null) {
                    onClaimClickListener.onClaimClick(challenge);
                }
            });
        } else {
            holder.binding.challengeClaimBtn.setEnabled(false);
            holder.binding.challengeClaimBtn.setText("In Progress");
        }
    }

    @Override
    public int getItemCount() {
        return challengeList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ItemChallengeBinding binding;
        public ViewHolder(@NonNull ItemChallengeBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}