package upb.dice.rcc.tool;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.aksw.word2vecrestful.word2vec.Word2VecModel;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

/**
 * Class to encapsulate all the utility methods used for Rich Context Challenge
 * 
 * @author nikitsrivastava
 *
 */
public class RccUtil {

	public static Logger LOG = LogManager.getLogger(RccUtil.class);

	public static final String WHITESPACE_REGEX = "\\s";
	public static final String JOINEDWORD_REGEX = ".*_.*";
	public static final String ALPHANUMERIC_REGEX = "[^\\w\\s-\\d]";
	public static final String WHITESPACE = " ";
	public static final String HIPHEN = "-";
	public static final String UNDERSCORE = "_";

	public static final Set<String> STOPWORDS = new HashSet<>();

	static {
		// Load default stop words
		String stopwordsFilePath = "data/rcc/english.stopwords.txt";
		File stopWordsFile = new File(stopwordsFilePath);
		try {
			STOPWORDS.addAll(readStopWords(stopWordsFile));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Method to read and return the set of stop-words from a given file
	 * 
	 * @param stopWordsFile - file to read stop-words from
	 * @return - set to stop words extracted from the file
	 * @throws IOException
	 */
	public static Set<String> readStopWords(File stopWordsFile) throws IOException {
		Set<String> stopWordSet = new HashSet<>();
		FileInputStream fin = null;
		BufferedReader br = null;
		try {
			fin = new FileInputStream(stopWordsFile);
			// Read file into a json
			br = new BufferedReader(new InputStreamReader(fin));
			String line;
			while ((line = br.readLine()) != null) {
				line = line.trim();
				if (line.length() > 0) {
					stopWordSet.add(line);
				}
			}
		} finally {
			br.close();
		}
		return stopWordSet;
	}

	/**
	 * Method to return the sum vector of all the words passed as a collection,
	 * vectors are calculated using the given model
	 * 
	 * @param wordCollection - word collection to sum all the vectors for
	 * @return - the sum of all the word vectors
	 */
	public static float[] getSumVector(Collection<String> wordCollection, Word2VecModel w2vModel) {
		float[] finalVec = new float[w2vModel.vectorSize];
		int validWordCount = 0;
		for (String wordEntry : wordCollection) {
			float[] wordVec = w2vModel.word2vec.get(wordEntry);
			if (wordVec == null) {
				LOG.info("No vector found for the word: " + wordEntry);
			} else {
				validWordCount++;
				for (int i = 0; i < w2vModel.vectorSize; i++) {
					finalVec[i] += wordVec[i];
				}
			}
		}
		if(validWordCount==0) {
			finalVec = null;
		}
		return finalVec;
	}

	/**
	 * Method to extract the words from a line of text after sanitization
	 * 
	 * @param line - line to sanitize
	 * @return extracted lowercased words from the line
	 */
	public static List<String> fetchAllWordTokens(String line, Word2VecModel w2vModel) {
		return fetchAllWordTokens(line, w2vModel, STOPWORDS);
	}

	/**
	 * Method to extract the words from a line of text after sanitization
	 * 
	 * @param line - line to sanitize
	 * @return extracted lowercased words from the line
	 */
	public static List<String> fetchAllWordTokens(String line, Word2VecModel w2vModel, Set<String> stopWords) {
		// change to lower case
		line = line.toLowerCase();
		// init wordset
		List<String> wordList = new ArrayList<>();
		// check if only one word is present
		if (line.matches("[\\w\\d]+")) {
			wordList.add(line);
		} else {
			// remove special characters
			String sanStr = line.replaceAll(ALPHANUMERIC_REGEX, WHITESPACE);
			// replace - with _
			sanStr = sanStr.replaceAll(HIPHEN, UNDERSCORE);
			// for each word in the field, add the lowercase string to the set
			for (String wordEntry : sanStr.split(WHITESPACE_REGEX)) {
				if (wordEntry.isEmpty()) {
					continue;
				}
				// if the word has _ and does not exist in the model then break it up before
				// adding
				if (wordEntry.matches(JOINEDWORD_REGEX)) {
					if (w2vModel.word2vec.get(wordEntry) != null) {
						// put the word in wordset
						wordList.add(wordEntry);
					} else {
						// split the word and then put it
						String[] splitWords = wordEntry.split(UNDERSCORE);
						wordList.addAll(Arrays.asList(splitWords));
					}
				} else {
					wordList.add(wordEntry);
				}

			}
		}
		// remove stop words
		wordList.removeAll(stopWords);
		return wordList;
	}

}
