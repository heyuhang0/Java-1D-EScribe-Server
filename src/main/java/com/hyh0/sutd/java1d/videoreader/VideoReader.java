package com.hyh0.sutd.java1d.videoreader;

import org.opencv.core.Mat;

import java.io.IOException;

public interface VideoReader {
    boolean isOpened();
    boolean read(Mat frame);
    void clip(int startIndex, int endIndex, String outputPath) throws IOException, InterruptedException;
}
