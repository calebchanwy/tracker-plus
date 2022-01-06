package com.example.exercisetracker.activities;

import static com.example.exercisetracker.other.BaseApp.CHANNEL_1_ID;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Notification;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.text.Html;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.example.exercisetracker.R;
import com.example.exercisetracker.other.Detector;
import com.example.exercisetracker.other.ExerciseService;
import com.example.exercisetracker.other.Filter;
import com.example.exercisetracker.other.Route;
import com.example.exercisetracker.other.User;
import com.example.exercisetracker.other.dbhelper;
import com.google.android.material.button.MaterialButton;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.Array;
import java.sql.Date;
import java.sql.Timestamp;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Locale;

public class RunningActivity extends AppCompatActivity{
    //Sensors
    private SensorManager sensorManager;
    private SensorEventListener listener;
    private LocationManager locationManager;
    private LocationListener locationListener;
    //TextViews
    private TextView timerText;
    private TextView stepText;
    private TextView calorieText;
    private TextView distText;
    private TextView paceText;
    //Buttons
    private MaterialButton finishBtn;
    private MaterialButton startStopBtn;
    //notification
    private NotificationManagerCompat notificationManagerCompat;

    //Specialised running variables
    private float MET;
    private double distance;
    private Filter filter;
    private Detector detector;
    private Boolean isRunning;
    private Integer seconds;
    private Integer steps;
    private Integer calories;
    private Float[] filtered_data;
    private Boolean hasProcessed;
    private Route route;
    private String timeStarted;
    private Date date;

