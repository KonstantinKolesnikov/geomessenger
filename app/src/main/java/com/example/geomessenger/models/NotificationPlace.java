package com.example.geomessenger.models;

import java.io.Serializable;
import java.util.Date;

public class NotificationPlace implements Serializable {
    public String userId, address, longitude, latitude, radius;
    public Date dateObject;
}
