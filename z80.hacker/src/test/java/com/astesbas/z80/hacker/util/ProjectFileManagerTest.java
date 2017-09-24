package com.astesbas.z80.hacker.util;

import static com.astesbas.z80.hacker.util.ConfigFileProperties.ConfigKey.*;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Optional;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * ProjectFileManager tests.
 * 
 * @author Luciano M. Christofoletti
 *         luciano@christofoletti.com.br
 * @since 25/06/2017
 */
public class ProjectFileManagerTest extends TestCase {
    
    /**
     * Create the test case
     * @param testName name of the test case
     */
    public ProjectFileManagerTest(String testName) {
        super(testName);
    }   
    
    /**
     * @return the suite of tests being tested
     */
    public static Test suite() {
        return new TestSuite(ProjectFileManagerTest.class);
    }   
    
    /**
     * Tests ProjectFileManager method load(). 
     * @throws IOException 
     * @throws IllegalArgumentException 
     * @throws IllegalAccessException 
     */
    public void testConfigFileLoad() 
            throws URISyntaxException, IllegalArgumentException, IOException, IllegalAccessException {
        
        // the project config file manager
        ConfigFileProperties manager = new ConfigFileProperties();
        
        // get the default config file resource from classpath
        java.net.URL filePath = manager.getClass().getClassLoader().getResource("shrubbles.cfg");
        java.io.File configFile = new java.io.File(filePath.toURI());
        
        // load the default configuration file
        manager.load(configFile);
        
        // get the string value for the BINARY_SOURCE key (predefined key)
        Optional<String> binarySource = manager.getString(BINARY_FILE);
        assertTrue("value for key BINARY_SOURCE not found!", binarySource.isPresent());
        
        // get the address value for the BINARY_START address key (predefined key) 
        Optional<String> binaryStart = manager.getString(BINARY_START);
        assertTrue("value for key BINARY_SOURCE not found!", binaryStart.isPresent());
        
        // try to get a value for a non existing key
        Optional<String> nonExistingKey = manager.getString("NON_EXISTING_KEY");
        assertTrue("value for key NON_EXISTING_FIELD not found!", !nonExistingKey.isPresent());
    }   
}
