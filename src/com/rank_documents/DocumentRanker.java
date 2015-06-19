package com.rank_documents;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.tartarus.snowball.EnglishSnowballStemmerFactory;
import org.tartarus.snowball.util.StemmerException;

import com.java.searchengine.util.FileUtilities;
import com.java.searchengine.util.Utilities;

/**
 * 
 * @author Swapnil Gupta
 * @purpose Pre process documents and then ranks them using the user selected scoring function
 *
 */
public class DocumentRanker {

    private static ScoringFunctions scoringFn;
    private static File termIndexFile;
    private static HashMap<String, String> properties;

    /******************************* Pre Processed data structures and variables ***************************************/
    private static int totalDocumentCount;
    private static double totalTermCount;
    private static double avgDocLength;
    private static double avgQueryLength;
    private static double vocabularySize;

    private static HashSet<String> stopWords;

    private static LinkedHashMap<String, String> queries;
    private static HashMap<Integer, Integer> docLengths;
    private static HashMap<Integer, Double> documentMagnitudes;
    private static HashMap<Integer, HashMap<String, Integer>> allTermsCountInDocument;
    /*****************************************************************************************************************/

    /******************************** Data structures created for individual query *************************************/
    private static double queryTermsOccurencesInCorpous = 0;
    private static LinkedHashMap<String, Integer> termOccurenceInDocuments;


    /*****************************************************************************************************************/

    public DocumentRanker () {

        properties = Utilities.loadProperties();
        scoringFn = new ScoringFunctions();
        termIndexFile = new File(properties.get("TERM_INDEX_FILE")).getAbsoluteFile();
    }


    /**
     * Rank the documents in order of decreasing relevance for each query, using the given scoring function, outputting
     * to given filename
     * @param scoringFunction
     *        scoring function to be used for scoring documents
     * @param outputFileName
     *        file to store the output of search results
     */
    public void rankDocuments (String scoringFunction, String outputFileName) {

        File outputFile = new File(outputFileName);
        FileUtilities.initializeFile(outputFile);

        try {
            BufferedWriter outputWriter = new BufferedWriter(new FileWriter(outputFile.getAbsoluteFile(), true));

            for (Map.Entry<String, String> query : queries.entrySet()) {
                Map<Integer, Double> scoredDocuments = scoreRelevantDocumentsForEachQuery(query.getValue(),
                        scoringFunction);
                Map<Integer, Double> rankedDocuments = Utilities.rankDocuments(scoredDocuments);
                writeOutput(query.getKey(), rankedDocuments, outputWriter);
            }
            outputWriter.close();
            System.out.println(outputFile + " has been created");
        } catch (IOException e) {
            System.err.println("Could not create output file : " + outputFile.getAbsolutePath());
        }
    }


    /**
     * Score relevant documents for each query in the given set
     * @param queryText
     *        Query to find relevant documents
     * @param scoringFunction
     *        String literal indicating the choice of scoring function
     * @return relevant documents for the query
     */
    public Map<Integer, Double> scoreRelevantDocumentsForEachQuery (String queryText, String scoringFunction) {

        Map<Integer, Double> scoredDocuments;
        HashMap<String, Double> queryVector;
        HashMap<Integer, HashMap<String, Double>> documentVector;
        LinkedHashMap<String, Integer> termFrequencyInQuery;
        LinkedHashMap<String, Long> termOffsetForEachTermInQuery = computeOffsetForEachTermInQuery(queryText, stopWords);
        LinkedHashMap<Integer, LinkedHashMap<String, Integer>> relevantDocumentsWithTermFrequenciesForQuery = findRelevantDocumentsForAllTermsQuery(termOffsetForEachTermInQuery);

        switch (scoringFunction) {
            case "1": // Okapi TF
                termFrequencyInQuery = scoringFn.computeTermFrequencyInQuery(termOffsetForEachTermInQuery);

                queryVector = scoringFn.buildQueryVector(termFrequencyInQuery, avgQueryLength);
                documentVector = scoringFn.buildTFDocumentVector(relevantDocumentsWithTermFrequenciesForQuery,
                        allTermsCountInDocument, docLengths, avgDocLength);
                documentMagnitudes = scoringFn.getDocumentMagnitudes();
                scoredDocuments = scoringFn.computeOkapiScore(documentVector, queryVector, documentMagnitudes);
                break;

            case "2": // TF-IDF
                termFrequencyInQuery = scoringFn.computeTermFrequencyInQuery(termOffsetForEachTermInQuery);
                queryVector = scoringFn.buildQueryVector(termFrequencyInQuery, avgQueryLength);
                LinkedHashMap<String, Double> termTfIdfScore = scoringFn.getTermTfIdfScores(termOccurenceInDocuments,
                        docLengths.size());
                documentVector = scoringFn.buildTFIDFDocumentVector(relevantDocumentsWithTermFrequenciesForQuery,
                        termTfIdfScore, allTermsCountInDocument, docLengths, avgDocLength);
                documentMagnitudes = scoringFn.getDocumentMagnitudes();
                scoredDocuments = scoringFn.computeOkapiScore(documentVector, queryVector, documentMagnitudes);
                break;

            case "3": // Okapi BM-25
                termFrequencyInQuery = scoringFn.computeTermFrequencyInQuery(termOffsetForEachTermInQuery);
                scoredDocuments = scoringFn.computeBM25Score(relevantDocumentsWithTermFrequenciesForQuery,
                        termFrequencyInQuery, termOccurenceInDocuments, docLengths, avgDocLength, totalDocumentCount);
                break;

            case "4": // Language model with Laplace Smoothing
                scoredDocuments = scoringFn.computeLaplaceScore(relevantDocumentsWithTermFrequenciesForQuery,
                        termOffsetForEachTermInQuery, docLengths, vocabularySize);
                break;

            case "5": // Language model with Jelinek-Mercer Smoothing
            default:
                double queryJMConstant = queryTermsOccurencesInCorpous / totalTermCount;
                scoredDocuments = scoringFn.computeJMScore(relevantDocumentsWithTermFrequenciesForQuery,
                        termOffsetForEachTermInQuery, docLengths, queryJMConstant);
        }
        return scoredDocuments;
    }


