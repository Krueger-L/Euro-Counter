package larskrueger.eurocounter;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfInt;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.*;
import org.opencv.core.*;

import java.util.ArrayList;
import java.util.List;

import static org.opencv.imgproc.Imgproc.circle;


public class ImageProcessor {

    Mat originalMat;
    //MatOfPoint contour;
    double sheetArea;
    Rect sheet;
    int coin1,coin2,coin3,coin4,coin5,coin6,coin7,coin8;
    ArrayList<BoundingBoxElement> boundingBoxElements = new ArrayList<BoundingBoxElement>();
    ArrayList<BoundingBoxElement> boundingBoxElementsFinal = new ArrayList<BoundingBoxElement>();
    double debugQuota;

    //Mat houghGrayMat;
    Mat debugMat;

    // convert preview data to CV Mat, reset coin count and start processing
    public void setOriginalMat(byte[] data, int width, int height){
        Mat yuvMat = new Mat(height+height/2, width, CvType.CV_8UC1);
        Mat rgba = new Mat(height, width, CvType.CV_8UC4);
        if(data.length>0){
            yuvMat.put(0, 0, data);
        }
        Imgproc.cvtColor(yuvMat, rgba, Imgproc.COLOR_YUV420sp2RGB);
        originalMat = rgba;

        coinReset();

        find(originalMat);

        // debugging
        /*
        System.out.println("Coin1" + coin1);
        System.out.println("Coin2" + coin2);
        System.out.println("Coin3" + coin3);
        System.out.println("Coin4" + coin4);
        System.out.println("Coin5" + coin5);
        System.out.println("Coin6" + coin6);
        System.out.println("Coin7" + coin7);
        System.out.println("Coin8" + coin8);
        */
    }

    // calculate value of coins
    public String coinSum(){
        int sum = coin1*1 + coin2*2 +  coin3*5 +  coin4*10 +  coin5*20 +  coin6*50 +  coin7*100 +  coin8*200;
        int sumH = sum/100;
        int sumL = sum%100;
        String s;
        if (sumL<10) {
            s = sumH + ",0" + sumL;
        }
        else{
            s = sumH + "," + sumL;
        }
        return s;
    }

    // getdebugMat is used by MainActivity for displaying purpose
    public Mat getdebugMat(){
        return debugMat;
    }

    // calculate angle between three points
    private static double angle(Point p1, Point p2, Point p0 ) {
        double dx1 = p1.x - p0.x;
        double dy1 = p1.y - p0.y;
        double dx2 = p2.x - p0.x;
        double dy2 = p2.y - p0.y;
        return (dx1 * dx2 + dy1 * dy2) / Math.sqrt((dx1 * dx1 + dy1 * dy1) * (dx2 * dx2 + dy2 * dy2) + 1e-10);
    }

    // calculate distance between two points
    private double distance(int p1X, int p1Y, int p2X, int p2Y){
        double d = Math.sqrt(Math.pow((p1X-p2X), 2)+Math.pow((p1Y-p2Y), 2));
        return d;
    }

    // check boundingboxes for relevancy
    private void finalizeBoxElements(){
        //copy arraylist
        boundingBoxElementsFinal.clear();
        for(BoundingBoxElement b : boundingBoxElements){
            boundingBoxElementsFinal.add(b);
        }
        for(BoundingBoxElement b : boundingBoxElements){
            BoundingBoxElement bFinal = boundingBoxElementsFinal.get(boundingBoxElements.indexOf(b));
            //see if element is still relevant
            if(bFinal.getIsRelevant()){
                double area = bFinal.getArea();
                int x = bFinal.getX();
                int y = bFinal.getY();
                //double maxArea = area;
                int maxId = boundingBoxElements.indexOf(b);
                for(BoundingBoxElement bIterate : boundingBoxElementsFinal){
                    if(bIterate.getIsRelevant()){
                        if((int)maxId != (int)boundingBoxElementsFinal.indexOf(bIterate)){
                            int x2 = boundingBoxElements.get(maxId).getX();
                            int y2 = boundingBoxElements.get(maxId).getY();
                            double maxArea = boundingBoxElements.get(maxId).getArea();

                            //debug
                            /*
                            System.out.println("x1: "+bIterate.getX()+" y1: "+bIterate.getY()+" x2: "+x2+" y2: "+y2);
                            System.out.println("distance: "+distance(bIterate.getX(),bIterate.getY(),x2,y2));
                            */

                            // boundingbox is smaller and too close to reference boundingbox
                            // boundingbox becomes irrelevant
                            if(bIterate.getArea() <= maxArea && distance(bIterate.getX(),bIterate.getY(),x2,y2)<40){
                                bIterate.setIsRelevant(false);
                            }
                            // boundingbox is bigger and close to reference boundingbox
                            // reference boundingbox becomes irrelevant
                            else if(bIterate.getArea() > maxArea && distance(bIterate.getX(),bIterate.getY(),x2,y2)<40){
                                maxId = boundingBoxElementsFinal.indexOf(bIterate);
                                bFinal.setIsRelevant(false);
                            }
                        }
                    }
                }

            }
        }
        // we are done with original arraylist
        boundingBoxElements.clear();

        //debug
        /*
        for(BoundingBoxElement el : boundingBoxElementsFinal) {
            System.out.println("x: "+el.getX()+" y: "+el.getY()+" Area: "+el.getArea()+" Relevent: "+el.getIsRelevant());
        }
        */
    }



