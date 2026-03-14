package com.neoviz.fit_lanka_demo.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.neoviz.fit_lanka_demo.databinding.ItemProductSliderBinding;

import java.util.List;

public class ProductImageSliderAdapter extends RecyclerView.Adapter<ProductImageSliderAdapter.ViewHolder>{
    
    private List<String> images;

    public ProductImageSliderAdapter(List<String> images) {
        this.images = images;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemProductSliderBinding binding = ItemProductSliderBinding
                .inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {

        Glide.with(holder.itemView.getContext())
                .load(images.get(position))
                .centerCrop()
                .into(holder.binding.itemProductSliderImage);

    }

    @Override
    public int getItemCount() {
        return images.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder{
        
        ItemProductSliderBinding binding;
        
        public ViewHolder(@NonNull ItemProductSliderBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
    
}
