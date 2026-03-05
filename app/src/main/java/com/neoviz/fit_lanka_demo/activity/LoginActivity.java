package com.neoviz.fit_lanka_demo.activity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.credentials.Credential;
import androidx.credentials.CredentialManager;
import androidx.credentials.GetCredentialRequest;
import androidx.credentials.GetCredentialResponse;
import androidx.credentials.exceptions.GetCredentialException;

import com.google.android.libraries.identity.googleid.GetGoogleIdOption;
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.FirebaseFirestore;
import com.neoviz.fit_lanka_demo.R;
import com.neoviz.fit_lanka_demo.databinding.ActivityLoginBinding;
import com.neoviz.fit_lanka_demo.model.User;
import com.neoviz.fit_lanka_demo.model.UserRole;

import java.util.ArrayList;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "TAG_AUTH";
    private ActivityLoginBinding binding;
    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firestore;
    private CredentialManager credentialManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        firebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
        credentialManager = CredentialManager.create(this);

        binding.loginBtn.setOnClickListener(view -> {
            userLogin();
        });

        binding.googleSignInBtn.setOnClickListener(view -> {
            signInWithGoogle();
        });

        binding.signUpText.setOnClickListener(view -> {
            startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
            finish();
        });
    }


    private void userLogin() {
        String email = binding.emailInput.getText().toString().trim();
        String password = binding.passwordInput.getText().toString().trim();


        binding.loginErrorText.setVisibility(View.GONE);

        if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            showError("Please enter a valid email address");
            return;
        }
        if (password.isEmpty()) {
            showError("Please enter your password");
            return;
        }

        setLoginLoading(true);

        firebaseAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "Normal Login: Success");
                        navigateToHome();
                    } else {
                        setLoginLoading(false);
                        Log.e(TAG, "Normal Login: Failed", task.getException());
                        showError("Login Failed: " + task.getException().getLocalizedMessage());
                    }
                });
    }


    private void signInWithGoogle() {
        binding.loginErrorText.setVisibility(View.GONE);
        setGoogleLoading(true);

        GetGoogleIdOption googleIdOption = new GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(getString(R.string.default_web_client_id))
                .build();

        GetCredentialRequest request = new GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build();

        credentialManager.getCredentialAsync(this, request, null, Runnable::run,
                new androidx.credentials.CredentialManagerCallback<GetCredentialResponse, GetCredentialException>() {
                    @Override
                    public void onResult(GetCredentialResponse result) {
                        handleSignInResult(result);
                    }

                    @Override
                    public void onError(@NonNull GetCredentialException e) {
                        setGoogleLoading(false);
                        Log.e(TAG, "Google Error: " + e.getMessage());
                        showError("Google Sign-in failed: " + e.getMessage());
                    }
                });
    }

    private void handleSignInResult(GetCredentialResponse result) {
        Credential credential = result.getCredential();
        try {
            if (credential.getType().equals(GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL)) {
                GoogleIdTokenCredential googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.getData());
                firebaseAuthWithGoogle(googleIdTokenCredential.getIdToken());
            }
        } catch (Exception e) {
            setGoogleLoading(false);
            showError("Error parsing Google data");
            Log.e(TAG, "Error parsing Google data", e);
        }
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        firebaseAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        if (task.getResult().getAdditionalUserInfo().isNewUser()) {
                            saveUserToFirestore(task.getResult().getUser());
                        } else {
                            navigateToHome();
                        }
                    } else {
                        setGoogleLoading(false);
                        showError("Firebase Auth Failed with Google");
                    }
                });
    }

    private void saveUserToFirestore(FirebaseUser firebaseUser) {
        User user = User.builder()
                .uid(firebaseUser.getUid())
                .name(firebaseUser.getDisplayName())
                .email(firebaseUser.getEmail())
                .contactNumber("")
                .role(UserRole.ROLE_USER)
                .createdAt(Timestamp.now())
                .isActive(true)
                .rewardPoints(0)
                .addresses(new ArrayList<>())
                .build();

        firestore.collection("users").document(firebaseUser.getUid())
                .set(user)
                .addOnSuccessListener(aVoid -> navigateToHome())
                .addOnFailureListener(e -> {
                    setGoogleLoading(false);
                    showError("Failed to save user data");
                    Log.e(TAG, "Firestore Error", e);
                });
    }

    private void showError(String message) {
        binding.loginErrorText.setText(message);
        binding.loginErrorText.setVisibility(View.VISIBLE);
    }

    private void setLoginLoading(boolean isLoading) {
        if (isLoading) {
            binding.loginBtn.setText("");
            binding.loginBtn.setEnabled(false);
            binding.loginProgressBar.setVisibility(View.VISIBLE);
        } else {
            binding.loginBtn.setText("Login");
            binding.loginBtn.setEnabled(true);
            binding.loginProgressBar.setVisibility(View.GONE);
        }
    }

    private void setGoogleLoading(boolean isLoading) {
        if (isLoading) {
            binding.googleSignInBtn.setText("");
            binding.googleSignInBtn.setIcon(null);
            binding.googleSignInBtn.setEnabled(false);
            binding.googleProgressBar.setVisibility(View.VISIBLE);
        } else {
            binding.googleSignInBtn.setText("Continue with Google");
            binding.googleSignInBtn.setIconResource(R.drawable.ic_google);
            binding.googleSignInBtn.setEnabled(true);
            binding.googleProgressBar.setVisibility(View.GONE);
        }
    }

    private void navigateToHome() {
        Intent intent = new Intent(LoginActivity.this, HomeActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}