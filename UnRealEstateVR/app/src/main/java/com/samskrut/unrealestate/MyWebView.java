
package com.samskrut.unrealestate;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.WindowManager;
import android.webkit.WebChromeClient;
import android.webkit.WebView;

public class MyWebView extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_web_view);
        setFullscreen(true);

        if(!checkConnection()){
            setContentView(R.layout.no_internet);
            return;
        }

        Intent intent = getIntent();
        String url = intent.getStringExtra("url");

        final ProgressDialog dialog = ProgressDialog.show(this,null,"Loading - 0%",true);

        //Set up the webview with the url extracted from the Intent and set a progress listener to update the ProgressDialog
        WebView webView = (WebView)findViewById(R.id.mwv);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.setWebChromeClient(new WebChromeClient() {
            public void onProgressChanged(WebView view, int progress) {
                dialog.setMessage("Loading - "+progress+"%");
                if(progress==100){
                    dialog.dismiss();
                }
            }
        });
        webView.loadUrl(url);
    }

    /**
     * A function to make the app go full screen- hides the status bar
     * @param fullscreen a value of true goes full screen, false comes back from full screen
     */
    private void setFullscreen(boolean fullscreen){
        WindowManager.LayoutParams attrs = getWindow().getAttributes();
        if (fullscreen){
            attrs.flags |= WindowManager.LayoutParams.FLAG_FULLSCREEN;
        }else{
            attrs.flags &= ~WindowManager.LayoutParams.FLAG_FULLSCREEN;
        }
        getWindow().setAttributes(attrs);
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