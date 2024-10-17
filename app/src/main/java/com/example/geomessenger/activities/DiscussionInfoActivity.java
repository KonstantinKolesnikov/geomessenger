package com.example.geomessenger.activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.geomessenger.R;
import com.example.geomessenger.models.Discussion;
import com.example.geomessenger.utilities.Constants;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class DiscussionInfoActivity extends BaseActivity {

    String userId, discussionId;
    FirebaseFirestore database;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_discussion_info);
        init();
        listenIsSubscribe();
        setListeners();
    }

    private void init(){
        database = FirebaseFirestore.getInstance();
        Bundle arguments = getIntent().getExtras();
        if(arguments != null){
            String encodedImage = arguments.getString(Constants.KEY_DISCUSSION_IMAGE);
            String title = arguments.getString(Constants.KEY_DISCUSSION_TITLE);
            userId = arguments.getString(Constants.KEY_USER_ID);
            discussionId = arguments.getString(Constants.KEY_DISCUSSION_ID);

            ImageView imageView = findViewById(R.id.imageDiscussion);
            TextView textView = findViewById(R.id.title);

            imageView.setImageBitmap(getConversionImage(encodedImage));
            textView.setText(title);
        }
    }

    private void listenIsSubscribe(){
        database.collection(Constants.KEY_COLLECTION_SUBSCRIBES)
                .whereEqualTo(Constants.KEY_DISCUSSION_ID, discussionId)
                .whereEqualTo(Constants.KEY_USER_ID, userId)
                .get()
                .addOnCompleteListener(task -> {
                    if(task.isSuccessful() && task.getResult() != null) {
                        if(task.getResult().getDocuments().isEmpty()){
                            findViewById(R.id.buttonSubscribe).setVisibility(View.VISIBLE);
                            findViewById(R.id.buttonUnsubscribe).setVisibility(View.GONE);
                        } else {
                            findViewById(R.id.buttonSubscribe).setVisibility(View.GONE);
                            findViewById(R.id.buttonUnsubscribe).setVisibility(View.VISIBLE);
                        }
                    }
                });
    }

    private void setListeners(){
        findViewById(R.id.imageBack).setOnClickListener(v -> {
            onBackPressed();
        });
        findViewById(R.id.buttonSubscribe).setOnClickListener(v -> {
            subscribe();
        });
        findViewById(R.id.buttonUnsubscribe).setOnClickListener(v -> {
            unsubscribe();
        });
    }

    private void subscribe(){
        HashMap<String, String> subscription = new HashMap<>();
        subscription.put(Constants.KEY_USER_ID, userId);
        subscription.put(Constants.KEY_DISCUSSION_ID, discussionId);
        database.collection(Constants.KEY_COLLECTION_SUBSCRIBES)
                .add(subscription)
                .addOnFailureListener(exception -> {
                    Toast.makeText(this, exception.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void unsubscribe(){
        database.collection(Constants.KEY_COLLECTION_SUBSCRIBES)
                .whereEqualTo(Constants.KEY_DISCUSSION_ID, discussionId)
                .whereEqualTo(Constants.KEY_USER_ID, userId)
                .get()
                .addOnCompleteListener(task -> {
                    if(task.isSuccessful() && task.getResult() != null) {
                        //QuerySnapshot querySnapshot = task.getResult();
                        //String documentId = querySnapshot.getDocuments().get(0).getId();
                        //database.document(documentId).delete();
                        for (QueryDocumentSnapshot queryDocumentSnapshot : task.getResult()) {
                            database.collection(Constants.KEY_COLLECTION_SUBSCRIBES).document(queryDocumentSnapshot.getId())
                                    .delete()
                                    .addOnFailureListener(new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception e) {
                                            Log.w("Error", "Error deleting document", e);
                                        }
                                    });
                        }
                    }
                });
    }

    public Bitmap getConversionImage(String encodedImage){
        byte[] bytes = Base64.decode(encodedImage, Base64.DEFAULT);
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
    }
}