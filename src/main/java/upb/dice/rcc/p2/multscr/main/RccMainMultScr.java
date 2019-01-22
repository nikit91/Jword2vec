package upb.dice.rcc.p2.multscr.main;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.aksw.word2vecrestful.utils.Cfg;
import org.aksw.word2vecrestful.utils.TimeLogger;
import org.aksw.word2vecrestful.word2vec.GenWord2VecModel;
import org.aksw.word2vecrestful.word2vec.W2VNrmlMemModelBinSrch;
import org.aksw.word2vecrestful.word2vec.Word2VecFactory;
import org.aksw.word2vecrestful.word2vec.Word2VecModel;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.opencsv.CSVReader;

import upb.dice.rcc.multscr.finder.RccFinderMult;
import upb.dice.rcc.multscr.finder.RfmSnglrCosSim;
import upb.dice.rcc.multscr.finder.RfmTopCosSimSum;
import upb.dice.rcc.tool.PublicationWordSetExtractor;
import upb.dice.rcc.tool.RccNounPhraseLabelPair;
import upb.dice.rcc.tool.rfld.generator.RsrchFldMdlGnrtrCsv;
import upb.dice.rcc.tool.rmthd.generator.DbpFrmtRsrchMthdMdlGnrtr;

/**
 * Class to help execute the RccFinder to find research fields and methods for
 * the publications in a given directory
 * 
 * @author nikitsrivastava
 *
 */
public class RccMainMultScr {
	/**
	 * Logger instance
	 */
	public static Logger LOG = LogManager.getLogger(RccMainMultScr.class);
	/**
	 * Time logger instance
	 */
	public static final TimeLogger TLOG = new TimeLogger();
	/**
	 * Index of 'id' field in a research field dataset file
	 */
	public static int idIndx = RsrchFldMdlGnrtrCsv.DEFAULT_ID_INDX;
	/**
	 * Index of 'label' field in a research field dataset file
	 */
	public static int resColIndx = 4;
	/**
	 * Label of the container element for Research Method label
	 */
	public static final String LABEL_FLD = "label";
	/**
	 * Object Mapper Instance for Json
	 */
	public static final ObjectMapper OBJ_MAPPER = new ObjectMapper();
	/**
	 * Object Reader Instance for Json
	 */
	public static final ObjectReader OBJ_READER = OBJ_MAPPER.reader();
	/**
	 * Object Writer Instance for Json
	 */
	public static final ObjectWriter OBJ_WRITER = OBJ_MAPPER.writer(new DefaultPrettyPrinter());
	/**
	 * Node factory Instance for Json
	 */
	public static final JsonNodeFactory JSON_NODE_FACTORY = OBJ_MAPPER.getNodeFactory();

	static {
		// initialize logger
		PropertyConfigurator.configure(Cfg.LOG_FILE);
	}
	/**
	 * Research field in-memory word2vec model
	 */
	protected GenWord2VecModel rFldMemModel;
	/**
	 * Research method in-memory word2vec model
	 */
	protected GenWord2VecModel rMthdMemModel;
	/**
	 * General in-memory word2vec model
	 */
	protected Word2VecModel genModel;
	/**
	 * Publication word set extractor instance
	 */
	protected PublicationWordSetExtractor wordSetExtractor;
	/**
	 * Research field id to label map
	 */
	protected Map<String, String> rFldLblMap;
	/**
	 * Research method id to label map
	 */
	protected Map<String, String> rMthdLblMap;
	/**
	 * Research field finder instance
	 */
	protected RccFinderMult rFldFinder;
	/**
	 * Research method finder instance
	 */
	protected RccFinderMult rMthdFinder;
	/**
	 * Intermediate research field results arraynode
	 */
	protected ArrayNode rFldNodes = new ArrayNode(JSON_NODE_FACTORY);
	/**
	 * Intermediate research method results arraynode
	 */
	protected ArrayNode rMthdNodes = new ArrayNode(JSON_NODE_FACTORY);

	/**
	 * Research field output file
	 */
	protected File rFldOutputFile;
	/**
	 * Research method output file
	 */
	protected File rMthdOutputFile;

	protected RccMainMultScr() {
	}

	public RccMainMultScr(File rFldInputFile, File rMthdInputFile, File rFldOutputFile, File rMthdOutputFile)
			throws IOException {
		this.initMemModels();
		this.rFldLblMap = getRfldLblMap(rFldInputFile);
		this.rMthdLblMap = getRmthdLblMap(rMthdInputFile);
		this.rFldOutputFile = rFldOutputFile;
		this.rMthdOutputFile = rMthdOutputFile;
	}

