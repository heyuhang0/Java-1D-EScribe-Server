package com.hyh0.sutd.java1d.paging;

import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;

import java.util.*;

public class ScreenFinder {
    private Mat gray = new Mat();
    private Mat binarized = new Mat();
    private LinkedList<Rect> screenBoxHistory = new LinkedList<>();
    private int maxHistoryLen;

    public ScreenFinder() {
        this(10);
    }

    public ScreenFinder(int maxHistoryLen) {
        this.maxHistoryLen = maxHistoryLen;
    }

    public Rect getScreenBox(Mat frame) {
        screenBoxHistory.add(getInstantScreenBox(frame));
        if (screenBoxHistory.size() > maxHistoryLen) {
            screenBoxHistory.removeFirst();
        }
        Rect[] sortedHistory = screenBoxHistory.toArray(new Rect[0]);
        Arrays.sort(sortedHistory, (o1, o2) -> (int) (o1.area() - o2.area()));
        return sortedHistory[sortedHistory.length / 2];
    }

    public Rect getInstantScreenBox(Mat frame) {
        Imgproc.cvtColor(frame, gray, Imgproc.COLOR_BGR2GRAY);
        Imgproc.adaptiveThreshold(gray, binarized, 255, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, Imgproc.THRESH_BINARY, 11, 2);
        List<MatOfPoint> contours = new LinkedList<>();
        Imgproc.findContours(binarized, contours, new Mat(), Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE, new Point(0, 0));
        Optional<MatOfPoint> screenContour = contours.stream()
                .filter(contour -> {
                    MatOfPoint2f arc = new MatOfPoint2f();
                    contour.convertTo(arc, CvType.CV_32F);
                    double area = Imgproc.contourArea(contour);
                    double perimeter = Imgproc.arcLength(arc, true);
                    return area / (perimeter * perimeter - 0.5) > 0.03;
                })
                .max(Comparator.comparingDouble(Imgproc::contourArea));
        return screenContour.map(Imgproc::boundingRect).orElseGet(() -> new Rect(0, 0, frame.width(), frame.height()));
    }
}