    private void find(Mat src) {
        Mat blurredMat = src.clone();

        //blur image
        Imgproc.medianBlur(src, blurredMat, 9);

        Mat gray0 = new Mat(blurredMat.size(), CvType.CV_8U), gray = new Mat();
        //get grayscale
        Imgproc.cvtColor(blurredMat, gray0, Imgproc.COLOR_RGB2GRAY);

        List<MatOfPoint> contours = new ArrayList<>();
        MatOfPoint2f aprxCurve;
        MatOfPoint2f aprxCurve2;
        double maxArea = 0;
        int maxId = -1;

        int edgeDetectionType = 2;
        switch (edgeDetectionType) {
            case 1:
                //binary threshold + canny edge detection + dilate
                Imgproc.adaptiveThreshold(gray0, gray, 255, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, Imgproc.THRESH_BINARY, 5, 2);
                Imgproc.Canny(gray, gray, 10, 20, 3, true);
                Imgproc.dilate(gray, gray, Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(3, 3)));
                debugQuota = 0.0003;
                break;
            case 2:
                Imgproc.adaptiveThreshold(gray0, gray, 255, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, Imgproc.THRESH_BINARY, 5, 2);
                Imgproc.Canny(gray, gray, 10, 20, 3, true);
                Imgproc.dilate(gray, gray, Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(3, 3)));
                Imgproc.dilate(gray, gray, Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(3, 3)));
                Imgproc.erode(gray, gray, Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(3, 3)));
                Imgproc.erode(gray, gray, Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(3, 3)));
                debugQuota = 0.0009;
                break;
            case 3:
                Imgproc.adaptiveThreshold(gray0, gray, 255, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, Imgproc.THRESH_BINARY_INV, 15, 2);
                debugQuota = 0.0009;
                break;
        }

        //debugMat is later displayed onscreen
        debugMat = gray.clone();

        //find contours
        Imgproc.findContours(gray, contours, new Mat(), Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE);

        for (MatOfPoint contour : contours) {
            MatOfPoint2f temp = new MatOfPoint2f(contour.toArray());

            double area = Imgproc.contourArea(contour);
            aprxCurve = new MatOfPoint2f();
            Imgproc.approxPolyDP(temp, aprxCurve, Imgproc.arcLength(temp, true) * 0.02, true);

            //check if contour has 4 points with roughly 90degree corners (check for Rectangles)
            if (aprxCurve.total() == 4 && area >= maxArea) {
                double maxCosine = 0;

                List<Point> curves = aprxCurve.toList();
                MatOfPoint boundingBoxPoints = new MatOfPoint(aprxCurve.toArray());
                for (int i = 2; i < 5; i++) {

                    double cosine = Math.abs(angle(curves.get(i % 4), curves.get(i - 2), curves.get(i - 1)));
                    maxCosine = Math.max(maxCosine, cosine);
                }

                if (maxCosine < 0.3) {
                    maxArea = area;
                    maxId = contours.indexOf(contour);
                    if(maxId>=0) {
                        //boundingrectangle is needed to check if points are inside sheet Area
                        sheet = Imgproc.boundingRect(boundingBoxPoints);
                    }
                }
            }
        }

        //biggest Rectangle is found
        if (maxId >= 0) {
            //debug
            //Imgproc.drawContours(src, contours, maxId, new Scalar(255, 0, 0, .8), 8);
            sheetArea = maxArea;
            //debug
            //System.out.println(sheetArea);
        }

        // check for contours that have more edges than a rectangle (i.e. circles)
        for (MatOfPoint contour : contours) {
            MatOfPoint2f temp4 = new MatOfPoint2f(contour.toArray());

            double area2 = Imgproc.contourArea(contour);
            aprxCurve2 = new MatOfPoint2f();
            Imgproc.approxPolyDP(temp4, aprxCurve2, Imgproc.arcLength(temp4, true) * 0.02, true);

            if (aprxCurve2.total()>4 && area2>30){
                //put necessary contour boundingbox details in Arraylist
                MatOfPoint boundingBoxPoints2 = new MatOfPoint(aprxCurve2.toArray());
                Rect rectTemp = Imgproc.boundingRect(boundingBoxPoints2);
                int x = rectTemp.x;
                int y = rectTemp.y;
                Point p = new Point(x,y);
                int w = rectTemp.width;
                double r = w/2;
                double rectArea = circleArea(r);
                if(sheet.contains(p)) {
                    BoundingBoxElement box = new BoundingBoxElement(rectArea, x, y);
                    boundingBoxElements.add(box);
                }
            }
        }
        //tidy up boundingboxelements
        finalizeBoxElements();
        //analyze all the relevant boundingboxes
        for(BoundingBoxElement b : boundingBoxElementsFinal){
            if (b.getIsRelevant()){
                coinChooser(b.getArea());
            }
        }
    }

