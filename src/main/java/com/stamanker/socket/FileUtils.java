package com.stamanker.socket;

import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

public class FileUtils {

    public static void writeToFile(String fileName, byte[] data) {
        try (OutputStream outputStream = Files.newOutputStream(Paths.get(fileName), StandardOpenOption.CREATE)){
            outputStream.write(data);
        } catch (Exception e) {
            System.err.println("Error while writing data to " + fileName);
        }
    }

}
