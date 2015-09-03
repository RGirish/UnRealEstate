package com.samskrut.omnipresence;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import com.parse.FindCallback;
import com.parse.GetDataCallback;
import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Splash extends AppCompatActivity implements SwipeRefreshLayout.OnRefreshListener{

    ProgressDialog dialog1;
    SQLiteDatabase db;
    SwipeRefreshLayout swipeLayout;
    int COUNT_th=0,CURR_COUNT_th=0;
    ArrayList<Integer> notAvailableList_th,toBeDeletedList_th;
    int COUNT=0,CURR_COUNT=0;
    ArrayList<String> notAvailableList,toBeDeletedList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.splash);
        setFullscreen(true);
        db = openOrCreateDatabase("omniPresence.db",SQLiteDatabase.CREATE_IF_NECESSARY, null);
        swipeLayout = (SwipeRefreshLayout) findViewById(R.id.swipe_container);
        swipeLayout.setOnRefreshListener(this);
        swipeLayout.setColorScheme(android.R.color.black);

        checkForDownload();
        display();

        (findViewById(R.id.logout)).setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    ((TextView) v).setTextColor(Color.parseColor("#aaffffff"));
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    ((TextView) v).setTextColor(Color.parseColor("#ffffff"));
                    SQLiteDatabase db = openOrCreateDatabase("omniPresence.db", SQLiteDatabase.CREATE_IF_NECESSARY, null);
                    db.execSQL("UPDATE session SET projectsTableName='NONE',subProjectsTableName='NONE';");
                    db.close();
                    Intent mainIntent = new Intent(Splash.this, Login.class);
                    mainIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(mainIntent);
                    Toast.makeText(Splash.this, "Logged out!", Toast.LENGTH_LONG).show();
                    finish();
                } else if (event.getAction() == MotionEvent.ACTION_CANCEL) {
                    ((TextView) v).setTextColor(Color.parseColor("#ffffff"));
                }
                return true;
            }
        });

        (findViewById(R.id.instructions)).setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    ((TextView) v).setTextColor(Color.parseColor("#aaffffff"));
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    ((TextView) v).setTextColor(Color.parseColor("#ffffff"));
                    startActivity(new Intent(Splash.this, Instructions.class));
                } else if (event.getAction() == MotionEvent.ACTION_CANCEL) {
                    ((TextView) v).setTextColor(Color.parseColor("#ffffff"));
                }
                return true;
            }
        });

        (findViewById(R.id.viewVirtualTours)).setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    ((TextView) v).setTextColor(Color.parseColor("#aaffffff"));
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    ((TextView) v).setTextColor(Color.parseColor("#ffffff"));
                    Intent mainIntent = new Intent(Splash.this, Countdown.class);
                    startActivity(mainIntent);
                } else if (event.getAction() == MotionEvent.ACTION_CANCEL) {
                    ((TextView) v).setTextColor(Color.parseColor("#ffffff"));
                }
                return true;
            }
        });

    }

    @Override
    protected void onResume(){
        super.onResume();
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    @Override
    protected void onPause(){
        super.onPause();
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    @Override
    protected void onDestroy(){
        super.onDestroy();
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    public void checkForDownload(){
        Cursor cursor = db.rawQuery("SELECT COUNT(pos) FROM "+Login.USERNAME+"_projects;", null);
        cursor.moveToFirst();
        int count = cursor.getInt(0);
        if(count == 0){
            if(checkConnection()){
                download();
            }else{
                Toast.makeText(this,"Please check your Internet Connection!",Toast.LENGTH_LONG).show();
                (findViewById(R.id.logout)).setVisibility(View.GONE);
                (findViewById(R.id.instructions)).setVisibility(View.GONE);
                (findViewById(R.id.viewVirtualTours)).setVisibility(View.GONE);
            }
        }
        cursor.close();
    }

    public void download(){
        dialog1 = ProgressDialog.show(this,null,"Downloading data...");
        Log.e("download", "download");

        final ParseQuery<ParseObject> query = ParseQuery.getQuery(Login.PROJECTS_TABLE_NAME);
        query.orderByAscending("pos");
        query.findInBackground(new FindCallback<ParseObject>() {
            public void done(List<ParseObject> objects, ParseException e) {
                if (e == null) {
                    for (ParseObject ob : objects) {
                        db.execSQL("INSERT INTO "+Login.USERNAME+"_projects_temp VALUES(" + ob.getNumber("pos") + ",'" + ob.getUpdatedAt() + "');");
                        Log.e("QUERY","INSERT INTO "+Login.USERNAME+"_projects_temp VALUES(" + ob.getNumber("pos") + ",'" + ob.getUpdatedAt() + "');");
                    }

                    final ParseQuery<ParseObject> query = ParseQuery.getQuery(Login.SUBPROJECTS_TABLE_NAME);
                    query.orderByAscending("projectPos");
                    query.addAscendingOrder("pos");
                    query.selectKeys(Arrays.asList("projectPos", "pos", "tts", "mediaType"));
                    query.findInBackground(new FindCallback<ParseObject>() {
                        public void done(List<ParseObject> objects, ParseException e) {
                            if (e == null) {
                                for (ParseObject ob : objects) {
                                    db.execSQL("INSERT INTO " + Login.USERNAME + "_subProjects_temp VALUES(" + ob.getNumber("projectPos") + ",'" + ob.getNumber("pos") + "','" + ob.getString("tts") + "','" + ob.getString("mediaType") + "','" + ob.getUpdatedAt() + "');");
                                    Log.e("QUERY", "INSERT INTO " + Login.USERNAME + "_subProjects_temp VALUES(" + ob.getNumber("projectPos") + ",'" + ob.getNumber("pos") + "','" + ob.getString("tts") + "','" + ob.getString("mediaType") + "','" + ob.getUpdatedAt() + "');");
                                }
                                db.execSQL("DELETE FROM login;");
                                final ParseQuery<ParseObject> query = ParseQuery.getQuery("Login");
                                query.findInBackground(new FindCallback<ParseObject>() {
                                    public void done(List<ParseObject> objects, ParseException e) {
                                        if (e == null) {
                                            for (ParseObject ob : objects) {
                                                db.execSQL("INSERT INTO login VALUES('" + ob.getString("username") + "','" + ob.getString("password") + "','"+ ob.getString("projectsTableName") +"','"+ ob.getString("subProjectsTableName") +"');");
                                                Log.e("QUERY","INSERT INTO login VALUES('" + ob.getString("username") + "','" + ob.getString("password") + "','"+ ob.getString("projectsTableName") +"','"+ ob.getString("subProjectsTableName") +"');");
                                            }
                                            downloadSplashImage();
                                        } else {
                                            Log.e("PARSE", "Error: " + e.getMessage());
                                        }
                                    }
                                });
                            } else {
                                Log.e("PARSE", "Error: " + e.getMessage());
                            }
                        }
                    });

                } else {
                    Log.e("PARSE", "Error: " + e.getMessage());
                }
            }
        });
    }

    public void downloadSplashImage() {
        Log.e("downloadSplashImage","downloadSplashImage");

        dialog1.setMessage("Downloading Splash Image...");
        ParseQuery<ParseObject> query = ParseQuery.getQuery("Splash");
        query.whereEqualTo("username", Login.USERNAME);
        query.findInBackground(new FindCallback<ParseObject>() {
            public void done(List<ParseObject> objects, ParseException e) {
                if (e == null) {
                    ParseFile myFile = objects.get(0).getParseFile("image");
                    myFile.getDataInBackground(new GetDataCallback() {
                        public void done(byte[] data, ParseException e) {
                            if (e == null) {
                                writeFile(data, Login.USERNAME+"_splash.jpg");
                                downloadProjectsThumbnails();
                            } else {
                                Log.e("Something went wrong", "Something went wrong");
                            }
                        }
                    });
                } else {
                    Log.e("PARSE", "Error: " + e.getMessage());
                }
            }
        });
    }

    public void downloadProjectsThumbnails() {

        Log.e("downloadProjectsTh","downloadProjectsTh");

        //SET NOTAVAILABLELIST FOR PROJECTS
        Cursor cursor = db.rawQuery("SELECT COUNT(pos) FROM "+Login.USERNAME+"_projects_temp;",null);
        cursor.moveToFirst();
        COUNT_th = cursor.getInt(0);
        notAvailableList_th = new ArrayList<>(COUNT_th);
        toBeDeletedList_th = new ArrayList<>(COUNT_th);
        cursor.close();

        cursor = db.rawQuery("SELECT pos,timestamp FROM "+Login.USERNAME+"_projects_temp ORDER BY pos;", null);
        try{
            cursor.moveToFirst();
            while(true){
                int pos = cursor.getInt(0);
                if (!exists(Login.USERNAME+"_" + pos + "_th.jpg")) {
                    //2 casees: case1:if its a new item. case2: if an existing item(with or without change) has been deleted somehow
                    notAvailableList_th.add(pos);
                }

                Cursor c = db.rawQuery("SELECT pos,timestamp FROM "+Login.USERNAME+"_projects WHERE pos="+pos+";", null);
                try {
                    c.moveToFirst();
                    int n = c.getInt(0);
                    String currentTime = c.getString(1);
                    String updatedTime = cursor.getString(1);
                    if(!currentTime.equals(updatedTime)){
                        //the item has been modified
                        if(!notAvailableList_th.contains(pos)) notAvailableList_th.add(pos);
                    }
                }catch (Exception e){
                    //it's a new item and it has already been added to the list
                }

                cursor.moveToNext();
                if(cursor.isAfterLast()){
                    break;
                }
            }
        }catch (Exception e){}
        cursor.close();


        //SET TOBEDELETEDLIST FOR PROJECTS
        cursor = db.rawQuery("SELECT pos FROM "+Login.USERNAME+"_projects ORDER BY pos;", null);
        try{
            cursor.moveToFirst();
            while(true){
                int pos = cursor.getInt(0);
                Cursor c = db.rawQuery("SELECT pos FROM "+Login.USERNAME+"_projects_temp WHERE pos="+pos+";", null);
                try {
                    c.moveToFirst();
                    int n = c.getInt(0);
                }catch (Exception e){
                    toBeDeletedList_th.add(pos);
                }
                cursor.moveToNext();
                if(cursor.isAfterLast()){
                    break;
                }
            }
        }catch (Exception e){}
        cursor.close();


        String s="";
        for(int i: notAvailableList_th){
            s= s+ (i+" ");
        }
        Log.e("notAvailableList_th",s);
        s="";
        for(int i: toBeDeletedList_th){
            s= s+ (i+" ");
        }
        Log.e("toBeDeletedList_th",s);






        //SET NOTAVAILABLELIST FOR SUBPROJECTS
        cursor = db.rawQuery("SELECT COUNT(pos) FROM "+Login.USERNAME+"_subProjects_temp;",null);
        cursor.moveToFirst();
        COUNT = cursor.getInt(0);
        notAvailableList = new ArrayList<>(COUNT);
        toBeDeletedList = new ArrayList<>(COUNT);
        cursor.close();

        cursor = db.rawQuery("SELECT projectPos,pos,timestamp,mediaType FROM "+Login.USERNAME+"_subProjects_temp ORDER BY projectPos,pos;", null);
        try{
            cursor.moveToFirst();
            while(true){
                int projectPos = cursor.getInt(0);
                int pos = cursor.getInt(1);
                String mediaType = cursor.getString(3);
                if(!mediaType.startsWith("youtube")) {
                    if (!exists(Login.USERNAME + "_" + projectPos + "_" + pos + ".jpg") && !exists(Login.USERNAME + "_" + projectPos + "_" + pos + ".mp4")) {
                        //2 casees: case1:if its a new item. case2: if an existing item(with or without change) has been deleted somehow
                        notAvailableList.add(projectPos + "_" + pos);
                    }
                }
                Cursor c = db.rawQuery("SELECT projectPos,pos,timestamp,mediaType FROM "+Login.USERNAME+"_subProjects WHERE projectPos="+projectPos+" AND pos="+pos+";", null);
                try {
                    c.moveToFirst();
                    int n = c.getInt(0);
                    String mt = c.getString(3);
                    String currentTime = c.getString(2);
                    String updatedTime = cursor.getString(2);
                    if(!currentTime.equals(updatedTime)){
                        //the item has been modified
                        if(!mt.startsWith("youtube")) {
                            if(!notAvailableList.contains(projectPos + "_" + pos)) notAvailableList.add(projectPos + "_" + pos);
                        }
                    }
                }catch (Exception e){
                    //it's a new item and it has already been added to the list
                }
                c.close();
                cursor.moveToNext();
                if(cursor.isAfterLast()){
                    break;
                }
            }
        }catch (Exception e){}
        cursor.close();


        //SET TOBEDELETEDLIST FOR SUBPROJECTS
        cursor = db.rawQuery("SELECT projectPos,pos FROM "+Login.USERNAME+"_subProjects ORDER BY projectPos,pos;", null);
        try{
            cursor.moveToFirst();
            while(true){
                int projectPos = cursor.getInt(0);
                int pos = cursor.getInt(1);
                Cursor c = db.rawQuery("SELECT projectPos,pos FROM "+Login.USERNAME+"_subProjects_temp WHERE projectPos="+projectPos+" AND pos="+pos+";", null);
                try {
                    c.moveToFirst();
                    int n = c.getInt(0);
                }catch (Exception e){
                    toBeDeletedList.add(projectPos + "_" + pos);
                }
                c.close();
                cursor.moveToNext();
                if(cursor.isAfterLast()){
                    break;
                }
            }
        }catch (Exception e){}
        cursor.close();

        s="";
        for(String i: notAvailableList){
            s= s+ (i+" ");
        }
        Log.e("notAvailableList",s);

        s="";
        for(String i: toBeDeletedList){
            s= s+ (i+" ");
        }
        Log.e("toBeDeletedList",s);



        //MOVE FROM TEMP TABLES TO ORIGINAL TABLES


        db.execSQL("DELETE FROM "+Login.USERNAME+"_projects;");
        db.execSQL("DELETE FROM "+Login.USERNAME+"_subProjects;");
        cursor = db.rawQuery("SELECT * FROM "+Login.USERNAME+"_projects_temp ORDER BY pos;", null);
        try{
            cursor.moveToFirst();
            while(true){
                int pos = cursor.getInt(0);
                String ts = cursor.getString(1);
                db.execSQL("INSERT INTO "+Login.USERNAME+"_projects VALUES("+pos+",'"+ts+"');");
                cursor.moveToNext();
                if(cursor.isAfterLast()){
                    break;
                }
            }
        }catch (Exception e){}
        cursor.close();
        cursor = db.rawQuery("SELECT * FROM "+Login.USERNAME+"_subProjects_temp ORDER BY projectPos,pos;", null);
        try{
            cursor.moveToFirst();
            while(true){
                int projectPos = cursor.getInt(0);
                int pos = cursor.getInt(1);
                String tts = cursor.getString(2);
                String mediatype = cursor.getString(3);
                String ts = cursor.getString(4);
                db.execSQL("INSERT INTO "+Login.USERNAME+"_subProjects VALUES("+projectPos+","+pos+",'"+tts+"','"+mediatype+"','"+ts+"');");
                cursor.moveToNext();
                if(cursor.isAfterLast()){
                    break;
                }
            }
        }catch (Exception e){}
        cursor.close();

        //CLEAR THE TEMP TABLES
        db.execSQL("DELETE FROM "+Login.USERNAME+"_projects_temp;");
        db.execSQL("DELETE FROM "+Login.USERNAME+"_subProjects_temp;");


        //DELETE FILES IF ANY
        File dir = getFilesDir();
        for(int i : toBeDeletedList_th){
            File file = new File(dir, Login.USERNAME+"_" + i + "_th.jpg");
            file.delete();
        }
        for(String i : toBeDeletedList){
            try {
                File file = new File(dir, Login.USERNAME+"_" + i + ".jpg");
                file.delete();
                file = new File(dir, Login.USERNAME+"_" + i + ".mp4");
                file.delete();
            }catch (Exception e){}
        }




        //START DOWNLOAD IF ANY


        if(notAvailableList_th.size()>0){
            dialog1.setMessage("Downloading Thumbnail 1/" + notAvailableList_th.size());
        }else{
            if(notAvailableList.size()>0){
                downloadSubProjectsMedia();
            }else{
                dialog1.dismiss();
                display();
            }
        }
        CURR_COUNT_th=0;
        for (final int k : notAvailableList_th) {
            ParseQuery<ParseObject> query = ParseQuery.getQuery(Login.PROJECTS_TABLE_NAME);
            query.whereEqualTo("pos", k);
            query.findInBackground(new FindCallback<ParseObject>() {
                public void done(List<ParseObject> objects, ParseException e) {
                    if (e == null) {
                        ParseFile myFile = objects.get(0).getParseFile("thumbnail");
                        myFile.getDataInBackground(new GetDataCallback() {
                            public void done(byte[] data, ParseException e) {
                                if (e == null) {
                                    writeFile(data, Login.USERNAME+"_" + k + "_th.jpg");
                                    CURR_COUNT_th++;
                                    dialog1.setMessage("Downloading Thumbnail "+(CURR_COUNT_th+1)+"/"+notAvailableList_th.size());
                                    if (CURR_COUNT_th == notAvailableList_th.size()) {

                                        if(notAvailableList.size()>0){
                                            downloadSubProjectsMedia();
                                        }else{
                                            dialog1.dismiss();
                                            display();
                                        }

                                    }
                                } else {
                                    Log.e("Something went wrong", "Something went wrong");
                                }
                            }
                        });
                    } else {
                        Log.e("PARSE", "Error: " + e.getMessage());
                    }
                }
            });
        }

    }

    public void downloadSubProjectsMedia(){

        Log.e("SubProjectsMedia","SubProjectsMedia");
        dialog1.setMessage("Downloading Panorama 1/" + notAvailableList.size());
        if(notAvailableList.size()==0){
            dialog1.dismiss();
            display();
        }
        CURR_COUNT=0;
        for (final String s : notAvailableList) {
            ParseQuery<ParseObject> query = ParseQuery.getQuery(Login.SUBPROJECTS_TABLE_NAME);
            String[] parts = s.split("_");
            final int projectPos = Integer.parseInt(parts[0]);
            final int pos = Integer.parseInt(parts[1]);
            query.whereEqualTo("pos", pos);
            query.whereEqualTo("projectPos", projectPos);
            query.findInBackground(new FindCallback<ParseObject>() {
                public void done(List<ParseObject> objects, ParseException e) {
                    if (e == null) {
                        final ParseFile myFile = objects.get(0).getParseFile("photoSphere");
                        myFile.getDataInBackground(new GetDataCallback() {
                            public void done(byte[] data, ParseException e) {
                                if (e == null) {
                                    if(myFile.getName().endsWith("jpg")){
                                        Log.e("FILENAME", "jpg");
                                        writeFile(data, Login.USERNAME+"_" + projectPos + "_" + pos + ".jpg");
                                    }else if(myFile.getName().endsWith("mp4")){
                                        Log.e("FILENAME", "mp4");
                                        writeFile(data, Login.USERNAME+"_" + projectPos + "_" + pos + ".mp4");
                                    }
                                    CURR_COUNT++;
                                    dialog1.setMessage("Downloading Panorama "+(CURR_COUNT+1)+"/"+notAvailableList.size());
                                    if (CURR_COUNT == notAvailableList.size()) {
                                        dialog1.dismiss();
                                        display();
                                    }
                                } else {
                                    Log.e("Something went wrong", "Something went wrong");
                                }
                            }
                        });
                    } else {
                        Log.e("PARSE", "Error: " + e.getMessage());
                    }
                }
            });
        }
    }

    public void display(){
        LinearLayout ll = (LinearLayout)findViewById(R.id.splashBackground);
        InputStream is = null;
        try{
            is = openFileInput(Login.USERNAME + "_splash.jpg");
        }catch (Exception ex){}
        Bitmap bitmap = BitmapFactory.decodeStream(is);
        Drawable d = new BitmapDrawable(getResources(), bitmap);
        ll.setBackground(d);
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

    public void writeFile(byte[] data, String fileName) {
        try {
            FileOutputStream fos = openFileOutput(fileName, Context.MODE_PRIVATE);
            fos.write(data);
            fos.close();
        }catch(Exception e){
            Log.e("WriteFile",e.getMessage());
        }
    }

    public boolean exists(String fname){
        File file = getBaseContext().getFileStreamPath(fname);
        return file.exists();
    }

    @Override
    public void onRefresh() {
        if(checkConnection()) {
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    swipeLayout.setRefreshing(false);
                }
            }, 1000);
            download();
        }else{
            Toast.makeText(Splash.this, "Check your Internet Connection!", Toast.LENGTH_LONG).show();
        }
    }

    public boolean checkConnection(){
        ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeInfo = connMgr.getActiveNetworkInfo();
        if (activeInfo == null ) return false;
        else return true;
    }

}