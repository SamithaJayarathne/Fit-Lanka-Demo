package com.neoviz.fit_lanka_demo.fragment;

import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.neoviz.fit_lanka_demo.R;
import com.neoviz.fit_lanka_demo.activity.HomeActivity;
import com.neoviz.fit_lanka_demo.adapter.ProductImageSliderAdapter;
import com.neoviz.fit_lanka_demo.databinding.FragmentProductDetailsBinding;
import com.neoviz.fit_lanka_demo.model.Product;
import com.neoviz.fit_lanka_demo.model.User;

import java.util.List;

public class ProductDetailsFragment extends Fragment {

    private Product selectedProduct;
    private FragmentProductDetailsBinding binding;
    private FirebaseFirestore firestore;
    private FirebaseAuth firebaseAuth;
    private User user;
    private int currentQuantity = 1;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            selectedProduct = (Product) getArguments().getSerializable("selectedProduct");
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentProductDetailsBinding.inflate(inflater, container, false);
        firestore = FirebaseFirestore.getInstance();
        firebaseAuth = FirebaseAuth.getInstance();

        if (firebaseAuth.getCurrentUser() == null) {
            requireActivity().finish();
        }

        String uid = firebaseAuth.getCurrentUser().getUid();
        firestore.collection("users").document(uid).get().addOnSuccessListener(documentSnapshot -> {
            user = documentSnapshot.toObject(User.class);
            if (user != null) {
                calculatePricing();
            }
        });

        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (selectedProduct == null) return;

        ProductImageSliderAdapter adapter = new ProductImageSliderAdapter(selectedProduct.getImages());
        binding.productDetailsProductImageSlider.setAdapter(adapter);
        binding.detailsWormDotsIndicator.attachTo(binding.productDetailsProductImageSlider);

        binding.detailsProductName.setText(selectedProduct.getName());
        binding.detailsCategoryName.setText(selectedProduct.getCategoryName());
        binding.detailsProductDescription.setText(selectedProduct.getDescription());
        binding.detailsRatingBar.setRating(selectedProduct.getRating());
        binding.detailsProductRatingValue.setText(String.valueOf(selectedProduct.getRating()));
        binding.detailsAvailableStock.setText("Available Stock: " + selectedProduct.getStockQty() + " Units");

        if (selectedProduct.getMaxPercentage() != null && selectedProduct.getMaxPercentage() > 0) {
            binding.detailsDiscountBadge.setVisibility(View.VISIBLE);
            binding.detailsDiscountTagText.setText(String.format("%.0f%% OFF", selectedProduct.getMaxPercentage()));
        } else {
            binding.detailsDiscountBadge.setVisibility(View.GONE);
        }


        binding.btnIncreaseQuantity.setOnClickListener(v -> {
            if (currentQuantity < selectedProduct.getStockQty()) {
                currentQuantity++;
                updateQuantityUI();
            }
        });

        binding.btnDecreaseQuantity.setOnClickListener(v -> {
            if (currentQuantity > 1) {
                currentQuantity--;
                updateQuantityUI();
            }
        });

        if (selectedProduct.getAttributes() != null) {
            renderAttributes(selectedProduct.getAttributes());
        }

        calculatePricing();


        /// buttons
        binding.detailsFavoriteButton.setOnClickListener(v -> {

        });

        binding.detailsAddToCartIconButton.setOnClickListener(v -> {

        });

        binding.detailsBuyNowButton.setOnClickListener(v -> {

        });
    }

    private void updateQuantityUI() {
        binding.tvQuantityCount.setText(String.valueOf(currentQuantity));
        calculatePricing();
    }

    private void calculatePricing() {
        if (selectedProduct == null) return;

        double unitPrice = selectedProduct.getPrice();
        double totalOriginalPrice = unitPrice * currentQuantity;

        binding.detailsBottomPrice.setText(String.format("Rs. %,.2f", totalOriginalPrice));

        if (user != null) {
            float maxPercent = selectedProduct.getMaxPercentage() != null ? selectedProduct.getMaxPercentage() : 0;
            double maxAllowedDiscount = totalOriginalPrice * (maxPercent / 100.0);
            double userCoins = user.getFitCoins();

            double applicableDiscount = Math.min(userCoins, maxAllowedDiscount);

            if (applicableDiscount > 0) {
                binding.detailsOriginalPrice.setVisibility(View.VISIBLE);
                binding.detailsOriginalPrice.setText(String.format("Rs. %,.2f", totalOriginalPrice));
                binding.detailsOriginalPrice.setPaintFlags(binding.detailsOriginalPrice.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);

                double discountedPrice = totalOriginalPrice - applicableDiscount;
                binding.detailsProductPrice.setText(String.format("Rs. %,.2f", discountedPrice));


                binding.detailsCoinOfferCard.setVisibility(View.VISIBLE);
                binding.detailsCoinMessage.setText(String.format("Use %,.0f Fit Coins to save Rs. %,.0f", applicableDiscount, applicableDiscount));
            } else {
                hideDiscountUI(totalOriginalPrice);
            }
        } else {
            hideDiscountUI(totalOriginalPrice);
        }
    }

    private void hideDiscountUI(double originalPrice) {
        binding.detailsOriginalPrice.setVisibility(View.GONE);
        binding.detailsProductPrice.setText(String.format("Rs. %,.2f", originalPrice));
        binding.detailsCoinOfferCard.setVisibility(View.GONE);
    }

    private void renderAttributes(List<Product.Attribute> attributes) {
        binding.detailsAttributesContainer.removeAllViews();
        for (Product.Attribute attribute : attributes) {
            TextView title = new TextView(getContext());
            title.setText("Select " + attribute.getName());
            title.setTypeface(null, Typeface.BOLD);
            title.setPadding(0, 32, 0, 16);

            ChipGroup chipGroup = new ChipGroup(getContext());
            chipGroup.setSingleSelection(true);
            chipGroup.setSelectionRequired(true);

            for (String value : attribute.getValues()) {
                Chip chip = new Chip(getContext());
                chip.setText(value);
                chip.setCheckable(true);
                chipGroup.addView(chip);
            }
            binding.detailsAttributesContainer.addView(title);
            binding.detailsAttributesContainer.addView(chipGroup);
        }
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