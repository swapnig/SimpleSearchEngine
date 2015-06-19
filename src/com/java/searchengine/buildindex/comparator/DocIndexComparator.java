package com.java.searchengine.buildindex.comparator;

import java.util.Comparator;

import com.java.searchengine.constants.Constants;

/**
 * 
 * @author Swapnil Gupta
 * @purpose Comparator used for comparison in external sort
 *          It first sorts on basis of term id and then sorts on basis of document id
 *
 */
public class DocIndexComparator implements Comparator<String> {

    @Override
    public int compare (String r1, String r2) {

        String[] line1 = r1.split(Constants.tab);
        String[] line2 = r2.split(Constants.tab);

        int termId1 = Integer.parseInt(line1[1]);
        int termId2 = Integer.parseInt(line2[1]);

        if (termId1 > termId2) {
            return 1;
        } else
            if (termId1 < termId2) {
                return -1;
            } else {
                int docId1 = Integer.parseInt(line1[0]);
                int docId2 = Integer.parseInt(line2[0]);
                if (docId1 > docId2) {
                    return 1;
                } else {
                    return -1;
                }
            }
    }
}
