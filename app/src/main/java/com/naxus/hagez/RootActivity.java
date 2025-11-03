package com.naxus.hagez;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class RootActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FirebaseApp.initializeApp(this);
        mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser != null) {
            // المستخدم مسجل دخول
            startActivity(new Intent(RootActivity.this, MainActivity.class));
        } else {
            // المستخدم غير مسجل
            startActivity(new Intent(RootActivity.this, AuthActivity.class));
        }

        finish(); // عشان مايرجعش تاني هنا
    }
}
