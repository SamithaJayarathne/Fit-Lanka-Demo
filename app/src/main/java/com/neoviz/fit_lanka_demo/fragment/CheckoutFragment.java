package com.neoviz.fit_lanka_demo.fragment;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
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
import com.neoviz.fit_lanka_demo.R;
import com.neoviz.fit_lanka_demo.activity.HomeActivity;
import com.neoviz.fit_lanka_demo.activity.ProfileActivity;
import com.neoviz.fit_lanka_demo.adapter.MinimalOrderAdapter;
import com.neoviz.fit_lanka_demo.api.EmailServiceApi;
import com.neoviz.fit_lanka_demo.client.RetrofitClient;
import com.neoviz.fit_lanka_demo.databinding.FragmentCheckoutBinding;
import com.neoviz.fit_lanka_demo.model.CartItem;
import com.neoviz.fit_lanka_demo.model.Order;
import com.neoviz.fit_lanka_demo.model.Product;
import com.neoviz.fit_lanka_demo.model.User;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lk.payhere.androidsdk.PHConstants;
import lk.payhere.androidsdk.PHMainActivity;
import lk.payhere.androidsdk.PHResponse;
import lk.payhere.androidsdk.model.InitRequest;
import lk.payhere.androidsdk.model.StatusResponse;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CheckoutFragment extends Fragment {

    private FragmentCheckoutBinding binding;
    private FirebaseFirestore firestore;
    private FirebaseAuth firebaseAuth;

    private MinimalOrderAdapter adapter;
    private List<CartItem> orderItems = new ArrayList<>();
    private double finalTotal = 0.0;
    private User currentUser;
    private int claimedCoins = 0;
    private boolean isCoinsClaimed = false;
    private boolean isDirectBuy = false;
    private Order currentOrder;

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

        setupRecyclerView();
        setupLogic();
        loadUserData();

        if (getArguments() != null) {
            isDirectBuy = getArguments().getBoolean("isDirectBuy", false);
            finalTotal = getArguments().getDouble("finalTotal", 0.0);
            isCoinsClaimed = getArguments().getBoolean("isCoinsClaimed", false);
            claimedCoins = getArguments().getInt("claimedCoinsAmount", 0);

            binding.tvFinalTotal.setText(String.format("Rs. %,.2f", finalTotal));

            if (isDirectBuy) {
                setupDirectOrderData();
            } else {
                loadCartData();
            }
        }

        binding.toolbar.setNavigationOnClickListener(v -> requireActivity().onBackPressed());

        binding.btnPlaceOrder.setOnClickListener(v -> {
            if (currentUser == null) {
                Toast.makeText(getContext(), "Loading user data...", Toast.LENGTH_SHORT).show();
                return;
            }

            if (binding.rbCard.isChecked()) {
                runPayhereGateway();
            } else {
                placeOrder();
            }
        });
    }

    private void setupDirectOrderData() {
        Product directProduct = (Product) getArguments().getSerializable("directProduct");
        int qty = getArguments().getInt("directQuantity");
        List<CartItem.SelectedAttribute> attrs = (List<CartItem.SelectedAttribute>) getArguments().getSerializable("directAttributes");

        if (directProduct != null) {
            orderItems.clear();
            CartItem directItem = CartItem.builder()
                    .productId(directProduct.getProductId())
                    .quantity(qty)
                    .attributes(attrs)
                    .build();
            orderItems.add(directItem);
            adapter.notifyDataSetChanged();
        }
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
                    PHResponse<StatusResponse> response = (PHResponse<StatusResponse>) result.getData().getSerializableExtra(PHConstants.INTENT_EXTRA_RESULT);
                    if (response != null && response.isSuccess()) {
                        placeOrder();
                    } else {
                        Toast.makeText(getContext(), "Payment Failed", Toast.LENGTH_SHORT).show();
                    }
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
                    if (currentUser != null) displayShippingAddress();
                });
    }

    private void displayShippingAddress() {
        if (currentUser.getAddresses() != null && !currentUser.getAddresses().isEmpty()) {
            User.Address shipping = null;
            for (User.Address a : currentUser.getAddresses()) {
                if ("Shipping".equalsIgnoreCase(a.getAddressType())) { shipping = a; break; }
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
                if ("Billing".equalsIgnoreCase(a.getAddressType())) { billing = a; break; }
            }
            if (billing != null) {
                binding.tvBillingAddress.setText(billing.getAddressLine1() + ", " + billing.getCity());
                binding.btnAddBillingAddress.setVisibility(View.GONE);
            } else {
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
        for (CartItem cartItem : orderItems) {
            firestore.collection("products").document(cartItem.getProductId()).get()
                    .addOnSuccessListener(pDoc -> {
                        if (pDoc.exists()) {
                            Product product = pDoc.toObject(Product.class);
                            List<Order.OrderItem.Attribute> selectedAttributes = new ArrayList<>();
                            if (cartItem.getAttributes() != null) {
                                for (CartItem.SelectedAttribute cartAttr : cartItem.getAttributes()) {
                                    selectedAttributes.add(new Order.OrderItem.Attribute(cartAttr.getAttributeName(), cartAttr.getAttributeValue()));
                                }
                            }
                            orderItemsList.add(Order.OrderItem.builder()
                                    .productId(cartItem.getProductId())
                                    .productName(pDoc.getString("name"))
                                    .productImage(product != null && product.getImages() != null && !product.getImages().isEmpty() ? product.getImages().get(0) : null)
                                    .quantity(cartItem.getQuantity())
                                    .unitPrice(pDoc.getDouble("price") != null ? pDoc.getDouble("price") : 0.0)
                                    .attributes(selectedAttributes)
                                    .build());
                        }
                        if (orderItemsList.size() == orderItems.size()) saveOrderToFirestore(orderItemsList);
                    });
        }
    }

    private void saveOrderToFirestore(List<Order.OrderItem> orderItemsList) {
        User.Address shipRaw = null;
        if (currentUser.getAddresses() != null && !currentUser.getAddresses().isEmpty()) {
            for (User.Address a : currentUser.getAddresses()) {
                if ("Shipping".equalsIgnoreCase(a.getAddressType())) { shipRaw = a; break; }
            }
            if (shipRaw == null) shipRaw = currentUser.getAddresses().get(0);
        }
        if (shipRaw == null) {
            Toast.makeText(getContext(), "Please add a shipping address", Toast.LENGTH_SHORT).show();
            binding.btnPlaceOrder.setEnabled(true);
            return;
        }

        String orderId = firestore.collection("orders").document().getId();
        currentOrder = Order.builder()
                .orderId(orderId).userId(firebaseAuth.getUid()).totalAmount(finalTotal)
                .status("Pending").orderDate(com.google.firebase.Timestamp.now())
                .paymentMethod(binding.rbCOD.isChecked() ? "Cash on Delivery" : "Card Payment")
                .paymentStatus(binding.rbCOD.isChecked() ? "Pending" : "Paid")
                .shippingAddress(convertToOrderAddress(shipRaw))
                .orderItems(orderItemsList).build();

        firestore.collection("orders").document(orderId).set(currentOrder)
                .addOnSuccessListener(aVoid -> {
                    sendOrderConfirmationEmail(currentOrder);
                    clearCartAndFinish();
                })
                .addOnFailureListener(e -> binding.btnPlaceOrder.setEnabled(true));
    }

    private void clearCartAndFinish() {
        String uid = firebaseAuth.getUid();
        WriteBatch batch = firestore.batch();
        if (isCoinsClaimed && claimedCoins > 0) {
            batch.update(firestore.collection("users").document(uid), "fitCoins", FieldValue.increment(-claimedCoins));
        }
        if (!isDirectBuy) {
            firestore.collection("users").document(uid).collection("cart").get()
                    .addOnSuccessListener(query -> {
                        for (DocumentSnapshot doc : query) batch.delete(doc.getReference());
                        executeBatch(batch);
                    });
        } else {
            executeBatch(batch);
        }
    }

    private void executeBatch(WriteBatch batch) {
        batch.commit().addOnSuccessListener(aVoid -> {
            if (isAdded() && getActivity() != null) {
                navigateToSuccess(currentOrder);
            }
        }).addOnFailureListener(e -> {
            binding.btnPlaceOrder.setEnabled(true);
            Toast.makeText(getContext(), "Failed to clear cart: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }

    private void sendOrderConfirmationEmail(Order order) {

        EmailServiceApi emailApi = RetrofitClient.getInstance().create(EmailServiceApi.class);
        Map<String, Object> emailBody = new HashMap<>();
        emailBody.put("sender", Map.of("name", "Fit Lanka", "email", "samithaai2003@gmail.com"));
        emailBody.put("to", List.of(Map.of("name", currentUser.getName(), "email", currentUser.getEmail())));
        emailBody.put("subject", "Order Confirmed: #" + order.getOrderId());


        StringBuilder itemsHtml = new StringBuilder();
        for (Order.OrderItem item : order.getOrderItems()) {
            itemsHtml.append("<tr>")
                    .append("<td style=\"padding: 10px; border-bottom: 1px solid #eee;\">").append(item.getProductName()).append("</td>")
                    .append("<td style=\"padding: 10px; border-bottom: 1px solid #eee; text-align: center;\">").append(item.getQuantity()).append("</td>")
                    .append("<td style=\"padding: 10px; border-bottom: 1px solid #eee; text-align: right;\">Rs. ").append(String.format("%,.2f", item.getUnitPrice())).append("</td>")
                    .append("</tr>");
        }

        String htmlContent = "<div style=\"font-family: 'Segoe UI', Arial, sans-serif; max-width: 600px; margin: auto; border: 1px solid #e0e0e0; border-radius: 10px; overflow: hidden;\">" +
                "  <div style=\"background-color: #000; padding: 25px; text-align: center;\">" +
                "    <h1 style=\"color: #fff; margin: 0; text-transform: uppercase; letter-spacing: 3px;\">Fit Lanka</h1>" +
                "  </div>" +
                "  <div style=\"padding: 30px; color: #333;\">" +
                "    <h2 style=\"color: #27ae60; margin-top: 0;\">Order Confirmed!</h2>" +
                "    <p>Hi <b>" + currentUser.getName() + "</b>, Your order has been placed successfully.</p>" +

                "    <div style=\"background-color: #f9f9f9; padding: 15px; border-radius: 5px; margin: 20px 0; font-size: 14px;\">" +
                "      <p style=\"margin: 5px 0;\"><b>Order ID:</b> #" + order.getOrderId() + "</p>" +
                "      <p style=\"margin: 5px 0;\"><b>Payment:</b> " + order.getPaymentMethod() + "</p>" +
                "    </div>" +

                "    <table style=\"width: 100%; border-collapse: collapse; margin: 20px 0;\">" +
                "      <thead>" +
                "        <tr style=\"background-color: #f2f2f2;\">" +
                "          <th style=\"padding: 10px; text-align: left;\">Item</th>" +
                "          <th style=\"padding: 10px; text-align: center;\">Qty</th>" +
                "          <th style=\"padding: 10px; text-align: right;\">Price</th>" +
                "        </tr>" +
                "      </thead>" +
                "      <tbody>" + itemsHtml.toString() + "</tbody>" +
                "    </table>" +

                "    <div style=\"text-align: right; margin-top: 20px;\">" +
                "      <p style=\"margin: 0; color: #777;\">Total Amount:</p>" +
                "      <p style=\"font-size: 22px; color: #000; font-weight: bold; margin: 5px 0;\">Rs. " + String.format("%,.2f", order.getTotalAmount()) + "</p>" +
                "    </div>" +

                "    <div style=\"margin-top: 30px; border-top: 1px solid #eee; padding-top: 20px;\">" +
                "       <p style=\"font-size: 14px; margin: 0;\"><b>Shipping Address:</b></p>" +
                "       <p style=\"font-size: 13px; color: #666; margin: 5px 0;\">" + order.getShippingAddress().getAddressLine1() + ", " + order.getShippingAddress().getCity() + "</p>" +
                "    </div>" +
                "  </div>" +
                "  <div style=\"background-color: #f1f1f1; padding: 20px; text-align: center; font-size: 12px; color: #777;\">" +
                "    <p>© 2026 Fit Lanka Fitness Store. All Rights Reserved.</p>" +
                "  </div>" +
                "</div>";

        emailBody.put("htmlContent", htmlContent);

        String apiKey = "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";

        emailApi.sendEmail(apiKey, emailBody)
                .enqueue(new Callback<Void>() {
                    @Override public void onResponse(Call<Void> call, Response<Void> response) {}
                    @Override public void onFailure(Call<Void> call, Throwable t) {}
                });
    }

    private Order.Address convertToOrderAddress(User.Address u) {
        if (u == null) return null;
        return Order.Address.builder().name(currentUser.getName()).email(currentUser.getEmail())
                .contactNo(currentUser.getContactNumber()).addressLine1(u.getAddressLine1())
                .addressLine2(u.getAddressLine2()).city(u.getCity()).postalCode(u.getState()).build();
    }

    private void navigateToSuccess(Order order) {
        SuccessFragment successFragment = new SuccessFragment();
        Bundle args = new Bundle();
        args.putString("orderId", order.getOrderId());
        args.putDouble("total", order.getTotalAmount());
        successFragment.setArguments(args);

        requireActivity().getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container_view, successFragment)
                .commit();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (requireActivity() instanceof HomeActivity) ((HomeActivity) requireActivity()).setNavigationVisibility(View.GONE);
    }

    @Override
    public void onPause() {
        super.onPause();
        if (requireActivity() instanceof HomeActivity) ((HomeActivity) requireActivity()).setNavigationVisibility(View.VISIBLE);
    }

    @Override
    public void onDestroyView() { super.onDestroyView(); binding = null; }
}