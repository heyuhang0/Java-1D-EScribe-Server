package com.hyh0.sutd.java1d;

import com.google.firebase.database.*;
import com.hyh0.sutd.java1d.util.firebase.ConfiguredFirebaseApp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Server {
    private static final Logger log = LoggerFactory.getLogger(Server.class);

    static { nu.pattern.OpenCV.loadShared(); }

    public static void main(String[] args) {
        FirebaseDatabase database = ConfiguredFirebaseApp.getDatabase();
        DatabaseReference commandsQ = database.getReference("_command");

        commandsQ.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                String command = (String)dataSnapshot.getValue();

                if ("_placeholder".equals(command)) {
                    log.info("Server connected");
                    return;
                }

                log.info("New command: " + command);

                Pattern commandPattern = Pattern.compile("process\\(\"(.*)\",\\s*(\\d+)\\)");

                Matcher parsedCommand = commandPattern.matcher(command);
                if (parsedCommand.find()) {
                    String firebasePath = parsedCommand.group(1);
                    int timeout = Integer.parseInt(parsedCommand.group(2));
                    log.info("Starting worker process (path: " + firebasePath + " timeout: " + timeout + ")");
                    new Thread(() -> {
                        try {
                            VideoProcessWorker.run(firebasePath, timeout);
                            dataSnapshot.getRef().removeValueAsync();
                        } catch (IOException | InterruptedException e) {
                            e.printStackTrace();
                        }
                    }).start();
                } else {
                    log.info("Bad command");
                    dataSnapshot.getRef().removeValueAsync();
                }
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {

            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        try {
            while (true) {
                Thread.sleep(1000);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
