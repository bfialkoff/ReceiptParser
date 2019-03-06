package com.app.betzalel.captureimage;

import java.util.ArrayList;
import java.util.List;

import org.opencv.android.JavaCameraView;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.hardware.Camera;
import android.hardware.Camera.PictureCallback;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;


public class CaptureImageView extends JavaCameraView implements PictureCallback {

    private static final String TAG = "Sample::Tutorial3View";
    public Bitmap scaledBmp;
    Context context;
    private Button okButton, retryButton;
    private ImageButton captureImageButton;

    public CaptureImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
        ((Activity)context).findViewById(android.R.id.content);
        }

    public void getCameraInstance() {
        if (mCamera == null)
            super.mCamera = Camera.open();
    }

    public void refocusFrame(View v, MotionEvent event) {
        if (mCamera == null)
            return;
        mCamera.cancelAutoFocus();//still AFs

        Rect focusRect = new Rect(-1000, -1000, 1000, 1000);
        Camera.Parameters parameters = mCamera.getParameters();
        if (!parameters.getFocusMode().equals( //i added the !
                Camera.Parameters.FOCUS_MODE_AUTO)) {
            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
        }

        if (parameters.getMaxNumFocusAreas() > 0) {
            List<Camera.Area> mylist = new ArrayList<Camera.Area>();
            mylist.add(new Camera.Area(focusRect, 1000));
            parameters.setFocusAreas(mylist);
        }

        try {
            mCamera.cancelAutoFocus();
            mCamera.setParameters(parameters);
            mCamera.startPreview();
            mCamera.autoFocus(new Camera.AutoFocusCallback() {
                @Override
                public void onAutoFocus(boolean success, Camera camera) {
                    if (camera.getParameters().getFocusMode().equals(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
                        Camera.Parameters parameters = camera.getParameters();
                        parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
                        if (parameters.getMaxNumFocusAreas() > 0) {
                            parameters.setFocusAreas(null);
                        }
                        camera.setParameters(parameters);
                        camera.startPreview();
                    }
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(context,e.getMessage() ,Toast.LENGTH_SHORT).show();

        }
    }

    public boolean takePicture() {
        Log.i(TAG, "Taking picture");
        mCamera.setPreviewCallback(null);
        // PictureCallback is implemented by the current class
        mCamera.takePicture(null, null,  this);
        return true;
    }

    @Override
    public void onPictureTaken(byte[] data, Camera camera) {
        Log.i(TAG, "Saving a bitmap to file");
        getButtons();
        scaledBmp = setReducedImageSize(data);
        setButtonState();
    }

    private Bitmap setReducedImageSize(byte [] data) {
        int targetImageViewWidth = getWidth();
        int targetImageViewHeight = getHeight();

        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        bmOptions.inJustDecodeBounds = true;

        int cameraImageWidth = bmOptions.outWidth;
        int cameraImageHeight = bmOptions.outHeight;

        bmOptions.inSampleSize = (int) Math.min(cameraImageWidth / targetImageViewWidth, cameraImageHeight / targetImageViewHeight);
        bmOptions.inJustDecodeBounds = false;

        return BitmapFactory.decodeByteArray(data, 0, data.length,bmOptions);
    }

    public boolean resetButtonState(){
        mCamera.startPreview();
        captureImageButton.setVisibility(View.VISIBLE);
        okButton.setVisibility(View.GONE);
        retryButton.setVisibility(View.GONE);
        return false;
    }

    private boolean setButtonState(){
        mCamera.stopPreview();
        okButton.setVisibility(View.VISIBLE);
        retryButton.setVisibility(View.VISIBLE);
        captureImageButton.setVisibility(View.GONE);
        return true;

    }

    public void clearButtons(){
        okButton.setVisibility(View.GONE);
        retryButton.setVisibility(View.GONE);
    }
    private void getButtons() {
        if(okButton == null){
            okButton = (Button) ((Activity)context).findViewById(R.id.ok_button);
            retryButton = (Button) ((Activity)context).findViewById(R.id.retry_button);
            captureImageButton = (ImageButton) ((Activity)context).findViewById(R.id.capture_image_button);
        }
    }
}
