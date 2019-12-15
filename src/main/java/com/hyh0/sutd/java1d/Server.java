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
        // get Firebase command queue reference
        // commands queue is a list of string with key "_command" under Firebase root
        FirebaseDatabase database = ConfiguredFirebaseApp.getDatabase();
        DatabaseReference commandsQ = database.getReference("_command");

        // setup listener for new command
        commandsQ.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                String command = (String)dataSnapshot.getValue();

                // a item with key "_placeholder" and value "_placeholder" is used as placeholder in Firebase
                // since Firebase will delete any empty list, and we do not want that happen
                // when we read this placeholder, we known that server is connected
                if ("_placeholder".equals(command)) {
                    log.info("Server connected");
                    return;  // does not parse it as command
                }

                log.info("New command: " + command);

                // regex to match command: process(String firebasePath, int timeout)
                Pattern commandPattern = Pattern.compile("process\\(\"(.*)\",\\s*(\\d+)\\)");

                Matcher parsedCommand = commandPattern.matcher(command);
                if (parsedCommand.find()) {
                    // parse command
                    String firebasePath = parsedCommand.group(1);
                    int timeout = Integer.parseInt(parsedCommand.group(2));
                    // start a new worker process to execute the command
                    log.info("Starting worker process (path: " + firebasePath + " timeout: " + timeout + ")");
                    new Thread(() -> {
                        try {
                            VideoProcessWorker.run(firebasePath, timeout);
                            dataSnapshot.getRef().removeValueAsync(); // remove command from queue once done
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

        // prevent main thread from exit, so Firebase listener can run
        try {
            while (true) {
                Thread.sleep(1000);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
