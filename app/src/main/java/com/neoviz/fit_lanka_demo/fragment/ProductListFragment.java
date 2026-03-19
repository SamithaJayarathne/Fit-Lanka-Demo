package com.neoviz.fit_lanka_demo.fragment;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.LinearSnapHelper;
import androidx.recyclerview.widget.SnapHelper;

import com.google.android.material.chip.Chip;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.neoviz.fit_lanka_demo.R;
import com.neoviz.fit_lanka_demo.adapter.ProductListingAdapter;
import com.neoviz.fit_lanka_demo.databinding.FragmentProductListBinding;
import com.neoviz.fit_lanka_demo.model.Category;
import com.neoviz.fit_lanka_demo.model.Product;

import java.util.ArrayList;
import java.util.List;

public class ProductListFragment extends Fragment {

    private FragmentProductListBinding binding;
    private FirebaseFirestore firestore;
    private List<Product> products;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        firestore = FirebaseFirestore.getInstance();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentProductListBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setupLayoutManagers();

        loadCategories();
        loadProducts();


        binding.listingProductSearchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String searchText = s.toString().trim();
                if (searchText.isEmpty()) {
                    showDefaultUI();
                } else {
                    handleSearch(searchText);
                }
            }
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void setupLayoutManagers() {

        binding.newArrivalsRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        binding.topDiscountedRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        binding.featuredRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        binding.forYouRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));

        binding.searchProductRecyclerView.setLayoutManager(new GridLayoutManager(requireContext(), 2));
        binding.categoryProductRecyclerView.setLayoutManager(new GridLayoutManager(requireContext(), 2));
    }

    private void loadProducts() {
        firestore.collection("products")
                .get()
                .addOnSuccessListener(task -> {
                    if (task.isEmpty()) return;

                    products = task.toObjects(Product.class);
                    List<Product> newArrivals = new ArrayList<>();
                    List<Product> featured = new ArrayList<>();
                    List<Product> topDiscounted = new ArrayList<>();

                    for (Product product : products) {
                        if (product.isNewArrival()) newArrivals.add(product);
                        if (product.isFeatured()) featured.add(product);
                        if (product.isTopDiscounted()) topDiscounted.add(product);
                    }



                    if (!newArrivals.isEmpty()) {
                        binding.newArrivalsContainer.setVisibility(View.VISIBLE);
                        SnapHelper snapHelper = new LinearSnapHelper();
                        snapHelper.attachToRecyclerView(binding.newArrivalsRecyclerView);
                        binding.newArrivalsRecyclerView.setAdapter(new ProductListingAdapter(newArrivals, this::loadSingleProduct));
                    }
                    if (!topDiscounted.isEmpty()) {
                        binding.topDiscountedContainer.setVisibility(View.VISIBLE);
                        SnapHelper snapHelper = new LinearSnapHelper();
                        snapHelper.attachToRecyclerView(binding.topDiscountedRecyclerView);
                        binding.topDiscountedRecyclerView.setAdapter(new ProductListingAdapter(topDiscounted, this::loadSingleProduct));
                    }
                    if (!featured.isEmpty()) {
                        binding.featuredContainer.setVisibility(View.VISIBLE);
                        SnapHelper snapHelper = new LinearSnapHelper();
                        snapHelper.attachToRecyclerView(binding.featuredRecyclerView);
                        binding.featuredRecyclerView.setAdapter(new ProductListingAdapter(featured, this::loadSingleProduct));
                    }


                    SnapHelper snapHelper = new LinearSnapHelper();
                    snapHelper.attachToRecyclerView(binding.forYouRecyclerView);
                    binding.forYouRecyclerView.setAdapter(new ProductListingAdapter(products, this::loadSingleProduct));
                });
    }

    private void loadSingleProduct(Product product){

        ProductDetailsFragment fragment = new ProductDetailsFragment();

        Bundle bundle = new Bundle();
        bundle.putSerializable("selectedProduct", product);

        fragment.setArguments(bundle);

        requireActivity().getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container_view, fragment)
                .addToBackStack(null)
                .commit();

    }

    private void loadCategories() {
        firestore.collection("categories")
                .orderBy("name", Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(task -> {
                    // 1. Fragment එක විනාශ වෙලා නම් (binding null නම්) ඉතිරි ටික කරන්න එපා
                    if (binding == null) return;

                    List<Category> categories = task.toObjects(Category.class);
                    // "All" category එක මුලට ඇඩ් කිරීම
                    categories.add(0, Category.builder().categoryId("All").name("All").build());

                    binding.categoryChipGroup.removeAllViews();

                    for (Category category : categories) {
                        // 2. Context එක null ද කියලා බලන්න (Safe check)
                        if (getContext() == null) continue;

                        Chip chip = new Chip(getContext());
                        chip.setText(category.getName());
                        chip.setTag(category.getCategoryId());
                        chip.setCheckable(true);
                        chip.setClickable(true);

                        // Icons setup (මෙහිදී icon එක drawable එකේ තියෙනවා නේද කියලා චෙක් කරගන්න)
                        chip.setCheckedIconResource(R.drawable.check);
                        chip.setCheckedIconVisible(true);

                        if (category.getName().equals("All")) {
                            chip.setChecked(true);
                        }

                        chip.setOnCheckedChangeListener((buttonView, isChecked) -> {
                            // 3. Chip එකක් click කරද්දීත් binding එක තියෙනවද කියලා බලන එක ආරක්ෂිතයි
                            if (isChecked && binding != null) {
                                String currentSearch = binding.listingProductSearchInput.getText().toString();
                                if (!currentSearch.isEmpty()) {
                                    binding.listingProductSearchInput.setText("");
                                }
                                loadCategoryProducts(category.getCategoryId());
                            }
                        });

                        binding.categoryChipGroup.addView(chip);
                    }
                })
                .addOnFailureListener(e -> {
                    // Error එකක් ආවොත් log එකක් දාමු
                    Log.e("ProductListFragment", "Categories loading failed: " + e.getMessage());
                });
    }

    private void loadCategoryProducts(String categoryId) {
        if (categoryId.equals("All")) {
            showDefaultUI();
            return;
        }

        showCategoryUI();

        firestore.collection("products")
                .whereEqualTo("categoryId", categoryId)
                .get()
                .addOnSuccessListener(task -> {
                    List<Product> catProducts = task.toObjects(Product.class);
                    binding.categoryProductRecyclerView.setAdapter(new ProductListingAdapter(catProducts, p -> {}));
                });
    }

    private void handleSearch(String query) {
        if (products == null) return;
        List<Product> filtered = new ArrayList<>();
        for (Product p : products) {
            if (p.getName().toLowerCase().contains(query.toLowerCase())) {
                filtered.add(p);
            }
        }
        showSearchUI();
        binding.searchProductRecyclerView.setAdapter(new ProductListingAdapter(filtered, p -> {}));
    }

    private void showDefaultUI() {
        binding.searchProductRecyclerView.setVisibility(View.GONE);
        binding.categoryProductRecyclerView.setVisibility(View.GONE);

        binding.chipGroupContainer.setVisibility(View.VISIBLE);
        binding.productListingSection.setVisibility(View.VISIBLE);
        binding.defaultProductListingContainer.setVisibility(View.VISIBLE);
    }

    private void showCategoryUI() {
        binding.searchProductRecyclerView.setVisibility(View.GONE);
        binding.defaultProductListingContainer.setVisibility(View.GONE);

        binding.chipGroupContainer.setVisibility(View.VISIBLE);
        binding.productListingSection.setVisibility(View.VISIBLE);
        binding.categoryProductRecyclerView.setVisibility(View.VISIBLE);
    }

    private void showSearchUI() {
        binding.chipGroupContainer.setVisibility(View.GONE);
        binding.productListingSection.setVisibility(View.GONE);

        binding.searchProductRecyclerView.setVisibility(View.VISIBLE);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}