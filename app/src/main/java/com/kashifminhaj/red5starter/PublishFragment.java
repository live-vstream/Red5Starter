package com.kashifminhaj.red5starter;


import android.hardware.Camera;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.red5pro.streaming.R5Connection;
import com.red5pro.streaming.R5Stream;
import com.red5pro.streaming.R5StreamProtocol;
import com.red5pro.streaming.config.R5Configuration;
import com.red5pro.streaming.event.R5ConnectionEvent;
import com.red5pro.streaming.event.R5ConnectionListener;
import com.red5pro.streaming.source.R5Camera;
import com.red5pro.streaming.source.R5Microphone;


/**
 * A simple {@link Fragment} subclass.
 */
public class PublishFragment extends Fragment implements SurfaceHolder.Callback {

    public R5Configuration configuration;

    protected Camera camera;
    protected boolean isPublishing = false;
    protected R5Stream stream;


    public PublishFragment() {
        // Required empty public constructor
    }

    public static PublishFragment newInstance() {
        PublishFragment fragment = new PublishFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_publish, container, false);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        configuration = new R5Configuration(R5StreamProtocol.RTSP, "192.168.43.219",  8554, "live", 1.0f);
    }

    private void preview() {
        camera = Camera.open(Camera.CameraInfo.CAMERA_FACING_FRONT);
        SurfaceView surface = (SurfaceView) getActivity().findViewById(R.id.surfaceView);
        surface.getHolder().addCallback(this);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        try {
            camera.setPreviewDisplay(holder);
            camera.setDisplayOrientation(90);
            camera.startPreview();
        }
        catch(Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {

    }

    @Override
    public void onResume() {
        super.onResume();
        preview();
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        Button publishButton = (Button) getActivity().findViewById(R.id.publishButton);
        publishButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onPublishToggle();
            }
        });
    }

    private void onPublishToggle() {
        Button publishButton = (Button) getActivity().findViewById(R.id.publishButton);
        if(isPublishing) {
            stop();
        }
        else {
            start();
        }
        isPublishing = !isPublishing;
        publishButton.setText(isPublishing ? "stop" : "start");
    }

    public void start() {
        camera.stopPreview();

        stream = new R5Stream(new R5Connection(configuration));



        stream.setLogLevel(R5Stream.LOG_LEVEL_DEBUG);

        stream.connection.addListener(new R5ConnectionListener() {
            @Override
            public void onConnectionEvent(R5ConnectionEvent event) {
                Log.d("publish","connection event code "+event.value()+"\n");
                switch(event.value()){
                    case 0://open
                        System.out.println("Connection Listener - Open");
                        break;
                    case 1://close
                        System.out.println("Connection Listener - Close");
                        break;
                    case 2://error
                        System.out.println("Connection Listener - Error: " + event.message);
                        break;

                }
            }
        });

        stream.setListener(new R5ConnectionListener() {
            @Override
            public void onConnectionEvent(R5ConnectionEvent event) {
                switch (event) {
                    case CONNECTED:
                        System.out.println("Stream Listener - Connected");
                        break;
                    case DISCONNECTED:
                        System.out.println("Stream Listener - Disconnected");
                        System.out.println(event.message);
                        break;
                    case START_STREAMING:
                        System.out.println("Stream Listener - Started Streaming");
                        break;
                    case STOP_STREAMING:
                        System.out.println("Stream Listener - Stopped Streaming");
                        break;
                    case CLOSE:
                        System.out.println("Stream Listener - Close");
                        break;
                    case TIMEOUT:
                        System.out.println("Stream Listener - Timeout");
                        break;
                    case ERROR:
                        System.out.println("Stream Listener - Error: " + event.message);
                        break;
                }
            }
        });

        SurfaceView cameraView = (SurfaceView) getActivity().findViewById(R.id.surfaceView);

        stream.setView(cameraView);

        R5Camera r5Camera = new R5Camera(camera, cameraView.getWidth(), cameraView.getHeight());
        R5Microphone r5Microphone = new R5Microphone();

        stream.attachCamera(r5Camera);
        stream.attachMic(r5Microphone);

        stream.publish("red5prostream", R5Stream.RecordType.Live);
        camera.startPreview();
    }

    public void stop() {
        if(stream != null) {
            stream.stop();
            camera.startPreview();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if(isPublishing) {
            onPublishToggle();
        }
    }
}
