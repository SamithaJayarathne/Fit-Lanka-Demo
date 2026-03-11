package com.neoviz.fit_lanka_demo.adapter;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.neoviz.fit_lanka_demo.R;
import com.neoviz.fit_lanka_demo.databinding.ItemLeaderboardBinding;
import com.neoviz.fit_lanka_demo.model.User;

import java.util.List;
import java.util.Locale;

public class LeaderboardAdapter extends RecyclerView.Adapter<LeaderboardAdapter.ViewHolder> {

    private List<User> userList;
    private String stepType;

    public LeaderboardAdapter(List<User> userList, String stepType) {
        this.userList = userList;
        this.stepType = stepType;
    }

    public void updateData(List<User> userList, String newStepType) {
        this.userList = userList;
        this.stepType = newStepType;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemLeaderboardBinding binding = ItemLeaderboardBinding
                .inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {

        User user = userList.get(position);
        holder.binding.tvRank.setText(String.valueOf(position + 1));
        holder.binding.tvName.setText(user.getName());

        if (position == 0) {
            holder.binding.tvRank.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#FFD700"))); // Gold
        } else if (position == 1) {
            holder.binding.tvRank.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#C0C0C0"))); // Silver
        } else if (position == 2) {
            holder.binding.tvRank.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#CD7F32"))); // Bronze
        } else {
            holder.binding.tvRank.setBackgroundTintList(ColorStateList.valueOf(holder.itemView.getContext().getColor(R.color.md_theme_tertiary)));
        }

        int steps = stepType.equals("weeklySteps") ? user.getWeeklySteps() : user.getMonthlySteps();
        holder.binding.tvSteps.setText(String.format(Locale.getDefault(), "%d", steps));

        if (user.getProfileImageUrl() != null && !user.getProfileImageUrl().isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(user.getProfileImageUrl())
                    .placeholder(R.drawable.profile_24px)
                    .centerCrop()
                    .into(holder.binding.ivProfile);

        }else{
            holder.binding.ivProfile.setImageResource(R.drawable.profile_24px);
        }

    }

    @Override
    public int getItemCount() {
        return userList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ItemLeaderboardBinding binding;

        public ViewHolder(@NonNull ItemLeaderboardBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
