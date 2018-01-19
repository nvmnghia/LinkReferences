package inMemory;

import bitap.Bitap;
import data.Article;
import data.References;
import util.StringUtl;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class ReferenceMatcher {
    private static final int NUM_OF_THREAD = Runtime.getRuntime().availableProcessors();
    static {
        try {
            Bitap.class.newInstance();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    private static ArrayList<Article> articles;

    // temp array to store results
    private static String[] tempResults = new String[NUM_OF_THREAD];
    private static int[] tempMatchCounters = new int[NUM_OF_THREAD];
    private static int[] tempArticlesHaveMatch = new int[NUM_OF_THREAD];

    public static void main(String[] args) throws SQLException, IOException {
        long start = System.currentTimeMillis();

        // Connect to the DB
        Connection connection = null;
        try {
            Class.forName("com.mysql.jdbc.Driver").newInstance();
            connection = DriverManager.getConnection("jdbc:mysql://localhost/test?user=root&password=");
        } catch (Exception ex) {
            // whoohoo what a broken implementation
        }

        Statement statement = connection.createStatement();
        statement.executeQuery("USE vci_scholar");

        ResultSet articleSet = statement.executeQuery("SELECT id, title, reference FROM articles LIMIT 12345");
        articleSet.last();
        articles = new ArrayList<>(articleSet.getRow());
        articleSet.beforeFirst();

        // Read the received ResultSet
        while (articleSet.next()) {
            String rawReference = articleSet.getString(3);

            // Only create an Article if the raw reference string is valuable
            if (rawReference != null && rawReference.length() != 0) {
                Article article = new Article();

                article.setId(articleSet.getInt(1));
                article.setTitle(StringUtl.clean(articleSet.getString(2)));
                article.setReferences(new References(StringUtl.clean(rawReference)));

                articles.add(article);
            }
        }

        // Match
        ExecutorService refMatchers = Executors.newFixedThreadPool(NUM_OF_THREAD);
        int size = articles.size() / NUM_OF_THREAD, startIdx, endIdx;
        for (int i = 0; i < NUM_OF_THREAD; i++) {
            startIdx = i * size;
            endIdx = startIdx + size;
            endIdx = endIdx > articles.size() ? articles.size() : endIdx;
            refMatchers.execute(new RefMatcher(startIdx, endIdx));
        }

        // (Wait) Till the end
        refMatchers.shutdown();
        try {
            refMatchers.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Print the result
        StringBuilder results = new StringBuilder("");
        int matchCounter = 0, articlesHaveMatch = 0;
        for (int i = 0; i < NUM_OF_THREAD; ++i) {
            results.append(tempResults[i]);
            matchCounter += tempMatchCounters[i];
            articlesHaveMatch += tempArticlesHaveMatch[i];
        }

        long end = System.currentTimeMillis();
        System.out.println(end - start);

        BufferedWriter writer = new BufferedWriter(new FileWriter(new File("results.txt")));
        writer.write(results.toString());
        writer.write("\nMatched references: " + matchCounter + "    Articles have match: " + articlesHaveMatch + "    Processing time: " + (end - start) + " ms    Thread: " + NUM_OF_THREAD);
        writer.close();
    }

    public static int numOfRecords(Statement statement) throws SQLException {
        ResultSet numOfRecords = statement.executeQuery("SELECT COUNT(*) FROM articles");
        numOfRecords.next();
        return numOfRecords.getInt(1);
    }

    public static void testMaxAllowedPacket(Statement statement) throws SQLException {
        ResultSet test = statement.executeQuery("SHOW VARIABLES");
        while (test.next()) {
            if (test.getString(1).equals("max_allowed_packet")) {
                System.out.println(test.getInt(2));
                break;
            }
        }
    }

    /**
     * Iterate through a predefined range of the articles
     * In each iteration, get its raw reference string, iterate over all the articles
     * to find titles which are "contained" in the raw reference
     * Just a crappy O(n^2) search
     *
     * The RefMatcher uses Bitap algorithm which enable fuzzy matching
     * String is marked as "contained" if it differs less than 10% to the most similar substring in the text
     * See Bitap class for more detail
     */
    public static class RefMatcher implements Runnable {
        // The start and end indices of the search
        private int start;
        private int end;

        public RefMatcher(int start, int end) {
            this.start = start;
            this.end = end;
        }

        @Override
        public void run() {
            int matchCounter = 0;
            int articleHasMatch = 0;
            StringBuilder result = new StringBuilder("");

            for (int i = start; i < end; ++i) {
                Article article = articles.get(i);
                boolean noCitFound = true;
                String rawCitation = article.getReferences().getRaw();

                for (Article citedArticle : articles) {
                    // If 90% match
                    if (Bitap.fuzzyContains(rawCitation, citedArticle.getTitle(), citedArticle.getTitle().length() / 10)) {
                        if (noCitFound) {
                            noCitFound = false;
                            result.append(article.getTitle()).append(":\n").append(rawCitation).append("\n\n");
                        }

                        article.getReferences().addArticleID(citedArticle.getId());
                        result.append("    ").append(citedArticle.getTitle()).append('\n');
                        ++matchCounter;
                    }
                }

                if (!noCitFound) {
                    result.append("\n\n\n");
                    ++articleHasMatch;
                }
            }

            // Write result to temp arrays
            int currentThreadIndex = (int) Thread.currentThread().getId() % NUM_OF_THREAD;
            tempResults[currentThreadIndex] = result.toString();
            tempMatchCounters[currentThreadIndex] = matchCounter;
            tempArticlesHaveMatch[currentThreadIndex] = articleHasMatch;
        }
    }
}
