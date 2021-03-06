package com.example.locationtrial;

import static androidx.core.content.ContextCompat.getSystemService;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.app.FragmentTransaction;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Random;

public class MainActivity extends FragmentActivity implements OnMapReadyCallback {

    Location currentLocation;
    FusedLocationProviderClient fusedLocationProviderClient;
    private static final int REQUEST_CODE = 101;
    ArrayList<Places> locations = new ArrayList<>();
    FloatingActionButton addLocButton, confirmLoc, deleteLoc;
    EditText locNameText;
    MarkerOptions selectedLoc;
    Places nearestLocation;

    Button save;
    Button load;
    Button openFragment;

    Handler handler = new Handler();
    Runnable runnable;
    int delay = 4000; //10 seconds between method is called again

    @Override
    protected void onResume() {
        handler.postDelayed(runnable = new Runnable() {
            public void run() {
                handler.postDelayed(runnable, delay);
                getClosestLocation();
            }
        }, delay);
        super.onResume();
    }
    @Override
    protected void onPause() {
        super.onPause();
        //handler.removeCallbacks(runnable); //stop handler when activity not visible super.onPause();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        save = findViewById(R.id.saveBtn);
        load = findViewById(R.id.loadBtn);

        addLocButton = findViewById(R.id.addLocBtn);
        locNameText = findViewById(R.id.locNameTextField);
        confirmLoc = findViewById(R.id.confirmLoc);
        deleteLoc = findViewById(R.id.deleteLoc);
        openFragment = findViewById(R.id.tochecklist);

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        fetchLastLocation();
    }

