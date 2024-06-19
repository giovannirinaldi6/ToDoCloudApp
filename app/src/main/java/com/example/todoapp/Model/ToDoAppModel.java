package com.example.todoapp.Model;

import com.google.firebase.Timestamp;


public class ToDoAppModel {
    private boolean status;
    private String task, imageUrl, audioUrl;

    private Timestamp dateTime;

    private Timestamp completedDateTime;



    public ToDoAppModel(){}

    public Timestamp getCompletedDateTime() {
        return completedDateTime;
    }

    public void setCompletedDateTime(Timestamp completedDateTime) {
        this.completedDateTime = completedDateTime;
    }

    public Timestamp getDateTime() {
        return dateTime;
    }

    public void setDateTime(Timestamp dateTime) {
        this.dateTime = dateTime;
    }
    public boolean getStatus() {
        return status;
    }

    public void setStatus(boolean status) {
        this.status = status;
    }

    public String getTask() {
        return task;
    }

    public void setTask(String task) {
        this.task = task;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getAudioUrl() {
        return audioUrl;
    }

    public void setAudioUrl(String audioUrl) {
        this.audioUrl = audioUrl;
    }
}
