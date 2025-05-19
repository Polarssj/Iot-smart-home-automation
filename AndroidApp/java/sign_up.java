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
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.FirebaseApp;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class sign_up extends AppCompatActivity {

    private TextInputLayout firstNameLayout, lastNameLayout, emailTextInputLayout,
            passwordTextInputLayout, confirmpasswordTextInputLayout;
    private EditText firstNameEditText, lastNameEditText, emailEditText,
            passwordEditText, confirmpasswordEditText;
    private ProgressBar progressBar;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);
        FirebaseApp.initializeApp(getApplicationContext());

        firstNameLayout = findViewById(R.id.firstNameLayout);
        lastNameLayout = findViewById(R.id.lastNameLayout);
        emailTextInputLayout = findViewById(R.id.emailTextInputLayout);
        passwordTextInputLayout = findViewById(R.id.passwordTextInputLayout);
        confirmpasswordTextInputLayout = findViewById(R.id.confirmpasswordTextInputLayout);
        progressBar = findViewById(R.id.progressBar);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        firstNameEditText = findViewById(R.id.firstNameEditText);
        lastNameEditText = findViewById(R.id.lastNameEditText);
        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        confirmpasswordEditText = findViewById(R.id.confirmpasswordEditText);

        // Set box stroke color and focus change listener for each field
        setFieldValidation(firstNameLayout, firstNameEditText);
        setFieldValidation(lastNameLayout, lastNameEditText);
        setFieldValidation(emailTextInputLayout, emailEditText);
        setFieldValidation(passwordTextInputLayout, passwordEditText);
        setFieldValidation(confirmpasswordTextInputLayout, confirmpasswordEditText);


        // Set a delay for showing the cursor in the first name EditText
        firstNameEditText.postDelayed(() -> {
            firstNameEditText.requestFocus();
            firstNameEditText.setSelection(firstNameEditText.getText().length()); // Move cursor to the end
        }, 1000); // Adjust the delay time as needed

        // Set click listener for sign up button
        TextView signUpButton = findViewById(R.id.button);
        shrinkMotion(signUpButton);
        signUpButton.setOnClickListener(view -> {
            // Check if any field is empty and focus on the first empty or incorrect field
            int strength = calculatePasswordStrength(passwordEditText.getText().toString());
            if (isAnyFieldEmpty()) {
                Toast.makeText(sign_up.this, "Please fill in all the fields", Toast.LENGTH_SHORT).show();
            } else if (!isNameValid(firstNameEditText.getText().toString())) {
                Toast.makeText(sign_up.this, "First name should contain only letters", Toast.LENGTH_SHORT).show();
            } else if (!isNameValid(lastNameEditText.getText().toString())) {
                Toast.makeText(sign_up.this, "Last name should contain only letters", Toast.LENGTH_SHORT).show();
            } else if (!isValidEmail()) {
                Toast.makeText(sign_up.this, "Invalid email format", Toast.LENGTH_SHORT).show();
            } else if (!(strength == 100)) {
                // Password does not meet criteria, set focus to password field and show toast
                passwordEditText.requestFocus();
                Toast.makeText(sign_up.this, "Password needs minimum 8 chars, 1 uppercase, 1 digit", Toast.LENGTH_SHORT).show();
            } else if (!isPasswordMatching()) {
                Toast.makeText(sign_up.this, "Passwords do not match", Toast.LENGTH_SHORT).show();
            } else {
                if (isNetworkConnected()) {
                    checkEmailExists(emailEditText.getText().toString());
                } else {
                    Toast.makeText(sign_up.this, "No Network Connection", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // Set click listener for login text
        TextView loginText = findViewById(R.id.textView3);
        shrinkMotion(loginText);
        loginText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Start the SignUpActivity when the "Sign Up" TextView is clicked
                Intent intent = new Intent(sign_up.this, MainActivity.class);
                startActivity(intent);
                overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
            }
        });

        // Set text change listener for password EditText to update password strength
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
                Intent intent = new Intent(sign_up.this, MainActivity.class);
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
        int startColor = ContextCompat.getColor(sign_up.this, R.color.colorError);
        int endColor;

        // Determine end color based on strength
        if (strength >= 75) {
            endColor = ContextCompat.getColor(sign_up.this, R.color.green);
        } else if (strength >= 50) {
            endColor = ContextCompat.getColor(sign_up.this, R.color.yellow);
        } else {
            endColor = ContextCompat.getColor(sign_up.this, R.color.red);
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

    private void setFieldValidation(TextInputLayout layout, EditText editText) {
        layout.setBoxStrokeColor(ContextCompat.getColor(this, R.color.my_status_bar_color1));
    }

    private boolean isAnyFieldEmpty() {
        boolean isEmpty = false;
        if (TextUtils.isEmpty(firstNameEditText.getText())) {
            TextInputLayout layout = firstNameLayout;
            layout.setBoxStrokeColor(ContextCompat.getColor(this, R.color.colorError));
            setFieldFocus(firstNameEditText);
            isEmpty = true;
        } else if (!isNameValid(firstNameEditText.getText().toString())) {
            TextInputLayout layout = firstNameLayout;
            layout.setBoxStrokeColor(ContextCompat.getColor(this, R.color.colorError));
            setFieldFocus(firstNameEditText);
        } else if (!isNameValid(lastNameEditText.getText().toString())) {
            TextInputLayout layout = lastNameLayout;
            layout.setBoxStrokeColor(ContextCompat.getColor(this, R.color.colorError));
            setFieldFocus(lastNameEditText);
        } else if (TextUtils.isEmpty(lastNameEditText.getText())) {
            TextInputLayout layout = lastNameLayout;
            layout.setBoxStrokeColor(ContextCompat.getColor(this, R.color.colorError));
            setFieldFocus(lastNameEditText);
            isEmpty = true;
        } else if (TextUtils.isEmpty(emailEditText.getText())) {
            TextInputLayout layout = emailTextInputLayout;
            layout.setBoxStrokeColor(ContextCompat.getColor(this, R.color.colorError));
            setFieldFocus(emailEditText);
            isEmpty = true;
        } else if (!isValidEmail()) {
            TextInputLayout layout = emailTextInputLayout;
            layout.setBoxStrokeColor(ContextCompat.getColor(this, R.color.colorError));
            setFieldFocus(emailEditText);
        } else if (TextUtils.isEmpty(passwordEditText.getText())) {
            TextInputLayout layout = passwordTextInputLayout;
            layout.setBoxStrokeColor(ContextCompat.getColor(this, R.color.colorError));
            setFieldFocus(passwordEditText);
            isEmpty = true;
        } else if (TextUtils.isEmpty(confirmpasswordEditText.getText())) {
            TextInputLayout layout = confirmpasswordTextInputLayout;
            layout.setBoxStrokeColor(ContextCompat.getColor(this, R.color.colorError));
            setFieldFocus(confirmpasswordEditText);
            isEmpty = true;
        }  else if (!isPasswordMatching()) {
            TextInputLayout layout = confirmpasswordTextInputLayout;
            layout.setBoxStrokeColor(ContextCompat.getColor(this, R.color.colorError));
            setFieldFocus(confirmpasswordEditText);
        } else {
            // If none of the above conditions are met, set layout color to default
            firstNameLayout.setBoxStrokeColor(ContextCompat.getColor(this, R.color.my_status_bar_color1));
            lastNameLayout.setBoxStrokeColor(ContextCompat.getColor(this, R.color.my_status_bar_color1));
            emailTextInputLayout.setBoxStrokeColor(ContextCompat.getColor(this, R.color.my_status_bar_color1));
            passwordTextInputLayout.setBoxStrokeColor(ContextCompat.getColor(this, R.color.my_status_bar_color1));
            confirmpasswordTextInputLayout.setBoxStrokeColor(ContextCompat.getColor(this, R.color.my_status_bar_color1));
        }
        return isEmpty;
    }

    private void setFieldFocus(EditText editText) {
        editText.requestFocus();
    }

    private boolean isPasswordMatching() {
        // Check if password and confirm password match
        return passwordEditText.getText().toString().equals(confirmpasswordEditText.getText().toString());
    }

    private void hideKeyboard() {
        View focusedView = getCurrentFocus();
        if (focusedView instanceof EditText) {
            InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(focusedView.getWindowToken(), 0);
            focusedView.clearFocus();
        }
    }

    // Method to initiate the registration process
    private void registerUser() {
        String firstName = firstNameEditText.getText().toString();
        String lastName = lastNameEditText.getText().toString();
        String email = emailEditText.getText().toString();
        String password = passwordEditText.getText().toString();

        // Hash the password before storing it
        String hashedPassword = hashPassword(password);

        // Create a User object with hashed password
        User user = new User(firstName, lastName, email, hashedPassword);

        // Get a reference to the Firebase database
        DatabaseReference myRef = FirebaseDatabase.getInstance().getReferenceFromUrl("https://smart-home-1c036-default-rtdb.firebaseio.com/Login");


        // Store the user object in Firebase Realtime Database
        myRef.child(email.replace(".", "_")).setValue(user)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        // Registration successful, navigate to MainActivity
                        Toast.makeText(sign_up.this, "Registration Success", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(sign_up.this, MainActivity.class);
                        startActivity(intent);
                        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        // Registration failed, show an error message
                        Toast.makeText(sign_up.this, "Registration failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private String hashPassword(String password) {
        // Implement password hashing algorithm (e.g., SHA-256)
        // For demonstration purposes, you can use a simple hashing method like MD5
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] messageDigest = md.digest(password.getBytes());
            BigInteger no = new BigInteger(1, messageDigest);
            String hashedPassword = no.toString(16);
            while (hashedPassword.length() < 32) {
                hashedPassword = "0" + hashedPassword;
            }
            return hashedPassword;
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        }
    }

    // Add this method to check the validity of the email format
    private boolean isValidEmail() {
        String email = emailEditText.getText().toString().trim();
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    private void checkEmailExists(String email) {
        DatabaseReference myRef = FirebaseDatabase.getInstance().getReferenceFromUrl("https://smart-home-1c036-default-rtdb.firebaseio.com/Login");

        myRef.orderByChild("email").equalTo(email).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    // Email already exists, show a Toast message
                    setFieldFocus(emailEditText);
                    Toast.makeText(sign_up.this, "Email already in use. Try another email.", Toast.LENGTH_SHORT).show();
                } else {
                    // Email does not exist, proceed with the registration process
                    registerUser();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                // Error occurred while checking email existence
                Toast.makeText(sign_up.this, "Error checking email existence", Toast.LENGTH_SHORT).show();
            }
        });
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

    private boolean isNameValid(String name) {
        // Define a regex pattern for validating names (letters only)
        String regex = "^[a-zA-Z\\s]+$";
        return name.matches(regex);
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
                Toast.makeText(sign_up.this, "Please fill in all the fields", Toast.LENGTH_SHORT).show();
            }
            else if (!isNameValid(firstNameEditText.getText().toString())) {
                Toast.makeText(sign_up.this, "First name should contain only letters", Toast.LENGTH_SHORT).show();
            }
            else if (!isNameValid(lastNameEditText.getText().toString())) {
                Toast.makeText(sign_up.this, "Last name should contain only letters", Toast.LENGTH_SHORT).show();
            }
            else if (!isValidEmail()) {
                Toast.makeText(sign_up.this, "Invalid email format", Toast.LENGTH_SHORT).show();
            }
            else if (!isPasswordMatching()) {
                Toast.makeText(sign_up.this, "Passwords do not match", Toast.LENGTH_SHORT).show();
            }
        }
        return super.onTouchEvent(event);
    }
}
