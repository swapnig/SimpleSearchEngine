package com.rank_documents;
import java.util.Map;
import java.util.Map.Entry;
import java.util.HashMap;
import java.util.LinkedHashMap;

/**
 * 
 * @author Swapnil Gupta
 * @purpose Implements of various scoring functions provided to rank documents
 *
 */
public class ScoringFunctions {
	
	double logBase2 = Math.log(2);																					//Compute constant log base 2
	HashMap<Integer, Double> documentMagnitudes;																	//Store document magnitude
	
	//Compute term frequency for each term in the query
	public LinkedHashMap<String, Integer> computeTermFrequencyInQuery(LinkedHashMap<String, Long> termOffsetInIndex){
		
		LinkedHashMap<String, Integer> termFrequencyInQuery = new LinkedHashMap<String, Integer>();					//Initialize struct for storing term frequency in query
		for (Map.Entry<String, Long> termOffset : termOffsetInIndex.entrySet()){									//Traverse termOffset to get all the terms in a query
			
			String termId = termOffset.getKey();																	//Extract term id from query
			if(termFrequencyInQuery.containsKey(termId))															//If term exist more than once in query
				termFrequencyInQuery.put(termId, termFrequencyInQuery.get(termId) + 1);								//Increment count by 1
			else																									//Encountered term for first time
				termFrequencyInQuery.put(termId, 1);																//Add term to struct
		}
		return termFrequencyInQuery;																				//Return query-term frequency for current query
	}
	

