
package com.samskrut.omnipresence;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Handler;
import android.os.Vibrator;
import android.speech.tts.TextToSpeech;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Toast;
import com.google.vrtoolkit.cardboard.CardboardActivity;
import com.google.vrtoolkit.cardboard.CardboardView;
import com.google.vrtoolkit.cardboard.Eye;
import com.google.vrtoolkit.cardboard.HeadTransform;
import com.google.vrtoolkit.cardboard.Viewport;
import java.io.File;
import java.io.InputStream;
import java.util.Locale;
import javax.microedition.khronos.egl.EGLConfig;

public class ProjectList extends CardboardActivity implements TextToSpeech.OnInitListener,CardboardView.StereoRenderer, SensorEventListener {

    public static SQLiteDatabase db;
    public static int projectCount=0;
    public static TextToSpeech tts;
    ScrollView mainScrollView;
    LinearLayout mainll;
    static int currentProject;
    int d=0;
    int Xint,Yint,Zint;
    private SensorManager senSensorManager;
    private Sensor senAccelerometer;
    boolean FLAG=true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        overridePendingTransition(R.anim.slide_up, R.anim.slide_down);
        setContentView(R.layout.activity_project_list);

        senSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        senAccelerometer = senSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        senSensorManager.registerListener(this, senAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);

        //This variable maintains the current project that is in display on screen.
        currentProject = 0;

        //To go fullscreen, hide the navigation bar and the status bar.
        View decorView = getWindow().getDecorView();
        int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_FULLSCREEN;
        decorView.setSystemUiVisibility(uiOptions);
        setFullscreen(true);

        mainScrollView = (ScrollView)findViewById(R.id.parent);
        mainll = (LinearLayout)findViewById(R.id.mainll);
        //Since we are going to let the user scroll through the volume buttons, we need to enable smoothscrolling.
        mainScrollView.setSmoothScrollingEnabled(true);

        //Whenever the scroll level changes, we need to keep track of what project is in display on screen currently and update the variable.
        mainScrollView.getViewTreeObserver().addOnScrollChangedListener(new ViewTreeObserver.OnScrollChangedListener() {
            @Override
            public void onScrollChanged() {
                int scrollYPx = mainScrollView.getScrollY();
                int scrollYDp = pxToDp(scrollYPx);
                currentProject = scrollYDp/360;
            }
        });

        db = openOrCreateDatabase("omniPresence.db",SQLiteDatabase.CREATE_IF_NECESSARY, null);

        displayEverything();

