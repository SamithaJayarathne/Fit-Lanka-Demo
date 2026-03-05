package com.neoviz.fit_lanka_demo.fragment;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.google.android.material.chip.Chip;
import com.neoviz.fit_lanka_demo.R;
import com.neoviz.fit_lanka_demo.adapter.FeaturedProductAdapter;
import com.neoviz.fit_lanka_demo.adapter.NewArrivalsProductAdapter;
import com.neoviz.fit_lanka_demo.adapter.TopDiscountProductAdapter;
import com.neoviz.fit_lanka_demo.databinding.FragmentProductListBinding;
import com.neoviz.fit_lanka_demo.model.Category;
import com.neoviz.fit_lanka_demo.model.Product;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ProductListFragment extends Fragment {

    private FragmentProductListBinding binding;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentProductListBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        loadCategories();
        loadNewArrivals();
        loadFeaturedProducts();
        loadTopDiscountedProducts();
    }

    private void loadCategories() {
        List<String> categories = Arrays.asList("All", "Equipments", "Supplements", "Apparel", "Accessories", "Footwear", "Sports Equipment", "Fitness Equipment", "Gym Equipment");
        binding.categoryChipGroup.removeAllViews();

        for (String category : categories) {
            Chip chip = new Chip(getContext());
            chip.setText(category);
            chip.setCheckable(true);
            chip.setClickable(true);

            if(category.equals("All")){
                chip.setChecked(true);
            }

            chip.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if(isChecked){
                    String text = chip.getText().toString();
                    Toast.makeText(getContext(), text, Toast.LENGTH_SHORT).show();
                }
            });

            binding.categoryChipGroup.addView(chip);

        }
    }

    private void loadTopDiscountedProducts() {
        List<Product> products = new ArrayList<>();

        // 1. Whey Protein
        products.add(Product.builder()
                .productId("P001")
                .name("Optimum Nutrition Whey")
                .description("High-quality protein powder for muscle recovery.")
                .price(18500.0)
                .images(List.of("https://images.unsplash.com/photo-1592262044987-497503fcdaa4?q=80&w=687&auto=format&fit=crop&ixlib=rb-4.1.0&ixid=M3wxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8fA%3D%3D"))
                .stockQty(25)
                .isNewArrival(true)
                .build());

        // 2. Gym Gloves
        products.add(Product.builder()
                .productId("P002")
                .name("Elite Gym Gloves")
                .description("Breathable workout gloves with wrist support.")
                .price(3200.0)
                .images(List.of("https://images.unsplash.com/photo-1557127972-1c446ea89ea5?q=80&w=687&auto=format&fit=crop&ixlib=rb-4.1.0&ixid=M3wxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8fA%3D%3D"))
                .stockQty(15)
                .isNewArrival(true)
                .build());

        // 3. Dumbbell Set
        products.add(Product.builder()
                .productId("P003")
                .name("Adjustable Dumbbells")
                .description("Perfect for home workouts, adjustable weight up to 20kg.")
                .price(12500.0)
                .images(List.of("https://images.unsplash.com/photo-1638536532686-d610adfc8e5c?q=80&w=500"))
                .stockQty(8)
                .isNewArrival(true)
                .build());

        // 4. Pre-Workout Drink
        products.add(Product.builder()
                .productId("P004")
                .name("C4 Original Pre-Workout")
                .description("Explosive energy and focus for your training sessions.")
                .price(9800.0)
                .images(List.of("https://images.unsplash.com/photo-1546483875-ad9014c88eba?q=80&w=500"))
                .stockQty(20)
                .isNewArrival(true)
                .build());

        // 5. Resistance Bands
        products.add(Product.builder()
                .productId("P005")
                .name("Resistance Band Set")
                .description("5-piece set for full-body stretching and strength.")
                .price(2800.0)
                .images(List.of("https://images.unsplash.com/photo-1598289431512-b97b0917affc?q=80&w=500"))
                .stockQty(50)
                .isNewArrival(true)
                .build());

        LinearLayoutManager layoutManager = new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false);
        binding.topDiscountedRecyclerView.setLayoutManager(layoutManager);

        TopDiscountProductAdapter adapter = new TopDiscountProductAdapter(products, product -> {

        });

        binding.topDiscountedRecyclerView.setAdapter(adapter);
    }

    private void loadFeaturedProducts() {
        List<Product> products = new ArrayList<>();

        // 1. Whey Protein
        products.add(Product.builder()
                .productId("P001")
                .name("Optimum Nutrition Whey")
                .description("High-quality protein powder for muscle recovery.")
                .price(18500.0)
                .images(List.of("https://images.unsplash.com/photo-1592262044987-497503fcdaa4?q=80&w=687&auto=format&fit=crop&ixlib=rb-4.1.0&ixid=M3wxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8fA%3D%3D"))
                .stockQty(25)
                .isNewArrival(true)
                .build());

        // 2. Gym Gloves
        products.add(Product.builder()
                .productId("P002")
                .name("Elite Gym Gloves")
                .description("Breathable workout gloves with wrist support.")
                .price(3200.0)
                .images(List.of("https://images.unsplash.com/photo-1557127972-1c446ea89ea5?q=80&w=687&auto=format&fit=crop&ixlib=rb-4.1.0&ixid=M3wxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8fA%3D%3D"))
                .stockQty(15)
                .isNewArrival(true)
                .build());

        // 3. Dumbbell Set
        products.add(Product.builder()
                .productId("P003")
                .name("Adjustable Dumbbells")
                .description("Perfect for home workouts, adjustable weight up to 20kg.")
                .price(12500.0)
                .images(List.of("https://images.unsplash.com/photo-1638536532686-d610adfc8e5c?q=80&w=500"))
                .stockQty(8)
                .isNewArrival(true)
                .build());

        // 4. Pre-Workout Drink
        products.add(Product.builder()
                .productId("P004")
                .name("C4 Original Pre-Workout")
                .description("Explosive energy and focus for your training sessions.")
                .price(9800.0)
                .images(List.of("https://images.unsplash.com/photo-1546483875-ad9014c88eba?q=80&w=500"))
                .stockQty(20)
                .isNewArrival(true)
                .build());

        // 5. Resistance Bands
        products.add(Product.builder()
                .productId("P005")
                .name("Resistance Band Set")
                .description("5-piece set for full-body stretching and strength.")
                .price(2800.0)
                .images(List.of("https://images.unsplash.com/photo-1598289431512-b97b0917affc?q=80&w=500"))
                .stockQty(50)
                .isNewArrival(true)
                .build());

        LinearLayoutManager layoutManager = new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false);
        binding.featuredRecyclerView.setLayoutManager(layoutManager);

        FeaturedProductAdapter adapter = new FeaturedProductAdapter(products, product -> {

        });

        binding.featuredRecyclerView.setAdapter(adapter);


    }

    private void loadNewArrivals() {

        List<Product> products = new ArrayList<>();

        // 1. Whey Protein
        products.add(Product.builder()
                .productId("P001")
                .name("Optimum Nutrition Whey")
                .description("High-quality protein powder for muscle recovery.")
                .price(18500.0)
                .images(List.of("https://images.unsplash.com/photo-1592262044987-497503fcdaa4?q=80&w=687&auto=format&fit=crop&ixlib=rb-4.1.0&ixid=M3wxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8fA%3D%3D"))
                .stockQty(25)
                .isNewArrival(true)
                .build());

        // 2. Gym Gloves
        products.add(Product.builder()
                .productId("P002")
                .name("Elite Gym Gloves")
                .description("Breathable workout gloves with wrist support.")
                .price(3200.0)
                .images(List.of("https://images.unsplash.com/photo-1557127972-1c446ea89ea5?q=80&w=687&auto=format&fit=crop&ixlib=rb-4.1.0&ixid=M3wxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8fA%3D%3D"))
                .stockQty(15)
                .isNewArrival(true)
                .build());

        // 3. Dumbbell Set
        products.add(Product.builder()
                .productId("P003")
                .name("Adjustable Dumbbells")
                .description("Perfect for home workouts, adjustable weight up to 20kg.")
                .price(12500.0)
                .images(List.of("https://images.unsplash.com/photo-1638536532686-d610adfc8e5c?q=80&w=500"))
                .stockQty(8)
                .isNewArrival(true)
                .build());

        // 4. Pre-Workout Drink
        products.add(Product.builder()
                .productId("P004")
                .name("C4 Original Pre-Workout")
                .description("Explosive energy and focus for your training sessions.")
                .price(9800.0)
                .images(List.of("https://images.unsplash.com/photo-1546483875-ad9014c88eba?q=80&w=500"))
                .stockQty(20)
                .isNewArrival(true)
                .build());

        // 5. Resistance Bands
        products.add(Product.builder()
                .productId("P005")
                .name("Resistance Band Set")
                .description("5-piece set for full-body stretching and strength.")
                .price(2800.0)
                .images(List.of("https://images.unsplash.com/photo-1598289431512-b97b0917affc?q=80&w=500"))
                .stockQty(50)
                .isNewArrival(true)
                .build());

        LinearLayoutManager layoutManager = new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false);
        binding.newArrivalsRecyclerView.setLayoutManager(layoutManager);

        NewArrivalsProductAdapter adapter = new NewArrivalsProductAdapter(products, product -> {

        });

        binding.newArrivalsRecyclerView.setAdapter(adapter);


    }


    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}