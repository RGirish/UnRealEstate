
package com.samskrut.omnipresence;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.Cursor;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Vibrator;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import com.google.vrtoolkit.cardboard.CardboardActivity;
import com.google.vrtoolkit.cardboard.CardboardView;
import com.google.vrtoolkit.cardboard.Eye;
import com.google.vrtoolkit.cardboard.HeadTransform;
import com.google.vrtoolkit.cardboard.Viewport;
import com.panframe.android.lib.PFAsset;
import com.panframe.android.lib.PFAssetObserver;
import com.panframe.android.lib.PFAssetStatus;
import com.panframe.android.lib.PFHotspot;
import com.panframe.android.lib.PFHotspotClickListener;
import com.panframe.android.lib.PFNavigationMode;
import com.panframe.android.lib.PFObjectFactory;
import com.panframe.android.lib.PFView;
import java.util.Timer;
import java.util.TimerTask;
import javax.microedition.khronos.egl.EGLConfig;

public class YoutubeStreamActivity extends CardboardActivity implements PFAssetObserver, OnSeekBarChangeListener, PFHotspotClickListener, SensorEventListener, CardboardView.StereoRenderer {

    PFView _pfview;
    PFAsset _pfasset;
    PFNavigationMode _currentNavigationMode = PFNavigationMode.MOTION;
    boolean _updateThumb = true;;
    Timer _scrubberMonitorTimer;
    ViewGroup _frameContainer;
    Button _stopButton;
    Button _playButton;
    Button _touchButton;
    SeekBar _scrubber;
    float X,Y,Z;
    int _projectPos=0,_pos=0;
    private SensorManager senSensorManager;
    private Sensor senAccelerometer;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_youtube);

        senSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        senAccelerometer = senSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        senSensorManager.registerListener(this, senAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);

        Intent intent = getIntent();
        _projectPos = intent.getIntExtra("projectPos",0);
        _pos = intent.getIntExtra("pos",0);

        _frameContainer = (ViewGroup) findViewById(R.id.framecontainer);
        _frameContainer.setBackgroundColor(0xFF000000);

        _playButton = (Button)findViewById(R.id.playbutton);
        _stopButton = (Button)findViewById(R.id.stopbutton);
        _touchButton = (Button)findViewById(R.id.touchbutton);
        _scrubber = (SeekBar)findViewById(R.id.scrubber);

        _playButton.setOnClickListener(playListener);
        _stopButton.setOnClickListener(stopListener);
        _touchButton.setOnClickListener(touchListener);
        _scrubber.setOnSeekBarChangeListener(this);

        _scrubber.setEnabled(false);

        //Text To Speech
        ProjectList.tts.stop();
        Cursor cursor = ProjectList.db.rawQuery("SELECT tts FROM "+Login.USERNAME+"_subProjects WHERE projectPos=" + _projectPos + " AND pos=" + _pos + ";", null);
        cursor.moveToFirst();
        if(android.os.Build.VERSION.SDK_INT >= 21){
            ProjectList.tts.speak(cursor.getString(0), TextToSpeech.QUEUE_FLUSH, null, null);
        }else{
            ProjectList.tts.speak(cursor.getString(0), TextToSpeech.QUEUE_FLUSH, null);
        }
        cursor.close();




        //Start Playing Video

        if (_pfasset == null)
            loadVideo("https://www.youtube.com/watch?v=TA8JprZlDYA");

        if (_pfasset.getStatus() == PFAssetStatus.PLAYING) {
            _pfasset.pause();
        } else {
            if (_pfview != null) {
                _pfview.injectImage(null);
            }
            _pfasset.play();
        }
        showControls(false);

    }

    public void onClick(PFHotspot hotspot) {
        hotspot.animate();
        //hotspot.setEnabled(false);
        Log.d("SimplePlayer", "Hotspot clicked: "+hotspot.getTag());
    }

    public void showControls(boolean bShow)
    {
        int visibility = View.GONE;

        if (bShow)
            visibility = View.VISIBLE;

        _playButton.setVisibility(visibility);
        _stopButton.setVisibility(visibility);
        _touchButton.setVisibility(visibility);
        _scrubber.setVisibility(visibility);

        if (_pfview != null)
        {
            if (!_pfview.supportsNavigationMode(PFNavigationMode.MOTION))
                _touchButton.setVisibility(View.GONE);
        }
    }

    public void loadVideo(String filename)
    {

        _pfview = PFObjectFactory.view(this);
        _pfasset = PFObjectFactory.assetFromUri(this, Uri.parse(filename), this);

        _pfview.displayAsset(_pfasset);
        _pfview.setNavigationMode(_currentNavigationMode);
        _pfview.setMode(2,0);

        _frameContainer.addView(_pfview.getView(), 0);

    }

    public void onStatusMessage(final PFAsset asset, PFAssetStatus status) {
        switch (status)
        {
            case LOADED:
                Log.d("SimplePlayer", "Loaded");
                break;
            case DOWNLOADING:
                Log.d("SimplePlayer", "Downloading 360¡ movie: "+_pfasset.getDownloadProgress()+" percent complete");
                break;
            case DOWNLOADED:
                Log.d("SimplePlayer", "Downloaded to "+asset.getUrl());
                break;
            case DOWNLOADCANCELLED:
                Log.d("SimplePlayer", "Download cancelled");
                break;
            case PLAYING:
                Log.d("SimplePlayer", "Playing");
                getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                _scrubber.setEnabled(true);
                _playButton.setText("pause");
                _scrubberMonitorTimer = new Timer();
                final TimerTask task = new TimerTask() {
                    public void run() {
                        if (_updateThumb)
                        {
                            _scrubber.setMax((int) asset.getDuration());
                            _scrubber.setProgress((int) asset.getPlaybackTime());
                        }
                    }
                };
                _scrubberMonitorTimer.schedule(task, 0, 33);
                break;
            case PAUSED:
                Log.d("SimplePlayer", "Paused");
                _playButton.setText("play");
                break;
            case STOPPED:
                Log.d("SimplePlayer", "Stopped");
                _playButton.setText("play");
                _scrubberMonitorTimer.cancel();
                _scrubberMonitorTimer = null;
                _scrubber.setProgress(0);
                _scrubber.setEnabled(false);
                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                break;
            case COMPLETE:
                Log.d("SimplePlayer", "Complete");
                _playButton.setText("play");
                _scrubberMonitorTimer.cancel();
                _scrubberMonitorTimer = null;
                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                break;
            case ERROR:
                Log.d("SimplePlayer", "Error");
                break;
        }
    }

    private OnClickListener playListener = new OnClickListener() {
        public void onClick(View v) {
            if (_pfasset.getStatus() == PFAssetStatus.PLAYING)
            {
                _pfasset.pause();
            }
            else
                _pfasset.play();
        }
    };

    private OnClickListener stopListener = new OnClickListener() {
        public void onClick(View v) {
            _pfasset.stop();
        }
    };

    private OnClickListener touchListener = new OnClickListener() {
        public void onClick(View v) {
            if (_pfview != null)
            {
                Button touchButton = (Button)findViewById(R.id.touchbutton);
                if (_currentNavigationMode == PFNavigationMode.TOUCH)
                {
                    _currentNavigationMode = PFNavigationMode.MOTION;
                    touchButton.setText("motion");
                }
                else
                {
                    _currentNavigationMode = PFNavigationMode.TOUCH;
                    touchButton.setText("touch");
                }
                _pfview.setNavigationMode(_currentNavigationMode);
            }
        }
    };

    public void onProgressChanged (SeekBar seekbar, int progress, boolean fromUser) {
    }

    public void onStartTrackingTouch(SeekBar seekbar) {
        _updateThumb = false;
    }

    public void onStopTrackingTouch(SeekBar seekbar) {
        _updateThumb = true;
    }


    public void onPause() {
        super.onPause();
        if (_pfasset != null)
        {
            if (_pfasset.getStatus() == PFAssetStatus.PLAYING)
                _pfasset.pause();
        }
        senSensorManager.unregisterListener(this);
    }

    protected void onResume() {
        super.onResume();
        senSensorManager.registerListener(this, senAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    public void onCardboardTrigger(){
        Log.e("onCardboardTrigger", "onCardboardTrigger");
        Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        v.vibrate(100);

        int Xint=(int)X;
        int Yint=(int)Y;
        int Zint=(int)Z;
        Log.e("VALUES", (Xint) + " " + (Yint) + " " + (Zint));

        if((Xint==0 || Xint==1 || Xint==2) && (Yint==0 || Yint==1 || Yint==2) && (Zint==10 || Zint==9 || Zint==8)){
            YoutubeStreamActivity.super.onBackPressed();
            return;
        }

        //goes to the next image
        Cursor cursor2 = ProjectList.db.rawQuery("SELECT COUNT(pos) FROM "+Login.USERNAME+"_subProjects WHERE projectPos=" + _projectPos + ";", null);
        cursor2.moveToFirst();
        int COUNT_2 = cursor2.getInt(0);
        cursor2.close();
        if (_pos + 1 < COUNT_2) {
            Cursor c = ProjectList.db.rawQuery("SELECT mediatype FROM "+Login.USERNAME+"_subProjects WHERE projectPos=" + _projectPos + " AND pos=" + (_pos + 1)+";",null);
            c.moveToFirst();
            String type = c.getString(0);
            c.close();
            if(type.equals("image")){
                Intent intent = new Intent(YoutubeStreamActivity.this, MyVrView.class);
                intent.putExtra("projectPos",_projectPos);
                intent.putExtra("pos", _pos+1);
                startActivity(intent);
                finish();
            } else if (type.equals("video")){
                Intent intent = new Intent(YoutubeStreamActivity.this, MyVrVideoView.class);
                intent.putExtra("projectPos",_projectPos);
                intent.putExtra("pos",_pos+1);
                startActivity(intent);
                finish();
            }
        } else {
            Cursor cursor1 = ProjectList.db.rawQuery("SELECT COUNT(pos) FROM "+Login.USERNAME+"_projects;", null);
            cursor1.moveToFirst();
            int COUNT_1 = cursor1.getInt(0);
            cursor1.close();
            if (_projectPos + 1 < COUNT_1) {
                Cursor c = ProjectList.db.rawQuery("SELECT mediatype FROM "+Login.USERNAME+"_subProjects WHERE projectPos=" + (_projectPos + 1) + " AND pos=0;", null);
                c.moveToFirst();
                String type = c.getString(0);
                if(type.equals("image")){
                    Intent intent = new Intent(YoutubeStreamActivity.this, MyVrView.class);
                    intent.putExtra("projectPos",_projectPos+1);
                    intent.putExtra("pos",0);
                    startActivity(intent);
                    finish();
                }else if(type.equals("video")){
                    Intent intent = new Intent(YoutubeStreamActivity.this, MyVrVideoView.class);
                    intent.putExtra("projectPos",_projectPos+1);
                    intent.putExtra("pos",0);
                    startActivity(intent);
                    finish();
                }
            } else {
                Cursor c = ProjectList.db.rawQuery("SELECT mediatype FROM "+Login.USERNAME+"_subProjects WHERE projectPos=0 AND pos=0;",null);
                c.moveToFirst();
                String type = c.getString(0);
                c.close();
                if(type.equals("image")){
                    Intent intent = new Intent(YoutubeStreamActivity.this, MyVrView.class);
                    intent.putExtra("projectPos",0);
                    intent.putExtra("pos",0);
                    startActivity(intent);
                    finish();
                }else if(type.equals("video")){
                    Intent intent = new Intent(YoutubeStreamActivity.this, MyVrVideoView.class);
                    intent.putExtra("projectPos",0);
                    intent.putExtra("pos",0);
                    startActivity(intent);
                    finish();
                }
            }
        }
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        Sensor mySensor = sensorEvent.sensor;
        if (mySensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            X = sensorEvent.values[0];
            Y = sensorEvent.values[1];
            Z = sensorEvent.values[2];
            //Log.e("ACCELEROMETER",X+" "+Y+" "+Z);
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if ((keyCode == KeyEvent.KEYCODE_VOLUME_UP)){

            //goes to the next image
            Cursor cursor2 = ProjectList.db.rawQuery("SELECT COUNT(pos) FROM "+Login.USERNAME+"_subProjects WHERE projectPos=" + _projectPos + ";", null);
            cursor2.moveToFirst();
            int COUNT_2 = cursor2.getInt(0);
            cursor2.close();
            if (_pos + 1 < COUNT_2) {
                Cursor c = ProjectList.db.rawQuery("SELECT mediatype FROM "+Login.USERNAME+"_subProjects WHERE projectPos="+_projectPos+" AND pos="+(_pos+1)+";",null);
                c.moveToFirst();
                String type = c.getString(0);
                if(type.equals("image")){
                    Intent intent = new Intent(YoutubeStreamActivity.this, MyVrView.class);
                    intent.putExtra("projectPos",_projectPos);
                    intent.putExtra("pos",_pos+1);
                    startActivity(intent);
                    finish();
                }else if(type.equals("video")){
                    Intent intent = new Intent(YoutubeStreamActivity.this, MyVrVideoView.class);
                    intent.putExtra("projectPos",_projectPos);
                    intent.putExtra("pos",_pos+1);
                    startActivity(intent);
                    finish();
                }
            } else {
                Cursor cursor1 = ProjectList.db.rawQuery("SELECT COUNT(pos) FROM "+Login.USERNAME+"_projects;", null);
                cursor1.moveToFirst();
                int COUNT_1 = cursor1.getInt(0);
                cursor1.close();
                if (_projectPos + 1 < COUNT_1) {
                    Cursor c = ProjectList.db.rawQuery("SELECT mediatype FROM "+Login.USERNAME+"_subProjects WHERE projectPos="+(_projectPos+1)+" AND pos=0;",null);
                    c.moveToFirst();
                    String type = c.getString(0);
                    if(type.equals("image")){
                        Intent intent = new Intent(YoutubeStreamActivity.this, MyVrView.class);
                        intent.putExtra("projectPos",_projectPos+1);
                        intent.putExtra("pos",0);
                        startActivity(intent);
                        finish();
                    }else if(type.equals("video")){
                        Intent intent = new Intent(YoutubeStreamActivity.this, MyVrVideoView.class);
                        intent.putExtra("projectPos",_projectPos+1);
                        intent.putExtra("pos",0);
                        startActivity(intent);
                        finish();
                    }
                } else {
                    Cursor c = ProjectList.db.rawQuery("SELECT mediatype FROM "+Login.USERNAME+"_subProjects WHERE projectPos=0 AND pos=0;",null);
                    c.moveToFirst();
                    String type = c.getString(0);
                    if(type.equals("image")){
                        Intent intent = new Intent(YoutubeStreamActivity.this, MyVrView.class);
                        intent.putExtra("projectPos",0);
                        intent.putExtra("pos",0);
                        startActivity(intent);
                        finish();
                    }else if(type.equals("video")){
                        Intent intent = new Intent(YoutubeStreamActivity.this, MyVrVideoView.class);
                        intent.putExtra("projectPos",0);
                        intent.putExtra("pos",0);
                        startActivity(intent);
                        finish();
                    }
                }
            }
        } else{
            //goes back
            super.onBackPressed();
        }
        return true;
    }


    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    @Override
    public void onNewFrame(HeadTransform headTransform) {}

    @Override
    public void onDrawEye(Eye eye) {}

    @Override
    public void onFinishFrame(Viewport viewport) {}

    @Override
    public void onSurfaceChanged(int i, int i1) {}

    @Override
    public void onSurfaceCreated(EGLConfig eglConfig) {}

    @Override
    public void onRendererShutdown() {}

}