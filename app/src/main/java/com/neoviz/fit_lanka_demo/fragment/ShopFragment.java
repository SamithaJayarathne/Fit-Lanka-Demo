package com.neoviz.fit_lanka_demo.fragment;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.neoviz.fit_lanka_demo.R;
import com.neoviz.fit_lanka_demo.adapter.ShopPagerAdapter;
import com.neoviz.fit_lanka_demo.databinding.FragmentShopBinding;

public class ShopFragment extends Fragment {

    private FragmentShopBinding binding;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {

        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ViewPager2 shopViewPager = binding.shopViewPager;
        TabLayout shopTabLayout = binding.shopTabLayout;

        ShopPagerAdapter adapter = new ShopPagerAdapter(this);
        shopViewPager.setAdapter(adapter);

        new TabLayoutMediator(shopTabLayout, shopViewPager,(tab, position)->{
            switch (position){
                case 0:
                    tab.setText("Products");
                    break;
                case 1:
                    tab.setText("Wishlist");
                    break;
                case 2:
                    tab.setText("Cart");
                    break;
                case 3:
                    tab.setText("Orders");
                    break;
            }
        }).attach();

        if(getArguments()!=null){
            int targetTab = getArguments().getInt("targetTab", 0);

            shopViewPager.post(()->{
                shopViewPager.setCurrentItem(targetTab, true);
            });
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentShopBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }
}