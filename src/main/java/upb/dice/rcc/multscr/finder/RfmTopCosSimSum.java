package upb.dice.rcc.multscr.finder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.aksw.word2vecrestful.utils.Word2VecMath;
import org.aksw.word2vecrestful.word2vec.GenWord2VecModel;
import org.aksw.word2vecrestful.word2vec.Word2VecModel;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import upb.dice.rcc.tool.PublicationWordSetExtractor;
import upb.dice.rcc.tool.RccNounPhraseLabelPair;
import upb.dice.rcc.tool.RccUtil;

/**
 * Extended implementation of {@link RfmSnglrCosSim}. Instead of the top
 * most cosine similarity this class uses top
 * {@link RfmTopCosSimSum#topCount} number of entries to find the closest
 * entry in the custom model.
 * 
 * @author nikitsrivastava
 *
 */
public class RfmTopCosSimSum extends RfmSnglrCosSim {

	public static Logger LOG = LogManager.getLogger(RfmTopCosSimSum.class);

	public static final int TOP_COUNT = 10;
	/**
	 * Number of top words to consider for the final similarity
	 */
	protected int topCount;

	/**
	 * @see RfmSnglrCosSim#RccFinderSnglrCosSim(Word2VecModel,
	 *      GenWord2VecModel)
	 */
	public RfmTopCosSimSum(Word2VecModel genModel, GenWord2VecModel memModel,
			PublicationWordSetExtractor wordSetExtractor) throws IOException {
		super(genModel, memModel, wordSetExtractor);
		this.topCount = TOP_COUNT;
	}

	/**
	 * @see RfmSnglrCosSim#RccFinderSnglrCosSim(Word2VecModel,
	 *      GenWord2VecModel, Map)
	 */
	public RfmTopCosSimSum(Word2VecModel genModel, GenWord2VecModel memModel,
			PublicationWordSetExtractor wordSetExtractor, Map<String, Float> weightMap) throws IOException {
		super(genModel, memModel, wordSetExtractor, weightMap);
		this.topCount = TOP_COUNT;
	}

	/**
	 * Constructor to initialize instance of {@link RccFinderMult} using the given
	 * general Normalized word model and a Word2Vec custom model
	 * 
	 * @param genModel - {@link #genModel}
	 * @param memModel - {@link #memModel}
	 * @param topCount - {@link #topCount}
	 * @throws IOException
	 */
	public RfmTopCosSimSum(Word2VecModel genModel, GenWord2VecModel memModel,
			PublicationWordSetExtractor wordSetExtractor, int topCount) throws IOException {
		super(genModel, memModel, wordSetExtractor);
		this.topCount = topCount;
	}

	/**
	 * 
	 * Constructor to initialize instance of {@link RccFinderMult} using the given
	 * general Normalized word model and a Word2Vec custom model
	 * 
	 * @param genModel  - {@link #genModel}
	 * @param memModel  - {@link #memModel}
	 * @param topCount  - {@link #topCount}
	 * @param weightMap - {@link #weightMap}
	 * @throws IOException
	 */
	public RfmTopCosSimSum(Word2VecModel genModel, GenWord2VecModel memModel,
			PublicationWordSetExtractor wordSetExtractor, int topCount, Map<String, Float> weightMap)
			throws IOException {
		super(genModel, memModel, wordSetExtractor, weightMap);
		this.topCount = topCount;
	}

	@Override
	protected List<RccNounPhraseLabelPair> fetchSimilarRsrchFld(Map<String, List<String>> fldWordsMap, String fileLabel) {
		List<RccNounPhraseLabelPair> resPairList = null;
		List<RccNounPhraseLabelPair> pairList = new ArrayList<>();
		for (String fldLabel : fldWordsMap.keySet()) {
			Float wgth = weightMap.get(fldLabel);
			if (wgth == null) {
				wgth = 1f;
			}
			Set<String> nounPhraseSet = new HashSet<>();
			nounPhraseSet.addAll(fldWordsMap.get(fldLabel));
			for (String nounPhrase : nounPhraseSet) {
				float[] sumVec = RccUtil.getSumVector(RccUtil.fetchAllWordTokens(nounPhrase, genModel), genModel);
				String closestWord = null;
				if (sumVec != null && (closestWord = memModel.getClosestEntry(sumVec)) != null) {
					float[] normSumVec = Word2VecMath.normalize(sumVec);
					Double cosSim = Word2VecMath.cosineSimilarity(normSumVec,
							memModel.getW2VMap().get(closestWord));
					// cosSim *= wgth;
					RccNounPhraseLabelPair tmpPair = new RccNounPhraseLabelPair(nounPhrase, closestWord, cosSim, wgth);
					tmpPair.setSumVec(sumVec);
					pairList.add(tmpPair);
				} else if (sumVec == null) {
					LOG.info("No sum vector found for the noun phrase: " + nounPhrase);
				}
			}

		}
		Collections.sort(pairList, Collections.reverseOrder());
		List<RccNounPhraseLabelPair> topPairList = new ArrayList<>();
		if (pairList.size() > topCount) {
			topPairList.addAll(pairList.subList(0, topCount));
		} else {
			topPairList.addAll(pairList);
		}
		resPairList = findClosestSumEntry(topPairList, fileLabel);
		return resPairList;
	}

	/**
	 * Method to find the closest entry in the custom model using the sum of all
	 * word vectors from the topPairList
	 * 
	 * @param topPairList - list of all the top selected words
	 * @param fileLabel   - label of the publication file
	 * @return
	 */
	protected List<RccNounPhraseLabelPair> findClosestSumEntry(List<RccNounPhraseLabelPair> topPairList, String fileLabel) {
		List<RccNounPhraseLabelPair> resPairList = new ArrayList<>();
		int vecSize = memModel.getVectorSize();
		float[] sumVec = new float[vecSize];
		for (RccNounPhraseLabelPair pair : topPairList) {
			float[] curVec = pair.getSumVec();
			if (curVec != null) {
				for (int i = 0; i < vecSize; i++) {
					sumVec[i] += curVec[i];
				}
			}
		}

		float[] normSumVec = Word2VecMath.normalize(sumVec);
		String closestWord = memModel.getClosestEntry(normSumVec);
		Double cosSim = Word2VecMath.cosineSimilarityNormalizedVecs(normSumVec, memModel.getW2VMap().get(closestWord));
		resPairList.add(new RccNounPhraseLabelPair(fileLabel, closestWord, cosSim, 1));
		return resPairList;
	}

}
