package com.example.geomessenger.adapters;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.geomessenger.R;
import com.example.geomessenger.listeners.DiscussionListener;
import com.example.geomessenger.models.ChatMessage;
import com.example.geomessenger.models.Discussion;
import com.example.geomessenger.models.User;

import java.util.List;

public class RecentDiscussionAdapter extends RecyclerView.Adapter<RecentDiscussionAdapter.DiscussionViewHolder>{

    private final List<Discussion> discussions;
    private final DiscussionListener discussionListener;

    public RecentDiscussionAdapter(List<Discussion> discussions, DiscussionListener discussionListener) {
        this.discussions = discussions;
        this.discussionListener = discussionListener;
    }

    @NonNull
    @Override
    public DiscussionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_container_discussion, parent, false);
        return new RecentDiscussionAdapter.DiscussionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DiscussionViewHolder holder, int position) {
        holder.setData(discussions.get(position));
    }

    @Override
    public int getItemCount() {
        return discussions.size();
    }

    class DiscussionViewHolder extends RecyclerView.ViewHolder{
        View root;

        DiscussionViewHolder(View view){
            super(view);
            root = view;
        }

        void setData(Discussion discussion){
            TextView textTitle = root.findViewById(R.id.textTitle);
            TextView textLocation = root.findViewById(R.id.textLocation);
            TextView textName = root.findViewById(R.id.textName);
            TextView textMessage = root.findViewById(R.id.textMessage);
            ImageView imageDiscussion = root.findViewById(R.id.imageDiscussion);

            textTitle.setText(discussion.title);
            textLocation.setText(discussion.locationTitle);
            imageDiscussion.setImageBitmap(getConversionImage(discussion.image));

            textName.setText("");
            textMessage.setText("");
            if (discussion.message != null && !discussion.message.equals("")){
                textName.setText(discussion.name);
                textMessage.setText(discussion.message);
            }

            root.setOnClickListener(v -> {
                discussionListener.onDiscussionClicked(discussion);
            });
        }
    }

    public Bitmap getConversionImage(String encodedImage){
        byte[] bytes = Base64.decode(encodedImage, Base64.DEFAULT);
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
    }
}
