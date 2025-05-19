package com.example.smarthome;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricPrompt;

import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    ImageView image1, image2, imageView2;
    TextView textView, textView1, textView2, textView7, textView4, textView8;
    Button button;
    EditText editTextText3, editTextTextPassword;
    String username;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize views
        image1 = findViewById(R.id.image_1);
        imageView2 = findViewById(R.id.imageView2);
        image2 = findViewById(R.id.image_2);
        textView1 = findViewById(R.id.textView1);
        textView2 = findViewById(R.id.textView2);
        textView7 = findViewById(R.id.textView7);
        textView4 = findViewById(R.id.textView4);
        textView8 = findViewById(R.id.textView8);
        button = findViewById(R.id.button);
        editTextText3 = findViewById(R.id.editTextText3);
        editTextTextPassword = findViewById(R.id.editTextTextPassword);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        SharedPreferences sharedPreferences = getSharedPreferences("login_status", Context.MODE_PRIVATE);
        boolean isLoggedIn = sharedPreferences.getBoolean("isLoggedIn", false);

        if (isLoggedIn) {
            // If the user is already logged in, navigate directly to BluetoothComm activity
            navigateToChooser();
        }
        // Set initial focus on editTextText3
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                editTextText3.requestFocus();
            }
        }, 1000);

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                animateTextView(textView1, -80f, 15f, true);
            }
        }, 1300);

        shrinkMotion(textView8);
        shrinkMotion(textView7);
        shrinkMotion(button);

        // Set onClickListener for the "Sign In" button
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                editTextText3.clearFocus();
                editTextTextPassword.clearFocus();
                // Get the entered username and password
                username = editTextText3.getText().toString().trim();
                String password = editTextTextPassword.getText().toString().trim();

                // Check if the username and password are not empty
                if (!username.isEmpty() && !password.isEmpty()) {
                    if(isNetworkConnected()) {
                    // Execute the RegisterTask AsyncTask
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        new LoginTask(MainActivity.this, new LoginTask.LoginTaskCallback() {
                            @Override
                            public void onLoginSuccess() {
                                // Handle login success, you may want to navigate to another activity
                                Toast.makeText(MainActivity.this, "Login successful", Toast.LENGTH_SHORT).show();
                                // Save login status in SharedPreferences
                                SharedPreferences sharedPreferences = getSharedPreferences("login_status", Context.MODE_PRIVATE);
                                SharedPreferences.Editor editor = sharedPreferences.edit();
                                editor.putBoolean("isLoggedIn", true);
                                editor.apply();
                                SharedPreferences sharedPreferences1 = getSharedPreferences("user_info", Context.MODE_PRIVATE);
                                SharedPreferences.Editor editor1 = sharedPreferences1.edit();
                                String username1 = username.replace(".", "_");
                                editor1.putString("username", username1);
                                editor1.apply();
                                // Navigate to BluetoothComm activity
                                navigateToChooser();
                            }

                            @Override
                            public void onLoginFailure() {
                                // Handle other login failures, show a generic error message
                                Toast.makeText(MainActivity.this, "Email not found or password incorrect", Toast.LENGTH_SHORT).show();
                            }

                            @Override
                            public void onIncorrectPassword() {
                                // Handle incorrect password, show a specific error message
                                Toast.makeText(MainActivity.this, "Incorrect password", Toast.LENGTH_SHORT).show();
                            }
                        }).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, username, password);
                    }
                    } else {
                        Toast.makeText(MainActivity.this, "No Network Connection", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    // Show a toast message if the username or password is empty
                    Toast.makeText(MainActivity.this, "Username or password cannot be empty", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // Set onClickListener for the "Sign Up" TextView
        textView8.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Start the SignUpActivity when the "Sign Up" TextView is clicked
                Intent intent = new Intent(MainActivity.this, sign_up.class);
                startActivity(intent);
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
            }
        });


        // Set onClickListener for the "Forgot Password?" TextView
        textView7.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Check if biometric authentication is available and enrolled
                if (isBiometricAvailable()) {
                    // Prompt biometric authentication
                    promptBiometricAuthentication();
                } else {
                    // Biometric authentication not available or not enrolled, handle accordingly
                    Toast.makeText(MainActivity.this, "Biometric authentication not available or not enrolled", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // Set onTouchListener for the main layout
        findViewById(R.id.constraintLayoutMain).setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                // Hide the keyboard and clear focus
                hideKeyboard();
                editTextText3.clearFocus();
                editTextTextPassword.clearFocus();
                // Check if the username is empty and set default text
                return false;
            }
        });

        // Set onFocusChangeListener for the username EditText
        editTextText3.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean hasFocus) {
                if (hasFocus) {
                    animateTextView(textView1, -80f, 15f, true);
                } else if (!hasFocus && editTextText3.getText().toString().isEmpty()){
                    animateTextView(textView1, 0f, 20f, false);
                }
            }
        });


        editTextTextPassword.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean hasFocus) {
                if (hasFocus) {
                    animateTextView(textView4, -80f, 15f, true);
                } else if (!hasFocus && editTextTextPassword.getText().toString().isEmpty()){
                    animateTextView(textView4, 0f, 20f, false);
                }
            }
        });

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
                    view.animate().scaleX(0.92f).scaleY(0.92f).setDuration(30).start();
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    view.animate().scaleX(1f).scaleY(1f).setDuration(30).start();
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

    private void navigateToChooser() {
        Intent intent = new Intent(MainActivity.this, choosing.class);
        startActivity(intent);
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
    }

    // Method to check if biometric authentication is available
    private boolean isBiometricAvailable() {
        // Check if the device supports biometric authentication and if the user has enrolled biometric credentials
        // You can use BiometricPrompt's canAuthenticate() method for API level 23 and above
        // For lower API levels, you may need to check if the device has fingerprint or face hardware and if the user has enrolled any credentials
        // Return true if biometric authentication is available, false otherwise
        return true; // Placeholder, replace with actual implementation
    }

    // Method to prompt biometric authentication
    private void promptBiometricAuthentication() {
        // Use BiometricPrompt to prompt the user for biometric authentication
        // You can provide a prompt message and handle authentication callbacks
        // For example:
        BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle("Biometric Authentication")
                .setSubtitle("Authenticate to reset password")
                .setNegativeButtonText("Cancel")
                .build();

        BiometricPrompt biometricPrompt = new BiometricPrompt(MainActivity.this, Executors.newSingleThreadExecutor(), new BiometricPrompt.AuthenticationCallback() {
            @Override
            public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                super.onAuthenticationError(errorCode, errString);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (errorCode == BiometricPrompt.ERROR_USER_CANCELED) {
                            // Handle cancellation by the user
                            Toast.makeText(MainActivity.this, "Biometric authentication cancelled by the user", Toast.LENGTH_SHORT).show();
                        } else {
                            // Handle other authentication errors
                            Toast.makeText(MainActivity.this, "Biometric authentication error: " + errString, Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }

            @Override
            public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                super.onAuthenticationSucceeded(result);
                // Biometric authentication succeeded, proceed to reset password
                resetPassword();
            }

            @Override
            public void onAuthenticationFailed() {
                super.onAuthenticationFailed();
                // Handle authentication failure on the main UI thread
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(MainActivity.this, "Biometric authentication failed", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });

        biometricPrompt.authenticate(promptInfo);
    }

    // Method to reset password
    private void resetPassword() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // Add your password reset logic here
                // Example: Show a dialog or navigate to the password reset screen
                Intent intent = new Intent(MainActivity.this, PasswordResetActivity.class);
                startActivity(intent);
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                Toast.makeText(MainActivity.this, "Password reset initiated", Toast.LENGTH_SHORT).show();
            }
        });
    }


    private void animateTextView(TextView textView, float targetTranslationY, float targetTextSize, boolean hasFocus) {
        // Apply translation animation to move the hint text
        ObjectAnimator animatorTranslationY = ObjectAnimator.ofFloat(textView, "translationY", targetTranslationY);
        animatorTranslationY.setDuration(200); // Adjust the duration as needed

        // Calculate the current and target scaled text size
        float currentTextSize = textView.getTextSize();
        float scaledTargetTextSize = targetTextSize * getResources().getDisplayMetrics().scaledDensity;

        // Apply text size change animation to the TextView using ValueAnimator
        ValueAnimator animatorTextSize = ValueAnimator.ofFloat(currentTextSize, scaledTargetTextSize);
        animatorTextSize.setDuration(200); // Adjust the duration as needed
        animatorTextSize.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                float animatedValue = (float) valueAnimator.getAnimatedValue();
                textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, animatedValue);
            }
        });

        // Play animations together
        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playTogether(animatorTranslationY, animatorTextSize);
        animatorSet.start();
    }

    // Helper method to hide the keyboard
    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(editTextText3.getWindowToken(), 0);
        }
    }
}
