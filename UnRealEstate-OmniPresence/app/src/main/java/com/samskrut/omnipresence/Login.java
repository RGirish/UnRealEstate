/**
 * This class is exactly similar to the Login class in UE, with only very small differences.
 * The db structure alone changes here. Different tables are used with this app. Take a note of that alone.
 * The rest of the functioning is exactly similar to the Login of UE.
 */
package com.samskrut.omnipresence;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import com.crashlytics.android.Crashlytics;
import com.parse.FindCallback;
import com.parse.Parse;
import com.parse.ParseCrashReporting;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import io.fabric.sdk.android.Fabric;
import java.util.List;

public class Login extends AppCompatActivity {

    public static String PROJECTS_TABLE_NAME="",SUBPROJECTS_TABLE_NAME="",USERNAME="";
    public SQLiteDatabase db;
    ProgressDialog dialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Fabric.with(this, new Crashlytics());

        setContentView(R.layout.activity_login);
        setFullScreen(true);

        try{ParseCrashReporting.enable(this);}catch (Exception e){}
        Parse.initialize(this, "Sq2yle2ei4MmMBXAChjGksJDqlwma3rjarvoZCsk", "vMw4I2I0fdSD1frBohAvWCaXZYqLaHZ8ljnwqavg");
        db = openOrCreateDatabase("omniPresence.db",SQLiteDatabase.CREATE_IF_NECESSARY, null);
        createTables();

        Cursor cursor = db.rawQuery("SELECT projectsTableName,subProjectsTableName,username FROM session;", null);
        try{
            cursor.moveToFirst();
            PROJECTS_TABLE_NAME = cursor.getString(0);//exception is thrown here if there is no session
            SUBPROJECTS_TABLE_NAME = cursor.getString(1);
            USERNAME = cursor.getString(2);
            try{
                db.execSQL("CREATE TABLE "+USERNAME+"_projects(pos NUMBER,timestamp TEXT);");
            }catch(Exception e){}
            try{
                db.execSQL("CREATE TABLE "+USERNAME+"_subProjects(projectPos NUMBER, pos NUMBER, tts TEXT, mediatype TEXT, timestamp TEXT);");
            }catch(Exception e){}
            try{
                db.execSQL("CREATE TABLE "+USERNAME+"_projects_temp(pos NUMBER,timestamp TEXT);");
            }catch(Exception e){}
            try{
                db.execSQL("CREATE TABLE "+USERNAME+"_subProjects_temp(projectPos NUMBER, pos NUMBER, tts TEXT, mediatype TEXT, timestamp TEXT);");
            }catch(Exception e){}
            if(!(PROJECTS_TABLE_NAME.equals("NONE") && SUBPROJECTS_TABLE_NAME.equals("NONE"))){
                startActivity(new Intent(Login.this,Splash.class));
                finish();
            }
        }catch(Exception e){}

