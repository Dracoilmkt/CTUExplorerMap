package com.example.ctuexplorermap;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.StrictMode;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Toast;

import org.osmdroid.bonuspack.routing.OSRMRoadManager;
import org.osmdroid.bonuspack.routing.Road;
import org.osmdroid.bonuspack.routing.RoadManager;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polyline;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    private static final int MULTIPLE_PERMISSION_REQUEST_CODE = 4;
    private MapView mapView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        //handle permissions first, before map is created. not depicted here

        //load/initialize the osmdroid configuration, this can be done
        Context ctx = getApplicationContext();
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx));
        //setting this before the layout is inflated is a good idea
        //it 'should' ensure that the map has a writable location for the map cache, even without permissions
        //if no tiles are displayed, you can try overriding the cache path using Configuration.getInstance().setCachePath
        //see also StorageUtils
        //note, the load method also sets the HTTP User Agent to your application's package name, abusing osm's tile servers will get you banned based on this string

        //inflate and create the map
        setContentView(R.layout.activity_main);

        checkPermissionsState();
    }

    private void checkPermissionsState() {
        int internetPermissionCheck = ContextCompat.checkSelfPermission(this,
                Manifest.permission.INTERNET);

        int networkStatePermissionCheck = ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_NETWORK_STATE);

        int writeExternalStoragePermissionCheck = ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE);

        int coarseLocationPermissionCheck = ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION);

        int fineLocationPermissionCheck = ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION);

        int wifiStatePermissionCheck = ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_WIFI_STATE);

        if (internetPermissionCheck == PackageManager.PERMISSION_GRANTED &&
                networkStatePermissionCheck == PackageManager.PERMISSION_GRANTED &&
                writeExternalStoragePermissionCheck == PackageManager.PERMISSION_GRANTED &&
                coarseLocationPermissionCheck == PackageManager.PERMISSION_GRANTED &&
                fineLocationPermissionCheck == PackageManager.PERMISSION_GRANTED &&
                wifiStatePermissionCheck == PackageManager.PERMISSION_GRANTED) {

            setupMap();

        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{
                            Manifest.permission.INTERNET,
                            Manifest.permission.ACCESS_NETWORK_STATE,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE,
                            Manifest.permission.ACCESS_COARSE_LOCATION,
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_WIFI_STATE},
                    MULTIPLE_PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == MULTIPLE_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0) {
                boolean somePermissionWasDenied = false;
                for (int result : grantResults) {
                    if (result == PackageManager.PERMISSION_DENIED) {
                        somePermissionWasDenied = true;
                    }
                }
                if (somePermissionWasDenied) {
                    Toast.makeText(this, "Can't load maps without all the permissions granted", Toast.LENGTH_SHORT).show();
                } else {
                    setupMap();
                }
            } else {
                Toast.makeText(this, "Can't load maps without all the permissions granted", Toast.LENGTH_SHORT).show();
            }
            return;
        }
    }

    private void setupMap() {

        mapView = findViewById(R.id.map);
        mapView.setClickable(true);
        mapView.setMultiTouchControls(true);
        mapView.getController().setZoom(9.5);
        mapView.setTileSource(TileSourceFactory.MAPNIK);

        LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }

        if (lm.getLastKnownLocation(LocationManager.GPS_PROVIDER) == null) {
            Toast.makeText(this, "Can't get your location without GPS. Please enable it on your device.", Toast.LENGTH_SHORT).show();
            return;
        }

        Location mLastLocation = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        double longitude = mLastLocation.getLongitude();
        double latitude = mLastLocation.getLatitude();

        //double longitude = 105.764811;
        //double latitude = 10.027050;

        GeoPoint startPoint = new GeoPoint(latitude, longitude);
        mapView.getController().setCenter(startPoint);

        Marker startMarker = new Marker(mapView);
        startMarker.setPosition(startPoint);
        startMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        mapView.getOverlays().add(startMarker);

        ArrayList<GeoPoint> waypoints = new ArrayList<>();
        waypoints.add(startPoint);
        GeoPoint endPoint = new GeoPoint(10.030325, 105.770221);
        waypoints.add(endPoint);

        Marker endMarker = new Marker(mapView);
        endMarker.setPosition(endPoint);
        endMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        mapView.getOverlays().add(endMarker);

        RoadManager roadManager = new OSRMRoadManager(this);
        Road road = roadManager.getRoad(waypoints);
        Polyline roadOverlay = RoadManager.buildRoadOverlay(road);
        mapView.getOverlays().add(roadOverlay);
        mapView.invalidate();
    }

    public void onResume(){
        super.onResume();
        //this will refresh the osmdroid configuration on resuming.
        //if you make changes to the configuration, use
        //SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        //Configuration.getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this));
        mapView.onResume(); //needed for compass, my location overlays, v6.0.0 and up
    }

    public void onPause(){
        super.onPause();
        //this will refresh the osmdroid configuration on resuming.
        //if you make changes to the configuration, use
        //SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        //Configuration.getInstance().save(this, prefs);
        mapView.onPause();  //needed for compass, my location overlays, v6.0.0 and up
    }
}