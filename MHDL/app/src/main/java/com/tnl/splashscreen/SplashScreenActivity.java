package com.tnl.splashscreen;

import android.annotation.SuppressLint;
import android.content.Intent; // Import Intent
import android.os.Bundle;
import android.widget.ImageView; // Import ImageView

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.tnl.myapplication.MainActivity;
import com.tnl.myapplication.R;

@SuppressLint("CustomSplashScreen")
public class SplashScreenActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_splash_screen);

        // Find the ImageView by its ID
        ImageView logo = findViewById(R.id.logo);

        // Animate the logo's alpha from 0 to 1 over 1500 milliseconds
        logo.setAlpha(0f);
        logo.animate().setDuration(1500).alpha(1f).withEndAction(() -> {
            // Create an Intent to start MainActivity
            Intent i = new Intent(SplashScreenActivity.this, MainActivity.class);
            startActivity(i);
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            finish();
        });
    }
}