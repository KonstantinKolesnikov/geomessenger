package com.example.geomessenger.activities;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.geomessenger.R;
import com.example.geomessenger.adapters.ChatAdapter;
import com.example.geomessenger.models.ChatMessage;
import com.example.geomessenger.models.Discussion;
import com.example.geomessenger.models.User;
import com.example.geomessenger.network.ApiClient;
import com.example.geomessenger.network.ApiService;
import com.example.geomessenger.utilities.Constants;
import com.example.geomessenger.utilities.PreferenceManager;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;


public class DiscussionChatActivity extends BaseActivity{

    private List<User> receivers;
    private Discussion discussion;
    private List<ChatMessage> chatMessages;
    private ChatAdapter chatAdapter;
    private PreferenceManager preferenceManager;
    private FirebaseFirestore database;
    private HashMap<String, Boolean> receiverAvailability;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
        loadDiscussionDetails();
        init();
        setListeners();
        listenMessages();
    }

    private void init() {
        preferenceManager = new PreferenceManager(getApplicationContext());
        receivers = new ArrayList<>();
        receiverAvailability = new HashMap<>();
        chatMessages = new ArrayList<>();
        chatAdapter = new ChatAdapter(
                chatMessages,
                preferenceManager.getString(Constants.KEY_USER_ID)
        );
        ((RecyclerView) findViewById(R.id.chatRecyclerView)).setAdapter(chatAdapter);
        database = FirebaseFirestore.getInstance();
    }

    private void sendMessage() {
        TextView inputMessage = findViewById(R.id.inputMessage);

        HashMap<String, Object> message = new HashMap<>();
        message.put(Constants.KEY_SENDER_ID, preferenceManager.getString(Constants.KEY_USER_ID));
        message.put(Constants.KEY_SENDER_IMAGE, preferenceManager.getString(Constants.KEY_IMAGE));
        message.put(Constants.KEY_DISCUSSION_ID, discussion.discussionId);
        message.put(Constants.KEY_MESSAGE, inputMessage.getText().toString());
        message.put(Constants.KEY_TIMESTAMP, new Date());
        database.collection(Constants.KEY_COLLECTION_TEMP_CHAT).add(message);
        updateDiscussion(preferenceManager.getString(Constants.KEY_USER) + " : " +
                inputMessage.getText().toString());

        for(User user : receivers){
            if (!receiverAvailability.get(user.id)) {
                try {
                    JSONArray tokens = new JSONArray();
                    tokens.put(user.token);

                    JSONObject data = new JSONObject();
                    data.put(Constants.KEY_DISCUSSION_ID, discussion.discussionId);
                    data.put(Constants.KEY_NAME, discussion.title);
                    data.put(Constants.KEY_FCM_TOKEN, preferenceManager.getString(Constants.KEY_FCM_TOKEN));
                    data.put(Constants.KEY_MESSAGE, preferenceManager.getString(Constants.KEY_NAME)
                            + " : " + inputMessage.getText().toString());

                    JSONObject body = new JSONObject();
                    body.put(Constants.REMOTE_MSG_DATA, data);
                    body.put(Constants.REMOTE_MSG_REGISTRATION_IDS, tokens);

                    sendNotification(body.toString());
                } catch (Exception e) {
                    showToast(e.getMessage());
                }
            }
        }
        inputMessage.setText(null);
    }

    private void showToast(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
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
                                showToast(error.getString("error"));
                                return;
                            }
                        }
                    } catch (JSONException e){
                        e.printStackTrace();
                    }
                    showToast("Notification sent successfully");
                } else {
                    showToast("Error: " + response.code());
                }
            }

            @Override
            public void onFailure(@NonNull Call<String> call, @NonNull Throwable t) {
                showToast(t.getMessage());
            }
        });
    }

    private void listenAvailabilityOfReceiver(){
        database.collection(Constants.KEY_COLLECTION_SUBSCRIBES)
                .whereEqualTo(Constants.KEY_DISCUSSION_ID, discussion.discussionId)
                .addSnapshotListener((value, error) -> {

            if (error != null) {
                return;
            }
            if (value != null) {
                for (DocumentSnapshot documentSnapshot : value.getDocuments()) {
                    database.collection(Constants.KEY_COLLECTION_USERS).document(
                            documentSnapshot.getString(Constants.KEY_USER_ID)
                    ).addSnapshotListener(DiscussionChatActivity.this, ((value1, error1) -> {
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
        });
    }

    private void listenMessages() {
        database.collection(Constants.KEY_COLLECTION_TEMP_CHAT)
                .whereEqualTo(Constants.KEY_DISCUSSION_ID, discussion.discussionId)
                .addSnapshotListener(eventListener);
    }

    private final EventListener<QuerySnapshot> eventListener = ((value, error) -> {
        if(error != null) {
            return;
        }
        ProgressBar progressBar = findViewById(R.id.progressBar);
        if (value != null) {
            RecyclerView recyclerView = findViewById(R.id.chatRecyclerView);
            int count = chatMessages.size();
            for(DocumentChange documentChange : value.getDocumentChanges()) {
                if(documentChange.getType() == DocumentChange.Type.ADDED) {
                    ChatMessage chatMessage = new ChatMessage();
                    chatMessage.senderId = documentChange.getDocument().getString(Constants.KEY_SENDER_ID);
                    chatMessage.conversionImage = documentChange.getDocument().getString(Constants.KEY_SENDER_IMAGE);
                    chatMessage.receiverId = discussion.discussionId;
                    chatMessage.message = documentChange.getDocument().getString(Constants.KEY_MESSAGE);
                    chatMessage.dateTime = getReadableDateTime(documentChange.getDocument().getDate(Constants.KEY_TIMESTAMP));
                    chatMessage.dateObject = documentChange.getDocument().getDate(Constants.KEY_TIMESTAMP);
                    chatMessages.add(chatMessage);
                }
            }
            Collections.sort(chatMessages, (obj1, obj2) -> obj1.dateObject.compareTo(obj2.dateObject));
            if (count == 0){
                chatAdapter.notifyDataSetChanged();
            } else {
                chatAdapter.notifyItemRangeInserted(chatMessages.size(), chatMessages.size());
                recyclerView.smoothScrollToPosition(chatMessages.size() - 1);
            }
            recyclerView.setVisibility(View.VISIBLE);
        }
        progressBar.setVisibility(View.GONE);
        //checkForDiscussion();
    });

    private void loadDiscussionDetails(){
        discussion = (Discussion) getIntent().getSerializableExtra(Constants.KEY_DISCUSSION);
        TextView textTitle = findViewById(R.id.textName);
        textTitle.setText(discussion.title);
    }

    private void setListeners(){
        findViewById(R.id.imageBack).setOnClickListener(v -> onBackPressed());
        findViewById(R.id.layoutSend).setOnClickListener(v -> sendMessage());
        findViewById(R.id.imageInfo).setOnClickListener(v -> {
            Intent intent = new Intent(this, DiscussionInfoActivity.class);
            intent.putExtra(Constants.KEY_DISCUSSION_IMAGE, discussion.image);
            intent.putExtra(Constants.KEY_DISCUSSION_TITLE, discussion.title);
            intent.putExtra(Constants.KEY_USER_ID, preferenceManager.getString(Constants.KEY_USER_ID));
            intent.putExtra(Constants.KEY_DISCUSSION_ID, discussion.discussionId);
            startActivity(intent);
        });
    }

    private String getReadableDateTime(Date date){
        return new SimpleDateFormat("MMM dd, yyy - hh:mm a", Locale.getDefault()).format(date);
    }

    private void updateDiscussion(String message){
        DocumentReference documentReference =
                database.collection(Constants.KEY_COLLECTION_CONVERSATIONS).document(discussion.discussionId);
        documentReference.update(
                Constants.KEY_LAST_MESSAGE, message,
                Constants.KEY_TIMESTAMP, new Date()
        );
    }

    /*private void checkForDiscussion(){
        database.collection(Constants.KEY_COLLECTION_DISCUSSION)
                .document(discussion.discussionId)
                .get().addOnCompleteListener(discussionOnCompleteListener);
    }

    private final OnCompleteListener<DocumentSnapshot> discussionOnCompleteListener = (value, error) -> {
        if (error != null){
            return;
        }
        if (value != null){
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
    };*/

    @Override
    protected void onResume() {
        super.onResume();
        listenAvailabilityOfReceiver();
    }
}
