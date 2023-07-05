package com.ma7moud27.speechemotionrecognition.animations;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;

import com.airbnb.lottie.LottieAnimationView;
import com.airbnb.lottie.LottieDrawable;
import com.ma7moud27.speechemotionrecognition.R;

import soup.neumorphism.NeumorphImageButton;

public class Animation {
    private Context mContext;

    public Animation(Context context) {
        mContext = context;
    }

    public void recordAnimation(NeumorphImageButton recButton, LottieAnimationView waveAnimation, LottieAnimationView micAnimation, boolean isRecPressed){
        recButton.setShapeType(isRecPressed ? 1:0);
        recButton.setShadowColorDark(isRecPressed ? mContext.getResources().getColor( R.color.color_primary_dark) : mContext.getResources().getColor( R.color.shadowColorDark));
        recButton.setShadowColorLight(isRecPressed ? mContext.getResources().getColor( R.color.color_primary_light) : mContext.getResources().getColor( R.color.shadowColorLight));
        waveAnimation.setRepeatCount(LottieDrawable.INFINITE);
        if(isRecPressed) {
            waveAnimation.resumeAnimation();
            micAnimation.setMinAndMaxFrame(0,48);
            micAnimation.addAnimatorListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    micAnimation.removeAnimatorListener(this);
                    micAnimation.setSpeed(-1f);
                    micAnimation.playAnimation();
                }
            });
        }
        else {
            waveAnimation.setProgress(0f);
            waveAnimation.cancelAnimation();
            micAnimation.addAnimatorListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    micAnimation.removeAnimatorListener(this);
                    micAnimation.setSpeed(-1f);
                    micAnimation.playAnimation();
                }
            });
        }
        micAnimation.setRepeatCount(0);
        micAnimation.setSpeed(1f);
        micAnimation.playAnimation();
    }

    public void playStopAnimation(NeumorphImageButton playStopButton, LottieAnimationView playStopAnimation, boolean isPlayPressed) {
        if (isPlayPressed) {
            playStopAnimation.playAnimation();
            playStopAnimation.setRepeatCount(0);
            playStopButton.setShapeType(1);
        } else {
            playStopAnimation.setSpeed(-1f);
            playStopAnimation.playAnimation();
            playStopAnimation.addAnimatorListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    playStopAnimation.removeAnimatorListener(this);
                    playStopAnimation.setProgress(0f);
                    playStopAnimation.setSpeed(1f);
                }
            });
            playStopButton.setShapeType(0);
        }
    }
}
