/**
 * For 360 Videos we are using the library PanFrame. (Check out panframe.com)
 * 80% of this code is from the library itself. I've modified and added a couple of more lines as per our need.
 */
package com.samskrut.omnipresence;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.BitmapFactory;
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
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import com.google.vrtoolkit.cardboard.CardboardActivity;
import com.google.vrtoolkit.cardboard.CardboardView;
import com.google.vrtoolkit.cardboard.Eye;
import com.google.vrtoolkit.cardboard.HeadTransform;
import com.google.vrtoolkit.cardboard.Viewport;
import com.panframe.android.lib.PFAsset;
import com.panframe.android.lib.PFAssetObserver;
import com.panframe.android.lib.PFAssetStatus;
import com.panframe.android.lib.PFNavigationMode;
import com.panframe.android.lib.PFObjectFactory;
import com.panframe.android.lib.PFView;
import javax.microedition.khronos.egl.EGLConfig;

public class MyVrVideoView extends CardboardActivity implements PFAssetObserver, SensorEventListener, CardboardView.StereoRenderer {

	PFView _pfview;
	PFAsset _pfasset;
    PFNavigationMode _currentNavigationMode = PFNavigationMode.MOTION;
    ViewGroup _frameContainer;

    int _projectPos=0,_pos=0;

    //Using the SensorManager class, we can detect the coordinates of the phone in space, and using the coords
    //we can determine the device orientation too- whether it's looking down or up or 45deg or whatever.
    private SensorManager senSensorManager;
    private Sensor senAccelerometer;
    //To keep track of the orientation of the device in space
    float X,Y,Z;

	public void onCreate(Bundle savedInstanceState) {        
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);                
        setContentView(R.layout.vr_video);

        //Initialize the SensorManager object- we are gonna use the sensor ACCELEROMETER.
        senSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        senAccelerometer = senSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        senSensorManager.registerListener(this, senAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);

        Intent intent = getIntent();
        _projectPos = intent.getIntExtra("projectPos",0);
        _pos = intent.getIntExtra("pos",0);

        _frameContainer = (ViewGroup) findViewById(R.id.framecontainer);

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
            loadVideo(getFilesDir().getAbsolutePath()+"/"+Login.USERNAME+"_"+_projectPos+"_"+_pos+".mp4");

