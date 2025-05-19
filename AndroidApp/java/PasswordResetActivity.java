package com.example.smarthome;

import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.ColorStateList;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.textfield.TextInputLayout;

import java.util.function.Consumer;

public class PasswordResetActivity extends AppCompatActivity {
    private TextInputLayout emailTextInputLayout, passwordTextInputLayout, confirmpasswordTextInputLayout;
    private EditText emailEditText, passwordEditText, confirmPasswordEditText;
    private Button submitButton;
    private ProgressBar progressBar;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_passwordreset);

        emailTextInputLayout = findViewById(R.id.emailTextInputLayout);
        passwordTextInputLayout = findViewById(R.id.passwordTextInputLayout);
        confirmpasswordTextInputLayout = findViewById(R.id.confirmpasswordTextInputLayout);
        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        confirmPasswordEditText = findViewById(R.id.confirmpasswordEditText);
        submitButton = findViewById(R.id.button);
        progressBar = findViewById(R.id.progressBar);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        // Set field validation for each EditText
        setFieldValidation(emailTextInputLayout, emailEditText);
        setFieldValidation(passwordTextInputLayout, passwordEditText);
        setFieldValidation(confirmpasswordTextInputLayout, confirmPasswordEditText);

        emailEditText.postDelayed(() -> {
            emailEditText.requestFocus();
            emailEditText.setSelection(emailEditText.getText().length()); // Move cursor to the end
        }, 1000); // Adjust the delay time as needed

        shrinkMotion(submitButton);

        // Set onClickListener for the submit button
        submitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = emailEditText.getText().toString();
                String password = passwordEditText.getText().toString();
                String confirmPassword = confirmPasswordEditText.getText().toString();
                int strength = calculatePasswordStrength(passwordEditText.getText().toString());
                if (isAnyFieldEmpty()) {
                    Toast.makeText(PasswordResetActivity.this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                } else if (!isEmailValid(email)) {
                    Toast.makeText(PasswordResetActivity.this, "Invalid email format", Toast.LENGTH_SHORT).show();
                } else if (!(strength == 100)) {
                    // Password does not meet criteria, set focus to password field and show toast
                    passwordEditText.requestFocus();
                    Toast.makeText(PasswordResetActivity.this, "Password needs minimum 8 chars, 1 uppercase, 1 digit", Toast.LENGTH_SHORT).show();
                } else if (!isPasswordMatching()) {
                    Toast.makeText(PasswordResetActivity.this, "Passwords do not match", Toast.LENGTH_SHORT).show();
                } else {
                    // Call password reset class
                    if(isNetworkConnected()) {
                        PasswordResetClass.execute(email, password, new Consumer<String>() {
                            @Override
                            public void accept(String result) {
                                if ("Success".equals(result)) {
                                    Toast.makeText(PasswordResetActivity.this, "Password reset successful for " + email, Toast.LENGTH_SHORT).show();
                                    // Handle success here
                                    Intent intent = new Intent(PasswordResetActivity.this, MainActivity.class);
                                    startActivity(intent);
                                    overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
                                } else if ("EmailNotFound".equals(result)) {
                                    Toast.makeText(PasswordResetActivity.this, "Email not found", Toast.LENGTH_SHORT).show();
                                    // Handle email not found here
                                } else {
                                    Toast.makeText(PasswordResetActivity.this, "Password reset failed", Toast.LENGTH_SHORT).show();
                                    // Handle failure here
                                    Intent intent = new Intent(PasswordResetActivity.this, MainActivity.class);
                                    startActivity(intent);
                                    overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
                                }
                            }
                        });
                    } else {
                        Toast.makeText(PasswordResetActivity.this, "No Network Connection", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });
        passwordEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                // Calculate the password strength
                String password = s.toString();
                int strength = calculatePasswordStrength(password);

                // Update the ProgressBar with animation
                animateProgressBar(strength);
            }
        });

        // Add OnBackPressedCallback
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                Intent intent = new Intent(PasswordResetActivity.this, MainActivity.class);
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

    // Method to animate ProgressBar with color transitions
    // Method to animate ProgressBar with color transitions
    private void animateProgressBar(int strength) {
        int currentProgress = progressBar.getProgress();

        // Determine colors for different strength levels
        int startColor = ContextCompat.getColor(PasswordResetActivity.this, R.color.colorError);
        int endColor;

        // Determine end color based on strength
        if (strength >= 75) {
            endColor = ContextCompat.getColor(PasswordResetActivity.this, R.color.green);
        } else if (strength >= 50) {
            endColor = ContextCompat.getColor(PasswordResetActivity.this, R.color.yellow);
        } else {
            endColor = ContextCompat.getColor(PasswordResetActivity.this, R.color.red);
        }

        // Create an ObjectAnimator for progress change
        ObjectAnimator progressAnimator = ObjectAnimator.ofInt(progressBar, "progress", currentProgress, strength);
        progressAnimator.setDuration(500); // Set duration for the animation in milliseconds
        progressAnimator.setInterpolator(new DecelerateInterpolator()); // Set interpolator for smooth animation

        // Start the progress animator
        progressAnimator.start();

        // Set the ProgressBar's progressTint directly
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            progressBar.setProgressTintList(ColorStateList.valueOf(endColor));
        }
    }

    private int calculatePasswordStrength(String password) {
        int length = password.length();
        boolean hasUpperCase = false;
        boolean hasDigit = false;

        for (char c : password.toCharArray()) {
            if (Character.isUpperCase(c)) {
                hasUpperCase = true;
            } else if (Character.isDigit(c)) {
                hasDigit = true;
            }
        }

        // Calculate strength based on criteria
        if (length >= 8 && hasUpperCase && hasDigit) {
            return 100; // 100% strength for password meeting all criteria
        } else {
            // Calculate strength as a percentage of the number of criteria met
            int criteriaCount = 0;
            if (length >= 8) {
                criteriaCount++;
            }
            if (hasUpperCase) {
                criteriaCount++;
            }
            if (hasDigit) {
                criteriaCount++;
            }
            return (criteriaCount * 100) / 3;
        }
    }

    private boolean isAnyFieldEmpty() {
        boolean isEmpty = false;
         if (TextUtils.isEmpty(emailEditText.getText())) {
            TextInputLayout layout = emailTextInputLayout;
            layout.setBoxStrokeColor(ContextCompat.getColor(this, R.color.colorError));
            setFieldFocus(emailEditText);
            isEmpty = true;
        } else if (!isEmailValid(emailEditText.getText().toString())) {
            TextInputLayout layout = emailTextInputLayout;
            layout.setBoxStrokeColor(ContextCompat.getColor(this, R.color.colorError));
            setFieldFocus(emailEditText);
        } else if (TextUtils.isEmpty(passwordEditText.getText())) {
            TextInputLayout layout = passwordTextInputLayout;
            layout.setBoxStrokeColor(ContextCompat.getColor(this, R.color.colorError));
            setFieldFocus(passwordEditText);
            isEmpty = true;
        } else if (TextUtils.isEmpty(confirmPasswordEditText.getText())) {
            TextInputLayout layout = confirmpasswordTextInputLayout;
            layout.setBoxStrokeColor(ContextCompat.getColor(this, R.color.colorError));
            setFieldFocus(confirmPasswordEditText);
            isEmpty = true;
        }  else if (!isPasswordMatching()) {
            TextInputLayout layout = confirmpasswordTextInputLayout;
            layout.setBoxStrokeColor(ContextCompat.getColor(this, R.color.colorError));
            setFieldFocus(confirmPasswordEditText);
        } else {
             emailTextInputLayout.setBoxStrokeColor(ContextCompat.getColor(this, R.color.my_status_bar_color1));
             passwordTextInputLayout.setBoxStrokeColor(ContextCompat.getColor(this, R.color.my_status_bar_color1));
             confirmpasswordTextInputLayout.setBoxStrokeColor(ContextCompat.getColor(this, R.color.my_status_bar_color1));
         }
        return isEmpty;
    }

    // Set field validation for TextInputLayout
    private void setFieldValidation(TextInputLayout layout, EditText editText) {
        layout.setBoxStrokeColor(ContextCompat.getColor(this, R.color.my_status_bar_color1));
    }
    private void hideKeyboard() {
        View focusedView = getCurrentFocus();
        if (focusedView instanceof EditText) {
            InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(focusedView.getWindowToken(), 0);
            focusedView.clearFocus();
        }
    }

    private boolean isPasswordMatching() {
        String password = passwordEditText.getText().toString().trim();
        String confirmPassword = confirmPasswordEditText.getText().toString().trim();
        return password.equals(confirmPassword);
    }

    // Method to check if email format is valid
    private boolean isEmailValid(String email) {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    private void setFieldFocus(EditText editText) {
        editText.requestFocus();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getActionMasked();

        // Display the Toast only for ACTION_DOWN
        if (action == MotionEvent.ACTION_DOWN) {
            Log.d("TouchEvent", "onTouchEvent called with action: " + action);

            hideKeyboard();

            // Check if any field is empty and if the Toast has not been shown
            if (isAnyFieldEmpty()) {
                Toast.makeText(PasswordResetActivity.this, "Please fill in all the fields", Toast.LENGTH_SHORT).show();
            }
            else if (!isEmailValid(emailEditText.getText().toString())) {
                Toast.makeText(PasswordResetActivity.this, "Invalid email format", Toast.LENGTH_SHORT).show();
            }
            else if (!isPasswordMatching()) {
                Toast.makeText(PasswordResetActivity.this, "Passwords do not match", Toast.LENGTH_SHORT).show();
            }
        }
        return super.onTouchEvent(event);
    }
}
