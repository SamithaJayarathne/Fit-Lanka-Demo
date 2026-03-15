package com.neoviz.fit_lanka_demo.fragment;

import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.LinearSnapHelper;
import androidx.recyclerview.widget.SnapHelper;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.neoviz.fit_lanka_demo.R;
import com.neoviz.fit_lanka_demo.activity.HomeActivity;
import com.neoviz.fit_lanka_demo.adapter.ProductImageSliderAdapter;
import com.neoviz.fit_lanka_demo.adapter.ProductListingAdapter;
import com.neoviz.fit_lanka_demo.adapter.ReviewAdapter;
import com.neoviz.fit_lanka_demo.databinding.FragmentProductDetailsBinding;
import com.neoviz.fit_lanka_demo.model.CartItem;
import com.neoviz.fit_lanka_demo.model.Feedback;
import com.neoviz.fit_lanka_demo.model.Product;
import com.neoviz.fit_lanka_demo.model.User;
import com.neoviz.fit_lanka_demo.model.WishlistItem;

import java.util.ArrayList;
import java.util.List;

public class ProductDetailsFragment extends Fragment {

    private Product selectedProduct;
    private FragmentProductDetailsBinding binding;
    private FirebaseFirestore firestore;
    private FirebaseAuth firebaseAuth;
    private User user;
    private int currentQuantity = 1;
    private Boolean isFavorite = false;
    private List<CartItem.SelectedAttribute> selectedAttributes = new ArrayList<>();

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
                checkIsFavorite();
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

        /// related products
        loadRelatedProducts(selectedProduct.getCategoryId(), selectedProduct.getProductId());


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
        loadReviews(selectedProduct.getProductId());


        /// buttons
        binding.detailsFavoriteButton.setOnClickListener(v -> {
            toggleWishlist();
        });

        binding.detailsAddToCartIconButton.setOnClickListener(v -> {
            addToCart();
        });

