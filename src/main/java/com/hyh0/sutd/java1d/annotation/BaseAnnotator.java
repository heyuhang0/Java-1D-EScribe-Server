package com.hyh0.sutd.java1d.annotation;

public abstract class BaseAnnotator implements Annotator {
    /**
     * default implementation for async task
     * @param videoPath path of the video to be annotated
     * @param callback callback to be executed when result is ready
     */
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
