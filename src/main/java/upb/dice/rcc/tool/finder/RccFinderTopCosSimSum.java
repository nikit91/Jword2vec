package upb.dice.rcc.tool.finder;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.aksw.word2vecrestful.utils.Word2VecMath;
import org.aksw.word2vecrestful.word2vec.GenWord2VecModel;
import org.aksw.word2vecrestful.word2vec.Word2VecModel;

import upb.dice.rcc.tool.RccUtil;

/**
 * Class to help find the closest research field for a given publication
 * 
 * @author nikitsrivastava
 *
 */
public class RccFinderTopCosSimSum extends RccFinder{

	public static final Map<String, Float> FIELD_WEIGHT_MAP = new HashMap<String, Float>();
	public static final int TOP_COUNT = 10;
	static {
		FIELD_WEIGHT_MAP.put("keywords", 1.2f);
	}

	public RccFinderTopCosSimSum(Word2VecModel genModel, GenWord2VecModel memModel) throws IOException {
		super(genModel, memModel);
	}

	/**
	 * Method to find the closest research field for the given publication
	 * 
	 * @param pubFile - file for the given publication
	 * @throws IOException
	 */
	public RccNounPhrasePair findClosestResearchField(File pubFile) throws IOException {
		Map<String, List<String>> fldWordsMap = wordSetExtractor.extractPublicationWordSet(pubFile);
		List<String> wordList = new ArrayList<>();
		for (List<String> wordEntries : fldWordsMap.values()) {
			wordList.addAll(wordEntries);
		}
		RccNounPhrasePair phrsPair = fetchSimilarRsrchFld(fldWordsMap, pubFile.getName());
		return phrsPair;
	}

	private RccNounPhrasePair fetchSimilarRsrchFld(Map<String, List<String>> fldWordsMap, String fileLabel) {
		RccNounPhrasePair resPair = null;
		List<RccNounPhrasePair> pairList = new ArrayList<>();
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
				String closestWord = memModel.getClosestEntry(normSumVec);
				if (closestWord != null) {
					Double cosSim = Word2VecMath.cosineSimilarityNormalizedVecs(normSumVec,
							memModel.getW2VMap().get(closestWord));
					cosSim *= wgth;
					RccNounPhrasePair tmpPair = new RccNounPhrasePair(wordEntry, closestWord, cosSim);
					pairList.add(tmpPair);
				}
			}

		}
		Collections.sort(pairList, Collections.reverseOrder());
		resPair = pairList.get(0);
		List<RccNounPhrasePair> topPairList = new ArrayList<>();
		if (pairList.size() > TOP_COUNT) {
			topPairList.addAll(pairList.subList(0, TOP_COUNT));
		} else {
			topPairList.addAll(pairList);
		}
		resPair = findClosestSumEntry(topPairList, fileLabel);
		return resPair;
	}

	private RccNounPhrasePair findClosestSumEntry(List<RccNounPhrasePair> topPairList, String fileLabel) {
		RccNounPhrasePair resPair = null;
		List<String> wordList = new ArrayList<>();
		for (RccNounPhrasePair pair : topPairList) {
			wordList.add(pair.getNounPhrase());
		}
		float[] sumVec = RccUtil.getSumVector(wordList, genModel);
		float[] normSumVec = Word2VecMath.normalize(sumVec);
		String closestWord = memModel.getClosestEntry(normSumVec);
		Double cosSim = Word2VecMath.cosineSimilarityNormalizedVecs(normSumVec, memModel.getW2VMap().get(closestWord));
		resPair = new RccNounPhrasePair(fileLabel, closestWord, cosSim);
		return resPair;
	}

}
