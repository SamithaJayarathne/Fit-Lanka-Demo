package com.neoviz.fit_lanka_demo.adapter;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.neoviz.fit_lanka_demo.R;
import com.neoviz.fit_lanka_demo.model.Order;
import java.util.List;

public class OrderHistoryAdapter extends RecyclerView.Adapter<OrderHistoryAdapter.ViewHolder> {
    private List<Order> orders;
    private InnerProductAdapter.OnItemClickListener listener;

    public OrderHistoryAdapter(List<Order> orders, InnerProductAdapter.OnItemClickListener listener) {
        this.orders = orders;
        this.listener = listener;
    }

    @NonNull @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup p, int vt) {
        return new ViewHolder(LayoutInflater.from(p.getContext()).inflate(R.layout.item_order, p, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int pos) {
        Order o = orders.get(pos);
        h.id.setText("#" + o.getOrderId());

        String status = o.getStatus().toLowerCase();
        h.status.setText(status.toUpperCase());
        h.total.setText(String.format("Total: Rs. %,.2f", o.getTotalAmount()));

        // Status එක අනුව පාට වෙනස් කිරීම
        int bgColor, textColor;

        switch (status) {
            case "delivered":
                bgColor = Color.parseColor("#E8F5E9"); // Light Green
                textColor = Color.parseColor("#2E7D32"); // Dark Green
                break;
            case "cancelled":
                bgColor = Color.parseColor("#FFEBEE"); // Light Red
                textColor = Color.parseColor("#C62828"); // Dark Red
                break;
            default: // "pending" සහ වෙනත් ඕනෑම එකකට
                bgColor = Color.parseColor("#FFF8E1"); // Light Gold/Yellow
                textColor = Color.parseColor("#F57F17"); // Dark Gold
                break;
        }

        // TextView එකේ background එකට පාට දෙනවා
        h.status.getBackground().setTint(bgColor);
        // Text එකේ පාට වෙනස් කරනවා
        h.status.setTextColor(textColor);

        // Inner RecyclerView setup
        h.rvInner.setLayoutManager(new LinearLayoutManager(h.itemView.getContext()));
        h.rvInner.setAdapter(new InnerProductAdapter(o.getOrderItems(), o.getOrderId(), listener));
    }

    @Override public int getItemCount() { return orders.size(); }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView id, status, total; RecyclerView rvInner;
        public ViewHolder(View v) { super(v);
            id = v.findViewById(R.id.tvOrderId);
            status = v.findViewById(R.id.tvOrderStatus);
            total = v.findViewById(R.id.tvOrderTotal);
            rvInner = v.findViewById(R.id.rvOrderItemsInner);
        }
    }
}