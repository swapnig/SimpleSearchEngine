package com.java.searchengine.buildindex;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.tartarus.snowball.EnglishSnowballStemmerFactory;
import org.tartarus.snowball.util.StemmerException;

import com.google.code.externalsorting.ExternalSort;
import com.java.searchengine.buildindex.comparator.DocIndexComparator;
import com.java.searchengine.constants.Constants;
import com.java.searchengine.util.FileUtilities;

/**
 * @author Swapnil Gupta
 * @purpose Builds forward and inverted indexes
 */
public class BuildIndexes {

    private String corpusPath = "";
    private static final String pattern = "\\w+(\\.?\\w+)*";

    private File[] listOfFiles;
    private EnglishSnowballStemmerFactory stemmer;

    private HashSet<String> stopWords;
    private static HashMap<String, String> propertyKeyToFileLocation;
    private HashMap<String, Integer> termToItsCountInCorpus;

    private int termsIdCounter = 0;
    private int docuemntIdCounter = 0;


    /**
     * Constructor
     * @param propertyKeyToFileLocation
     *        kev value pairs for all the configurations in property files
     */
    public BuildIndexes (HashMap<String, String> propertyKeyToFileLocation) {

        BuildIndexes.propertyKeyToFileLocation = propertyKeyToFileLocation;
        stemmer = EnglishSnowballStemmerFactory.getInstance();
        termToItsCountInCorpus = new HashMap<String, Integer>();

        corpusPath = propertyKeyToFileLocation.get("INPUT_CORPUS_PATH");
        listOfFiles = FileUtilities.getFileHandlers(corpusPath);
        stopWords = FileUtilities.getFileWords(propertyKeyToFileLocation.get("STOP_WORDS_FILE"));
    }


    /**
     * Driver function for building intermediate index's for the corpus
     */
    public void buildIndex () {

        System.out.println("\nBuilding indexes....");

        initializeOutputFiles();
        buildForwardIndex();
        externalSortDocumentIndex();
        buildInvertedIndex();

        System.out.println("Indexes created in " + propertyKeyToFileLocation.get("INDEX_FOLDER")
                + " folder in current directory");
    }


    /**
     * Build forward index for all the files located in corpus: doc_index.txt.
     */
    public void buildForwardIndex () {

        try {

            File documentIdFile = new File(propertyKeyToFileLocation.get("DOCUMENT_ID_FILE")).getAbsoluteFile();
            File termIdFile = new File(propertyKeyToFileLocation.get("TERMS_ID_FILE")).getAbsoluteFile();
            File documentIndexFile = new File(propertyKeyToFileLocation.get("DOCUMENT_INDEX_FILE")).getAbsoluteFile();

            BufferedWriter documentIdWriter = new BufferedWriter(new FileWriter(documentIdFile, true));
            BufferedWriter termIdWriter = new BufferedWriter(new FileWriter(termIdFile, true));
            BufferedWriter documentIndexWriter = new BufferedWriter(new FileWriter(documentIndexFile, true));

            processAllDocumentsInCorpus(documentIdWriter, termIdWriter, documentIndexWriter);

            documentIdWriter.close();
            termIdWriter.close();
            documentIndexWriter.close();

        } catch (IOException io) {
            io.printStackTrace();
        } catch (StemmerException se) {
            se.printStackTrace();
        }
    }


    /**
     * Process all the documents in corpus, building the forward (document) index
     * 
     * @param documentIdWriter
     *        Buffered Writer for document, id pairs
     * @param termIdWriter
     *        Buffered Writer for term, id pairs
     * @param documentIndexWriter
     *        Buffered Writer for document index
     * @throws IOException
     * @throws StemmerException
     */
    public void processAllDocumentsInCorpus (BufferedWriter documentIdWriter, BufferedWriter termIdWriter,
            BufferedWriter documentIndexWriter) throws IOException, StemmerException {

        for (File corpusFile : listOfFiles) {
            if (corpusFile.isFile()) {
                String corpusFileName = corpusFile.getName();
                Matcher matchedTermsInDocument = extractMatchingTermsFromDocument(corpusFileName);
                HashMap<Integer, ArrayList<Integer>> termIdToAllItsPositionsInDocument = processAllTermsInDocument(
                        matchedTermsInDocument, termIdWriter);

                documentIdWriter.write(++docuemntIdCounter + Constants.tab + corpusFileName + Constants.newline);
                writeDocIndex(docuemntIdCounter, termIdToAllItsPositionsInDocument, documentIndexWriter);
            } else {
                System.out.println(corpusFile + " is invalid file");
            }
        }
    }


