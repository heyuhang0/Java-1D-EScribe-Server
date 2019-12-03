package com.hyh0.sutd.java1d.annotation;

import com.google.cloud.vision.v1.*;
import com.google.protobuf.ByteString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

public class OCRAnnotator extends BaseAnnotator {
    private static final Logger log = LoggerFactory.getLogger(AudioAnnotator.class);

    public static String extractPicture(String videoPath) throws IOException, InterruptedException {
        Path tempPic = Files.createTempFile(OCRAnnotator.class.getCanonicalName() + "temp-picture", ".jpg");

        String command = "ffmpeg -y -sseof -5 -t 1 -i " + videoPath + " -update 1 -q:v 1 " + tempPic.toString();
        Process process = Runtime.getRuntime().exec(command);
        process.waitFor();
        if (process.exitValue() != 0) {
            throw new IOException("ffmpeg returns non zero exit value when execute: " + command);
        }

        log.info("Temporarily saved extracted picture to " + tempPic);
        return tempPic.toString();
    }

    @Override
    public String annotate(String videoPath) throws Exception {
        String imagePath = extractPicture(videoPath);
        String result = detectText(imagePath);
        if (!new File(imagePath).delete())
            new File(imagePath).deleteOnExit();
        return result;
    }

    private static String detectText(String filePath) throws Exception {
        try (FileInputStream imageStream = new FileInputStream(filePath)) {
            List<AnnotateImageRequest> requests = new ArrayList<>();

            ByteString imgBytes = ByteString.readFrom(imageStream);

            Image img = Image.newBuilder().setContent(imgBytes).build();
            Feature feat = Feature.newBuilder().setType(Feature.Type.DOCUMENT_TEXT_DETECTION).build();
            AnnotateImageRequest request =
                    AnnotateImageRequest.newBuilder().addFeatures(feat).setImage(img).build();
            requests.add(request);

            try (ImageAnnotatorClient client = ImageAnnotatorClient.create()) {
                BatchAnnotateImagesResponse response = client.batchAnnotateImages(requests);
                List<AnnotateImageResponse> responses = response.getResponsesList();

                StringJoiner result = new StringJoiner("\n");

                for (AnnotateImageResponse res : responses) {
                    if (res.hasError()) {
                        throw new Exception("Error: " + res.getError().getMessage());
                    }

                    TextAnnotation annotation = res.getFullTextAnnotation();
                    result.add(annotation.getText());
                }

                return result.toString();
            }
        }
    }
}