    /**
     * Get term offsets for all the terms in query
     * @param query
     *        text
     * @param stopWords
     *        set of stop words
     * @return
     */
    public LinkedHashMap<String, Long> computeOffsetForEachTermInQuery (String query, HashSet<String> stopWords) {

        File termIdFile = new File(properties.get("TERMS_ID_FILE")).getAbsoluteFile();
        LinkedHashMap<String, Long> termOffsetForEachTermInQuery = new LinkedHashMap<String, Long>();

        for (String term : query.split(" ")) {
            term = term.toLowerCase();
            if (!stopWords.contains(term)) {
                try {
                    term = EnglishSnowballStemmerFactory.getInstance().process(term);
                    String termId = FileUtilities.getID(termIdFile, term);
                    computeOffsetForSingleTermInQuery(termId, termOffsetForEachTermInQuery);
                } catch (StemmerException e) {
                    System.out.println("Stemming failed for term: " + term);
                } catch (NumberFormatException e) {
                    System.err.println("Could not read file : " + termIdFile.getAbsolutePath());
                } catch (IOException e) {
                    System.err.println("Could not read file : " + termIdFile.getAbsolutePath());
                }

            }
        }
        return termOffsetForEachTermInQuery;
    }


    /**
     * Get term offset for a single term in query
     * @param term
     *        id for which offset needs to be found
     * @param stopWords
     *        set of stop words
     * @return
     */
    public void computeOffsetForSingleTermInQuery (String termId, LinkedHashMap<String, Long> termOffsetInIndex)
            throws NumberFormatException, IOException {

        String line;
        File termInfoFile = new File(properties.get("TERM_INFO_FILE")).getAbsoluteFile();
        BufferedReader reader = new BufferedReader(new FileReader(termInfoFile));

        while ((line = reader.readLine()) != null) {
            String[] tokens = line.split("\t");
            if (tokens[0].equals(termId)) {
                long offset = Long.parseLong(tokens[1]);
                termOffsetInIndex.put(tokens[0], offset);
                queryTermsOccurencesInCorpous += Integer.parseInt(tokens[2]);
                break;
            }
        }
        reader.close();
    }


    /**
     * Find relevant documents for all the terms in the current query
     * @param termOffsetForEachTermInQuery
     *        term offset for each term in query
     * @return All the relevant documents for a query
     */
    public LinkedHashMap<Integer, LinkedHashMap<String, Integer>> findRelevantDocumentsForAllTermsQuery (
            HashMap<String, Long> termOffsetForEachTermInQuery) {

        LinkedHashMap<String, Integer> termFrequencyPairs = new LinkedHashMap<String, Integer>();
        LinkedHashMap<Integer, LinkedHashMap<String, Integer>> relevantDocumentsWithTermFrequenciesForQuery = new LinkedHashMap<Integer, LinkedHashMap<String, Integer>>();

        try {
            for (Map.Entry<String, Long> termOffset : termOffsetForEachTermInQuery.entrySet()) {
                findRelevantDocumentsForATermInQuery(termOffset, termFrequencyPairs,
                        relevantDocumentsWithTermFrequenciesForQuery);
            }
        } catch (IOException e) {
            System.err.println("Could not read file : " + termIndexFile.getAbsolutePath());
        }
        return relevantDocumentsWithTermFrequenciesForQuery;
    }


