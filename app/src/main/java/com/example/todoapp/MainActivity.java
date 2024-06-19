package com.example.todoapp;

import android.Manifest;
import android.app.Activity;
import android.content.ClipData;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import androidx.appcompat.widget.SearchView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;


import com.example.todoapp.Adapter.ToDoAppAdapter;
import com.example.todoapp.Model.ToDoAppModel;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class MainActivity extends AppCompatActivity {
    private static final int NOTIFICATIONS_PERMISSION_CODE = 100;
    private ActivityResultLauncher<Intent> createFileLauncher;
    private FirebaseAuth auth;
    private FirebaseUser currentUser;
    private ImageButton menuButton;
    private TextView nomeUtente;
    private RecyclerView recyclerView;
    private SearchView searchView;

    private FloatingActionButton addButton;
    private ToDoAppAdapter adapter;

    private List<ToDoAppModel> taskList = new ArrayList<>();
    private ExecutorService executorService = null;



    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);



        auth = FirebaseAuth.getInstance();

        menuButton = findViewById(R.id.menuButton);
        nomeUtente = findViewById(R.id.nomeUtente);

        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        setupRecyclerView();

        searchView = findViewById(R.id.searchView);
        searchView.clearFocus();
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                filterList(query);
                return false;
            }

            @Override
            public boolean onQueryTextChange(String Text) {
                filterList(Text);
                return false;
            }
        });

        addButton = findViewById(R.id.addButton);


        addButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(MainActivity.this, AddTask.class));
            }
        });

        menuButton.setOnClickListener((v)->showMenu());


        createFileLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        Intent data = result.getData();
                        if (data != null) {
                            Uri uri = data.getData();
                            writePdfToUri(uri);
                        }
                    }
                }
        );

    }

    private void filterList(String text) {
        Query query = Utility.getUserReference().orderBy("task").startAt(text).endAt(text+"\uf8ff");
        FirestoreRecyclerOptions<ToDoAppModel> options = new FirestoreRecyclerOptions.Builder<ToDoAppModel>()
                .setQuery(query, ToDoAppModel.class).build();

        adapter.updateOptions(options);
        adapter.notifyDataSetChanged();

    }

    void showMenu()
    {
        PopupMenu menu = new PopupMenu(MainActivity.this, menuButton);
        menu.getMenu().add("Logout");
        menu.getMenu().add("Condividi Task");
        menu.getMenu().add("Avviso scadenza");
        menu.show();
        menu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                if(menuItem.getTitle()=="Logout")
                {
                    auth.signOut();
                    startActivity(new Intent(MainActivity.this, LoginActivity.class));
                    finish();
                }else if(menuItem.getTitle()=="Condividi Task")
                {
                    Log.d("ThreadCheck", "Thread Menu: " + Thread.currentThread().getName());
                    executorService = Executors.newSingleThreadExecutor();
                    Future<Void> future = executorService.submit(new Callable<Void>() {
                        @Override
                        public Void call() throws Exception {
                            createListTask();
                            return null;
                        }
                    });
                    //createListTask();
                }else if(menuItem.getTitle()=="Avviso scadenza"){
                    SharedPreferences sharedPreferences = getSharedPreferences("service_state", Context.MODE_PRIVATE);
                    boolean isServiceRunning = sharedPreferences.getBoolean("is_service_running", false);
                    if(isServiceRunning){
                        Intent serviceIntent = new Intent(MainActivity.this, CheckDeadlineService.class);
                        stopService(serviceIntent);
                        Toast.makeText(MainActivity.this, "Notifiche Disattivate", Toast.LENGTH_SHORT).show();
                    }else{
                        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, NOTIFICATIONS_PERMISSION_CODE);
                        } else {
                            startServiceDeadline();
                        }

                    }

                }
                return false;
            }
        });
    }

    private void startServiceDeadline(){
        Intent serviceIntent = new Intent(MainActivity.this, CheckDeadlineService.class);
        ContextCompat.startForegroundService(MainActivity.this, serviceIntent);
        Toast.makeText(MainActivity.this, "Notifiche Attivate", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == NOTIFICATIONS_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startServiceDeadline();
            } else {
                Toast.makeText(this, "Notifications permission is required.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    void createListTask()
    {
        Log.d("ThreadCheck", "Thread Cerate List: " + Thread.currentThread().getName());
        taskList.clear();
        Query query = Utility.getUserReference();
        query.get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if (task.isSuccessful()) {
                    for (QueryDocumentSnapshot document : task.getResult()) {
                        ToDoAppModel taskModel = document.toObject(ToDoAppModel.class);
                        taskList.add(taskModel);
                    }
                    createFile();

                } else {
                    Log.w("Firestore", "Errore nel recuperare i documenti.", task.getException());
                }
            }
        });

    }

    void createFile ()
    {
        Log.d("ThreadCheck", "Create File " + Thread.currentThread().getName());
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/pdf");
        intent.putExtra(Intent.EXTRA_TITLE, "TaskList.pdf");
        createFileLauncher.launch(intent);
    }

    private void writePdfToUri(final Uri uri) {
        executorService.submit(()->{
            Log.d("ThreadCheck", "ThreadWrite Pdf: " + Thread.currentThread().getName());
            PdfDocument pdfDocument = Utility.convertListToPdf(taskList, this);
            try {
                ParcelFileDescriptor pfd = getContentResolver().openFileDescriptor(uri, "w");
                if (pfd != null) {
                    FileOutputStream fileOutputStream = new FileOutputStream(pfd.getFileDescriptor());
                    // Scrivi il contenuto del PDF qui
                    pdfDocument.writeTo(fileOutputStream);
                    pfd.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
                runOnUiThread(()->{
                    Toast.makeText(this, "Errore durante la scrittura del PDF", Toast.LENGTH_SHORT).show();
                });
            }finally {
                executorService.shutdown();
            }
        });

    }



    void setupRecyclerView()
    {
        Query query = Utility.getUserReference();
        FirestoreRecyclerOptions<ToDoAppModel> options = new FirestoreRecyclerOptions.Builder<ToDoAppModel>()
                .setQuery(query, ToDoAppModel.class).build();
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ToDoAppAdapter(options, this);
        recyclerView.setAdapter(adapter);
    }



    @Override
    protected void onStart() {
        super.onStart();

        currentUser = auth.getCurrentUser();
        if (currentUser == null)
        {
            // Utente non loggato, reindirizza alla LoginActivity
            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
        }
        else
        {
            // Utente loggato, puoi accedere alle informazioni dell'utente
            String userEmail = currentUser.getEmail();
            String uid = currentUser.getUid();
            nomeUtente.setText(userEmail);
            adapter.startListening();
            // Mostra o utilizza le informazioni dell'utente loggato
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        adapter.stopListening();
    }

    @Override
    protected void onResume() {
        super.onResume();
        adapter.notifyDataSetChanged();
    }
}
