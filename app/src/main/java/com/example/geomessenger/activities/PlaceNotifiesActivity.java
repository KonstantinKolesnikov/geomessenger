package com.example.geomessenger.activities;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.example.geomessenger.R;
import com.example.geomessenger.adapters.NotificationPlaceAdapter;
import com.example.geomessenger.adapters.UsersAdapter;
import com.example.geomessenger.listeners.NotificationPlaceListener;
import com.example.geomessenger.models.NotificationPlace;
import com.example.geomessenger.models.User;
import com.example.geomessenger.utilities.Constants;
import com.example.geomessenger.utilities.PreferenceManager;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class PlaceNotifiesActivity extends BaseActivity implements NotificationPlaceListener {

    private PreferenceManager preferenceManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_place_notifies);
        preferenceManager = new PreferenceManager(getApplicationContext());
        getNotificationPlaces();
        setListeners();
    }

    private void setListeners(){
        findViewById(R.id.imageBack).setOnClickListener(v -> {
            onBackPressed();
        });
        findViewById(R.id.buttonCreatePlaceNotification).setOnClickListener(v -> {
            Intent intent = new Intent(this, CreateNotificationPlaceActivity.class);
            startActivity(intent);
        });
    }

    private void getNotificationPlaces(){
        loading(true);
        FirebaseFirestore database = FirebaseFirestore.getInstance();
        database.collection(Constants.KEY_COLLECTION_NOTIFICATION_LOCATIONS)
                .whereEqualTo(Constants.KEY_USER_ID, preferenceManager.getString(Constants.KEY_USER_ID))
                .get()
                .addOnCompleteListener(task -> {
                    loading(false);
                    if(task.isSuccessful() && task.getResult() != null){
                        List<NotificationPlace> places = new ArrayList<>();
                        for (QueryDocumentSnapshot queryDocumentSnapshot : task.getResult()){
                            NotificationPlace place = new NotificationPlace();
                            place.userId = queryDocumentSnapshot.getString(Constants.KEY_USER_ID);
                            place.radius = queryDocumentSnapshot.getString(Constants.KEY_RADIUS);
                            place.longitude = queryDocumentSnapshot.getString(Constants.KEY_LONGITUDE);
                            place.latitude = queryDocumentSnapshot.getString(Constants.KEY_LATITUDE);
                            place.address = queryDocumentSnapshot.getString(Constants.KEY_ADDRESS);
                            place.dateObject = queryDocumentSnapshot.getDate(Constants.KEY_TIMESTAMP);
                            places.add(place);
                        }
                        if (places.size() > 0) {
                            NotificationPlaceAdapter placeAdapter = new NotificationPlaceAdapter(places, this);
                            RecyclerView recyclerView = findViewById(R.id.notificationPlacesRecyclerView);
                            recyclerView.setAdapter(placeAdapter);
                            recyclerView.setVisibility(View.VISIBLE);
                        } else {
                            showErrorMessage();
                        }
                    } else {
                        showErrorMessage();
                    }
                });
    }

    private void loading(Boolean isLoading){
        ProgressBar progressBar = findViewById(R.id.progressBar);

        if (isLoading){
            progressBar.setVisibility(View.VISIBLE);
        } else {
            progressBar.setVisibility(View.INVISIBLE);
        }
    }

    private void showErrorMessage() {
        TextView textErrorMessage = findViewById(R.id.textErrorMessage);
        textErrorMessage.setText(String.format("%s", "Нет доступных уведомлений по локации"));
        textErrorMessage.setVisibility(View.VISIBLE);
    }

    @Override
    public void onNotificationPlaceClicked(NotificationPlace place) {
        Intent intent = new Intent(getApplicationContext(), SearchActivity.class);
        intent.putExtra(Constants.KEY_FILTER, "la");
        startActivity(intent);
        finish();
    }
}