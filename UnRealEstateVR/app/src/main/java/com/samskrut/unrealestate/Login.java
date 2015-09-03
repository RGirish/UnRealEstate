
package com.samskrut.unrealestate;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
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
import com.nispok.snackbar.Snackbar;
import com.nispok.snackbar.SnackbarManager;
import com.nispok.snackbar.enums.SnackbarType;
import com.nispok.snackbar.listeners.ActionClickListener;
import com.parse.FindCallback;
import com.parse.Parse;
import com.parse.ParseCrashReporting;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import io.fabric.sdk.android.Fabric;
import java.util.List;

public class Login extends AppCompatActivity {

    public static String USERNAME="";
    public static SQLiteDatabase db;
    ProgressDialog dialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Fabric.with(this, new Crashlytics());
        setContentView(R.layout.activity_login);

        //to keep screen on while this activity is active
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        setFullScreen(true);

        if(getIntent().getBooleanExtra("loggedout", false)){
            //Toast.makeText(Login.this, "Logged out!", Toast.LENGTH_LONG).show();
            //SnackbarManager.show(Snackbar.with(Login.this).text("Username and Password do not match!"));

            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    SnackbarManager.show(
                            Snackbar.with(getApplicationContext())
                                    .type(SnackbarType.MULTI_LINE)
                                    .duration(Snackbar.SnackbarDuration.LENGTH_LONG)
                                    .text("Logged out!")
                                    .actionLabel("CLOSE")
                                    .actionListener(new ActionClickListener() {
                                        @Override
                                        public void onActionClicked(Snackbar snackbar) {
                                            SnackbarManager.dismiss();
                                        }
                                    }), Login.this);
                }
            }, 1000);
        }

        //set up Parse
        try{ParseCrashReporting.enable(this);}catch (Exception e){}
        Parse.initialize(this, "Sq2yle2ei4MmMBXAChjGksJDqlwma3rjarvoZCsk", "vMw4I2I0fdSD1frBohAvWCaXZYqLaHZ8ljnwqavg");

        //create the sqlite db
        db = openOrCreateDatabase("unrealestate.db",SQLiteDatabase.CREATE_IF_NECESSARY, null);
        createTables();

        //Check if there is a session going on. i.e., if a user has already logged in.
        Cursor cursor = db.rawQuery("SELECT username FROM session;", null);
        try{
            cursor.moveToFirst();
            USERNAME = cursor.getString(0);
            //if the table 'session' is empty, it will go to the catch block.
            //if the table 'session' isn't empty, it means there is a session going on.
            if(!(USERNAME.equals("NONE"))){
                //create the projects and projects_temp tables for this particular user
                try{
                    db.execSQL("CREATE TABLE "+USERNAME+"_projects(pos NUMBER, name TEXT, desc TEXT, url TEXT, username TEXT, timestamp TEXT);");
                }catch(Exception e){}
                try{
                    db.execSQL("CREATE TABLE "+USERNAME+"_projects_temp(pos NUMBER, name TEXT, desc TEXT, url TEXT, username TEXT, timestamp TEXT);");
                }catch(Exception e){}
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

        //To call the onclick function when the 'Go' button at the bottom-right end of the keyboard is clicked after typing the password
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

    /**
     * Thsi function updates the login credentials in the local db. It clears whatever is in the local table 'login'
     * and downloads from Parse again, to get all the latest login credentials.
     * @param username is the username entered in the EditText
     * @param password is the password entered in the EditText
     */
    public void downloadLoginDetails(final String username, final String password) {
        db.execSQL("DELETE FROM login;");

        //Parse query to downlaod from the Parse table 'Login'
        final ParseQuery<ParseObject> query = ParseQuery.getQuery("Login");
        query.findInBackground(new FindCallback<ParseObject>() {
            public void done(List<ParseObject> objects, ParseException e) {
                if (e == null) {
                    for (ParseObject ob : objects) {
                        //Insert each object retrieved from Parse, into the local sqlite table 'login'
                        db.execSQL("INSERT INTO login VALUES('" + ob.getString("username") + "','" + ob.getString("password") + "');");
                    }

                    //Now that the local sqlite db is updated, check if the username-password pair exists now.
                    //If it still doesn't exist, it throws an error and goes to the catch block.
                    Cursor c = db.rawQuery("SELECT username FROM login WHERE username='" + username + "' AND password='" + password + "';", null);
                    try{
                        c.moveToFirst();
                        USERNAME = c.getString(0);
                        c.close();
                        dialog.dismiss();
                        //username-password pair exists in local sqlite table
                        //create the projects and projects_temp tables for this particular user
                        try{
                            db.execSQL("CREATE TABLE "+USERNAME+"_projects(pos NUMBER, name TEXT, desc TEXT, url TEXT, username TEXT, timestamp TEXT);");
                        }catch(Exception exc){}
                        try{
                            db.execSQL("CREATE TABLE "+USERNAME+"_projects_temp(pos NUMBER, name TEXT, desc TEXT, url TEXT, username TEXT, timestamp TEXT);");
                        }catch(Exception ex){}
                        //update the session with the new username
                        db.execSQL("DELETE FROM session;");
                        db.execSQL("INSERT INTO session VALUES('" + username + "');");

                        startActivity(new Intent(Login.this, Splash.class));
                        finish();

                    }catch (Exception ee){
                        //username-password pair still doesn't exist even after update of login credentials.
                        //This means the username-password pair is wrong, or the user still hasn't signed up
                        Log.e(ee.getMessage(),ee.toString());
                        dialog.dismiss();
                        //Toast.makeText(Login.this,"Username and Password do not match!",Toast.LENGTH_LONG).show();
                        //SnackbarManager.show(Snackbar.with(Login.this).text("Username and Password do not match!"));
                        SnackbarManager.show(
                                Snackbar.with(getApplicationContext())
                                        .type(SnackbarType.MULTI_LINE)
                                        .duration(Snackbar.SnackbarDuration.LENGTH_LONG)
                                        .text("Wrong credentials!")
                                        .actionLabel("CLOSE")
                                        .actionListener(new ActionClickListener() {
                                            @Override
                                            public void onActionClicked(Snackbar snackbar) {
                                                SnackbarManager.dismiss();
                                            }
                                        }), Login.this);
                        ((EditText)findViewById(R.id.password)).setText("");
                        ((EditText)findViewById(R.id.username)).setText("");
                    }

                } else {
                    Log.e("PARSE", "Error: " + e.getMessage());
                }
            }
        });
    }

    /**
     * This function is called when the login button is clicked on the screen.
     */
    public void onClickLogin(View v){

        dialog = ProgressDialog.show(this,null,"Please wait...",true);

        String username = ((EditText)findViewById(R.id.username)).getText().toString();
        String password = ((EditText)findViewById(R.id.password)).getText().toString();

        if(username.equals("") || password.equals("")){
            //Toast.makeText(Login.this, "Don't leave the fields empty!", Toast.LENGTH_SHORT).show();
            //SnackbarManager.show(Snackbar.with(Login.this).text("Username and Password do not match!"));
            SnackbarManager.show(
                    Snackbar.with(getApplicationContext())
                            .type(SnackbarType.MULTI_LINE)
                            .duration(Snackbar.SnackbarDuration.LENGTH_LONG)
                            .text("Fields are empty!")
                            .actionLabel("CLOSE")
                            .actionListener(new ActionClickListener() {
                                @Override
                                public void onActionClicked(Snackbar snackbar) {
                                    SnackbarManager.dismiss();
                                }
                            }), Login.this);
            dialog.dismiss();
            return;
        }

        //First, it checks if the username-password pair is already in the local sqlite table. If it's not there, an exception
        //will be thrown and it will go to the catch block.
        Cursor c = db.rawQuery("SELECT username FROM login WHERE username='" + username + "' AND password='" + password + "';", null);
        try{
            c.moveToFirst();
            USERNAME = c.getString(0);
            dialog.dismiss();
            c.close();
            //username-password pair exists in local sqlite table
            //create the projects and projects_temp tables for this particular user
            try{
                db.execSQL("CREATE TABLE "+USERNAME+"_projects(pos NUMBER, name TEXT, desc TEXT, url TEXT, username TEXT, timestamp TEXT);");
            }catch(Exception e){}
            try{
                db.execSQL("CREATE TABLE "+USERNAME+"_projects_temp(pos NUMBER, name TEXT, desc TEXT, url TEXT, username TEXT, timestamp TEXT);");
            }catch(Exception e){}

            //update the session with the new username
            db.execSQL("DELETE FROM session;");
            db.execSQL("INSERT INTO session VALUES('"+USERNAME+"');");

            startActivity(new Intent(Login.this, Splash.class));
            finish();
        }catch (Exception e){
            //username-password pair doesn't exist in the local db. We need to re-download login credentials from Parse.
            if(checkConnection()){
                downloadLoginDetails(username,password);
            }else{
                dialog.dismiss();
                findViewById(R.id.firstTime).setVisibility(View.VISIBLE);
                //Toast.makeText(this,"Username and Password do not match!",Toast.LENGTH_LONG).show();
                //SnackbarManager.show(Snackbar.with(Login.this).text("Username and Password do not match!"));
                SnackbarManager.show(
                        Snackbar.with(getApplicationContext())
                                .type(SnackbarType.MULTI_LINE)
                                .duration(Snackbar.SnackbarDuration.LENGTH_LONG)
                                .text("Wrong credentials!")
                                .actionLabel("CLOSE")
                                .actionListener(new ActionClickListener() {
                                    @Override
                                    public void onActionClicked(Snackbar snackbar) {
                                        SnackbarManager.dismiss();
                                    }
                                }), Login.this);
                ((EditText)findViewById(R.id.username)).setText("");
                ((EditText)findViewById(R.id.password)).setText("");
                findViewById(R.id.username).requestFocus();
            }
        }
    }

    /**
     * A function to create the necessary tables for the app.
     * Each CREATE TABLE statement is in a separate try-catch block, so that, from the second time the app is opened,
     * the error 'Table already exists' is caught by the catch block.
     * If any more tables are to be added, add them in separate try-catch blocks.
     */
    public void createTables() {
        try{
            db.execSQL("CREATE TABLE session(username TEXT);");
        }catch(Exception e){}
        try{
            db.execSQL("CREATE TABLE login(username TEXT,password TEXT);");
        }catch(Exception e){}
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

    /**
     * A function to hide the keyboard - as soon as the login button is clicked, this function hides the keyboard before
     * any processing takes place.
     */
    private void hideKeyboard() {
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager inputManager = (InputMethodManager) this.getSystemService(Context.INPUT_METHOD_SERVICE);
            inputManager.hideSoftInputFromWindow(view.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
        }
    }

    /**
     * A function to make the app go full screen- hides the status bar
     * @param fullscreen a value of true goes full screen, false comes back from full screen
     */
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