package com.neoviz.fit_lanka_demo.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.google.android.material.tabs.TabLayoutMediator;
import com.neoviz.fit_lanka_demo.databinding.FragmentLeaderboardBinding;

public class LeaderboardFragment extends Fragment {

    private FragmentLeaderboardBinding binding;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentLeaderboardBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 1. Adapter එක සෙට් කරනවා
        LeaderboardPagerAdapter pagerAdapter = new LeaderboardPagerAdapter(this);
        binding.viewPager2.setAdapter(pagerAdapter);

        // 2. TabLayout එකයි ViewPager එකයි යා කරනවා (Swipe Magic)
        new TabLayoutMediator(binding.tabLayout, binding.viewPager2, (tab, position) -> {
            if (position == 0) {
                tab.setText("Weekly");
            } else {
                tab.setText("Monthly");
            }
        }).attach();
    }

    // ViewPager එකට අදාළ Adapter එක (Inner Class එකක් විදිහට ලියමු)
    private static class LeaderboardPagerAdapter extends FragmentStateAdapter {
        public LeaderboardPagerAdapter(@NonNull Fragment fragment) {
            super(fragment);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            // 0 නම් weeklySteps, 1 නම් monthlySteps කියලා Child Fragment එකට යවනවා
            String type = (position == 0) ? "weeklySteps" : "monthlySteps";
            return LeaderboardListFragment.newInstance(type);
        }

        @Override
        public int getItemCount() {
            return 2; // Tabs 2යි (Weekly & Monthly)
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}