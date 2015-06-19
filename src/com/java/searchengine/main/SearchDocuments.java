package com.java.searchengine.main;

import java.io.File;
import java.util.HashMap;
import java.util.Scanner;

import com.java.searchengine.buildindex.BuildIndexes;
import com.java.searchengine.constants.Constants;
import com.java.searchengine.extractinfo.ExtractInfo;
import com.java.searchengine.util.FileUtilities;
import com.java.searchengine.util.Utilities;
import com.rank_documents.DocumentPreProcessor;
import com.rank_documents.DocumentRanker;

/**
 * @author Swapnil Gupta
 * @purpose Main interface to interact with the application
 */
public class SearchDocuments {

    private static DocumentRanker ranker;
    private static HashMap<String, String> propertyKeyToFileLocation = new HashMap<String, String>();


    public static void main (final String[] args) {

        propertyKeyToFileLocation = Utilities.loadProperties();
        checkContinueMenu("displayMainMenu", "Continue in main menu (y/n):");
        System.out.println(Constants.newline + "Program execution complete");
    }


    /**
     * Display main menu
     * @return boolean true if want to exit program else false to continue
     */
    @SuppressWarnings("resource")
    public static boolean displayMainMenu () {

        System.out.println(Constants.newline + "What do you want to do");
        System.out.println("1. Build indexes");
        System.out.println("2. Rank documents");
        System.out.println("3. Get Info");
        System.out.println("4. Exit Program");
        System.out.println("Note : You need to build indexes to rank documents or to get info");

        final String choice = new Scanner(System.in).next();
        return processMainMenuChoice(choice);
    }


    /**
     * Display menu to choose scoring function to rank documents
     * @return boolean true if want to exit program else false to continue
     */
    @SuppressWarnings("resource")
    public static boolean displayRankMenu () {

        System.out.println(Constants.newline + "What do you want to do");
        System.out.println("1. Okapi TF");
        System.out.println("2. TF-IDF");
        System.out.println("3. Okapi BM-25");
        System.out.println("4. Language model with Laplace Smoothing");
        System.out.println("5. Language model with Jelinek-Mercer Smoothing");
        System.out.println("6. Exit Ranking");

        final String scoringFunction = new Scanner(System.in).next();
        return processRankMenuChoice(scoringFunction);
    }


    /**
     * Display read menu to the user, providing options to know metadata about documents and terms
     * @return boolean true if want to exit program else false to continue
     */
    @SuppressWarnings("resource")
    public static boolean displayReadMenu () {

        System.out.println(Constants.newline + "What do you want to do");
        System.out.println("1. Search term");
        System.out.println("2. Search document");
        System.out.println("3. Search term in document");
        System.out.println("4. Go back to main menu");

        final String choice = new Scanner(System.in).next();
        return processReadMenuChoice(choice);
    }


    /**
     * Check whether to continue menu
     * @param menuChoice
     *        String indicating the menu to continue in
     * @param message
     *        String to be showed when querying user for continuing in menu
     */
    public static void checkContinueMenu (final String menuChoice, final String message) {

        String response = "y";

        while (!response.equals("n")) {
            if (response.equals("y") && (true == selectMenuForPurpose(menuChoice))) {
                break;
            }
            System.out.println(Constants.newline + message);
            response = new Scanner(System.in).next();
        }
    }


    /**
     * Show menu for given purpose
     * @param menuChoice
     *        : String indicating menu choice
     * @return boolean indicating whether to continue in menu
     */
    public static boolean selectMenuForPurpose (final String menuChoice) {

        switch (menuChoice) {
            case "displayRankMenu":
                return displayRankMenu();
            case "displayReadMenu":
                return displayReadMenu();
            case "displayMainMenu":
                return displayMainMenu();
            default:
                return displayMainMenu();
        }
    }


