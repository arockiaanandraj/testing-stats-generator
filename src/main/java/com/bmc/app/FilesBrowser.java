package com.bmc.app;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class FilesBrowser {
    private FilesBrowser() {
    }

    public static List<File> getFiles(String directoryPath) {

        try (Stream<Path> filesWalk = Files.walk(Paths.get(directoryPath))) {

            return filesWalk.filter(path -> path.toString().endsWith(".xlsm")).map(Path::toFile)
                    .collect(Collectors.toList());

        } catch (IOException e) {
            e.printStackTrace();
        }

        return Collections.emptyList();
    }

}