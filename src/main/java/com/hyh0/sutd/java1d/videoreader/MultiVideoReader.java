package com.hyh0.sutd.java1d.videoreader;

import org.apache.commons.io.FilenameUtils;
import org.opencv.core.Mat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;

public abstract class MultiVideoReader implements VideoReader {
    private static final Logger log = LoggerFactory.getLogger(MultiVideoReader.class);

    private LinkedList<SingleVideoReader> videos = new LinkedList<>();

    protected abstract boolean hasNext();

    protected abstract String getNextVideo();

    @Override
    public boolean isOpened() {
        if (videos.size() > 0 && videos.getLast().isOpened()) {
            return true;
        }
        return hasNext();
    }

    @Override
    public boolean read(Mat frame) {
        if (videos.size() > 0 && videos.getLast().isOpened()) {
            if (videos.getLast().read(frame)) {
                return true;
            }
        }
        if (hasNext()) {
            String videoPath = getNextVideo();
            if (videoPath != null) {
                SingleVideoReader nextVideo = new SingleVideoReader(videoPath);
                videos.add(nextVideo);
                log.info("Loading next video: " + nextVideo.getVideoPath());
                return nextVideo.read(frame);
            }
        }
        return false;
    }

    /**
     * Export a list of all available videos (Used by FFmpeg during video concatenation)
     * @param dst output
     */
    private void exportVideoList(Path dst) {
        try {
            PrintWriter writer = new PrintWriter(dst.toFile(), "UTF-8");
            for (SingleVideoReader v : videos) {
                writer.println("file '" + v.getVideoPath() + "'");
            }
            writer.close();
        } catch (FileNotFoundException | UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    /**
     * Concat videos in list to a single video
     * @return Path of concat video
     * @throws IOException
     * @throws InterruptedException
     */
    private Path concatVideo() throws IOException, InterruptedException {
        Path listFile = Files.createTempFile(getClass().getCanonicalName() + "-video-list", ".txt");
        exportVideoList(listFile);
        String videoExtension = FilenameUtils.getExtension(Paths.get(videos.getFirst().getVideoPath()).toString());
        Path concatVideo = Files.createTempFile(getClass().getCanonicalName() + "-concat", "." + videoExtension);

        String command = "ffmpeg -y -f concat -safe 0 -i " + listFile + " -c copy " + concatVideo;
        Process process = Runtime.getRuntime().exec(command);
        process.waitFor();
        if (process.exitValue() != 0) {
            throw new IOException("ffmpeg returns non zero exit value when execute: " + command);
        }

        if (!listFile.toFile().delete())
            listFile.toFile().deleteOnExit();

        return concatVideo;
    }

    /**
     * Export a video clip
     * @param startIndex index of clip first frame
     * @param endIndex index of clip last frame
     * @param outputPath output path
     * @throws IOException
     */
    @Override
    public void clip(int startIndex, int endIndex, String outputPath) throws IOException, InterruptedException {
        Path concatVideoPath = concatVideo();
        new SingleVideoReader(concatVideoPath.toString()).clip(startIndex, endIndex, outputPath);
        // delete concat video
        if (!concatVideoPath.toFile().delete())
            concatVideoPath.toFile().deleteOnExit();
    }
}
