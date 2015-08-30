
package com.samskrut.unrealestate;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
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

public class MainActivity extends AppCompatActivity implements SwipeRefreshLayout.OnRefreshListener{

    SwipeRefreshLayout swipeLayout;
    ProgressDialog dialog1;
    int COUNT=0,CURR_COUNT=0;
    ArrayList<Integer> notAvailableList,toBeDeletedList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //set up wake lock for the screen, so that the screen doesn't turn off when the data and images are being downloaded
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.activity_main);
        getSupportActionBar().setTitle(Html.fromHtml("<font color=\"#ffffff\">unrealEstate</font>"));

        //set up Pull-To-Refresh
        swipeLayout = (SwipeRefreshLayout) findViewById(R.id.swipe_container);
        swipeLayout.setOnRefreshListener(this);
        swipeLayout.setColorScheme(android.R.color.black);

        checkForDownload();
        displayEverything();
    }

    /**
     * On activity resume, the wake lock is added
     */
    @Override
    protected void onResume(){
        super.onResume();
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    /**
     * On activity pause, the wake lock is cleared
     */
    @Override
    protected void onPause(){
        super.onPause();
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    /**
     * On activity destroy, the wake lock is cleared
     */
    @Override
    protected void onDestroy(){
        super.onDestroy();
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    /**
     * To dynamically add all the views to the screen for the listview.
     */
    public void displayEverything(){

        LinearLayout mainll = (LinearLayout)findViewById(R.id.mainll);
        mainll.removeAllViews();

        //Iterate through the projects table and for each item, add a view to the linearlayout
        Cursor cursor = Login.db.rawQuery("SELECT pos,name,desc,url FROM "+Login.USERNAME+"_projects WHERE username='"+Login.USERNAME+"' ORDER BY pos;",null);
        try{
            cursor.moveToFirst();
            while(true){
                final int pos = cursor.getInt(0);
                final String url = cursor.getString(3);

                LinearLayout ll = new LinearLayout(this);
                ll.setOrientation(LinearLayout.VERTICAL);
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                ll.setLayoutParams(params);
                ll.setBackgroundColor(Color.WHITE);
                ll.setGravity(Gravity.CENTER_HORIZONTAL);

                ImageView imageButton = new ImageView(this);
                InputStream is = openFileInput(Login.USERNAME + "_" + pos + ".jpg");
                Bitmap bitmap = BitmapFactory.decodeStream(is);
                imageButton.setImageBitmap(bitmap);
                imageButton.setScaleType(ImageView.ScaleType.CENTER_CROP);
                params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (int)getResources().getDimension(R.dimen.dp200));
                params.setMargins(0, (int) getResources().getDimension(R.dimen.dp5), 0, (int) getResources().getDimension(R.dimen.dp5));
                imageButton.setLayoutParams(params);
                imageButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Intent intent = new Intent(MainActivity.this, MyWebView.class);
                        intent.putExtra("url", url);
                        startActivity(intent);
                    }
                });
                ll.addView(imageButton);

                TextView textView = new TextView(this);
                textView.setText(cursor.getString(1));
                textView.setTextSize(20);
                params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                params.setMargins(0, (int) getResources().getDimension(R.dimen.dp10), 0, 0);
                textView.setLayoutParams(params);
                textView.setTextColor(Color.parseColor("#666666"));
                textView.setTypeface(null, Typeface.BOLD);
                ll.addView(textView);


                textView = new TextView(this);
                textView.setText(cursor.getString(2));
                textView.setTextSize(16);
                textView.setLayoutParams(params);
                textView.setTextColor(Color.parseColor("#666666"));
                textView.setGravity(Gravity.CENTER);
                ll.addView(textView);

                Button button = new Button(this);
                params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (int) getResources().getDimension(R.dimen.dp1));
                params.setMargins(0, (int) getResources().getDimension(R.dimen.dp20), 0, (int) getResources().getDimension(R.dimen.dp20));
                button.setLayoutParams(params);
                button.setBackgroundColor(Color.parseColor("#cccccc"));
                button.setPadding((int) getResources().getDimension(R.dimen.dp10), 0, (int) getResources().getDimension(R.dimen.dp10), 0);
                ll.addView(button);

                mainll.addView(ll);

                cursor.moveToNext();
                if(cursor.isAfterLast()){
                    break;
                }
            }
        }catch (Exception e){
            Log.e(e.toString(),e.getMessage());
        }

        cursor.close();

    }

    /**
     * Set up the actionbar icon for logout
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    /**
     * To clear the session when the user clicks on the logout button and go back to the login activity.
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case R.id.actionLogout:
                SQLiteDatabase db = openOrCreateDatabase("unrealestate.db", SQLiteDatabase.CREATE_IF_NECESSARY, null);
                db.execSQL("DELETE FROM session;");
                db.close();
                Intent mainIntent = new Intent(this, Login.class);
                mainIntent.putExtra("loggedout", true);
                mainIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(mainIntent);
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /**
     * This function is called whenever the action Pull-To-Refresh is done
     */
    @Override
    public void onRefresh() {
        //After a delay of one second, hide the spinning arrow.
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                swipeLayout.setRefreshing(false);
            }
        }, 1000);
        //start downloading the data from Parse
        download();
    }

    /**
     * The first time the app is ever opened, there will be no data present in the local db. So, we HAVE to download from Parse
     * This function checks if a download is completely necessary and if it is, it starts the download and
     * if it isn't it lets the app function with offline data
     */
    public void checkForDownload(){
        Cursor cursor = Login.db.rawQuery("SELECT COUNT(name) FROM "+Login.USERNAME+"_projects WHERE username='"+Login.USERNAME+"';", null);
        cursor.moveToFirst();
        int count = cursor.getInt(0);
        if (count == 0) {
            if (checkConnection()) {
                download();
            } else {
                //Toast.makeText(this, "Please check your Internet Connection!", Toast.LENGTH_LONG).show();
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
                                }), MainActivity.this);
                (findViewById(R.id.viewVirtualTours)).setVisibility(View.GONE);
            }
        }
        cursor.close();
    }

    /**
     * This function downloads all the data (except images) from the Parse table and inserts in the local temp table.
     * Later we transfer all data from the temp table to the original table and clear the temp table off
     */
    public void download(){
        dialog1 = ProgressDialog.show(this, null, "Downloading Data...");

        Login.db.execSQL("DELETE FROM login;");
        Login.db.execSQL("DELETE FROM "+Login.USERNAME+"_projects_temp;");

        final ParseQuery<ParseObject> query = ParseQuery.getQuery("unrealEstate");
        //download data only for this particular user
        query.whereEqualTo("username",Login.USERNAME);
        query.orderByAscending("pos");
        query.findInBackground(new FindCallback<ParseObject>() {
            public void done(List<ParseObject> objects, ParseException e) {
                if (e == null) {
                    for (ParseObject ob : objects) {
                        Login.db.execSQL("INSERT INTO "+Login.USERNAME+"_projects_temp VALUES(" + ob.getNumber("pos") + ",'" + ob.getString("name") + "','" + ob.getString("description") + "','" + ob.getString("url") + "','" + ob.getString("username") + "','" + ob.getUpdatedAt() + "');");
                        Log.e("QUERY","INSERT INTO "+Login.USERNAME+"_projects_temp VALUES(" + ob.getNumber("pos") + ",'" + ob.getString("name") + "','" + ob.getString("description") + "','" + ob.getString("url") + "','" + ob.getString("username") + "','" + ob.getUpdatedAt() + "');");
                    }
                    //once the projects data download is complete, download the login data

                    //Parse query to download from the Parse table 'Login'
                    final ParseQuery<ParseObject> query = ParseQuery.getQuery("Login");
                    query.findInBackground(new FindCallback<ParseObject>() {
                        public void done(List<ParseObject> objects, ParseException e) {
                            if (e == null) {
                                for (ParseObject ob : objects) {
                                    //Insert each object retrieved from Parse, into the local sqlite table 'login'
                                    Login.db.execSQL("INSERT INTO login VALUES('" + ob.getString("username") + "','" + ob.getString("password") + "');");
                                }
                                //Once the download is complete, now call the function to download the Splash Image
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
    }

    /**
     * This function downloads the Splash Image from the Parse Table 'Splash'
     */
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
                                //user the function writeFile() to create the file in the Internal Memory
                                //the file name will be like someusername_splash.jpg
                                writeFile(data, Login.USERNAME+"_splash.jpg");
                                //once the splash image is downloaded, we need to download the images for the listview.
                                downloadImages();
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

    /**
     * This function takes care of downloading all the images from Parse for the listview.
     * It checks for newly added images at Parse, images that are deleted at Parse and images that are modified at Parse
     */
    public void downloadImages(){

        Log.e("downloadImages","downloadImages");

        /*
            We are going to mainatain 2 lists:
            notavailablelist - it contains the list of project pos numbers for which the images are not present in the internal memory
            tobedeletedlist - it contains the list of project pos numbers for which the images are to be deleted from the internal memory
            These lists could take a max length of the total numebr of iamges there exists. So we initialize the lists with the max number
        */
        Cursor cursor = Login.db.rawQuery("SELECT COUNT(pos) FROM "+Login.USERNAME+"_projects_temp WHERE username='"+Login.USERNAME+"';",null);
        cursor.moveToFirst();
        COUNT = cursor.getInt(0);
        notAvailableList = new ArrayList<>(COUNT);
        toBeDeletedList = new ArrayList<>(COUNT);
        cursor.close();

        /*
            SET NOTAVAILABLELIST
            We set the notavailablelist for 3 cases- when there is a new item at Parse, when an existin item has been deleted
            from the internal memory and when an existing item has been modified at Parse
        */
        //We go through the entire projects_temp table for this particular username
        cursor = Login.db.rawQuery("SELECT pos,timestamp FROM "+Login.USERNAME+"_projects_temp WHERE username='"+Login.USERNAME+"' ORDER BY pos;", null);
        try{
            cursor.moveToFirst();
            while(true){
                int pos = cursor.getInt(0);
                if (!exists(Login.USERNAME+"_" + pos + ".jpg")) {
                    /*  2 cases:
                        case 1: if its a new item.
                        case 2: if an existing item(with or without change) has been deleted somehow
                    */
                    notAvailableList.add(pos);
                }
                //We compare the temp table with the old data in the original table and see if any items have been modified at Parse using the timestamp
                Cursor c = Login.db.rawQuery("SELECT pos,timestamp FROM "+Login.USERNAME+"_projects WHERE pos="+pos+" AND username='"+Login.USERNAME+"';", null);
                try {
                    c.moveToFirst();
                    int n = c.getInt(0);
                    String currentTime = c.getString(1);
                    String updatedTime = cursor.getString(1);
                    if(!currentTime.equals(updatedTime)){
                        //the item has been modified
                        if(!notAvailableList.contains(pos)) notAvailableList.add(pos);
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


        /*
            SET TOBEDELETEDLIST
            We set the tobedeletedlist for items that once existed at Parse, but now have been removed
        */
        //We go through the entire projects table for this particular username and compare it with the projects_temp table
        cursor = Login.db.rawQuery("SELECT pos FROM "+Login.USERNAME+"_projects WHERE username='"+Login.USERNAME+"' ORDER BY pos;", null);
        try{
            cursor.moveToFirst();
            while(true){
                int pos = cursor.getInt(0);
                Cursor c = Login.db.rawQuery("SELECT pos FROM "+Login.USERNAME+"_projects_temp WHERE username='"+Login.USERNAME+"' AND pos="+pos+";", null);
                try {
                    c.moveToFirst();
                    //if the pos doesn't exist in projects_temp table, an exception will be thrown and it goes to the catch block.
                    int n = c.getInt(0);
                }catch (Exception e){
                    toBeDeletedList.add(pos);
                }
                cursor.moveToNext();
                if(cursor.isAfterLast()){
                    break;
                }
            }
        }catch (Exception e){
            Log.e(e.getMessage(), e.toString());
        }
        cursor.close();


        String s="";
        for(int i: notAvailableList){
            s= s+ (i+" ");
        }
        Log.e("notAvailableList",s);
        s="";
        for(int i: toBeDeletedList){
            s= s+ (i+" ");
        }
        Log.e("toBeDeletedList",s);



        //MOVE FROM TEMP TABLE TO ORIGINAL TABLE
        Login.db.execSQL("DELETE FROM "+Login.USERNAME+"_projects;");
        cursor = Login.db.rawQuery("SELECT * FROM "+Login.USERNAME+"_projects_temp ORDER BY pos;", null);
        try{
            cursor.moveToFirst();
            while(true){
                int pos = cursor.getInt(0);
                String name = cursor.getString(1);
                String desc = cursor.getString(2);
                String url = cursor.getString(3);
                String username = cursor.getString(4);
                String ts = cursor.getString(5);
                Login.db.execSQL("INSERT INTO "+Login.USERNAME+"_projects VALUES("+pos+",'"+name+"','"+desc+"','"+url+"','"+username+"','"+ts+"');");
                cursor.moveToNext();
                if(cursor.isAfterLast()){
                    break;
                }
            }
        }catch (Exception e){}
        cursor.close();



        //CLEAR THE TEMP TABLE
        Login.db.execSQL("DELETE FROM "+Login.USERNAME+"_projects_temp WHERE username='"+Login.USERNAME+"';");



        //DELETE FILES IF ANY
        File dir = getFilesDir();
        //Iterate through the toBeDeletedList and for each item, delete the respective image from the internal storage
        for(int i : toBeDeletedList){
            File file = new File(dir, Login.USERNAME+"_" + i + ".jpg");
            file.delete();
        }



        //START DOWNLOADS IF ANY
        dialog1.setMessage("Downloading Image 1/"+notAvailableList.size());
        CURR_COUNT=0;
        //Iterate through the notAvailableList and for each item, download the respective image from Parse
        for (final int k : notAvailableList) {
            ParseQuery<ParseObject> query = ParseQuery.getQuery("unrealEstate");
            query.whereEqualTo("pos", k);
            query.whereEqualTo("username", Login.USERNAME);
            query.findInBackground(new FindCallback<ParseObject>() {
                public void done(List<ParseObject> objects, ParseException e) {
                    if (e == null) {
                        ParseFile myFile = objects.get(0).getParseFile("image");
                        myFile.getDataInBackground(new GetDataCallback() {
                            public void done(byte[] data, ParseException e) {
                                if (e == null) {
                                    //the image filename is like someusername_posnumber.jpg
                                    writeFile(data, Login.USERNAME + "_" + k + ".jpg");
                                    CURR_COUNT++;
                                    dialog1.setMessage("Downloading Image "+(CURR_COUNT+1)+"/"+notAvailableList.size());
                                    if (CURR_COUNT == notAvailableList.size()) {
                                        dialog1.dismiss();
                                        displayEverything();
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

        if(notAvailableList.size()==0){
            dialog1.dismiss();
            displayEverything();
        }

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