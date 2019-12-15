package com.hyh0.sutd.java1d.paging;

import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;

import java.util.*;

/**
 * Class to locate screen/whiteboard/paper in a video
 */
public class ScreenFinder {
    private Mat gray = new Mat();
    private Mat binarized = new Mat();
    private LinkedList<Rect> screenBoxHistory = new LinkedList<>();
    private int maxHistoryLen;

    public ScreenFinder() {
        this(10);
    }

    /**
     * @param maxHistoryLen size of history buffer, buffer is used to smooth the result
     */
    public ScreenFinder(int maxHistoryLen) {
        this.maxHistoryLen = maxHistoryLen;
    }

    /**
     * Get smoothed screen box
     * @param frame one video frame
     * @return smoothed screen box
     */
    public Rect getScreenBox(Mat frame) {
        // limit buffer size
        screenBoxHistory.add(getInstantScreenBox(frame));
        if (screenBoxHistory.size() > maxHistoryLen) {
            screenBoxHistory.removeFirst();
        }
        // get the median result from the buffer
        Rect[] sortedHistory = screenBoxHistory.toArray(new Rect[0]);
        Arrays.sort(sortedHistory, (o1, o2) -> (int) (o1.area() - o2.area()));
        return sortedHistory[sortedHistory.length / 2];
    }

    /**
     * Get instant screen box
     * @param frame one video frame
     * @return instant screen box
     */
    public Rect getInstantScreenBox(Mat frame) {
        // convert frame to gray scale
        Imgproc.cvtColor(frame, gray, Imgproc.COLOR_BGR2GRAY);
        // use adaptive threshold as edge detector
        Imgproc.adaptiveThreshold(gray, binarized, 255, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, Imgproc.THRESH_BINARY, 11, 2);
        // find contours
        List<MatOfPoint> contours = new LinkedList<>();
        Imgproc.findContours(binarized, contours, new Mat(), Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE, new Point(0, 0));
        // find the largest, rectangle like ( area/perimeter^2 > 0.03 ) contour
        Optional<MatOfPoint> screenContour = contours.stream()
                .filter(contour -> {
                    MatOfPoint2f arc = new MatOfPoint2f();
                    contour.convertTo(arc, CvType.CV_32F);
                    double area = Imgproc.contourArea(contour);
                    double perimeter = Imgproc.arcLength(arc, true);
                    return area / (perimeter * perimeter - 0.5) > 0.03;
                })
                .max(Comparator.comparingDouble(Imgproc::contourArea));
        // get the bounding box for that contour
        return screenContour.map(Imgproc::boundingRect).orElseGet(() -> new Rect(0, 0, frame.width(), frame.height()));
    }
}
