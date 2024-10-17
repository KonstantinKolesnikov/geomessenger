package com.example.geomessenger.models;

import java.io.Serializable;
import java.util.Date;

public class Discussion implements Serializable {
    public String discussionId, title, locationTitle, image, message, name, longitude, latitude;
    public Date dateObject;
}
