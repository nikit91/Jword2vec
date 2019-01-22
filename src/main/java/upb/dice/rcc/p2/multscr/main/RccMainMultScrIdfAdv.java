package upb.dice.rcc.p2.multscr.main;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.aksw.word2vecrestful.word2vec.W2VNrmlMemModelBinSrch;
import org.aksw.word2vecrestful.word2vec.Word2VecFactory;
import org.aksw.word2vecrestful.word2vec.Word2VecModel;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.node.ArrayNode;

import upb.dice.rcc.multscr.finder.RccFinderMult;
import upb.dice.rcc.multscr.finder.RfmSnglrCosSim;
import upb.dice.rcc.multscr.finder.RfmSnglrCosSimIdf;
import upb.dice.rcc.multscr.finder.RfmTopCosSimSum;
import upb.dice.rcc.tool.PublicationWordSetExtractor;
import upb.dice.rcc.tool.RccNounPhraseLabelPair;
import upb.dice.rcc.tool.rmthd.RsrchMthdIdfUtil;

/**
 * Class to help execute the RccFinder to find research fields and methods for
 * the publications in a given directory.
 * 
 * It executes a naive research method finder ({@link RfmSnglrCosSim}) run on
 * the research methods first and then combine the results with the provided
 * research method results. The combined result is then used to generate IDF
 * weight map for each research method in the result.
 * 
 * After the generation of IDF map, A Top N cosine similarity sum algorithm
 * ({@link RfmTopCosSimSum}) for Research Fields and a Top IDF weighted singular
 * cosine similarity algorithm ({@link RfmSnglrCosSimIdf}) is used to identify
 * Research Fields and Research Methods respectively.
 * 
 * @author nikitsrivastava
 *
 */
public class RccMainMultScrIdfAdv extends RccMainMultScrIdf {
	/**
	 * File containing the previous results for research methods
	 */
	protected File rMthdResDbInputFile;
	/**
	 * section based map for research fields
	 */
	protected Map<String, Float> rFldSecWgthMap;
	/**
	 * section based map for research methods
	 */
	protected Map<String, Float> rMthdSecWgthMap;
	/**
	 * Norm value for weighted cosine similarity of research methods
	 */
	protected double rsrchMthdNorm;

