package com.example.smarthome;

import android.os.Build;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.function.Consumer;

public class PasswordResetClass {

    public static void execute(String email, String newPassword, Consumer<String> callback) {
        DatabaseReference myRef = FirebaseDatabase.getInstance().getReferenceFromUrl("https://smart-home-1c036-default-rtdb.firebaseio.com/Login");
        Query query = myRef.orderByChild("email").equalTo(email);

        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                        String key = snapshot.getKey();
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                            myRef.child(key).child("password").setValue(hashPassword(newPassword))
                                    .addOnSuccessListener(aVoid -> callback.accept("Success"))
                                    .addOnFailureListener(e -> callback.accept("Error"));
                        }
                    }
                } else {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        callback.accept("EmailNotFound");
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    callback.accept("Error");
                }
            }
        });
    }

    private static String hashPassword(String password) {
        // Implement your password hashing logic here
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
}
