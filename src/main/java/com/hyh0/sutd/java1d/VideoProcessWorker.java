package com.hyh0.sutd.java1d;

import com.hyh0.sutd.java1d.annotation.Annotator;
import com.hyh0.sutd.java1d.annotation.AudioAnnotator;
import com.hyh0.sutd.java1d.annotation.OCRAnnotator;
import com.hyh0.sutd.java1d.paging.VideoClip;
import com.hyh0.sutd.java1d.paging.VideoClipper;
import com.hyh0.sutd.java1d.util.firebase.ConfiguredFirebaseApp;
import com.hyh0.sutd.java1d.util.googlecloud.CloudStorage;
import com.hyh0.sutd.java1d.videoreader.FirebaseVideoReader;
import com.hyh0.sutd.java1d.videoreader.VideoReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class VideoProcessWorker {
    private static final Logger log = LoggerFactory.getLogger(VideoProcessWorker.class);

    public static void run(String firebaseCoursePath, int timeout) throws IOException, InterruptedException {
        String originalVideosPath = firebaseCoursePath + "/originalVideos";

        VideoReader reader = new FirebaseVideoReader(originalVideosPath, timeout);
        VideoClipper clipper = new VideoClipper(reader);

        Annotator audioAnnotator = new AudioAnnotator();
        Annotator ocrAnnotator = new OCRAnnotator();

        for (int i = 0; !clipper.isFinished(); i ++) {
            // search for next video clip
            VideoClip clip = clipper.getNext();
            log.info("New clip generated: " + clip);

            // encode and save video clip
            Path exportPath = Files.createTempFile(VideoProcessWorker.class.getCanonicalName() + "exported-video", ".mp4");
            clip.export(exportPath.toString());
            exportPath.toFile().deleteOnExit();
            log.info("Clip has been saved to " + exportPath);

            final int videoIndex = i;
            // do speech-to-text recognition for encoded video
            // and upload result to Firebase
            audioAnnotator.asyncAnnotate(exportPath.toString(), s -> {
                log.info("updating speech recognition result");
                ConfiguredFirebaseApp.getDatabase()
                        .getReference(firebaseCoursePath + "/processedVideos/" + videoIndex + "/speechRecognition")
                        .setValueAsync(s);
            });
            // do OCR to recognize texts on screen and whiteboard
            // upload result to Firebase
            ocrAnnotator.asyncAnnotate(exportPath.toString(), s -> {
                log.info("updating slide recognition result");
                ConfiguredFirebaseApp.getDatabase()
                        .getReference(firebaseCoursePath + "/processedVideos/" + videoIndex + "/slidesRecognition")
                        .setValueAsync(s);
            });

            // Upload video file to Google Cloud Storage
            // and save url in Firebase
            String bucketName = "1d-processed-video";
            String blobName = exportPath.getFileName().toString();
            CloudStorage.asyncUploadFile(bucketName, blobName, exportPath, () -> {
                String videoURL = CloudStorage.getPublicURL(bucketName, blobName);
                log.info("clip" + videoIndex + " has been uploaded to " + videoURL);
                ConfiguredFirebaseApp.getDatabase()
                        .getReference(firebaseCoursePath + "/processedVideos/" + videoIndex + "/url")
                        .setValueAsync(videoURL);
                log.info("URL added to firebase");
                return null;
            });

            Path thumbnailPath = Paths.get(OCRAnnotator.extractPicture(exportPath.toString()));
            String thumbnailBlobName = thumbnailPath.getFileName().toString();
            CloudStorage.asyncUploadFile(bucketName, thumbnailBlobName, thumbnailPath, () -> {
                String imageURL = CloudStorage.getPublicURL(bucketName, thumbnailBlobName);
                log.info("thumbnail" + videoIndex + " has been uploaded to " + imageURL);
                ConfiguredFirebaseApp.getDatabase()
                        .getReference(firebaseCoursePath + "/processedVideos/" + videoIndex + "/thumbnail")
                        .setValueAsync(imageURL);
                if (!thumbnailPath.toFile().delete()) {
                    thumbnailPath.toFile().deleteOnExit();
                }
                return null;
            });
        }
    }
}
