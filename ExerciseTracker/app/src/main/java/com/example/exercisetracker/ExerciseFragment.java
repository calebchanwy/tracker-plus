package com.example.exercisetracker;

import static com.example.exercisetracker.BaseApp.CHANNEL_1_ID;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.fragment.app.Fragment;

public class ExerciseFragment extends Fragment implements View.OnClickListener, ActivityCompat.OnRequestPermissionsResultCallback{
    private String[] PERMISSIONS;
    //notification
    private NotificationManagerCompat notificationManagerCompat;


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view =  inflater.inflate(R.layout.fragment_exercise,container,false);
        Button runBtn = (Button) view.findViewById(R.id.runBtn);
        Button walkBtn = (Button) view.findViewById(R.id.walkBtn);
        Button treadmillBtn = (Button) view.findViewById(R.id.treadmillBtn);
        Button pushUpBtn = (Button) view.findViewById(R.id.pushUpBtn);
        treadmillBtn.setOnClickListener(this);
        pushUpBtn.setOnClickListener(this);
        runBtn.setOnClickListener(this);
        walkBtn.setOnClickListener(this);
        return view;
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public void onClick(View v) {
        switch(v.getId()){
            case R.id.runBtn:
                //starting activity
                Intent intent1 = new Intent(getContext(), RunningActivity.class);
                startActivity(intent1);
                break;
            case R.id.walkBtn:
                //starting activity
                Intent intent2 = new Intent(getContext(), WalkingActivity.class);
                startActivity(intent2);
                break;
            case R.id.treadmillBtn:
                //starting activity
                Intent intent3 = new Intent(getContext(), TreadmillActivity.class);
                startActivity(intent3);
                break;
            case R.id.pushUpBtn:
                //starting activity
                Intent intent4 = new Intent(getContext(), PushUpActivity.class);
                startActivity(intent4);
                break;
        }
    }


}
