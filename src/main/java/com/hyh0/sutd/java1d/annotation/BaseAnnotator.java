package com.hyh0.sutd.java1d.annotation;

public abstract class BaseAnnotator implements Annotator {
    @Override
    public void asyncAnnotate(String videoPath, AnnotatorCallback callback) {
        new Thread(() -> {
            try {
                String result = annotate(videoPath);
                callback.call(result);
            } catch (Exception e) {
                callback.onException(e);
            }
        }).start();
    }
}
