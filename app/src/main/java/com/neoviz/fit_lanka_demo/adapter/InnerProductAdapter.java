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
import com.neoviz.fit_lanka_demo.model.Order;
import java.util.List;

public class InnerProductAdapter extends RecyclerView.Adapter<InnerProductAdapter.ViewHolder> {
    private List<Order.OrderItem> items;
    private String orderId;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(Order.OrderItem item, String orderId);
    }

    public InnerProductAdapter(List<Order.OrderItem> items, String orderId, OnItemClickListener listener) {
        this.items = items;
        this.orderId = orderId;
        this.listener = listener;
    }

    @NonNull @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup p, int vt) {
        return new ViewHolder(LayoutInflater.from(p.getContext()).inflate(R.layout.item_inner_product, p, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int pos) {
        Order.OrderItem item = items.get(pos);
        h.name.setText(item.getProductName());
        h.qty.setText("Qty: " + item.getQuantity());
        Glide.with(h.itemView.getContext()).load(item.getProductImage()).into(h.img);
        h.itemView.setOnClickListener(v -> listener.onItemClick(item, orderId));
    }

    @Override public int getItemCount() { return items.size(); }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView name, qty; ImageView img;
        public ViewHolder(View v) { super(v);
            name = v.findViewById(R.id.tvInnerProductName);
            qty = v.findViewById(R.id.tvInnerProductQty);
            img = v.findViewById(R.id.ivProductImage);
        }
    }
}