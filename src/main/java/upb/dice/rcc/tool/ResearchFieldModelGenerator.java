package upb.dice.rcc.tool;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.aksw.word2vecrestful.tool.ModelNormalizer;
import org.aksw.word2vecrestful.utils.Cfg;
import org.aksw.word2vecrestful.word2vec.Word2VecFactory;
import org.aksw.word2vecrestful.word2vec.Word2VecModel;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Utility class to generate vector model for research fields in a given json
 * file input json file format:
 * 
 * <pre>
 *{
 * "main_field_name1": {
 *   "research_field_name1" : [ {
 *   "fieldAltLabel" : "...",
 *   "fieldId" : "...",
 *   "fieldLabel" : "..."
 *  }...],
 *  "research_field_name2" : [ { ... } ] ...
 * }
 * "main_field_name2": { ... } ...
 *}
 * </pre>
 * <dl>
 * <dt>fieldAltLabel</dt>
 * <dd>Alternative label for the field</dd>
 * <dt>fieldId</dt>
 * <dd>id of the field</dd>
 * <dt>fieldLabel</dt>
 * <dd>label for the field</dd>
 * </dl>
 * 
 * @author nikitsrivastava
 *
 */
public class ResearchFieldModelGenerator {

	public static Logger LOG = LogManager.getLogger(ResearchFieldModelGenerator.class);

	private Map<String, Set<String>> rsrchFldWrdsMap = new HashMap<>();

	public static final String[] FLDS_TO_READ = { "fieldAltLabel", "fieldLabel" };
	public static final String WHITESPACE_REGEX = "\\s";
	public static final ObjectMapper OBJ_MAPPER = new ObjectMapper();
	public static final ObjectReader OBJ_READER = OBJ_MAPPER.reader();

	private Word2VecModel w2vModel;
	private Set<String> stopWords = new HashSet<>();

	public ResearchFieldModelGenerator(Word2VecModel w2vModel) {
		this.w2vModel = w2vModel;
	}

	/**
	 * Method to map the research fields to the unique words in it (Takes a json
	 * node as input) (converts all words to lowercase)
	 * 
	 * @param subFldName  - name/label of the research field
	 * @param subFieldArr - json array of the related entries
	 * @param mainFldName - name/label of the main field
	 */
	private void mapWordsToField(String subFldName, ArrayNode subFieldArr, String mainFldName) {
		Set<String> wordSet = new HashSet<>();
		rsrchFldWrdsMap.put(subFldName, wordSet);
		wordSet.addAll(fetchAllWordTokens(mainFldName));
		wordSet.addAll(fetchAllWordTokens(subFldName));
		// for each entry in sub field
		Iterator<JsonNode> inpIt = subFieldArr.iterator();
		while (inpIt.hasNext()) {
			JsonNode node = inpIt.next();
			// for each useful field in the entry
			for (String fld : FLDS_TO_READ) {
				wordSet.addAll(fetchAllWordTokens(node.get(fld).asText()));
			}

		}
	}

	/**
	 * Method to extract the words from a line of text after sanitization
	 * 
	 * @param line - line to sanitize
	 * @return extracted lowercased words from the line
	 */
	private Set<String> fetchAllWordTokens(String line) {
		// change to lower case
		line = line.toLowerCase();
		// init wordset
		Set<String> wordSet = new HashSet<>();
		// remove special characters
		String sanStr = line.replaceAll("[^\\w\\s-\\d]", " ");
		// replace - with _
		sanStr = sanStr.replaceAll("-", "_");
		// for each word in the field, add the lowercase string to the set
		for (String wordEntry : sanStr.split(WHITESPACE_REGEX)) {
			if (wordEntry.isEmpty()) {
				continue;
			}
			// if the word has _ and does not exist in the model then break it up before
			// adding
			if (wordEntry.matches(".*_.*")) {
				if (w2vModel.word2vec.get(wordEntry) != null) {
					// put the word in wordset
					wordSet.add(wordEntry);
				} else {
					// split the word and then put it
					String[] splitWords = wordEntry.split("_");
					wordSet.addAll(Arrays.asList(splitWords));
				}
			} else {
				wordSet.add(wordEntry);
			}

		}
		// remove stop words
		wordSet.removeAll(stopWords);
		return wordSet;
	}

	/**
	 * Method to return the sum vector of all the words passed as a set, vectors are
	 * calculated using the given model
	 * 
	 * @param wordSet - word set to sum all the vectors for
	 * @return - the sum of all the word vectors
	 */
	private float[] getSumVector(Set<String> wordSet) {
		float[] finalVec = new float[w2vModel.vectorSize];
		for (String wordEntry : wordSet) {
			float[] wordVec = w2vModel.word2vec.get(wordEntry);
			if (wordVec == null) {
				LOG.info("No vector found for the word: " + wordEntry);
			} else {
				for (int i = 0; i < w2vModel.vectorSize; i++) {
					finalVec[i] += wordVec[i];
				}
			}
		}
		return finalVec;
	}

