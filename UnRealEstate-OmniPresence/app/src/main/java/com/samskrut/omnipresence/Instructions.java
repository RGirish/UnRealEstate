
package com.samskrut.omnipresence;

import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.TextView;

import com.xgc1986.parallaxPagerTransformer.ParallaxPagerTransformer;

public class Instructions extends AppCompatActivity {

    SQLiteDatabase db;
    ViewPager mPager;
    ParallaxAdapter mAdapter;
    int prevPos=0,currPos=0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_parallax);

        //Set the onclick function for the Finish button that appears after all the instruction images are viewed.
        TextView finishButton = (TextView) findViewById(R.id.finishButton);
        finishButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //when Finish is clicked, we need to open Countdown activity.
                Intent mainIntent = new Intent(Instructions.this, Countdown.class);
                Instructions.this.startActivity(mainIntent);
                Instructions.this.finish();
            }
        });

        db = openOrCreateDatabase("omniPresence.db", SQLiteDatabase.CREATE_IF_NECESSARY, null);

        displayInstructionsImages();

        setFullscreen(true);
    }

    /**
     * This function sets up the view pager with the 4 instruction images.
     * A viewpager is like a slidehow of images/other views that you can view through by swiping left/right.
     */
    public void displayInstructionsImages(){
        mPager = (ViewPager) findViewById(R.id.pager);

        //set the basic attributes for the view pager and the adapter
        mPager.setBackgroundColor(0xFF000000);
        ParallaxPagerTransformer pt = new ParallaxPagerTransformer((R.id.image));
        pt.setBorder(20);
        mPager.setPageTransformer(false, pt);
        mAdapter = new ParallaxAdapter(getSupportFragmentManager());
        mAdapter.setPager(mPager);

        //Each page that you want in the view pager should be given as a Bundle to the Adapter.
        Bundle bundle = new Bundle();
        bundle.putInt("pos", R.mipmap.instruction_1);
        ParallaxFragment parallaxFragment = new ParallaxFragment();
        parallaxFragment.setArguments(bundle);
        mAdapter.add(parallaxFragment);

        bundle = new Bundle();
        bundle.putInt("pos", R.mipmap.instruction_2);
        parallaxFragment = new ParallaxFragment();
        parallaxFragment.setArguments(bundle);
        mAdapter.add(parallaxFragment);

        bundle = new Bundle();
        bundle.putInt("pos", R.mipmap.instruction_3);
        parallaxFragment = new ParallaxFragment();
        parallaxFragment.setArguments(bundle);
        mAdapter.add(parallaxFragment);

        bundle = new Bundle();
        bundle.putInt("pos", R.mipmap.instruction_4);
        parallaxFragment = new ParallaxFragment();
        parallaxFragment.setArguments(bundle);
        mAdapter.add(parallaxFragment);


        //Once the user goes to the last page, we need to display the Finish button. For this, we add a PageChangeListener
        mPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {}

            @Override
            public void onPageSelected(int position) {
                //For each page, we keep note of the previous and current page numbers
                prevPos = currPos;
                currPos = position;
                //Page numbers are 0, 1, 2 and 3

                if(currPos == 3 && prevPos == 2){
                    //When user goes from page 2 to 3, we need to fade in the Finish button.
                    TextView finishButton = (TextView) findViewById(R.id.finishButton);
                    finishButton.setText("<Finish>");
                    final Animation in = new AlphaAnimation(0.0f, 1.0f);
                    in.setDuration(500);
                    finishButton.startAnimation(in);
                }else if(currPos == 2 && prevPos == 3){
                    //When user goes from page 3 to 2, we need to fade out the Finish button.
                    final TextView finishButton = (TextView) findViewById(R.id.finishButton);
                    final Animation out = new AlphaAnimation(1.0f, 0.0f);
                    out.setDuration(500);
                    finishButton.startAnimation(out);
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            finishButton.setText("");
                        }
                    }, 450);
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {}
        });


        //Once you set the adapter to the view pager, the adapter class will take care of displaying the pages on screen.
        mPager.setAdapter(mAdapter);
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