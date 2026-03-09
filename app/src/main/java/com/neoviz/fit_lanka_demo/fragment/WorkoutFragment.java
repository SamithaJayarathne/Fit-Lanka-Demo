package com.neoviz.fit_lanka_demo.fragment;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.CircularBounds;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.libraries.places.api.net.SearchNearbyRequest;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.neoviz.fit_lanka_demo.R;
import com.neoviz.fit_lanka_demo.databinding.FragmentWorkoutBinding;
// මෙතන ඔයා හදපු අලුත් Service එක Import කරගන්න
import com.neoviz.fit_lanka_demo.service.LocationService;
import com.neoviz.fit_lanka_demo.util.Utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class WorkoutFragment extends Fragment implements SensorEventListener, OnMapReadyCallback {

    private FragmentWorkoutBinding binding;
    private SensorManager sensorManager;
    private Sensor stepSensor;

    private FirebaseFirestore firestore;
    private FirebaseAuth firebaseAuth;

    private boolean isWorkoutActive = false;
    private int stepsAtStart = 0;
    private int currentWorkoutSteps = 0;
    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationClient;
    private List<LatLng> pathPoints;
    private Polyline polyline;
    private LocationReceiver locationReceiver;
    private PlacesClient placesClient;

    private class LocationReceiver extends BroadcastReceiver{

        @Override
        public void onReceive(Context context, Intent intent) {
            if("ACTION_LOCATION_UPDATE".equals(intent.getAction())){
                double lat = intent.getDoubleExtra("lat", 0);
                double lng = intent.getDoubleExtra("lng", 0);

                LatLng newPoint = new LatLng(lat, lng);
                pathPoints.add(newPoint);
                updatePolyline();

                if(mMap!=null && isWorkoutActive){
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(newPoint, 18f));
                }
            }
        }
    }

    private final ActivityResultLauncher<String[]> locationPermissionRequest =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                Boolean fineLocationGranted = result.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false);
                Boolean backgroundLocationGranted = result.getOrDefault(Manifest.permission.ACCESS_BACKGROUND_LOCATION, false);

                if (fineLocationGranted != null && fineLocationGranted) {
                    if (mMap != null) {
                        try {
                            mMap.setMyLocationEnabled(true);
                        } catch (SecurityException e) {
                            e.printStackTrace();
                        }
                    }
                } else {
                    Toast.makeText(getContext(), "Location permission is required for workout tracking", Toast.LENGTH_SHORT).show();
                }
            });

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentWorkoutBinding.inflate(inflater, container, false);
        firestore = FirebaseFirestore.getInstance();
        firebaseAuth = FirebaseAuth.getInstance();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());

        binding.workoutMapView.onCreate(savedInstanceState);
        binding.workoutMapView.getMapAsync(this);

        binding.statsContainer.setOnClickListener(view->{
            if(binding.statsContainerDetails.getVisibility()==View.VISIBLE){
                binding.statsContainerDetails.setVisibility(View.GONE);
                binding.statsContainerArrow.setRotation(180f);
            }else{
                binding.statsContainerDetails.setVisibility(View.VISIBLE);
                binding.statsContainerArrow.setRotation(0f);
            }

        });

        if(!Places.isInitialized()){
            Places.initializeWithNewPlacesApiEnabled(requireContext(), "AIzaSyASdoLRQ5c8zouLkyI6a44ib8xTKlPe320");
        }

        placesClient = Places.createClient(requireActivity());

        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        sensorManager = (SensorManager) requireActivity().getSystemService(Context.SENSOR_SERVICE);
        stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);

        binding.btnStartWorkout.setOnClickListener(v -> startWorkout());
        binding.btnStopWorkout.setOnClickListener(v -> stopWorkout());
        binding.btnStopWorkout.setVisibility(View.GONE);
    }

    private void findNearbyPlaces(LatLng location) {
        // 5.0.0 වලට අලුත් Fields සහ Types පාවිච්චි කරමු
        List<Place.Field> placeFields = Arrays.asList(Place.Field.ID, Place.Field.DISPLAY_NAME, Place.Field.LOCATION);

        CircularBounds circularBounds = CircularBounds.newInstance(location, 10000);

        // මචං, මෙතන 'gym' සහ 'park' වෙනුවට අලුත් Types පාවිච්චි කරන්න පුළුවන් (උදා: fitness_center)
        SearchNearbyRequest request = SearchNearbyRequest.builder(circularBounds, placeFields)
                .setIncludedTypes(Arrays.asList("park", "fitness_center"))
                .setMaxResultCount(10)
                .build();

        placesClient.searchNearby(request)
                .addOnSuccessListener(response -> {
                    Log.d("WorkoutFragment", "Places loaded");
                    for (Place place : response.getPlaces()) {
                        if (place.getLocation() != null) {

                            List<String> placeTypes = place.getPlaceTypes();

                            int resourceId = R.drawable.park;

                            if(placeTypes!=null && placeTypes.contains("fitness_center")){
                                resourceId = R.drawable.gym;
                            }

                            mMap.addMarker(new MarkerOptions()
                                    .position(place.getLocation())
                                    .title(place.getDisplayName())
                                    .icon(BitmapDescriptorFactory.fromResource(resourceId)));
                        }
                    }
                })
                .addOnFailureListener(error -> {
                    Log.e("WorkoutFragment", "Error: " + error.getMessage());
                });
    }

    private void startWorkout() {
        if (stepSensor == null) {
            Toast.makeText(getContext(), "Sensor not found!", Toast.LENGTH_SHORT).show();
            return;
        }

        // Permission Check
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestLocationPermissions();
            return;
        }

        isWorkoutActive = true;
        stepsAtStart = 0;
        currentWorkoutSteps = 0;
        pathPoints.clear();
        if (polyline != null) {
            polyline.remove();
            polyline = null;
        }

        binding.btnStartWorkout.setVisibility(View.GONE);
        binding.btnStopWorkout.setVisibility(View.VISIBLE);
        binding.tvWorkoutSteps.setText("0");

        // Start Updates
        fusedLocationClient.requestLocationUpdates(createLocationRequest(), locationCallback, Looper.getMainLooper());

        // Start Foreground Service (LocationService.class යොදාගන්න)
        Intent serviceIntent = new Intent(requireContext(), LocationService.class);
        ContextCompat.startForegroundService(requireContext(), serviceIntent);

        Toast.makeText(getContext(), "Workout Started!", Toast.LENGTH_LONG).show();
    }

    private void requestLocationPermissions() {
        List<String> permissions = new ArrayList<>();
        permissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
        permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION);

        // Android 10+ නම් Background Location Permission එකත් List එකට දානවා
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            permissions.add(Manifest.permission.ACCESS_BACKGROUND_LOCATION);
        }

        // Android 13+ නම් Notification Permission එකත් ඕනේ Service එකට
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS);
        }

        locationPermissionRequest.launch(permissions.toArray(new String[0]));
    }

    private void stopWorkout() {
        isWorkoutActive = false;
        binding.btnStopWorkout.setEnabled(false);

        fusedLocationClient.removeLocationUpdates(locationCallback);

        Intent serviceIntent = new Intent(requireContext(), LocationService.class);
        requireContext().stopService(serviceIntent);

        updateWorkoutToFirestore(currentWorkoutSteps);
    }

    private void updateWorkoutToFirestore(int totalSteps) {
        if (firebaseAuth.getCurrentUser() == null || totalSteps <= 0) {
            resetUI();
            return;
        }

        double earnedCoins = totalSteps * Utils.COINS_PER_STEP_ACTIVE;
        if (binding != null)
            binding.tvEarnedCoins.setText(String.format(Locale.getDefault(), "%.2f", earnedCoins));
        String uid = firebaseAuth.getCurrentUser().getUid();

        Map<String, Object> updates = new HashMap<>();
        updates.put("currentDaySteps", FieldValue.increment(totalSteps));
        updates.put("allTimeSteps", FieldValue.increment(totalSteps));
        updates.put("fitCoins", FieldValue.increment(earnedCoins));

        firestore.collection("users").document(uid)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "Workout Saved!", Toast.LENGTH_SHORT).show();
                    showWorkoutSummary(totalSteps, earnedCoins);
                })
                .addOnFailureListener(e -> {
                    if (binding != null) binding.btnStopWorkout.setEnabled(true);
                });
    }

    private void resetUI() {
        if (binding != null) {
            binding.btnStartWorkout.setVisibility(View.VISIBLE);
            binding.btnStopWorkout.setVisibility(View.GONE);
            binding.btnStopWorkout.setEnabled(true);
            binding.tvWorkoutSteps.setText("0");
            binding.tvEarnedCoins.setText("0.00");
        }
        currentWorkoutSteps = 0;
    }

    private LocationRequest createLocationRequest() {
        return new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000)
                .setMinUpdateDistanceMeters(2)
                .build();
    }

    private final LocationCallback locationCallback = new LocationCallback() {
        @Override
        public void onLocationResult(@NonNull LocationResult locationResult) {
            if (!isWorkoutActive) return;
            for (Location location : locationResult.getLocations()) {
                LatLng newPoint = new LatLng(location.getLatitude(), location.getLongitude());
                pathPoints.add(newPoint);
                updatePolyline();
                if (mMap != null)
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(newPoint, 18f));
            }
        }
    };

    private void updatePolyline() {
        if (mMap == null || pathPoints.isEmpty()) return;
        if (polyline == null) {
            polyline = mMap.addPolyline(new PolylineOptions().addAll(pathPoints).color(Color.BLUE).width(12f));
        } else {
            polyline.setPoints(pathPoints);
        }
    }

    private void showWorkoutSummary(int steps, double coins) {
        double distance = (steps * Utils.METER_PER_STEP) / 1000.0;
        double calories = steps * Utils.CALORIES_PER_STEP;

        new MaterialAlertDialogBuilder(requireActivity())
                .setTitle("🏃 Workout Summary")
                .setMessage(String.format(Locale.getDefault(),
                        "Great job, Champion!\n\n👣 Steps: %d\n📍 Distance: %.2f km\n🔥 Calories: %.1f kcal\n💰 Coins: %.2f",
                        steps, distance, calories, coins))
                .setPositiveButton("Done", (d, w) -> resetUI())
                .setCancelable(false)
                .show();
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        pathPoints = new ArrayList<>();
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);
            fusedLocationClient.getLastLocation().addOnSuccessListener(loc -> {
                if (loc != null)
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(loc.getLatitude(), loc.getLongitude()), 15f));
                LatLng userLocation = new LatLng(loc.getLatitude(), loc.getLongitude());
                findNearbyPlaces(userLocation);
            });
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (isWorkoutActive && event.sensor.getType() == Sensor.TYPE_STEP_COUNTER) {
            int totalSteps = (int) event.values[0];
            if (stepsAtStart == 0) stepsAtStart = totalSteps;
            currentWorkoutSteps = totalSteps - stepsAtStart;
            if (binding != null)
                binding.tvWorkoutSteps.setText(String.valueOf(currentWorkoutSteps));
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    @Override
    public void onResume() {
        super.onResume();
        if (binding != null) binding.workoutMapView.onResume();
        if (stepSensor != null)
            sensorManager.registerListener(this, stepSensor, SensorManager.SENSOR_DELAY_UI);

        locationReceiver = new LocationReceiver();
        requireActivity()
                .registerReceiver(locationReceiver,
                        new IntentFilter("ACTION_LOCATION_UPDATE"),
                        Context.RECEIVER_NOT_EXPORTED
                );
    }

    @Override
    public void onPause() {
        if (binding != null) binding.workoutMapView.onPause();
        super.onPause();

        if(locationReceiver!=null){
            requireActivity().unregisterReceiver(locationReceiver);
        }
    }

    @Override
    public void onDestroyView() {
        if (binding != null) binding.workoutMapView.onDestroy();
        if (sensorManager != null) sensorManager.unregisterListener(this);
        super.onDestroyView();
        binding = null;
    }
}