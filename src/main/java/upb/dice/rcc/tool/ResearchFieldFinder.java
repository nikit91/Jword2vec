package upb.dice.rcc.tool;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.aksw.word2vecrestful.utils.Cfg;
import org.aksw.word2vecrestful.utils.Word2VecMath;
import org.aksw.word2vecrestful.word2vec.GenWord2VecModel;
import org.aksw.word2vecrestful.word2vec.W2VNrmlMemModelBruteForce;
import org.aksw.word2vecrestful.word2vec.Word2VecFactory;
import org.aksw.word2vecrestful.word2vec.Word2VecModel;
import org.apache.log4j.PropertyConfigurator;

import com.fasterxml.jackson.core.JsonProcessingException;

/**
 * Class to help find the closest research field for a given publication
 * 
 * @author nikitsrivastava
 *
 */
public class ResearchFieldFinder {

	private PublicationWordSetExtractor wordSetExtractor;
	private Word2VecModel genModel;
	private GenWord2VecModel memModel;
	private Map<Long, String> idMap;

	public ResearchFieldFinder(Word2VecModel genModel, GenWord2VecModel memModel, File idMapFile, File stopWordsFile)
			throws IOException {
		this.genModel = genModel;
		this.memModel = memModel;
		this.wordSetExtractor = new PublicationWordSetExtractor(genModel, stopWordsFile);
		this.idMap = this.readIdMap(idMapFile);
	}

	/**
	 * Method to find the closest research field for the given publication
	 * 
	 * @param pubFile - file for the given publication
	 * @throws IOException
	 */
	public void findClosestResearchField(File pubFile) throws IOException {
		Set<String> wordSet = wordSetExtractor.extractPublicationWordSet(pubFile);
		float[] sumVec = ResearchFieldModelGenerator.getSumVector(wordSet, genModel);
		float[] normSumVec = Word2VecMath.normalize(sumVec);
		String idStr = memModel.getClosestEntry(normSumVec);
		Long id = Long.valueOf(idStr);
		System.out.println(idMap.get(id));
	}

	/**
	 * Method to the id map tsv file to memory
	 * 
	 * @param idMapFile - file to read id map from
	 * @return - Map between the id's and their corresponding data
	 * @throws IOException
	 */
	public Map<Long, String> readIdMap(File idMapFile) throws IOException {
		Map<Long, String> resMap = new HashMap<Long, String>();
		BufferedReader br = null;
		try {
			br = new BufferedReader(new FileReader(idMapFile));
			String line;
			while ((line = br.readLine()) != null) {
				int indx = line.indexOf("\t");
				Long id = Long.parseLong(line.substring(0, indx));
				String rsrchFldLbl = line.substring(indx + 1);
				resMap.put(id, rsrchFldLbl);
			}
		} finally {
			br.close();
		}
		return resMap;
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

		Word2VecModel nrmlRsrchFldMdl = Word2VecFactory.getNrmlRsrchFldModel();
		// final GenWord2VecModel memModel = new
		// W2VNrmlMemModelUnitVec(nrmlRsrchFldMdl.word2vec, nrmlRsrchFldMdl.vectorSize);
		W2VNrmlMemModelBruteForce memModel = new W2VNrmlMemModelBruteForce(nrmlRsrchFldMdl.word2vec,
				nrmlRsrchFldMdl.vectorSize);

		String pubFilePath = "data/rcc/ext_publications/105.txt";
		String stopwordsFilePath = "data/rcc/english.stopwords.txt";
		String idMapFilePath = "data/rcc/rsrchFldIdMap.tsv";

		File inputFile = new File(pubFilePath);
		File stopWordsFile = new File(stopwordsFilePath);
		File idMapFile = new File(idMapFilePath);

		ResearchFieldFinder finder = new ResearchFieldFinder(genModel, memModel, idMapFile, stopWordsFile);
		finder.findClosestResearchField(inputFile);

	}

}
