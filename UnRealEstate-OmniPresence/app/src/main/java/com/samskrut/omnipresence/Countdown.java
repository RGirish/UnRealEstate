package com.samskrut.omnipresence;

import android.content.Intent;
import android.os.CountDownTimer;
import android.os.Handler;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;
import android.widget.TextView;


public class Countdown extends AppCompatActivity {

    CountDownTimer countDownTimer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_countdown);
        setFullscreen(true);

        //this part of the code starts a countdown timer with duration 11 seconds and a change interval of 1 second
        countDownTimer = new CountDownTimer(11000, 1000) {

            public void onTick(long millisUntilFinished) {
                //for every second, this line modifies the textview to display the number of seconds left
                ((TextView)findViewById(R.id.count)).setText(String.valueOf(millisUntilFinished/1000));
            }
            public void onFinish() {
                //once the count down is finished, this function is called. It opens the ProjectList Activity.
                Intent intent = new Intent(Countdown.this, ProjectList.class);
                startActivity(intent);
                finish();
            }
        };

        countDownTimer.start();
    }

    public void onBackPressed(){
        //when the user goes out of the count down activity, we need to cancel the timer, else it will open the ProjectList activity after 10 seconds
        countDownTimer.cancel();
        super.onBackPressed();
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

}