	/**
	 * Method to read the json research field file and extract each subfield's node
	 * 
	 * @param inputFile - file object of the input json fiel
	 * @throws IOException
	 */
	private void readResearchFields(File inputFile) throws IOException {
		FileInputStream fin = null;
		try {
			fin = new FileInputStream(inputFile);
			// Read file into a json
			ObjectNode inpObj = (ObjectNode) OBJ_READER.readTree(fin);
			Iterator<Entry<String, JsonNode>> mainFldIt = inpObj.fields();
			while (mainFldIt.hasNext()) {
				Entry<String, JsonNode> mainEntry = mainFldIt.next();
				String mainFldName = mainEntry.getKey();
				JsonNode rsrchFldEntries = mainEntry.getValue();
				Iterator<Entry<String, JsonNode>> rsrchFldIt = rsrchFldEntries.fields();
				while (rsrchFldIt.hasNext()) {
					Entry<String, JsonNode> rsrchEntry = rsrchFldIt.next();
					String rsrchFldName = rsrchEntry.getKey();
					ArrayNode rsrchFldLblEntries = (ArrayNode) rsrchEntry.getValue();
					// Map the words
					mapWordsToField(rsrchFldName, rsrchFldLblEntries, mainFldName);
				}
			}
		} finally {
			fin.close();
		}
	}

	/**
	 * Method to generate a research field model for the given input research filed
	 * json file
	 * 
	 * @param inputFile    - input json file
	 * @param outputFile   - output bin model file
	 * @param stopwordFile - file containing stopwords to avoid
	 * @throws IOException
	 */
	public void generateResearchFieldsModel(File inputFile, File outputFile, File stopwordFile) throws IOException {
		// read stop words
		readStopWords(stopwordFile);
		// ensure directory creation
		outputFile.getParentFile().mkdirs();
		// declare the output stream
		BufferedOutputStream bOutStrm = null;
		try {
			// read and load the input file onto memory
			readResearchFields(inputFile);

			Integer totWords = rsrchFldWrdsMap.size();
			Integer vecSize = w2vModel.vectorSize;
			int w = 0;
			LOG.info("Writing " + totWords + " research fields with " + vecSize + " values per vector.");
			// initiate the output stream
			bOutStrm = new BufferedOutputStream(new FileOutputStream(outputFile));

			// write the first line
			bOutStrm.write(totWords.toString().getBytes(StandardCharsets.UTF_8));
			bOutStrm.write(ModelNormalizer.WHITESPACE_BA);
			bOutStrm.write(vecSize.toString().getBytes(StandardCharsets.UTF_8));
			bOutStrm.write(ModelNormalizer.END_LINE_BA);

			// for each field in the map
			for (String rsrchfld : rsrchFldWrdsMap.keySet()) {
				Set<String> wordSet = rsrchFldWrdsMap.get(rsrchfld);
				// calculate the sum vector for the field
				float[] sumVec = getSumVector(wordSet);
				// get the byte array for the normalized sum vector
				byte[] bSumVec = ModelNormalizer.getNormalizedVecBA(sumVec);
				// write the research field
				bOutStrm.write(rsrchfld.getBytes(StandardCharsets.UTF_8));
				// write whitespace
				bOutStrm.write(ModelNormalizer.WHITESPACE_BA);
				// write the vector
				bOutStrm.write(bSumVec);

				if ((w + 1) % 10000 == 0) {
					bOutStrm.flush();
					LOG.info((w + 1) + " Records inserted.");
				}
				w++;
			}
			// close the output writer
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			bOutStrm.close();
		}

	}

	/**
	 * Method to read stopwords
	 * 
	 * @param stopWordsFile - file to read stopwords from
	 * @throws IOException
	 */
	private void readStopWords(File stopWordsFile) throws IOException {
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
					stopWords.add(line);
				}
			}
		} finally {
			br.close();
		}
	}

	public static void main(String[] args) throws IOException {
		// initialize logger
		PropertyConfigurator.configure(Cfg.LOG_FILE);
		// init w2v model
		Word2VecModel genModel = Word2VecFactory.get();
		ResearchFieldModelGenerator generator = new ResearchFieldModelGenerator(genModel);
		String inputFilePath = "data/rcc/train_test/sage_research_fields.json";
		String outputFilePath = "data/rcc/ResearchFields_NormalizedModel.bin";
		String stopwordsFilePath = "data/rcc/english.stopwords.txt";

		File inputFile = new File(inputFilePath);
		File outputFile = new File(outputFilePath);
		File stopwordFile = new File(stopwordsFilePath);

		generator.generateResearchFieldsModel(inputFile, outputFile, stopwordFile);
	}

}
