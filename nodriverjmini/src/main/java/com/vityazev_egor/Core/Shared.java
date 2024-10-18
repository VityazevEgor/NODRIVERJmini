package com.vityazev_egor.Core;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Optional;
import java.util.stream.Collectors;

public class Shared {
    private static CustomLogger logger = new CustomLogger(Shared.class.getName());
    public static void sleep(long milis){
        try {
            Thread.sleep(milis);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static Optional<String> readResource(String fileName){
        InputStream inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(fileName);
        if (inputStream == null){
            logger.error("There is no such file - " + fileName, null);
            return Optional.empty();
        }

        try {
            var reader = new BufferedReader(new InputStreamReader(inputStream));
            return Optional.ofNullable(reader.lines().collect(Collectors.joining("\n")));
        } catch (Exception e) {
            logger.error("Can't read lines of file", e);
            return Optional.empty();
        }
    }
}
