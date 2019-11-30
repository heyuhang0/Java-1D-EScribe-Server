package com.hyh0.sutd.java1d.paging;

import com.hyh0.sutd.java1d.videoreader.VideoReader;
import org.opencv.core.*;
import org.opencv.features2d.*;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class VideoClipper {
    private VideoReader videoReader;
    private int prevIndex = 0;
    private int currIndex = 0;
    private ScreenFinder screenFinder = new ScreenFinder();
    private Feature2D featureDetector = ORB.create();
    private DescriptorMatcher matcher = BFMatcher.create();
    private boolean finished = false;

    public VideoClipper(VideoReader videoReader) {
        this.videoReader = videoReader;
    }

    private boolean readNextNFrames(int n, Mat frame) {
        for (int i = 0; i < n; i++) {
            if (!videoReader.read(frame)) {
                return false;
            }
            currIndex += 1;
        }
        return true;
    }

    public VideoClip getNext() {
        Mat prevFrame = null;
        Mat currFrame;
        int currMatchCount;
        int prevMatchCount = 1;
        while (videoReader.isOpened()) {
            currFrame = new Mat();
            if (!readNextNFrames(120, currFrame)) {
                break;
            }
            if (prevFrame != null) {
                // Detect projector screen, use that as ROI
                Rect screenBox = screenFinder.getScreenBox(currFrame);

                Mat currFrameROI = new Mat(currFrame, screenBox);
                Mat prevFrameROI = new Mat(prevFrame, screenBox);

                // Detect the key points using ORB Detector, compute the descriptors
                MatOfKeyPoint currKeyPoints = new MatOfKeyPoint();
                Mat currDescriptors = new Mat();
                featureDetector.detectAndCompute(currFrameROI, new Mat(), currKeyPoints, currDescriptors);

                MatOfKeyPoint prevKeyPoints = new MatOfKeyPoint();
                Mat prevDescriptors = new Mat();
                featureDetector.detectAndCompute(prevFrameROI, new Mat(), prevKeyPoints, prevDescriptors);

                // Matching descriptor vectors
                List<MatOfDMatch> knnMatches = new LinkedList<>();
                matcher.knnMatch(prevDescriptors, currDescriptors, knnMatches, 2);

                // Filter matches using the Lowe's ratio test
                float ratioThresh = 0.75f;
                List<DMatch> listOfGoodMatches = new ArrayList<>();
                for (MatOfDMatch knnMatch : knnMatches) {
                    if (knnMatch.rows() > 1) {
                        DMatch[] matches = knnMatch.toArray();
                        if (matches[0].distance < ratioThresh * matches[1].distance) {
                            listOfGoodMatches.add(matches[0]);
                        }
                    }
                }

                currMatchCount = listOfGoodMatches.size();
                if (currMatchCount < 0.5 * prevMatchCount && currMatchCount < 50) {
                    VideoClip clip = new VideoClip(prevIndex, currIndex, videoReader);
                    prevIndex = currIndex;
                    return clip;
                }
                prevMatchCount = currMatchCount;
            }
            prevFrame = currFrame;
        }
        finished = true;
        return new VideoClip(prevIndex, currIndex, videoReader);
    }

    public boolean isFinished() {
        return finished;
    }
}
