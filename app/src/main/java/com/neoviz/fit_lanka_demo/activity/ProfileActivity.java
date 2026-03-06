package com.neoviz.fit_lanka_demo.activity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.neoviz.fit_lanka_demo.R;
import com.neoviz.fit_lanka_demo.databinding.ActivityProfileBinding;
import com.neoviz.fit_lanka_demo.model.User;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProfileActivity extends AppCompatActivity {

    private ActivityProfileBinding binding;
    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firestore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);
        binding = ActivityProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ViewCompat.setOnApplyWindowInsetsListener(binding.mainProfileLayout, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        firebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        setSupportActionBar(binding.profileToolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("My Profile");
        }
        binding.profileToolbar.setNavigationOnClickListener(v -> finish());

        binding.statWeight.statLabel.setText("Weight (kg)");
        binding.statHeight.statLabel.setText("Height (cm)");
        binding.statPoints.statLabel.setText("Fit Coins");

        loadUserDetails();

        binding.editProfileLayout.setOnClickListener(view ->
                toggleContainer(binding.editProfileContainer, binding.profileDetailsArrow));
        binding.editAddressLayout.setOnClickListener(view ->
                toggleContainer(binding.editAddressContainer, binding.addressDetailsArrow));

        binding.btnSaveProfile.setOnClickListener(view -> updateUserProfile());
        binding.btnSaveAddress.setOnClickListener(view -> updateAddressDetails());

        binding.chipGroupAddressType.setOnCheckedStateChangeListener((group, checkedIds) -> {
            loadAddressFromLocal(binding.chipShipping.isChecked());
        });

        binding.profileBtnLogout.setOnClickListener(view->{
           logOut();
        });
    }

    private void logOut(){
        new MaterialAlertDialogBuilder(this)
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout")
                .setPositiveButton("Logout", (dialog, which) -> {
                    FirebaseAuth.getInstance().signOut();
                    Intent intent = new Intent(ProfileActivity.this, LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void toggleContainer(View container, View arrow) {
        if (container.getVisibility() == View.GONE) {
            container.setVisibility(View.VISIBLE);
            arrow.setRotation(180f);
        } else {
            container.setVisibility(View.GONE);
            arrow.setRotation(0f);
        }
    }

    private void loadUserDetails() {
        if (firebaseAuth.getCurrentUser() == null) return;
        String uid = firebaseAuth.getCurrentUser().getUid();

        firestore.collection("users").document(uid).get()
                .addOnSuccessListener(documentSnapshot -> {
                    User user = documentSnapshot.toObject(User.class);
                    if (user != null) {

                        binding.profileName.setText(user.getName());
                        binding.profileEmail.setText(user.getEmail());
                        binding.editName.setText(user.getName());
                        binding.editPhone.setText(user.getContactNumber());


                        binding.statWeight.statValue.setText(String.valueOf(user.getWeight()));
                        binding.statHeight.statValue.setText(String.valueOf(user.getHeight()));
                        binding.statPoints.statValue.setText(String.format("%.1f", user.getFitCoins()));


                        if (user.getProfileImageUrl() != null && !user.getProfileImageUrl().isEmpty()) {
                            Glide.with(this).load(user.getProfileImageUrl())
                                    .placeholder(R.drawable.profile_24px).centerCrop().into(binding.profileUserImg);
                        }


                        loadAddressFromLocal(binding.chipShipping.isChecked());
                    }
                })
                .addOnFailureListener(e -> Log.e("Profile", "Load failed: " + e.getMessage()));
    }

    private void updateUserProfile() {
        String uid = firebaseAuth.getCurrentUser().getUid();
        String name = binding.editName.getText().toString().trim();
        String phone = binding.editPhone.getText().toString().trim();
        String wStr = binding.editWeight.getText().toString().trim();
        String hStr = binding.editHeight.getText().toString().trim();

        if (name.isEmpty() || phone.isEmpty()) {
            Toast.makeText(this, "Name and Phone are required", Toast.LENGTH_SHORT).show();
            return;
        }

        binding.btnSaveProfile.setEnabled(false);
        binding.saveProfileProgress.setVisibility(View.VISIBLE);

        Map<String, Object> updates = new HashMap<>();
        updates.put("name", name);
        updates.put("contactNumber", phone);
        try {
            if (!wStr.isEmpty()) updates.put("weight", Double.parseDouble(wStr));
            if (!hStr.isEmpty()) updates.put("height", Double.parseDouble(hStr));
        } catch (Exception e) {

        }

        firestore.collection("users").document(uid).update(updates)
                .addOnSuccessListener(aVoid -> {
                    resetButtonState();
                    loadUserDetails();
                    Toast.makeText(this, "Profile Updated!", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    resetButtonState();
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void updateAddressDetails() {
        String uid = firebaseAuth.getCurrentUser().getUid();
        String l1 = binding.editAddrLine1.getText().toString().trim();
        String l2 = binding.editAddrLine2.getText().toString().trim();
        String city = binding.editCity.getText().toString().trim();
        String state = binding.editState.getText().toString().trim();
        String type = binding.chipShipping.isChecked() ? "Shipping" : "Billing";

        if (l1.isEmpty() || city.isEmpty()) {
            Toast.makeText(this, "Address Line 1 and City are required", Toast.LENGTH_SHORT).show();
            return;
        }

        binding.btnSaveAddress.setEnabled(false);
        binding.btnSaveAddress.setText("Updating...");

        firestore.collection("users").document(uid).get().addOnSuccessListener(doc -> {
            User user = doc.toObject(User.class);
            List<User.Address> addressList = new ArrayList<>();
            if (user != null && user.getAddresses() != null) {
                addressList.addAll(user.getAddresses());
            }

            User.Address newAddr = User.Address.builder()
                    .addressId(String.valueOf(System.currentTimeMillis()))
                    .addressType(type)
                    .addressLine1(l1)
                    .addressLine2(l2)
                    .city(city)
                    .state(state)
                    .build();

            addressList.removeIf(a -> a.getAddressType().equals(type));
            addressList.add(newAddr);

            firestore.collection("users").document(uid).update("addresses", addressList)
                    .addOnSuccessListener(aVoid -> {
                        resetAddressButton();
                        Toast.makeText(this, type + " Address Saved!", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        resetAddressButton();
                        Toast.makeText(this, "Update failed", Toast.LENGTH_SHORT).show();
                    });
        });
    }

    private void loadAddressFromLocal(boolean isShipping) {
        String uid = firebaseAuth.getCurrentUser().getUid();
        String targetType = isShipping ? "Shipping" : "Billing";

        firestore.collection("users").document(uid).get().addOnSuccessListener(doc -> {
            User user = doc.toObject(User.class);
            if (user != null && user.getAddresses() != null) {
                User.Address found = null;
                for (User.Address a : user.getAddresses()) {
                    if (targetType.equals(a.getAddressType())) {
                        found = a;
                        break;
                    }
                }

                if (found != null) {
                    binding.editAddrLine1.setText(found.getAddressLine1());
                    binding.editAddrLine2.setText(found.getAddressLine2());
                    binding.editCity.setText(found.getCity());
                    binding.editState.setText(found.getState());
                } else {
                    clearAddressInputs();
                }
            }
        });
    }

    private void clearAddressInputs() {
        binding.editAddrLine1.setText("");
        binding.editAddrLine2.setText("");
        binding.editCity.setText("");
        binding.editState.setText("");
    }

    private void resetButtonState() {
        binding.saveProfileProgress.setVisibility(View.GONE);
        binding.btnSaveProfile.setEnabled(true);
        binding.btnSaveProfile.setText("Save Changes");
    }

    private void resetAddressButton() {
        binding.btnSaveAddress.setEnabled(true);
        binding.btnSaveAddress.setText("Update Address");
    }
}