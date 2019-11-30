package com.hyh0.sutd.java1d.paging;

import com.hyh0.sutd.java1d.videoreader.VideoReader;

import java.io.IOException;

public class VideoClip {
    private VideoReader source;
    private final int startIndex;
    private final int endIndex;

    public VideoClip(int startIndex, int endIndex, VideoReader source) {
        this.startIndex = startIndex;
        this.endIndex = endIndex;
        this.source = source;
    }

    public int getStartIndex() {
        return startIndex;
    }

    public int getEndIndex() {
        return endIndex;
    }

    public void export(String path) throws IOException, InterruptedException {
        source.clip(startIndex, endIndex, path);
    }

    @Override
    public String toString() {
        return source.toString() + "{" + startIndex + ":" + endIndex + "}";
    }
}
