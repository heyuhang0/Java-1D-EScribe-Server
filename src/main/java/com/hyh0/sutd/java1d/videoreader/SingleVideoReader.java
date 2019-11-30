package com.hyh0.sutd.java1d.videoreader;

import net.bramp.ffmpeg.FFmpegExecutor;
import net.bramp.ffmpeg.FFmpegUtils;
import net.bramp.ffmpeg.builder.FFmpegBuilder;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.Videoio;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

public class SingleVideoReader extends VideoCapture implements VideoReader {
    private String videoPath;
    private String videoTitle;

    public SingleVideoReader(String videoPath) {
        super(videoPath);
        this.videoPath = videoPath;
        this.videoTitle = Paths.get(videoPath).getFileName().toString();
    }

    @Override
    public String toString() {
        return "[Video: " + videoTitle + "]";
    }

    private double getFrameRate() {
        return super.get(Videoio.CAP_PROP_FPS);
    }

    @Override
    public void clip(int startIndex, int endIndex, String outputPath) throws IOException {
        long startTime = (long)(startIndex * 1000.0 / getFrameRate());
        long duration = (long)((endIndex - startIndex) * 1000.0 / getFrameRate());
        FFmpegBuilder builder = new FFmpegBuilder()
                .setInput(videoPath)
                .overrideOutputFiles(true)
                .setStartOffset(startTime, TimeUnit.MILLISECONDS)
                .addExtraArgs("-t", FFmpegUtils.toTimecode(duration, TimeUnit.MILLISECONDS))
                .addOutput(outputPath)
                .done();
        FFmpegExecutor executor = new FFmpegExecutor();
        executor.createJob(builder).run();
    }

    public String getVideoPath() {
        return this.videoPath;
    }

    public String getVideoTitle() {
        return this.videoTitle;
    }
}
