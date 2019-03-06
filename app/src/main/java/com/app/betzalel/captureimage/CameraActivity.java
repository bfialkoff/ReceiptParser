package com.app.betzalel.captureimage;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;


public class CameraActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2, View.OnTouchListener {


    private static final String TAG = "OCVSample::Activity";

    private CaptureImageView mOpenCvCameraView;
    private ImageButton captureImageButton;
    private Button okButton, retryButton;
    private boolean isPaused = false;

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    Log.i(TAG, "OpenCV loaded successfully");
                    mOpenCvCameraView.enableView();
                    mOpenCvCameraView.setOnTouchListener(CameraActivity.this);
                }
                break;
                default: {
                    super.onManagerConnected(status);
                }
                break;
            }
        }
    };

    public CameraActivity() {
        Log.i(TAG, "Instantiated new " + this.getClass());
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "called onCreate");
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.activity_camera);
        mOpenCvCameraView = (CaptureImageView) findViewById(R.id.java_surface_view);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LOCKED);

        captureImageButton = (ImageButton) findViewById(R.id.capture_image_button);
        okButton = (Button) findViewById(R.id.ok_button);
        retryButton = (Button) findViewById(R.id.retry_button);

        mOpenCvCameraView.getCameraInstance();
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);
        captureImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                captureFrame();
            }
        });

        retryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                isPaused = mOpenCvCameraView.resetButtonState();
            }
        });
        okButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                isPaused = mOpenCvCameraView.resetButtonState();
                Context context = CameraActivity.this;
                Intent startImgProc = new Intent(context, ImageProcessing.class);
                ImageProcessing.bitmap = mOpenCvCameraView.scaledBmp;
                startImgProc.putExtra("Width", mOpenCvCameraView.getWidth());
                startImgProc.putExtra("Height", mOpenCvCameraView.getHeight());
                mOpenCvCameraView.clearButtons();
                mOpenCvCameraView.callDisconnectCamera();
                context.startActivity(startImgProc);
            }
        });
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if(isPaused){
            return false;
        }

        int touchAction = event.getAction();
        //confirm values being used
        switch (touchAction) {
            case MotionEvent.ACTION_DOWN:
                mOpenCvCameraView.refocusFrame(v, event);
                break;
        }
        return false;
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mOpenCvCameraView != null) {
            mOpenCvCameraView.disableView();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_3, this, mLoaderCallback);
        captureImageButton.setVisibility(View.VISIBLE);
    }

    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null) {
            mOpenCvCameraView.disableView();
        }
    }

    public void onCameraViewStarted(int width, int height) {
    }

    public void onCameraViewStopped() {
    }

    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        return inputFrame.rgba();
    }

    public void captureFrame() {
        Log.i(TAG, "onTouch event");
        isPaused = mOpenCvCameraView.takePicture();
    }
}
