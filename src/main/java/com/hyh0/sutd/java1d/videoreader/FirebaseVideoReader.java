package com.hyh0.sutd.java1d.videoreader;

import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.hyh0.sutd.java1d.util.firebase.ConfiguredFirebaseApp;
import org.apache.commons.io.FilenameUtils;
import org.asynchttpclient.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileOutputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.*;

public class FirebaseVideoReader extends MultiVideoReader {
    private static final Logger log = LoggerFactory.getLogger(FirebaseVideoReader.class);

    private int timeout;
    private BlockingQueue<Future<String>> videos = new LinkedBlockingDeque<>();
    private volatile boolean stopped = false;
    private DatabaseReference originalVideosRef;
    private ChildEventListener firebaseNewVideoListener;

    private class FirebaseNewVideoListener implements ChildEventListener {
        private AsyncHttpClient client = Dsl.asyncHttpClient();

        @Override
        public void onChildAdded(DataSnapshot dataSnapshot, String s) {
            try {
                String childData = (String)dataSnapshot.getValue();
                log.info("New item in Firebase: " + childData);

                URL url = new URL(childData);

                String tempVideoPrefix = FirebaseVideoReader.class.getCanonicalName() + "-firebase-video";
                String tempVideoSuffix = FilenameUtils.getExtension(url.getPath());
                Path receivedVideo = Files.createTempFile(tempVideoPrefix, "." + tempVideoSuffix);
                receivedVideo.toFile().deleteOnExit();

                FileOutputStream stream = new FileOutputStream(receivedVideo.toString());

                log.info("Start downloading: " + url);
                Future<String> futurePath = client.prepareGet(url.toString()).execute(new AsyncCompletionHandler<String>() {
                    @Override
                    public void onThrowable(Throwable t) {
                        t.printStackTrace();
                    }

                    @Override
                    public State onBodyPartReceived(HttpResponseBodyPart bodyPart)
                            throws Exception {
                        stream.getChannel().write(bodyPart.getBodyByteBuffer());
                        return State.CONTINUE;
                    }

                    @Override
                    public String onCompleted(Response response) {
                        log.info("Finish downloading video from " + url);
                        log.info("File has been saved to " + receivedVideo);
                        return receivedVideo.toString();
                    }
                });
                videos.put(futurePath);

            } catch (Exception e) {
                e.printStackTrace();
                stop();
            }
        }
        public void onChildChanged(DataSnapshot dataSnapshot, String s) {}
        public void onChildRemoved(DataSnapshot dataSnapshot) {}
        public void onChildMoved(DataSnapshot dataSnapshot, String s) {}
        public void onCancelled(DatabaseError databaseError) {}
    }

    public FirebaseVideoReader(String firebasePath, int timeout) {
        this.timeout = timeout;
        this.originalVideosRef = ConfiguredFirebaseApp.getDatabase().getReference(firebasePath);
        this.firebaseNewVideoListener = new FirebaseNewVideoListener();

        originalVideosRef.addChildEventListener(firebaseNewVideoListener);
    }

    private void stop() {
        log.info("Closing firebase video reader");
        this.stopped = true;
        this.originalVideosRef.removeEventListener(firebaseNewVideoListener);
    }

    @Override
    protected boolean hasNext() {
        return !stopped;
    }

    @Override
    protected String getNextVideo() {
        if (stopped) {
            return null;
        }
        try {
            log.info("Getting new video");
            Future<String> futureVideoPath = videos.poll(timeout, TimeUnit.SECONDS);
            if (futureVideoPath != null) {
                String videoPath = futureVideoPath.get();
                log.info("Video file path: " + videoPath);
                if (videoPath != null) {
                    return videoPath;
                }
            }
            log.info("Time out");
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        stop();
        return null;
    }
}
