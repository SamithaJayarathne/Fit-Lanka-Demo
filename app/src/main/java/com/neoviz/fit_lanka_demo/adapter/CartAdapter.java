package com.neoviz.fit_lanka_demo.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.google.firebase.firestore.FirebaseFirestore;
import com.neoviz.fit_lanka_demo.databinding.ItemCartBinding;
import com.neoviz.fit_lanka_demo.model.CartItem;
import com.neoviz.fit_lanka_demo.model.Product;
import java.util.List;

public class CartAdapter extends RecyclerView.Adapter<CartAdapter.CartViewHolder> {

    private List<CartItem> cartItems;
    private OnCartItemChangeListener listener;
    private FirebaseFirestore firestore = FirebaseFirestore.getInstance();

    public interface OnCartItemChangeListener {
        void onQuantityChanged(CartItem item, int newQuantity);
        void onRemoveItem(CartItem item);
    }

    public CartAdapter(List<CartItem> cartItems, OnCartItemChangeListener listener) {
        this.cartItems = cartItems;
        this.listener = listener;
    }

    @NonNull
    @Override
    public CartViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemCartBinding binding = ItemCartBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new CartViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull CartViewHolder holder, int position) {
        CartItem cartItem = cartItems.get(position);
        holder.bind(cartItem);
    }

    @Override
    public int getItemCount() {
        return cartItems.size();
    }

    class CartViewHolder extends RecyclerView.ViewHolder {
        private ItemCartBinding binding;

        public CartViewHolder(ItemCartBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(CartItem cartItem) {
            binding.tvCartQuantity.setText(String.valueOf(cartItem.getQuantity()));

            StringBuilder attrText = new StringBuilder();
            if (cartItem.getAttributes() != null) {
                for (CartItem.SelectedAttribute attr : cartItem.getAttributes()) {
                    attrText.append(attr.getAttributeName()).append(": ")
                            .append(attr.getAttributeValue()).append(" | ");
                }
            }

            String finalAttr = attrText.toString().isEmpty() ? "" : attrText.substring(0, attrText.length() - 3);
            binding.cartItemAttributes.setText(finalAttr);

            firestore.collection("products").document(cartItem.getProductId())
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        Product product = documentSnapshot.toObject(Product.class);
                        if (product != null) {
                            binding.cartItemName.setText(product.getName());
                            binding.cartItemPrice.setText(String.format("LKR. %,.2f", product.getPrice()));

                            if (product.getImages() != null && !product.getImages().isEmpty()) {
                                Glide.with(itemView.getContext())
                                        .load(product.getImages().get(0))
                                        .into(binding.cartItemImage);
                            }
                        }
                    });

            binding.btnCartPlus.setOnClickListener(v -> {
                listener.onQuantityChanged(cartItem, cartItem.getQuantity() + 1);
            });

            binding.btnCartMinus.setOnClickListener(v -> {
                if (cartItem.getQuantity() > 1) {
                    listener.onQuantityChanged(cartItem, cartItem.getQuantity() - 1);
                }
            });

            binding.cartItemRemove.setOnClickListener(v -> {
                listener.onRemoveItem(cartItem);
            });
        }
    }
}