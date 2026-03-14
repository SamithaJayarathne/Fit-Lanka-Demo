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
import com.google.firebase.firestore.Query;
import com.neoviz.fit_lanka_demo.R;
import com.neoviz.fit_lanka_demo.adapter.WishlistAdapter;
import com.neoviz.fit_lanka_demo.databinding.FragmentWishlistBinding;
import com.neoviz.fit_lanka_demo.model.Product;
import com.neoviz.fit_lanka_demo.model.WishlistItem;

import java.util.ArrayList;
import java.util.List;

public class WishlistFragment extends Fragment {

    private FragmentWishlistBinding binding;
    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firestore;
    private WishlistAdapter adapter;
    private List<WishlistItem> wishlistItems;
    private ListenerRegistration wishlistListener;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentWishlistBinding.inflate(inflater, container, false);
        firestore = FirebaseFirestore.getInstance();
        firebaseAuth = FirebaseAuth.getInstance();
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setupRecyclerView();
        loadWishlistData();
    }

    private void setupRecyclerView() {
        wishlistItems = new ArrayList<>();
        adapter = new WishlistAdapter(wishlistItems, new WishlistAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(WishlistItem item) {
                goToProductDetails(item.getProductId());
            }

            @Override
            public void onRemoveClick(WishlistItem item) {
                removeFromWishlist(item.getProductId());
            }
        });

        binding.wishlistRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.wishlistRecyclerView.setAdapter(adapter);
    }

    private void loadWishlistData() {
        if (firebaseAuth.getCurrentUser() == null) return;

        String uid = firebaseAuth.getCurrentUser().getUid();

        binding.wishlistProgressBar.setVisibility(View.VISIBLE);

        wishlistListener = firestore.collection("users").document(uid)
                .collection("wishlist")
                .orderBy("addedAt", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {

                    if (binding == null || !isAdded()) return;

                    binding.wishlistProgressBar.setVisibility(View.GONE);

                    if (error != null) {
                        if (getContext() != null) {
                            Toast.makeText(getContext(), "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                        return;
                    }

                    if (value != null) {
                        wishlistItems.clear();
                        for (DocumentSnapshot doc : value.getDocuments()) {
                            WishlistItem item = doc.toObject(WishlistItem.class);
                            if (item != null) {
                                wishlistItems.add(item);
                            }
                        }

                        adapter.notifyDataSetChanged();

                        if (wishlistItems.isEmpty()) {
                            binding.wishlistEmptyView.setVisibility(View.VISIBLE);
                            binding.wishlistRecyclerView.setVisibility(View.GONE);
                        } else {
                            binding.wishlistEmptyView.setVisibility(View.GONE);
                            binding.wishlistRecyclerView.setVisibility(View.VISIBLE);
                        }
                    }
                });
    }

    private void removeFromWishlist(String productId) {
        if (firebaseAuth.getCurrentUser() == null) return;

        String uid = firebaseAuth.getCurrentUser().getUid();

        firestore.collection("users").document(uid)
                .collection("wishlist").document(productId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "Item removed", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void goToProductDetails(String productId) {
        if (productId == null || productId.isEmpty()) return;
        binding.wishlistProgressBar.setVisibility(View.VISIBLE);

        firestore.collection("products").document(productId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (binding == null) return;
                    binding.wishlistProgressBar.setVisibility(View.GONE);

                    if (documentSnapshot.exists()) {
                        Product product = documentSnapshot.toObject(Product.class);
                        if (product != null) {
                            navigateToDetails(product);
                        }
                    } else {
                        Toast.makeText(getContext(), "Product no longer available", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    if (binding == null) return;
                    binding.wishlistProgressBar.setVisibility(View.GONE);
                    Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void navigateToDetails(Product product) {
        ProductDetailsFragment detailsFragment = new ProductDetailsFragment();
        Bundle bundle = new Bundle();
        bundle.putSerializable("selectedProduct", product);
        detailsFragment.setArguments(bundle);

        requireActivity().getSupportFragmentManager().beginTransaction()
                .setCustomAnimations(
                        R.anim.slide_in_right,
                        R.anim.slide_out_left,
                        R.anim.slide_in_left,
                        R.anim.slide_out_right
                )
                .replace(R.id.fragment_container_view, detailsFragment)
                .addToBackStack(null)
                .commit();
    }

    @Override
    public void onDestroyView() {
        if (wishlistListener != null) {
            wishlistListener.remove();
        }
        super.onDestroyView();
        binding = null;
    }
}