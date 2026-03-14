package com.neoviz.fit_lanka_demo.fragment;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.neoviz.fit_lanka_demo.R;
import com.neoviz.fit_lanka_demo.activity.HomeActivity;

public class ProductDetailsFragment extends Fragment {

        @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_product_details, container, false);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (requireActivity() instanceof HomeActivity) {
            ((HomeActivity) requireActivity()).setNavigationVisibility(View.GONE);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (requireActivity() instanceof HomeActivity) {
            ((HomeActivity) requireActivity()).setNavigationVisibility(View.VISIBLE);
        }
    }
}