        if (_pfasset.getStatus() == PFAssetStatus.PLAYING) {
            _pfasset.pause();
        } else {
            if (_pfview != null) {
                _pfview.injectImage(null);
            }
            _pfasset.play();
        }
    }

    /**
     * Initializes the video player view and sets the parameters.
     */
    public void loadVideo(String filename){

        _pfview = PFObjectFactory.view(this);
        _pfasset = PFObjectFactory.assetFromUri(this, Uri.parse(filename), this);
        _pfview.displayAsset(_pfasset);
        _pfview.setNavigationMode(_currentNavigationMode);
        //Mode number '2' is the side-by-side view.
        _pfview.setMode(2,0);
        _frameContainer.addView(_pfview.getView(), 0);

        //To keep the screen from turning OFF or going dim.
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

	public void onStatusMessage(final PFAsset asset, PFAssetStatus status) {}

    /**
     * Clear the window SCREEN_ON flag when the activity is exiting.
     */
    public void onDestroy() {
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        super.onDestroy();
    }

    /**
     * Clear the window SCREEN_ON flag when the activity is exiting.
     * Pause the video when the activity is exiting.
     * Unregister the listener in SensorManager.
     */
    public void onPause() {
        super.onPause();
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        if (_pfasset != null)
        {
	        if (_pfasset.getStatus() == PFAssetStatus.PLAYING)
	        	_pfasset.pause();
        }
        senSensorManager.unregisterListener(this);
    }

    /**
     * Clear the window SCREEN_ON flag when the activity is exiting.
     * Register the listener in SensorManager.
     */
    protected void onResume() {
        super.onResume();
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        senSensorManager.registerListener(this, senAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
    }

    /**
     * This function is called when the magnetic trigger is pulled in the Cardboard headset.
     * We first calculate the position and orientation of the device using the Sensor listener.
     * If it is looking down, we need to exit the activity and go back to the list.
     * If not, we need to move to the next image/video in the list.
     */
	@Override
    public void onCardboardTrigger(){
        Log.e("onCardboardTrigger", "onCardboardTrigger");

        //Vibrate for one tenth of a second- a haptic feedback ensuring that the function has been entered.
        Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        v.vibrate(100);

        //X, Y and Z will always have the current device coords.
        int Xint=(int)X;
        int Yint=(int)Y;
        int Zint=(int)Z;
        Log.e("VALUES", (Xint) + " " + (Yint) + " " + (Zint));

        //If the device is found to be looking down when the trigger was pulled, we go back and exit the function.
        if((Xint==0 || Xint==1 || Xint==2) && (Yint==0 || Yint==1 || Yint==2) && (Zint==10 || Zint==9 || Zint==8)){
            MyVrVideoView.super.onBackPressed();
            return;
        }

        //Goes to the next image, if the device hasn't been looking down when the trigger was pulled.
        Cursor cursor2 = ProjectList.db.rawQuery("SELECT COUNT(pos) FROM "+Login.USERNAME+"_subProjects WHERE projectPos=" + _projectPos + ";", null);
        cursor2.moveToFirst();
        int COUNT_2 = cursor2.getInt(0);
        cursor2.close();
        //If the current subproject isn't the last one in the current project, we just move on to the next subproject within the same project
        if (_pos + 1 < COUNT_2) {
            Cursor c = ProjectList.db.rawQuery("SELECT mediatype FROM "+Login.USERNAME+"_subProjects WHERE projectPos="+_projectPos+" AND pos="+(_pos+1)+";",null);
            c.moveToFirst();
            String type = c.getString(0);
            c.close();
            //If mediaType is 'image', we open the activity 'MyVrView'.
            if(type.equals("image")){
                Intent intent = new Intent(MyVrVideoView.this, MyVrView.class);
                intent.putExtra("projectPos",_projectPos);
                intent.putExtra("pos",_pos+1);
                startActivity(intent);
                finish();
            }
            //If mediaType is 'video', we open the activity 'MyVrVideoView'.
            else if(type.equals("video")){
                Intent intent = new Intent(MyVrVideoView.this, MyVrVideoView.class);
                intent.putExtra("projectPos",_projectPos);
                intent.putExtra("pos",_pos+1);
                startActivity(intent);
                finish();
            }
        }//If it's the last image/video in the current project, we move on to the first subproject of the next project.
        else {
            Cursor cursor1 = ProjectList.db.rawQuery("SELECT COUNT(pos) FROM "+Login.USERNAME+"_projects;", null);
            cursor1.moveToFirst();
            int COUNT_1 = cursor1.getInt(0);
            cursor1.close();
            if (_projectPos + 1 < COUNT_1) {
                Cursor c = ProjectList.db.rawQuery("SELECT mediatype FROM "+Login.USERNAME+"_subProjects WHERE projectPos=" + (_projectPos + 1) + " AND pos=0;", null);
                c.moveToFirst();
                String type = c.getString(0);
                if(type.equals("image")){
                    Intent intent = new Intent(MyVrVideoView.this, MyVrView.class);
                    intent.putExtra("projectPos",_projectPos+1);
                    intent.putExtra("pos",0);
                    startActivity(intent);
                    finish();
                }else if(type.equals("video")){
                    Intent intent = new Intent(MyVrVideoView.this, MyVrVideoView.class);
                    intent.putExtra("projectPos",_projectPos+1);
                    intent.putExtra("pos",0);
                    startActivity(intent);
                    finish();
                }
            }//If it is the last project itself, we move on to the first subproject of the first project.
            else {
                Cursor c = ProjectList.db.rawQuery("SELECT mediatype FROM "+Login.USERNAME+"_subProjects WHERE projectPos=0 AND pos=0;",null);
                c.moveToFirst();
                String type = c.getString(0);
                c.close();
                if(type.equals("image")){
                    Intent intent = new Intent(MyVrVideoView.this, MyVrView.class);
                    intent.putExtra("projectPos",0);
                    intent.putExtra("pos",0);
                    startActivity(intent);
                    finish();
                }else if(type.equals("video")){
                    Intent intent = new Intent(MyVrVideoView.this, MyVrVideoView.class);
                    intent.putExtra("projectPos",0);
                    intent.putExtra("pos",0);
                    startActivity(intent);
                    finish();
                }
            }
        }
    }

    /**
     * Even for the slightest change in the device's position or orientation, this function is called by the SensorManager.
     * Inside the function, we are constantly updating the device coords in the variables X,Y and Z.
     */
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

    /**
     * This function is called whenever any kind of a key is pressed on the device- back, home, volume, lock, etc.
     * We are interested only with the volume buttons.
     */
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        //If the Volume Up button is pressed, we need to move to the next image in line.
        if ((keyCode == KeyEvent.KEYCODE_VOLUME_UP)){

            //Goes to the next image
            Cursor cursor2 = ProjectList.db.rawQuery("SELECT COUNT(pos) FROM "+Login.USERNAME+"_subProjects WHERE projectPos=" + _projectPos + ";", null);
            cursor2.moveToFirst();
            int COUNT_2 = cursor2.getInt(0);
            cursor2.close();
            if (_pos + 1 < COUNT_2) {
                Cursor c = ProjectList.db.rawQuery("SELECT mediatype FROM subProjects WHERE "+Login.USERNAME+"_projectPos="+_projectPos+" AND pos="+(_pos+1)+";",null);
                c.moveToFirst();
                String type = c.getString(0);
                if(type.equals("image")){
                    Intent intent = new Intent(MyVrVideoView.this, MyVrView.class);
                    intent.putExtra("projectPos",_projectPos);
                    intent.putExtra("pos",_pos+1);
                    startActivity(intent);
                    finish();
                }else if(type.equals("video")){
                    Intent intent = new Intent(MyVrVideoView.this, MyVrVideoView.class);
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
                        Intent intent = new Intent(MyVrVideoView.this, MyVrView.class);
                        intent.putExtra("projectPos",_projectPos+1);
                        intent.putExtra("pos",0);
                        startActivity(intent);
                        finish();
                    }else if(type.equals("video")){
                        Intent intent = new Intent(MyVrVideoView.this, MyVrVideoView.class);
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
                        Intent intent = new Intent(MyVrVideoView.this, MyVrView.class);
                        intent.putExtra("projectPos",0);
                        intent.putExtra("pos",0);
                        startActivity(intent);
                        finish();
                    }else if(type.equals("video")){
                        Intent intent = new Intent(MyVrVideoView.this, MyVrVideoView.class);
                        intent.putExtra("projectPos",0);
                        intent.putExtra("pos",0);
                        startActivity(intent);
                        finish();
                    }
                }
            }
        }//If the Volume Down button is pressed, we have to go back to the list.
        else{
            //Goes back.
            super.onBackPressed();
        }
        return true;
    }


    /*
        These functions need to be declared because we're implementing CardboardView.StereoRenderer.
        The function onCardboardTriggered() doesn't work without implementing CardboardView.StereoRenderer.
    */

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