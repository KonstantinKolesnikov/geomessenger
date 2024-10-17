package com.example.geomessenger.activities;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.geomessenger.R;
import com.example.geomessenger.models.User;
import com.example.geomessenger.network.ApiClient;
import com.example.geomessenger.network.ApiService;
import com.example.geomessenger.utilities.Constants;
import com.example.geomessenger.utilities.PreferenceManager;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CreateDiscussionActivity extends BaseActivity {

    private PreferenceManager preferenceManager;
    private String encodedImage, address;
    private FirebaseFirestore database;
    private HashMap<String, Object> discussion;
    private List<User> receivers;
    private HashMap<String, Boolean> receiverAvailability;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_discussion);
        preferenceManager = new PreferenceManager(getApplicationContext());
        receivers = new ArrayList<>();
        receiverAvailability = new HashMap<>();
        setListeners();
    }

    private void setListeners(){
        findViewById(R.id.imageBack).setOnClickListener(v -> onBackPressed());
        findViewById(R.id.imageDiscussion).setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            pickImage.launch(intent);
        });
        findViewById(R.id.buttonCreateDiscussion).setOnClickListener(v -> createDiscussion());
    }

    private void createDiscussion(){
        loading(true);

        TextView textView;
        textView = findViewById(R.id.inputTitle);
        String inputTitle = textView.getText().toString();
        textView = findViewById(R.id.inputLocationTitle);
        String inputLocationTitle = textView.getText().toString();
        textView = findViewById(R.id.inputLongitude);
        String inputLongitude = textView.getText().toString();
        textView = findViewById(R.id.inputLatitude);
        String inputLatitude = textView.getText().toString();

        database = FirebaseFirestore.getInstance();

        discussion = new HashMap<>();
        discussion.put(Constants.KEY_CREATOR_ID, preferenceManager.getString(Constants.KEY_USER_ID));
        discussion.put(Constants.KEY_DISCUSSION_IMAGE, encodedImage);
        discussion.put(Constants.KEY_DISCUSSION_TITLE, inputTitle);
        discussion.put(Constants.KEY_LOCATION_TITLE, inputLocationTitle);
        discussion.put(Constants.KEY_LONGITUDE, inputLongitude);
        discussion.put(Constants.KEY_LATITUDE, inputLatitude);
        discussion.put(Constants.KEY_TIMESTAMP, new Date());


        database.collection(Constants.KEY_COLLECTION_DISCUSSION)
                .add(discussion)
                .addOnSuccessListener(documentReference -> {
                    loading(false);

                    HashMap<String, Object> discussionUser = new HashMap<>();
                    discussionUser.put(Constants.KEY_USER_ID, preferenceManager.getString(Constants.KEY_USER_ID));
                    discussionUser.put(Constants.KEY_DISCUSSION_ID, documentReference.getId());
                    database.collection(Constants.KEY_COLLECTION_SUBSCRIBES)
                            .add(discussionUser);

                    listenAvailabilityOfReceiver();
                    for(User user : receivers){
                        if (!receiverAvailability.get(user.id)) {
                            try {
                                JSONArray tokens = new JSONArray();
                                tokens.put(user.token);

                                JSONObject data = new JSONObject();
                                data.put(Constants.KEY_NAME, address);
                                data.put(Constants.KEY_FCM_TOKEN, preferenceManager.getString(Constants.KEY_FCM_TOKEN));
                                data.put(Constants.KEY_MESSAGE, "Новая дискуссия : " + discussion.get(Constants.KEY_DISCUSSION_TITLE));

                                JSONObject body = new JSONObject();
                                body.put(Constants.REMOTE_MSG_DATA, data);
                                body.put(Constants.REMOTE_MSG_REGISTRATION_IDS, tokens);

                                sendNotification(body.toString());
                            } catch (Exception e) {
                                Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        }
                    }

                    onBackPressed();
                })
                .addOnFailureListener(e -> {
                    loading(false);
                    Toast.makeText(this, "Произошла ошибка", Toast.LENGTH_SHORT).show();
                });
    }

    private void sendNotification(String messageBody){
        ApiClient.getClient().create(ApiService.class).sendMessage(
                Constants.getRemoteMsgHeaders(),
                messageBody
        ).enqueue(new Callback<String>() {
            @Override
            public void onResponse(@NonNull Call<String> call, @NonNull Response<String> response) {
                if(response.isSuccessful()){
                    try {
                        if(response.body() != null){
                            JSONObject responseJson = new JSONObject(response.body());
                            JSONArray results = responseJson.getJSONArray("results");
                            if(responseJson.getInt("failure") == 1){
                                JSONObject error = (JSONObject) results.get(0);
                                Toast.makeText(CreateDiscussionActivity.this, error.getString("error"), Toast.LENGTH_SHORT).show();
                                return;
                            }
                        }
                    } catch (JSONException e){
                        e.printStackTrace();
                    }
                    Toast.makeText(CreateDiscussionActivity.this, "Notification sent successfully", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(CreateDiscussionActivity.this, "Error: " + response.code(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<String> call, @NonNull Throwable t) {
                Toast.makeText(CreateDiscussionActivity.this, t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void listenAvailabilityOfReceiver(){
        database.collection(Constants.KEY_COLLECTION_NOTIFICATION_LOCATIONS)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        return;
                    }
                    if (value != null) {
                        for (DocumentSnapshot documentSnapshot : value.getDocuments()) {
                            address = documentSnapshot.getString(Constants.KEY_ADDRESS);

                            Location discussionLoc = new Location("");
                            Double discussionLatitude = Double.parseDouble(discussion.get(Constants.KEY_LATITUDE).toString());
                            Double discussionLongitude= Double.parseDouble(discussion.get(Constants.KEY_LONGITUDE).toString());
                            discussionLoc.setLatitude(discussionLatitude);
                            discussionLoc.setLongitude(discussionLongitude);

                            Double longitude = Double.parseDouble(documentSnapshot.getString(Constants.KEY_LONGITUDE));
                            Double latitude = Double.parseDouble(documentSnapshot.getString(Constants.KEY_LATITUDE));
                            Location destination = new Location("");
                            destination.setLongitude(longitude);
                            destination.setLatitude(latitude);

                            Float distance = discussionLoc.distanceTo(destination);
                            Double radius = Double.parseDouble(documentSnapshot.getString(Constants.KEY_RADIUS));
                            if(distance <= radius) {
                                database.collection(Constants.KEY_COLLECTION_USERS).document(
                                        documentSnapshot.getString(Constants.KEY_USER_ID)
                                ).addSnapshotListener(CreateDiscussionActivity.this, ((value1, error1) -> {
                                    if (error1 != null){
                                        return;
                                    }
                                    if (value1 != null){
                                        User receiver = new User();
                                        receiver.id = documentSnapshot.getString(Constants.KEY_USER_ID);
                                        receiver.token = value1.getString(Constants.KEY_FCM_TOKEN);
                                        if(value1.getLong(Constants.KEY_AVAILABILITY) != null){
                                            int availability = Objects.requireNonNull(
                                                    value1.getLong(Constants.KEY_AVAILABILITY)
                                            ).intValue();
                                            receiverAvailability.put(receiver.id, availability == 1);
                                        }
                                        receivers.add(receiver);
                                    }
                                }));
                            }
                        }
                    }
                });
    }

    private String encodedImage(Bitmap bitmap){
        int previewWidth = 150;
        int previewHeight = bitmap.getHeight() * previewWidth / bitmap.getWidth();
        Bitmap previewBitmap = Bitmap.createScaledBitmap(bitmap, previewWidth, previewHeight, false);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        previewBitmap.compress(Bitmap.CompressFormat.JPEG, 50, byteArrayOutputStream);
        byte[] bytes = byteArrayOutputStream.toByteArray();
        return Base64.encodeToString(bytes, Base64.DEFAULT);
    }

    private final ActivityResultLauncher<Intent> pickImage = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK){
                    if (result.getData() != null){
                        Uri imageUri = result.getData().getData();
                        try {
                            InputStream inputStream = getContentResolver().openInputStream(imageUri);
                            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                            ImageView imageProfile = findViewById(R.id.imageDiscussion);
                            imageProfile.setImageBitmap(bitmap);
                            findViewById(R.id.textAddImage).setVisibility(View.GONE);
                            encodedImage = encodedImage(bitmap);
                        } catch (FileNotFoundException e){
                            e.printStackTrace();;
                        }
                    }
                }
            }
    );

    private void loading(Boolean isLoading){
        Button buttonCreateDiscussion = findViewById(R.id.buttonCreateDiscussion);
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