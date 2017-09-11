package com.astesbas.z80.hacker.util;

import java.util.List;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Multimap tests.
 * 
 * @author Luciano M. Christofoletti
 *         luciano@christofoletti.com.br
 * @since 25/06/2017
 */
public class MultimapTest extends TestCase {
    
    /**
     * Create the test case
     * @param testName name of the test case
     */
    public MultimapTest(String testName) {
        super(testName);
    }   
    
    /**
     * @return the suite of tests being tested
     */
    public static Test suite() {
        return new TestSuite(MultimapTest.class);
    }   
    
    /**
     * Tests multimap methods get(), size() and remove(). 
     * @throws IllegalAccessException 
     */
    public void testMultimapGet() throws IllegalAccessException {
        
        MultiMap<String, String> map = new MultiMap<>();
        
        // fill the map with three keys and five values
        map.map("key 1", "value 1");
        map.map("key 2", "value 2");
        map.map("key 3", "value 3");
        map.map("key 3", "value 4");
        map.map("key 3", "value 5");
        
        // tests the multimap sizes for given map
        assertEquals("Wrong size of multimap!", 3, map.size());
        assertEquals("Wrong size of list for key 1!", 1, map.get("key 1").size());
        assertEquals("Wrong size of list for key 2!", 1, map.get("key 2").size());
        assertEquals("Wrong size of list for key 3!", 3, map.get("key 3").size());
        
        // tests the get for multimap
        assertEquals("Wrong size of list for key 0!", null, map.get("key 0")); // key not available
        assertEquals("Wrong size of list for key 0!", null, map.getSingle("key 0")); // key not available
        assertEquals("Wrong value for key 1!", "value 1", map.get("key 1").get(0));
        assertEquals("Wrong value for key 2!", "value 2", map.getSingle("key 2")); // may throw IllegalAccessException
        assertEquals("Wrong value for key 3!", "value 3", map.get("key 3").get(0));
        assertEquals("Wrong value for key 3!", "value 4", map.get("key 3").get(1));
        assertEquals("Wrong value for key 3!", "value 5", map.get("key 3").get(2));
        
        // tests the multimap element removal
        List<String> removedList = map.remove("key 3");
        assertEquals("Wrong size of multimap after removal of key 3!", 2, map.size());
        assertEquals("Wrong size of removed list (key 3)!", 3, removedList.size());
        
        // tests the return value of map() method
        String value0a = map.map("key 0", "value 0a");
        String value0b = map.map("key 0", "value 0b");
        assertEquals("Wrong return value of map() for value 0a!", value0a, "value 0a");
        assertEquals("Wrong return value of map() for value 0b!", value0b, "value 0b");
    }   
    
    /**
     * Tests the multimap getFirst() method.
     */
    public void testMultimapGetFirst() {
        
        MultiMap<String, String> map = new MultiMap<>();
        
        // fill the map with three keys and five values
        map.map("key 1", "value 1a");
        map.map("key 2", "value 2a");
        map.map("key 2", "value 2b");
        map.map("key 3", "value 3a");
        map.map("key 3", "value 3b");
        map.map("key 3", "value 3c");
        
        // tests the multimap getFirst() for given map
        assertEquals("Wrong value of first value for key 1!", "value 1a", map.getFirst("key 1"));
        assertEquals("Wrong value of first value for key 2!", "value 2a", map.getFirst("key 2"));
        assertEquals("Wrong value of first value for key 3!", "value 3a", map.getFirst("key 3"));
        
        // tests getFirst() for unavailable key
        assertEquals("Wrong behavior of getFirst() for unavailable key 0!", null, map.getFirst("key 0"));
    }   
    
    /**
     * Tests the multimap getLast() method.
     */
    public void testMultimapGetLast() {
        
        MultiMap<String, String> map = new MultiMap<>();
        
        // fill the map with three keys and five values
        map.map("key 1", "value 1a");
        map.map("key 2", "value 2a");
        map.map("key 2", "value 2b");
        map.map("key 3", "value 3a");
        map.map("key 3", "value 3b");
        map.map("key 3", "value 3c");
        
        // tests the multimap getLast() for given map
        assertEquals("Wrong value of first value for key 1!", "value 1a", map.getLast("key 1"));
        assertEquals("Wrong value of first value for key 2!", "value 2b", map.getLast("key 2"));
        assertEquals("Wrong value of first value for key 3!", "value 3c", map.getLast("key 3"));
        
        // tests getLast() for unavailable key
        assertEquals("Wrong behavior of getLast() for unavailable key 0!", null, map.getLast("key 0"));
        
    }   
}
