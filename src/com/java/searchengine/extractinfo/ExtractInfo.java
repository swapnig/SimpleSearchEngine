package com.java.searchengine.extractinfo;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;

import org.tartarus.snowball.EnglishSnowballStemmerFactory;
import org.tartarus.snowball.util.StemmerException;

import com.java.searchengine.util.FileUtilities;

/**
 * @author Swapnil Gupta
 * @purpose Interface to extract information for any term or document
 *
 */
public class ExtractInfo {
	
	private static String seperator = "\t";
	
	/**
	 * Get metadata for given document
	 * @param filename file name for which metadata needs to be extracted
	 * @param docIdFile file object containing all the document id's
	 * @param docIndexFile file object containing all the document index's
	 */
	public static void getDocumentMetadata (String filename, File docIdFile, File docIndexFile) {
		String docId = FileUtilities.getID(docIdFile, filename);
		if(null != docId) {
			try {
				readMetaDataFromDocumentIndex(filename, docId, docIndexFile);
			} catch(IOException e) {
				System.err.println("Could not read file" + docIndexFile.getAbsolutePath());
			}
		}
	}
	
	/**
	 * Read document metadata using document indexes
	 * @param filename file name for which metadata needs to be extracted
	 * @param docId document id for given filename
	 * @param docIndexFile file object containing all the document index's
	 * @throws IOException
	 */
	private static void readMetaDataFromDocumentIndex (String filename, String docId, File docIndexFile) 
			throws IOException {
		int termsCount = 0;
		int distinctTermsCount = 0;
		
		String line;
		BufferedReader reader = new BufferedReader(new FileReader(docIndexFile));
		while((line = reader.readLine()) != null) {
			String[] temp = line.split(seperator);
			if(temp[0].equals(docId)) {
				distinctTermsCount++;					//One distinct term per line, increment distinct terms count
				termsCount += temp.length - 2;			//Increment total terms count
			}
		}
		System.out.println("\n Listing for document: " + filename);
		System.out.println("DOCID: " + docId);
		System.out.println("Distinct terms: " + distinctTermsCount);
		System.out.println("Total terms: " + termsCount);
		
		reader.close();
	}
	
	
	/**
	 * Get metadata for given term
	 * @param term for which metadata needs to be extracted
	 * @param termsIdFile file object containing all the term id's
	 * @param termInfoFile file object containing all the term index's
	 */
	public static void getTermMetadata (String term, File termsIdFile, File termInfoFile) {
		String stemmedTerm = getStemmedTerm(term);	
		String termId = FileUtilities.getID(termsIdFile, stemmedTerm);
		
		if (null != termId) {
			try{
				readMetaDataFromTermInfo(stemmedTerm, termId, termInfoFile);
			} catch(IOException e) {
				e.printStackTrace();
				System.err.println("Could not read file" + termInfoFile.getAbsolutePath());
			}
		}
	}
	
	/**
	 * Read term metadata using term info
	 * @param stemmedTerm for which metadata needs to be extracted
	 * @param termsId term id containing all the terms
	 * @param termInfoFile file object containing all the term info's
	 * @throws IOException
	 */
	private static void readMetaDataFromTermInfo (String stemmedTerm, String termId, File termInfoFile) 
			throws IOException {
		String line;
		BufferedReader reader = new BufferedReader(new FileReader(termInfoFile));
		while((line = reader.readLine()) != null) {
			String[] temp = line.split(seperator);
			if(temp[0].equals(termId)) {
				System.out.println("\nListing for term: " + stemmedTerm);
				System.out.println("TERMID: " + termId);
				System.out.println("Number of documents containing term: " + temp[3]);
				System.out.println("Term frequency in corpus: " + temp[2]);
				System.out.println("Inverted list offset: " + temp[1]);
				
				reader.close();
				break;
			}	
		}
	}
	
