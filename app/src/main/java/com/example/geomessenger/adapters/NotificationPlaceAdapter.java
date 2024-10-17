package com.example.geomessenger.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.geomessenger.R;
import com.example.geomessenger.listeners.NotificationPlaceListener;
import com.example.geomessenger.models.NotificationPlace;

import java.util.List;

public class NotificationPlaceAdapter extends RecyclerView.Adapter<NotificationPlaceAdapter.NotificationPlaceViewHolder>{

    private final List<NotificationPlace> places;
    private final NotificationPlaceListener notificationPlaceListener;

    public NotificationPlaceAdapter(List<NotificationPlace> places, NotificationPlaceListener notificationPlaceListener) {
        this.places = places;
        this.notificationPlaceListener = notificationPlaceListener;
    }

    @NonNull
    @Override
    public NotificationPlaceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_container_notification_place, parent, false);
        return new NotificationPlaceAdapter.NotificationPlaceViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NotificationPlaceViewHolder holder, int position) {
        holder.setData(places.get(position));
    }

    @Override
    public int getItemCount() {
        return places.size();
    }

    class NotificationPlaceViewHolder extends RecyclerView.ViewHolder {
        View root;

        NotificationPlaceViewHolder(View view){
            super(view);
            root = view;
        }

        void setData(NotificationPlace place){
            TextView textAddress = root.findViewById(R.id.textAddress);
            TextView textRadius = root.findViewById(R.id.textRadius);

            textAddress.setText(place.address);
            textRadius.setText("Радиус: " + place.radius.toString());

            root.setOnClickListener(v -> {
                notificationPlaceListener.onNotificationPlaceClicked(place);
            });
        }
    }
}
