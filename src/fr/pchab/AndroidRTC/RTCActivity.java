
package fr.pchab.AndroidRTC;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Point;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import org.json.JSONException;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.VideoRenderer;

public class RTCActivity extends Activity implements WebRtcClient.RTCListener {
    private final static int VIDEO_CALL_SENT = 666;
    private VideoStreamsView vsv;
    private WebRtcClient client;
    private String mSocketAddress;
    private LinearLayout mVideoViewContainer;
    private EditText mEditText;
    private Button mCallBtn;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_call);

        mSocketAddress = "http://" + getResources().getString(R.string.host);
        mSocketAddress += (":" + getResources().getString(R.string.port) + "/");

        PeerConnectionFactory.initializeAndroidGlobals(this);

        // Camera display view
        Point displaySize = new Point();
        getWindowManager().getDefaultDisplay().getSize(displaySize);

        vsv = new VideoStreamsView(this, displaySize);
        client = new WebRtcClient(this, mSocketAddress);

        // view init
        mVideoViewContainer = (LinearLayout) findViewById(R.id.video_view_container);
        mVideoViewContainer.addView(vsv);

        mEditText = (EditText) findViewById(R.id.edit_text);
        mEditText.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return true;
            }
        });

        mCallBtn = (Button) findViewById(R.id.call_btn);
        mCallBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                client.call(mEditText.getText().toString());
            }
        });
    }

    public void onConfigurationChanged(Configuration newConfig)
    {
        super.onConfigurationChanged(newConfig);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
    }

    @Override
    public void onPause() {
        super.onPause();
        vsv.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        vsv.onResume();
    }

    @Override
    public void onCallReady(String callId) {
        startCam();
    }

    public void answer(String callerId) throws JSONException {
        client.sendMessage(callerId, "init", null);
        startCam();
    }

    public void startCam() {
        // Camera settings
        client.setCamera("front", "640", "480");
        // no need create room
        //client.start("android_test", true);
    }

    @Override
    public void onStatusChanged(final String newStatus) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getApplicationContext(), newStatus, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onLocalStream(MediaStream localStream) {
        localStream.videoTracks.get(0).addRenderer(new VideoRenderer(new VideoCallbacks(vsv, 0)));
    }

    @Override
    public void onAddRemoteStream(MediaStream remoteStream, int endPoint) {
        remoteStream.videoTracks.get(0).addRenderer(new VideoRenderer(new VideoCallbacks(vsv, endPoint)));
        vsv.shouldDraw[endPoint] = true;
    }

    @Override
    public void onRemoveRemoteStream(MediaStream remoteStream, int endPoint) {
        remoteStream.videoTracks.get(0).dispose();
        vsv.shouldDraw[endPoint] = false;
    }

    // Implementation detail: bridge the VideoRenderer.Callbacks interface to the
    // VideoStreamsView implementation.
    private class VideoCallbacks implements VideoRenderer.Callbacks {
        private final VideoStreamsView view;
        private final int stream;

        public VideoCallbacks(VideoStreamsView view, int stream) {
            this.view = view;
            this.stream = stream;
        }

        @Override
        public void setSize(final int width, final int height) {
            view.queueEvent(new Runnable() {
                public void run() {
                    view.setSize(stream, width, height);
                }
            });
        }

        @Override
        public void renderFrame(VideoRenderer.I420Frame frame) {
            view.queueFrame(stream, frame);
        }
    }
}
