package com.hyh0.sutd.java1d.annotation;

public interface AnnotatorCallback {
    void call(String annotation);
    default void onException(Exception e) {
        e.printStackTrace();
    }
}
