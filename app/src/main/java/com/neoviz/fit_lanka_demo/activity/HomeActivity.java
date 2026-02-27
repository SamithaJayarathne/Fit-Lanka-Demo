package com.neoviz.fit_lanka_demo.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toolbar;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.GravityCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;
import com.google.android.material.navigation.NavigationView;
import com.neoviz.fit_lanka_demo.R;
import com.neoviz.fit_lanka_demo.databinding.ActivityHomeBinding;
import com.neoviz.fit_lanka_demo.databinding.ActivityLoginBinding;
import com.neoviz.fit_lanka_demo.fragment.HomeFragment;
import com.neoviz.fit_lanka_demo.fragment.LeaderboardFragment;
import com.neoviz.fit_lanka_demo.fragment.ShopFragment;
import com.neoviz.fit_lanka_demo.fragment.WorkoutFragment;

public class HomeActivity extends AppCompatActivity implements
        NavigationView.OnNavigationItemSelectedListener,
        NavigationBarView.OnItemSelectedListener {

    private ActivityHomeBinding binding;
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private BottomNavigationView bottomNavigationView;
    private MaterialToolbar toolbar;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        binding = ActivityHomeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        loadFragment(new HomeFragment());

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

        } else if (itemId==R.id.side_nav_wishlist) {
            navigationView.setCheckedItem(R.id.side_nav_wishlist);
            bottomNavigationView.getMenu().findItem(R.id.bottom_nav_shop).setChecked(true);
            ShopFragment fragment = new ShopFragment();
            Bundle bundle = new Bundle();
            bundle.putInt("targetTab", 1);
            fragment.setArguments(bundle);
            loadFragment(fragment);

        } else if (itemId==R.id.side_nav_orders) {
            navigationView.setCheckedItem(R.id.side_nav_orders);
            bottomNavigationView.getMenu().findItem(R.id.bottom_nav_shop).setChecked(true);
            ShopFragment fragment = new ShopFragment();
            Bundle bundle = new Bundle();
            bundle.putInt("targetTab", 3);
            fragment.setArguments(bundle);
            loadFragment(fragment);
        }

        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container_view, fragment)
                .commit();
    }
}