	public RccMainMultScrIdfAdv(File pubDir, File rFldDsInputFile, File rMthdDsInputFile, File rMthdResDbInputFile,
			File rFldOutputFile, File rMthdOutputFile) throws IOException {
		super();
		this.rFldOutputFile = rFldOutputFile;
		this.rMthdOutputFile = rMthdOutputFile;
		this.pubDir = pubDir;
		this.rMthdResDbInputFile = rMthdResDbInputFile;
		// Further processing
		this.rFldLblMap = getRfldLblMap(rFldDsInputFile);
		this.rMthdLblMap = getRmthdLblMap(rMthdDsInputFile);
		this.initMemModels();
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
		// Research field input file path
		String rFldDsInputFilePath = args[1];
		// Research method input file path
		String rMthdDsInputFilePath = args[2];
		// Research method result database file path
		String rMthdResDbInputFilePath = args[3];
		// Research field results output file path
		String rFldOutputFilePath = args[4];
		// Research method results output file path
		String rMthdOutputFilePath = args[5];

		// Publication directory
		File pubDir = new File(pubDirPath);
		// Research field dataset input file
		File rFldDsInputFile = new File(rFldDsInputFilePath);
		// Research method dataset input file
		File rMthdDsInputFile = new File(rMthdDsInputFilePath);
		// Research Method result database file
		File rMthdResDbInputFile = new File(rMthdResDbInputFilePath);
		// Research field output file
		File rFldOutputFile = new File(rFldOutputFilePath);
		// Research method output file
		File rMthdOutputFile = new File(rMthdOutputFilePath);

		try {
			// init the main
			RccMainMultScrIdfAdv rccMain = new RccMainMultScrIdfAdv(pubDir, rFldDsInputFile, rMthdDsInputFile,
					rMthdResDbInputFile, rFldOutputFile, rMthdOutputFile);
			rccMain.processEntries();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Method to normalize the weighted cosine similarity values in a given list of
	 * research methods
	 * 
	 * @param mthdPairList - list of research method results
	 */
	protected void normalizeRsrchMthdResult(List<RccNounPhraseLabelPair> mthdPairList) {
		for (RccNounPhraseLabelPair mthdPair : mthdPairList) {
			double wgthdVal = mthdPair.getCosineSimWgthd();
			mthdPair.setCosineSimWgthd(wgthdVal / this.rsrchMthdNorm);
		}
	}

	/**
	 * Method find the maximum float value from a given map
	 * 
	 * @param valMap - Map between the String and Float values
	 * @return - maximum float value from the map
	 */
	public static float getMaxMapValue(Map<String, Float> valMap) {
		Float maxVal = 1f;
		List<Float> valList = new ArrayList<Float>();
		valList.addAll(valMap.values());
		Collections.sort(valList, Collections.reverseOrder());
		if (valList.size() > 0) {
			maxVal = valList.get(0);
		}
		return maxVal;
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
		// Research field weight map
		this.rFldSecWgthMap = new HashMap<String, Float>();
		this.rFldSecWgthMap.put("keywords", 1.2f);
		this.rFldFinder = new RfmTopCosSimSum(genModel, rFldMemModel, wordSetExtractor, this.rFldSecWgthMap);
		// Research method weight map
		this.rMthdSecWgthMap = new HashMap<String, Float>();
		rMthdSecWgthMap.put("methodology", 1.2f);
		// fetch total doc count
		this.docCount = pubDir.listFiles().length;
		// this.defaultIdfVal = Math.log(docCount);
		this.idfValMap = generateRmthdIdfValMap(rMthdSecWgthMap);
		this.rMthdFinder = new RfmSnglrCosSimIdf(genModel, rMthdMemModel, wordSetExtractor, rMthdSecWgthMap, idfValMap,
				defaultIdfVal);
		// Calculate research method norm
		double maxIdfWght = this.defaultIdfVal;
		double maxSecWgth = getMaxMapValue(this.rMthdSecWgthMap);

		this.rsrchMthdNorm = maxIdfWght + maxSecWgth;
	}

	/**
	 * Method to generate a IDF value map for research methods using the information
	 * extracted from a previous result file and performing a naive research method
	 * finding operation on the current set of publications
	 * 
	 * @param sectionWeightMap - section based weight map for the Research Methods
	 * @return - Map of Research Method ids against their Idf value
	 * @throws IOException
	 */
	protected Map<String, Double> generateRmthdIdfValMap(Map<String, Float> sectionWeightMap) throws IOException {
		Map<String, Double> resMap = null;
		// do naive processing and generate idf map
		RccFinderMult tempRfmFinder = new RfmSnglrCosSim(genModel, rMthdMemModel, wordSetExtractor, sectionWeightMap);
		for (final File fileEntry : pubDir.listFiles()) {
			// Closest Research Method
			List<RccNounPhraseLabelPair> rMthdPair = tempRfmFinder.findClosestResearchField(fileEntry);
			// Save research method
			this.saveRmthdEntry(fileEntry.getName(), rMthdPair);
		}
		// Fetch old result
		ArrayNode oldArrNode = (ArrayNode) RsrchMthdIdfUtil.readJsonFile(rMthdResDbInputFile);
		ArrayNode newArrNode = this.rMthdNodes;

		ArrayNode combArrNode = RsrchMthdIdfUtil.mergeJsonResults(oldArrNode, newArrNode);

		// Set Default IDF Value
		this.defaultIdfVal = Math.log(combArrNode.size());

		// Store the combined results
		// writeJsonToFile(combArrNode, rMthdResDbInputFile);

		resMap = RsrchMthdIdfUtil.generateMethodIdfMap(combArrNode);
		// clear out results from naive processing
		newArrNode.removeAll();
		return resMap;
	}

	/**
	 * Method to read all the publication files in a given directory and save the
	 * research field and method associated with them
	 * 
	 * @throws IOException
	 */
	protected void fetchClosestEntries() throws IOException {

		for (final File fileEntry : pubDir.listFiles()) {
			// Closest Research Field
			List<RccNounPhraseLabelPair> rFldPair = this.rFldFinder.findClosestResearchField(fileEntry);
			// Closest Research Method
			List<RccNounPhraseLabelPair> rMthdPair = this.rMthdFinder.findClosestResearchField(fileEntry);
			// Normalize the research method weight
			this.normalizeRsrchMthdResult(rMthdPair);
			// Save research field
			this.saveRfldEntry(fileEntry.getName(), rFldPair);
			// Save research method
			this.saveRmthdEntry(fileEntry.getName(), rMthdPair);
		}

	}

}
