package com.example.todoapp;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.example.todoapp.Model.ToDoAppModel;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.Calendar;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CheckDeadlineService extends Service {
    private static final String CHANNEL_ID = "CheckDeadlineServiceChannel";
    private static final String TAG = "CheckDeadlineService";
    private Handler handler;
    private Runnable runnable;
    private ExecutorService executorService1;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();

        executorService1 = Executors.newSingleThreadExecutor();
        handler = new Handler();
        runnable = new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "Eseguendo controllo delle scadenze...");
                executorService1.execute(()->checkDeadlines());
                //handler.postDelayed(this, 6 * 60 * 60 * 1000); // Ripeti ogni 6 ore
                handler.postDelayed(this, 5000); // Ripeti ogni 5 secondi
            }
        };
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        saveServiceState(true);
        Log.d(TAG, "Servizio avviato");
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);


        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Controllo Scadenze")
                .setSmallIcon(R.drawable.baseline_notification_important_24)
                .setContentIntent(pendingIntent)
                .build();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC);
        }

        //  controllo delle scadenze
        handler.post(runnable);

        return START_STICKY;
    }

    private void checkDeadlines() {
        Log.d("ThreadCheck", "Scadenze deadline: " + Thread.currentThread().getName());
        Log.d(TAG, "Controllo delle scadenze in corso...");
        Utility.getUserReference().get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        for (QueryDocumentSnapshot document : task.getResult()) {

                            ToDoAppModel taskModel = document.toObject(ToDoAppModel.class);

                            //Timestamp deadline = document.getTimestamp("dateTime");

                            if(taskModel.getDateTime()!=null) {
                                Timestamp deadline = taskModel.getDateTime();
                                Calendar deadlineCalendar = Calendar.getInstance();
                                deadlineCalendar.setTime(deadline.toDate());
                                Calendar now = Calendar.getInstance();

                                if (Utility.isSameDay(now, deadlineCalendar)) {
                                    sendNotification("Scadenza in Arrivo", "Il Task: " + document.getString("task") + " è in scadenza oggi");
                                }
                            }
                        }
                    }else {
                        Log.e(TAG, "Errore nel recupero delle scadenze", task.getException());
                    }
                });
    }

    private void sendNotification(String title, String message) {
        Log.d(TAG, "Invio notifica: " + title + " - " + message);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.baseline_notification_important_24)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH);

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(2, builder.build());
    }

    private void createNotificationChannel() {
        NotificationChannel serviceChannel = new NotificationChannel(
                CHANNEL_ID,
                "Check Deadline Service Channel",
                NotificationManager.IMPORTANCE_DEFAULT
        );

        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager != null) {
            manager.createNotificationChannel(serviceChannel);
        }
    }


    private void saveServiceState(boolean isRunning) {
        SharedPreferences sharedPreferences = getSharedPreferences("service_state", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean("is_service_running", isRunning);
        editor.apply();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(runnable);
        executorService1.shutdown();
        stopForeground(true);
        saveServiceState(false);
    }
}
