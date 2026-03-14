package com.neoviz.fit_lanka_demo.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.neoviz.fit_lanka_demo.R;
import com.neoviz.fit_lanka_demo.adapter.CartAdapter;
import com.neoviz.fit_lanka_demo.databinding.FragmentCartBinding;
import com.neoviz.fit_lanka_demo.model.CartItem;
import com.neoviz.fit_lanka_demo.model.Product;
import com.neoviz.fit_lanka_demo.model.User;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class CartFragment extends Fragment {

    private FragmentCartBinding binding;
    private FirebaseFirestore firestore;
    private FirebaseAuth firebaseAuth;
    private CartAdapter adapter;
    private List<CartItem> cartItems = new ArrayList<>();
    private User currentUser;
    private ListenerRegistration cartListener;

    private double subtotal = 0.0;
    private double deliveryFee = 500.0;
    private double totalMaxDiscountPossible = 0.0;
    private double appliedDiscount = 0.0;
    private boolean isDiscountApplied = false;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentCartBinding.inflate(inflater, container, false);
        firestore = FirebaseFirestore.getInstance();
        firebaseAuth = FirebaseAuth.getInstance();
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        loadUserData();
        setupRecyclerView();
        loadCartItems();
        setupClickListeners();
    }

    private void loadUserData() {
        if (firebaseAuth.getCurrentUser() == null) return;
        firestore.collection("users").document(firebaseAuth.getCurrentUser().getUid())
                .get().addOnSuccessListener(doc -> {
                    currentUser = doc.toObject(User.class);
                });
    }

    private void setupRecyclerView() {
        adapter = new CartAdapter(cartItems, new CartAdapter.OnCartItemChangeListener() {
            @Override
            public void onQuantityChanged(CartItem item, int newQuantity) {
                updateQuantityInFirestore(item, newQuantity);
            }

            @Override
            public void onRemoveItem(CartItem item) {
                removeItemFromFirestore(item);
            }
        });
        binding.cartRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.cartRecyclerView.setAdapter(adapter);
    }

    private void loadCartItems() {
        if (firebaseAuth.getCurrentUser() == null) return;
        String uid = firebaseAuth.getCurrentUser().getUid();

        binding.cartProgressBar.setVisibility(View.VISIBLE);
        cartListener = firestore.collection("users").document(uid).collection("cart")
                .addSnapshotListener((value, error) -> {
                    if (binding == null || !isAdded()) return;
                    binding.cartProgressBar.setVisibility(View.GONE);
                    if (value != null) {
                        cartItems.clear();
                        for (DocumentSnapshot doc : value.getDocuments()) {
                            CartItem item = doc.toObject(CartItem.class);
                            if (item != null) cartItems.add(item);
                        }
                        adapter.notifyDataSetChanged();
                        checkEmptyState();
                        calculatePricing();
                    }
                });
    }

    private void calculatePricing() {
        if (cartItems.isEmpty()) {
            resetTotals();
            return;
        }

        subtotal = 0;
        totalMaxDiscountPossible = 0;
        AtomicInteger processedItems = new AtomicInteger(0);

        for (CartItem item : cartItems) {
            firestore.collection("products").document(item.getProductId())
                    .get().addOnSuccessListener(doc -> {
                        Product product = doc.toObject(Product.class);
                        if (product != null) {
                            double itemTotal = product.getPrice() * item.getQuantity();
                            subtotal += itemTotal;

                            float maxPercent = product.getMaxPercentage() != null ? product.getMaxPercentage() : 0;
                            totalMaxDiscountPossible += (itemTotal * (maxPercent / 100.0));
                        }

                        if (processedItems.incrementAndGet() == cartItems.size()) {
                            updateUIWithTotals();
                        }
                    });
        }
    }

    private void updateUIWithTotals() {
        if (binding == null || currentUser == null) return;

        double maxPotentialDiscount = Math.min(currentUser.getFitCoins(), totalMaxDiscountPossible);

        if (isDiscountApplied) {
            appliedDiscount = maxPotentialDiscount;
            binding.btnApplyFitCoins.setText("Remove Fit Coins Discount");
        } else {
            appliedDiscount = 0;
            binding.btnApplyFitCoins.setText(String.format("Apply Fit Coins (Save LKR. %,.0f)", maxPotentialDiscount));
        }

        double grandTotal = subtotal + deliveryFee - appliedDiscount;

        binding.tvSubtotal.setText(String.format("LKR. %,.2f", subtotal));
        binding.tvDelivery.setText(String.format("LKR. %,.2f", deliveryFee));
        binding.tvDiscount.setText(String.format("- LKR. %,.2f", appliedDiscount));
        binding.tvGrandTotal.setText(String.format("LKR. %,.2f", grandTotal));
    }

    private void setupClickListeners() {

        binding.btnExpandSummary.setOnClickListener(v -> {
            if (binding.expandableSummary.getVisibility() == View.GONE) {
                binding.expandableSummary.setVisibility(View.VISIBLE);
                binding.btnExpandSummary.setRotation(0f);
            } else {
                binding.expandableSummary.setVisibility(View.GONE);
                binding.btnExpandSummary.setRotation(180f);
            }
        });


        binding.btnApplyFitCoins.setOnClickListener(v -> {
            if (currentUser != null && currentUser.getFitCoins() > 0) {
                isDiscountApplied = !isDiscountApplied;
                calculatePricing();
            } else {
                Toast.makeText(getContext(), "You don't have enough Fit Coins", Toast.LENGTH_SHORT).show();
            }
        });

        binding.btnCheckout.setOnClickListener(v -> {

        });
    }

    private void updateQuantityInFirestore(CartItem item, int newQuantity) {
        String uid = firebaseAuth.getUid();
        String docId = generateDocId(item);
        firestore.collection("users").document(uid).collection("cart").document(docId)
                .update("quantity", newQuantity);
    }

    private void removeItemFromFirestore(CartItem item) {
        String uid = firebaseAuth.getUid();
        String docId = generateDocId(item);
        firestore.collection("users").document(uid).collection("cart").document(docId).delete();
    }

    private String generateDocId(CartItem item) {
        StringBuilder builder = new StringBuilder(item.getProductId());
        for (CartItem.SelectedAttribute attr : item.getAttributes()) {
            builder.append("_").append(attr.getAttributeValue());
        }
        return builder.toString();
    }

    private void checkEmptyState() {
        if (cartItems.isEmpty()) {
            binding.cartEmptyView.setVisibility(View.VISIBLE);
            binding.checkoutCard.setVisibility(View.GONE);
        } else {
            binding.cartEmptyView.setVisibility(View.GONE);
            binding.checkoutCard.setVisibility(View.VISIBLE);
        }
    }

    private void resetTotals() {
        subtotal = 0; appliedDiscount = 0;
        updateUIWithTotals();
    }

    @Override
    public void onDestroyView() {
        if (cartListener != null) cartListener.remove();
        super.onDestroyView();
        binding = null;
    }
}