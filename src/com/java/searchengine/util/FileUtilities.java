package com.java.searchengine.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Scanner;

/**
 * @author Swapnil Gupta
 * @purpose Implement utility methods for files used by search engine
 * 
 */
public class FileUtilities {
	
	/**
	 * Check whether the provided corpus path is a valid directory
	 * @return true if path is of a valid directory else return false
	 */
	public static boolean isValidCorpusDirectory(String corpusPath) {
		try {
			return new File(corpusPath).getCanonicalFile().isDirectory();
		} catch (IOException e) {
			e.printStackTrace();
			System.err.println(corpusPath + " is not a valid corpus path");
		}
		return false;
	}
	
	/**
	 * Check whether the provided file is a file with given extension
	 * @return true if it is a file with given extension else return false
	 */
	public static boolean isValidFile(String filePath, String extension) {
		try {
			return new File(filePath).getCanonicalFile().exists() && filePath.endsWith(extension);
		} catch (IOException e) {
			e.printStackTrace();
			System.err.println(filePath + " is not a valid corpus path");
		}
		return false;
	}
	
	/**
	 * Extract all the words(one word per line) from given file
	 * @param stopListFileName file name from which words are to be extracted
	 * @return HashSet containing all the file words
	 */
	public static HashSet<String> getFileWords(String stopListFileName) {
		File stopListFile = new File(stopListFileName);
		HashSet<String> tokens = new HashSet<String>();
		try {
			Scanner inputFile = new Scanner(stopListFile);
			while (inputFile.hasNextLine()) {
				tokens.add(inputFile.nextLine());
			}
			inputFile.close();					
		} catch (FileNotFoundException e) {
			System.out.println("Stop list file not found at : " + stopListFile.getAbsolutePath());
		}
		return tokens;
	}
	
	
	/**
	 * Initialize the text file for recording links
	 * @param file file to be initialized
	 */
	public static void initializeFile(File file) {
		try {
			if (!file.exists()) {
				file.createNewFile();
			}
			new FileOutputStream(file, false).close();
		} catch (IOException e) { 
			System.err.println("I/O exception occured in opening: " + file.getAbsolutePath());
		}
	}
	
	
	/**
	 * Extract all the filenames in given folder
	 * @param folderPath path to the folder from where files need to extracted
	 * @return File[] containing all the files in given folder
	 */
	public static File[] getFileHandlers(String folderPath) {
		File folder = new File(folderPath);
		File[] listOfFiles = folder.listFiles();
		
		if(null == listOfFiles) {
			System.out.println("No files found at : " + folderPath);
		}
		return listOfFiles;
	}                                                                                                                           
	                                                                                                                                  
	                                                                                                                                  
	/**
	 * Extract document id's and name from text file into a hash map                                                                   
	 * @param docIDFile File containing the document id's
	 * @return hash map containg all the document names
	 */
	public static HashMap<Integer, String> getDocNames(File docIDFile) {                                                                                                                                                                                           
		HashMap<Integer,String> docIds = new HashMap<Integer,String>(); 
		
		try {
			String line;
			BufferedReader reader = new BufferedReader(new FileReader(docIDFile));                                                                           
			while((line = reader.readLine()) != null) {                                                                                      
				String[] tokens = line.split("\t");                                  							
				docIds.put(Integer.parseInt(tokens[0]), tokens[1]);						
			}                                                                                                                                                                                                                                                        
			reader.close();    
		} catch (IOException e) {
			System.err.println("Could not read file" + docIDFile.getAbsolutePath());
		}		                 							
		return docIds;              							
	}                                                                                                                                 
	                                                                                                                                  
	                                                                                                                                  
	/**
	 * Provide id for the specified parameterName from given text file                                                          
	 * @param readFile
	 * @param parameterName
	 * @return id for the specified parameterName from given text file     
	 */
	public static String getID(File readFile, String parameterName) {
		try {
			String line;
			BufferedReader fileReader = new BufferedReader(new FileReader(readFile));
			while((line = fileReader.readLine()) != null) {							
				String[] temp = line.split("\t");
				if(temp[1].equals(parameterName)) {										
					fileReader.close();
					
					//Return the id for matched parameter in text file
					return temp[0];															
				}
			}
			fileReader.close();
		} catch (IOException e) {
			System.out.println("Could not read the file : " + readFile.getAbsolutePath());
		}                                                                                                                             
		System.out.println(parameterName + " is not present in corpus");  		                 							
		return null;                                                                                                                  
	} 
}