package com.java.searchengine.util;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * @author Swapnil Gupta
 * @purpose Implement utility methods for data structures used by search engine
 * 
 */
public class Utilities {
	                                                                                                                                                                                                                                                                 
	/**
	 * Generic function to print a map                                                                                                 
	 * @param map Map to be printed to standard output
	 */
	public static void printMap(Map<?, ?> map) {                                                                                             
	    for (Map.Entry<?, ?> entry : map.entrySet()) {                            							
	         System.out.println( entry.getKey() + " " + entry.getValue());                							
	    }
	}
	
	
	/**
	 * Sort the documents in descending order of ranking
	 * @param documentScore Map to be sorted
	 * @return sorted map
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static Map<Integer,Double> rankDocuments(Map<Integer,Double> documentScore) {
		List list = new LinkedList(documentScore.entrySet());
 
		Collections.sort(list, new Comparator() {
			public int compare(Object o1, Object o2){
				return ((Comparable) ((Map.Entry) (o2)).getValue()).compareTo(((Map.Entry) (o1)).getValue());
			}
		});

        //Put sorted list into map again. LinkedHashMap make sure order in which keys were inserted
		Map sortedMap = new LinkedHashMap();
		for (Iterator iterator = list.iterator(); iterator.hasNext();) {
			Map.Entry entry = (Map.Entry) iterator.next();
			sortedMap.put(entry.getKey(), entry.getValue());
		}
		return sortedMap;
	}
	
	/**
	 * Load all the properties from the the program input locations properties file
	 * @return A map containing all the configuration properties
	 */
	public static HashMap<String, String> loadProperties() {
		Properties properties = new Properties();
		HashMap<String, String> propertiesMap = new HashMap<String, String>();
		try {
			properties.load(new FileInputStream("resources/fileLocations.properties"));
		} catch (IOException e) {
			System.err.println("I/O exception occured in opening: properties file");
		}
		propertiesMap.put("INPUT_CORPUS_PATH", properties.getProperty("INPUT_CORPUS_PATH"));
		propertiesMap.put("STOP_WORDS_FILE", properties.getProperty("STOP_WORDS_FILE"));
		propertiesMap.put("QUERY_XML", properties.getProperty("QUERY_XML"));
		
		propertiesMap.put("INDEX_FOLDER", properties.getProperty("INDEX_FOLDER"));
		
		propertiesMap.put("DOCUMENT_ID_FILE", properties.getProperty("DOCUMENT_ID_FILE"));
		propertiesMap.put("DOCUMENT_INDEX_FILE", properties.getProperty("DOCUMENT_INDEX_FILE"));
		propertiesMap.put("SORTED_DOCUMENT_INDEX_FILE", properties.getProperty("SORTED_DOCUMENT_INDEX_FILE"));
		
		propertiesMap.put("TERMS_ID_FILE", properties.getProperty("TERMS_ID_FILE"));
		propertiesMap.put("TERM_INDEX_FILE", properties.getProperty("TERM_INDEX_FILE"));
		propertiesMap.put("TERM_INFO_FILE", properties.getProperty("TERM_INFO_FILE"));
		
		return propertiesMap;
	}
}
