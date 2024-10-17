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
import com.example.geomessenger.models.ChatMessage;

import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder>{

    private final List<ChatMessage> chatMessages;
    private final String senderId;

    public static final int VIEW_TYPE_SENT = 1;
    public static final int VIEW_TYPE_RECEIVED = 2;


    public ChatAdapter(List<ChatMessage> chatMessages, String senderId) {
        this.chatMessages = chatMessages;
        this.senderId = senderId;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if(viewType == VIEW_TYPE_SENT){
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_container_sent_message, parent, false);
            return new SentMessageViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_container_received_message, parent, false);
            return new ReceivedMessageViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if(getItemViewType(position) == VIEW_TYPE_SENT){
            ((SentMessageViewHolder) holder).setData(chatMessages.get(position));
        } else {
            ((ReceivedMessageViewHolder) holder).setData(chatMessages.get(position));
        }
    }

    @Override
    public int getItemCount() {
        return chatMessages.size();
    }

    @Override
    public int getItemViewType(int position) {
        if(chatMessages.get(position).senderId.equals(senderId)){
            return VIEW_TYPE_SENT;
        } else {
            return VIEW_TYPE_RECEIVED;
        }
    }

    static class SentMessageViewHolder extends RecyclerView.ViewHolder {

        View itemContainerSentMessage;

        SentMessageViewHolder(View view){
            super(view);
            itemContainerSentMessage = view;
        }

        void setData(ChatMessage chatMessage){
            TextView textMessage = itemContainerSentMessage.findViewById(R.id.textMessage);
            TextView textDateTime = itemContainerSentMessage.findViewById(R.id.textDateTime);

            textMessage.setText(chatMessage.message);
            textDateTime.setText(chatMessage.dateTime);
        }
    }

    class ReceivedMessageViewHolder extends RecyclerView.ViewHolder {

        View itemContainerReceivedMessage;

        ReceivedMessageViewHolder(View view){
            super(view);
            itemContainerReceivedMessage = view;
        }

        void setData(ChatMessage chatMessage){
            TextView textMessage = itemContainerReceivedMessage.findViewById(R.id.textMessage);
            TextView textDateTime = itemContainerReceivedMessage.findViewById(R.id.textDateTime);
            ImageView imageProfile = itemContainerReceivedMessage.findViewById(R.id.imageProfile);

            textMessage.setText(chatMessage.message);
            textDateTime.setText(chatMessage.dateTime);
            if (chatMessage.conversionImage != null) {
                imageProfile.setImageBitmap(getConversionImage(chatMessage.conversionImage));
            }
        }
    }

    public Bitmap getConversionImage(String encodedImage){
        byte[] bytes = Base64.decode(encodedImage, Base64.DEFAULT);
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
    }
}
