package com.neoviz.fit_lanka_demo.fragment;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;
import com.neoviz.fit_lanka_demo.activity.HomeActivity;
import com.neoviz.fit_lanka_demo.activity.ProfileActivity;
import com.neoviz.fit_lanka_demo.adapter.MinimalOrderAdapter;
import com.neoviz.fit_lanka_demo.databinding.FragmentCheckoutBinding;
import com.neoviz.fit_lanka_demo.model.CartItem;
import com.neoviz.fit_lanka_demo.model.Order;
import com.neoviz.fit_lanka_demo.model.Product;
import com.neoviz.fit_lanka_demo.model.User;

import java.util.ArrayList;
import java.util.List;

import lk.payhere.androidsdk.PHConstants;
import lk.payhere.androidsdk.PHMainActivity;
import lk.payhere.androidsdk.PHResponse;
import lk.payhere.androidsdk.model.InitRequest;
import lk.payhere.androidsdk.model.StatusResponse;

public class CheckoutFragment extends Fragment {

    private FragmentCheckoutBinding binding;
    private FirebaseFirestore firestore;
    private FirebaseAuth firebaseAuth;

    private MinimalOrderAdapter adapter;
    private List<CartItem> orderItems = new ArrayList<>();
    private double finalTotal = 0.0;
    private User currentUser;

    // Coins වලට අදාළ variables
    private int claimedCoins = 0;
    private boolean isCoinsClaimed = false;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentCheckoutBinding.inflate(inflater, container, false);
        firestore = FirebaseFirestore.getInstance();
        firebaseAuth = FirebaseAuth.getInstance();
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Bundle එකෙන් දත්ත ලබා ගැනීම
        if (getArguments() != null) {
            finalTotal = getArguments().getDouble("finalTotal", 0.0);
            isCoinsClaimed = getArguments().getBoolean("isCoinsClaimed", false);
            claimedCoins = getArguments().getInt("claimedCoinsAmount", 0);

            binding.tvFinalTotal.setText(String.format("Rs. %,.2f", finalTotal));

            if (isCoinsClaimed && claimedCoins > 0) {
                Log.d("CHECKOUT", "Coins to be deducted: " + claimedCoins);
            }
        }

        binding.toolbar.setNavigationOnClickListener(v -> requireActivity().onBackPressed());

        setupRecyclerView();
        setupLogic();
        loadUserData();
        loadCartData();

