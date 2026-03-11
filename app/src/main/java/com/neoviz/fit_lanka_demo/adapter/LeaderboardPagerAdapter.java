package com.neoviz.fit_lanka_demo.adapter;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.neoviz.fit_lanka_demo.fragment.LeaderboardListFragment;

public class LeaderboardPagerAdapter extends FragmentStateAdapter {
    public LeaderboardPagerAdapter(@NonNull Fragment fragment) {
        super(fragment);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        return LeaderboardListFragment.newInstance(position == 0 ? "weeklySteps" : "monthlySteps");
    }

    @Override
    public int getItemCount() {
        return 2;
    }
}
