package com.example.idtcodingchallenge;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.mongodb.stitch.android.core.Stitch;
import com.mongodb.stitch.android.core.StitchAppClient;
import com.mongodb.stitch.android.core.auth.StitchUser;
import com.mongodb.stitch.android.services.mongodb.remote.RemoteMongoClient;
import com.mongodb.stitch.android.services.mongodb.remote.RemoteMongoCollection;
import com.mongodb.stitch.core.auth.providers.anonymous.AnonymousCredential;

import org.bson.Document;

import java.util.Calendar;

public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;

    RemoteMongoCollection _remoteCollection;
    String user_id;

    Button saveLocation;
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        final StitchAppClient client = Stitch.initializeAppClient(getResources().getString(R.string.stitch_app_id));

        client.getAuth().loginWithCredential(new AnonymousCredential())
                .addOnCompleteListener(new OnCompleteListener<StitchUser>() {
                    @Override
                    public void onComplete(@NonNull Task<StitchUser> task) {
                        final RemoteMongoClient remoteMongoClient = client.getServiceClient(RemoteMongoClient.factory, "mongodb-atlas_Sandbox1");
                        _remoteCollection = remoteMongoClient.getDatabase("IDTCodingChallenge").getCollection("Locations");
                        user_id = task.getResult().getId();
                    }
                });

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        saveLocation = findViewById(R.id.btn_SaveLocation);
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);

        this.setTitle("Current Location");

        if ( ContextCompat.checkSelfPermission( this, Manifest.permission.ACCESS_FINE_LOCATION ) != PackageManager.PERMISSION_GRANTED ) {
            ActivityCompat.requestPermissions(this, new String[] { Manifest.permission.ACCESS_FINE_LOCATION}, 101);
        } else {
            mapFragment.getMapAsync(this);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults){

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        saveLocation.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                saveLocation();
            }
        });
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        Location currLoc = getCurrentLocation();

        if(currLoc != null){
            LatLng position =  new LatLng(currLoc.getLatitude(), currLoc.getLongitude());
            mMap.addMarker(new MarkerOptions().position(position)).setTitle("Your Current Location");
            mMap.moveCamera(CameraUpdateFactory.newLatLng(position));
        }
        else{
            Toast.makeText(this, "Unable to get current location", Toast.LENGTH_SHORT).show();
        }
    }

    public Location getCurrentLocation(){
        LocationManager lm = (LocationManager)this.getSystemService(Context.LOCATION_SERVICE);

        boolean isGPS = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
        boolean isNetwork = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

        Location network_loc = null, gps_loc = null, final_loc = null;
        try {
            if (isGPS) gps_loc = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (isNetwork) network_loc = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);

            if(gps_loc != null && network_loc != null) final_loc = gps_loc.getAccuracy() > network_loc.getAccuracy() ? network_loc : gps_loc;
            else{
                if(gps_loc == null) final_loc = network_loc;
                else if(network_loc == null) final_loc = gps_loc;
            }

        } catch (SecurityException e){
            Toast.makeText(this, "There was an error with location services", Toast.LENGTH_SHORT).show();
        }
        return final_loc;
    }

    public void saveLocation(){
        Location currLoc = getCurrentLocation();
        Document locationDoc = new Document();
        locationDoc.append("Latitude", currLoc.getLatitude());
        locationDoc.append("Longitude", currLoc.getLongitude());
        locationDoc.append("DateTime Logged", Calendar.getInstance().getTime());
        locationDoc.append("user_id", user_id);

        _remoteCollection.insertOne(locationDoc).addOnSuccessListener(new OnSuccessListener() {
            @Override
            public void onSuccess(Object o) {
                Log.d("STITCH", "One Document Inserted");
            }
        });
    }
}