        binding.btnPlaceOrder.setOnClickListener(v -> {
            if (currentUser == null) {
                Toast.makeText(getContext(), "Please wait, loading user data...", Toast.LENGTH_SHORT).show();
                return;
            }

            if (binding.rbCard.isChecked()) {
                runPayhereGateway();
            } else {
                placeOrder();
            }
        });
    }

    private void runPayhereGateway() {
        InitRequest req = new InitRequest();
        req.setSandBox(true);
        req.setMerchantId("1223969");
        req.setMerchantSecret("MzEzMDA2ODIwODE0NzgxMTA5NDc5NjY4NTgxNDExMzIxMTE1ODU0");

        req.setCurrency("LKR");
        req.setAmount(finalTotal);
        req.setOrderId("ORDER_" + System.currentTimeMillis());
        req.setItemsDescription("Fitness Products Purchase");

        req.getCustomer().setFirstName(currentUser.getName());
        req.getCustomer().setLastName("");
        req.getCustomer().setEmail(currentUser.getEmail());
        req.getCustomer().setPhone(currentUser.getContactNumber());

        if (currentUser.getAddresses() != null && !currentUser.getAddresses().isEmpty()) {
            User.Address addr = currentUser.getAddresses().get(0);
            req.getCustomer().getAddress().setAddress(addr.getAddressLine1());
            req.getCustomer().getAddress().setCity(addr.getCity());
            req.getCustomer().getAddress().setCountry("Sri Lanka");
        }

        req.setNotifyUrl("https://fitlanka.requestcatcher.com/");

        Intent intent = new Intent(getActivity(), PHMainActivity.class);
        intent.putExtra(PHConstants.INTENT_EXTRA_DATA, req);
        payhereLauncher.launch(intent);
    }

    private final ActivityResultLauncher<Intent> payhereLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Intent data = result.getData();
                    if (data.hasExtra(PHConstants.INTENT_EXTRA_RESULT)) {
                        PHResponse<StatusResponse> response = (PHResponse<StatusResponse>) data.getSerializableExtra(PHConstants.INTENT_EXTRA_RESULT);
                        if (response != null && response.isSuccess()) {
                            Log.i("PAYHERE", "Payment Success");
                            placeOrder();
                        } else {
                            Log.e("PAYHERE", "Payment Failed");
                            Toast.makeText(getContext(), "Payment Failed. Please try again.", Toast.LENGTH_SHORT).show();
                        }
                    }
                } else if (result.getResultCode() == Activity.RESULT_CANCELED) {
                    Log.w("PAYHERE", "Payment Cancelled");
                }
            }
    );

    private void setupRecyclerView() {
        adapter = new MinimalOrderAdapter(orderItems);
        binding.rvOrderSummary.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.rvOrderSummary.setAdapter(adapter);
    }

    private void setupLogic() {
        binding.cbSameAddress.setOnCheckedChangeListener((buttonView, isChecked) -> {
            binding.cardBillingAddress.setVisibility(isChecked ? View.GONE : View.VISIBLE);
            if (!isChecked) displayBillingAddress();
        });

        View.OnClickListener goToProfile = v -> {
            Intent intent = new Intent(requireActivity(), ProfileActivity.class);
            intent.putExtra("scrollToAddress", true);
            startActivity(intent);
        };
        binding.btnAddAddress.setOnClickListener(goToProfile);
        binding.btnEditAddress.setOnClickListener(goToProfile);
        binding.btnAddBillingAddress.setOnClickListener(goToProfile);
    }

    private void loadUserData() {
        if (firebaseAuth.getUid() == null) return;
        firestore.collection("users").document(firebaseAuth.getUid())
                .get().addOnSuccessListener(doc -> {
                    currentUser = doc.toObject(User.class);
                    if (currentUser != null) {
                        displayShippingAddress();
                    }
                });
    }

    private void displayShippingAddress() {
        if (currentUser.getAddresses() != null && !currentUser.getAddresses().isEmpty()) {
            User.Address shipping = null;
            for (User.Address a : currentUser.getAddresses()) {
                if ("Shipping".equalsIgnoreCase(a.getAddressType())) {
                    shipping = a;
                    break;
                }
            }
            if (shipping == null) shipping = currentUser.getAddresses().get(0);

            binding.layoutAddressExists.setVisibility(View.VISIBLE);
            binding.btnAddAddress.setVisibility(View.GONE);
            binding.tvUserAddress.setText(shipping.getAddressLine1() + ", " + shipping.getCity());
        }
    }

    private void displayBillingAddress() {
        if (currentUser.getAddresses() != null) {
            User.Address billing = null;
            for (User.Address a : currentUser.getAddresses()) {
                if ("Billing".equalsIgnoreCase(a.getAddressType())) {
                    billing = a;
                    break;
                }
            }
            if (billing != null) {
                binding.tvBillingAddress.setText(billing.getAddressLine1() + ", " + billing.getCity());
                binding.btnAddBillingAddress.setVisibility(View.GONE);
            } else {
                binding.tvBillingAddress.setText("No Billing Address found");
                binding.btnAddBillingAddress.setVisibility(View.VISIBLE);
            }
        }
    }

    private void loadCartData() {
        String uid = firebaseAuth.getUid();
        if (uid == null) return;
        firestore.collection("users").document(uid).collection("cart").get()
                .addOnSuccessListener(query -> {
                    orderItems.clear();
                    for (DocumentSnapshot doc : query) {
                        CartItem item = doc.toObject(CartItem.class);
                        if (item != null) orderItems.add(item);
                    }
                    adapter.notifyDataSetChanged();
                });
    }

    private void placeOrder() {
        if (currentUser == null || orderItems.isEmpty()) return;
        binding.btnPlaceOrder.setEnabled(false);
        Toast.makeText(getContext(), "Processing order...", Toast.LENGTH_SHORT).show();

        List<Order.OrderItem> orderItemsList = new ArrayList<>();

        for (int i = 0; i < orderItems.size(); i++) {
            CartItem cartItem = orderItems.get(i);

            firestore.collection("products").document(cartItem.getProductId()).get()
                    .addOnSuccessListener(pDoc -> {
                        if (pDoc.exists()) {
                            Product product = pDoc.toObject(Product.class);
                            List<Order.OrderItem.Attribute> selectedAttributes = new ArrayList<>();

                            if (cartItem.getAttributes() != null) {
                                for (CartItem.SelectedAttribute cartAttr : cartItem.getAttributes()) {
                                    selectedAttributes.add(new Order.OrderItem.Attribute(
                                            cartAttr.getAttributeName(),
                                            cartAttr.getAttributeValue()
                                    ));
                                }
                            }

                            Order.OrderItem orderItem = Order.OrderItem.builder()
                                    .productId(cartItem.getProductId())
                                    .productName(pDoc.getString("name"))
                                    .productImage(product != null && product.getImages() != null && !product.getImages().isEmpty()
                                            ? product.getImages().get(0) : null)
                                    .quantity(cartItem.getQuantity())
                                    .unitPrice(pDoc.getDouble("price") != null ? pDoc.getDouble("price") : 0.0)
                                    .attributes(selectedAttributes)
                                    .build();

                            orderItemsList.add(orderItem);
                        }

                        if (orderItemsList.size() == orderItems.size()) {
                            saveOrderToFirestore(orderItemsList);
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e("CHECKOUT", "Product fetch failed: " + e.getMessage());
                        binding.btnPlaceOrder.setEnabled(true);
                    });
        }
    }

    private void saveOrderToFirestore(List<Order.OrderItem> orderItemsList) {
        User.Address shipRaw = null;
        if (currentUser.getAddresses() != null && !currentUser.getAddresses().isEmpty()) {
            shipRaw = currentUser.getAddresses().get(0);
        }

        User.Address billRaw = binding.cbSameAddress.isChecked() ? shipRaw : shipRaw;

        String orderId = firestore.collection("orders").document().getId();
        String method = binding.rbCOD.isChecked() ? "Cash on Delivery" : "Card Payment";
        String pStatus = binding.rbCOD.isChecked() ? "Pending" : "Paid";

        Order finalOrder = Order.builder()
                .orderId(orderId).userId(firebaseAuth.getUid()).totalAmount(finalTotal)
                .status("Pending").orderDate(com.google.firebase.Timestamp.now())
                .paymentMethod(method).paymentStatus(pStatus)
                .shippingAddress(convertToOrderAddress(shipRaw))
                .billingAddress(convertToOrderAddress(billRaw))
                .orderItems(orderItemsList).build();

        firestore.collection("orders").document(orderId).set(finalOrder)
                .addOnSuccessListener(aVoid -> clearCartAndFinish());
    }

    private void clearCartAndFinish() {
        String uid = firebaseAuth.getUid();
        if (uid == null) return;

        firestore.collection("users").document(uid).collection("cart").get()
                .addOnSuccessListener(query -> {
                    WriteBatch batch = firestore.batch();

                    // 1. Cart එකේ Items ටික අයින් කරනවා
                    for (DocumentSnapshot doc : query) {
                        batch.delete(doc.getReference());
                    }

                    // 2. Claim කරපු Coins ටික Database එකෙන් අඩු කරනවා
                    if (isCoinsClaimed && claimedCoins > 0) {
                        // 'fitCoins' කියන field එක තමයි ඔයාගේ User model එකේ තියෙන නම කියලා හිතනවා
                        batch.update(firestore.collection("users").document(uid),
                                "fitCoins", FieldValue.increment(-claimedCoins));
                    }

                    // 3. එකවර (Atomically) ඔක්කොම Update කරනවා
                    batch.commit().addOnSuccessListener(aVoid -> {
                        Toast.makeText(getContext(), "Order Placed Successfully!", Toast.LENGTH_LONG).show();
                        requireActivity().onBackPressed();
                    }).addOnFailureListener(e -> {
                        Log.e("CHECKOUT", "Batch commit failed: " + e.getMessage());
                    });
                });
    }

    private Order.Address convertToOrderAddress(User.Address u) {
        if (u == null) return null;
        return Order.Address.builder().name(currentUser.getName()).email(currentUser.getEmail())
                .contactNo(currentUser.getContactNumber()).addressLine1(u.getAddressLine1())
                .addressLine2(u.getAddressLine2()).city(u.getCity()).postalCode(u.getState()).build();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadUserData();
        if (requireActivity() instanceof HomeActivity) {
            ((HomeActivity) requireActivity()).setNavigationVisibility(View.GONE);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (requireActivity() instanceof HomeActivity) {
            ((HomeActivity) requireActivity()).setNavigationVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}