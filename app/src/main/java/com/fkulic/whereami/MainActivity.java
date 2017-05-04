package com.fkulic.whereami;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.TextView;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback, LocationListener {
    private static final String TAG = "MainActivity";
    private static final int PERMISSION_REQ_FINE_LOC = 10;
    GoogleMap mGoogleMap;
    SupportMapFragment mMapFragment;


    TextView tvLatitudeValue;
    TextView tvLongitudeValue;
    TextView tvCountryValue;
    TextView tvLocalityValue;
    TextView tvAddressValue;


    private Marker mCurrentPostion;
    private LocationManager mLocationManager;
    private GoogleMap.OnMapClickListener mGoogleMapClickListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        this.setUpUI();
    }

    private void setUpUI() {
        this.tvLatitudeValue = (TextView) findViewById(R.id.tvLatitudeValue);
        this.tvLongitudeValue = (TextView) findViewById(R.id.tvLongitudeValue);
        this.tvCountryValue = (TextView) findViewById(R.id.tvCountryValue);
        this.tvLocalityValue = (TextView) findViewById(R.id.tvLocalityValue);
        this.tvAddressValue = (TextView) findViewById(R.id.tvAddressValue);
        this.mMapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.fGoogleMaps);
        this.mMapFragment.getMapAsync(this);
        this.mGoogleMapClickListener = new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {
                MarkerOptions markerOptions = new MarkerOptions();
                markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.marker_new));
                markerOptions.title("New marker");
                markerOptions.position(latLng);
                mGoogleMap.addMarker(markerOptions);
            }
        };
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        this.mGoogleMap = googleMap;
        UiSettings settings = this.mGoogleMap.getUiSettings();
        settings.setZoomControlsEnabled(true);
        settings.setMyLocationButtonEnabled(true);
        settings.setZoomGesturesEnabled(true);
        this.mGoogleMap.setOnMapClickListener(this.mGoogleMapClickListener);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_REQ_FINE_LOC);
            return;
        }
        this.mGoogleMap.setMyLocationEnabled(true);

        this.mLocationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        this.mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 30000, 30, this);
    }


    @Override
    public void onLocationChanged(Location location) {
        if (mCurrentPostion != null) {
            mCurrentPostion.remove();
        }

        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.marker_current));
        markerOptions.title(getString(R.string.imHere));
        markerOptions.position(latLng);
        this.mCurrentPostion = mGoogleMap.addMarker(markerOptions);

        changeLocationLabels(location);
    }

    private void changeLocationLabels(Location location) {
        tvLatitudeValue.setText(String.valueOf(location.getLatitude()));
        tvLongitudeValue.setText(String.valueOf(location.getLongitude()));
        if (Geocoder.isPresent()) {
            Geocoder geocoder = new Geocoder(this, Locale.getDefault());
            try {
                List<Address> addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
                if (addresses.size() > 0) {
                    Address address = addresses.get(0);
                    tvCountryValue.setText(address.getCountryName());
                    tvLocalityValue.setText(address.getLocality());
                    tvAddressValue.setText(address.getAddressLine(0));
                } else {
                    tvCountryValue.setText(R.string.notFound);
                    tvLocalityValue.setText(R.string.notFound);
                    tvAddressValue.setText(R.string.notFound);
                }
            } catch (IOException e) {
                Log.e(TAG, "Error getting info from geocoder: " + e.getMessage());
            }
        }
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }
}
