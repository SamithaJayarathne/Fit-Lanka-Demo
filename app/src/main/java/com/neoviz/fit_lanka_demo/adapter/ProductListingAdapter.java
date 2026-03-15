package com.neoviz.fit_lanka_demo.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.neoviz.fit_lanka_demo.R;
import com.neoviz.fit_lanka_demo.model.Product;

import java.util.List;

public class ProductListingAdapter extends RecyclerView.Adapter<ProductListingAdapter.ViewHolder> {

    private List<Product> products;
    private OnProductClickListener productClickListener;

    public ProductListingAdapter(List<Product> products, OnProductClickListener productClickListener) {
        this.products = products;
        this.productClickListener = productClickListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_product, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Product product = products.get(position);

        holder.productName.setText(product.getName());
        holder.productPrice.setText(String.format("LKR. %,.2f", product.getPrice()));
        holder.productCategory.setText(product.getCategoryName().toUpperCase());

        if (product.getMaxPercentage() != null && product.getMaxPercentage() > 0) {
            holder.productDiscountText.setVisibility(View.VISIBLE);
            holder.productDiscountText.setText(product.getMaxPercentage() + "% OFF");
        } else {
            holder.productDiscountText.setVisibility(View.GONE);
        }

        if (product.getImages() != null && !product.getImages().isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(product.getImages().get(0))
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .centerCrop()
                    .into(holder.productImage);
        }

        holder.itemView.setOnClickListener(v -> {
            if (productClickListener != null) {
                productClickListener.onProductClick(product);
            }
        });
    }

    @Override
    public int getItemCount() {
        return products != null ? products.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView productImage;
        TextView productName, productCategory, productPrice, productDiscountText;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            productImage = itemView.findViewById(R.id.item_product_image);
            productName = itemView.findViewById(R.id.item_product_name);
            productCategory = itemView.findViewById(R.id.item_product_category);
            productPrice = itemView.findViewById(R.id.item_product_price);
            productDiscountText = itemView.findViewById(R.id.item_product_discount_text);
        }
    }

    public interface OnProductClickListener {
        void onProductClick(Product product);
    }
}