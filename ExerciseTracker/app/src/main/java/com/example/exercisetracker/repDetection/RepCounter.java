package com.example.exercisetracker.repDetection;

import android.content.Context;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.example.exercisetracker.R;
import com.google.mlkit.vision.common.PointF3D;
import com.google.mlkit.vision.pose.Pose;
import com.google.mlkit.vision.pose.PoseLandmark;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * RepCounter class used to detect number of reps from results of mlk tracking
 * Landmarks added as entries to class
 */

public class RepCounter {
    private boolean enteredPose;
    private TextView indicator, debugTV;
    private int reps;
    private Context context;
    //calculations
    private int duration;
    private boolean countedRep;
    private boolean pushedDown;
    private boolean returnedToPosition;
    private Map<Integer, PointF3D> startPoint;
    private Float uncertainty;
    private Float minDistance;

    public int getReps() {
        return reps;
    }


    public RepCounter(Context context, TextView indicator, TextView debug, Float uncertainty, Float minDistance) {
        this.context = context;
        this.uncertainty = uncertainty;
        this.minDistance = minDistance;
        this.debugTV = debug;
        this.indicator = indicator;
        reps = 0;
        enteredPose = false;
        duration = 0;
        pushedDown = false;
        returnedToPosition = false;
        countedRep = false;
        startPoint = new HashMap<>();
    }

    private void reset(){
        countedRep = false;
        duration = 0;
        pushedDown = false;
        returnedToPosition = false;

    }

    public void addEntry(List<PoseLandmark> landmarks) {
        if (!landmarks.isEmpty()) {
            Map<Integer, PointF3D> relevantLandmarks = getRelevantLandmarks(landmarks);
            //Z value decreases as you move close to the phone
            //Using Z value from ML KIT, determining if body is laying down horizontally
            //i.e. Lower body has a LARGER z value than upper body

            enteredPose =
                    relevantLandmarks.get(PoseLandmark.LEFT_HIP).getZ() > relevantLandmarks.get(PoseLandmark.NOSE).getZ()
                            && relevantLandmarks.get(PoseLandmark.LEFT_KNEE).getZ() > relevantLandmarks.get(PoseLandmark.LEFT_HIP).getZ()
                            && relevantLandmarks.get(PoseLandmark.RIGHT_HIP).getZ() > relevantLandmarks.get(PoseLandmark.NOSE).getZ()
                            && relevantLandmarks.get(PoseLandmark.RIGHT_KNEE).getZ() > relevantLandmarks.get(PoseLandmark.RIGHT_HIP).getZ();
            if (enteredPose){
                detectReps(relevantLandmarks);
                debugTV.setText(relevantLandmarks.get(PoseLandmark.NOSE).getY()+"\n Start pos:"+startPoint.get(PoseLandmark.NOSE).getY() + "\nDuration: "+duration);
            }
            else{
                reset();
                debugTV.setText("0\n0\n0");
            }
            updateIndicator();

        }
    }

    private void detectReps(Map<Integer,PointF3D> relevantLandmarks){
        if (duration == 0){
            //first time entering pose
            startPoint = relevantLandmarks;
        }
        //calculation of reps is done in a process:
        //when user first initially enters pose, the position is recorded
        //user has to then push down by at least x amount and return to original position for a rep to be counted
        if (countedRep){
            reset();
        }
        else {
            if (!pushedDown) {
                pushedDown = relevantLandmarks.get(PoseLandmark.NOSE).getY() >= startPoint.get(PoseLandmark.NOSE).getY() + minDistance;
            }
            if (!returnedToPosition) {
                returnedToPosition = relevantLandmarks.get(PoseLandmark.NOSE).getY() < startPoint.get(PoseLandmark.NOSE).getY() - uncertainty
                        && relevantLandmarks.get(PoseLandmark.LEFT_HIP).getY() < startPoint.get(PoseLandmark.LEFT_HIP).getY() - uncertainty;
            }
            if (pushedDown) {
                //checking whether user has pushed down
                if (returnedToPosition) {
                    if (!countedRep) {
                        Toast.makeText(this.context, "Rep Counted", Toast.LENGTH_SHORT).show();
                        reps++;
                        countedRep = true;

                    }
                } else {
                    duration++;
                }
            } else {
                duration++;
            }
        }


    }

    private void updateIndicator() {
        //method to update text view indicator on screen to show if user has entered a push up or not
        if (enteredPose && pushedDown && returnedToPosition) {
            indicator.setTextColor(ContextCompat.getColor(context, R.color.green));
            indicator.setText("Push Up Entered, Pushed Down, Returned");
        } else if (enteredPose && pushedDown ){
            indicator.setTextColor(ContextCompat.getColor(context, R.color.green));
            indicator.setText("Push Up Entered, Pushed Down");
        }
        else if (enteredPose){
            indicator.setTextColor(ContextCompat.getColor(context, R.color.green));
            indicator.setText("Push Up Entered");
        }else {
            indicator.setTextColor(ContextCompat.getColor(context, R.color.red));
            indicator.setText("Push Up Not Entered");
        }
    }


    private Map<Integer, PointF3D> getRelevantLandmarks(List<PoseLandmark> landmarks) {
        //filtering through all landmarks given by ML kit to only relevant ones for push up
        //these are the nose, hip and knee landmarks representing the face, lower and upper body
        Map<Integer, PointF3D> relevantLandmarks = new HashMap<>();
        for (PoseLandmark landmark : landmarks) {
            switch (landmark.getLandmarkType()) {
                case PoseLandmark.NOSE:
                    relevantLandmarks.put(PoseLandmark.NOSE, landmark.getPosition3D());
                    break;
                case PoseLandmark.LEFT_HIP:
                    relevantLandmarks.put(PoseLandmark.LEFT_HIP, landmark.getPosition3D());
                    break;
                case PoseLandmark.RIGHT_HIP:
                    relevantLandmarks.put(PoseLandmark.RIGHT_HIP, landmark.getPosition3D());
                    break;
                case PoseLandmark.LEFT_KNEE:
                    relevantLandmarks.put(PoseLandmark.LEFT_KNEE, landmark.getPosition3D());
                    break;
                case PoseLandmark.RIGHT_KNEE:
                    relevantLandmarks.put(PoseLandmark.RIGHT_KNEE, landmark.getPosition3D());
                    break;
            }
        }
        return relevantLandmarks;
    }
}
