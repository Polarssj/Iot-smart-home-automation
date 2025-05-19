package com.example.smarthome;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.ToggleButton;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class cloud_comm extends AppCompatActivity {

    private DatabaseReference mDatabase;
    private ToggleButton toggleButton1;
    private ToggleButton toggleButton2;
    private ToggleButton toggleButton3;
    private ToggleButton toggleButton4;
    private ToggleButton toggleButton5;
    private ToggleButton toggleButton6;
    private ToggleButton toggleButton7;
    private ToggleButton toggleButton8;
    char[] toggleState = new char[8];
    int Update_state;
    int ESP32_status;
    ImageView connectionStatusImageView;
    private Handler handler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cloud_comm);

        // Initialize Firebase database reference
        mDatabase = FirebaseDatabase.getInstance().getReferenceFromUrl("https://smart-home-1c036-default-rtdb.firebaseio.com/");
        mDatabase.child("Update_Origin").setValue(0);

        // Set up the toggle buttons
        toggleButton1 = findViewById(R.id.toggleButton1);
        toggleButton2 = findViewById(R.id.toggleButton2);
        toggleButton3 = findViewById(R.id.toggleButton3);
        toggleButton4 = findViewById(R.id.toggleButton4);
        toggleButton5 = findViewById(R.id.toggleButton5);
        toggleButton6 = findViewById(R.id.toggleButton6);
        toggleButton7 = findViewById(R.id.toggleButton7);
        toggleButton8 = findViewById(R.id.toggleButton8);
        shrinkMotion(toggleButton1);
        shrinkMotion(toggleButton2);
        shrinkMotion(toggleButton3);
        shrinkMotion(toggleButton4);
        shrinkMotion(toggleButton5);
        shrinkMotion(toggleButton6);
        shrinkMotion(toggleButton7);
        shrinkMotion(toggleButton8);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        connectionStatusImageView = findViewById(R.id.imageView);

        for (int i = 0; i < toggleState.length; i++) {
            toggleState[i] = '0';
        }

        ImageView imageView1 = findViewById(R.id.imageView1);
        imageView1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showPopupMenu(v);
            }
        });

        startLoop();
        setupToggleButtonListeners();

        mDatabase.child("Pin_State").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                // Get the value of Pin_State from the dataSnapshot
                String toggleStateString = dataSnapshot.getValue(String.class);
                toggleState = toggleStateString.toCharArray();

                // Assuming toggleState is the char array containing the state of each toggle button
                for (int i = 0; i < toggleState.length; i++) {
                    boolean isToggled = toggleState[i] == '1'; // Assuming '1' represents toggled and '0' represents not toggled
                    switch (i) {
                        case 0:
                            toggleButton1.setChecked(isToggled);
                            break;
                        case 1:
                            toggleButton2.setChecked(isToggled);
                            break;
                        case 2:
                            toggleButton3.setChecked(isToggled);
                            break;
                        case 3:
                            toggleButton4.setChecked(isToggled);
                            break;
                        case 4:
                            toggleButton5.setChecked(isToggled);
                            break;
                        case 5:
                            toggleButton6.setChecked(isToggled);
                            break;
                        case 6:
                            toggleButton7.setChecked(isToggled);
                            break;
                        case 7:
                            toggleButton8.setChecked(isToggled);
                            break;
                        default:
                            break;
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                // Handle potential errors here
                Log.e("Firebase", "Error getting Pin_State value", databaseError.toException());
            }
        });

        // Add OnBackPressedCallback
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                Intent intent = new Intent(cloud_comm.this, choosing.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
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

    private void setupToggleButtonListeners() {
        toggleButton1.setOnCheckedChangeListener(createToggleListener('1', '0', 0));
        toggleButton2.setOnCheckedChangeListener(createToggleListener('1', '0', 1));
        toggleButton3.setOnCheckedChangeListener(createToggleListener('1', '0', 2));
        toggleButton4.setOnCheckedChangeListener(createToggleListener('1', '0', 3));
        toggleButton5.setOnCheckedChangeListener(createToggleListener('1', '0', 4));
        toggleButton6.setOnCheckedChangeListener(createToggleListener('1', '0', 5));
        toggleButton7.setOnCheckedChangeListener(createToggleListener('1', '0', 6));
        toggleButton8.setOnCheckedChangeListener(createToggleListener('1', '0', 7));
    }

    private CompoundButton.OnCheckedChangeListener createToggleListener(final char on, final char off, final int no) {
        return new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (buttonView.isPressed()) {
                if (isChecked) {
                    // Send upper case character when toggle button is turned on
                    toggleState[no] = on;
                } else {
                    // Send lower case character when toggle button is turned off
                    toggleState[no] = off;
                }
                    // Send the updated toggleState to Firebase
                    Update_state = 0;
                    sendToggleState();
                }
            }
        };
    }

    private void startLoop() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                loop();
                handler.postDelayed(this, 20000); // Delay for 20 seconds
            }
        }, 0); // Start immediately
    }

    public void loop() {
            // Perform some action
            mDatabase.child("Update_Origin").addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    // Get the value of Pin_State from the dataSnapshot
                    ESP32_status = dataSnapshot.getValue(Integer.class);
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    // Handle potential errors here
                    Log.e("Firebase", "Error getting Pin_State value", databaseError.toException());
                }
            });

            if (ESP32_status == 0) {
                connectionStatusImageView.setImageResource(R.drawable.firebase_logo_disconnect);
                Log.d("DelayAndCompare", "Variable is still 0 after 10 seconds");
                // Perform actions if variable is still 0 after 10 seconds
            } else {
                connectionStatusImageView.setImageResource(R.drawable.firebase_logo_connect);
                Log.d("DelayAndCompare", "Variable has changed from 0 after 10 seconds");
                // Perform actions if variable has changed from 0 after 10 seconds
            }
    }

    @SuppressLint("ResourceType")
    private void showPopupMenu(View v) {
        PopupMenu popupMenu = new PopupMenu(this, v);
        popupMenu.inflate(R.menu.popup_menu); // Inflate the menu resource
        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                if (item.getItemId() == R.id.logout) {
                    // Handle menu item click for logout
                    SharedPreferences sharedPreferences = getSharedPreferences("login_status", Context.MODE_PRIVATE);
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putBoolean("isLoggedIn", false);
                    editor.apply();
                    Intent intent = new Intent(cloud_comm.this, MainActivity.class);
                    startActivity(intent);
                    overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
                    return true;
                }
                return false;
            }
        });
        popupMenu.show();
    }

    private void sendToggleState() {
        // Convert the toggleState array to a string
        String toggleStateString = new String(toggleState);

        // Set the value of the "Pin_State" node in Firebase to the toggleStateString
        mDatabase.child("Update_Origin").setValue(Update_state);
        mDatabase.child("Pin_State").setValue(toggleStateString);
    }
}
