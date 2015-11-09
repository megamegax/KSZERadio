package hu.hanprog.kszeradio;

import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;

/**
 * Created by MegaX on 2015. 11. 06..
 */
public class MusicIntentReceiver extends android.content.BroadcastReceiver {
    @Override
    public void onReceive(Context ctx, Intent intent) {
        if (intent.getAction().equals(
                android.media.AudioManager.ACTION_AUDIO_BECOMING_NOISY)) {
            Intent i = new Intent(ctx, MediaService.class);
            i.setAction("PAUSE");
            ctx.getApplicationContext().startService(i);
            // signal your service to stop playback
            // (via an Intent, for instance)
        } else if (intent.getAction().equals(AudioManager.ACTION_HEADSET_PLUG)) {
            Intent i = new Intent(ctx, MediaService.class);
            i.setAction("CONTINUE");
            ctx.getApplicationContext().startService(i);
        }
    }
}
