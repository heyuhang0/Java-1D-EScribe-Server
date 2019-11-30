package com.hyh0.sutd.java1d.annotation;

import com.google.api.gax.longrunning.OperationFuture;
import com.google.cloud.speech.v1.*;
import com.hyh0.sutd.java1d.util.googlecloud.CloudStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.StringJoiner;

public class AudioAnnotator extends BaseAnnotator {
    private static final Logger log = LoggerFactory.getLogger(AudioAnnotator.class);

    private String extractAudio(String videoPath) throws IOException, InterruptedException {
        Path tempAudio = Files.createTempFile(getClass().getCanonicalName() + "temp-audio", ".wav");
        String command = "ffmpeg -i " + videoPath + " -y -ar 16000 -acodec pcm_s16le -ac 1 " + tempAudio.toString();
        Process process = Runtime.getRuntime().exec(command);
        process.waitFor();
        if (process.exitValue() != 0) {
            throw new IOException("ffmpeg returns non zero exit value when execute: " + command);
        }
        log.info("Temporarily saved extracted audio to " + tempAudio);
        return tempAudio.toString();
    }

    @Override
    public String annotate(String videoPath) throws Exception {
        String audioPath = extractAudio(videoPath);
        String audioURI = uploadAudio(audioPath);
        Files.deleteIfExists(Paths.get(audioPath));

        return googleSpeechToText(audioURI);
    }

    private static String uploadAudio(String audioFile) throws IOException {
        Path audioFilePath = Paths.get(audioFile);
        String bucketName = "1d-speech-to-text-audio";
        String blobName = audioFilePath.getFileName().toString();

        return CloudStorage.uploadFile(bucketName, blobName, audioFilePath);
    }

    private static String googleSpeechToText(String audioURI) throws Exception {
        log.info("Start speech-to-text annotation for video " + audioURI);
        // Instantiates a client
        try (SpeechClient speechClient = SpeechClient.create()) {

            // Builds metadata
            RecognitionMetadata metadata = RecognitionMetadata.newBuilder()
                    .setInteractionType(RecognitionMetadata.InteractionType.PRESENTATION)
                    .setRecordingDeviceType(RecognitionMetadata.RecordingDeviceType.SMARTPHONE)
                    .build();

            // Builds the sync recognize request
            RecognitionConfig config = RecognitionConfig.newBuilder()
                    .setEncoding(RecognitionConfig.AudioEncoding.LINEAR16)
                    .setSampleRateHertz(16000)
                    .setLanguageCode("en-US")
                    .setModel("video")
                    .setEnableAutomaticPunctuation(true)
                    .setMetadata(metadata)
                    .build();
            RecognitionAudio audio = RecognitionAudio.newBuilder()
                    .setUri(audioURI)
                    .build();

            // Use non-blocking call for getting file transcription
            OperationFuture<LongRunningRecognizeResponse, LongRunningRecognizeMetadata> response =
                    speechClient.longRunningRecognizeAsync(config, audio);

            List<SpeechRecognitionResult> results = response.get().getResultsList();

            StringJoiner transcription = new StringJoiner("\n");

            for (SpeechRecognitionResult result : results) {
                // There can be several alternative transcripts for a given chunk of speech. Just use the
                // first (most likely) one here.
                SpeechRecognitionAlternative alternative = result.getAlternativesList().get(0);
                transcription.add(alternative.getTranscript());
            }

            return transcription.toString();
        }
    }
}
