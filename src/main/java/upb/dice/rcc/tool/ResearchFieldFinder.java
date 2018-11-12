package upb.dice.rcc.tool;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Map;
import java.util.Set;

import org.aksw.word2vecrestful.word2vec.GenWord2VecModel;
import org.aksw.word2vecrestful.word2vec.Word2VecModel;

import com.opencsv.CSVReader;

/**
 * Class to help find the closest research field for a given publication
 * 
 * @author nikitsrivastava
 *
 */
public abstract class ResearchFieldFinder {

	protected PublicationWordSetExtractor wordSetExtractor;
	protected Word2VecModel genModel;
	protected GenWord2VecModel memModel;

	protected int idIndx = RsrchFldMdlGnrtrCsv.DEFAULT_ID_INDX;
	protected int resColIndx = 4;

	public ResearchFieldFinder(Word2VecModel genModel, GenWord2VecModel memModel, File idMapFile, File stopWordsFile)
			throws IOException {
		this.genModel = genModel;
		this.memModel = memModel;
		this.wordSetExtractor = new PublicationWordSetExtractor(genModel, stopWordsFile);
	}

	/**
	 * Method to find the closest research field for the given publication
	 * 
	 * @param pubFile - file for the given publication
	 * @throws IOException
	 */
	abstract public RsrchFldNounPhrsPair findClosestResearchField(File pubFile) throws IOException;

	public static void printResults(Map<String, Set<String>> resIdMap, int idIndx, int resColIndx) throws IOException {
		// logic to read labels from the csv file
		String inputFilePath = "data/rcc/train_test/sage_research_fields.csv";

		File inputFile = new File(inputFilePath);
		CSVReader csvReader = null;
		try {
			csvReader = new CSVReader(new FileReader(inputFile));
			// Reading header
			csvReader.readNext();
			String[] lineArr;
			Set<String> idSet = resIdMap.keySet();
			// Read the csv file line by line
			while ((lineArr = csvReader.readNext()) != null) {
				String lineId = lineArr[idIndx];
				if (idSet.contains(lineId)) {
					Set<String> fileNameSet = resIdMap.get(lineId);
					for (String fileName : fileNameSet) {
						System.out.println(fileName + " : " + lineArr[resColIndx]);
					}
				}
			}

		} finally {
			csvReader.close();
		}
	}

}
