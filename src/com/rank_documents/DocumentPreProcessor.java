package com.rank_documents;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.java.searchengine.constants.Constants;
import com.java.searchengine.util.Utilities;

/**
 * 
 * @author Swapnil Gupta
 * @purpose Pre processing logic for documents
 *
 */
public class DocumentPreProcessor {

    // Document length
    private HashMap<Integer, Integer> docIdToItsTermCount;
    private static HashMap<String, String> propertyKeyToFileLocation;

    private int vocabularySize = 0;
    private int termCountInCorpus = 0;
    private int totalDocumentCount = 1;
    private int termCountInDocument = 0;
    private double avgDocLength = 0.0;
    private double avgQueryLength = 0.0;


    public DocumentPreProcessor () {

        propertyKeyToFileLocation = Utilities.loadProperties();
    }


    /**
     * Extract queries from search queries XML file
     * @return map containing all the queries in the xml, keyed by the queryId
     *         Format: key<queryId>, value<query>
     */
    public LinkedHashMap<String, String> extractQueriesXML () {

        File topicsXML = new File(propertyKeyToFileLocation.get("QUERY_XML")).getAbsoluteFile();

        try {
            DocumentBuilder dBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            Document document = dBuilder.parse(topicsXML);
            NodeList topics = document.getElementsByTagName(Constants.SEARCH_QUERY_ROOT_XML_ELEMENT);
            return parseAllXmlNodes(topics);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }


    /**
     * Parse all the xml nodes extracting search queries
     * @param topics
     *        List of xml nodes containing the search queries
     * @return map containing all the queries in the xml, keyed by the queryId
     *         Format: key<queryId>, value<query>
     */
    public LinkedHashMap<String, String> parseAllXmlNodes (NodeList topics) {

        LinkedHashMap<String, String> queryIdToQuery = new LinkedHashMap<String, String>();
        for (int count = 0; count < topics.getLength(); count++) {
            Node topicNode = topics.item(count);
            if (topicNode.getNodeType() == Node.ELEMENT_NODE) {
                Element topic = (Element) topicNode;
                String queryId = topic.getAttribute(Constants.SEARCH_QUERY_ATTRIBUTE_ID);
                String queryText = topic.getElementsByTagName(Constants.SEARCH_QUERY_ELEMENT).item(0).getTextContent();
                queryIdToQuery.put(queryId, queryText);
            }
        }
        return queryIdToQuery;
    }


    /**
     * Compute average length for the list of given search queries
     * @param queryIdToQuery
     *        All the search queries along with their query id
     * @param stopWords
     *        A set of stop words to be excluded
     */
    public void computeAvgQueryLength (HashMap<String, String> queryIdToQuery, HashSet<String> stopWords) {

        int totalTermCountInAllQueries = 0;
        String queryTokens[];

        for (Map.Entry<String, String> query : queryIdToQuery.entrySet()) {
            queryTokens = query.getValue().split(" ");
            for (String token : queryTokens) {
                if (!stopWords.contains(token)) {
                    totalTermCountInAllQueries++;
                }
            }
        }
        setAvgQueryLength((double) totalTermCountInAllQueries / queryIdToQuery.size());
    }


    /**
     * Compute vocabulary size i.e number of unique terms in the corpus, using the termId's file
     */
    public void computeVocabularySize () {

        int vocabularySize = 0;
        File termIdFile = new File(propertyKeyToFileLocation.get("TERMS_ID_FILE")).getAbsoluteFile();

        try {
            BufferedReader reader = new BufferedReader(new FileReader(termIdFile));
            String line = null;
            while ((line = reader.readLine()) != null) {
                vocabularySize++;
            }
            reader.close();
        } catch (IOException e) {
            System.err.println("Could not read file" + termIdFile.getAbsolutePath());
        }
        setVocabularySize(vocabularySize);
    }


    /**
     * Get term count for each term in a document, for all documents
     * @return docToTermToTermCountInDoc, format <docId, <termId, termCount>>
     */
    public HashMap<Integer, HashMap<String, Integer>> getTermCountPerDocument () {// getDocTermCounts

        File documentIndexFile = new File(propertyKeyToFileLocation.get("DOCUMENT_INDEX_FILE")).getAbsoluteFile();
        HashMap<Integer, HashMap<String, Integer>> docToTermToTermCountInDoc = new HashMap<Integer, HashMap<String, Integer>>();

        try {
            BufferedReader documentIndexReader = new BufferedReader(new FileReader(documentIndexFile));
            docToTermToTermCountInDoc = computeTermCountPerDocument(documentIndexReader);
            setAvgDocLength((double) termCountInCorpus / totalDocumentCount);

        } catch (IOException e) {
            System.err.println("Could not read file" + documentIndexFile.getAbsolutePath());
        }
        return docToTermToTermCountInDoc;
    }


    /**
     * Read document index to compute term count for each term in a document, for all documents
     * @param documentIndexReader
     *        buffered reader for document(forward index)
     * @return docToTermToTermCountInDoc, format <docId, <termId, termCount>>
     * @throws NumberFormatException
     * @throws IOException
     */
    HashMap<Integer, HashMap<String, Integer>> computeTermCountPerDocument (BufferedReader documentIndexReader)
            throws NumberFormatException, IOException {

        HashMap<String, Integer> termCount;
        docIdToItsTermCount = new HashMap<Integer, Integer>();
        HashMap<Integer, HashMap<String, Integer>> docToTermToTermCountInDoc = new HashMap<Integer, HashMap<String, Integer>>();

        String line;
        while ((line = documentIndexReader.readLine()) != null) {

            String[] temp = line.split(Constants.tab);
            // -2 as first element is docId and second is termId
            int termFrequency = temp.length - 2;

            // Parse term-positions for same document
            if (Integer.parseInt(temp[0]) == totalDocumentCount) {
                if (!docToTermToTermCountInDoc.containsKey(totalDocumentCount)) {
                    termCount = new HashMap<String, Integer>();
                } else {
                    termCount = docToTermToTermCountInDoc.get(totalDocumentCount);
                    termCount.put(temp[1], termFrequency);
                    docToTermToTermCountInDoc.put(totalDocumentCount, termCount);
                    termCountInDocument += termFrequency;
                }
            } else {
                termCountInCorpus += termCountInDocument;
                docIdToItsTermCount.put(totalDocumentCount, termCountInDocument);
                totalDocumentCount++;
                termCountInDocument = termFrequency;
            }
        }
        termCountInCorpus += termCountInDocument;
        docIdToItsTermCount.put(totalDocumentCount, termCountInDocument);

        documentIndexReader.close();
        return docToTermToTermCountInDoc;
    }


    /**
     * Extract total count of documents in which term occurs
     * @return termToItsContainingDocCount, format: <term, doc count containing the term
     */
    public LinkedHashMap<String, Integer> extractTermOccurenceInDocuments () {

        File termInfoFile = new File(propertyKeyToFileLocation.get("TERM_INFO_FILE")).getAbsoluteFile();
        LinkedHashMap<String, Integer> termToItsContainingDocCount = new LinkedHashMap<String, Integer>();

        try {
            BufferedReader reader = new BufferedReader(new FileReader(termInfoFile));
            String line;
            while ((line = reader.readLine()) != null) {
                String[] tokens = line.split(Constants.tab);
                String termId = tokens[0];
                int termDocCount = Integer.parseInt(tokens[3]);
                termToItsContainingDocCount.put(termId, termDocCount);
            }
            reader.close();
        } catch (IOException e) {
            System.err.println("Could not read file" + termInfoFile.getAbsolutePath());
        }
        return termToItsContainingDocCount;
    }


    /**
     * @return the docIdToItsTermCount
     */
    public HashMap<Integer, Integer> getDocLengths () {

        return docIdToItsTermCount;
    }


    /**
     * @param docIdToItsTermCount
     *        the docLengths to set
     */
    public void setDocLengths (HashMap<Integer, Integer> docIdToItsTermCount) {

        this.docIdToItsTermCount = docIdToItsTermCount;
    }


    /**
     * @return the totalDocumentCount
     */
    public int getotalDocumentCount () {

        return totalDocumentCount;
    }


    /**
     * @param totalDocumentCount
     *        the documentCount to set
     */
    public void setTotalDocumentCount (int totalDocumentCount) {

        this.totalDocumentCount = totalDocumentCount;
    }


    /**
     * @return the termCountInCorpus
     */
    public int getTermCountInCorpus () {

        return termCountInCorpus;
    }


    /**
     * @param termCountInCorpus
     *        the termCountInCorpus to set
     */
    public void setTermCountInCorpus (int totalTermCountInCorpus) {

        this.termCountInCorpus = totalTermCountInCorpus;
    }


    /**
     * @return the avgDocLength
     */
    public double getAvgDocLength () {

        return avgDocLength;
    }


    /**
     * @param avgDocLength
     *        the avgDocLength to set
     */
    public void setAvgDocLength (double avgDocLength) {

        this.avgDocLength = avgDocLength;
    }


    /**
     * @return the avgQueryLength
     */
    public double getAvgQueryLength () {

        return avgQueryLength;
    }


    /**
     * @param avgQueryLength
     *        the avgQueryLength to set
     */
    public void setAvgQueryLength (double avgQueryLength) {

        this.avgQueryLength = avgQueryLength;
    }


    /**
     * @return the avgQueryLength
     */
    public double getVocabularySize () {

        return vocabularySize;
    }


    /**
     * @param vocabularySize
     *        the vocabularySize to set
     */
    public void setVocabularySize (int vocabularySize) {

        this.vocabularySize = vocabularySize;
    }
}
