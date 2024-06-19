package com.example.todoapp;

import androidx.activity.result.ActivityResultLauncher;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;

import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;

import android.provider.MediaStore;


import android.util.Log;
import android.view.View;
import android.widget.Button;

import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;


import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnCompleteListener;

import com.google.android.gms.tasks.Task;
import com.example.todoapp.Model.ToDoAppModel;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;


import com.google.firebase.Timestamp;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.Calendar;
import java.util.List;


public class AddTask extends AppCompatActivity {
    private static final int CAMERA_PERMISSION_CODE = 100;
    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;


    private ActivityResultLauncher<Intent> cameraLauncher;

    private EditText taskText;
    private Button saveButton, playButton, recordButton;
    private TextView titlePage, deleteTask, selectDate;
    private ImageView imageView;
    private String title, Id, imageUrl, audioUrl;
    private boolean isEdit = false;
    private Bitmap imageBitmap;
    private Timestamp timestamp;

    private MediaRecorder recorder = null;
    private String fileNameAudio = null;
    private Uri fileAudio = null;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_task);

        taskText = findViewById(R.id.taskText);
        saveButton = findViewById(R.id.saveTask);
        titlePage = findViewById(R.id.titlePage);
        deleteTask = findViewById(R.id.deleteTask);
        imageView = findViewById(R.id.imageView);
        selectDate = findViewById(R.id.dateSelect);
        recordButton = findViewById(R.id.recordButton);
        playButton = findViewById(R.id.playButton);
        recordButton.setBackgroundColor(getResources().getColor(R.color.green));
        playButton.setBackgroundColor(getResources().getColor(R.color.green));
        playButton.setEnabled(false);


        Intent intent = getIntent();
        title = intent.getStringExtra("title");
        Id = intent.getStringExtra("ID");
        imageUrl = intent.getStringExtra("imageUrl");
        audioUrl = intent.getStringExtra("audioUrl");
        long millis = intent.getLongExtra("dateTimeTimestamp", -1);


        if(title!=null && Id!=null) {
            isEdit = true;
        }

        if(isEdit){
            titlePage.setText("Modifica Task");
            deleteTask.setVisibility(View.VISIBLE);

            taskText.setText(title);

            if(millis!=-1){
               timestamp = new Timestamp(new Date(millis));
               selectDate.setText(Utility.convertTimestampReadble(timestamp));
            }

            displayImageFromFirestore();

            fileNameAudio = audioUrl;
            if(audioUrl!=null){
                playButton.setEnabled(true);
            }

        }




        saveButton.setOnClickListener((v)->startSave());
        deleteTask.setOnClickListener((v)->deleteTaskFire());
        imageView.setOnClickListener((v)->askCameraPermission());
        selectDate.setOnClickListener((v)->openDialogDateSelect());


        cameraLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Bundle extras = result.getData().getExtras();
                        imageBitmap = (Bitmap) extras.get("data");
                        imageView.setImageBitmap(imageBitmap);
                    }
                }
        );



        recordButton.setOnClickListener((v)->askMicPermission());

        playButton.setOnClickListener((v)->{
            if(fileNameAudio!=null) {
                MediaPlayer mediaPlayer = new MediaPlayer();
                try {
                    mediaPlayer.setDataSource(fileNameAudio);
                    mediaPlayer.prepare();
                    mediaPlayer.start();
                } catch (IOException e) {
                    Toast.makeText(this, "Audio prepare failed!", Toast.LENGTH_SHORT).show();
                    Log.e("AudioActivity", "prepare() failed");
                }

            }
        });


    }


    private void askMicPermission(){
        if ((ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) ||
                (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) ||
                (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)){
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO, android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_RECORD_AUDIO_PERMISSION);
        }
        else{
            recording();
        }
    }

    private void recording() {
        if (recorder == null) {
            startRecordin();
            recordButton.setText("Stop");
            recordButton.setBackgroundColor(getResources().getColor(R.color.red));
        } else {
            stopRecordin();
            recordButton.setText("Record");
            recordButton.setBackgroundColor(getResources().getColor(R.color.green));
            playButton.setEnabled(true);
        }

    }

    private void stopRecordin() {
        recorder.stop();
        recorder.release();
        recorder = null;

        fileAudio = Uri.fromFile(new File(fileNameAudio));
    }

    private void startRecordin() {
        fileNameAudio = getExternalCacheDir().getAbsolutePath()+"/audiorecordtest.3gp";
        recorder = new MediaRecorder();
        recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        recorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        recorder.setOutputFile(fileNameAudio);
        recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);

        try {
            recorder.prepare();
        } catch (IOException e) {
            Toast.makeText(this, "Audio prepare failed!", Toast.LENGTH_SHORT).show();
            Log.w("audio fail", e.toString());
        }

        recorder.start();
    }


    private void openDialogDateSelect(){
        DateTimePicker.showDateTimePicker(this, new DateTimePicker.DateTimePickerCallback() {
            @Override
            public void onDateTimeSelected(Calendar dateTime) {
                Date date = dateTime.getTime();
                timestamp = new Timestamp(date);
                selectDate.setText(Utility.convertTimestampReadble(timestamp));
            }
        });
    }

    private void displayImageFromFirestore(){
        if(imageUrl!=null){
            Glide.with(this).load(imageUrl).into(imageView);
        }else{
            imageView.setImageResource(R.mipmap.ic_launcher);
        }
    }

    private void askCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.CAMERA}, CAMERA_PERMISSION_CODE);
        } else {
            openCamera();
        }
    }


    private void openCamera() {
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        cameraLauncher.launch(cameraIntent);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openCamera();
            } else {
                Toast.makeText(this, "Camera permission is required to use camera.", Toast.LENGTH_SHORT).show();
            }
        }else if(requestCode==REQUEST_RECORD_AUDIO_PERMISSION){
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                recording();
            }else {
                Toast.makeText(this, "Mic permission is required to use mic.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    void startSave()
    {
        String text = taskText.getText().toString();

        ToDoAppModel uploadTask = new ToDoAppModel();
        uploadTask.setStatus(false);
        uploadTask.setCompletedDateTime(null);
        uploadTask.setTask(text);
        uploadTask.setDateTime(timestamp);



        if(imageBitmap!=null || fileAudio!=null){
            uploadOnStorage(uploadTask);
            //uploadImageOnStorage(uploadTask);
        }else{
            saveToFirebase(uploadTask);
        }




    }


    void deleteTaskFire()
    {
        FirebaseStorage storage = FirebaseStorage.getInstance();


        DocumentReference document;
        document = Utility.getUserReference().document(Id);

        document.delete().addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if(task.isSuccessful()){
                    Toast.makeText(AddTask.this, "Eliminato", Toast.LENGTH_SHORT).show();
                    finish();
                }else {
                    Toast.makeText(AddTask.this, "Errore nella rimozione", Toast.LENGTH_SHORT).show();
                }
            }
        });

        if(imageUrl!=null){
            StorageReference imageRef = storage.getReferenceFromUrl(imageUrl);
            imageRef.delete(); //aggiungere eccezioni.
        }

        if(audioUrl!=null){
            StorageReference audioRef = storage.getReferenceFromUrl(audioUrl);
            audioRef.delete();
        }




    }

    void saveToFirebase(ToDoAppModel task)
    {
        DocumentReference document;
        if(isEdit){
            task.setImageUrl(imageUrl);
            task.setAudioUrl(audioUrl);
            document = Utility.getUserReference().document(Id);
        }else {
            //task.setStatus(false);
            document = Utility.getUserReference().document();
        }

        document.set(task).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if(task.isSuccessful()){
                    Toast.makeText(AddTask.this, "Salvataggio Eseguito", Toast.LENGTH_SHORT).show();
                    finish();
                }else {
                    Toast.makeText(AddTask.this, "Errore nel salvataggio", Toast.LENGTH_SHORT).show();
                }
            }
        });

    }


    private void uploadOnStorage(ToDoAppModel task){
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        List<Task<Uri>> urlTask = new ArrayList<>();

        if(fileAudio!=null){
            StorageReference srAudio = FirebaseStorage.getInstance().getReference().child(currentUser.getUid()+"/"+ LocalDateTime.now()+"audioTask.3gp");
            UploadTask uTaskAudio =  srAudio.putFile(fileAudio);
            urlTask.add(uTaskAudio.continueWithTask(t ->{
                if (!t.isSuccessful()) {
                    throw t.getException();
                }
                return srAudio.getDownloadUrl();
            }));
        }

        if(imageBitmap!=null){
            StorageReference srImage = FirebaseStorage.getInstance().getReference().child(currentUser.getUid()+"/"+ LocalDateTime.now()+"imageTask.jpg");
            UploadTask uTaskImage = srImage.putBytes(Utility.getBitmapData(imageBitmap));
            urlTask.add(uTaskImage.continueWithTask(t ->{
                if (!t.isSuccessful()) {
                    throw t.getException();
                }
                return srImage.getDownloadUrl();
            }));
        }

        Tasks.whenAllComplete(urlTask).addOnSuccessListener(t -> {
            if (fileAudio != null) {
                audioUrl = ((Task<Uri>) t.get(0)).isSuccessful() ? ((Task<Uri>) t.get(0)).getResult().toString() : null;
            }

            if (imageBitmap != null) {
                imageUrl = ((Task<Uri>) t.get(fileAudio != null ? 1 : 0)).isSuccessful() ? ((Task<Uri>) t.get(fileAudio != null ? 1 : 0)).getResult().toString() : null;
            }

            task.setAudioUrl(audioUrl);
            task.setImageUrl(imageUrl);
            saveToFirebase(task);

        }).addOnFailureListener(e->{
            Toast.makeText(AddTask.this, "File Upload Error!!!", Toast.LENGTH_SHORT).show();
        });


    }

}