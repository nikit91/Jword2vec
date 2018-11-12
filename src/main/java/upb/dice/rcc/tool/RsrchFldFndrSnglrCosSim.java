package upb.dice.rcc.tool;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
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

public class RsrchFldFndrSnglrCosSim extends ResearchFieldFinder {

	public static final Map<String, Float> FIELD_WEIGHT_MAP = new HashMap<String, Float>();
	static {
		FIELD_WEIGHT_MAP.put("keywords", 1.2f);
	}

	public RsrchFldFndrSnglrCosSim(Word2VecModel genModel, GenWord2VecModel memModel, File idMapFile,
			File stopWordsFile) throws IOException {
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
		RsrchFldNounPhrsPair phrsPair = fetchSimilarRsrchFld(fldWordsMap);
		return phrsPair;
	}

	private RsrchFldNounPhrsPair fetchSimilarRsrchFld(Map<String, List<String>> fldWordsMap) {
		RsrchFldNounPhrsPair resPair = null;
		List<RsrchFldNounPhrsPair> pairList = new ArrayList<>();
		for (String fldLabel : fldWordsMap.keySet()) {
			Float wgth = FIELD_WEIGHT_MAP.get(fldLabel);
			if (wgth == null) {
				wgth = 1f;
			}
			Set<String> wordSet = new HashSet<>();
			wordSet.addAll(fldWordsMap.get(fldLabel));
			for (String wordEntry : wordSet) {
				float[] sumVec = RccUtil.getSumVector(RccUtil.fetchAllWordTokens(wordEntry, genModel), genModel);
				float[] normSumVec = Word2VecMath.normalize(sumVec);
				String closestWord = memModel.getClosestEntry(sumVec);
				if (closestWord != null) {
					Double cosSim = Word2VecMath.cosineSimilarityNormalizedVecs(normSumVec,
							memModel.getW2VMap().get(closestWord));
					cosSim *= wgth;
					RsrchFldNounPhrsPair tmpPair = new RsrchFldNounPhrsPair(wordEntry, closestWord, cosSim);
					pairList.add(tmpPair);
				}
			}

		}
		Collections.sort(pairList, Collections.reverseOrder());
		resPair = pairList.get(0);
		return resPair;
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
		final GenWord2VecModel memModel = new W2VNrmlMemModelBinSrch(nrmlRsrchFldMdl.word2vec,
				nrmlRsrchFldMdl.vectorSize);
		memModel.process();
		String stopwordsFilePath = "data/rcc/english.stopwords.txt";
		String idMapFilePath = "data/rcc/rsrchFldIdMap.tsv";

		// File inputFile = new File(pubFilePath);
		File stopWordsFile = new File(stopwordsFilePath);
		File idMapFile = new File(idMapFilePath);

		RsrchFldFndrSnglrCosSim finder = new RsrchFldFndrSnglrCosSim(genModel, memModel, idMapFile, stopWordsFile);

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