/*
    // did not work as intended
    private void calculateHoughCircles(Mat src){
        Mat destination = new Mat(src.rows(), src.cols(), src.type());

        Imgproc.cvtColor(src, destination, Imgproc.COLOR_RGB2GRAY);
        //Imgproc.cvtColor(src, destination, Imgproc.COLOR_RGB2HSV);

        Imgproc.GaussianBlur(destination, destination, new Size(9, 9), 2, 2);

        Imgproc.threshold(destination, destination, 0, 255, Imgproc.THRESH_BINARY + Imgproc.THRESH_OTSU);
        //Imgproc.threshold(destination, destination, 0, 255, Imgproc.THRESH_BINARY + Imgproc.THRESH_OTSU);
        //Imgproc.Canny(destination, destination, 10, 20, 3, true);
        houghGrayMat = destination.clone();

        MatOfPoint3f circles = new MatOfPoint3f();
        //Imgproc.HoughCircles(destination, circles, Imgproc.CV_HOUGH_GRADIENT, 1, 60, 200, 100, 8, 200);
        Imgproc.HoughCircles(destination, circles, Imgproc.CV_HOUGH_GRADIENT, 1, 60);


        int radius;
        Point pt;
        System.out.println("Circles.cols: " + circles.cols());
        for (int x = 0; x < circles.cols(); x++) {
            double vCircle[] = circles.get(0,x);

            if (vCircle == null)
                break;

            pt = new Point(Math.round(vCircle[0]), Math.round(vCircle[1]));
            radius = (int)Math.round(vCircle[2]);
            coinChooser(circleArea(radius));
            System.out.println("Radius"+radius);
            // draw the found circle
            //Imgproc.circle(src, pt, radius, new Scalar(0, 255, 255), 3);
            //Imgproc.circle(src, pt, 3, new Scalar(255, 255, 255), 3);
            //Imgproc.rectangle(src, pt, new Point(pt.x+5, pt.y+5), new Scalar(255, 255, 255));
        }
    }
*/
    // calculate area of a circle
    private double circleArea(double r){
        double area = Math.PI*r*r;
        return area;
    }

    // reset coin count
    private void coinReset(){
        coin1 = 0;
        coin2 = 0;
        coin3 = 0;
        coin4 = 0;
        coin5 = 0;
        coin6 = 0;
        coin7 = 0;
        coin8 = 0;
    }

    // put recognized coins in different cointypes depending on their area compared to the DinA4-Area
    private void coinChooser(double coinArea){
        double quota = coinArea/sheetArea;
        //Debug Quota if necessary
        quota = quota-debugQuota;
        System.out.println(quota);
        // 1 cent
        if(quota>=0.0025 && quota<0.0038){
            coin1++;
        }
        // 2cent
        else if(quota>=0.0038 && quota<0.0047){
            coin2++;
        }
        // 10 cent
        else if(quota>=0.0047 && quota<0.00525){
            coin4++;
        }
        // 5 cent
        else if(quota>=0.00525 && quota<0.0059){
            coin3++;
        }
        // 20 cent
        else if(quota>=0.0059 && quota<0.0065){
            coin5++;
        }
        // 1 euro
        else if(quota>=0.0065 && quota<0.0071){
            coin7++;
        }
        // 50 cent
        else if(quota>=0.0071 && quota<0.0078){
            coin6++;
        }
        // 2 euro
        else if(quota>=0.0078 && quota<0.0099){
            coin8++;
        }
        else{
            //coin is too small/big
        }
    }


}
