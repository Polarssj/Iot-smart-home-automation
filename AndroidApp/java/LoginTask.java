package com.example.smarthome;

import android.content.Context;
import android.os.AsyncTask;

import androidx.annotation.NonNull;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class LoginTask extends AsyncTask<String, Void, Void> {
    private Context context;
    private LoginTaskCallback callback;
    private boolean isEmailCorrect = false;
    private boolean isPasswordCorrect = false;

    public LoginTask(Context context, LoginTaskCallback callback) {
        this.context = context;
        this.callback = callback;
    }

    @Override
    protected Void doInBackground(String... params) {
        String username = params[0];
        String password = params[1];

        DatabaseReference myRef = FirebaseDatabase.getInstance().getReferenceFromUrl("https://smart-home-1c036-default-rtdb.firebaseio.com/Login");
        myRef.orderByChild("email").equalTo(username).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    isEmailCorrect = true;
                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                        // Check if the hashed password matches the stored hashed password
                        String storedHashedPassword = snapshot.child("password").getValue(String.class);
                        if (hashPassword(password).equals(storedHashedPassword)) {
                            isPasswordCorrect = true;
                            break;
                        }
                    }
                }
                // Notify the callback based on email and password correctness
                if (isEmailCorrect && isPasswordCorrect) {
                    callback.onLoginSuccess();
                } else if (isEmailCorrect && !isPasswordCorrect) {
                    callback.onIncorrectPassword();
                } else {
                    callback.onLoginFailure();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                callback.onLoginFailure();
            }
        });
        return null;
    }

    // Implement your own hashing function here
    private String hashPassword(String password) {
        // Implement your hashing logic (e.g., using MD5, SHA-256, etc.)
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

    public interface LoginTaskCallback {
        void onLoginSuccess();
        void onLoginFailure();
        void onIncorrectPassword();
    }
}
