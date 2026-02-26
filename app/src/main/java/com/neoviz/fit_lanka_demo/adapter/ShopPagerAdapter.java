package com.neoviz.fit_lanka_demo.adapter;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.neoviz.fit_lanka_demo.fragment.CartFragment;
import com.neoviz.fit_lanka_demo.fragment.OrdersFragment;
import com.neoviz.fit_lanka_demo.fragment.ProductListFragment;
import com.neoviz.fit_lanka_demo.fragment.WishlistFragment;

public class ShopPagerAdapter extends FragmentStateAdapter {
    public ShopPagerAdapter(@NonNull Fragment fragment) {
        super(fragment);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {

        switch (position){
            case 0: return new ProductListFragment();
            case 1: return new WishlistFragment();
            case 2: return new CartFragment();
            default: return new OrdersFragment();
        }
    }

    @Override
    public int getItemCount() {
        return 4;
    }
}
