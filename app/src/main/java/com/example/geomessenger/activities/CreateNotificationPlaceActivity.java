package com.example.geomessenger.activities;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.geomessenger.R;
import com.example.geomessenger.models.User;
import com.example.geomessenger.utilities.Constants;
import com.example.geomessenger.utilities.PreferenceManager;
import com.google.firebase.firestore.FirebaseFirestore;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public class CreateNotificationPlaceActivity extends BaseActivity {

    private PreferenceManager preferenceManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_notification_place);
        preferenceManager = new PreferenceManager(getApplicationContext());
        setListeners();
    }

    private void setListeners(){
        findViewById(R.id.imageBack).setOnClickListener(v -> onBackPressed());
        findViewById(R.id.buttonCreate).setOnClickListener(v -> create());
    }

    private void create(){
        loading(true);

        TextView textView;
        textView = findViewById(R.id.inputRadius);
        String inputRadius = textView.getText().toString();
        textView = findViewById(R.id.inputLongitude);
        String inputLongitude = textView.getText().toString();
        textView = findViewById(R.id.inputLatitude);
        String inputLatitude = textView.getText().toString();

        FirebaseFirestore database = FirebaseFirestore.getInstance();

        Geocoder gcd = new Geocoder(getBaseContext(),
                Locale.getDefault());
        List<Address> addresses;
        String address = "";
        try {
            addresses = gcd.getFromLocation(Double.parseDouble(inputLatitude), Double.parseDouble(inputLongitude), 1);
            if (addresses.size() > 0)
                address = addresses.get(0).getAddressLine(0);
        } catch (IOException e) {
            e.printStackTrace();
        }

        HashMap<String, Object> place = new HashMap<>();
        place.put(Constants.KEY_ADDRESS, address);
        place.put(Constants.KEY_USER_ID, preferenceManager.getString(Constants.KEY_USER_ID));
        place.put(Constants.KEY_RADIUS, inputRadius);
        place.put(Constants.KEY_LONGITUDE, inputLongitude);
        place.put(Constants.KEY_LATITUDE, inputLatitude);
        place.put(Constants.KEY_TIMESTAMP, new Date());


        database.collection(Constants.KEY_COLLECTION_NOTIFICATION_LOCATIONS)
                .add(place)
                .addOnSuccessListener(documentReference -> {
                    loading(false);
                    onBackPressed();
                })
                .addOnFailureListener(e -> {
                    loading(false);
                    Toast.makeText(this, "Произошла ошибка", Toast.LENGTH_SHORT).show();
                });
    }

    private void loading(Boolean isLoading){
        Button buttonCreateDiscussion = findViewById(R.id.buttonCreate);
        ProgressBar progressBar = findViewById(R.id.progressBar);

        if (isLoading){
            buttonCreateDiscussion.setVisibility(View.INVISIBLE);
            progressBar.setVisibility(View.VISIBLE);
        } else {
            progressBar.setVisibility(View.INVISIBLE);
            buttonCreateDiscussion.setVisibility(View.VISIBLE);
        }
    }
}