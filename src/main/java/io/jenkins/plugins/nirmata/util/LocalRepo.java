package io.jenkins.plugins.nirmata.util;

import com.google.common.base.Strings;
import hudson.FilePath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class LocalRepo {

    private static final Logger logger = LoggerFactory.getLogger(LocalRepo.class);

    private LocalRepo() {

    }

    public static List<String> getFilesInDirectory(List<String> directories, String includes, String excludes) {
        List<String> listOfFiles = new ArrayList<String>();
        String finalIncludes = String.format("%s", Strings.isNullOrEmpty(includes) ? "*.yaml,*.yml,*.json" : includes);

        logger.debug("Includes = {}, excludes = {}", finalIncludes, excludes);
        for (String directory : directories) {
            try {
                logger.debug("Directory = {}", directory);
                FilePath filePath = new FilePath(new File(directory));
                FilePath[] files = filePath.list(finalIncludes, excludes);

                for (FilePath file : files) {
                    listOfFiles.add(directory + "/" + file.getName());
                    logger.debug("File = {}", file.getName());
                }
            } catch (Throwable e) {
                logger.error("Error listing files, {}", e);
                throw new RuntimeException(e);
            }
        }

        return listOfFiles;
    }

}