        binding.detailsBuyNowButton.setOnClickListener(v -> {
            if (firebaseAuth.getCurrentUser() == null) return;

            if (selectedProduct.getAttributes() != null && selectedAttributes.size() < selectedProduct.getAttributes().size()) {
                Toast.makeText(getContext(), "Please select all options before buying", Toast.LENGTH_SHORT).show();
                return;
            }

            double unitPrice = selectedProduct.getPrice();
            double totalOriginalPrice = unitPrice * currentQuantity;
            float maxPercent = selectedProduct.getMaxPercentage() != null ? selectedProduct.getMaxPercentage() : 0;
            double maxAllowedDiscount = totalOriginalPrice * (maxPercent / 100.0);
            double userCoins = (user != null) ? user.getFitCoins() : 0;

            double applicableDiscount = Math.min(userCoins, maxAllowedDiscount);
            double grandTotal = (totalOriginalPrice + 500.0) - applicableDiscount; /// 500.0 is delivery fee

            CheckoutFragment checkoutFragment = new CheckoutFragment();
            Bundle bundle = new Bundle();

            bundle.putBoolean("isDirectBuy", true);
            bundle.putSerializable("directProduct", selectedProduct);
            bundle.putInt("directQuantity", currentQuantity);
            bundle.putSerializable("directAttributes", (ArrayList<CartItem.SelectedAttribute>) selectedAttributes);

            bundle.putDouble("finalTotal", grandTotal);
            bundle.putBoolean("isCoinsClaimed", applicableDiscount > 0);
            bundle.putInt("claimedCoinsAmount", (int) applicableDiscount);

            checkoutFragment.setArguments(bundle);

            requireActivity().getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container_view, checkoutFragment)
                    .addToBackStack(null)
                    .commit();
        });
        binding.detailsBackButton.setOnClickListener(v -> {
            requireActivity().getOnBackPressedDispatcher().onBackPressed();
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

        selectedAttributes.clear();

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

                chip.setOnCheckedChangeListener((buttonView, isChecked) -> {
                    removeExistingAttribute(attribute.getName());

                    selectedAttributes.add(new CartItem.SelectedAttribute(attribute.getName(), value));
                });

            }
            binding.detailsAttributesContainer.addView(title);
            binding.detailsAttributesContainer.addView(chipGroup);
        }
    }

    private void removeExistingAttribute(String attributeName) {
        selectedAttributes.removeIf(attribute -> attribute.getAttributeName().equals(attributeName));
    }

    private void checkIsFavorite() {
        if (selectedProduct == null || user == null) return;

        String uid = firebaseAuth.getCurrentUser().getUid();
        String productId = selectedProduct.getProductId();

        firestore.collection("users")
                .document(uid)
                .collection("wishlist")
                .document(productId)
                .addSnapshotListener((value, error) -> {
                    if (getActivity() == null) return;

                    if (value != null && value.exists()) {
                        if (!isFavorite) {
                            playPopAnimation(binding.detailsFavoriteButton);
                        }
                        isFavorite = true;
                        binding.detailsFavoriteButton.setImageResource(R.drawable.filled_favorite);
                    } else {
                        if (isFavorite) {
                            playPopAnimation(binding.detailsFavoriteButton);
                        }
                        isFavorite = false;
                        binding.detailsFavoriteButton.setImageResource(R.drawable.favorite_24px);
                    }
                });
    }

    private void toggleWishlist() {
        String uid = firebaseAuth.getCurrentUser().getUid();
        String productId = selectedProduct.getProductId();

        if (isFavorite) {
            firestore.collection("users")
                    .document(uid)
                    .collection("wishlist")
                    .document(productId)
                    .delete()
                    .addOnSuccessListener(aVoid ->
                            Toast.makeText(getContext(), "Removed from wishlist", Toast.LENGTH_SHORT).show());
        } else {
            WishlistItem item = new WishlistItem(
                    productId,
                    selectedProduct.getName(),
                    selectedProduct.getImages().get(0),
                    selectedProduct.getPrice(),
                    selectedProduct.getCategoryName(),
                    Timestamp.now()
            );

            firestore.collection("users")
                    .document(uid)
                    .collection("wishlist")
                    .document(productId)
                    .set(item)
                    .addOnSuccessListener(aVoid ->
                            Toast.makeText(getContext(), "Added to wishlist", Toast.LENGTH_SHORT).show());
        }
    }

    private void addToCart() {

        playPopAnimation(binding.detailsAddToCartIconButton);

        if (firebaseAuth.getCurrentUser() == null) return;

        if (selectedProduct.getAttributes() != null && selectedAttributes.size() < selectedProduct.getAttributes().size()) {
            Toast.makeText(getContext(), "Please select all options before add to cart", Toast.LENGTH_SHORT).show();
            return;
        }

        String uid = firebaseAuth.getCurrentUser().getUid();

        CartItem cartItem = CartItem.builder()
                .productId(selectedProduct.getProductId())
                .quantity(currentQuantity)
                .attributes(selectedAttributes)
                .addedAt(Timestamp.now())
                .build();

        /// unique id for cart item
        StringBuilder builder = new StringBuilder(selectedProduct.getProductId());

        Log.d("AddToCart", "Builder: "+builder.toString());

        for (CartItem.SelectedAttribute attribute : selectedAttributes) {
            builder.append("_").append(attribute.getAttributeValue());
        }

        String cartItemId = builder.toString();
        Log.d("AddToCart", "Cart Item Document ID: "+cartItemId);

        firestore.collection("users")
                .document(uid)
                .collection("cart")
                .document(cartItemId)
                .set(cartItem)
                .addOnSuccessListener(task -> {
                    Toast.makeText(getContext(), "Added to cart", Toast.LENGTH_SHORT).show();
                    resetUIAfterAddToCart();
                })
                .addOnFailureListener(error -> {
                    Toast.makeText(getContext(), "Oops!: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void resetUIAfterAddToCart() {
        currentQuantity = 1;
        updateQuantityUI();

        selectedAttributes.clear();

        if (selectedProduct.getAttributes() != null) {
            renderAttributes(selectedProduct.getAttributes());
        }

        calculatePricing();

        binding.detailsAttributesContainer
                .getParent()
                .requestChildFocus(binding.detailsAttributesContainer, binding.detailsAttributesContainer);
    }

    private void playPopAnimation(View view) {
        Animation anim = AnimationUtils.loadAnimation(getContext(), R.anim.pop_in);
        view.startAnimation(anim);
    }

    private void loadReviews(String productId) {
        firestore.collection("products").document(productId)
                .collection("reviews")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(5)
                .get()
                .addOnSuccessListener(qds -> {
                    List<Feedback> reviews = new ArrayList<>();
                    float totalRating = 0;

                    for (DocumentSnapshot doc : qds) {
                        Feedback f = doc.toObject(Feedback.class);
                        if (f != null) {
                            reviews.add(f);
                            totalRating += f.getRating();
                        }
                    }

                    if (reviews.isEmpty()) {
                        binding.tvNoReviews.setVisibility(View.VISIBLE);
                        binding.rvProductReviews.setVisibility(View.GONE);
                    } else {
                        binding.tvNoReviews.setVisibility(View.GONE);
                        binding.rvProductReviews.setVisibility(View.VISIBLE);


                        ReviewAdapter adapter = new ReviewAdapter(reviews);
                        binding.rvProductReviews.setLayoutManager(new LinearLayoutManager(getContext()));
                        binding.rvProductReviews.setAdapter(adapter);

                        float avg = totalRating / reviews.size();
                        binding.detailsRatingBar.setRating(avg);
                        binding.detailsProductRatingValue.setText("(" + String.format("%.1f", avg) + ")");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("ProductDetails", "Review load error: " + e.getMessage());
                });
    }

    private void loadRelatedProducts(String category, String currentProductId) {
        firestore.collection("products")
                .whereEqualTo("categoryId", category)
                .limit(10)
                .get()
                .addOnSuccessListener(qds -> {
                    List<Product> relatedList = new ArrayList<>();
                    for (DocumentSnapshot doc : qds) {
                        Product p = doc.toObject(Product.class);

                        if (p != null && !doc.getId().equals(currentProductId)) {
                            p.setProductId(doc.getId());
                            relatedList.add(p);
                        }
                    }

                    if (!relatedList.isEmpty()) {

                        ProductListingAdapter adapter = new ProductListingAdapter(relatedList, product -> {
                            openProductDetails(product.getProductId());
                        });


                        SnapHelper snapHelper = new LinearSnapHelper();
                        snapHelper.attachToRecyclerView(binding.rvRelatedProducts);

                        binding.rvRelatedProducts.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
                        binding.rvRelatedProducts.setAdapter(adapter);
                    }
                });
    }

    private void openProductDetails(String id) {
        Bundle b = new Bundle();
        b.putString("productId", id);
        ProductDetailsFragment fragment = new ProductDetailsFragment();
        fragment.setArguments(b);

        requireActivity().getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container_view, fragment)
                .addToBackStack(null)
                .commit();
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