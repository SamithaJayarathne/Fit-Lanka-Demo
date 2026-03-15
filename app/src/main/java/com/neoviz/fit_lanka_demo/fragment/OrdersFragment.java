package com.neoviz.fit_lanka_demo.fragment;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.neoviz.fit_lanka_demo.R;
import com.neoviz.fit_lanka_demo.adapter.OrderHistoryAdapter;
import com.neoviz.fit_lanka_demo.databinding.FragmentOrdersBinding;
import com.neoviz.fit_lanka_demo.model.Feedback;
import com.neoviz.fit_lanka_demo.model.Order;

import java.util.ArrayList;
import java.util.List;

public class OrdersFragment extends Fragment {
    private FragmentOrdersBinding binding;
    private FirebaseFirestore db;
    private List<Order> orderList = new ArrayList<>();
    private OrderHistoryAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inf, @Nullable ViewGroup cont, @Nullable Bundle b) {
        binding = FragmentOrdersBinding.inflate(inf, cont, false);
        db = FirebaseFirestore.getInstance();
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        adapter = new OrderHistoryAdapter(orderList, this::showFeedbackSheet);
        binding.rvOrders.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.rvOrders.setAdapter(adapter);
        loadOrders();
    }

    private void loadOrders() {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;

        db.collection("orders")
                .whereEqualTo("userId", uid)
                .orderBy("orderDate", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(qds -> {
                    orderList.clear();
                    for (DocumentSnapshot doc : qds) {
                        Order o = doc.toObject(Order.class);
                        if (o != null) {

                            if (o.getReviewedProducts() != null && o.getOrderItems() != null) {
                                for (Order.OrderItem item : o.getOrderItems()) {
                                    if (o.getReviewedProducts().contains(item.getProductId())) {
                                        item.setReviewed(true);
                                    }
                                }
                            }
                            orderList.add(o);
                        }
                    }
                    adapter.notifyDataSetChanged();
                }).addOnFailureListener(error -> {
                    Log.e("FirestoreError", "Error loading orders: " + error.getMessage());
                });
    }

    private void showFeedbackSheet(Order.OrderItem item, String orderId) {
        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        View v = getLayoutInflater().inflate(R.layout.layout_order_details_sheet, null);
        dialog.setContentView(v);

        TextView tvName = v.findViewById(R.id.tvSheetProductName);
        RatingBar rb = v.findViewById(R.id.ratingBar);
        EditText etFeedback = v.findViewById(R.id.etFeedback);
        Button btnAdd = v.findViewById(R.id.btnAddFeedback);

        tvName.setText(item.getProductName());

        if (item.isReviewed()) {
            btnAdd.setEnabled(false);
            btnAdd.setText("Feedback Already Submitted");
            btnAdd.setAlpha(0.6f);
            rb.setIsIndicator(true);
            etFeedback.setEnabled(false);
            etFeedback.setHint("Feedback already provided for this item.");
        }

        v.findViewById(R.id.btnCallSeller).setOnClickListener(view -> {
            Intent i = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:0712345678"));
            startActivity(i);
        });

        btnAdd.setOnClickListener(view -> {
            float rating = rb.getRating();
            String feedbackStr = etFeedback.getText().toString();
            if (rating > 0) {
                saveFeedback(item.getProductId(), rating, feedbackStr, orderId, item, dialog);
            } else {
                Toast.makeText(getContext(), "Please select a rating!", Toast.LENGTH_SHORT).show();
            }
        });
        dialog.show();
    }

    private void saveFeedback(String productId, float rating, String feedback, String orderId, Order.OrderItem item, BottomSheetDialog dialog) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        Feedback feedbackObj = Feedback.builder()
                .userId(user.getUid())
                .userName(user.getDisplayName())
                .rating(rating)
                .review(feedback)
                .timestamp(com.google.firebase.Timestamp.now())
                .build();

        db.collection("products").document(productId)
                .collection("reviews").add(feedbackObj)
                .addOnSuccessListener(aVoid -> {

                    db.collection("orders").document(orderId)
                            .update(
                                    "status", "delivered",
                                    "reviewedProducts", FieldValue.arrayUnion(productId)
                            )
                            .addOnSuccessListener(unused -> {
                                Toast.makeText(getContext(), "Thank you for your feedback!", Toast.LENGTH_SHORT).show();

                                item.setReviewed(true);
                                for (Order o : orderList) {
                                    if (o.getOrderId().equals(orderId)) {
                                        o.setStatus("delivered");
                                        break;
                                    }
                                }
                                adapter.notifyDataSetChanged();
                                dialog.dismiss();
                            })
                            .addOnFailureListener(e -> {
                                Log.e("OrdersFragment", "Update fail: " + e.getMessage());
                                dialog.dismiss();
                            });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Submission failed", Toast.LENGTH_SHORT).show();
                });
    }
}