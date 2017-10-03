package com.astesbas.z80.hacker.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;

/**
 * General file and date utilities.
 * 
 * @author Luciano M. Christofoletti
 *         luciano@christofoletti.com.br
 *         
 * @version 1.0
 * @since 14/sep/2017
 */
public class FileDateUtil {
    
    private static final SimpleDateFormat DATE_FORMATTER = new SimpleDateFormat("dd.MM.yyyy h:mm:ss a");
    
    /** Avoid instantiation of this class */
    private FileDateUtil() {};
    
    /**
     * Return the formatted current date/time.
     * @return
     */
    public static String getCurrentTime() {
        return DATE_FORMATTER.format(System.currentTimeMillis());
    }   
    
    /**
     * Get base file name. If the file name has 
     * @param fileName the complete file name (with path and extension)
     * @return the base file name (with path)
     */
    public static String getBaseFileName(String fileName) {
        int index = fileName.lastIndexOf('.');
        return (index > 0) ? fileName.substring(0, index):fileName;
    }   
    
    /**
     * Get the Path for the given file name.
     * @param fileName the file
     * @return the path for the file name
     */
    public static Path getFilePath(String fileName) {
        Path path = null;
        try {
            path = Paths.get(fileName);
            Files.deleteIfExists(path);
        } catch (IOException | InvalidPathException exception) {
            System.err.printf("%nError getting path to file: \"%s\"\n\t%s%n", fileName, exception.getMessage());
            System.exit(-1);
        }   
        return path;
    }   
    
}