    /**
     * Process user choice from main menu
     * @param choice
     *        1 : Build indexes
     *        2 : Rank documents
     *        3 : Get Info
     *        4 : Exit Program
     * @return boolean indicating whether to continue in menu
     */
    public static boolean processMainMenuChoice (final String choice) {

        switch (choice) {
            case "1":
                final String corpusPath = propertyKeyToFileLocation.get("INPUT_CORPUS_PATH");
                String stopList = propertyKeyToFileLocation.get("STOP_WORDS_FILE");

                if (FileUtilities.isValidCorpusDirectory(corpusPath)
                        && FileUtilities.isValidFile(stopList, Constants.TEXT_EXTENSION)) {
                    new BuildIndexes(propertyKeyToFileLocation).buildIndex();
                }
                break;

            case "2":
                final String queryXml = propertyKeyToFileLocation.get("QUERY_XML");
                stopList = propertyKeyToFileLocation.get("STOP_WORDS_FILE");

                if (FileUtilities.isValidFile(queryXml, Constants.XML_EXTENSION)
                        && FileUtilities.isValidFile(stopList, Constants.TEXT_EXTENSION)) {

                    // Ensure that ranker object is only created once
                    if (null == ranker) {
                        System.out.println(Constants.newline + "Processing documents...." + Constants.newline);

                        final DocumentPreProcessor preProcess = new DocumentPreProcessor();
                        ranker = new DocumentRanker();
                        ranker.rankingPreProcess(preProcess, queryXml, stopList);
                    }
                    checkContinueMenu("displayRankMenu", "Continue in ranking menu (y/n):");
                }
                break;

            case "3":
                checkContinueMenu("displayReadMenu", "Continue in extract information (y/n):");
                break;

            case "4":
                return true;

            default:
                System.out.println("Incorrect input");
        }
        return false;
    }


    /**
     * Process user choice from rank menu
     * @param scoringFunction
     *        1, 2, 3, 4, 5 : Available scoring functions
     *        6 : Exit
     * @return boolean indicating whether to continue in menu
     */
    public static boolean processRankMenuChoice (final String scoringFunction) {

        switch (scoringFunction) {
            case "1":
            case "2":
            case "3":
            case "4":
            case "5":
                System.out.println(Constants.newline + "Please enter output file name");
                final String outputFileName = new Scanner(System.in).next();
                ranker.rankDocuments(scoringFunction, outputFileName);
                break;

            case "6":
                return true;

            default:
                System.out.println(Constants.newline + "Incorrect input");
        }
        return false;
    }


    /**
     * Process user choice from rank menu
     * @param scoringFunction
     *        1 : Get term information
     *        2 : Get document information
     *        3 : Get term information in document
     *        4 : Exit
     * @return boolean indicating whether to continue in menu
     */
    public static boolean processReadMenuChoice (final String choice) {

        final File documentIdFile = new File(propertyKeyToFileLocation.get("DOCUMENT_ID_FILE")).getAbsoluteFile();
        final File termIdFile = new File(propertyKeyToFileLocation.get("TERMS_ID_FILE")).getAbsoluteFile();
        final File documentIndexFile = new File(propertyKeyToFileLocation.get("DOCUMENT_INDEX_FILE")).getAbsoluteFile();
        final File termIndexFile = new File(propertyKeyToFileLocation.get("TERM_INDEX_FILE")).getAbsoluteFile();
        final File termInfoFile = new File(propertyKeyToFileLocation.get("TERM_INFO_FILE")).getAbsoluteFile();

        switch (choice) {
            case "1":
                System.out.println(Constants.newline + "Enter Term");
                ExtractInfo.getTermMetadata(new Scanner(System.in).next(), termIdFile, termInfoFile);
                break;

            case "2":
                System.out.println(Constants.newline + "Enter document name");
                ExtractInfo.getDocumentMetadata(new Scanner(System.in).next(), documentIdFile, documentIndexFile);
                break;

            case "3":
                final Scanner input = new Scanner(System.in);
                System.out.println(Constants.newline + "Enter Term");
                final String term = input.next();
                System.out.println("Enter Document name");
                final String doc = input.next();

                ExtractInfo.getTermMetadataWithinDocument(term, doc, documentIdFile, termIdFile, termIndexFile,
                        termInfoFile);
                break;

            case "4":
                return true;

            default:
                System.out.println(Constants.newline + "Incorrect input");
        }
        return false;
    }
}
