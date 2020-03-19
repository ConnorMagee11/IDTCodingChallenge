package com.example.idtcodingchallenge;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
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
import com.google.android.gms.tasks.Task;
import com.mongodb.stitch.android.core.Stitch;
import com.mongodb.stitch.android.core.StitchAppClient;
import com.mongodb.stitch.android.core.auth.StitchUser;
import com.mongodb.stitch.android.services.mongodb.remote.RemoteMongoClient;
import com.mongodb.stitch.android.services.mongodb.remote.RemoteMongoCollection;
import com.mongodb.stitch.core.auth.providers.anonymous.AnonymousCredential;

public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;

    RemoteMongoCollection _remoteCollection;
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        final StitchAppClient client = Stitch.initializeAppClient(getResources().getString(R.string.stitch_app_id));

        client.getAuth().loginWithCredential(new AnonymousCredential())
                .addOnCompleteListener(new OnCompleteListener<StitchUser>() {
                    @Override
                    public void onComplete(@NonNull Task<StitchUser> task) {
                        final RemoteMongoClient remoteMongoClient = client.getServiceClient(RemoteMongoClient.factory, "mongodb-atlas");
                        _remoteCollection = remoteMongoClient.getDatabase("IDTCodingChallenge").getCollection("Locations");
                    }
                });

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        this.setTitle("Current Location");

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.


        if ( ContextCompat.checkSelfPermission( this, Manifest.permission.ACCESS_FINE_LOCATION ) != PackageManager.PERMISSION_GRANTED ) {

            ActivityCompat.requestPermissions(this, new String[] { Manifest.permission.ACCESS_FINE_LOCATION}, 101);
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults){

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

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

        if(final_loc != null){
            LatLng position =  new LatLng(final_loc.getLatitude(), final_loc.getLongitude());
            mMap.addMarker(new MarkerOptions().position(position)).setTitle("Your Current Location");
            mMap.moveCamera(CameraUpdateFactory.newLatLng(position));
        }
        else{
            Toast.makeText(this, "Unable to get current location", Toast.LENGTH_SHORT).show();
        }
    }
}
