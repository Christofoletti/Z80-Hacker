package com.astesbas.z80.hacker.util;

import java.util.List;

/**
 * A simple Multimap implementation.
 * The "get() type" methods implemented in this class does not return optional values to be consistent with
 * the get() method from HashMap implementation. These methods returns null values instead of Optional.empty()
 * for keys not present in the map.
 * 
 * @author Luciano M. Christofoletti
 *         luciano@christofoletti.com.br
 * @version 1.0
 * @since 25/06/2017
 */
public class MultiMap<K, V> extends java.util.HashMap<K, List<V>> {
    
    /** Serial UID */
    private static final long serialVersionUID = -6057366665818249058L;
    
    /**
     * Maps a value to a given key (not thread safe).
     * 
     * @param key the key to map the value
     * @param value the value
     */
    public V map(K key, V value) {
        List<V> list = super.computeIfAbsent(key, v -> new java.util.ArrayList<>());
        return list.add(value) ? value:null;
    }   
    
    /**
     * Returns the single value mapped to the given key. This method is intended to be used for keys
     * that are expected to have only one mapped value.
     * Note: if there is more than one values mapped to the given key, an IllegalAccessException is thrown.
     * 
     * @param key the key
     * @return the unique value mapped to the given key or null if the key is not present in the map
     * @throws IllegalAccessException if there is more than one value mapped to the given key
     */
    public V getSingle(K key) throws IllegalAccessException {
        
        List<V> values = super.get(key);
        if(values == null) {
            return null;
        } else if(values.size() == 1) {
            return values.get(0);
        }   
        
        throw new IllegalAccessException(String.format("There is more than one value mapped to key %s", key));
    }   
    
    /**
     * Return the first value associated to the given key (if available)
     * @param key the key string
     * @return the first value associated to the key or null if the key is unavailable
     */
    public V getFirst(K key) {
        try {
            return super.get(key).get(0);
        } catch(NullPointerException exception) {
            return null;
        }   
    }   
    
    /**
     * Return the last value associated to the given key (if available)
     * @param key the key string
     * @return the last value associated to the key or null if the key is unavailable
     */
    public V getLast(K key) {
        List<V> list = super.get(key);
        try {
            return list.get(list.size()-1);
        } catch(NullPointerException exception) {
            return null;
        }   
    }   
}
