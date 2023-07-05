package com.ma7moud27.speechemotionrecognition;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.airbnb.lottie.LottieAnimationView;
import com.ma7moud27.speechemotionrecognition.animations.Animation;
import com.ma7moud27.speechemotionrecognition.network.ApiService;
import com.ma7moud27.speechemotionrecognition.network.Response;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;

import me.bastanfar.semicirclearcprogressbar.SemiCircleArcProgressBar;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import soup.neumorphism.NeumorphButton;
import soup.neumorphism.NeumorphImageButton;

public class MainActivity extends AppCompatActivity {
    private Animation mAnimationManager;

    private NeumorphImageButton recButton;
    private NeumorphImageButton folderButton;
    private NeumorphImageButton playStopButton;
    private NeumorphImageButton infoButton;
    private NeumorphImageButton backButton;

    private NeumorphButton submitButton;

    private TextView timer, timerLight, timerDark;
    private TextView filename;

    private SemiCircleArcProgressBar progressBar;
    private LottieAnimationView waveAnimation;
    private LottieAnimationView micAnimation;
    private LottieAnimationView playStopAnimation;

    private boolean isRecording;
    private boolean isPlaying;
    private String recPath, recName;
    private Uri recUri;

    private MediaRecorder mMediaRecorder;
    private ActivityResultLauncher<Intent> mResultLauncher;
    private MediaPlayer mMediaPlayer;

    private Handler mStopwatch;
    private Runnable mStopwatchEvent;
    private long startTime;

    private CountDownTimer mTimer;
    private long currentFileDuration;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initComponents();
        if(!checkPermissions(new ArrayList<>(Arrays.asList(android.Manifest.permission.RECORD_AUDIO,android.Manifest.permission.READ_MEDIA_AUDIO)))) grantPermission();

        recButton.setOnClickListener(v-> handleRecording());

        playStopButton.setOnClickListener(v -> handlePlaying());

        folderButton.setOnClickListener(v -> getFile());

