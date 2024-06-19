package com.example.todoapp;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;

import com.example.todoapp.Model.ToDoAppModel;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import com.bumptech.glide.Glide;

public class Utility {
    public static CollectionReference getUserReference()
    {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        return FirebaseFirestore.getInstance().collection("tasks")
                .document(currentUser.getUid())
                .collection("my_tasks");
    }

    public static byte[] getBitmapData(Bitmap bitmap) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        return baos.toByteArray();
    }


    public static PdfDocument convertListToPdf(List<ToDoAppModel> taskList, Context context)
    {
        Log.d("ThreadCheck", "Thread Utility: " + Thread.currentThread().getName());

        // Definisci le dimensioni della pagina A4
        final int PAGE_WIDTH = 595;
        final int PAGE_HEIGHT = 842;
        final int MARGIN = 10;

        // Crea un nuovo documento PDF
        PdfDocument pdfDocument = new PdfDocument();

        // Inizia una nuova pagina
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create();
        PdfDocument.Page page = pdfDocument.startPage(pageInfo);

        // Disegna sulla pagina
        Canvas canvas = page.getCanvas();
        Paint paint = new Paint();
        paint.setTextSize(12);

        int x = MARGIN;
        int y = MARGIN + 15; // Partenza iniziale

        for (ToDoAppModel task : taskList) {
            // Verifica se c'è abbastanza spazio per il testo e l'immagine
            if (y + 320 > PAGE_HEIGHT - MARGIN) {
                // Finisci la pagina corrente e inizia una nuova pagina
                pdfDocument.finishPage(page);
                pageInfo = new PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pdfDocument.getPages().size() + 1).create();
                page = pdfDocument.startPage(pageInfo);
                canvas = page.getCanvas();
                y = MARGIN + 15; // Reimposta la coordinata Y per la nuova pagina
            }

            // Disegna il testo del task
            canvas.drawText(task.getTask() + " - " + (task.getStatus() ? "Completato" : "Non completato")
                    + " - Pianificato il " + convertTimestampReadble(task.getDateTime()) +
                    (task.getStatus() ? " - Completato il " + convertTimestampReadble(task.getCompletedDateTime()) : ""), x, y, paint);

            y += 20; // Aggiorna la coordinata Y per l'immagine

            // Disegna l'immagine se presente
            if (task.getImageUrl() != null) {
                try {
                    Bitmap bitmapImage = Glide.with(context).asBitmap().load(task.getImageUrl()).submit().get();
                    canvas.drawBitmap(bitmapImage, x, y, paint);
                    y += bitmapImage.getHeight() + 20; // Aggiorna la coordinata Y dopo l'immagine
                } catch (Exception e) {
                    canvas.drawText("Immagine presente, Errore nel download dell'immagine", x, y, paint);
                    y += 40; // Aggiorna la coordinata Y dopo l'errore di disegno dell'immagine
                }
            } else {
                y += 20; // Aggiungi spazio extra se non c'è immagine
            }

            y += 20; // Aggiungi spazio extra tra i task
        }

        // Finisci l'ultima pagina
        pdfDocument.finishPage(page);

        return pdfDocument;
    }

    public static String convertTimestampReadble(Timestamp timestamp) {
        if (timestamp != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
            return sdf.format(timestamp.toDate());
        } else {
            return "Data non disponibile";
        }
    }

    public static boolean isSameDay(Calendar cal1, Calendar cal2) {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.MONTH) == cal2.get(Calendar.MONTH) &&
                cal1.get(Calendar.DAY_OF_MONTH) == cal2.get(Calendar.DAY_OF_MONTH);
    }

    public static boolean isNetworkAvailable(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        if (connectivityManager != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                NetworkCapabilities networkCapabilities = connectivityManager.getNetworkCapabilities(connectivityManager.getActiveNetwork());
                if (networkCapabilities != null) {
                    return networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
                }
            } else {
                NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
                return activeNetworkInfo != null && activeNetworkInfo.isConnected();
            }
        }

        return false;
    }


}
