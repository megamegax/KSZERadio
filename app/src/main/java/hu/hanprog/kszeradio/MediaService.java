package hu.hanprog.kszeradio;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.wifi.WifiManager;
import android.os.Binder;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.v4.app.TaskStackBuilder;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;

/**
 * Created by MegaX on 2015. 11. 06..
 */
public class MediaService extends Service implements MediaPlayer.OnPreparedListener, AudioManager.OnAudioFocusChangeListener {
    private static final String ACTION_PLAY = "hu.hanprog.action.PLAY";
    private static final int ONGOING_NOTIFICATION_ID = 1;
    MediaPlayer mMediaPlayer = null;
    private WifiManager.WifiLock wifiLock;
    private IBinder mBinder = new LocalBinder();
    private boolean alreadyStarted = false;

    public int onStartCommand(Intent intent, int flags, int startId) {
        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        int result = audioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN);
        if (result != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            // could not get audio focus
            Log.e("tag", "nincs fokusz");
        }
        wifiLock = ((WifiManager) getSystemService(Context.WIFI_SERVICE))
                .createWifiLock(WifiManager.WIFI_MODE_FULL, "mylock");

        Notification.Builder mBuilder =
                new Notification.Builder(this)
                        .setSmallIcon(R.mipmap.ic_launcher)
                        .setContentTitle("KSZE Rádió")
                        .setContentText("A háttérben fut...");


        startForeground(ONGOING_NOTIFICATION_ID, mBuilder.build());
        if (alreadyStarted) {
            start();
        }
        if (intent.getAction() != null) {
            if (intent.getAction().equals("PAUSE")) {
                if (isPlaying()) {
                    stop();
                    Log.e("service", "pause");
                    Toast.makeText(getApplicationContext(), "pause", Toast.LENGTH_SHORT).show();
                }
            }
            if (intent.getAction().equals("CONTINUE")) {
                if (alreadyStarted) {
                    if (!isPlaying()) {
                        start();
                    }
                    Log.e("service", "continue");
                    Toast.makeText(getApplicationContext(), "continue", Toast.LENGTH_SHORT).show();
                }
            }
        }
        return Service.START_STICKY;
    }

    public void onAudioFocusChange(int focusChange) {
        switch (focusChange) {
            case AudioManager.AUDIOFOCUS_GAIN:
                // resume playback
                if (mMediaPlayer == null) initMediaPlayer();
                else if (!mMediaPlayer.isPlaying()) mMediaPlayer.start();
                mMediaPlayer.setVolume(1.0f, 1.0f);
                break;

            case AudioManager.AUDIOFOCUS_LOSS:
                // Lost focus for an unbounded amount of time: stop playback and release media player
                if (mMediaPlayer.isPlaying()) mMediaPlayer.stop();
                mMediaPlayer.release();
                mMediaPlayer = null;
                break;

            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                // Lost focus for a short time, but we have to stop
                // playback. We don't release the media player because playback
                // is likely to resume
                if (mMediaPlayer.isPlaying()) mMediaPlayer.pause();
                break;

            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                // Lost focus for a short time, but it's ok to keep playing
                // at an attenuated level
                if (mMediaPlayer.isPlaying()) mMediaPlayer.setVolume(0.1f, 0.1f);
                break;
        }
    }

    private void initMediaPlayer() {
        mMediaPlayer = new MediaPlayer();
        mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mMediaPlayer.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);

        wifiLock.acquire();
        try {
            mMediaPlayer.setDataSource(AppFields.URL);
        } catch (IOException e) {
            e.printStackTrace();
        }
        mMediaPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
            @Override
            public boolean onError(MediaPlayer mp, int what, int extra) {
                wifiLock.release();
                mp.reset();
                initMediaPlayer();
                return true;
            }
        });
        mMediaPlayer.setOnPreparedListener(this);
        mMediaPlayer.prepareAsync(); // prepare async to not block main thread
    }

    @Override
    public IBinder onBind(Intent arg0) {
        return mBinder;
    }

    public class LocalBinder extends Binder {
        MediaService getService() {
            return MediaService.this;
        }
    }

    /**
     * Called when MediaPlayer is ready
     */
    public void onPrepared(MediaPlayer player) {
        player.start();
    }

    public void start() {
        if (alreadyStarted) {
            togglePause();
        } else {
            alreadyStarted = true;
            initMediaPlayer();
            wifiLock.acquire();
        }
    }

    public boolean isPlaying() {
        if (mMediaPlayer == null) {
            return false;
        }
        return mMediaPlayer.isPlaying();
    }

    public void togglePause() {
        if (mMediaPlayer.isPlaying()) {
            if (wifiLock.isHeld()) {
                wifiLock.release();
            }
            mMediaPlayer.pause();
        } else {
            mMediaPlayer.start();
            wifiLock.acquire();
        }
    }

    public void stop() {
        mMediaPlayer.stop();
        wifiLock.release();
        alreadyStarted = false;

    }

    @Override
    public void onDestroy() {
        if (mMediaPlayer != null) mMediaPlayer.release();
        alreadyStarted = false;
        wifiLock.release();

    }
}