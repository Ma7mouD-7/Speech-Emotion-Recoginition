package com.ma7moud27.speechemotionrecognition;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.HashMap;

public class ResultActivity extends AppCompatActivity {

    private HashMap <String, Integer> emoDict;
    private String emotion;

    private TextView emotionTextView;
    private TextView emotionLight, emotionDark;
    private ImageView emotionImage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);

        initComponents();
    }

    private void initComponents() {
        emotionTextView = findViewById(R.id.emotion);
        emotionLight = findViewById(R.id.emotion_light_shadow);
        emotionDark = findViewById(R.id.emotion_dark_shadow);

        emotionImage = findViewById(R.id.emo_image);

        emoDict = new HashMap<>();
        initDict();
        Intent intent = getIntent();
        emotion = intent.getStringExtra("RESULT_EMOTION");

        emotionTextView.setText(emotion);
        emotionLight.setText(emotion);
        emotionDark.setText(emotion);
        emotionImage.setImageResource(emoDict.get(emotion));



    }

    private void initDict() {
        emoDict.put("Neutral",R.drawable.emo_neutral);
        emoDict.put("Happy",R.drawable.emo_happy);
        emoDict.put("Sad",R.drawable.emo_sad);
        emoDict.put("Angry",R.drawable.emo_angry);
        emoDict.put("Fear",R.drawable.emo_fear);
        emoDict.put("Disgust",R.drawable.emo_disgust);
        emoDict.put("Surprise",R.drawable.emo_surprise);
    }


}