	/**
	 * Method to demonstrate example usage
	 * 
	 * @param args
	 * @throws JsonProcessingException
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public static void main(String[] args) {
		// Publications directory
		String pubDirPath = args[0];
		// Research field output file path
		String rFldOutputFilePath = args[1];
		// Research method output file path
		String rMthdOutputFilePath = args[2];
		// Research field input file path
		String rFldInputFilePath = args[3];
		// Research method input file path
		String rMthdInputFilePath = args[4];

		File rFldInputFile = new File(rFldInputFilePath);
		File rMthdInputFile = new File(rMthdInputFilePath);
		// Research field output file
		File rFldOutputFile = new File(rFldOutputFilePath);
		// Research method output file
		File rMthdOutputFile = new File(rMthdOutputFilePath);
		try {
			// init the main
			RccMainMultScr rccMain = new RccMainMultScr(rFldInputFile, rMthdInputFile, rFldOutputFile, rMthdOutputFile);
			rccMain.processEntries(pubDirPath);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Method to process the entries in the given publication folder to identify the
	 * research methods and research fields
	 * 
	 * @param pubDirPath - the given publication files folder
	 * @throws Exception
	 */
	public void processEntries(String pubDirPath) throws Exception {
		TLOG.logTime(1);
		// fetch closest entries
		this.fetchClosestEntries(pubDirPath);
		// write the closest entries to file
		this.writeClosestEntries(rFldOutputFile, rMthdOutputFile);
		TLOG.printTime(1, "Total Fetch time (Methods and Fields)");
	}

	/**
	 * Method to write the closest research field and research method json results
	 * into the files
	 * 
	 * @param rFldOutputFile  - research field result output file
	 * @param rMthdOutputFile - research method result output file
	 * @throws JsonGenerationException
	 * @throws JsonMappingException
	 * @throws IOException
	 */
	protected void writeClosestEntries(File rFldOutputFile, File rMthdOutputFile)
			throws JsonGenerationException, JsonMappingException, IOException {
		// write research fields
		writeJsonToFile(rFldNodes, rFldOutputFile);
		// write research methods
		writeJsonToFile(rMthdNodes, rMthdOutputFile);
	}

	/**
	 * Method to write a json object to a file
	 * 
	 * @param node       - json object to be written
	 * @param outputFile - file to write json in
	 * @throws JsonGenerationException
	 * @throws JsonMappingException
	 * @throws IOException
	 */
	protected void writeJsonToFile(JsonNode node, File outputFile)
			throws JsonGenerationException, JsonMappingException, IOException {
		// ensure directory creation
		outputFile.getParentFile().mkdirs();
		OBJ_WRITER.writeValue(outputFile, node);
	}

	/**
	 * Method to initialize the word2vec models
	 * 
	 * @throws IOException
	 */
	public void initMemModels() throws IOException {
		// General Non-Normalized Word2Vec Model
		genModel = Word2VecFactory.get();
		// init wordsetextractor
		wordSetExtractor = new PublicationWordSetExtractor(genModel);
		// Normalized ResearchField Word2VecModel
		Word2VecModel nrmlRsrchFldMdl = Word2VecFactory.getNrmlRsrchFldModel();
		// Normalized ResearchMethod Word2VecModel
		Word2VecModel nrmlRsrchMthdMdl = Word2VecFactory.getNrmlStstclMthdModel();

		rFldMemModel = new W2VNrmlMemModelBinSrch(nrmlRsrchFldMdl.word2vec, nrmlRsrchFldMdl.vectorSize);
		rFldMemModel.process();

		rMthdMemModel = new W2VNrmlMemModelBinSrch(nrmlRsrchMthdMdl.word2vec, nrmlRsrchMthdMdl.vectorSize);
		rMthdMemModel.process();

		this.rFldFinder = new RfmTopCosSimSum(genModel, rFldMemModel, wordSetExtractor);
		// Manual weight map
		Map<String, Float> weightMap = new HashMap<String, Float>();
		weightMap.put("methodology", 1.2f);
		this.rMthdFinder = new RfmSnglrCosSim(genModel, rMthdMemModel, wordSetExtractor, weightMap);
	}

	/**
	 * Method to construct a mapping between research field id and corresponding
	 * labels
	 * 
	 * @param inputFile - research field file
	 * @return - Mapping between id and its label
	 * @throws IOException
	 */
	public static Map<String, String> getRfldLblMap(File inputFile) throws IOException {
		Map<String, String> resMap = new HashMap<String, String>();
		CSVReader csvReader = null;
		try {
			csvReader = new CSVReader(new FileReader(inputFile));
			// Reading header
			csvReader.readNext();
			String[] lineArr;
			// Read the csv file line by line
			while ((lineArr = csvReader.readNext()) != null) {
				String lineId = lineArr[idIndx];
				String lineLbl = lineArr[resColIndx];
				resMap.put(lineId, lineLbl);
			}

		} finally {
			csvReader.close();
		}
		return resMap;
	}

