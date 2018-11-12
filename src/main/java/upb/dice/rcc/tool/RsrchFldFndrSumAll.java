package upb.dice.rcc.tool;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.aksw.word2vecrestful.utils.Cfg;
import org.aksw.word2vecrestful.utils.Word2VecMath;
import org.aksw.word2vecrestful.word2vec.GenWord2VecModel;
import org.aksw.word2vecrestful.word2vec.W2VNrmlMemModelBinSrch;
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
public class RsrchFldFndrSumAll extends ResearchFieldFinder{


	public RsrchFldFndrSumAll(Word2VecModel genModel, GenWord2VecModel memModel, File idMapFile, File stopWordsFile)
			throws IOException {
		super(genModel, memModel, idMapFile, stopWordsFile);
	}

	/**
	 * Method to find the closest research field for the given publication
	 * 
	 * @param pubFile - file for the given publication
	 * @throws IOException
	 */
	public RsrchFldNounPhrsPair findClosestResearchField(File pubFile) throws IOException {
		Map<String, List<String>> fldWordsMap = wordSetExtractor.extractPublicationWordSet(pubFile);
		List<String> wordList = new ArrayList<>();
		for (List<String> wordEntries : fldWordsMap.values()) {
			wordList.addAll(wordEntries);
		}
		float[] sumVec = RccUtil.getSumVector(wordList, genModel);
		float[] normSumVec = Word2VecMath.normalize(sumVec);
		String idStr = memModel.getClosestEntry(normSumVec);
		Double cosSim = Word2VecMath.cosineSimilarityNormalizedVecs(normSumVec, memModel.getW2VMap().get(idStr));
		RsrchFldNounPhrsPair tmpPair = new RsrchFldNounPhrsPair(pubFile.getName(), idStr, cosSim);
		return tmpPair;
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
		final GenWord2VecModel memModel = new W2VNrmlMemModelBinSrch(nrmlRsrchFldMdl.word2vec,
				nrmlRsrchFldMdl.vectorSize);
		memModel.process();
//		W2VNrmlMemModelBruteForce memModel = new W2VNrmlMemModelBruteForce(nrmlRsrchFldMdl.word2vec,
//				nrmlRsrchFldMdl.vectorSize);

		// String pubFilePath = "data/rcc/ext_publications/105.txt";
		String stopwordsFilePath = "data/rcc/english.stopwords.txt";
		String idMapFilePath = "data/rcc/rsrchFldIdMap.tsv";

		// File inputFile = new File(pubFilePath);
		File stopWordsFile = new File(stopwordsFilePath);
		File idMapFile = new File(idMapFilePath);

		RsrchFldFndrSumAll finder = new RsrchFldFndrSumAll(genModel, memModel, idMapFile, stopWordsFile);

		String pubFileDirPath = "data/rcc/ext_publications/";
		File pubFileDir = new File(pubFileDirPath);
		Map<String, Set<String>> resIdMap = new HashMap<>();
		for (final File fileEntry : pubFileDir.listFiles()) {

			RsrchFldNounPhrsPair tmpPair = finder.findClosestResearchField(fileEntry);
			String fldId = tmpPair.getClosestWord();
			Set<String> fileNameSet = resIdMap.get(fldId);
			if (fileNameSet == null) {
				fileNameSet = new HashSet<>();
				resIdMap.put(fldId, fileNameSet);
			}
			fileNameSet.add(fileEntry.getName());
		}

		printResults(resIdMap, finder.idIndx, finder.resColIndx);

	}

}
