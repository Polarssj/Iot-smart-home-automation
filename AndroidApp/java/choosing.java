package com.example.smarthome;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class choosing extends AppCompatActivity {

    private ImageButton Button1;
    private ImageButton Button2;
    private TextView textView, textView13;
    private DatabaseReference mDatabase;
    String firstname;
    String lastname;
    String receivedString;
    private Handler handler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_choosing);

        mDatabase = FirebaseDatabase.getInstance().getReferenceFromUrl("https://smart-home-1c036-default-rtdb.firebaseio.com/Login/");

        Button1 = findViewById(R.id.Button1);
        Button2 = findViewById(R.id.Button2);
        textView = findViewById(R.id.textView);
        textView13 = findViewById(R.id.textView13);
        shrinkMotion(Button1);
        shrinkMotion(Button2);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        handler = new Handler();

        SharedPreferences sharedPreferences = getSharedPreferences("user_info", Context.MODE_PRIVATE);
        receivedString = sharedPreferences.getString("username", null);

        Button1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(choosing.this, BluetoothComm.class);
                startActivity(intent);
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
            }
        });

        Button2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(choosing.this, cloud_comm.class);
                startActivity(intent);
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
            }
        });

        ImageView imageView1 = findViewById(R.id.imageView1);
        imageView1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showPopupMenu(v);
            }
        });

        if(isNetworkConnected()) {
            username();
        } else {
            Toast.makeText(this, "No Network Connection", Toast.LENGTH_SHORT).show();
            updateTextView();
        }

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                finishAffinity();
            }
        });
    }

    @SuppressLint("ClickableViewAccessibility")
    void shrinkMotion (View view1) {
        view1.setOnTouchListener((view, motionEvent) -> {
            switch (motionEvent.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    view1.animate().scaleX(0.92f).scaleY(0.92f).setDuration(30).start();
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    view1.animate().scaleX(1f).scaleY(1f).setDuration(30).start();
                    break;
            }
            return false;
        });
    }

    public boolean isNetworkConnected() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        Network activeNetwork = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            activeNetwork = connectivityManager.getActiveNetwork();
        }
        if (activeNetwork == null) {
            return false;
        }
        NetworkCapabilities networkCapabilities = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            networkCapabilities = connectivityManager.getNetworkCapabilities(activeNetwork);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            return networkCapabilities != null && networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
        }
        return false;
    }

    private void username() {
        SharedPreferences sharedPreferences = getSharedPreferences("login_details", Context.MODE_PRIVATE);
        mDatabase.child(receivedString + "/firstname").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                // Get the value of firstname from the dataSnapshot
                firstname = dataSnapshot.getValue(String.class);
                if(firstname != null) {
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putString("FirstName", firstname);
                    editor.apply();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                // Handle potential errors here
                Log.e("Firebase", "Error getting firstname value", databaseError.toException());
            }
        });

        mDatabase.child(receivedString + "/lastname").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                // Get the value of lastname from the dataSnapshot
                lastname = dataSnapshot.getValue(String.class);
                if(lastname != null) {
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putString("LastName", lastname);
                    editor.apply();
                }
                updateTextView();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                // Handle potential errors here
                Log.e("Firebase", "Error getting lastname value", databaseError.toException());
            }
        });
    }

    private void animateText(final String text1, final String text2) {
        final Handler handler = new Handler();
        final int delay = 70; //milliseconds

        handler.postDelayed(new Runnable() {
            int count = 0;

            @Override
            public void run() {
                if (count < text1.length()) {
                    textView.setText(text1.substring(0, count + 1));
                    count++;
                    handler.postDelayed(this, delay);
                }
            }
        }, delay);

        // Define a Handler
        Handler handler1 = new Handler();

// Define a Runnable for the second animation
        Runnable secondAnimationRunnable = new Runnable() {
            int count = 0;

            @Override
            public void run() {
                if (count < text2.length()) {
                    textView13.setText(text2.substring(0, count + 1));
                    count++;
                    handler1.postDelayed(this, 50);
                }
            }
        };

// Post the second animation Runnable after the first animation is finished
        handler.postDelayed(secondAnimationRunnable, text1.length() * delay + 300);
    }

    private void updateTextView() {
        SharedPreferences sharedPreferences = getSharedPreferences("login_details", Context.MODE_PRIVATE);
        String firstname1 = sharedPreferences.getString("FirstName", null);
        String lastname1 = sharedPreferences.getString("LastName", null);
        if (firstname1 != null && lastname1 != null) {
            // Both firstname and lastname have been retrieved
            animateText(("Hello, " + firstname1 + " " + lastname1 + "!"),("Opt for Bluetooth or Cloud services\nas per your needs, with ease."));
        }
    }

    @SuppressLint("ResourceType")
    private void showPopupMenu(View v) {
        SharedPreferences sharedPreferences = getSharedPreferences("login_status", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean("isLoggedIn", false);
        editor.apply();
        Intent intent = new Intent(choosing.this, MainActivity.class);
        startActivity(intent);
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
    }
}