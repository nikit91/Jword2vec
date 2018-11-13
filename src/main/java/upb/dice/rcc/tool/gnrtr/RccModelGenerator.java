package upb.dice.rcc.tool.gnrtr;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.aksw.word2vecrestful.tool.ModelNormalizer;
import org.aksw.word2vecrestful.word2vec.Word2VecModel;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import upb.dice.rcc.tool.RccUtil;

/**
 * Abstract class to help Generate a Word2Vec model for a given input file for
 * Rich Context Challenge
 * 
 * @author nikitsrivastava
 *
 */
public abstract class RccModelGenerator {

	public static Logger LOG = LogManager.getLogger(RccModelGenerator.class);
	/**
	 * Mapping of Word/Phrase/Id to their corresponding list of words
	 */
	protected Map<String, List<String>> wrdsIdMap = new HashMap<>();
	/**
	 * General Non-Normalized Word2Vec Model
	 */
	protected Word2VecModel w2vModel;

	abstract protected void loadWordIdMap(File inputFile) throws IOException;

	/**
	 * Constructor to initialize {@link RccModelGenerator}
	 * 
	 * @param w2vModel - instance of {@link #w2vModel}
	 */
	public RccModelGenerator(Word2VecModel w2vModel) {
		this.w2vModel = w2vModel;
	}

	/**
	 * Method to generate a custom Word2Vec Model for a given input file into a
	 * given outputFile
	 * 
	 * @param inputFile  - input file
	 * @param outputFile - output bin model file
	 * @throws IOException
	 */
	public void generateResearchFieldsModel(File inputFile, File outputFile) throws IOException {
		// ensure directory creation
		outputFile.getParentFile().mkdirs();
		// declare the output stream
		BufferedOutputStream bOutStrm = null;
		try {
			// read and load the input file onto memory
			loadWordIdMap(inputFile);

			Integer totWords = wrdsIdMap.size();
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
			for (String rsrchFldId : wrdsIdMap.keySet()) {
				List<String> rsrchfldWords = wrdsIdMap.get(rsrchFldId);
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
			if (bOutStrm != null) {
				bOutStrm.close();
			}
		}

	}

}
