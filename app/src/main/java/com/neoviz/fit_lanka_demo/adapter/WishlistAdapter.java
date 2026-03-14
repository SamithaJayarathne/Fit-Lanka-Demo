package com.neoviz.fit_lanka_demo.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.neoviz.fit_lanka_demo.R;
import com.neoviz.fit_lanka_demo.model.Product;
import com.neoviz.fit_lanka_demo.model.WishlistItem;

import java.util.List;

public class WishlistAdapter extends RecyclerView.Adapter<WishlistAdapter.ViewHolder> {

    private List<WishlistItem> list;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(WishlistItem item);
        void onRemoveClick(WishlistItem item);
    }

    public WishlistAdapter(List<WishlistItem> list, OnItemClickListener listener) {
        this.list = list;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_wishlist, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        WishlistItem item = list.get(position);
        holder.name.setText(item.getProductName());
        holder.price.setText(String.format("Rs. %,.2f", item.getPrice()));
        holder.category.setText(item.getCategoryName());
        Glide.with(holder.itemView.getContext()).load(item.getProductImage()).into(holder.image);

        holder.itemView.setOnClickListener(v -> listener.onItemClick(item));
        holder.removeBtn.setOnClickListener(v -> listener.onRemoveClick(item));
    }

    @Override
    public int getItemCount() { return list.size(); }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView image;
        TextView name, price, category;
        ImageButton removeBtn;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            image = itemView.findViewById(R.id.wishlist_item_image);
            name = itemView.findViewById(R.id.wishlist_item_name);
            price = itemView.findViewById(R.id.wishlist_item_price);
            removeBtn = itemView.findViewById(R.id.wishlist_item_remove);
            category = itemView.findViewById(R.id.wishlist_item_category);
        }
    }

}