    /**
     * Find relevant documents for single term in the current query
     * @param termOffset
     * @param termFrequencyPairs
     * @param relevantDocumentsWithTermFrequenciesForQuery
     * @throws IOException
     */
    void findRelevantDocumentsForATermInQuery (Map.Entry<String, Long> termOffset,
            LinkedHashMap<String, Integer> termFrequencyPairs,
            LinkedHashMap<Integer, LinkedHashMap<String, Integer>> relevantDocumentsWithTermFrequenciesForQuery)
            throws IOException {

        String termId = termOffset.getKey();
        long offset = termOffset.getValue();

        RandomAccessFile indexFile = new RandomAccessFile(termIndexFile, "r");
        indexFile.seek(offset);

        String line = indexFile.readLine();
        String[] termPoisitons = line.split("\t");

        int previousDocId = Integer.parseInt(termPoisitons[1].split(":")[0]);
        int termFrequency = 0;

        String[] deltaDocId;

        int docId = 0;
        for (int i = 1; i < termPoisitons.length; i++) {

            // Split term document-position pairs
            deltaDocId = termPoisitons[i].split(":");

            // Update document id for unique documents it is non 0 while for different positions within the same
            // document it is 0 so no effect of summation
            docId += Integer.parseInt(deltaDocId[0]);

            // Update frequency as traversing same document
            if (docId == previousDocId) {
                termFrequency++;
                // New doc id found for given term
            } else {
                if (!relevantDocumentsWithTermFrequenciesForQuery.containsKey(previousDocId)) {
                    termFrequencyPairs = new LinkedHashMap<String, Integer>();
                } else {

                    termFrequencyPairs = relevantDocumentsWithTermFrequenciesForQuery.get(previousDocId);
                    termFrequencyPairs.put(termId, termFrequency);
                    relevantDocumentsWithTermFrequenciesForQuery.put(previousDocId, termFrequencyPairs);

                    termFrequency = 1;
                    previousDocId = docId;
                }
            }
        }

        // Add the values for last document
        if (!relevantDocumentsWithTermFrequenciesForQuery.containsKey(previousDocId)) {
            termFrequencyPairs = new LinkedHashMap<String, Integer>();
        } else {
            termFrequencyPairs = relevantDocumentsWithTermFrequenciesForQuery.get(previousDocId);
        }
        termFrequencyPairs.put(termId, termFrequency);
        relevantDocumentsWithTermFrequenciesForQuery.put(previousDocId, termFrequencyPairs);

        indexFile.close(); // Close term_index RandomAccess Reader
    }


    /**
     * Write the ranked documents to output file
     * @param queryNumber
     *        query number for current query
     * @param rankedDocuments
     *        documents ranked in order of decreasing relevance for given query
     * @param outputWriter
     *        writer for output file
     */
    public void writeOutput (String queryNumber, Map<Integer, Double> rankedDocuments, BufferedWriter outputWriter) {

        File documentIdFile = new File(properties.get("DOCUMENT_ID_FILE")).getAbsoluteFile();
        HashMap<Integer, String> docIds = FileUtilities.getDocNames(documentIdFile);
        int rank = 1;

        for (Entry<Integer, Double> entry : rankedDocuments.entrySet()) {
            double score = entry.getValue();
            int docID = entry.getKey();
            String documentName = docIds.get(docID);

            try {
                outputWriter.write(queryNumber + " 0 " + documentName + " " + rank++ + " " + score + " run1" + "\n");
            } catch (IOException e) {
                System.err.println("Could not write to file" + documentIdFile.getAbsolutePath());
            }
        }
    }


    /**
     * Pre process all the queries and documents in corpus
     * @param preProcess
     *        reference to pre processed documents
     * @param topicsXml
     *        query xml contacting all the queries
     * @param stopList
     *        file containing all the stop list words
     */
    public void rankingPreProcess (DocumentPreProcessor preProcess, String topicsXml, String stopList) {

        stopWords = FileUtilities.getFileWords(stopList);

        queries = preProcess.extractQueriesXML();
        preProcess.computeAvgQueryLength(queries, stopWords);
        avgQueryLength = preProcess.getAvgQueryLength();

        allTermsCountInDocument = preProcess.getTermCountPerDocument();
        docLengths = preProcess.getDocLengths();
        avgDocLength = preProcess.getAvgDocLength();
        preProcess.computeVocabularySize();
        vocabularySize = preProcess.getVocabularySize();
        termOccurenceInDocuments = preProcess.extractTermOccurenceInDocuments();

        totalDocumentCount = preProcess.getotalDocumentCount();
        totalTermCount = preProcess.getTermCountInCorpus();
    }

}
