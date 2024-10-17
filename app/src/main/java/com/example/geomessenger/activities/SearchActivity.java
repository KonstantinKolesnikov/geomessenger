package com.example.geomessenger.activities;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import android.content.ContentResolver;
import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.TextView;

import com.example.geomessenger.R;
import com.example.geomessenger.utilities.Constants;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class SearchActivity extends AppCompatActivity implements SearchDiscussionsFragment.OnItemCheckListener {

    private LocationManager locationManager;
    private LocationListener locationListener;
    private Location myLoc;
    private String address;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);
        init();
        setListeners();
    }

    private void init(){
        Bundle arguments = getIntent().getExtras();
        if(arguments != null){
            String filter = arguments.get(Constants.KEY_FILTER).toString();
            ((TextView)findViewById(R.id.inputSearch)).setText(filter);
        }
        locationManager = (LocationManager)
                getSystemService(Context.LOCATION_SERVICE);

        locationListener = new MyLocationListener();
        locationManager.requestSingleUpdate(LocationManager
                .GPS_PROVIDER, locationListener, null);
    }

    private void setListeners(){
        findViewById(R.id.imageBack).setOnClickListener(v -> onBackPressed());
        findViewById(R.id.textDiscussions).setOnClickListener(v -> {
            getSupportFragmentManager().beginTransaction().replace(R.id.searchFragmentView, new SearchDiscussionsFragment()).commit();
        });
        findViewById(R.id.textConversations).setOnClickListener(v -> {
            getSupportFragmentManager().beginTransaction().replace(R.id.searchFragmentView, new SearchUsersFragment()).commit();
        });
        findViewById(R.id.imageSearch).setOnClickListener(v -> {
            Fragment fragment = getSupportFragmentManager().getFragments().get(0);
            getSupportFragmentManager().beginTransaction().detach(fragment).commit();
            getSupportFragmentManager().beginTransaction().attach(fragment).commit();
        });
    }

    @Override
    public String onCheckTitle() {
        TextView inputSearch = findViewById(R.id.inputSearch);
        return inputSearch.getText().toString().trim();
    }

    @Override
    public Integer onCheckDistance(Double longitude, Double latitude) {
        TextView inputRadius = findViewById(R.id.inputRadius);
        if(inputRadius.getText().toString().isEmpty() ){
           return -1;
        }

        Location destination = new Location("");
        destination.setLongitude(longitude);
        destination.setLatitude(latitude);

        Float distance = myLoc.distanceTo(destination);

        if (distance > Integer.parseInt(inputRadius.getText().toString().trim())){
            return 0;
        } else {
            return 1;
        }
    }

    private Boolean displayGpsStatus() {
        ContentResolver contentResolver = getBaseContext()
                .getContentResolver();
        boolean gpsStatus = Settings.Secure
                .isLocationProviderEnabled(contentResolver,
                        LocationManager.GPS_PROVIDER);
        if (gpsStatus) {
            return true;
        } else {
            return false;
        }
    }

    private class MyLocationListener implements LocationListener {
        @Override
        public void onLocationChanged(Location loc) {

            myLoc = loc;

            Geocoder gcd = new Geocoder(getBaseContext(),
                    Locale.getDefault());
            List<Address> addresses;
            try {
                addresses = gcd.getFromLocation(loc.getLatitude(), loc
                        .getLongitude(), 1);
                if (addresses.size() > 0)
                    address = addresses.get(0).getAddressLine(0);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}