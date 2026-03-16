package com.neoviz.fit_lanka_demo.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.neoviz.fit_lanka_demo.R;
import com.neoviz.fit_lanka_demo.databinding.FragmentSuccessBinding;

public class SuccessFragment extends Fragment {

    private FragmentSuccessBinding binding;

    public SuccessFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentSuccessBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getArguments() != null) {
            String orderId = getArguments().getString("orderId", "N/A");
            double total = getArguments().getDouble("total", 0.0);

            binding.tvOrderId.setText("#" + orderId);
            binding.tvOrderTotal.setText(String.format("Rs. %,.2f", total));
        }

        binding.btnBackToHome.setOnClickListener(v -> {
            requireActivity().getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container_view, new HomeFragment())
                    .commit();
        });

        binding.lottieSuccess.playAnimation();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null; // Memory leaks වළක්වන්න
    }
}