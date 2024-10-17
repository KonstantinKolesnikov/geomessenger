package com.example.geomessenger.activities;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.example.geomessenger.R;
import com.example.geomessenger.adapters.RecentDiscussionAdapter;
import com.example.geomessenger.listeners.DiscussionListener;
import com.example.geomessenger.models.Discussion;
import com.example.geomessenger.utilities.Constants;
import com.example.geomessenger.utilities.PreferenceManager;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class SearchDiscussionsFragment extends Fragment implements DiscussionListener {

    interface OnItemCheckListener{
        String onCheckTitle();
        Integer onCheckDistance(Double Longitude, Double latitude);
    }

    private PreferenceManager preferenceManager;
    private View view;
    private OnItemCheckListener fragmentItemCheckListener;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);

        try {
            fragmentItemCheckListener = (OnItemCheckListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString()
                    + " должен реализовывать интерфейс OnItemCheckListener");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_search_discussions, container, false);

        preferenceManager = new PreferenceManager(view.getContext());
        getDiscussions();

        return view;
    }

    public void getDiscussions(){
        loading(true);
        FirebaseFirestore database = FirebaseFirestore.getInstance();
        database.collection(Constants.KEY_COLLECTION_DISCUSSION)
                .get()
                .addOnCompleteListener(task -> {
                    loading(false);
                    if(task.isSuccessful() && task.getResult() != null){
                        List<Discussion> discussions = new ArrayList<>();
                        for (QueryDocumentSnapshot queryDocumentSnapshot : task.getResult()){

                            Discussion discussion = new Discussion();
                            discussion.title = queryDocumentSnapshot.getString(Constants.KEY_DISCUSSION_TITLE);

                            if (!discussion.title.startsWith(fragmentItemCheckListener.onCheckTitle()))
                            {continue;}

                            discussion.latitude = queryDocumentSnapshot.getString(Constants.KEY_LATITUDE);
                            discussion.longitude = queryDocumentSnapshot.getString(Constants.KEY_LONGITUDE);
                            if (fragmentItemCheckListener.onCheckDistance(Double.parseDouble(discussion.longitude),
                                    Double.parseDouble(discussion.latitude)) == 0)
                            {continue;}

                            discussion.discussionId = queryDocumentSnapshot.getId();
                            discussion.image = queryDocumentSnapshot.getString(Constants.KEY_DISCUSSION_IMAGE);
                            discussion.locationTitle = queryDocumentSnapshot.getString(Constants.KEY_LOCATION_TITLE);
                            discussion.dateObject = queryDocumentSnapshot.getDate(Constants.KEY_TIMESTAMP);
                            discussions.add(discussion);
                        }
                        if (discussions.size() > 0) {
                            RecentDiscussionAdapter discussionAdapter = new RecentDiscussionAdapter(discussions, this);
                            RecyclerView recyclerView = view.findViewById(R.id.discussionsRecyclerView);
                            recyclerView.setAdapter(discussionAdapter);
                            recyclerView.setVisibility(View.VISIBLE);
                        } else {
                            showErrorMessage();
                        }
                    } else {
                        showErrorMessage();
                    }
                });
    }

    private void showErrorMessage() {
        TextView textErrorMessage = view.findViewById(R.id.textErrorMessage);
        textErrorMessage.setText(String.format("%s", "No discussion available"));
        textErrorMessage.setVisibility(View.VISIBLE);
    }

    private void loading(Boolean isLoading){
        ProgressBar progressBar = view.findViewById(R.id.progressBar);

        if (isLoading){
            progressBar.setVisibility(View.VISIBLE);
        } else {
            progressBar.setVisibility(View.INVISIBLE);
        }
    }

    @Override
    public void onDiscussionClicked(Discussion discussion) {
        Intent intent = new Intent(view.getContext(), DiscussionChatActivity.class);
        intent.putExtra(Constants.KEY_DISCUSSION, discussion);
        startActivity(intent);
    }
}