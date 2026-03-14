package com.neoviz.fit_lanka_demo.activity;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;
import android.widget.Toolbar;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.GravityCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.navigation.NavigationBarView;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.neoviz.fit_lanka_demo.R;
import com.neoviz.fit_lanka_demo.databinding.ActivityHomeBinding;
import com.neoviz.fit_lanka_demo.databinding.ActivityLoginBinding;
import com.neoviz.fit_lanka_demo.databinding.NavHeaderBinding;
import com.neoviz.fit_lanka_demo.fragment.ChallengesFragment;
import com.neoviz.fit_lanka_demo.fragment.HomeFragment;
import com.neoviz.fit_lanka_demo.fragment.LeaderboardFragment;
import com.neoviz.fit_lanka_demo.fragment.ShopFragment;
import com.neoviz.fit_lanka_demo.fragment.WorkoutFragment;
import com.neoviz.fit_lanka_demo.model.User;
import com.neoviz.fit_lanka_demo.service.StepCounterService;

public class HomeActivity extends AppCompatActivity implements
        NavigationView.OnNavigationItemSelectedListener,
        NavigationBarView.OnItemSelectedListener {

    private ActivityHomeBinding binding;
    private NavHeaderBinding headerBinding;
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private BottomNavigationView bottomNavigationView;
    private MaterialToolbar toolbar;
    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firestore;
    private androidx.appcompat.app.AlertDialog loadingDialog;

    private final ActivityResultLauncher<Intent> pickImageLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri imageURI = result.getData().getData();
                    uploadImageToFirebase(imageURI);
                }
            }
    );


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);


        binding = ActivityHomeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        View headerView = binding.sideNavigationView.getHeaderView(0);
        headerBinding = NavHeaderBinding.bind(headerView);

        loadFragment(new HomeFragment());

        firebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
        drawerLayout = binding.drawerLayout;
        navigationView = binding.sideNavigationView;
        bottomNavigationView = binding.bottomNavigationView;
        toolbar = binding.homeToolbar;

        setSupportActionBar(toolbar);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this,
                drawerLayout,
                toolbar,
                R.string.navigation_drawer_open,
                R.string.navigation_drawer_close
        );

        loadUserDetails();

        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        getOnBackPressedDispatcher().addCallback(new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START);
                } else {
                    finish();
                }
            }
        });

        navigationView.setNavigationItemSelectedListener(this);
        bottomNavigationView.setOnItemSelectedListener(this);

        headerBinding.navHeaderImg.setOnClickListener(view -> {
            openGallery();
        });

        startStepService();

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
                    getSupportFragmentManager().popBackStack();
                } else {
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                }
            }
        });

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 101) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // දැන් පර්මිෂන් ලැබුණා, සර්විස් එක පටන් ගන්න
                startMyForegroundService();
            } else {
                // User පර්මිෂන් එක reject කළොත් පියවර මනින්න බැරි බව කියන්න
                Toast.makeText(this, "Permission denied. Step counter won't work.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void startStepService() {
        // Android 10 (Q) සහ ඊට ඉහළ නම් ACTIVITY_RECOGNITION ඕනේ
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_GRANTED) {
                startMyForegroundService();
            } else {
                // පර්මිෂන් ඉල්ලන්න
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACTIVITY_RECOGNITION}, 101);
            }
        } else {
            // පරණ Android version වලට කෙලින්ම start කරන්න
            startMyForegroundService();
        }
    }

    private void startMyForegroundService() {
        Intent serviceIntent = new Intent(this, StepCounterService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }
    }

    private void uploadImageToFirebase(Uri uri) {
        String uid = firebaseAuth.getCurrentUser().getUid();

        showLoadingDialog(true);

        StorageReference storageRef = FirebaseStorage.getInstance().getReference()
                .child("profile-images")
                .child(uid)
                .child("profile.jpg");

        storageRef.putFile(uri).addOnSuccessListener(ts -> {
            ts.getStorage().getDownloadUrl().addOnSuccessListener(downloadUri -> {
                String imageURL = downloadUri.toString();

                firestore.collection("users")
                        .document(uid)
                        .update("profileImageUrl", imageURL)
                        .addOnSuccessListener(aVoid -> {
                            loadUserDetails();
                            showLoadingDialog(false);
                            Toast.makeText(this, "Profile Updated!", Toast.LENGTH_SHORT).show();
                        });
            });

        }).addOnFailureListener(e -> {
            showLoadingDialog(false);
            Toast.makeText(this, "Upload Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }

    private void openGallery() {
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        pickImageLauncher.launch(intent);
    }

    private void loadUserDetails() {
        if (firebaseAuth.getCurrentUser() != null) {
            String uid = firebaseAuth.getCurrentUser().getUid();

            firestore.collection("users")
                    .document(uid)
                    .get()
                    .addOnSuccessListener(qds -> {

                        User user = qds.toObject(User.class);

                        if (user != null) {
                            headerBinding.navHeaderName.setText(user.getName());
                            headerBinding.navHeaderPoints.setText(String.format("%.2f Fit-Coins", user.getFitCoins()));

                            if (user.getProfileImageUrl() != null && !user.getProfileImageUrl().isEmpty()) {
                                Glide.with(getApplicationContext())
                                        .load(user.getProfileImageUrl())
                                        .placeholder(R.drawable.profile_24px)
                                        .centerCrop()
                                        .into(headerBinding.navHeaderImg);
                            } else if (firebaseAuth.getCurrentUser().getPhotoUrl() != null) {
                                Glide.with(getApplicationContext())
                                        .load(firebaseAuth.getCurrentUser().getPhotoUrl())
                                        .centerCrop()
                                        .into(headerBinding.navHeaderImg);
                            }
                        }
                    })
                    .addOnFailureListener(ex -> {
                        Log.e("HomeActivity", "Error loading user details", ex);
                    });
        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {

        int itemId = menuItem.getItemId();

        Menu navMenu = navigationView.getMenu();
        Menu bottomNavMenu = bottomNavigationView.getMenu();

        for (int i = 0; i < navMenu.size(); i++) {
            navMenu.getItem(i).setChecked(false);
        }

        for (int i = 0; i < bottomNavMenu.size(); i++) {
            bottomNavMenu.getItem(i).setChecked(false);
        }

        if (itemId == R.id.side_nav_home || itemId == R.id.bottom_nav_home) {
            navigationView.setCheckedItem(R.id.side_nav_home);
            bottomNavigationView.getMenu().findItem(R.id.bottom_nav_home).setChecked(true);
            loadFragment(new HomeFragment());

        } else if (itemId == R.id.side_nav_workout || itemId == R.id.bottom_nav_workouts) {
            navigationView.setCheckedItem(R.id.side_nav_workout);
            bottomNavigationView.getMenu().findItem(R.id.bottom_nav_workouts).setChecked(true);
            loadFragment(new WorkoutFragment());

        } else if (itemId == R.id.side_nav_leaderboard || itemId == R.id.bottom_nav_leaderboard) {
            navigationView.setCheckedItem(R.id.side_nav_leaderboard);
            bottomNavigationView.getMenu().findItem(R.id.bottom_nav_leaderboard).setChecked(true);
            loadFragment(new LeaderboardFragment());

        } else if (itemId == R.id.side_nav_shop || itemId == R.id.bottom_nav_shop) {
            navigationView.setCheckedItem(R.id.side_nav_shop);
            bottomNavigationView.getMenu().findItem(R.id.bottom_nav_shop).setChecked(true);
            loadFragment(new ShopFragment());

        } else if (itemId == R.id.side_nav_profile) {
            navigationView.setCheckedItem(R.id.side_nav_shop);

            Intent intent = new Intent(this, ProfileActivity.class);

            intent.putExtra("name", "sasd");

            startActivity(intent);

        } else if (itemId == R.id.side_nav_login) {
            navigationView.setCheckedItem(R.id.side_nav_login);
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
            finish();

        } else if (itemId == R.id.side_nav_cart) {
            navigationView.setCheckedItem(R.id.side_nav_cart);
            bottomNavigationView.getMenu().findItem(R.id.bottom_nav_shop).setChecked(true);
            ShopFragment fragment = new ShopFragment();

            Bundle bundle = new Bundle();
            bundle.putInt("targetTab", 2);
            fragment.setArguments(bundle);
            loadFragment(fragment);

        } else if (itemId == R.id.side_nav_wishlist) {
            navigationView.setCheckedItem(R.id.side_nav_wishlist);
            bottomNavigationView.getMenu().findItem(R.id.bottom_nav_shop).setChecked(true);
            ShopFragment fragment = new ShopFragment();
            Bundle bundle = new Bundle();
            bundle.putInt("targetTab", 1);
            fragment.setArguments(bundle);
            loadFragment(fragment);

        } else if (itemId == R.id.side_nav_orders) {
            navigationView.setCheckedItem(R.id.side_nav_orders);
            bottomNavigationView.getMenu().findItem(R.id.bottom_nav_shop).setChecked(true);
            ShopFragment fragment = new ShopFragment();
            Bundle bundle = new Bundle();
            bundle.putInt("targetTab", 3);
            fragment.setArguments(bundle);
            loadFragment(fragment);

        } else if (itemId == R.id.side_nav_logout) {
            new MaterialAlertDialogBuilder(this)
                    .setTitle("Logout")
                    .setMessage("Are you sure you want to logout")
                    .setPositiveButton("Logout", (dialog, which) -> {
                        FirebaseAuth.getInstance().signOut();
                        Intent intent = new Intent(HomeActivity.this, LoginActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        } else if (itemId == R.id.side_nav_challenges) {
            navigationView.setCheckedItem(R.id.side_nav_challenges);
            loadFragment(new ChallengesFragment());

        }

        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container_view, fragment)
                .addToBackStack(null)
                .commit();
    }

    private void showLoadingDialog(boolean isShow) {
        if (isShow) {

            if (loadingDialog == null) {
                View view = getLayoutInflater().inflate(R.layout.dialog_loading, null);
                loadingDialog = new MaterialAlertDialogBuilder(this)
                        .setView(view)
                        .setCancelable(false)
                        .create();

            }
            loadingDialog.show();

        } else {
            if (loadingDialog != null && loadingDialog.isShowing()) {
                loadingDialog.dismiss();
            }
        }
    }

    public void setNavigationVisibility(int visibility) {
        binding.bottomNavigationView.setVisibility(visibility);
        binding.appBarLayout.setVisibility(visibility);
    }
}