    /**
     * Process all the terms in the document, finding all the positions of a term in the document
     * 
     * @param matchedWords
     *        Set of words in an individual document matching a fixed pattern
     * @param termsIDWriter
     *        Buffered Writer for term Id's
     * @return all the terms in the document along with their positions within the document Format : key<termId>,
     *         value<list(pos1, pos2, pos3....)>
     * @throws IOException
     * @throws StemmerException
     */
    public HashMap<Integer, ArrayList<Integer>> processAllTermsInDocument (Matcher matchedTermsInDocument,
            BufferedWriter termsIDWriter) throws IOException, StemmerException {

        int wordPositionInDocument = 0;
        ArrayList<Integer> termPositionInDocument;
        HashMap<Integer, ArrayList<Integer>> termIdToAllItsPositionsInDocument = new HashMap<Integer, ArrayList<Integer>>();

        while (matchedTermsInDocument.find()) {
            wordPositionInDocument++;

            // Get input subsequence matched by the previous match
            String matchedSubSequence = matchedTermsInDocument.group().toLowerCase();
            if (!stopWords.contains(matchedSubSequence)) {

                String stemmedTerm = stemmer.process(matchedSubSequence);
                if (!termToItsCountInCorpus.containsKey(stemmedTerm)) {
                    ++termsIdCounter;
                    termToItsCountInCorpus.put(stemmedTerm, termsIdCounter);
                    termsIDWriter.write(termsIdCounter + Constants.tab + stemmedTerm + Constants.newline);
                }

                // New term found for current document, initialize positions list for it
                int termKey = termToItsCountInCorpus.get(stemmedTerm);
                if (!termIdToAllItsPositionsInDocument.containsKey(termKey)) {
                    termIdToAllItsPositionsInDocument.put(termKey, new ArrayList<Integer>());
                }
                termPositionInDocument = termIdToAllItsPositionsInDocument.get(termKey);
                termPositionInDocument.add(wordPositionInDocument);
            }
        }
        return termIdToAllItsPositionsInDocument;
    }


    /**
     * Write forward index: doc_index, one document at a time
     * 
     * @param docId
     *        document id for which document index is currently being written
     * @param docTerms
     *        Hash map containing all the terms for the given document along with their delta encoded positions
     *        within the document Format : key<termId>, value<list(pos1, pos2, pos3....)>
     * @param docIndexWriter
     *        BufferedWrite for document index
     * @throws IOException
     */
    public void writeDocIndex (int docId, HashMap<Integer, ArrayList<Integer>> termIdToAllItsPositionsInDocument,
            BufferedWriter docIndexWriter) throws IOException {

        for (Map.Entry<Integer, ArrayList<Integer>> entry : termIdToAllItsPositionsInDocument.entrySet()) {
            int termId = entry.getKey();
            ArrayList<Integer> termPositionsInDocument = entry.getValue();

            docIndexWriter.write(docId + Constants.tab + termId);
            for (Integer position : termPositionsInDocument) {
                docIndexWriter.write(Constants.tab + position);
            }
            docIndexWriter.newLine();
        }
    }


