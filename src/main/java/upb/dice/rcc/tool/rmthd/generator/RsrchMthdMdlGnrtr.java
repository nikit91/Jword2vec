package upb.dice.rcc.tool.rmthd.generator;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.aksw.word2vecrestful.utils.Cfg;
import org.aksw.word2vecrestful.word2vec.Word2VecFactory;
import org.aksw.word2vecrestful.word2vec.Word2VecModel;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import upb.dice.rcc.tool.RccUtil;
import upb.dice.rcc.tool.gnrtr.RccModelGenerator;

public class RsrchMthdMdlGnrtr extends RccModelGenerator {

	public static Logger LOG = LogManager.getLogger(RsrchMthdMdlGnrtr.class);

	public static final String[] FLDS_TO_READ = { "skos:definition", "skos:prefLabel" };
	public static final String ID_FLD = "@id";
	public static final String CNTNT_ARR_FLD = "@graph";
	public static final String VALUE_FLD = "@value";
	public static final String WHITESPACE_REGEX = "\\s";
	public static final ObjectMapper OBJ_MAPPER = new ObjectMapper();
	public static final ObjectReader OBJ_READER = OBJ_MAPPER.reader();

	public RsrchMthdMdlGnrtr(Word2VecModel w2vModel) {
		super(w2vModel);
	}

	/**
	 * Method to map the research method ids to the word tokens in it (Takes a json
	 * node as input) (converts all words to lowercase)
	 * 
	 * @param subFldName  - name/label of the research field
	 * @param subFieldArr - json array of the related entries
	 * @param mainFldName - name/label of the main field
	 */
	private void mapWordsToField(JsonNode rsrchMthd) {
		List<String> wordList = new ArrayList<>();
		wrdsIdMap.put(rsrchMthd.get(ID_FLD).asText(), wordList);
		// for each useful field in the entry
		for (String fld : FLDS_TO_READ) {
			JsonNode subNode = rsrchMthd.get(fld);
			if (subNode != null) {
				String line = subNode.get(VALUE_FLD).asText();
				wordList.addAll(RccUtil.fetchAllWordTokens(line, w2vModel));
			}
		}

	}

	/**
	 * Method to read the json research method file and extract words from each
	 * method
	 * 
	 * @param inputFile - file object of the input json file
	 * @throws IOException
	 */
	protected void loadWordIdMap(File inputFile) throws IOException {
		FileInputStream fin = null;
		try {
			fin = new FileInputStream(inputFile);
			// Read file into a json
			ObjectNode inpObj = (ObjectNode) OBJ_READER.readTree(fin);
			ArrayNode methodArr = (ArrayNode) inpObj.get(CNTNT_ARR_FLD);
			Iterator<JsonNode> methodItr = methodArr.iterator();
			while (methodItr.hasNext()) {
				JsonNode methodEle = methodItr.next();
				mapWordsToField(methodEle);
			}
		} finally {
			fin.close();
		}
	}

	/**
	 * Method to demonstrate example usage
	 * 
	 * @param args
	 * @throws JsonProcessingException
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {
		// initialize logger
		PropertyConfigurator.configure(Cfg.LOG_FILE);
		// init w2v model
		Word2VecModel genModel = Word2VecFactory.get();
		RccModelGenerator generator = new RsrchMthdMdlGnrtr(genModel);
		String inputFilePath = "data/rcc/train_test/sage_research_methods.json";
		String outputFilePath = Cfg.get("org.aksw.word2vecrestful.word2vec.nrmlrsrchmthdbinmodel.model");

		File inputFile = new File(inputFilePath);
		File outputFile = new File(outputFilePath);

		generator.generateResearchFieldsModel(inputFile, outputFile);

	}

}