        tts = new TextToSpeech(this,this);
    }

    /**
     * Converts the given dp value to its equivalent in px units.
     */
    public int dpToPx(int dp) {
        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        return (int)((dp * displayMetrics.density) + 0.5);
    }

    /**
     * Converts the given px value to its equivalent in dp units.
     */
    public int pxToDp(int px) {
        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        return (int) ((px/displayMetrics.density)+0.5);
    }

    /**
     * When Volume UP is pressed once, we need to scroll to the next project in line.
     * When Volume DOWN is pressed twice, we need to open the currently displayed project.
     */
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if ((keyCode == KeyEvent.KEYCODE_VOLUME_UP)){

            d++;
            Handler handler = new Handler();
            Runnable r = new Runnable() {
                @Override
                public void run() {
                    //Single Click
                    if (d == 1){
                        //Scroll Down to next project in list
                        Cursor cursor = ProjectList.db.rawQuery("SELECT COUNT(pos) FROM "+Login.USERNAME+"_projects;", null);
                        cursor.moveToFirst();
                        int COUNT = cursor.getInt(0);
                        cursor.close();
                        if(currentProject+1<COUNT) {
                            currentProject++;
                            int scrollYPx = mainScrollView.getScrollY();
                            int scrollYDp = pxToDp(scrollYPx);

                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
                            {
                                ValueAnimator realSmoothScrollAnimation =
                                        ValueAnimator.ofInt(mainScrollView.getScrollY(), mainScrollView.getScrollY() + dpToPx(360 - scrollYDp % 360));
                                realSmoothScrollAnimation.setDuration(800);
                                realSmoothScrollAnimation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener(){
                                    @Override
                                    public void onAnimationUpdate(ValueAnimator animation)
                                    {
                                        int scrollTo = (Integer) animation.getAnimatedValue();
                                        mainScrollView.scrollTo(0, scrollTo);
                                    }
                                });

                                realSmoothScrollAnimation.start();
                            }
                            else{
                                mainScrollView.smoothScrollBy(0, dpToPx(360 - scrollYDp % 360));
                            }

                        }else{
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
                            {
                                ValueAnimator realSmoothScrollAnimation =
                                        ValueAnimator.ofInt(mainScrollView.getScrollY(), 0);
                                realSmoothScrollAnimation.setDuration(800);
                                realSmoothScrollAnimation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener(){
                                    @Override
                                    public void onAnimationUpdate(ValueAnimator animation)
                                    {
                                        int scrollTo = (Integer) animation.getAnimatedValue();
                                        mainScrollView.scrollTo(0, scrollTo);
                                    }
                                });

                                realSmoothScrollAnimation.start();
                            }
                            else{
                                mainScrollView.smoothScrollTo(0,0);
                            }
                        }
                    }
                    //Double Click
                    if (d == 2){
                        //Open current project in list
                        Cursor c = db.rawQuery("SELECT mediatype FROM "+Login.USERNAME+"_subProjects WHERE projectPos="+currentProject+" AND pos=0;",null);
                        c.moveToFirst();
                        String type = c.getString(0);
                        if(type.equals("image")){
                            Intent intent = new Intent(ProjectList.this, MyVrView.class);
                            intent.putExtra("projectPos",currentProject);
                            intent.putExtra("pos",0);
                            startActivity(intent);
                        }else if(type.equals("video")){
                            Intent intent = new Intent(ProjectList.this, MyVrVideoView.class);
                            intent.putExtra("projectPos",currentProject);
                            intent.putExtra("pos",0);
                            startActivity(intent);
                        }
                    }
                    d = 0;
                }
            };
            if (d == 1) {
                handler.postDelayed(r, 500);
            }
        }else if((keyCode == KeyEvent.KEYCODE_BACK)){
            super.onBackPressed();
        }
        return true;
    }

    protected void onPause() {
        super.onPause();
        senSensorManager.unregisterListener(this);
    }

    @Override
    public void onResume(){
        super.onResume();
        senSensorManager.registerListener(this, senAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        tts.stop();
        View decorView = getWindow().getDecorView();
        if (android.os.Build.VERSION.SDK_INT >= 19) {
            decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        }else{
            decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_FULLSCREEN);
        }
    }

    public void displayEverything(){

        LinearLayout mainll = (LinearLayout)findViewById(R.id.mainll);
        mainll.removeAllViews();

        Cursor cursor = db.rawQuery("SELECT pos FROM "+Login.USERNAME+"_projects ORDER BY pos;",null);
        try{
            cursor.moveToFirst();
            while(true){
                projectCount++;
                final int projectPos = cursor.getInt(0);

                LinearLayout ll = new LinearLayout(this);
                ll.setOrientation(LinearLayout.VERTICAL);
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                ll.setPadding(0, 0, 0, (int) getResources().getDimension(R.dimen.dp20));
                ll.setLayoutParams(params);
                ll.setBackgroundColor(Color.BLACK);
                ll.setGravity(Gravity.CENTER_HORIZONTAL);

                ImageView imageButton = new ImageView(this);
                imageButton.setTag("project" + projectPos);
                InputStream is = openFileInput(Login.USERNAME+"_" + projectPos + "_th.jpg");
                Bitmap bitmap = BitmapFactory.decodeStream(is);
                imageButton.setImageBitmap(bitmap);
                params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (int)getResources().getDimension(R.dimen.dp330));
                params.setMargins(0, (int) getResources().getDimension(R.dimen.dp5), 0, (int) getResources().getDimension(R.dimen.dp5));
                imageButton.setLayoutParams(params);
                imageButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Cursor c = db.rawQuery("SELECT mediatype FROM "+Login.USERNAME+"_subProjects WHERE projectPos="+projectPos+" AND pos=0;",null);
                        c.moveToFirst();
                        String type = c.getString(0);
                        if(type.equals("image")){
                            Intent intent = new Intent(ProjectList.this, MyVrView.class);
                            intent.putExtra("projectPos",projectPos);
                            intent.putExtra("pos",0);
                            startActivity(intent);
                        }else if(type.equals("video")){
                            Intent intent = new Intent(ProjectList.this, MyVrVideoView.class);
                            intent.putExtra("projectPos",projectPos);
                            intent.putExtra("pos",0);
                            startActivity(intent);
                        }else if(type.equals("youtube")){
                            Intent intent = new Intent(ProjectList.this, YoutubeStreamActivity.class);
                            intent.putExtra("projectPos",projectPos);
                            intent.putExtra("pos",0);
                            startActivity(intent);
                        }

                    }
                });
                ll.addView(imageButton);

                mainll.addView(ll);

                cursor.moveToNext();
                if(cursor.isAfterLast()){
                    break;
                }
            }
        }catch (Exception e){
            Log.e("JONNNNNNN","SSNOOOWWWWWWWWWWWWWWWW");
            Log.e(e.toString(),e.getMessage());
        }

        cursor.close();


    }

    private void setFullscreen(boolean fullscreen) {
        WindowManager.LayoutParams attrs = getWindow().getAttributes();
        if (fullscreen) {
            attrs.flags |= WindowManager.LayoutParams.FLAG_FULLSCREEN;
        }
        else{
            attrs.flags &= ~WindowManager.LayoutParams.FLAG_FULLSCREEN;
        }
        getWindow().setAttributes(attrs);
    }

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            int result = tts.setLanguage(Locale.US);
            tts.setSpeechRate(0.75f);
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED){
                Toast.makeText(this,"This Language is not supported!",Toast.LENGTH_LONG).show();
            }
        }else{
            Toast.makeText(this,"TTS Initialization Failed!",Toast.LENGTH_LONG).show();
        }
    }

    public boolean exists(String fname){
        File file = getBaseContext().getFileStreamPath(fname);
        return file.exists();
    }

    @Override
    protected void onDestroy() {
        if(tts != null) {
            tts.stop();
            tts.shutdown();
        }
        super.onDestroy();
    }

    @Override
    public void onCardboardTrigger(){

        if(!((Xint == 6 || Xint == 7 || Xint == 8) && (Yint == -2 || Yint == -1 || Yint == 0 || Yint == 1 || Yint == 2) && (Zint == 6 || Zint == 7 || Zint == 8)) && !((Xint == 6 || Xint == 7 || Xint == 8) && (Yint == -2 || Yint == -1 || Yint == 0 || Yint == 1 || Yint == 2) && (Zint == -6 || Zint == -7 || Zint == -8))){
            Cursor c = db.rawQuery("SELECT mediatype FROM "+Login.USERNAME+"_subProjects WHERE projectPos="+currentProject+" AND pos=0;",null);
            c.moveToFirst();
            String type = c.getString(0);
            if(type.equals("image")){
                Intent intent = new Intent(ProjectList.this, MyVrView.class);
                intent.putExtra("projectPos",currentProject);
                intent.putExtra("pos",0);
                startActivity(intent);
            }else if(type.equals("video")){
                Intent intent = new Intent(ProjectList.this, MyVrVideoView.class);
                intent.putExtra("projectPos",currentProject);
                intent.putExtra("pos",0);
                startActivity(intent);
            }
        }
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {

        Sensor mySensor = sensorEvent.sensor;
        if (mySensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            Xint = (int) sensorEvent.values[0];
            Yint = (int) sensorEvent.values[1];
            Zint = (int) sensorEvent.values[2];

            //Log.e("COORDINATES", Xint + " " + Yint + " " + Zint);

            if(FLAG){

                if (((Xint == 6 || Xint == 7 || Xint == 8) && (Yint == -2 || Yint == -1 || Yint == 0 || Yint == 1 || Yint == 2) && (Zint == 6 || Zint == 7 || Zint == 8))) {

                    Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                    v.vibrate(200);

                    Cursor cursor = ProjectList.db.rawQuery("SELECT COUNT(pos) FROM "+Login.USERNAME+"_projects;", null);
                    cursor.moveToFirst();
                    int COUNT = cursor.getInt(0);
                    cursor.close();
                    if (currentProject + 1 < COUNT) {
                        currentProject++;
                        int scrollYPx = mainScrollView.getScrollY();
                        int scrollYDp = pxToDp(scrollYPx);

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
                        {
                            ValueAnimator realSmoothScrollAnimation =
                                    ValueAnimator.ofInt(mainScrollView.getScrollY(), mainScrollView.getScrollY() + dpToPx(360 - scrollYDp % 360));
                            realSmoothScrollAnimation.setDuration(800);
                            realSmoothScrollAnimation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener(){
                                @Override
                                public void onAnimationUpdate(ValueAnimator animation)
                                {
                                    int scrollTo = (Integer) animation.getAnimatedValue();
                                    mainScrollView.scrollTo(0, scrollTo);
                                }
                            });

                            realSmoothScrollAnimation.start();
                        }
                        else{
                            mainScrollView.smoothScrollBy(0, dpToPx(360 - scrollYDp % 360));
                        }


                    } else {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
                        {
                            ValueAnimator realSmoothScrollAnimation = ValueAnimator.ofInt(mainScrollView.getScrollY(), 0);
                            realSmoothScrollAnimation.setDuration(800);
                            realSmoothScrollAnimation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener(){
                                @Override
                                public void onAnimationUpdate(ValueAnimator animation)
                                {
                                    int scrollTo = (Integer) animation.getAnimatedValue();
                                    mainScrollView.scrollTo(0, scrollTo);
                                }
                            });

                            realSmoothScrollAnimation.start();
                        }
                        else{
                            mainScrollView.smoothScrollTo(0,0);
                        }
                    }
                    FLAG = false;

                }else if (((Xint == 6 || Xint == 7 || Xint == 8) && (Yint == -2 || Yint == -1 || Yint == 0 || Yint == 1 || Yint == 2) && (Zint == -6 || Zint == -7 || Zint == -8))) {

                    Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                    v.vibrate(200);

                    if (currentProject - 1 >= 0) {
                        currentProject--;
                        int scrollYPx = mainScrollView.getScrollY();
                        int scrollYDp = pxToDp(scrollYPx);
                        int val = mainScrollView.getScrollY() - dpToPx(scrollYDp % 360);
                        if(dpToPx(scrollYDp % 360)==0)val=mainScrollView.getScrollY()-dpToPx(360);

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
                        {
                            ValueAnimator realSmoothScrollAnimation =
                                    ValueAnimator.ofInt(mainScrollView.getScrollY(), val);
                            realSmoothScrollAnimation.setDuration(800);
                            realSmoothScrollAnimation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener(){
                                @Override
                                public void onAnimationUpdate(ValueAnimator animation) {
                                    int scrollTo = (Integer) animation.getAnimatedValue();
                                    mainScrollView.scrollTo(0, scrollTo);
                                }
                            });
                            realSmoothScrollAnimation.start();
                        }else{
                            mainScrollView.smoothScrollTo(0, mainScrollView.getScrollY() - dpToPx(scrollYDp % 360));
                        }
                    }else{
                        Cursor cursor = ProjectList.db.rawQuery("SELECT MAX(pos) FROM "+Login.USERNAME+"_projects;", null);
                        cursor.moveToFirst();
                        int MAX = cursor.getInt(0);
                        cursor.close();
                        currentProject = MAX;
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
                        {
                            ValueAnimator realSmoothScrollAnimation = ValueAnimator.ofInt(mainScrollView.getScrollY(), dpToPx(360*(currentProject)));
                            realSmoothScrollAnimation.setDuration(800);
                            realSmoothScrollAnimation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener(){
                                @Override
                                public void onAnimationUpdate(ValueAnimator animation)
                                {
                                    int scrollTo = (Integer) animation.getAnimatedValue();
                                    mainScrollView.scrollTo(0, scrollTo);
                                }
                            });

                            realSmoothScrollAnimation.start();
                        }
                        else{
                            mainScrollView.smoothScrollTo(0, dpToPx(360*(currentProject)));
                        }
                    }
                    FLAG = false;
                }

            }else{
                if(   (Xint == 8 || Xint == 9 || Xint == 10) && (Yint == -2 || Yint == -1 || Yint == 0 || Yint == 1 || Yint == 2) && (Zint == -1 || Zint == 0 || Zint == 1)   ){
                    FLAG = true;
                }
            }
        }
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