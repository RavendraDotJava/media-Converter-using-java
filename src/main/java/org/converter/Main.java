package org.converter;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class Main {
    public static void main(String[] args) {
        try {
            // Path to your local ffmpeg binary
            String ffmpegPath = "ffmpeg"; // Mac/Linux
            // String ffmpegPath = "ffmpeg\\ffmpeg.exe"; // Windows

            ProcessBuilder pb = new ProcessBuilder(
                    ffmpegPath,
                    "-i", "my_error.aiff",
                    "-vn",
                    "-acodec", "mp3",
                    "output.mp3"
            );

            pb.redirectErrorStream(true);
            Process process = pb.start();

            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream())
            );

            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }

            int exitCode = process.waitFor();
            System.out.println("Done: " + exitCode);

        } catch (Exception e) {
            e.printStackTrace();
        }


    }
}