package com.fkulic.whereami;

import android.Manifest;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.AudioManager;
import android.media.RingtoneManager;
import android.media.SoundPool;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback, LocationListener {
    private static final String TAG = "MainActivity";
    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final int PERMISSION_REQ_FINE_LOC = 10;
    private static final int PERMISSION_REQ_WRITE_EXT = 11;
    GoogleMap mGoogleMap;
    SupportMapFragment mMapFragment;
    SoundPool mSoundPool;
    private boolean mSoundsLoaded = false;
    private int mBlopSoundID;

    NotificationManagerCompat mNotificationManager;

    String mPhotoPath;

    TextView tvLatitudeValue;
    TextView tvLongitudeValue;
    TextView tvCountryValue;
    TextView tvLocalityValue;
    TextView tvAddressValue;
    ImageButton ibTakePhoto;

    private Marker mCurrentPosition;
    LocationManager mLocationManager;
    private GoogleMap.OnMapClickListener mGoogleMapClickListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        this.setUpUI();
        this.loadSounds();
        this.mNotificationManager = NotificationManagerCompat.from(this);
    }

    private void setUpUI() {
        this.tvLatitudeValue = (TextView) findViewById(R.id.tvLatitudeValue);
        this.tvLongitudeValue = (TextView) findViewById(R.id.tvLongitudeValue);
        this.tvCountryValue = (TextView) findViewById(R.id.tvCountryValue);
        this.tvLocalityValue = (TextView) findViewById(R.id.tvLocalityValue);
        this.tvAddressValue = (TextView) findViewById(R.id.tvAddressValue);
        this.ibTakePhoto = (ImageButton) findViewById(R.id.ibTakePhoto);
        this.mMapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.fGoogleMaps);
        this.mMapFragment.getMapAsync(this);
        this.mGoogleMapClickListener = new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {
                if (mSoundsLoaded) {
                    mSoundPool.play(mBlopSoundID, 1, 1, 1, 0, 1);
                }
                MarkerOptions markerOptions = new MarkerOptions();
                markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.marker_new));
                markerOptions.position(latLng);
                mGoogleMap.addMarker(markerOptions);
            }
        };
        this.ibTakePhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSION_REQ_WRITE_EXT);
                    return;
                }
                takePicture();
            }
        });
    }

    private void loadSounds() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            this.mSoundPool = new SoundPool(10, AudioManager.STREAM_MUSIC, 0);
        } else {
            this.mSoundPool = new SoundPool.Builder().setMaxStreams(1).build();
        }

        this.mSoundPool.setOnLoadCompleteListener(new SoundPool.OnLoadCompleteListener() {
            @Override
            public void onLoadComplete(SoundPool soundPool, int sampleId, int status) {
                mSoundsLoaded = true;
            }
        });

        this.mBlopSoundID = this.mSoundPool.load(this, R.raw.blop, 1);
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
        this.mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 10000, 30, this);
    }


    @Override
    public void onLocationChanged(Location location) {
        if (mCurrentPosition != null) {
            mCurrentPosition.remove();
        }

        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.marker_current));
        markerOptions.title(getString(R.string.imHere));
        markerOptions.position(latLng);
        this.mCurrentPosition = mGoogleMap.addMarker(markerOptions);

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

    private void takePicture() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException e) {
                Log.d(TAG, "takePicture: couldn't create image file" + e.getMessage());
            }

            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(this, "com.fkulic.whereami.fileprovider", photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
            }
        }
    }

    private File createImageFile() throws IOException {
        StringBuilder imageFileName = new StringBuilder();
        imageFileName.append(tvLocalityValue.getText());
        imageFileName.append("_");
        imageFileName.append(new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date()));
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(imageFileName.toString(), ".jpg", storageDir);
        this.mPhotoPath = image.getAbsolutePath();
        return image;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_IMAGE_CAPTURE) {
            if (resultCode == RESULT_OK) {
                sendNotificationPictureTaken();
            } else {
                //delete if picture wasn't taken
                File image = new File(mPhotoPath);
                image.delete();
            }
        }
    }

    private void sendNotificationPictureTaken() {
        String messageTxt = mPhotoPath;

        Intent notificationIntent = new Intent();
        notificationIntent.setAction(Intent.ACTION_VIEW);
        notificationIntent.setDataAndType(Uri.parse("file://" + mPhotoPath), "image/*");

        PendingIntent pi = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_ONE_SHOT);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
        builder.setAutoCancel(true)
                .setContentTitle(getString(R.string.newPhoto))
                .setContentText(messageTxt)
                .setSmallIcon(android.R.drawable.ic_menu_camera)
                .setContentIntent(pi)
                .setLights(Color.GREEN, 1000, 3000)
                .setVibrate(new long[] { 500,110,500,110,450,110,200,110,170,40,450,110,200,110,170,40,500 })
                .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION));
        Notification notification = builder.build();

        mNotificationManager.notify(0, notification);
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
