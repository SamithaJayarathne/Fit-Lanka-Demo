package com.neoviz.fit_lanka_demo.adapter;

import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.neoviz.fit_lanka_demo.R;
import com.neoviz.fit_lanka_demo.model.Notification;
import java.util.List;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.ViewHolder> {

    private final OnNotificationClickListener listener;
    private final OnNotificationLongClickListener longClickListener;
    private final List<Notification> notificationList;

    public NotificationAdapter(List<Notification> notificationList, OnNotificationClickListener listener, OnNotificationLongClickListener longClickListener) {
        this.listener = listener;
        this.notificationList = notificationList;
        this.longClickListener = longClickListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_notification, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Notification notification = notificationList.get(position);

        holder.title.setText(notification.getTitle());
        holder.message.setText(notification.getMessage());

        holder.unreadDot.setVisibility(notification.isRead() ? View.GONE : View.VISIBLE);

        if (notification.getTimestamp() != null) {
            long timeMillis = notification.getTimestamp().toDate().getTime();
            CharSequence ago = DateUtils.getRelativeTimeSpanString(timeMillis,
                    System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS);
            holder.time.setText(ago);
        }

        holder.itemView.setOnClickListener(v -> listener.onNotificationClick(notification));
        holder.itemView.setOnLongClickListener(v -> {
            longClickListener.onNotificationLongClick(notification);
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return notificationList != null ? notificationList.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView title, message, time;
        View unreadDot, typeIcon;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.item_notification_title);
            message = itemView.findViewById(R.id.item_notification_message);
            time = itemView.findViewById(R.id.item_notification_time);
            unreadDot = itemView.findViewById(R.id.item_notification_unread_dot);
            typeIcon = itemView.findViewById(R.id.item_notification_type_icon);
        }
    }

    public interface OnNotificationClickListener {
        void onNotificationClick(Notification notification);
    }

    public interface OnNotificationLongClickListener {
        void onNotificationLongClick(Notification notification);
    }
}