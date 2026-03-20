package com.neoviz.fit_lanka_demo.fragment;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
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
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.JointType;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.maps.model.RoundCap;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.CircularBounds;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.libraries.places.api.net.SearchNearbyRequest;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.neoviz.fit_lanka_demo.R;
import com.neoviz.fit_lanka_demo.api.DirectionsApiService;
import com.neoviz.fit_lanka_demo.databinding.FragmentWorkoutBinding;
import com.neoviz.fit_lanka_demo.service.LocationService;
import com.neoviz.fit_lanka_demo.util.AppUtils;
import com.neoviz.fit_lanka_demo.util.StepUtils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

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
    private Marker startMarker;
    private Marker runnerMarker;
    private Circle accuracyCircle;

    private Polyline directionPolyline;
    private LatLng selectedDestination;
    private BottomSheetBehavior<View> bottomSheetBehavior;

    private class LocationReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ("ACTION_LOCATION_UPDATE".equals(intent.getAction())) {
                double lat = intent.getDoubleExtra("lat", 0);
                double lng = intent.getDoubleExtra("lng", 0);

                LatLng newPoint = new LatLng(lat, lng);
                pathPoints.add(newPoint);
                updatePolyline();

                if (mMap != null && isWorkoutActive) {
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(newPoint, 20f));
                }
            }
        }
    }

    private final ActivityResultLauncher<String[]> locationPermissionRequest =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                Boolean fineLocationGranted = result.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false);
                if (fineLocationGranted != null && fineLocationGranted) {
                    if (mMap != null) {
                        try { mMap.setMyLocationEnabled(true); } catch (SecurityException e) { e.printStackTrace(); }
                    }
                } else {
                    Toast.makeText(getContext(), "Location permission is required", Toast.LENGTH_SHORT).show();
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

        binding.statsContainer.setOnClickListener(view -> {
            if (binding.statsContainerDetails.getVisibility() == View.VISIBLE) {
                binding.statsContainerDetails.setVisibility(View.GONE);
                binding.statsContainerArrow.setRotation(180f);
            } else {
                binding.statsContainerDetails.setVisibility(View.VISIBLE);
                binding.statsContainerArrow.setRotation(0f);
            }
        });

        if (!Places.isInitialized()) {
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

        // 1. Bottom Sheet Behavior Initialize කිරීම
        bottomSheetBehavior = BottomSheetBehavior.from(binding.bottomSheetRoute);
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);

        binding.btnStartWorkout.setOnClickListener(v -> startWorkout());
        binding.btnStopWorkout.setOnClickListener(v -> stopWorkout());
        binding.btnStopWorkout.setVisibility(View.GONE);

        // 2. Bottom Sheet එකේ Clear/Cancel Route Button එකේ Logic එක
        binding.btnCloseRoute.setOnClickListener(v -> {
            if (directionPolyline != null) {
                directionPolyline.remove();
                directionPolyline = null;
            }
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
            selectedDestination = null;
            binding.btnGetDirections.setVisibility(View.GONE);
        });

        binding.btnGetDirections.setOnClickListener(v -> {
            if (selectedDestination != null) {
                if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
                        if (location != null) {
                            LatLng userLoc = new LatLng(location.getLatitude(), location.getLongitude());
                            fetchDirections(userLoc, selectedDestination);
                            // Directions fetch කරන අතරතුර බටන් එක හංගනවා
                            binding.btnGetDirections.setVisibility(View.GONE);
                        } else {
                            Toast.makeText(getContext(), "Location not found!", Toast.LENGTH_SHORT).show();
                        }
                    });
                } else {
                    requestLocationPermissions();
                }
            }
        });
    }

    private void findNearbyPlaces(LatLng location) {
        List<Place.Field> placeFields = Arrays.asList(Place.Field.ID, Place.Field.DISPLAY_NAME, Place.Field.LOCATION, Place.Field.TYPES);
        CircularBounds circularBounds = CircularBounds.newInstance(location, 5000);
        SearchNearbyRequest request = SearchNearbyRequest.builder(circularBounds, placeFields)
                .setIncludedTypes(Arrays.asList("park", "gym", "stadium", "sports_complex"))
                .setMaxResultCount(15)
                .build();

        placesClient.searchNearby(request).addOnSuccessListener(response -> {
            for (Place place : response.getPlaces()) {
                if (place.getLocation() != null) {
                    List<String> types = place.getPlaceTypes();
                    int iconRes = -1;
                    if (types != null) {
                        if (types.contains("gym")) iconRes = R.drawable.fitness_center_24px;
                        else if (types.contains("stadium") || types.contains("sports_complex")) iconRes = R.drawable.stadium_24px;
                        else if (types.contains("park")) iconRes = R.drawable.nature_24px;
                    }

                    if (iconRes != -1) {
                        mMap.addMarker(new MarkerOptions()
                                .position(place.getLocation())
                                .title(place.getDisplayName())
                                .anchor(0.5f, 1.0f)
                                .icon(getMarkerWithPinPoint(requireContext(), iconRes)));
                    }
                }
            }
        });
    }

    private BitmapDescriptor getMarkerWithPinPoint(Context context, int resId) {
        int circleRadius = 50; int pinHeight = 25;
        int width = circleRadius * 2; int height = (circleRadius * 2) + pinHeight;
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        android.graphics.Paint paint = new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);

        android.graphics.Path path = new android.graphics.Path();
        path.moveTo(width / 2f, height);
        path.lineTo((width / 2f) - 15, (circleRadius * 2f) - 5);
        path.lineTo((width / 2f) + 15, (circleRadius * 2f) - 5);
        path.close();
        paint.setColor(Color.WHITE);
        canvas.drawPath(path, paint);
        canvas.drawCircle(width / 2f, circleRadius, circleRadius, paint);

        paint.setStyle(android.graphics.Paint.Style.STROKE);
        paint.setColor(Color.parseColor("#39FF14"));
        paint.setStrokeWidth(4f);
        canvas.drawCircle(width / 2f, circleRadius, circleRadius - 2, paint);

        Drawable drawable = ContextCompat.getDrawable(context, resId);
        if (drawable != null) {
            int padding = 25;
            drawable.setBounds(padding, padding, width - padding, (circleRadius * 2) - padding);
            drawable.setTint(Color.parseColor("#333333"));
            drawable.draw(canvas);
        }
        return BitmapDescriptorFactory.fromBitmap(bitmap);
    }

    private void startWorkout() {
        if (stepSensor == null) {
            Toast.makeText(getContext(), "Sensor not found!", Toast.LENGTH_SHORT).show();
            return;
        }
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

        if (runnerMarker != null) {
            runnerMarker.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.run));
        }

        binding.btnStartWorkout.setVisibility(View.GONE);
        binding.btnStopWorkout.setVisibility(View.VISIBLE);

        // Workout එක පටන් ගද්දී Bottom Sheet එක expand වෙලා තිබ්බොත් ඒක පොඩ්ඩක් පහත් කරනවා
        if(bottomSheetBehavior.getState() == BottomSheetBehavior.STATE_EXPANDED){
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
        }

        binding.tvWorkoutSteps.setText("0");

        fusedLocationClient.requestLocationUpdates(createLocationRequest(), locationCallback, Looper.getMainLooper());
        Intent serviceIntent = new Intent(requireContext(), LocationService.class);
        ContextCompat.startForegroundService(requireContext(), serviceIntent);

        if (mMap != null) {
            fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
                if (location != null) {
                    LatLng userLocation = new LatLng(location.getLatitude(), location.getLongitude());
                    CameraPosition cameraPosition = new CameraPosition.Builder()
                            .target(userLocation)
                            .zoom(20f)
                            .tilt(60f)
                            .bearing(location.getBearing())
                            .build();
                    mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
                }
            });
        }
        Toast.makeText(getContext(), "Workout Started!", Toast.LENGTH_LONG).show();
    }

    private void requestLocationPermissions() {
        List<String> permissions = new ArrayList<>();
        permissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
        permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) permissions.add(Manifest.permission.ACCESS_BACKGROUND_LOCATION);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) permissions.add(Manifest.permission.POST_NOTIFICATIONS);
        locationPermissionRequest.launch(permissions.toArray(new String[0]));
    }

    private void stopWorkout() {
        isWorkoutActive = false;
        binding.btnStopWorkout.setEnabled(false);

        fusedLocationClient.removeLocationUpdates(locationCallback);
        requireContext().stopService(new Intent(requireContext(), LocationService.class));

        updateWorkoutToFirestore(currentWorkoutSteps);

        // 3. Workout එක ඉවර කරද්දී පාර සහ Bottom Sheet එක සම්පූර්ණයෙන්ම අයින් කරනවා
        if (directionPolyline != null) {
            directionPolyline.remove();
            directionPolyline = null;
        }
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);

        if (mMap != null) {
            mMap.animateCamera(CameraUpdateFactory.newCameraPosition(
                    new CameraPosition.Builder()
                            .target(mMap.getCameraPosition().target)
                            .zoom(16f)
                            .tilt(45f)
                            .build()
            ));
        }

        if (runnerMarker != null) {
            runnerMarker.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.stand));
            runnerMarker.setRotation(0);
        }

        if (startMarker != null) { startMarker.remove(); startMarker = null; }
        if (accuracyCircle != null) { accuracyCircle.remove(); accuracyCircle = null; }
    }

    private void updateWorkoutToFirestore(int totalSteps) {
        if (firebaseAuth.getCurrentUser() == null || totalSteps <= 0) { resetUI(); return; }
        double earnedCoins = totalSteps * AppUtils.COINS_PER_STEP_ACTIVE;
        if (binding != null) binding.tvEarnedCoins.setText(String.format(Locale.getDefault(), "%.2f", earnedCoins));
        String uid = firebaseAuth.getCurrentUser().getUid();
        StepUtils.updateLeaderboardSteps(uid, totalSteps);

        Map<String, Object> updates = new HashMap<>();
        updates.put("currentDaySteps", FieldValue.increment(totalSteps));
        updates.put("allTimeSteps", FieldValue.increment(totalSteps));
        updates.put("fitCoins", FieldValue.increment(earnedCoins));

        firestore.collection("users").document(uid).update(updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "Workout Saved!", Toast.LENGTH_SHORT).show();
                    showWorkoutSummary(totalSteps, earnedCoins);
                })
                .addOnFailureListener(e -> { if (binding != null) binding.btnStopWorkout.setEnabled(true); });
    }

    private void resetUI() {
        if (binding != null) {
            binding.btnStartWorkout.setVisibility(View.VISIBLE);
            binding.btnStopWorkout.setVisibility(View.GONE);
            binding.btnStopWorkout.setEnabled(true);
            binding.tvWorkoutSteps.setText("0");
            binding.tvEarnedCoins.setText("0.00");
            binding.tvWorkoutSpeed.setText("0.0");
        }
        currentWorkoutSteps = 0;
    }

    private LocationRequest createLocationRequest() {
        return new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000).setMinUpdateDistanceMeters(2).build();
    }

    private final LocationCallback locationCallback = new LocationCallback() {
        @Override
        public void onLocationResult(@NonNull LocationResult locationResult) {
            if (!isWorkoutActive) return;
            for (Location location : locationResult.getLocations()) {
                LatLng newPoint = new LatLng(location.getLatitude(), location.getLongitude());
                pathPoints.add(newPoint);
                updatePolyline();

                float speedInKmh = (float) (location.getSpeed() * 3.6);
                if (speedInKmh < 0.5) speedInKmh = 0.0f;
                if(binding !=null) binding.tvWorkoutSpeed.setText(String.format(Locale.getDefault(), "%.1f", speedInKmh));

                if (mMap != null) {
                    if(startMarker==null && pathPoints.size() > 0){
                        startMarker = mMap.addMarker(new MarkerOptions().position(pathPoints.get(0)).title("Start").icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));
                    }
                    if(runnerMarker==null){
                        runnerMarker = mMap.addMarker(new MarkerOptions().position(newPoint).anchor(0.5f, 0.5f).flat(true).icon(BitmapDescriptorFactory.fromResource(R.drawable.run)));
                    }else{
                        runnerMarker.setPosition(newPoint);
                        runnerMarker.setRotation(location.getBearing());
                    }
                    if(accuracyCircle==null){
                        accuracyCircle = mMap.addCircle(new CircleOptions().center(newPoint).radius(location.getAccuracy()).fillColor(Color.parseColor("#2239FF14")).strokeColor(Color.parseColor("#39FF14")).strokeWidth(3f).zIndex(99f));
                    }else{
                        accuracyCircle.setCenter(newPoint);
                        accuracyCircle.setRadius(location.getAccuracy());
                    }
                    CameraPosition cp = new CameraPosition.Builder().target(newPoint).zoom(20f).tilt(60f).bearing(location.getBearing()).build();
                    mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cp));
                }
            }
        }
    };

    private void updatePolyline() {
        if (mMap == null || pathPoints.isEmpty()) return;
        if (polyline == null) {
            polyline = mMap.addPolyline(new PolylineOptions()
                    .addAll(pathPoints)
                    .color(Color.parseColor("#39FF14"))
                    .width(18f)
                    .zIndex(105f)
                    .startCap(new RoundCap())
                    .endCap(new RoundCap())
                    .jointType(JointType.ROUND));
        } else {
            polyline.setPoints(pathPoints);
        }
    }

    private void showWorkoutSummary(int steps, double coins) {
        double distance = (steps * AppUtils.METER_PER_STEP) / 1000.0;
        double calories = steps * AppUtils.CALORIES_PER_STEP;
        new MaterialAlertDialogBuilder(requireActivity()).setTitle("🏃 Workout Summary").setMessage(String.format(Locale.getDefault(), "Steps: %d\nDistance: %.2f km\nCalories: %.1f kcal\nCoins: %.2f", steps, distance, calories, coins)).setPositiveButton("Done", (d, w) -> resetUI()).setCancelable(false).show();
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        pathPoints = new ArrayList<>();
        try { mMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(requireContext(), R.raw.map_style)); } catch (Exception e) { Log.e("MapStyle", "Error: ", e); }
        mMap.getUiSettings().setTiltGesturesEnabled(true);
        mMap.setBuildingsEnabled(true);

        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);
            fusedLocationClient.getLastLocation().addOnSuccessListener(loc -> {
                if (loc != null) {
                    LatLng userLocation = new LatLng(loc.getLatitude(), loc.getLongitude());
                    CameraPosition cameraPosition = new CameraPosition.Builder()
                            .target(userLocation).zoom(16f).tilt(45f).build();
                    mMap.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));

                    if (runnerMarker == null) {
                        runnerMarker = mMap.addMarker(new MarkerOptions()
                                .position(userLocation)
                                .title("You are here")
                                .anchor(0.5f, 0.5f)
                                .flat(true)
                                .icon(BitmapDescriptorFactory.fromResource(R.drawable.stand)));
                    } else {
                        runnerMarker.setPosition(userLocation);
                    }
                    findNearbyPlaces(userLocation);
                }
            });
        }

        mMap.setOnMarkerClickListener(marker -> {
            // Workout එක active නම් marker clicks ignore කරනවා (optional)
            if (isWorkoutActive) return true;

            selectedDestination = marker.getPosition();

            // 4. Marker එක click කරපු ගමන් Bottom Sheet එකේ Title එක update කරනවා
            binding.tvDestinationName.setText(marker.getTitle());
            binding.btnGetDirections.setVisibility(View.VISIBLE);
            binding.btnGetDirections.setText("Go to " + marker.getTitle());

            return false;
        });

        mMap.setOnMapClickListener(latLng -> {
            if (!isWorkoutActive) {
                binding.btnGetDirections.setVisibility(View.GONE);
                selectedDestination = null;
                // මැප් එකේ හිස් තැනක් ක්ලික් කළොත් පාර සහ Bottom Sheet එක හංගනවා
                if (directionPolyline != null) directionPolyline.remove();
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
            }
        });
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (isWorkoutActive && event.sensor.getType() == Sensor.TYPE_STEP_COUNTER) {
            int totalSteps = (int) event.values[0];
            if (stepsAtStart == 0) stepsAtStart = totalSteps;
            currentWorkoutSteps = totalSteps - stepsAtStart;
            if (binding != null) binding.tvWorkoutSteps.setText(String.valueOf(currentWorkoutSteps));
        }
    }

    @Override public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    @Override
    public void onResume() {
        super.onResume();
        if (binding != null) binding.workoutMapView.onResume();
        if (stepSensor != null) sensorManager.registerListener(this, stepSensor, SensorManager.SENSOR_DELAY_UI);
        locationReceiver = new LocationReceiver();
        requireActivity().registerReceiver(locationReceiver, new IntentFilter("ACTION_LOCATION_UPDATE"), Context.RECEIVER_NOT_EXPORTED);
    }

    @Override
    public void onPause() {
        if (binding != null) binding.workoutMapView.onPause();
        super.onPause();
        if (locationReceiver != null) requireActivity().unregisterReceiver(locationReceiver);
    }

    @Override
    public void onDestroyView() {
        if (binding != null) binding.workoutMapView.onDestroy();
        if (sensorManager != null) sensorManager.unregisterListener(this);
        super.onDestroyView();
        binding = null;
    }

    private List<LatLng> decodePoly(String encoded) {
        List<LatLng> poly = new ArrayList<>();
        int index = 0, len = encoded.length();
        int lat = 0, lng = 0;
        while (index < len) {
            int b, shift = 0, result = 0;
            do { b = encoded.charAt(index++) - 63; result |= (b & 0x1f) << shift; shift += 5; } while (b >= 0x20);
            int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1)); lat += dlat;
            shift = 0; result = 0;
            do { b = encoded.charAt(index++) - 63; result |= (b & 0x1f) << shift; shift += 5; } while (b >= 0x20);
            int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1)); lng += dlng;
            poly.add(new LatLng((double) lat / 1E5, (double) lng / 1E5));
        }
        return poly;
    }

    private void fetchDirections(LatLng origin, LatLng dest) {
        String originStr = origin.latitude + "," + origin.longitude;
        String destStr = dest.latitude + "," + dest.longitude;

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://maps.googleapis.com/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        DirectionsApiService apiService = retrofit.create(DirectionsApiService.class);

        apiService.getDirections(originStr, destStr, "walking", "AIzaSyASdoLRQ5c8zouLkyI6a44ib8xTKlPe320")
                .enqueue(new Callback<ResponseBody>() {
                    @Override
                    public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                        try {
                            if (response.body() != null) {
                                JSONObject jsonObject = new JSONObject(response.body().string());
                                JSONArray routes = jsonObject.getJSONArray("routes");

                                if (routes.length() > 0) {
                                    JSONObject leg = routes.getJSONObject(0).getJSONArray("legs").getJSONObject(0);
                                    String distText = leg.getJSONObject("distance").getString("text");
                                    String timeText = leg.getJSONObject("duration").getString("text");

                                    // 5. API දත්ත Bottom Sheet එකට දමා එය පෙන්වීම (EXPAND)
                                    binding.tvRouteDistance.setText(distText);
                                    binding.tvRouteDuration.setText(timeText);
                                    bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);

                                    String encodedPoints = routes.getJSONObject(0).getJSONObject("overview_polyline").getString("points");
                                    drawDirectionOnMap(decodePoly(encodedPoints));
                                    flyToDestination(selectedDestination);
                                }
                            }
                        } catch (Exception e) { e.printStackTrace(); }
                    }
                    @Override public void onFailure(Call<ResponseBody> call, Throwable t) { Toast.makeText(getContext(), "Direction Error!", Toast.LENGTH_SHORT).show(); }
                });
    }

    private void drawDirectionOnMap(List<LatLng> points) {
        if (directionPolyline != null) directionPolyline.remove();

        directionPolyline = mMap.addPolyline(new PolylineOptions()
                .addAll(points)
                .width(18f)
                .color(Color.parseColor("#00E5FF"))
                .geodesic(true)
                .zIndex(101f)
                .startCap(new RoundCap())
                .endCap(new RoundCap())
                .jointType(JointType.ROUND));

        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        for (LatLng point : points) { builder.include(point); }
        int padding = 200;
        mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), padding));
    }

    private void flyToDestination(LatLng destination) {
        if (mMap == null) return;
        CameraPosition overviewPos = new CameraPosition.Builder()
                .target(destination).zoom(14f).tilt(45f).build();

        mMap.animateCamera(CameraUpdateFactory.newCameraPosition(overviewPos), 2000, new GoogleMap.CancelableCallback() {
            @Override
            public void onFinish() {
                CameraPosition closeUpPos = new CameraPosition.Builder()
                        .target(destination).zoom(18f).tilt(65f).bearing(90).build();
                mMap.animateCamera(CameraUpdateFactory.newCameraPosition(closeUpPos), 2000, null);
            }
            @Override public void onCancel() {}
        });
    }
}