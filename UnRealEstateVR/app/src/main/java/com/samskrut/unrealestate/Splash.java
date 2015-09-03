
package com.samskrut.unrealestate;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import com.nispok.snackbar.Snackbar;
import com.nispok.snackbar.SnackbarManager;
import com.nispok.snackbar.enums.SnackbarType;
import com.nispok.snackbar.listeners.ActionClickListener;
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
import java.util.List;

public class Splash extends AppCompatActivity{

    ProgressDialog dialog1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.splash);
        setFullscreen(true);

        if( !exists(Login.USERNAME + "_splash.jpg") && !checkConnection() ){
            //Toast.makeText(Splash.this, "Check your Internet Connection!", Toast.LENGTH_SHORT).show();
            //SnackbarManager.show(Snackbar.with(Login.this).text("Username and Password do not match!"));
            SnackbarManager.show(
                    Snackbar.with(getApplicationContext())
                            .type(SnackbarType.MULTI_LINE)
                            .duration(Snackbar.SnackbarDuration.LENGTH_LONG)
                            .text("No Internet Connection!")
                            .actionLabel("CLOSE")
                            .actionListener(new ActionClickListener() {
                                @Override
                                public void onActionClicked(Snackbar snackbar) {
                                    SnackbarManager.dismiss();
                                }
                            }), Splash.this);
            findViewById(R.id.viewVirtualTours).setVisibility(View.GONE);
            return;
        }

        if( !exists(Login.USERNAME + "_splash.jpg") && checkConnection() ){
            downloadSplashImage();
        }

        display();

        //Set up an on-click function for the Text View 'View Virtual Tours'
        (findViewById(R.id.viewVirtualTours)).setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    //When finger goes down on the text, change the text color a little to visually show that it's beign clicked
                    ((TextView) v).setTextColor(Color.parseColor("#aaffffff"));
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    //When finger goes up, a click is complete. So change the color back and start the next activity.
                    ((TextView) v).setTextColor(Color.parseColor("#ffffff"));
                    Intent mainIntent = new Intent(Splash.this, MainActivity.class);
                    startActivity(mainIntent);
                    finish();
                } else if (event.getAction() == MotionEvent.ACTION_CANCEL) {
                    ((TextView) v).setTextColor(Color.parseColor("#ffffff"));
                }
                return true;
            }
        });

    }

    /**
     * Display the downloaded Splash Image(set it as background to the linearlayout)
     */
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

    /**
     * This function downloads the Splash Image from the Parse Table 'Splash'
     */
    public void downloadSplashImage() {
        Log.e("downloadSplashImage","downloadSplashImage");
        dialog1 = ProgressDialog.show(this, null, "Downloading Splash Image...", true);
        ParseQuery<ParseObject> query = ParseQuery.getQuery("Splash");
        query.whereEqualTo("username", Login.USERNAME);
        query.findInBackground(new FindCallback<ParseObject>() {
            public void done(List<ParseObject> objects, ParseException e) {
                if (e == null) {
                    ParseFile myFile = objects.get(0).getParseFile("image");
                    myFile.getDataInBackground(new GetDataCallback() {
                        public void done(byte[] data, ParseException e) {
                            if (e == null) {
                                //user the function writeFile() to create the file in the Internal Memory
                                //the file name will be like someusername_splash.jpg
                                writeFile(data, Login.USERNAME + "_splash.jpg");
                                //once the splash image is downloaded, we need to download the images for the listview.
                                dialog1.dismiss();
                                display();
                            } else {
                                findViewById(R.id.viewVirtualTours).setVisibility(View.GONE);
                                dialog1.dismiss();
                                //Toast.makeText(Splash.this, "Something went wrong", Toast.LENGTH_LONG).show();
                                //SnackbarManager.show(Snackbar.with(Login.this).text("Username and Password do not match!"));
                                SnackbarManager.show(
                                        Snackbar.with(getApplicationContext())
                                                .type(SnackbarType.MULTI_LINE)
                                                .duration(Snackbar.SnackbarDuration.LENGTH_LONG)
                                                .text("Something went wrong! Try again.")
                                                .actionLabel("CLOSE")
                                                .actionListener(new ActionClickListener() {
                                                    @Override
                                                    public void onActionClicked(Snackbar snackbar) {
                                                        SnackbarManager.dismiss();
                                                    }
                                                }), Splash.this);
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

    /**
     * A function to make the app go full screen- hides the status bar
     * @param fullscreen a value of true goes full screen, false comes back from full screen
     */
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

    /**
     * Creates a file in the Internal Memory with the given filename and the bytes as data
     * @param data is the content of the file in byte[] format
     * @param fileName si the name of the file to be created
     */
    public void writeFile(byte[] data, String fileName) {
        try {
            FileOutputStream fos = openFileOutput(fileName, Context.MODE_PRIVATE);
            fos.write(data);
            fos.close();
        }catch(Exception e){
            Log.e("WriteFile",e.getMessage());
        }
    }

    /**
     * @param fname is the filename given as input
     * @return true if the file exists in the Internal Memory, false, if it doesn't exist
     */
    public boolean exists(String fname){
        File file = getBaseContext().getFileStreamPath(fname);
        return file.exists();
    }

    /**
     * A function to check if there is Internet conn or not - checks both WiFi and Mobile Data
     * @return true if there is Internet conn, false if not.
     */
    public boolean checkConnection(){
        ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeInfo = connMgr.getActiveNetworkInfo();
        if (activeInfo == null ) return false;
        else return true;
    }

}