        //The ripple effect on touch for Lollipop doesn't work below Lollipop. So, for lower android versions,
        //we change the bg color of the button on touch to a darker shade; just to visually show the click.
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            final Button loginButton = (Button) findViewById(R.id.loginButton);
            loginButton.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    if (event.getAction() == MotionEvent.ACTION_DOWN) {
                        loginButton.setBackgroundColor(getResources().getColor(R.color.login_button_dark));
                    } else if (event.getAction() == MotionEvent.ACTION_UP) {
                        loginButton.setBackgroundColor(getResources().getColor(R.color.login_button));
                        hideKeyboard();
                        onClickLogin(null);
                    } else if (event.getAction() == MotionEvent.ACTION_CANCEL) {
                        loginButton.setBackgroundColor(getResources().getColor(R.color.login_button));
                    }
                    return true;
                }
            });
        }

        EditText editText = (EditText)findViewById(R.id.password);
        editText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                hideKeyboard();
                onClickLogin(null);
                return true;
            }
        });

    }



    public void downloadLoginDetails(final String username, final String password) {
        db.execSQL("DELETE FROM login;");
        final ParseQuery<ParseObject> query = ParseQuery.getQuery("Login");
        query.findInBackground(new FindCallback<ParseObject>() {
            public void done(List<ParseObject> objects, ParseException e) {
                if (e == null) {
                    for (ParseObject ob : objects) {
                        db.execSQL("INSERT INTO login VALUES('" + ob.getString("username") + "','" + ob.getString("password") + "','"+ ob.getString("projectsTableName") +"','"+ ob.getString("subProjectsTableName") +"');");
                    }

                    Cursor c = db.rawQuery("SELECT projectsTableName,subProjectsTableName FROM login WHERE username='" + username + "' AND password='" + password + "';", null);
                    try{
                        c.moveToFirst();
                        PROJECTS_TABLE_NAME = c.getString(0);
                        SUBPROJECTS_TABLE_NAME = c.getString(1);
                        USERNAME = username;
                        try{
                            db.execSQL("CREATE TABLE "+USERNAME+"_projects(pos NUMBER,timestamp TEXT);");
                        }catch(Exception ee){}
                        try{
                            db.execSQL("CREATE TABLE "+USERNAME+"_subProjects(projectPos NUMBER, pos NUMBER, tts TEXT, mediatype TEXT, timestamp TEXT);");
                        }catch(Exception ee){}
                        try{
                            db.execSQL("CREATE TABLE "+USERNAME+"_projects_temp(pos NUMBER,timestamp TEXT);");
                        }catch(Exception ee){}
                        try{
                            db.execSQL("CREATE TABLE "+USERNAME+"_subProjects_temp(projectPos NUMBER, pos NUMBER, tts TEXT, mediatype TEXT, timestamp TEXT);");
                        }catch(Exception ee){}
                        c.close();
                        dialog.dismiss();
                        Cursor cursor = db.rawQuery("SELECT username FROM session;", null);
                        cursor.moveToFirst();
                        db.execSQL("DELETE FROM session;");
                        db.execSQL("INSERT INTO session VALUES('" + PROJECTS_TABLE_NAME + "','" + SUBPROJECTS_TABLE_NAME + "','" + username + "');");
                        startActivity(new Intent(Login.this, Splash.class));
                        finish();

                    }catch (Exception ee){
                        Log.e(ee.getMessage(),ee.toString());
                        dialog.dismiss();
                        Toast.makeText(Login.this,"Username and Password do not match!",Toast.LENGTH_LONG).show();
                        ((EditText)findViewById(R.id.password)).setText("");
                        ((EditText)findViewById(R.id.username)).setText("");
                    }

                } else {
                    Log.e("PARSE", "Error: " + e.getMessage());
                }
            }
        });
    }

    public void onClickLogin(final View view){

        dialog = ProgressDialog.show(this,null,"Please wait...",true);

        String username = ((EditText)findViewById(R.id.username)).getText().toString();
        String password = ((EditText)findViewById(R.id.password)).getText().toString();

        if(username.equals("") || password.equals("")){
            Toast.makeText(Login.this, "Don't leave the fields empty!", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
            return;
        }

        Cursor c = db.rawQuery("SELECT projectsTableName,subProjectsTableName FROM login WHERE username='" + username + "' AND password='" + password + "';", null);
        try{
            c.moveToFirst();
            PROJECTS_TABLE_NAME = c.getString(0);
            SUBPROJECTS_TABLE_NAME = c.getString(1);
            USERNAME = username;
            try{
                db.execSQL("CREATE TABLE "+USERNAME+"_projects(pos NUMBER,timestamp TEXT);");
            }catch(Exception e){}
            try{
                db.execSQL("CREATE TABLE "+USERNAME+"_subProjects(projectPos NUMBER, pos NUMBER, tts TEXT, mediatype TEXT, timestamp TEXT);");
            }catch(Exception e){}
            try{
                db.execSQL("CREATE TABLE "+USERNAME+"_projects_temp(pos NUMBER,timestamp TEXT);");
            }catch(Exception e){}
            try{
                db.execSQL("CREATE TABLE "+USERNAME+"_subProjects_temp(projectPos NUMBER, pos NUMBER, tts TEXT, mediatype TEXT, timestamp TEXT);");
            }catch(Exception e){}
            dialog.dismiss();

            Cursor cursor = db.rawQuery("SELECT username FROM session;", null);
            cursor.moveToFirst();
            db.execSQL("DELETE FROM session;");
            db.execSQL("INSERT INTO session VALUES('"+PROJECTS_TABLE_NAME+"','"+SUBPROJECTS_TABLE_NAME+"','"+USERNAME+"');");
            startActivity(new Intent(Login.this, Splash.class));
            finish();
        }catch (Exception e){
            if(checkConnection()){
                downloadLoginDetails(username,password);
            }else{
                dialog.dismiss();
                findViewById(R.id.firstTime).setVisibility(View.VISIBLE);
                Toast.makeText(this,"Username and Password do not match!",Toast.LENGTH_LONG).show();
                ((EditText)findViewById(R.id.password)).setText("");
                ((EditText)findViewById(R.id.username)).setText("");
            }
        }
    }

    public void createTables() {
        try{
            db.execSQL("CREATE TABLE session(projectsTableName TEXT,subProjectsTableName TEXT, username TEXT);");
        }catch(Exception e){}
        try{
            db.execSQL("CREATE TABLE login(username TEXT,password TEXT,projectsTableName TEXT, subProjectsTableName TEXT);");
        }catch(Exception e){}
    }

    public boolean checkConnection(){
        ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeInfo = connMgr.getActiveNetworkInfo();
        if (activeInfo == null ) return false;
        else return true;
    }

    private void hideKeyboard() {
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager inputManager = (InputMethodManager) this.getSystemService(Context.INPUT_METHOD_SERVICE);
            inputManager.hideSoftInputFromWindow(view.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
        }
    }

    void setFullScreen(boolean fullscreen) {
        WindowManager.LayoutParams attrs = getWindow().getAttributes();
        if (fullscreen) {
            attrs.flags |= WindowManager.LayoutParams.FLAG_FULLSCREEN;
        }
        else{
            attrs.flags &= ~WindowManager.LayoutParams.FLAG_FULLSCREEN;
        }
        getWindow().setAttributes(attrs);
    }

}