    /**
     * External sort document index (forward index), to build term index (inverted index) Sorts first on basis of term
     * id and then on basis of document id
     */
    public void externalSortDocumentIndex () {

        try {
            File documentIndexFile = new File(propertyKeyToFileLocation.get("DOCUMENT_INDEX_FILE")).getAbsoluteFile();
            File sortedDocumentIndexFile = new File(propertyKeyToFileLocation.get("SORTED_DOCUMENT_INDEX_FILE"))
                    .getAbsoluteFile();
            DocIndexComparator docIndexComparator = new DocIndexComparator();
            List<File> fileChunk = ExternalSort.sortInBatch(documentIndexFile, docIndexComparator);
            ExternalSort.mergeSortedFiles(fileChunk, sortedDocumentIndexFile, docIndexComparator);
        } catch (IOException e) {
            System.err.println("Unable to external sort forward index as I/O exception occured");
        }

    }


    /**
     * Process a forward index to form a word level inverted index(documents & positions) : term_index.txt and
     * term_info.txt
     */
    public void buildInvertedIndex () {

        try {
            File sortedDocumentIndexFile = new File(propertyKeyToFileLocation.get("SORTED_DOCUMENT_INDEX_FILE"))
                    .getAbsoluteFile();
            File termIndexFile = new File(propertyKeyToFileLocation.get("TERM_INDEX_FILE")).getAbsoluteFile();
            File termInfoFile = new File(propertyKeyToFileLocation.get("TERM_INFO_FILE")).getAbsoluteFile();

            RandomAccessFile indexFileReadWrite = new RandomAccessFile(termIndexFile, "rw");
            BufferedReader sortedDocIndexReader = new BufferedReader(new FileReader(sortedDocumentIndexFile));
            BufferedWriter termInfoWriter = new BufferedWriter(new FileWriter(termInfoFile, true));

            processForwardIndex(indexFileReadWrite, sortedDocIndexReader, termInfoWriter);

            indexFileReadWrite.close();
            termInfoWriter.close();
            sortedDocIndexReader.close();

        } catch (IOException e) {
            System.err.println("Unable to create inverted index as I/O exception occured");
        }
    }


    /**
     * Process forward index to build inverted index
     * 
     * @param indexFileReadWrite
     *        RandomAccessFile access for termIndexFile
     * @param sortedDocIndexReader
     *        Buffered Writer for sorted term, (document, position) pairs
     * @param termInfoWriter
     *        Buffered Writer for term info
     * @throws IOException
     */
    public void processForwardIndex (RandomAccessFile indexFileReadWrite, BufferedReader sortedDocIndexReader,
            BufferedWriter termInfoWriter) throws IOException {

        int documentCountforTerm = 0;
        int totalPositionCountForTerm = 0;
        int previousDocId = 0;

        String line = "";
        String previousTermId = "1";
        long offset = indexFileReadWrite.getFilePointer();

        indexFileReadWrite.writeBytes(previousTermId + "");
        while (null != (line = sortedDocIndexReader.readLine())) {

            String[] tokens = line.split(Constants.tab);
            int currentDocId = Integer.parseInt(tokens[0]);
            String currentTermId = tokens[1];

            if (!currentTermId.equals(previousTermId)) {
                // Write term_info for previous term
                termInfoWriter.write(previousTermId + Constants.tab + offset + Constants.tab
                        + totalPositionCountForTerm + Constants.tab + documentCountforTerm + Constants.newline);

                offset = writeInfoForNewTerm(indexFileReadWrite, offset, currentTermId);

                // Reset variables for new term
                totalPositionCountForTerm = 0;
                documentCountforTerm = 0;
                previousDocId = 0;
                previousTermId = currentTermId;
            }
            // Write first delta encoded doc:position pair
            indexFileReadWrite.write((Constants.tab + (currentDocId - previousDocId) + ":" + tokens[2]).getBytes());

            int documentPositionCount = getTermPositionCountInDocument(tokens, indexFileReadWrite);
            documentCountforTerm++;
            totalPositionCountForTerm += documentPositionCount - 2;
            previousDocId = currentDocId;
        }
        // Write term_info for last term
        termInfoWriter.write(previousTermId + Constants.tab + offset + Constants.tab + totalPositionCountForTerm
                + Constants.tab + documentCountforTerm + Constants.newline);
    }


