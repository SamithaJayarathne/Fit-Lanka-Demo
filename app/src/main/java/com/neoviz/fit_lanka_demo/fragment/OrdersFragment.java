package com.neoviz.fit_lanka_demo.fragment;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
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
import com.google.firebase.firestore.ListenerRegistration;
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

    // Performance සහ Memory Leaks වැළැක්වීමට Listener එකක් පාවිච්චි කරමු
    private ListenerRegistration orderListener;

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

        // Adapter එක Initialize කිරීම
        adapter = new OrderHistoryAdapter(orderList, this::showFeedbackSheet);
        binding.rvOrders.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.rvOrders.setAdapter(adapter);

        // Data load වෙන්න කලින් ProgressBar එක පෙන්වමු
        if (binding.progressBar != null) {
            binding.progressBar.setVisibility(View.VISIBLE);
        }

        loadOrders();
    }

    private void loadOrders() {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;

        // get() වෙනුවට addSnapshotListener පාවිච්චි කිරීමෙන් Speed එක වැඩි වෙනවා
        // limit(30) මගින් එකපාර දත්ත ගොඩක් load වීම නවතා RAM එක ඉතිරි කරනවා
        orderListener = db.collection("orders")
                .whereEqualTo("userId", uid)
                .orderBy("orderDate", Query.Direction.DESCENDING)
                .limit(30)
                .addSnapshotListener((qds, error) -> {

                    // වැදගත්ම දේ: Fragment එක screen එකේ නැත්නම් හෝ error එකක් නම් නතර කරන්න
                    if (binding == null) return;

                    if (error != null) {
                        if (binding.progressBar != null) binding.progressBar.setVisibility(View.GONE);
                        Toast.makeText(getContext(), "Error loading orders", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (qds != null) {
                        orderList.clear();
                        for (DocumentSnapshot doc : qds) {
                            Order o = doc.toObject(Order.class);
                            if (o != null) {
                                o.setOrderId(doc.getId());
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

                        // UI එකේ පෙනුම පාලනය කිරීම
                        if (binding.progressBar != null) binding.progressBar.setVisibility(View.GONE);

                        if (orderList.isEmpty()) {
                            binding.rvOrders.setVisibility(View.GONE);
                            if (binding.tvNoOrders != null) binding.tvNoOrders.setVisibility(View.VISIBLE);
                        } else {
                            binding.rvOrders.setVisibility(View.VISIBLE);
                            if (binding.tvNoOrders != null) binding.tvNoOrders.setVisibility(View.GONE);
                        }
                    }
                });
    }

    private void showFeedbackSheet(Order.OrderItem item, String orderId) {
        if (getContext() == null) return;

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
            btnAdd.setText("Already Reviewed");
            rb.setIsIndicator(true);
            etFeedback.setEnabled(false);
        }

        v.findViewById(R.id.btnCallSeller).setOnClickListener(view -> {
            startActivity(new Intent(Intent.ACTION_DIAL, Uri.parse("tel:0712345678")));
        });

        btnAdd.setOnClickListener(view -> {
            float rating = rb.getRating();
            String feedbackStr = etFeedback.getText().toString();
            if (rating > 0) {
                saveFeedback(item.getProductId(), rating, feedbackStr, orderId, item, dialog);
            } else {
                Toast.makeText(getContext(), "Please give a rating", Toast.LENGTH_SHORT).show();
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

        // Feedbacks save කිරීමේදී Firestore calls chain කිරීම
        db.collection("products").document(productId)
                .collection("reviews").add(feedbackObj)
                .addOnSuccessListener(docRef -> {
                    db.collection("orders").document(orderId)
                            .update("reviewedProducts", FieldValue.arrayUnion(productId))
                            .addOnSuccessListener(aVoid -> {
                                // Fragment එක තවමත් පවතිනවාදැයි පරීක්ෂා කිරීම (Safety first)
                                if (isAdded() && binding != null) {
                                    Toast.makeText(requireContext(), "Review Saved!", Toast.LENGTH_SHORT).show();
                                    item.setReviewed(true);
                                    adapter.notifyDataSetChanged();
                                    dialog.dismiss();
                                }
                            });
                })
                .addOnFailureListener(e -> {
                    if (isAdded()) Toast.makeText(requireContext(), "Failed to save review", Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    public void onDestroyView() {
        // 🛑 ඉතාමත් වැදගත්: Fragment එකෙන් අයින් වෙද්දී listener එක අනිවාර්යයෙන්ම remove කරන්න.
        // මෙය නොකළහොත් වෙනත් Fragment එකකදීත් මෙය background එකේ දුවමින් App එක හිර කරයි.
        if (orderListener != null) {
            orderListener.remove();
        }
        super.onDestroyView();
        binding = null;
    }
}