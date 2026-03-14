package com.neoviz.fit_lanka_demo.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.neoviz.fit_lanka_demo.databinding.ItemCartMinimalBinding;
import com.neoviz.fit_lanka_demo.model.CartItem;

import java.util.List;

public class MinimalOrderAdapter extends RecyclerView.Adapter<MinimalOrderAdapter.ViewHolder> {

    private final List<CartItem> itemList;
    private final FirebaseFirestore firestore;

    public MinimalOrderAdapter(List<CartItem> itemList) {
        this.itemList = itemList;
        this.firestore = FirebaseFirestore.getInstance();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemCartMinimalBinding binding = ItemCartMinimalBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CartItem cartItem = itemList.get(position);

        // 1. මුලින්ම පවතින දත්ත සෙට් කරමු (Quantity & Attributes)
        holder.binding.tvMiniQty.setText(cartItem.getQuantity() + "x");
        setAttributes(holder, cartItem);

        // 2. Firestore එකෙන් Product details ටික ගමු
        if (cartItem.getProductId() != null) {
            firestore.collection("products").document(cartItem.getProductId())
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            // Firestore එකෙන් නම සහ මිල ගමු
                            String productName = documentSnapshot.getString("name");
                            Double price = documentSnapshot.getDouble("price");

                            if (productName != null) {
                                holder.binding.tvMiniProductName.setText(productName);
                            }

                            if (price != null) {
                                double totalItemPrice = price * cartItem.getQuantity();
                                holder.binding.tvMiniPrice.setText(String.format("Rs. %,.2f", totalItemPrice));
                            }
                        } else {
                            holder.binding.tvMiniProductName.setText("Product not found");
                        }
                    })
                    .addOnFailureListener(e -> {
                        holder.binding.tvMiniProductName.setText("Error loading data");
                    });
        }
    }

    private void setAttributes(ViewHolder holder, CartItem cartItem) {
        if (cartItem.getAttributes() != null && !cartItem.getAttributes().isEmpty()) {
            StringBuilder stringBuilder = new StringBuilder();
            for (int i = 0; i < cartItem.getAttributes().size(); i++) {
                CartItem.SelectedAttribute attr = cartItem.getAttributes().get(i);
                stringBuilder.append(attr.getAttributeName())
                        .append(": ")
                        .append(attr.getAttributeValue());

                if (i < cartItem.getAttributes().size() - 1) {
                    stringBuilder.append(" | ");
                }
            }
            holder.binding.tvMiniAttributes.setText(stringBuilder.toString());
        } else {
            holder.binding.tvMiniAttributes.setText("");
        }
    }

    @Override
    public int getItemCount() {
        return itemList != null ? itemList.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ItemCartMinimalBinding binding;

        public ViewHolder(ItemCartMinimalBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}