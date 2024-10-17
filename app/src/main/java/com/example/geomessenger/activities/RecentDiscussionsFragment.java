package com.example.geomessenger.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import com.example.geomessenger.R;
import com.example.geomessenger.adapters.RecentDiscussionAdapter;
import com.example.geomessenger.listeners.DiscussionListener;
import com.example.geomessenger.models.Discussion;
import com.example.geomessenger.utilities.Constants;
import com.example.geomessenger.utilities.PreferenceManager;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RecentDiscussionsFragment extends Fragment implements DiscussionListener{

    private PreferenceManager preferenceManager;
    private List<Discussion> discussionList;
    private RecentDiscussionAdapter discussionsAdapter;
    private FirebaseFirestore database;
    private View view;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_recent_discussions, container, false);

        preferenceManager = new PreferenceManager(view.getContext());
        init();

        listenSubscribes();
        return view;
    }

    private void init(){
        discussionList = new ArrayList<>();
        discussionsAdapter = new RecentDiscussionAdapter(discussionList, this);
        ((RecyclerView) view.findViewById(R.id.discussionsRecyclerView)).setAdapter(discussionsAdapter);
        database = FirebaseFirestore.getInstance();
    }

    private void listenSubscribes(){
        database.collection(Constants.KEY_COLLECTION_SUBSCRIBES)
                .whereEqualTo(Constants.KEY_USER_ID, preferenceManager.getString(Constants.KEY_USER_ID))
                .addSnapshotListener(subscribesListener);
    }

    private final EventListener<QuerySnapshot> subscribesListener = ((value, error) -> {
        if (error != null) {
            return;
        }
        if (value != null) {
            for (DocumentChange documentChange : value.getDocumentChanges()) {
                if (documentChange.getType() == DocumentChange.Type.ADDED) {
                    String id = documentChange.getDocument().getString(Constants.KEY_DISCUSSION_ID);
                    listenDiscussion(id);
                }
            }
        }
    });

    private void listenDiscussion(String discussionId){
            //database.collection(Constants.KEY_COLLECTION_DISCUSSION).whereEqualTo(Constants.KEY_DISCUSSION_ID, discussionId).addSnapshotListener(discussionsListener);

            database.collection(Constants.KEY_COLLECTION_DISCUSSION).document(discussionId)
                    .addSnapshotListener(discussionListener);
    }

    private final EventListener<DocumentSnapshot> discussionListener = new EventListener<DocumentSnapshot>() {
        @Override
        public void onEvent(@Nullable DocumentSnapshot value, @Nullable FirebaseFirestoreException error) {
            if (error != null) {
                return;
            }
            if (value != null && value.exists()){
                Discussion discussion = new Discussion();
                discussion.discussionId = value.getId();
                discussion.title = value.getString(Constants.KEY_DISCUSSION_TITLE);
                discussion.image = value.getString(Constants.KEY_DISCUSSION_IMAGE);
                discussion.locationTitle = value.getString(Constants.KEY_LOCATION_TITLE);
                discussion.latitude = value.getString(Constants.KEY_LATITUDE);
                discussion.longitude = value.getString(Constants.KEY_LONGITUDE);
                discussion.dateObject = value.getDate(Constants.KEY_TIMESTAMP);
                if (value.getString(Constants.KEY_LAST_MESSAGE) != null) {
                    discussion.message = value.getString(Constants.KEY_LAST_MESSAGE);
                    discussion.name = value.getString(Constants.KEY_USER);
                }
                discussionList.add(discussion);
                Collections.sort(discussionList, (obj1, obj2) -> obj2.dateObject.compareTo(obj1.dateObject));
                discussionsAdapter.notifyDataSetChanged();
                RecyclerView recyclerView = view.findViewById(R.id.discussionsRecyclerView);
                recyclerView.smoothScrollToPosition(0);
                recyclerView.setVisibility(View.VISIBLE);
                view.findViewById(R.id.progressBar).setVisibility(View.GONE);
            }
        }
    };

    @Override
    public void onDiscussionClicked(Discussion discussion) {
        Intent intent = new Intent(view.getContext(), DiscussionChatActivity.class);
        intent.putExtra(Constants.KEY_DISCUSSION, discussion);
        startActivity(intent);
    }
}
