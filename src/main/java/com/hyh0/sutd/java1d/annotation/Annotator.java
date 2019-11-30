package com.hyh0.sutd.java1d.annotation;

public interface Annotator {
    String annotate(String videoPath) throws Exception;
    void asyncAnnotate(String videoPath, AnnotatorCallback callback);
}
