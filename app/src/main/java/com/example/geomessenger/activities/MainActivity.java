package com.example.geomessenger.activities;

import androidx.annotation.NonNull;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.geomessenger.R;
import com.example.geomessenger.utilities.Constants;
import com.example.geomessenger.utilities.PreferenceManager;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.HashMap;

public class MainActivity extends BaseActivity implements NavigationView.OnNavigationItemSelectedListener {

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private PreferenceManager preferenceManager;
    private int page;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        preferenceManager = new PreferenceManager(this);

        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        getToken();
        loadUserDetails();
        setListeners();
    }

    private void loadUserDetails(){
        TextView textView = navigationView.getHeaderView(0).findViewById(R.id.textName1);
        ImageView imageView = navigationView.getHeaderView(0).findViewById(R.id.imageProfile1);

        textView.setText(preferenceManager.getString(Constants.KEY_NAME));
        byte[] bytes = Base64.decode(preferenceManager.getString(Constants.KEY_IMAGE), Base64.DEFAULT);
        Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        imageView.setImageBitmap(bitmap);
    }

    private void setListeners(){
        findViewById(R.id.imageDrawerToggle).setOnClickListener(v ->{
            if (!drawerLayout.isDrawerOpen(GravityCompat.START)){
                drawerLayout.openDrawer(GravityCompat.START);
            } else {
                drawerLayout.closeDrawer(GravityCompat.START);
            }
            });
        findViewById(R.id.textDiscussions).setOnClickListener(v -> {
            getSupportFragmentManager().beginTransaction().replace(R.id.mainFragmentView, new RecentDiscussionsFragment()).commit();
        });
        findViewById(R.id.textConversations).setOnClickListener(v -> {
            getSupportFragmentManager().beginTransaction().replace(R.id.mainFragmentView, new RecentConversationsFragment()).commit();
        });
        findViewById(R.id.imageSearch).setOnClickListener(v -> {
            Intent intent = new Intent(this, SearchActivity.class);
            TextView textView = findViewById(R.id.inputSearch);
            String filter = textView.getText().toString();
            if (!filter.isEmpty()) {
                intent.putExtra(Constants.KEY_FILTER, filter);
            }
            startActivity(intent);
        });
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        Intent intent;
        switch (item.getItemId()){
            case R.id.nav_add_discussion:
                intent = new Intent(this, CreateDiscussionActivity.class);
                startActivity(intent);
                break;
            case R.id.nav_add_place_notification:
                intent = new Intent(this, PlaceNotifiesActivity.class);
                startActivity(intent);
                break;
            case R.id.nav_logout:
                Toast.makeText(this, "Signing out...", Toast.LENGTH_SHORT).show();
                FirebaseFirestore database = FirebaseFirestore.getInstance();
                DocumentReference documentReference =
                        database.collection(Constants.KEY_COLLECTION_USERS).document(
                                preferenceManager.getString(Constants.KEY_USER_ID)
                        );
                HashMap<String, Object> updates = new HashMap<>();
                updates.put(Constants.KEY_FCM_TOKEN, FieldValue.delete());
                documentReference.update(updates)
                        .addOnSuccessListener(unused -> {
                            preferenceManager.clear();
                            startActivity(new Intent(this, SignInActivity.class));
                            finish();
                        })
                        .addOnFailureListener(e ->
                                Toast.makeText(this, "Unable to sign out", Toast.LENGTH_SHORT).show());
                break;
        }
        return true;
    }

    private void  getToken() {
        FirebaseMessaging.getInstance().getToken().addOnSuccessListener(this::updateToken);
    }

    private void updateToken(String token){
        preferenceManager.putString(Constants.KEY_FCM_TOKEN, token);
        FirebaseFirestore database = FirebaseFirestore.getInstance();
        DocumentReference documentReference =
                database.collection(Constants.KEY_COLLECTION_USERS).document(
                        preferenceManager.getString(Constants.KEY_USER_ID)
                );
        documentReference.update(Constants.KEY_FCM_TOKEN, token)
                .addOnFailureListener(e -> showToast("Unable to update token"));
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)){
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }
}