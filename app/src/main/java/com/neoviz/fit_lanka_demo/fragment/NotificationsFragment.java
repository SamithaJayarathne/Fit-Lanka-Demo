package com.neoviz.fit_lanka_demo.fragment;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.neoviz.fit_lanka_demo.adapter.NotificationAdapter;
import com.neoviz.fit_lanka_demo.databinding.FragmentNotificationsBinding;
import com.neoviz.fit_lanka_demo.model.Notification;
import java.util.ArrayList;
import java.util.List;

public class NotificationsFragment extends Fragment {

    private FragmentNotificationsBinding binding;
    private FirebaseFirestore firestore;
    private FirebaseAuth firebaseAuth;
    private NotificationAdapter adapter;
    private final List<Notification> notificationList = new ArrayList<>();

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentNotificationsBinding.inflate(inflater, container, false);
        firestore = FirebaseFirestore.getInstance();
        firebaseAuth = FirebaseAuth.getInstance();
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupRecyclerView();
        loadNotifications();
    }

    private void setupRecyclerView() {
        adapter = new NotificationAdapter(notificationList,
                notification -> updateReadStatus(notification),
                notification -> showDeleteConfirmation(notification));
        binding.notificationsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.notificationsRecyclerView.setAdapter(adapter);
    }

    private void loadNotifications() {
        String uid = firebaseAuth.getCurrentUser() != null ? firebaseAuth.getCurrentUser().getUid() : null;
        if (uid == null) return;

        firestore.collection("users")
                .document(uid)
                .collection("notifications")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e("Notifications", "Listener failed.", error);
                        return;
                    }

                    if (value != null) {
                        notificationList.clear();
                        for (DocumentSnapshot doc : value.getDocuments()) {
                            Notification n = doc.toObject(Notification.class);
                            if (n != null) {
                                n.setId(doc.getId());
                                notificationList.add(n);
                            }
                        }
                        adapter.notifyDataSetChanged();

                        if (notificationList.isEmpty()) {
                            binding.layoutEmpty.setVisibility(View.VISIBLE);
                            binding.notificationsRecyclerView.setVisibility(View.GONE);
                        } else {
                            binding.layoutEmpty.setVisibility(View.GONE);
                            binding.notificationsRecyclerView.setVisibility(View.VISIBLE);
                        }
                    }
                });
    }

    private void updateReadStatus(Notification notification) {
        if (!notification.isRead()) {
            String uid = firebaseAuth.getUid();
            if (uid != null) {
                firestore.collection("users").document(uid)
                        .collection("notifications").document(notification.getId())
                        .update("read", true)
                        .addOnSuccessListener(aVoid -> Log.d("Notifications", "Read status updated"))
                        .addOnFailureListener(e -> Log.e("Notifications", "Failed to update read status", e));
            }
        }
    }

    private void showDeleteConfirmation(Notification notification) {
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                .setTitle("Delete Notification")
                .setMessage("Are you sure you want to delete this notification?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    deleteNotificationFromFirestore(notification);
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void deleteNotificationFromFirestore(Notification notification) {
        String uid = firebaseAuth.getUid();
        if (uid != null && notification.getId() != null) {
            firestore.collection("users").document(uid)
                    .collection("notifications").document(notification.getId())
                    .delete()
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(getContext(), "Notification deleted", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        Log.e("Notifications", "Error deleting document", e);
                    });
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}