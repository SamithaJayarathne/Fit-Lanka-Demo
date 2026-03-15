package com.neoviz.fit_lanka_demo.adapter;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.neoviz.fit_lanka_demo.R;
import com.neoviz.fit_lanka_demo.model.Order;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class OrderHistoryAdapter extends RecyclerView.Adapter<OrderHistoryAdapter.ViewHolder> {
    private List<Order> orders;
    private InnerProductAdapter.OnItemClickListener listener;

    public OrderHistoryAdapter(List<Order> orders, InnerProductAdapter.OnItemClickListener listener) {
        this.orders = orders;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup p, int vt) {
        return new ViewHolder(LayoutInflater.from(p.getContext()).inflate(R.layout.item_order, p, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int pos) {
        Order o = orders.get(pos);

        // 1. Firebase Timestamp එක Format කිරීම (Ex: Oct 24, 2026)
        if (o.getOrderDate() != null) {
            SimpleDateFormat sfd = new SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.getDefault());
            String dateStr = sfd.format(o.getOrderDate().toDate());
            h.date.setText(dateStr);
        }

        // 2. Item Count එක
        if (o.getOrderItems() != null) {
            int count = o.getOrderItems().size();
            h.itemCount.setText(count + (count > 1 ? " Items" : " Item"));
        }

        // 3. Total Amount
        h.total.setText(String.format("LKR %,.2f", o.getTotalAmount()));

        // 4. Status Styling
        String status = (o.getStatus() != null) ? o.getStatus().toLowerCase() : "pending";
        h.status.setText(status.toUpperCase());

        int bgColor, textColor;
        switch (status) {
            case "delivered":
                bgColor = Color.parseColor("#E8F5E9");
                textColor = Color.parseColor("#2E7D32");
                break;
            case "cancelled":
                bgColor = Color.parseColor("#FFEBEE");
                textColor = Color.parseColor("#C62828");
                break;
            default:
                bgColor = Color.parseColor("#FFF8E1");
                textColor = Color.parseColor("#F57F17");
                break;
        }

        h.statusBadgeContainer.setCardBackgroundColor(bgColor);
        h.status.setTextColor(textColor);

        // Inner RecyclerView setup
        h.rvInner.setLayoutManager(new LinearLayoutManager(h.itemView.getContext()));
        h.rvInner.setAdapter(new InnerProductAdapter(o.getOrderItems(), o.getOrderId(), listener));
    }

    @Override
    public int getItemCount() {
        return orders != null ? orders.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView status, total, date, itemCount;
        RecyclerView rvInner;
        MaterialCardView statusBadgeContainer;

        public ViewHolder(View v) {
            super(v);
            // ID පේළිය අයින් කරා
            status = v.findViewById(R.id.tvOrderStatus);
            total = v.findViewById(R.id.tvOrderTotal);
            date = v.findViewById(R.id.tvOrderDate);
            itemCount = v.findViewById(R.id.tvItemCount);
            rvInner = v.findViewById(R.id.rvOrderItemsInner);
            statusBadgeContainer = v.findViewById(R.id.statusBadgeContainer);
        }
    }
}