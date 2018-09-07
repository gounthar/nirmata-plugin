
package io.jenkins.plugins.nirmata.util;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;

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
        } catch (Exception e) {
            logger.error("Failed to read files, ", e);
        }

        return stringBuffer.toString();
    }

    public static String readFile(String fileName) throws Exception {
        String fileContent = null;

        try {
            fileContent = IOUtils.toString(new FileInputStream(fileName));
            logger.debug("Read file {} content: {}", fileName, fileContent);
        } catch (IOException e) {
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
