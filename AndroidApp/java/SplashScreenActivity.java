package com.example.smarthome;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class SplashScreenActivity extends AppCompatActivity {

    private final String welcomeText1 = "WELCOME";
    TextView welcomeText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash_screen);

        ImageView rectangle = findViewById(R.id.circle_scaleup);
        welcomeText = findViewById(R.id.welcomeText);
        ImageView logo = findViewById(R.id.logo);

        Animation scaleAnimation = AnimationUtils.loadAnimation(this, R.anim.scale_up);
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                rectangle.setVisibility(View.VISIBLE);
                rectangle.startAnimation(scaleAnimation);
            }
        }, 500);

        scaleAnimation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {}

            @Override
            public void onAnimationEnd(Animation animation) {
                Animation fadeInAnimation = AnimationUtils.loadAnimation(SplashScreenActivity.this, R.anim.fade_in);
                Animation fadeOutAnimation = AnimationUtils.loadAnimation(SplashScreenActivity.this, R.anim.fade_out);
                logo.startAnimation(fadeOutAnimation);

                fadeOutAnimation.setAnimationListener(new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {}

                    @Override
                    public void onAnimationEnd(Animation animation) {
                        logo.setVisibility(View.INVISIBLE);
                        welcomeText.setVisibility(View.VISIBLE);
                        animateText();
                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) {}
                });

                fadeInAnimation.setAnimationListener(new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {}

                    @Override
                    public void onAnimationEnd(Animation animation) {
                        Intent intent = new Intent(SplashScreenActivity.this, MainActivity.class);
                        startActivity(intent);
                        finish();
                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) {}
                });
            }

            @Override
            public void onAnimationRepeat(Animation animation) {}
        });
    }

    private void animateText() {
        final Handler handler = new Handler();
        final int delay = 100; // milliseconds

        handler.postDelayed(new Runnable() {
            int index = 0;

            @Override
            public void run() {
                if (index <= welcomeText1.length()) {
                    SpannableString spannableString = new SpannableString(welcomeText1.substring(0, index));
                    spannableString.setSpan(new ForegroundColorSpan(Color.WHITE), 0, spannableString.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    welcomeText.setText(spannableString);
                    index++;
                    handler.postDelayed(this, delay);
                } else {
                    // Delay before starting the intent
                    Handler handler = new Handler();
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            Intent intent = new Intent(SplashScreenActivity.this, MainActivity.class);
                            startActivity(intent);
                            finish();
                        }
                    }, 1500); // Delay in milliseconds (2 seconds in this example)
                }
            }
        }, delay);
    }
}
