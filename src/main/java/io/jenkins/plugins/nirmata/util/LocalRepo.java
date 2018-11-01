
package io.jenkins.plugins.nirmata.util;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.jenkinsci.remoting.RoleChecker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;

import hudson.FilePath;
import hudson.remoting.VirtualChannel;

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
                FilePath[] files = filePath.act(new Files(finalIncludes, excludes));

                for (FilePath file : files) {
                    listOfFiles.add(file.getRemote());
                    logger.debug("File = {}", file.getRemote());
                }
            } catch (Throwable e) {
                logger.error("Error listing files, {}", e);
                throw new RuntimeException(e);
            }
        }

        return listOfFiles;
    }

    private static final class Files implements FilePath.FileCallable<FilePath[]> {

        private static final long serialVersionUID = 7668288892944268487L;
        private String includes;
        private String excludes;

        Files(String includes, String excludes) {
            this.includes = includes;
            this.excludes = excludes;
            logger.debug("Includes = {}, excludes = {}", includes, excludes);
        }

        @Override
        public FilePath[] invoke(File f, VirtualChannel channel) throws IOException, InterruptedException {
            FilePath filePath = new FilePath(f);
            return filePath.list(includes, excludes);
        }

        @Override
        public void checkRoles(RoleChecker checker) throws SecurityException {

        }
    }
}
