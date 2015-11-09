package hu.hanprog.kszeradio;

import android.app.ActivityManager;
import android.app.ApplicationErrorReport;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.WebView;
import android.widget.Toast;

import hu.hanprog.kszeradio.MediaService.LocalBinder;

public class MainActivity extends AppCompatActivity {
    private static final String ACTION_PLAY = "hu.hanprog.action.PLAY";
    private MediaService mediaService;
    private boolean mBound = false;
    private FloatingActionButton fab;
    private WebView webView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle("KSZE Rádió");
        toolbar.setSubtitle("„Azért a hit hallásból van, a hallás pedig Isten Igéje (Rhema) által.\" Róma.10,17");

        setSupportActionBar(toolbar);
        webView = (WebView) findViewById(R.id.webView);
        webView.loadUrl("file:///android_asset/chat.html");
        fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (mBound) {
                            if (!mediaService.isPlaying()) {
                                fab.setImageDrawable(getResources().getDrawable(android.R.drawable.ic_media_pause));
                            } else {
                                fab.setImageDrawable(getResources().getDrawable(android.R.drawable.ic_media_play));
                            }
                            mediaService.start();
                        }
                    }
                }
        );
    }

    private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Bind to LocalService
        if (!isMyServiceRunning(MediaService.class)) {
            startService(new Intent(getBaseContext(), MediaService.class));
        }
        Intent intent = new Intent(this, MediaService.class);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mediaService != null) {
            if (mediaService.isPlaying()) {
                fab.setImageDrawable(getResources().getDrawable(android.R.drawable.ic_media_pause));
            } else {
                fab.setImageDrawable(getResources().getDrawable(android.R.drawable.ic_media_play));
            }
        } else {
            fab.setImageDrawable(getResources().getDrawable(android.R.drawable.ic_media_play));
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        // Unbind from the service
        if (mBound) {
            unbindService(mConnection);
            mBound = false;
        }
    }

    private ServiceConnection mConnection = new ServiceConnection() {

        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            LocalBinder binder = (LocalBinder) service;
            mediaService = binder.getService();
            mBound = true;
        }

        public void onServiceDisconnected(ComponentName className) {
            mediaService = null;
            mBound = false;
        }
    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


}