	/**
	 * Get metadata for given term within given document
	 * @param term for which metadata needs to be extracted
	 * @param filename file name for which metadata needs to be extracted
	 * @param docIdFile file object containing all the document id's
	 * @param termsIdFile file object containing all the term id's
	 * @param termIndexFile file object containing all the term index's
	 * @param termInfoFile file object containing all the term info's
	 */
	public static void getTermMetadataWithinDocument(String term, String filename, File docIdFile, 
			File termsIdFile, File termIndexFile, File termInfoFile) {
		String stemmed = getStemmedTerm(term);                                                                                                           
		String termId = FileUtilities.getID(termsIdFile, stemmed);                                 
		String docId = FileUtilities.getID(docIdFile, filename);
		
		if(docId != null && termId != null){
			long offset = getTermOffset(termId, termInfoFile);
			ArrayList<Integer> positions = getAllPoitionsForTermInDocument(offset, docId, termIndexFile);
			
	    	System.out.println("\nInverted list for term: " + stemmed);
	    	System.out.println("In document: " + filename);
			System.out.println("TERMID: " + termId);
			System.out.println("DOCID: " + docId);
			System.out.println("Term frequency in document: " + positions.size());
			
			//Print the positions of a term in a document if not empty else display custom message
			if (!positions.isEmpty()) {
	        	System.out.print("Positions: ");
	        	for (int i = 0; i < positions.size()-1 ; i++ ) {
	        		System.out.print(positions.get(i) + ", ");
	        	}
	        	System.out.print(positions.get(positions.size()-1) + "\n");
			}
			else
				System.out.print("Positions: Not present \n");
		}
	}
	
	/**
	 * Find term_index.txt offset for given term id from terms_info.txt
	 * @param termId
	 * @param termInfoFile
	 * @return offset for given term if available else return 0
	 */
	private static long getTermOffset(String termId, File termInfoFile) {
		String line;
		try {
			BufferedReader reader = new BufferedReader(new FileReader(termInfoFile));
			while((line = reader.readLine()) != null) {
				String[] temp = line.split(seperator);
				if(temp[0].equals(termId)) {
					reader.close();
					return Long.parseLong(temp[1]);
				}
			}
			reader.close();
		}
		catch(IOException e){
			System.err.println("Could not read file" + termInfoFile.getAbsolutePath());
		}
		return 0;
	}
	
	/** Get all positions for given term within given document
	 * @param offset for given term in term index
	 * @param docId document id for given filename
	 * @param termIndexFile file object containing all the term index's
	 */
	private static ArrayList<Integer> getAllPoitionsForTermInDocument (long offset, String docId, File termIndexFile) {
		ArrayList<Integer> positions = new ArrayList<Integer>();
		try {
			//Randomly access terms_index file
			RandomAccessFile indexFile = new RandomAccessFile(termIndexFile, "r");                                           		
			indexFile.seek(offset);                                                                                                                              
			                                                                                                                                                     
	        String line = indexFile.readLine();                                             	
	    	String[] termOcurrence = line.split(seperator);                                                        	
	    	String[] docOcurrence;                                                                                                                               
	    	                                                                                                                                                                                                                                            
	    	int docCount = 0;                                                                                                                                    
	    	int count = 0; 
	    	int counter = 1;
	    	                                                                                                                                                     
//	    	for(int i = 1 ; i < termOcurrence.length ; i++) {
//	    		//Split document:position pair
//	    		docOcurrence = termOcurrence[i].split(":");							                                               	
//	    		docCount += Integer.parseInt(docOcurrence[0]);
//	    		
//	    		//If document id does not match continue, else traverse positions for required document build position list
//	    		if (docCount != Integer.parseInt(docId)) {								                                             	
//	    			continue;                    
//	    		} else {     	
//	    			count += Integer.parseInt(docOcurrence[1]);                                                                                                  
//	    			positions.add(count);                                                                                                                        
//	    		}
//	    		
//	    		//Found the document skip traversing remaining documents for the term
//	    		if(docCount > Integer.parseInt(docId)) {									
//	    			break;                              
//	    		}
//	    	}            
	    	
	    	while (counter <= termOcurrence.length && docCount <= Integer.parseInt(docId)) {
	    		//Split document:position pair
	    		docOcurrence = termOcurrence[counter++].split(":");							                                               	
	    		docCount += Integer.parseInt(docOcurrence[0]);
	    		
	    		//If document found for term, build position list for the document
	    		if (docCount == Integer.parseInt(docId)) {								                                             	    	
	    			count += Integer.parseInt(docOcurrence[1]);                                                                                                  
	    			positions.add(count);                                                                                                                        
	    		}
	    	}
	    	indexFile.close();
		}
		catch(IOException e) {
			System.err.println("Could not read file" + termIndexFile.getAbsolutePath());
		}
		return positions;
	}
	
	/**
	 * Get stemmed term for given term using snowball stemmer
	 * @param term to be stemmed
	 * @return stemmed term
	 */
	private static String getStemmedTerm (String term) {
		try {
			return EnglishSnowballStemmerFactory.getInstance().process(term);
		} catch (StemmerException e) {
			System.out.println("Stemming failed for term: " + term);
		}
		return term;
	}

}
