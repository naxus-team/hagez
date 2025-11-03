package com.naxus.hagez;

import android.animation.Animator;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.splashscreen.SplashScreen;

import com.airbnb.lottie.LottieAnimationView;
import com.airbnb.lottie.LottieComposition;
import com.airbnb.lottie.LottieCompositionFactory;
import com.airbnb.lottie.LottieResult;

public class SplashActivity extends AppCompatActivity {

    // نخزن آخر Composition في الذاكرة مؤقتًا لتسريع التشغيل القادم
    private static LottieComposition cachedComposition;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SplashScreen.installSplashScreen(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        LottieAnimationView lottie = findViewById(R.id.lottieLogo);
        if (cachedComposition != null) {
            lottie.setComposition(cachedComposition);
            lottie.playAnimation();
            onLottieEnd(lottie);
            return;
        }
        LottieCompositionFactory.fromAsset(this, "hagez.json")
                .addListener(composition -> {
                    cachedComposition = composition;
                    lottie.setComposition(composition);
                    lottie.playAnimation();
                    onLottieEnd(lottie);
                })
                .addFailureListener(error -> {
                    Log.e("SplashActivity", "Lottie load failed", error);
                    startNext();
                });
    }

    private void onLottieEnd(LottieAnimationView lottie) {
        lottie.addAnimatorListener(new Animator.AnimatorListener() {
            @Override public void onAnimationStart(Animator animation) {}
            @Override public void onAnimationRepeat(Animator animation) {}
            @Override public void onAnimationCancel(Animator animation) {}

            @Override
            public void onAnimationEnd(Animator animation) {
                startNext();
            }
        });
    }

    private void startNext() {
        startActivity(new Intent(SplashActivity.this, RootActivity.class));
        finish();
    }
}
