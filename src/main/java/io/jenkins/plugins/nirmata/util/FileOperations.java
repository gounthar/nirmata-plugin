
package io.jenkins.plugins.nirmata.util;

import java.io.*;
import java.util.*;

import org.apache.commons.io.IOUtils;
import org.slf4j.*;

import com.google.common.base.Strings;

import hudson.FilePath;

public class FileOperations {

    private static final Logger logger = LoggerFactory.getLogger(FileOperations.class);

    private FileOperations() {

    }

    public static List<String> getDirectories(String basePath, String commaSeparatedDirectories) {
        List<String> listOfDirectories = new ArrayList<>();
        List<String> directories = getList(commaSeparatedDirectories);

        for (String directory : directories) {
            String directoryWithPathIncluded = appendBasePath(basePath, directory);
            listOfDirectories.add(directoryWithPathIncluded);
        }

        return listOfDirectories;
    }

    public static List<String> getList(String listOfNames) {
        List<String> names = new ArrayList<>();

        if (!Strings.isNullOrEmpty(listOfNames)) {
            for (String name : listOfNames.split(",")) {
                names.add(name.trim());
            }
        }

        return names;
    }

    public static String appendFiles(List<String> files) {
        StringBuffer stringBuffer = new StringBuffer();

        try {
            for (String file : files) {
                String fileContent = readFile(file);
                stringBuffer.append(fileContent);
            }
        } catch (Throwable e) {
            logger.error("Failed to read files, ", e);
            throw new RuntimeException(e);
        }

        return stringBuffer.toString();
    }

    public static String readFile(String fileName) {
        String fileContent = null;

        try {
            FilePath file = new FilePath(new File(fileName));
            fileContent = IOUtils.toString(new FileInputStream(file.getRemote()));
            logger.debug("Read file {} content: {}", fileName, fileContent);
        } catch (Throwable e) {
            logger.error("Failed to read file {}: ", fileName, e);
            throw new RuntimeException(e);
        }

        return fileContent;
    }

    private static String appendBasePath(String basePath, String relativePath) {
        String appendedPath = null;

        if (!relativePath.startsWith(basePath)) {
            appendedPath = basePath.trim() + "/" + relativePath.trim();
        } else {
            appendedPath = relativePath.trim();
        }

        return appendedPath;
    }
}
