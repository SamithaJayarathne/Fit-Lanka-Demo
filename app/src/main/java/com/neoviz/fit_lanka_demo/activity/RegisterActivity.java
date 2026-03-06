package com.neoviz.fit_lanka_demo.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.firestore.FirebaseFirestore;
import com.neoviz.fit_lanka_demo.databinding.ActivityRegisterBinding;
import com.neoviz.fit_lanka_demo.model.User;
import com.neoviz.fit_lanka_demo.model.UserRole;

import java.util.ArrayList;

public class RegisterActivity extends AppCompatActivity {

    private ActivityRegisterBinding binding;
    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firestore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        binding = ActivityRegisterBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        firebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        /// create new account btn click listener
        binding.registerBtn.setOnClickListener(view -> {
            registerNewUser();
        });

        binding.loginText.setOnClickListener(view -> {
            Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
        });
    }

    private void registerNewUser() {
        String name = binding.nameInput.getText().toString().trim();
        String email = binding.emailInput.getText().toString().trim();
        String phone = binding.phoneInput.getText().toString().trim();
        String password = binding.passwordInput.getText().toString();

        if (name.isEmpty()) {
            showError("User name is required");
            return;
        }
        if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            showError("Valid email is required");
            return;
        }
        if (phone.length() != 10) {
            showError("Invalid Phone Number (10 digits required)");
            return;
        }
        if (password.length() < 6) {
            showError("Password must be at least 6 characters");
            return;
        }

        toggleLoading(true);

        firebaseAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        String uid = task.getResult().getUser().getUid();

                        User user = User.builder()
                                .uid(uid)
                                .name(name)
                                .email(email)
                                .contactNumber(phone)
                                .role(UserRole.ROLE_USER)
                                .createdAt(Timestamp.now())
                                .isActive(true)
                                .fitCoins(0)
                                .addresses(new ArrayList<>())
                                .build();

                        firestore.collection("users")
                                .document(uid)
                                .set(user)
                                .addOnSuccessListener(aVoid -> {
                                    toggleLoading(false);
                                    clearFields();
                                    Toast.makeText(this, "Registration Successful!", Toast.LENGTH_SHORT).show();

                                })
                                .addOnFailureListener(e -> {
                                    toggleLoading(false);
                                    showError("Firestore Error: " + e.getMessage());
                                });

                    } else {
                        toggleLoading(false);
                        try {
                            throw task.getException();
                        } catch (FirebaseAuthUserCollisionException e) {
                            showError("Email is already registered");
                        } catch (Exception e) {
                            showError("Registration Failed: " + e.getMessage());
                        }
                    }
                });
    }

    private void toggleLoading(boolean isLoading) {
        if (isLoading) {
            binding.registerBtn.setText("");
            binding.registerBtn.setEnabled(false);
            binding.registerProgressIndicator.setVisibility(View.VISIBLE);
            binding.registrationErrorText.setVisibility(View.GONE);
        } else {
            binding.registerBtn.setText("Create Account");
            binding.registerBtn.setEnabled(true);
            binding.registerProgressIndicator.setVisibility(View.GONE);
        }
    }

    private void showError(String msg) {
        binding.registrationErrorText.setText(msg);
        binding.registrationErrorText.setVisibility(View.VISIBLE);
    }

    private void clearFields() {
        binding.nameInput.setText("");
        binding.emailInput.setText("");
        binding.phoneInput.setText("");
        binding.passwordInput.setText("");
    }
}