	//Create and return the query vector for the current query
	public HashMap<String, Double> buildQueryVector(LinkedHashMap<String, Integer> termFrequencyInQuery, double avgQueryLength){
		
		double ratio = termFrequencyInQuery.size() / avgQueryLength;												//Compute constant ratio for using calculations
		HashMap<String, Double> queryVector = new HashMap<String, Double>();										//Initialize query vector
		
		for (Map.Entry<String, Integer> term : termFrequencyInQuery.entrySet()){									//Parse queryTermFrequency hash map
			String termId = term.getKey();																			//Extract termId
			int termFrequency = term.getValue();																	//Extract term frequency
			double termOkapiComponent = computeOkapiComponent(termFrequency, ratio);								//Get Okapi component for query term
			
			queryVector.put(termId, termOkapiComponent);															//Put query term Okapi component into hash map
		}																											
		return queryVector;																							//Return query vector for current query
	}
	
	
	//Compute the okapi vector for current query
	public double computeOkapiComponent(int frequency, double ratio){
		return (frequency / (frequency + 0.5 + 1.5 * ratio));														//okapi term vector formula
	}
	
	
	//Create and return the Okapi TF document vector for all the relevant documents for current query
	public HashMap<Integer, HashMap<String,Double>> buildTFDocumentVector(LinkedHashMap<Integer,LinkedHashMap<String,Integer>> relevantDocuments,
																		  HashMap<Integer, HashMap<String,Integer>> docTermCount,
																		  HashMap<Integer,Integer> docLengths, double avgDocLength){
		
		int docId, termFrequency;																					//Define variables to hold intermediate values
		double ratio, magnitude, termOkapiComponent;
		
		HashMap<String,Integer> termCounts;																			//Track document term counts
		HashMap<String,Double> termVector;																			//Track query-term okapi component
		documentMagnitudes = new HashMap<Integer, Double>();														//Track document magnitudes
		
		HashMap<Integer, HashMap<String,Double>> documentVector = new HashMap<Integer, HashMap<String,Double>>();	//Initialize new documentVector object
		
		for (Entry<Integer,LinkedHashMap<String,Integer>> document : relevantDocuments.entrySet()){					//Parse each relevant document one by one
			
			magnitude = 0.0;																						//Initialize document magnitude score
			docId = document.getKey();																				//Extract document id
			ratio = docLengths.get(docId) / avgDocLength;															//Compute constant ratio for using calculations
			
			termVector = new HashMap<String,Double>();																//Initialize termVector reference
			for (Map.Entry<String, Integer> termDocumentFrequency : document.getValue().entrySet()){				//Traverse all the query-terms in document
				
				String termId = termDocumentFrequency.getKey();														//Extract term id
				termFrequency = termDocumentFrequency.getValue();													//Extract term frequency
				termOkapiComponent = computeOkapiComponent(termFrequency, ratio);									//Compute term okapi component
				
				termVector.put(termId, termOkapiComponent);															//Link term id and term okapi component
				documentVector.put(docId, termVector);																//Store term okapi component for all query terms in document
			}
			
			termCounts = docTermCount.get(docId);																	//Get all term counts for current document
			for(Map.Entry<String, Integer> termDocumentFrequency : termCounts.entrySet()){							//Process  term frequency one at a time
				termFrequency = termDocumentFrequency.getValue();													//Get term frequency of current term
				termOkapiComponent = computeOkapiComponent(termFrequency, ratio);									//Compute and store okapi component for a term
				magnitude += (termOkapiComponent * termOkapiComponent);												//Increment magnitude of the document 
			}
			documentMagnitudes.put(docId, Math.sqrt(magnitude));													//Store document magnitude into hash map	
		}
		return documentVector;																						//Return set of all the document vectors
	}
	
	
	//Return document magnitude for all the documents in corpus
	public HashMap<Integer, Double> getDocumentMagnitudes(){
		return documentMagnitudes;
	}
	
	
	//Create and return the TF IDF document vector for all the relevant documents for current query
	public HashMap<Integer, HashMap<String,Double>> buildTFIDFDocumentVector(LinkedHashMap<Integer,LinkedHashMap<String,Integer>> relevantDocuments, 
																			 LinkedHashMap<String,Double> termTfIdfScore,
																			 HashMap<Integer, HashMap<String, Integer>> docTermCount,
			   																 HashMap<Integer,Integer> docLengths, double avgDocLength){
		
		String termId;
		int docId, termFrequency;																					//Define variables to hold intermediate values
		double ratio, termOkapiComponent, tfidfFactor, tfIdfScore, magnitude;
		
		documentMagnitudes = new HashMap<Integer, Double>();														//Store document magnitudes
		HashMap<String, Integer> termCounts = new HashMap<String, Integer>();										//Stor term frequency in documents
		
		HashMap<String,Double> termVector;																			//Track query-term okapi component
		HashMap<Integer, HashMap<String,Double>> documentVector = new HashMap<Integer, HashMap<String,Double>>();	//Initialize new documentVector object				
		
		for (Entry<Integer,LinkedHashMap<String,Integer>> document : relevantDocuments.entrySet()){					//Parse each relevant document one by one
			magnitude = 0.0;
			docId = document.getKey();																				//Extract document id
			ratio = docLengths.get(docId) / avgDocLength;															//Compute constant ratio for using calculations
			
			termVector = new HashMap<String,Double>();																//Initialize termVector reference
			for (Map.Entry<String, Integer> termDocumentFrequency : document.getValue().entrySet()){				//Traverse all the query-terms in document
				
				termId = termDocumentFrequency.getKey();															//Extract term id
				termFrequency = termDocumentFrequency.getValue();													//Extract term frequency
				
				termOkapiComponent = computeOkapiComponent(termFrequency, ratio);									//Compute tf-idf term component for document
				tfidfFactor = termTfIdfScore.get(termId);															//Extract tf-idf factor for given term	
				tfIdfScore = termOkapiComponent * tfidfFactor;														//Compute okapi tf-idf score for document-term pair
				
				termVector.put(termId, tfIdfScore);																	//Link term id and term tf-idf component
				documentVector.put(docId, termVector);																//Store term tf-idf component for all query terms in document
			}
			
			termCounts = docTermCount.get(docId);                                                                   //Get all term counts for current document     
			for(Map.Entry<String, Integer> termDocumentFrequency : termCounts.entrySet()){                          //Process  term frequency one at a time
				termId = termDocumentFrequency.getKey();
				termFrequency = termDocumentFrequency.getValue();													//Get term frequency of current term
				termOkapiComponent = computeOkapiComponent(termFrequency, ratio);                                   //Compute and store okapi component for a term
				tfidfFactor = termTfIdfScore.get(termId);															//Extract tf-idf factor for given term	
				tfIdfScore = termOkapiComponent * tfidfFactor;														//Compute okapi tf-idf score for document-term pair
				magnitude += (termOkapiComponent * tfIdfScore);                                             		//Increment magnitude of the document          
			}                                                                                                                                                      
			documentMagnitudes.put(docId, Math.sqrt(magnitude));                                                    //Store document magnitude into hash map	   
		}
			return documentVector;																					//Return set of all the document vectors																					//Return set of all the document vectors
	}
	
	
	//Get constant tf-idf factor for each query-term log base 2
	public LinkedHashMap<String,Double> getTermTfIdfScores(LinkedHashMap<String,Integer> termOccurenceInDocuments, int documentCount){
		
		LinkedHashMap<String,Double> termTfIdfScore = new LinkedHashMap<String,Double>();							//Initialize termTfIdf score hash map
		
		for (Entry<String, Integer> entry : termOccurenceInDocuments.entrySet()){									//Parse each element one by one
			String termId = entry.getKey();																			//Extract term id
			int occurenceInDistinctDocuments = entry.getValue();													//Extract term occurence in document
			double tfIdfScore = Math.log(documentCount / occurenceInDistinctDocuments) / logBase2;					//Compute tf-idf score
			
			termTfIdfScore.put(termId, tfIdfScore);																	//Store tf-idf factor for each query-term
		}
		return termTfIdfScore;																						//Return tf-idf score for each query term
	}
	
	
	//Compute document rank using okapi tf/tf-idf algorithm
	public Map<Integer,Double> computeOkapiScore(HashMap<Integer, HashMap<String,Double>> documentVector, HashMap<String,Double> queryVector,
												 HashMap<Integer, Double> documentMagnitudes){
		
		Map<Integer, Double> okapiScoredDocuments = new HashMap<Integer, Double>();									//Initialize struct for document score
		
		double okapiScore = 0.0;																					//Track total document score
		double termDocumentScore = 0.0;																				//Track term score in document
		double termQueryScore = 0.0;																				//Track term score in query
		
		double vectorProduct;																						//Store summation of cosine product
		double documentMagnitude;																					//Store summation of square of document scores
		double termQuerySquares = 0.0;																				//Store summation of square of query scores
		
		for (double queryTermFrequency : queryVector.values())														//Process each term in query one a a time
			termQuerySquares += (queryTermFrequency * queryTermFrequency);											//Compute query squares for each query term
		
		double sqrtQueryVector = Math.sqrt(termQuerySquares);														//Compute square root for query magnitude
		
		for (Entry<Integer, HashMap<String, Double>> document : documentVector.entrySet()){							//Parse each element one by one
			
			int docId = document.getKey();																			//Reinitialize values for current document
			vectorProduct = 0.0;
			
			for (Entry<String, Double> termDocumentFrequency : document.getValue().entrySet()){						//Process each term in query one a a time
				
				String termId = termDocumentFrequency.getKey();
				termDocumentScore = termDocumentFrequency.getValue();												//Store term score for document
				termQueryScore = queryVector.get(termId);															//Store term score for query
				
				vectorProduct += (termDocumentScore * termQueryScore);												//Compute vector product for given term						
			}
			documentMagnitude = documentMagnitudes.get(docId);														//Extract document magnitude for given document-term
			okapiScore = vectorProduct / ( documentMagnitude * sqrtQueryVector);									//Compute final document okapi score
			okapiScoredDocuments.put(docId, okapiScore);															//Store document okapi score
		}
		return okapiScoredDocuments;																				//Return okapi scored documents
	}
	
	
	//Compute document rank using BM 25 algorithm
	public HashMap<Integer,Double> computeBM25Score(LinkedHashMap<Integer,LinkedHashMap<String,Integer>> relevantDocuments, 
														   LinkedHashMap<String,Integer> queryTermFrequency, LinkedHashMap<String,Integer> termDocumentOccurence, 
														   HashMap<Integer,Integer> docLengths, double avgDocLength, int documentCount){
		
		double k1 = 1.2;																							//BM 25 constant values
		double k2 = 100;
		double b = 0.75;
		double param1, param2, param3, K, product, score;															//Intermediate values
		
		int docId, termDocumentFrequency, termQueryFrequency;														//Term frequencies
		HashMap<Integer,Double> bm25ScoredDocuments = new HashMap<Integer,Double>();								//Initialize struct for document score
		
		for (Entry<Integer,LinkedHashMap<String,Integer>> document : relevantDocuments.entrySet()){					//Parse each relevant document one by one
			
			score = 0.0;																							//Initialize score
			docId = document.getKey();																				//Extract docId
			K = k1 * ((1-b) + (b * (docLengths.get(docId) / avgDocLength)));										//Compute constant K
			
			for (Map.Entry<String, Integer> termFrequency : document.getValue().entrySet()){						//Process one term at a time
				
				String termId = termFrequency.getKey();																//Extract term id
				termDocumentFrequency = termFrequency.getValue();													//Extract term document frequency
				termQueryFrequency = queryTermFrequency.get(termId);												//Extract term query frequency
				
				param1 = (documentCount + 0.5) / (termDocumentOccurence.get(termId) + 0.5);							//Compute intermediate values
				param2 = (((1 + k1) * termDocumentFrequency ) / (K + termDocumentFrequency));
				param3 = (((1 + k2) * termQueryFrequency ) / (k2 + termQueryFrequency));
				
				product = (Math.log(param1) / logBase2)  * param2 * param3;
				score += product;																					//Compute score for the document
			}
			bm25ScoredDocuments.put(docId, score);																	//Store BM25 score for document
		}
		return bm25ScoredDocuments;																					//Return BM25 scored documents
	}
	
	
	//Compute document rank using Language model with Laplace Smoothing algorithm
	public HashMap<Integer,Double> computeLaplaceScore(LinkedHashMap<Integer, LinkedHashMap<String,Integer>> relevantDocuments, 
													   LinkedHashMap<String, Long> termOffsetInIndex, HashMap<Integer, Integer> docLengths, double vocabularySize){
		
		int termFrequency, docId;
		double denominator, probability, score;
		
		LinkedHashMap<String,Integer> termDocumentFrequency;														//Track term frequency in document
		HashMap<Integer,Double> laplaceScoredDocuments = new HashMap<Integer,Double>();								//Initialize struct for document score														//Track query-term count in document
		
		for (Entry<Integer,LinkedHashMap<String,Integer>> document : relevantDocuments.entrySet()){					//Parse each relevant document one by one
			
			score = 0.0;																							//Initialize score for current document to 0
			docId = document.getKey();																				//Extract docId
			
			termDocumentFrequency = document.getValue();															//Extract term count in document
			denominator = docLengths.get(docId) + vocabularySize;													//Compute constant denominator for a document
			
			for (Map.Entry<String,Long> term : termOffsetInIndex.entrySet()){										//Process each term one at a time
				String termId = term.getKey();
				
				termFrequency = termDocumentFrequency.containsKey(termId) ? termDocumentFrequency.get(termId) : 0;	//Term exists so get termFrequency else 0
				probability = (termFrequency + 1) / denominator;													//Compute probability for given term
				score += (Math.log(probability) / logBase2);														//Increment laplace score for document 
			}
			laplaceScoredDocuments.put(docId, score);																//Store laplace score for document
		}
		return laplaceScoredDocuments;																				//Return laplace scored documents
	}
	
	
	//Compute document rank using Language model with Jelinek-Mercer Smoothing algorithm
	public HashMap<Integer,Double> computeJMScore(LinkedHashMap<Integer,LinkedHashMap<String,Integer>> relevantDocuments,
												  LinkedHashMap<String,Long> termOffsetInIndex, HashMap<Integer,Integer> docLengths, double JMConstant){
		
		int docId, termFrequency, documentLength;																	//Define intermediate variable
		double probability, score;																					
		double lambda = 0.2;																						//Initialize lambda value
		
		LinkedHashMap<String,Integer> termDocumentFrequency;														//Track term frequency in document
		HashMap<Integer,Double> jmScoredDocuments = new HashMap<Integer,Double>();									//Initialize struct for document score
		
		for (Entry<Integer,LinkedHashMap<String,Integer>> docs : relevantDocuments.entrySet()){						//Parse each relevant document one by one
			
			docId = docs.getKey();																					//Extract docId
			documentLength = docLengths.get(docId);																	//Extract document length for current document 
			termDocumentFrequency = docs.getValue();																//Extract term frequency in document

			score = 0.0;																							//Initialize jm score of document to 0
			for (Map.Entry<String,Long> term : termOffsetInIndex.entrySet()){										//Process one term at a time
				String termId = term.getKey();
				
				termFrequency = termDocumentFrequency.containsKey(termId) ? termDocumentFrequency.get(termId) : 0;	//Term exists so get termFrequency else 0 
				probability = (lambda * ((double)termFrequency / documentLength)) + ((1-lambda) * JMConstant);		//Compute probability for given term
				score += (Math.log(probability));																	//Increment JM score for document
			}
				jmScoredDocuments.put(docId, score);																//Store JM score for document
		}
		return jmScoredDocuments;																					//Return JM scored documents
	}
	
}
