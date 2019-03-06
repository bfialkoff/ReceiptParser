package com.app.betzalel.captureimage;

import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.support.v4.app.NavUtils;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.android.Utils;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import static org.opencv.imgproc.Imgproc.MORPH_ELLIPSE;
//TODO make sure Mat convention is consisten with what is done in PreProcessor and
// for the love of god clean this code!!!

public class ImageProcessing extends AppCompatActivity {

    //initialized in the last activity
    public static Bitmap bitmap;
    int width, height;
    ImageView imageView;
    Button changeImageButton;

    Mat colorImg = new Mat();//source image
    Mat greyMat = new Mat();//will be used to isolate text, may contain other noise
    Mat textMat = new Mat();//destination text
    Mat bounding_box = new Mat();//this will be the bounding box mask
    Mat textOnlymat = new Mat();//text
    Mat rotatedTextOnlyMat = new Mat();
    Mat edgesMat = new Mat();
    Mat linesMat = new Mat();
    Mat linesMask = new Mat();
    Mat cleanEdges = new Mat();
    Preprocessor preProcessor;
    int toggle = 2;
    double PI = 3.14159;


    @Override
    protected void onDestroy() {
        super.onDestroy();
        colorImg.release();
        greyMat.release();
        textMat.release();
        bounding_box.release();
        textOnlymat.release();
        edgesMat.release();
        cleanEdges.release();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home)
            NavUtils.navigateUpFromSameTask(this);
        return true;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_processing);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LOCKED);

        Bundle extras = getIntent().getExtras();
        width = extras.getInt("Width");
        height = extras.getInt("Height");
        imageView = (ImageView) findViewById(R.id.imageView);
        changeImageButton = (Button) findViewById(R.id.changeImageButton);

        //create base rbg mat
        setRBGMat(colorImg);
        //edgesMat = edges(colorImg, edgesMat);
        edges(colorImg, edgesMat);
        cleanEdges = Mat.zeros(edgesMat.size(), edgesMat.type());
        removeSmallObjects(edgesMat, cleanEdges);
        lines(cleanEdges);

        displayMat(colorImg);
        ////get text from paper
        //textMat = getTextMat(colorImg);//try to improve on textMats thresholding

        //preProcessor = new Preprocessor();

        //rotatedTextOnlyMat = preProcessor.isolateAndRotateTextMat(textMat.clone());//something with pointers, need to send a copy...


        changeImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch (ImageProcessing.this.toggle) {
                    case 1:
                        displayMat(colorImg);
                        ImageProcessing.this.toggle = 2;
                        break;

                    case 2:
                        displayMat(edgesMat);
                        ImageProcessing.this.toggle = 3;
                        break;
                    case 3:
                        //displayMat(preProcessor.getDrawnRect());
                        displayMat(cleanEdges);
                        ImageProcessing.this.toggle = 4;
                        break;
                    case 4:
                        //displayMat(preProcessor.getMask());
                        ImageProcessing.this.toggle = 5;
                        displayMat(linesMask);
                        ImageProcessing.this.toggle = 1;
                        break;
                    case 5:
                        displayMat(textMat);
                        ImageProcessing.this.toggle = 6;
                        break;
                    case 6:
                        displayMat(rotatedTextOnlyMat);
                        ImageProcessing.this.toggle = 1;
                        break;
                }

            }
        });

    }

    private void displayMat(Mat mat) {
        Bitmap img = Bitmap.createBitmap(mat.cols(), mat.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(mat, img);
        imageView.setImageBitmap(img);
    }

    private void removeSmallObjects(Mat src, Mat dst) {
        List<MatOfPoint> contours = new ArrayList<>();
        Imgproc.findContours(src, contours, new Mat(), Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE);

        Collections.sort(contours, new Comparator<MatOfPoint>() {
            @Override
            public int compare(final MatOfPoint lhs, MatOfPoint rhs) {
                double area1 = Imgproc.contourArea(rhs);
                double area2 = Imgproc.contourArea(lhs);
                if (area1 > area2) {
                    return 1;
                }
                if (area1 < area2) {
                    return -1;
                }
                return 0;
            }
        });
        Imgproc.drawContours(dst, contours, 0, new Scalar(255), 4);
        Iterator<MatOfPoint> iterator = contours.iterator();
    }

    private void edges(Mat rgbMat, Mat edgesMat) {
        Imgproc.blur(rgbMat, edgesMat, new Size(15, 15));
        toGreyScale(rgbMat, edgesMat);
        Imgproc.Canny(edgesMat, edgesMat, 10, 100);
        Mat dilateElement = Imgproc.getStructuringElement(MORPH_ELLIPSE, new Size(3, 3));
        Imgproc.morphologyEx(edgesMat, edgesMat, Imgproc.MORPH_DILATE, dilateElement);
        //return edgesMat;
    }

    private void lines(Mat edgesMat) {
        Imgproc.HoughLinesP(edgesMat, linesMat, 1, 2 * PI / 180, 150, 110, 120);
        linesMask = Mat.zeros(edgesMat.size(), edgesMat.type());
        // Draw the lines
        for (int x = 0; x < linesMat.cols(); x++) {
            double[] l = linesMat.get(0, x);
            Core.line(linesMask, new Point(l[0], l[1]), new Point(l[2], l[3]), new Scalar(255), 6, Core.LINE_AA, 0);
        }
    }

    private void setRBGMat(Mat colorImg) {
        //create mat from bitmap
        Bitmap bmp32 = bitmap.copy(Bitmap.Config.ARGB_8888, true);
        Utils.bitmapToMat(bmp32, colorImg);

        //rotate to the correct orientation
        Core.transpose(colorImg, colorImg);
        Core.flip(colorImg, colorImg, 1);
    }

    private void toGreyScale(Mat src, Mat dst) {
        Imgproc.cvtColor(src, dst, Imgproc.COLOR_RGBA2GRAY);
    }

    private void getTextMat(Mat src, Mat dst) {
        toGreyScale(src, dst);
        Imgproc.adaptiveThreshold(dst, dst, 255, Imgproc.ADAPTIVE_THRESH_MEAN_C, Imgproc.THRESH_BINARY_INV, 65, 40);
    }

}
