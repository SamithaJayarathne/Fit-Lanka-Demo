package com.neoviz.fit_lanka_demo.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
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
        holder.productPrice.setText("LKR. " + product.getPrice());

        Glide.with(holder.itemView.getContext())
                .load(product.getImages().get(0))
                .centerCrop()
                .into(holder.productImage);

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                /// load single product details

                if (productClickListener != null) {
                    productClickListener.onProductClick(product);
                }
            }
        });

    }

    @Override
    public int getItemCount() {
        return products.size();
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
