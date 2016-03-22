package orz.kassy.chromecastaudio;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;

import com.google.android.gms.cast.Cast;
import com.google.android.gms.cast.CastDevice;
import com.google.android.gms.cast.MediaInfo;
import com.google.android.gms.cast.MediaMetadata;
import com.google.android.gms.cast.RemoteMediaPlayer;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;

import java.io.IOException;

public class CastActivity extends AppCompatActivity implements View.OnClickListener {

    public static final String INTENT_EX_CAST_DEVICE = "intent_ex_cast_device";
    private static final String TAG = "Cast";

    private int mStatus = 0;
    private static final int STATUS_READY = 1;
    private static final int STATUS_PLAYING = 2;

    private CastDevice mCastDevice;
    private GoogleApiClient mApiClient;
    private String mSessionId;
    private RemoteMediaPlayer mRemoteMediaPlayer;
    private ImageButton mImageButton;
    private ProgressBar mProgressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cast);

        mRemoteMediaPlayer = new RemoteMediaPlayer();
        mCastDevice = (CastDevice) getIntent().getParcelableExtra(INTENT_EX_CAST_DEVICE);

        mImageButton = (ImageButton) findViewById(R.id.imgButtonCast);
        mImageButton.setOnClickListener(this);
        mProgressBar = (ProgressBar) findViewById(R.id.prgCast);

        setTitle(mCastDevice.getFriendlyName());
        standbyCast();
    }

    /**
     * Cast 1. GoogleApiClientを立ち上げる
     */
    private void standbyCast() {
        Cast.CastOptions.Builder apiOptionsBuilder = Cast.CastOptions
                .builder(mCastDevice, mCastClientListener);

        mApiClient = new GoogleApiClient.Builder(this)
                .addApi(Cast.API, apiOptionsBuilder.build())
                .addConnectionCallbacks(mConnectionCallbacks)
                .build();

        mApiClient.connect();
    }


    /**
     * Cast 2. CastListener
     */
    private Cast.Listener mCastClientListener = new Cast.Listener() {
        @Override
        public void onApplicationStatusChanged() {
            if (mApiClient != null) {
                Log.i(TAG, "onApplicationStatusChanged : " + Cast.CastApi.getApplicationStatus(mApiClient));
            }
        }

        @Override
        public void onVolumeChanged() {
            if (mApiClient != null) {
                Log.i(TAG, "onVolumeChanged:" + Cast.CastApi.getVolume(mApiClient));
                // ここでNot connected to a deviceって言われて落ちる
            }
        }

        @Override
        public void onApplicationDisconnected(int errorCode) {
            Log.i(TAG, "onApplicationDisconnected");
        }
    };


    /**
     * Cast 3. GoogleApiClient接続コールバック
     */
    private GoogleApiClient.ConnectionCallbacks mConnectionCallbacks = new GoogleApiClient.ConnectionCallbacks() {
        @Override
        public void onConnected(Bundle connectionHint) {
            Log.d(TAG, "GoogleApiClient onConnected");

            try {
                Cast.CastApi
                        .launchApplication(mApiClient, getString(R.string.app_id), false)
                        .setResultCallback(mResultCallback);
            } catch (Exception ex) {
                Log.d(TAG, ex.toString());
            }
        }

        @Override
        public void onConnectionSuspended(int cause) {
            Log.d(TAG, "GoogleApiClient onConnectionSuspended");
        }
    };

    /**
     * Cast 4. ResultCallback
     */
    private ResultCallback<Cast.ApplicationConnectionResult> mResultCallback = new ResultCallback<Cast.ApplicationConnectionResult>() {
        @Override
        public void onResult(Cast.ApplicationConnectionResult result) {
            Log.i(TAG, "Launch Application onResult");
            Status status = result.getStatus();
            if (status.isSuccess()) {
                mSessionId = result.getSessionId();
                Log.d(TAG, "mSessionID=" + mSessionId);
                setRemoteMedia();
            }
        }
    };

    /**
     * Cast 5. RemoteMediaをセット
     */
    private void setRemoteMedia() {
        try {
            Cast.CastApi.setMessageReceivedCallbacks(mApiClient,
                    mRemoteMediaPlayer.getNamespace(), mRemoteMediaPlayer);
        } catch (IOException e) {
            Log.e(TAG, "Exception while creating media channel", e);
        }

        mRemoteMediaPlayer
                .requestStatus(mApiClient)
                .setResultCallback(mResultCallback2);
    }

    /**
     * Cast 6. RemoteMediaPlayerセットのコールバック
     */
    ResultCallback<RemoteMediaPlayer.MediaChannelResult> mResultCallback2 = new ResultCallback<RemoteMediaPlayer.MediaChannelResult>() {
        @Override
        public void onResult(RemoteMediaPlayer.MediaChannelResult result) {
            Log.i(TAG, "RemoteMediaPlayer onResult");
            if (result.getStatus().isSuccess()) {
                Log.i(TAG, "  success");
                loadRemoteMedia();
            } else {
            }
        }
    };

    /**
     * Cast 7. RemoteMediaPlayerのロード
     */
    private void loadRemoteMedia() {
        MediaMetadata mediaMetadata = new MediaMetadata(MediaMetadata.MEDIA_TYPE_MOVIE);
        mediaMetadata.putString(MediaMetadata.KEY_TITLE, "My Audio");
        MediaInfo mediaInfo = new MediaInfo.Builder(
                "http://kassy-kz.github.io/ChromeCastTest/sound.mp3")
                .setContentType("audio/mp3")
                .setStreamType(MediaInfo.STREAM_TYPE_BUFFERED)
                .setMetadata(mediaMetadata)
                .build();
        try {
            Log.i(TAG, "mediaplayer load");
            mRemoteMediaPlayer
                    .load(mApiClient, mediaInfo, false)
                    .setResultCallback(mResultCallback3);

        } catch (IllegalStateException e) {
            Log.e(TAG, "Problem occurred with media during loading", e);
        } catch (Exception e) {
            Log.e(TAG, "Problem opening media during loading", e);
        }
    }

    /**
     * Cast 8. RemoteMediaPlayerのロードのコールバック
     */
    ResultCallback<RemoteMediaPlayer.MediaChannelResult> mResultCallback3 = new ResultCallback<RemoteMediaPlayer.MediaChannelResult>() {
        @Override
        public void onResult(RemoteMediaPlayer.MediaChannelResult result) {
            Log.i(TAG, "Media load onResult : " + result.getStatus());
            if (result.getStatus().isSuccess()) {
                Log.i(TAG, "Media loaded successfully");
                mProgressBar.setVisibility(View.GONE);
                mImageButton.setImageResource(R.mipmap.ic_play);
                mStatus = STATUS_READY;
            }
        }
    };


    ResultCallback<RemoteMediaPlayer.MediaChannelResult> mResultCallback4 = new ResultCallback<RemoteMediaPlayer.MediaChannelResult>() {
        @Override
        public void onResult(RemoteMediaPlayer.MediaChannelResult mediaChannelResult) {
            Log.i(TAG, "Media player paused");
        }
    };

    /**
     * ボタンをおした時
     * @param v
     */
    @Override
    public void onClick(View v) {
        switch (mStatus) {
            // 停止中のとき
            case STATUS_READY:
                mRemoteMediaPlayer.play(mApiClient);
                mStatus = STATUS_PLAYING;
                mImageButton.setImageResource(R.mipmap.ic_pause);
                break;

            // 再生中のとき
            case STATUS_PLAYING:
                mRemoteMediaPlayer.pause(mApiClient);
                mStatus = STATUS_READY;
                mImageButton.setImageResource(R.mipmap.ic_play);
                break;
        }
    }
}