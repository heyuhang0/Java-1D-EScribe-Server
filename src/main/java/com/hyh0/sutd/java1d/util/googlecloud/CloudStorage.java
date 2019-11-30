package com.hyh0.sutd.java1d.util.googlecloud;

import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Callable;

public class CloudStorage {

    public static String uploadFile(String bucketName, String blobName, Path file) throws IOException {
        Storage storage = StorageOptions.getDefaultInstance().getService();
        BlobId blobId = BlobId.of(bucketName, blobName);

        BlobInfo blobInfo = BlobInfo.newBuilder(blobId).setContentType(Files.probeContentType(file)).build();
        storage.create(blobInfo, Files.readAllBytes(file));

        return getGsURI(bucketName, blobName);
    }

    public static void asyncUploadFile(String bucketName, String blobName, Path file, Callable callback) {
        new Thread(() -> {
            try {
                uploadFile(bucketName, blobName, file);
                callback.call();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    public static String getGsURI(String bucketName, String blobName) {
        return "gs://" + bucketName + "/"  + blobName;
    }

    public static String getPublicURL(String bucketName, String blobName) {
        return "https://storage.googleapis.com/" + bucketName + "/" + blobName;
    }
}