    private void fetchLastLocation() {
        System.out.println("getting here");
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            System.out.println("here pt two");
            ActivityCompat.requestPermissions(this, new String[]
                    {Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_CODE);
            return;
        }
        System.out.println("here pt 3");
        Task<Location> task = fusedLocationProviderClient.getLastLocation();
        task.addOnSuccessListener(new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {
                System.out.println("here pt 4");

                if (location != null){
                    System.out.println("here pt 5");
                    currentLocation = location;
                    System.out.println("lat: " + currentLocation.getLatitude() + ", " + " long: " + currentLocation.getLongitude());
                    Toast.makeText(getApplicationContext(), currentLocation.getLatitude() + ""+ currentLocation.getLongitude(),Toast.LENGTH_SHORT).show();
                    SupportMapFragment supportMapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.google_map);
                    supportMapFragment.getMapAsync(MainActivity.this);
                }
            }
        });
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        System.out.println("getting here");
        LatLng myLoc;
        myLoc = new LatLng(currentLocation.getLatitude(),currentLocation.getLongitude());
        MarkerOptions markerOptions = new MarkerOptions().position(myLoc)
                .title("Current Location");

        googleMap.addMarker(markerOptions);

        // Construct a CameraPosition focusing on Mountain View and animate the camera to that position.
        CameraPosition cameraPosition = new CameraPosition.Builder()
                .target(myLoc)             // Sets the center of the map to school
                .zoom(15)                   // Sets the zoom
                .tilt(35)                   // Sets the tilt of the camera to 20 degrees
                .build();                   // Creates a CameraPosition from the builder
        googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
        googleMap.getUiSettings().setZoomControlsEnabled(true);

        googleMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng point) {
                googleMap.clear();
                addCurrentLoc(googleMap);
                addSavedLocations(googleMap);
                selectedLoc = new MarkerOptions().position(point);
                googleMap.addMarker(selectedLoc);
                addLocButton.setVisibility(View.VISIBLE);
                confirmLoc.setVisibility(View.INVISIBLE);
                deleteLoc.setVisibility(View.VISIBLE);
                locNameText.setVisibility(View.INVISIBLE);
            }
        });

        addLocButton.setOnClickListener(view -> {
            locNameText.setVisibility(View.VISIBLE);
            confirmLoc.setVisibility(View.VISIBLE);
            addLocButton.setVisibility(View.INVISIBLE);
            locNameText.setText(R.string.Location_Name);
        });

        confirmLoc.setOnClickListener(view -> {
            LatLng newPos = selectedLoc.getPosition();
            String newName = locNameText.getText().toString();
            locations.add(new Places(newPos, newName));
            sendNotification("New Place Added", newName);
           // createNotification("New Place Added", "hi");
            confirmLoc.setVisibility(View.INVISIBLE);
            deleteLoc.setVisibility(View.INVISIBLE);
            locNameText.setVisibility(View.INVISIBLE);
            googleMap.clear();
            addCurrentLoc(googleMap);
            addSavedLocations(googleMap);
        });

        deleteLoc.setOnClickListener(view -> {
            googleMap.clear();
            addCurrentLoc(googleMap);
            addSavedLocations(googleMap);
            locNameText.setVisibility(View.INVISIBLE);
            confirmLoc.setVisibility(View.INVISIBLE);
            addLocButton.setVisibility(View.INVISIBLE);
            deleteLoc.setVisibility(View.INVISIBLE);
        });

        openFragment.setOnClickListener(view ->{
            Bundle bundle = new Bundle();
            bundle.putParcelableArrayList("places", locations);

            addLocButton.setVisibility(View.GONE);
            deleteLoc.setVisibility(View.GONE);
            confirmLoc.setVisibility(View.GONE);
            openFragment.setVisibility(View.GONE);
            save.setVisibility(View.GONE);
            load.setVisibility(View.GONE);
            Fragment fragment = new ListTasks();
            fragment.setArguments(bundle);
            getSupportFragmentManager().beginTransaction().replace(R.id.main_activity, fragment).commit();
        });

        save.setOnClickListener(view ->{
            save();
        });

        load.setOnClickListener(view -> {
            load(view, googleMap);
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case REQUEST_CODE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    fetchLastLocation();
                }
                break;
        }
    }

    private void addCurrentLoc(GoogleMap map){
        map.clear();
        LatLng myLoc;
        myLoc = new LatLng(currentLocation.getLatitude(),currentLocation.getLongitude());
        MarkerOptions markerOptions = new MarkerOptions().position(myLoc)
                .title("Current Location");

        map.addMarker(markerOptions);
    }

    private void addSavedLocations(GoogleMap googleMap) {
        for(Places p: locations){
            MarkerOptions markerOptions = new MarkerOptions().position(p.getCoordinates())
                    .title(p.getPlace_name());

            googleMap.addMarker(markerOptions);
        }
        //getClosestLocation();
    }

    public ArrayList<Places> getPlaces(){
        return locations;
    }

    public void save()
    {
        File file= null;
        FileOutputStream fileOutputStream = null;

        try {
            file = getFilesDir();
            fileOutputStream = openFileOutput("Locations.txt", Context.MODE_PRIVATE); //MODE PRIVATE

            for(Places p:locations){
                String locData = p.getPlace_name() + ": " + p.getCoordinates().latitude + ", " + p.getCoordinates().longitude + " || ";
                fileOutputStream.write(locData.getBytes());
            }
            Toast.makeText(this, "Successfully Saved \n" + "Path --" + file + "\tCode.txt", Toast.LENGTH_SHORT).show();

            return;
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            try {
                fileOutputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void load(View view, GoogleMap googleMap) {
        locations.clear();
        ArrayList<Places> newLocations = new ArrayList<>();
        try {
            FileInputStream fileInputStream =  openFileInput("Locations.txt");
            int read = -1;
            StringBuffer buffer = new StringBuffer();
            while((read =fileInputStream.read())!= -1){
                buffer.append((char)read);
            }

            String newString = buffer.toString();
            System.out.println("new string: " + newString);
            while(newString.length() > 0){
                System.out.println(newString.indexOf(" || "));
                String location = newString.substring(0, newString.indexOf(" || "));
              //  Toast.makeText(this, location, Toast.LENGTH_SHORT).show();
                locations.add(getLocFromString(location));
                newString = newString.substring(newString.indexOf(" || ") + 3);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        Toast.makeText(this,"Loaded", Toast.LENGTH_SHORT).show();
        addSavedLocations(googleMap);
    }

    private Places getLocFromString(String loc){
        String placeName = loc.substring(0, loc.indexOf(": ") );
        String placeLat = loc.substring(loc.indexOf(": ") + 2, loc.indexOf(", "));
        String placeLong = (loc.substring(loc.indexOf(", ") + 2));

        LatLng x = new LatLng(Double.parseDouble(placeLat), Double.parseDouble(placeLong));
        return new Places(x, placeName);
    }

    private void getClosestLocation(){
        if(locations.size() > 0){
            ArrayList<Float> distancesFromCurrent = new ArrayList<>();
            for(Places p: locations){
                float[] results = new float[1];
                Location.distanceBetween(currentLocation.getLatitude(), currentLocation.getLongitude(),
                        p.getCoordinates().latitude, p.getCoordinates().longitude,
                        results);
                distancesFromCurrent.add(results[0]);
            }
            float leastDistance = distancesFromCurrent.get(0);
            for(int i = 0; i < distancesFromCurrent.size(); i++){
                if(distancesFromCurrent.get(i) < leastDistance){
                    leastDistance = distancesFromCurrent.get(i);
                }
            }
            Places newclosestLocation = locations.get(distancesFromCurrent.indexOf(leastDistance));
            if(nearestLocation == null){
                nearestLocation = newclosestLocation;
            }
            if(!newclosestLocation.equals(nearestLocation)){
                nearestLocation = newclosestLocation;
                sendNotification("You're reaching " + nearestLocation.getPlace_name(), "Open your productivity app to see the tasks you have to complete at " + nearestLocation.getPlace_name() + "!");
            }
           // Toast.makeText(this, "Closest: " + newclosestLocation.getPlace_name(), Toast.LENGTH_SHORT).show();
        }

    }


    public void sendNotification(String title, String content) {
        String NOTIFICATION_CHANNEL_ID = "edmt_multiple_location";
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.O){
            NotificationChannel notificationChannel = new NotificationChannel(NOTIFICATION_CHANNEL_ID, "My notifications", NotificationManager.IMPORTANCE_DEFAULT);
            notificationChannel.setDescription("Channel description");
            notificationChannel.enableLights(true);
            notificationChannel.setLightColor(Color.RED);
            notificationChannel.setVibrationPattern(new long[] {0, 1000, 500, 1000});
            notificationChannel.enableVibration(true);
            notificationManager.createNotificationChannel(notificationChannel);
        }
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID);
        builder.setContentTitle(title)
                .setContentText(content)
                .setAutoCancel(false)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher));
        Notification notification = builder.build();
        notificationManager.notify(new Random().nextInt(), notification);
    }

    //would not be used in real life; for the purpose of demo-ing changing location
    public void demoChangingLocation(){
        Toast.makeText(MainActivity.this, "Manually changing current phone location", Toast.LENGTH_SHORT).show();
        //need to think through how I'm changing loc
        for (Places p:locations){
            LatLng currentCoordinates = new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude());
            LatLng pCoordinates = p.getCoordinates();
            if(!pCoordinates.equals(currentCoordinates)){
                Location location = new Location("Test");
                location.setLatitude(p.getCoordinates().latitude);
                location.setLongitude(p.getCoordinates().longitude);
                location.setTime(new Date().getTime()); //Set time as current Date
                currentLocation = location;
                break;
            }
        }
    }

}