    /**
     * Write info for the newly encountered term
     * 
     * @param indexFileReadWrite
     *        RandomAccessFile access for termIndexFile
     * @param offset
     *        current offset in the term_info file
     * @param currentTermId
     *        termId for the new term
     * @throws IOException
     */
    public long writeInfoForNewTerm (RandomAccessFile indexFileReadWrite, long offset, String currentTermId)
            throws IOException {

        indexFileReadWrite.writeBytes(Constants.newline);
        offset = indexFileReadWrite.getFilePointer();
        indexFileReadWrite.writeBytes(currentTermId + "");
        return offset;
    }


    /**
     * Count all the positions for a term in a given document
     * 
     * @param tokens
     *        All the tokens (documentId, termId, termPositions1, termpositions2, .....)
     * @param indexFileReadWrite
     *        RandomAccessFile access for termIndexFile
     * @return counts for all the occurrences of a term in a given document
     * @throws IOException
     */
    public int getTermPositionCountInDocument (String[] tokens, RandomAccessFile indexFileReadWrite) throws IOException {

        int documentPositionCount;
        int previousPosition = Integer.parseInt(tokens[2]);
        for (documentPositionCount = 3; documentPositionCount < tokens.length; documentPositionCount++) {
            int currentPosition = Integer.parseInt(tokens[documentPositionCount]);

            // Write delta position to term_index.txt
            indexFileReadWrite.writeBytes(Constants.tab + 0 + ":" + (currentPosition - previousPosition));
            previousPosition = currentPosition;
        }
        return documentPositionCount;
    }


    /**
     * Function to process the given document (remove headers, Jsoup.parse, pattern matching) and extract words matching
     * a specific pattern
     * 
     * @param corpusFile
     *        file name for an individual file in corpus
     * @return processed text for given corpus file
     * @throws IOException
     */
    public Matcher extractMatchingTermsFromDocument (String corpusFileName) throws IOException {

        // Compile pattern to tokenize words
        Pattern wordPattern = Pattern.compile(pattern);
        String headerLessText = removeFileHeader(corpusFileName);
        Document doc = Jsoup.parse(headerLessText);
        String parsedText = doc.text();

        return wordPattern.matcher(parsedText);
    }


    /**
     * Function to strip of file header(here the pattern used is consecutive occurrence of 2 new lines)
     * 
     * @param corpusFileName
     *        file name for which header needs to be removed
     * @return header less text for given file
     * @throws IOException
     */
    public String removeFileHeader (String corpusFileName) throws IOException {

        String pattern = Constants.newline + Constants.newline;
        File corpusFile = new File(corpusPath + "/" + corpusFileName);
        String htmlText = FileUtils.readFileToString(corpusFile);

        int firstHeaderIndex = htmlText.indexOf(pattern);
        int headerLessTextIndex = htmlText.indexOf(pattern, firstHeaderIndex + 1);

        return htmlText.substring(headerLessTextIndex);
    }


    /**
     * Initialize all the intermediate index files
     */
    public static void initializeOutputFiles () {

        new File("indexes").mkdirs();

        File documentIdFile = new File(propertyKeyToFileLocation.get("DOCUMENT_ID_FILE")).getAbsoluteFile();
        File termIdFile = new File(propertyKeyToFileLocation.get("TERMS_ID_FILE")).getAbsoluteFile();
        File documentIndexFile = new File(propertyKeyToFileLocation.get("DOCUMENT_INDEX_FILE")).getAbsoluteFile();
        File termIndexFile = new File(propertyKeyToFileLocation.get("TERM_INDEX_FILE")).getAbsoluteFile();
        File termInfoFile = new File(propertyKeyToFileLocation.get("TERM_INFO_FILE")).getAbsoluteFile();

        FileUtilities.initializeFile(documentIdFile);
        FileUtilities.initializeFile(termIdFile);
        FileUtilities.initializeFile(documentIndexFile);
        FileUtilities.initializeFile(termIndexFile);
        FileUtilities.initializeFile(termInfoFile);
    }

}
