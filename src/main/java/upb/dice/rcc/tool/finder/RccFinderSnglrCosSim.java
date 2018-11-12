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
public class RccFinderSnglrCosSim extends RccFinder {

	public static final Map<String, Float> FIELD_WEIGHT_MAP = new HashMap<String, Float>();
	static {
		FIELD_WEIGHT_MAP.put("keywords", 1.2f);
	}

	public RccFinderSnglrCosSim(Word2VecModel genModel, GenWord2VecModel memModel) throws IOException {
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
		RccNounPhrasePair phrsPair = fetchSimilarRsrchFld(fldWordsMap);
		return phrsPair;
	}

	private RccNounPhrasePair fetchSimilarRsrchFld(Map<String, List<String>> fldWordsMap) {
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
				String closestWord = memModel.getClosestEntry(sumVec);
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
		return resPair;
	}

}
