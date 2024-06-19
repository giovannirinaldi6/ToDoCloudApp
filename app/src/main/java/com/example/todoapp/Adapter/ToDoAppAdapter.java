package com.example.todoapp.Adapter;


import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;


import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.todoapp.AddTask;

import com.example.todoapp.Model.ToDoAppModel;
import com.example.todoapp.R;
import com.example.todoapp.Utility;
import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.Timestamp;

import java.util.Date;

public class ToDoAppAdapter extends FirestoreRecyclerAdapter<ToDoAppModel, ToDoAppAdapter.TaskViewHolder> {

    private Context context;
    private String docId;
    public ToDoAppAdapter(@NonNull FirestoreRecyclerOptions<ToDoAppModel> options, Context context) {
        super(options);
        this.context = context;
    }

    static void updateCheckBoxStatus(boolean isChecked, String Id)
    {
        Utility.getUserReference().document(Id).update("status", isChecked);
        long timestamp = System.currentTimeMillis();
        if(isChecked) {
            Timestamp timestampCompleted = new Timestamp(new Date(timestamp));
            Utility.getUserReference().document(Id).update("completedDateTime", timestampCompleted);
        }
    }

    @Override
    protected void onBindViewHolder(@NonNull TaskViewHolder holder, int position, @NonNull ToDoAppModel task) {
        //holder.cktask.setText(task.getTask());
        holder.cktask.setChecked(task.getStatus());
        holder.taskText.setText(task.getTask());
        holder.planDateTime.setText("Pianificato il \n"+Utility.convertTimestampReadble(task.getDateTime()));
        holder.completedDataTimeTv.setText("Completato il \n"+Utility.convertTimestampReadble(task.getCompletedDateTime()));


        holder.itemView.setOnClickListener((v)->{
            Intent intent = new Intent(context, AddTask.class);
            intent.putExtra("title", task.getTask());
            docId = this.getSnapshots().getSnapshot(position).getId();
            intent.putExtra("ID", docId);
            intent.putExtra("imageUrl", task.getImageUrl());
            if(task.getDateTime()!=null){
                intent.putExtra("dateTimeTimestamp", task.getDateTime().toDate().getTime());
            }
            intent.putExtra("audioUrl", task.getAudioUrl());
            context.startActivity(intent);
        });

        holder.cktask.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                int currentPosition = holder.getBindingAdapterPosition();
                docId = getSnapshots().getSnapshot(currentPosition).getId();
                DocumentReference document = Utility.getUserReference().document(docId);
                updateCheckBoxStatus(b, docId);
            }
        });

    }

    @NonNull
    @Override
    public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.activity_task, parent, false);
        return new TaskViewHolder(itemView);
    }

    class TaskViewHolder extends RecyclerView.ViewHolder{

        CheckBox cktask;
        TextView completedDataTimeTv, planDateTime, taskText;

        public TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            cktask = itemView.findViewById(R.id.checkBox);
            completedDataTimeTv = itemView.findViewById(R.id.completedDateTime);
            planDateTime = itemView.findViewById(R.id.planDateTime);
            taskText = itemView.findViewById(R.id.taskText);
        }
    }

}
