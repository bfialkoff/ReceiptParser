package com.app.betzalel.captureimage;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.RotatedRect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

public class Preprocessor {
    private Mat nonZeros;
    private Mat textOnlymat = new Mat();
    private Mat mask;
    private MatOfPoint points;
    private Mat rotationMat;
    private Mat drawnRect;

    private RotatedRect minRotatedRect;

    public Mat getMask(){
        return this.mask;
    }

    public Mat getDrawnRect(){
        return this.drawnRect;
    }

    public Mat isolateAndRotateTextMat(Mat textMat) {
        this.mask = this.getTextMask(textMat);
        textMat.copyTo(textOnlymat, this.mask);
        textMat = rotate(textMat);
        return textMat;
    }

    public void releaseAll() {
        this.nonZeros.release();
        this.textOnlymat.release();
        this.points.release();
        this.rotationMat.release();
        this.drawnRect.release();
    }

    private void getMinRotatedRect(Mat textMat) {
        this.nonZeros = Mat.zeros(textMat.size(), textMat.channels());
        //should erode textMat before using findNonZero
        Core.findNonZero(textMat, nonZeros);
        MatOfPoint points = new MatOfPoint(nonZeros);
        MatOfPoint2f points2f = new MatOfPoint2f(points.toArray());//this is slow...see above comment
        this.minRotatedRect = Imgproc.minAreaRect(points2f);
    }

    private Mat drawRect(RotatedRect minRotatedRect, Size size, int type) {
        this.drawnRect = Mat.zeros(size, type);
        Point points[] = new Point[4];
        minRotatedRect.points(points);
        for (int i = 0; i < 4; ++i) {
            Core.line(this.drawnRect, points[i], points[(i + 1) % 4], new Scalar(255));
        }
        return drawnRect;
    }

    private Mat getTextMask(Mat textMat) {
        this.getMinRotatedRect(textMat);
        double angle = this.minRotatedRect.angle;
        this.drawnRect = this.drawRect(this.minRotatedRect, textMat.size(), textMat.type());
        this.nonZeros = Mat.zeros(drawnRect.size(), drawnRect.channels());
        Core.findNonZero(this.drawnRect, nonZeros);
        this.points = new MatOfPoint(nonZeros);
        this.mask = Mat.zeros(this.drawnRect.size(), this.drawnRect.type());
        Core.fillConvexPoly(mask, points, new Scalar(255));
        return mask;
    }


    private Mat rotate(Mat textMat) {
        double angle = this.minRotatedRect.angle;
        if (getRotatedRectPoint(this.minRotatedRect)[0].x > this.minRotatedRect.center.x)
            angle = angle + 90;

        Point pt = minRotatedRect.center;//center of the bounding box
        this.rotationMat = Imgproc.getRotationMatrix2D(pt, angle, 1.0);//angle is negative. this is only good when pt[0] is the bottom left vertex of the rect, otherwise not good. need to have a check
        Imgproc.warpAffine(this.textOnlymat, textMat, this.rotationMat, new Size(textMat.cols(), textMat.rows()));//does rotatedTextOnlyMat need to be initialized?
        return textMat;
    }

    private Point[] getRotatedRectPoint(RotatedRect minRotatedRect) {
        Point[] p = new Point[4];
        minRotatedRect.points(p);
        return p;
    }
}


