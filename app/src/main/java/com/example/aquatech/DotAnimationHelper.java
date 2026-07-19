package com.example.aquatech;

import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.LinearInterpolator;
import android.widget.TextView;

public class DotAnimationHelper {

    private ObjectAnimator anim1, anim2, anim3;
    private ObjectAnimator rotateAnim, scaleAnim;

    public void startPulseAnimation(TextView dot1, TextView dot2, TextView dot3) {
        anim1 = createPulseAnimator(dot1, 0);
        anim2 = createPulseAnimator(dot2, 200);
        anim3 = createPulseAnimator(dot3, 400);

        anim1.start();
        anim2.start();
        anim3.start();
    }

    private ObjectAnimator createPulseAnimator(View target, int delay) {
        ObjectAnimator animator = ObjectAnimator.ofPropertyValuesHolder(
                target,
                PropertyValuesHolder.ofFloat("scaleX", 1.0f, 1.5f, 1.0f),
                PropertyValuesHolder.ofFloat("scaleY", 1.0f, 1.5f, 1.0f),
                PropertyValuesHolder.ofFloat("alpha", 0.5f, 1.0f, 0.5f)
        );
        animator.setDuration(800);
        animator.setStartDelay(delay);
        animator.setRepeatCount(ValueAnimator.INFINITE);
        animator.setRepeatMode(ValueAnimator.REVERSE);
        return animator;
    }

    public void startMaintenanceAnimation(View icon) {
        // 1. PREMIUM SLOW ROTATION (4 seconds per 360 cycle)
        // Ginagamit ang LinearInterpolator para walang "hinto" sa bawat ikot
        rotateAnim = ObjectAnimator.ofFloat(icon, "rotation", 0f, 360f);
        rotateAnim.setDuration(4000); 
        rotateAnim.setInterpolator(new LinearInterpolator()); 
        rotateAnim.setRepeatCount(ValueAnimator.INFINITE);

        // 2. SMOOTH BREATHING SCALE (3 seconds pulse)
        // Ginagamit ang AccelerateDecelerate para "swabe" ang pag-zoom in at out
        scaleAnim = ObjectAnimator.ofPropertyValuesHolder(
                icon,
                PropertyValuesHolder.ofFloat("scaleX", 1.0f, 1.08f),
                PropertyValuesHolder.ofFloat("scaleY", 1.0f, 1.08f)
        );
        scaleAnim.setDuration(3000); 
        scaleAnim.setInterpolator(new AccelerateDecelerateInterpolator());
        scaleAnim.setRepeatCount(ValueAnimator.INFINITE);
        scaleAnim.setRepeatMode(ValueAnimator.REVERSE);

        rotateAnim.start();
        scaleAnim.start();
    }

    public void stopAnimation() {
        if (anim1 != null) anim1.cancel();
        if (anim2 != null) anim2.cancel();
        if (anim3 != null) anim3.cancel();
        if (rotateAnim != null) rotateAnim.cancel();
        if (scaleAnim != null) scaleAnim.cancel();
    }
}
