package upb.dice.rcc.tool;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.aksw.word2vecrestful.tool.ModelNormalizer;
import org.aksw.word2vecrestful.utils.Cfg;
import org.aksw.word2vecrestful.word2vec.Word2VecFactory;
import org.aksw.word2vecrestful.word2vec.Word2VecModel;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.opencsv.CSVReader;

/**
 * Utility class to generate vector model for research fields in a given
 * json.<br>
 * Input file csv format:
 * 
 * <pre>
 * L1,L2,L3,ID,altLabel
 * ...,...,...,...,...
 * ...
 * </pre>
 * <dl>
 * <dt>L1</dt>
 * <dd>Level 1 label</dd>
 * <dt>L2</dt>
 * <dd>Level 2 label</dd>
 * <dt>L3</dt>
 * <dd>Level 3 label</dd>
 * <dt>ID</dt>
 * <dd>id for the research field</dd>
 * <dt>altLabel</dt>
 * <dd>alternate label for the research field</dd>
 * </dl>
 * 
 * @author nikitsrivastava
 *
 */
public class RsrchFldMdlGnrtrCsv {

	public static Logger LOG = LogManager.getLogger(RsrchFldMdlGnrtrCsv.class);

	private Map<String, List<String>> rsrchFldWrdsMap = new HashMap<>();
	private Map<String, List<String>> wordListCacheMap = new HashMap<>();
	private Word2VecModel w2vModel;

	public static final int[] DEFAULT_COL_INDXS = { 0, 1, 2, 4 };
	public static final int DEFAULT_ID_INDX = 3;
	private int[] colIndxs;
	private int idIndx;

	public RsrchFldMdlGnrtrCsv(Word2VecModel w2vModel) {
		this.w2vModel = w2vModel;
		this.colIndxs = DEFAULT_COL_INDXS;
		this.idIndx = DEFAULT_ID_INDX;
	}

	public RsrchFldMdlGnrtrCsv(Word2VecModel w2vModel, int idIndx, int[] colIndxs) {
		this.w2vModel = w2vModel;
		this.colIndxs = colIndxs;
		this.idIndx = idIndx;
	}

	/**
	 * Method to read the json research field file and extract each subfield's node
	 * 
	 * @param inputFile - file object of the input json file
	 * @throws IOException
	 */
	private void readResearchFieldsCsv(File inputFile) throws IOException {
		CSVReader csvReader = null;
		try {
			csvReader = new CSVReader(new FileReader(inputFile));
			// Reading header
			csvReader.readNext();
			String[] lineArr;
			// Read the csv file line by line
			while ((lineArr = csvReader.readNext()) != null) {
				List<String> rsrchFldWords = new ArrayList<>();
				for (int cIndx : colIndxs) {
					// extract column text
					String colText = lineArr[cIndx];
					// check the cache for word tokens
					List<String> colWords = wordListCacheMap.get(colText);
					if (colWords == null) {
						// extract all word tokens from colText
						colWords = RccUtil.fetchAllWordTokens(colText, w2vModel);
						// save to cache
						wordListCacheMap.put(colText, colWords);
					}
					// add all the words to research field list
					rsrchFldWords.addAll(colWords);
				}
				// extract id
				String idText = lineArr[idIndx];
				rsrchFldWrdsMap.put(idText, rsrchFldWords);
			}

		} finally {
			csvReader.close();
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
	public void generateResearchFieldsModel(File inputFile, File outputFile) throws IOException {
		// ensure directory creation
		outputFile.getParentFile().mkdirs();
		// declare the output stream
		BufferedOutputStream bOutStrm = null;
		try {
			// read and load the input file onto memory
			readResearchFieldsCsv(inputFile);

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
			for (String rsrchFldId : rsrchFldWrdsMap.keySet()) {
				List<String> rsrchfldWords = rsrchFldWrdsMap.get(rsrchFldId);
				// calculate the sum vector for the field
				float[] sumVec = RccUtil.getSumVector(rsrchfldWords, w2vModel);
				// get the byte array for the normalized sum vector
				byte[] bSumVec = ModelNormalizer.getNormalizedVecBA(sumVec);
				// write the research field id
				bOutStrm.write(rsrchFldId.getBytes(StandardCharsets.UTF_8));
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
		RsrchFldMdlGnrtrCsv generator = new RsrchFldMdlGnrtrCsv(genModel);
		String inputFilePath = "data/rcc/train_test/sage_research_fields.csv";
		String outputFilePath = Cfg.get("org.aksw.word2vecrestful.word2vec.nrmlrsrchfldbinmodel.model");

		File inputFile = new File(inputFilePath);
		File outputFile = new File(outputFilePath);
		
		generator.generateResearchFieldsModel(inputFile, outputFile);

	}

}