    private ExerciseService customService = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //visuals
        getSupportActionBar().hide();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);//  set status text dark
        }
        getWindow().setNavigationBarColor(getResources().getColor(R.color.main_colour));
        getWindow().setStatusBarColor(getResources().getColor(R.color.main_colour));
        setContentView(R.layout.activity_running);
        init();
        handlePermissions();
    }

    private void init(){
        //instantiating all private variables
        long millis = System.currentTimeMillis();
        Timestamp timestamp = new Timestamp(millis);
        timeStarted = timestamp.toString().substring(11, 16);
        date = new Date(millis);
        isRunning = true;
        seconds = 0;
        steps = 0;
        distance = 0f;
        timerText = findViewById(R.id.timerText);
        stepText = findViewById(R.id.stepText);
        distText = findViewById(R.id.distText);
        paceText = findViewById(R.id.paceText);
        calorieText = findViewById(R.id.calText);
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        MET = Float.parseFloat(getString(R.string.met_running));
        startStopBtn = findViewById(R.id.startStopBtn);
        finishBtn = findViewById(R.id.finishBtn);

        //CUSTOM JAVA CLASSES
        filter = new Filter(-10f, 10f);
        detector = new Detector(0.5f, 2);
        ArrayList<Double[]> currentRoute = new ArrayList<>();
        route = new Route(currentRoute);

        //foreground services
        Intent intent = new Intent(this, ExerciseService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent);
        }else{
            startService(intent);
        }
    }
    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            customService = ExerciseService.getInstance();
            // now you have the instance of service.
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            customService = null;
        }
    };

    @SuppressLint("MissingPermission")
    private void startRunning() {
        //NOTIFICATION MANAGER
        notificationManagerCompat = NotificationManagerCompat.from(this);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            //requesting background permission for android q+
            //Android forces you to request this separately
            isRunning = false;
            requestPermissions(new String[]{Manifest.permission.ACCESS_BACKGROUND_LOCATION}, 1);
        }
        //click listeners
        finishBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finishRunning();
            }
        });
        startStopBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isRunning) {
                    startStopBtn.setText("Resume");
                    isRunning = false;

                } else {
                    startStopBtn.setText("Pause");
                    isRunning = true;
                }
            }
        });

        //handling location changes
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(@NonNull Location location) {
                if (isRunning) {
                    //location in form of latitude and longitude
                    double latitude = location.getLatitude();
                    double longitude = location.getLongitude();
                    Double[] entry = {latitude, longitude};
                    System.out.println(entry);
                    route.addRoute(entry);
                }
            }

        };
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 3, locationListener);
        createTimer();

        //2d arrays to store a variable amount of samples, each sample consisting of the x y z values
        ArrayList<Float[]> accel = new ArrayList<Float[]>();
        ArrayList<Float[]> grav = new ArrayList<Float[]>();
        listener = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent event) {
                Sensor sensor = event.sensor;
                //formatting by rounding to 2 decimal places
                DecimalFormat df = new DecimalFormat("#.##");
                if (sensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION & isRunning) {
                    //handling the linear acceleration
                    Float[] entry = convertToEntry(event.values[0], event.values[1], event.values[2], df);
                    System.out.println("acceleration:" + String.format("%f, %f, %f", entry[0], entry[1], entry[2]));
                    accel.add(entry);
                }
                if (sensor.getType() == Sensor.TYPE_GRAVITY & isRunning) {
                    //handling gravimeter
                    Float[] entry = convertToEntry(event.values[0], event.values[1], event.values[2], df);
                    grav.add(entry);
                    System.out.println("gravity: " + String.format("%f, %f, %f", entry[0], entry[1], entry[2]));
                }


                if (((seconds % 5) == 0 && (grav.size() > 0)) && (accel.size() > 0) && (hasProcessed == Boolean.FALSE)) {
                    //PROCESSING DATA
                    ArrayList<Float> results= processData(accel, grav, df);
                    grav.clear();
                    accel.clear();
                    //hasProcessed to true to prevent small chunks of data being processed
                    hasProcessed = Boolean.TRUE;

                    //FILTERING DATA
                    filter.filter(results);
                    filtered_data = filter.getFiltered_data();

                    //detecting steps
                    detector.detect(filtered_data);
                    steps = detector.getStepCount();

                }
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {
            }
        };
        sensorManager.registerListener(listener, sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION), SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(listener, sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY), SensorManager.SENSOR_DELAY_NORMAL);

    }

    private Float[] convertToEntry(Float raw_x, Float raw_y, Float raw_z, DecimalFormat df) {
        Float x = Float.parseFloat(df.format(raw_x));
        Float y = Float.parseFloat(df.format(raw_y));
        Float z = Float.parseFloat(df.format(raw_z));
        Float[] entry = new Float[3];
        entry[0] = x;
        entry[1] = y;
        entry[2] = z;
        return entry;
    }

    private ArrayList<Float> processData(ArrayList<Float[]> accel, ArrayList<Float[]> grav, DecimalFormat df){
        ArrayList<Float> results = new ArrayList<Float>();
        //PRE-PROCESSING DATA
        //handling when grav array and accel array are unequal:
        while (accel.size() != grav.size()) {
            if (accel.size() > grav.size()) {
                accel.remove(accel.size() - 1);

            } else {
                grav.remove(grav.size() - 1);
            }
        }

        System.out.println("Seconds: " + seconds);
        //PERFORM DOT PRODUCT
        System.out.println(String.format("gravsize: %d accelsize: %d", grav.size(), accel.size()));
        for (int j = 0; j < grav.size(); j++) {
            Float[] accelValues = accel.get(j);
            Float[] gravValues = grav.get(j);
            Float result = Float.parseFloat(df.format(gravValues[0] * accelValues[0] + gravValues[1] * accelValues[1] + gravValues[2] * accelValues[2]));
            result = result / 9.81f;
            results.add(result);
            System.out.println("result: " + j + " " + result.toString());
        }
        return results;
    }


    //saving to csv file for debugging
    private void saveToStorage(Float[] filtered_data) {
        for (int i = 0; i < filtered_data.length; i++) {
            String entry = filtered_data[i].toString() + "\n";
            System.out.print(entry);
            try {
                File storage = Environment.getExternalStorageDirectory();
                File dir = new File(storage.getAbsolutePath() + "/documents");
                File file = new File(dir, "output.csv");
                FileOutputStream f = new FileOutputStream(file, true);
                try {
                    f.write(entry.getBytes());
                    f.flush();
                    f.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    private void createTimer() {
        //creating handler to run simultaneously to track duration in seconds
        final Handler handler = new Handler();
        handler.post(new Runnable() {
            @SuppressLint("DefaultLocale")
            @Override
            public void run() {
                handler.postDelayed(this, 1000);
                if (isRunning) {
                    seconds++;
                    //calculating distances between location updates and updating text views
                    DecimalFormat df = new DecimalFormat("#.##");
                    if (route.getRouteSize() >= 2) {
                        route.calculateDistance();
                        distance = route.getDistance();
                    }
                    updateViews(df);
                    //allowing preprocessing to happen at the instance of a 5 second interval
                    if ((seconds % 5) == 0) {
                        hasProcessed = Boolean.FALSE;
                    }
                    //updating notification every second

                }

            }
        });
    }

    private void updateViews(DecimalFormat df) {
        //changing timer text view
        int hours = seconds / 3600;
        int minutes = (seconds % 3600) / 60;
        int secs = seconds % 60;
        String time = String.format(Locale.getDefault(), "%d:%02d:%02d", hours, minutes, secs);
        timerText.setText(time);
        //changing calorie text view
        calories = Math.round(MET * User.getWeight() * (seconds.floatValue() / 3600));
        calorieText.setText(String.format("Calories:\n%d", calories));
        //changing step text view
        stepText.setText(String.format("Steps:\n%d", steps));
        distText.setText(String.format("Distance:\n%sm", df.format(distance)));
        //changing pace text view
        paceText.setText(Html.fromHtml("Pace:\n" + df.format(distance / seconds.floatValue()) + "ms<sup>-1</sup"));
    }

    private void handlePermissions(){
        //HANDLING PERMISSIONS
        //Permissions
        String[] PERMISSIONS = new String[]{
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.WRITE_EXTERNAL_STORAGE};

        if (checkPermissions(this, PERMISSIONS) == Boolean.FALSE) {
            //dealt with overriding onRequestPermissionsResult method
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(PERMISSIONS, 0);
            }
        } else {
            startRunning();
        }
    }

    //PERMISSIONS
    private boolean checkPermissions(Context context, String[] PERMISSIONS) {
        //CHECKING FOR EXISTING PERMISSIONS
        if (context != null && PERMISSIONS != null) {
            for (String permission : PERMISSIONS) {
                if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case 0:
                if (grantResults.length > 0) {
                    //checking if all permissions are granted on UI dialog
                    boolean granted = true;
                    for (int result : grantResults) {
                        if (result == PackageManager.PERMISSION_DENIED) {
                            granted = false;
                        }
                    }
                    if (granted) {
                        startRunning();
                    } else {
                        Toast.makeText(this, "Permissions Denied\nPlease allow permissions in settings", Toast.LENGTH_SHORT).show();
                        finishRunning();
                    }
                    return;
                }

            case 1:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    isRunning = true;
                    return;
                } else {
                    Toast.makeText(this, "Permissions Denied\nPlease allow permissions in settings", Toast.LENGTH_SHORT).show();
                    finishRunning();
                }
        }

    }

    private void finishRunning() {
        isRunning = false;
        sensorManager.unregisterListener(listener);
        if (locationManager != null && timeStarted != null) {
            locationManager.removeUpdates(locationListener);
            //exiting the running activity and saving data to database
            //will only save activities which last longer than 60s
            if (seconds > 60) {
                dbhelper helper = new dbhelper(RunningActivity.this);
                if (helper.saveActivity("running", date.toString(), timeStarted, seconds.toString(), calories.toString(), steps.toString(), String.valueOf(distance), null)) {
                    Toast.makeText(RunningActivity.this, "Save successful", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(RunningActivity.this, "Save unsuccessful", Toast.LENGTH_SHORT).show();
                }
            } else {
                //saves space and resources on database
                Toast.makeText(RunningActivity.this, "Activity too short, save unsuccessful", Toast.LENGTH_SHORT).show();
            }

        }
        //destroying notification
        notificationManagerCompat.cancel(1);
        this.finish();

    }


    //handling live notification bar
    public void sendOnChannel1() {
        //
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_1_ID)
                .setSmallIcon(R.mipmap.appicon)
                .setContentTitle("Running Tracking")
                .setContentText(String.format("Steps: %d Distance: %s Calories: %d", steps, distText.getText(), calories))
                .setCategory(NotificationCompat.CATEGORY_WORKOUT)
                .setOnlyAlertOnce(true)
                .build();
        notificationManagerCompat.notify(1, notification);
    }


}