	/**
	 * Method to construct a mapping between research method id and corresponding
	 * labels
	 * 
	 * @param inputFile - research method file
	 * @return - Mapping between id and its label
	 * @throws IOException
	 */
	public static Map<String, String> getRmthdLblMap(File inputFile) throws IOException {
		Map<String, String> resMap = new HashMap<String, String>();
		FileInputStream fin = null;
		try {
			fin = new FileInputStream(inputFile);
			// Read file into a json
			ObjectNode inpObj = (ObjectNode) OBJ_READER.readTree(fin);
			
			Iterator<JsonNode> categoryItr = inpObj.iterator();
			while (categoryItr.hasNext()) {
				ArrayNode methodsArr = (ArrayNode) categoryItr.next();
				Iterator<JsonNode> methodItr = methodsArr.iterator();
				while (methodItr.hasNext()) {
					JsonNode rsrchMthd = methodItr.next();
					String lineId = rsrchMthd.get(DbpFrmtRsrchMthdMdlGnrtr.ID_FLD).asText();
					String lineLbl = rsrchMthd.get(LABEL_FLD).asText();
					resMap.put(lineId, lineLbl);
				}
			}
		} finally {
			fin.close();
		}
		return resMap;
	}

	/**
	 * Method to save a research field entry
	 * 
	 * @param fileName  - name of the file
	 * @param labelPair - {@link RccNounPhraseLabelPair} instance
	 */
	protected void saveRfldEntry(String fileName, List<RccNounPhraseLabelPair> labelPairList) {
		ObjectNode fldNode = getEntryNode(fileName, labelPairList, rFldLblMap, "field");
		rFldNodes.add(fldNode);
	}

	/**
	 * Method to save a research method entry
	 * 
	 * @param fileName  - name of the file
	 * @param labelPair - {@link RccNounPhraseLabelPair} instance
	 */
	protected void saveRmthdEntry(String fileName, List<RccNounPhraseLabelPair> labelPairList) {
		ObjectNode mthdNode = getEntryNode(fileName, labelPairList, rMthdLblMap, "method");
		rMthdNodes.add(mthdNode);
	}

	/**
	 * Method to generate a result json node for the given information
	 * 
	 * @param fileName  - name of the file
	 * @param labelPair - {@link RccNounPhraseLabelPair} instance
	 * @param idMap     - id to label mapping
	 * @return - json node enclosing the information passed
	 */
	protected ObjectNode getEntryNode(String fileName, List<RccNounPhraseLabelPair> labelPairList,
			Map<String, String> idMap, String prefix) {
		ArrayNode arrayNode = JSON_NODE_FACTORY.arrayNode();
		ObjectNode entryNode = JSON_NODE_FACTORY.objectNode();
		for (RccNounPhraseLabelPair labelPair : labelPairList) {
			String id = labelPair.getClosestWord();
			String fldLabel = idMap.get(id);
			double score = labelPair.getCosineSim();

			ObjectNode fldNode = JSON_NODE_FACTORY.objectNode();
			fldNode.put(prefix + "Id", id);
			fldNode.put(prefix + "Label", fldLabel);
			fldNode.put("score", score);
			arrayNode.add(fldNode);
		}
		entryNode.put("fileName", fileName);
		entryNode.set("result", arrayNode);
		return entryNode;
	}

	/**
	 * Method to read all the publication files in a given directory and save the
	 * research field and method associated with them
	 * 
	 * @param pubDirPath - path to the publications directory
	 * @throws IOException
	 */
	protected void fetchClosestEntries(String pubDirPath) throws IOException {

		File pubFileDir = new File(pubDirPath);
		for (final File fileEntry : pubFileDir.listFiles()) {
			// Closest Research Field
			List<RccNounPhraseLabelPair> rFldPair = this.rFldFinder.findClosestResearchField(fileEntry);
			// Closest Research Method
			List<RccNounPhraseLabelPair> rMthdPair = this.rMthdFinder.findClosestResearchField(fileEntry);
			// Save research field
			this.saveRfldEntry(fileEntry.getName(), rFldPair);
			// Save research method
			this.saveRmthdEntry(fileEntry.getName(), rMthdPair);
		}

	}

}