        submitButton.setOnClickListener(v -> {
            if(recPath == null && recUri == null)
                Toast.makeText(this, "Please Record or Choose File First", Toast.LENGTH_LONG).show();
            else{
//                getPrediction();
                AlertDialog dialog = showProgressbar(this, "Loading");
                dialog.show();
                new Handler().postDelayed(() -> {
                    dialog.dismiss();
                    Intent intent = new Intent(MainActivity.this,ResultActivity.class);
                    intent.putExtra("RESULT_EMOTION","Angry");
                    startActivity(intent);
                },7500);
            }
        });
    }
    public static AlertDialog showProgressbar(Context context, String text){
        LayoutInflater inflater = LayoutInflater.from(context);
        View customLayout = inflater.inflate(R.layout.progress_bar_layout, null);
        TextView titleTextView = customLayout.findViewById(R.id.loading_dialog_title);
        titleTextView.setText(text);

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setView(customLayout);
        AlertDialog dialog = builder.create();
        dialog.setCancelable(false);
        return dialog;
    }

    private void initComponents() {
        mAnimationManager = new Animation(this);

        isRecording = false;
        isPlaying = false;

        recButton = findViewById(R.id.rec_btn);
        playStopButton = findViewById(R.id.play_stop_btn);
        folderButton = findViewById(R.id.folder_btn);
        submitButton = findViewById(R.id.submit_btn);
        infoButton = findViewById(R.id.info_btn);
        backButton = findViewById(R.id.back_btn);

        filename = findViewById(R.id.filename_tv);
        filename.setText("");
        timer = findViewById(R.id.timer);
        timerLight = findViewById(R.id.timer_light_shadow);
        timerDark = findViewById(R.id.timer_dark_shadow);

        progressBar = findViewById(R.id.progress_bar);

        waveAnimation = findViewById(R.id.wave_animation);
        micAnimation = findViewById(R.id.mic_animation);

        playStopAnimation = findViewById(R.id.play_stop_animation);
        playStopAnimation.setMinAndMaxFrame(30,60);

        mResultLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    Intent intent = result.getData();
                    if (intent != null) {
                        recUri = intent.getData();
                        String path = getNameFromUri(recUri);
                        filename.setText(path);
                        currentFileDuration = getAudioDuration();
                        updateTimer(currentFileDuration);
                    }
                }
        );
        mStopwatch = new Handler();

    }

    private void toggleStopwatch(boolean isEnable) {
        if(isEnable){
            startTime = System.currentTimeMillis();
            mStopwatchEvent = new Runnable() {
                @Override
                public void run() {
                    long elapsedTime = (System.currentTimeMillis() - startTime)/1000;
                    long minutes = elapsedTime / 60;
                    long seconds = elapsedTime % 60;
                    updateTimer(minutes,seconds);
                    mStopwatch.postDelayed(this,1000);
                }
            };
            mStopwatch.post(mStopwatchEvent);
        }
        else mStopwatch.removeCallbacks(mStopwatchEvent);


    }

    private boolean startRecording(){
        if(checkPermissions(new ArrayList<>(Arrays.asList(android.Manifest.permission.RECORD_AUDIO,android.Manifest.permission.READ_MEDIA_AUDIO)))){
            String curTime = new SimpleDateFormat("yyyy_MM_dd HH_mm_ss", Locale.getDefault()).format(new Date());
            ContextWrapper contextWrapper = new ContextWrapper(getApplicationContext());
            File dir = contextWrapper.getExternalFilesDir(Environment.DIRECTORY_MUSIC);
            File file = new File(dir,"REC_"+curTime+"_SER.wav");
            recPath = file.getPath();
            recName = file.getName();
            mMediaRecorder = new MediaRecorder();
            mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.DEFAULT);
            mMediaRecorder.setAudioSamplingRate(48000);
            mMediaRecorder.setAudioChannels(1);
            mMediaRecorder.setAudioEncodingBitRate(768000);




            mMediaRecorder.setOutputFile(recPath);

            try {
                progressBar.setVisibility(View.INVISIBLE);
                mMediaRecorder.prepare();
                mMediaRecorder.start();
                toggleStopwatch(true);
                filename.setText(R.string.recording);
                filename.setTextSize(20);
                filename.setTextColor(getResources().getColor( R.color.color_secondary_shade) );
                return true;
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }

        }
        else {
            grantPermission();
            return false;
        }
    }

    private boolean stopRecording() {
        mMediaRecorder.stop();
        mMediaRecorder.release();
        toggleStopwatch(false);
        currentFileDuration = getAudioDuration();
        filename.setText(recName);
        filename.setTextSize(14);
        filename.setTextColor(Color.parseColor("#FF757575"));
        return false;
    }

    private void handleRecording() {
        if(!isRecording) isRecording = startRecording();
        else isRecording = stopRecording();

        mAnimationManager.recordAnimation(recButton,waveAnimation,micAnimation, isRecording);
    }

    private void handlePlaying() {
        if(recPath == null && recUri == null)
            Toast.makeText(this, "Please Record or Choose File First", Toast.LENGTH_LONG).show();
        else{
            if (!isPlaying) isPlaying = startPlaying();
            else isPlaying = stopPlaying();

            mAnimationManager.playStopAnimation(playStopButton, playStopAnimation, isPlaying);
        }
    }

    private boolean startPlaying() {
        mMediaPlayer = new MediaPlayer();

        try {
            if(recPath != null) mMediaPlayer.setDataSource(recPath);
            else mMediaPlayer.setDataSource(getApplicationContext(),recUri);


            mMediaPlayer.prepare();

            mMediaPlayer.start();
            handleProgress();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    private void handleProgress() {
        progressBar.setVisibility(View.VISIBLE);
        mTimer = new CountDownTimer(currentFileDuration * 1000,1000) {
            @Override
            public void onTick(long l) {
                updateTimer(currentFileDuration - (int)Math.ceil(l/1000.0));
                progressBar.setPercent(100 - (int)( Math.floor(l/10.0) / currentFileDuration));
            }

            @Override
            public void onFinish() {
                progressBar.setPercent(100);
                updateTimer(currentFileDuration);
                handlePlaying();
            }
        };
        mTimer.start();
    }

    private boolean stopPlaying(){
        if(mMediaPlayer != null) {
            mMediaPlayer.stop();
            mMediaPlayer.release();
            return false;
        }
        return true;
    }

    private void getFile(){
        if(isPlaying)
            Toast.makeText(this, "Please Stop The Current Playing Audio", Toast.LENGTH_LONG).show();
        else{
            recPath = null;
            if (ContextCompat.checkSelfPermission(this,
                    android.Manifest.permission.READ_MEDIA_AUDIO)
                    != PackageManager.PERMISSION_GRANTED)
            {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    ActivityCompat.requestPermissions(MainActivity.this,
                            new String[] {Manifest.permission.READ_MEDIA_AUDIO },
                            2);
                }
                else ActivityCompat.requestPermissions(MainActivity.this,
                        new String[] {Manifest.permission.READ_EXTERNAL_STORAGE },
                        2);
            }
            else {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("audio/*");
                mResultLauncher.launch(intent);
            }
        }

    }

    private boolean checkPermissions(ArrayList<String> Permissions) {
        boolean result = true;
        for (String permission : Permissions) {
            result = result && ContextCompat.checkSelfPermission(this,
                    permission) == PackageManager.PERMISSION_GRANTED ;
        }
        return result ;
    }

    private void grantPermission(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(MainActivity.this,new String[]{
                    Manifest.permission.RECORD_AUDIO, Manifest.permission.READ_MEDIA_AUDIO
            },1);
        }
        else{
            ActivityCompat.requestPermissions(MainActivity.this,new String[]{
                    Manifest.permission.RECORD_AUDIO, Manifest.permission.READ_EXTERNAL_STORAGE
            },1);
        }
    }

    @SuppressLint("Range")
    private String getNameFromUri(Uri uri){
        String fileName = "";
        Cursor cursor = null;
        cursor = getContentResolver().query(uri, new String[]{
                MediaStore.MediaColumns.DISPLAY_NAME
        }, null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            fileName = cursor.getString(cursor.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME));
        }
        if (cursor != null) {
            cursor.close();
        }
        return fileName;
    }

    private void updateTimer(long minutes, long seconds) {
        timer.setText(String.format(Locale.getDefault(),"%02d:%02d",minutes,seconds));
        timerDark.setText(String.format(Locale.getDefault(),"%02d:%02d",minutes,seconds));
        timerLight.setText(String.format(Locale.getDefault(),"%02d:%02d",minutes,seconds));
    }

    private void updateTimer(long duration) {
        long seconds = duration % 60;
        long minutes = duration / 60;
        timer.setText(String.format(Locale.getDefault(),"%02d:%02d",minutes,seconds));
        timerDark.setText(String.format(Locale.getDefault(),"%02d:%02d",minutes,seconds));
        timerLight.setText(String.format(Locale.getDefault(),"%02d:%02d",minutes,seconds));

    }

    private long getAudioDuration(){
        mMediaPlayer = new MediaPlayer();
        try{
            if(recPath != null) mMediaPlayer.setDataSource(recPath);
            else mMediaPlayer.setDataSource(getApplicationContext(),recUri);
            mMediaPlayer.prepare();
            return mMediaPlayer.getDuration() /1000;
        } catch (IOException e) {
            e.printStackTrace();
        } finally{
            mMediaPlayer.release();
        }
        return  0;
    }

    public static byte[] getBytes(Uri uri, Context c) throws IOException {
        InputStream inputStream = null;
        try {
            inputStream = c.getContentResolver().openInputStream(uri);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } finally {
            assert inputStream != null;
            inputStream.close();
        }
        ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();
        int bufferSize = 1024;
        byte[] buffer = new byte[bufferSize];

        int len = 0;
        while (true) {
            try {
                if ((len = inputStream.read(buffer)) == -1) break;
            } catch (IOException e) {
                e.printStackTrace();
            }
            byteBuffer.write(buffer, 0, len);
        }
        return byteBuffer.toByteArray();
    }

    public void getPrediction()
    {
        try {
            File file = null ;
            if (recPath == null)
            {
                String extension = "";

                int i = recUri.getPath().lastIndexOf('.');
                if (i > 0)
                {
                    extension = recUri.getPath().substring(i + 1);
                }
                file= new File(getApplicationContext().getCacheDir().getPath()+"tmp"+extension);
                if(!file.exists()){
                    Log.d("hi","kkk");
                    file.createNewFile();
                } else {
                    System.out.println("Exists");
                }
                FileOutputStream stream = new FileOutputStream(file.getPath());
                stream.write(getBytes(recUri,this));
            }
            else {
                file= new File(recPath);
            }


            RequestBody req = RequestBody.create(MediaType.parse("audio/mpeg"),file);
            MultipartBody.Part audio = MultipartBody.Part.createFormData("file", file.getName(), req);
            Retrofit retrofit = new Retrofit.Builder().baseUrl("https://mohamed41-medo.hf.space").addConverterFactory(GsonConverterFactory.create()).build();
            ApiService apiService = retrofit.create(ApiService.class);
            Call<Response> call = apiService.uploadAudio(audio);
            call.enqueue(new Callback<Response>() {
                @Override
                public void onResponse(Call<Response> call, retrofit2.Response<Response> response) {
                    if (response.isSuccessful()) {
                        // Handle success
                        Response resp = response.body();
                        Log.d("from Testing", resp.getPrediction().toString());
                    } else {
                        // Handle error
                        Log.d("from Testing", "onResponse: error" + response.body());
                    }
                }

                @Override
                public void onFailure(Call<Response> call, Throwable t) {
                    Log.d("from Testing", "onFailure: " + t.getMessage());
                }
            });
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mStopwatch.removeCallbacks(mStopwatchEvent);
    }
}