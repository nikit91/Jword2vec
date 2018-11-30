package upb.dice.rcc.multscr.main;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.aksw.word2vecrestful.word2vec.W2VNrmlMemModelBinSrch;
import org.aksw.word2vecrestful.word2vec.Word2VecFactory;
import org.aksw.word2vecrestful.word2vec.Word2VecModel;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.opencsv.CSVReader;

import upb.dice.rcc.multscr.finder.RfmSnglrCosSimIdf;
import upb.dice.rcc.multscr.finder.RfmTopCosSimSum;
import upb.dice.rcc.tool.PublicationWordSetExtractor;
import upb.dice.rcc.tool.RccNounPhraseLabelPair;

/**
 * Class to help execute the RccFinder to find research fields and methods for
 * the publications in a given directory
 * 
 * @author nikitsrivastava
 *
 */
public class RccMainMultScrIdf extends RccMainMultScr{
	/**
	 * the number of documents to process
	 */
	protected int docCount;
	/**
	 * default idf weight value
	 */
	protected double defaultIdfVal;
	/**
	 * Idf weight value file for research methods
	 */
	protected File idfValFile;
	/**
	 * File object for the directory containing the publications to be processed
	 */
	protected File pubDir;
	/**
	 * Map of Research methods ids to their idf weight values
	 */
	protected Map<String, Double> idfValMap;
	
	protected RccMainMultScrIdf() {
	}

	public RccMainMultScrIdf(File rFldInputFile, File rMthdInputFile, File rFldOutputFile, File rMthdOutputFile,
			File idfValFile, File pubDir) throws IOException {
		this.rFldOutputFile = rFldOutputFile;
		this.rMthdOutputFile = rMthdOutputFile;
		this.idfValFile = idfValFile;
		this.pubDir = pubDir;
		
		//Further processing
		this.initMemModels();
		this.rFldLblMap = getRfldLblMap(rFldInputFile);
		this.rMthdLblMap = getRmthdLblMap(rMthdInputFile);
		
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
		// Research method input file path
		String idfValFilePath = args[5];

		File rFldInputFile = new File(rFldInputFilePath);
		File rMthdInputFile = new File(rMthdInputFilePath);
		// Research field output file
		File rFldOutputFile = new File(rFldOutputFilePath);
		// Research method output file
		File rMthdOutputFile = new File(rMthdOutputFilePath);
		// Research method output file
		File idfValFile = new File(idfValFilePath);

		File pubDir = new File(pubDirPath);
		try {
			// init the main
			RccMainMultScrIdf rccMain = new RccMainMultScrIdf(rFldInputFile, rMthdInputFile, rFldOutputFile,
					rMthdOutputFile, idfValFile, pubDir);
			rccMain.processEntries();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Method to process the entries in the given publication folder to identify the
	 * research methods and research fields
	 * 
	 * @throws Exception
	 */
	public void processEntries() throws Exception {
		TLOG.logTime(1);
		// fetch closest entries
		this.fetchClosestEntries();
		// write the closest entries to file
		this.writeClosestEntries(rFldOutputFile, rMthdOutputFile);
		TLOG.printTime(1, "Total Fetch time (Methods and Fields)");
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
		Word2VecModel nrmlRsrchMthdMdl = Word2VecFactory.getNrmlRsrchMthdModel();

		rFldMemModel = new W2VNrmlMemModelBinSrch(nrmlRsrchFldMdl.word2vec, nrmlRsrchFldMdl.vectorSize);
		rFldMemModel.process();

		rMthdMemModel = new W2VNrmlMemModelBinSrch(nrmlRsrchMthdMdl.word2vec, nrmlRsrchMthdMdl.vectorSize);
		rMthdMemModel.process();

		this.rFldFinder = new RfmTopCosSimSum(genModel, rFldMemModel, wordSetExtractor);
		// Manual weight map
		Map<String, Float> sectionWeightMap = new HashMap<String, Float>();
		sectionWeightMap.put("methodology", 1.2f);
		// fetch total doc count
		this.docCount = pubDir.listFiles().length;
		this.defaultIdfVal = Math.log(docCount);
		this.idfValMap = getIdfValMap(this.idfValFile);
		this.rMthdFinder = new RfmSnglrCosSimIdf(genModel, rMthdMemModel, wordSetExtractor, sectionWeightMap, idfValMap, defaultIdfVal);
	}

	/**
	 * Method to construct a mapping between research method id and corresponding
	 * Idf weights
	 * 
	 * @param inputFile - research method and idf map file
	 * @return - Mapping between id and its label
	 * @throws IOException
	 */
	public static Map<String, Double> getIdfValMap(File inputFile) throws IOException {
		Map<String, Double> resMap = new HashMap<String, Double>();
		CSVReader csvReader = null;
		try {
			csvReader = new CSVReader(new FileReader(inputFile));
			String[] lineArr;
			// Read the csv file line by line
			while ((lineArr = csvReader.readNext()) != null) {
				String lineId = lineArr[0];
				String lineLbl = lineArr[3];
				resMap.put(lineId, Double.parseDouble(lineLbl));
			}

		} finally {
			csvReader.close();
		}
		return resMap;
	}

	/**
	 * Method to read all the publication files in a given directory and save the
	 * research field and method associated with them
	 * 
	 * @param pubDirPath - path to the publications directory
	 * @throws IOException
	 */
	protected void fetchClosestEntries() throws IOException {

		for (final File fileEntry : pubDir.listFiles()) {
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
