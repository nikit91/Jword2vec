package upb.dice.rcc.multscr.finder;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.aksw.word2vecrestful.utils.Word2VecMath;
import org.aksw.word2vecrestful.word2vec.GenWord2VecModel;
import org.aksw.word2vecrestful.word2vec.Word2VecModel;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import upb.dice.rcc.tool.PublicationWordSetExtractor;
import upb.dice.rcc.tool.RccNounPhraseLabelPair;
import upb.dice.rcc.tool.RccNplpLabelComparator;
import upb.dice.rcc.tool.RccUtil;

/**
 * Implementation for {@link RccFinderMult}. This class finds the closest term
 * for each noun phrase in the publication and then select the one with highest
 * cosine similarity to its corresponding entry in the custom model. Also, it
 * uses weight map to assign weights to the words from a particular section of
 * the publication.
 * 
 * @author nikitsrivastava
 *
 */
public class RfmSnglrCosSim extends RccFinderMult {

	public static Logger LOG = LogManager.getLogger(RfmSnglrCosSim.class);

	public static final int TOP_RES_LENGTH = 1;

	public static final double CSIM_CUTOFF = 0.9d;

	protected static final Map<String, Float> FIELD_WEIGHT_MAP = new HashMap<String, Float>();
	static {
		FIELD_WEIGHT_MAP.put("keywords", 1.2f);
	}
	/**
	 * weight map to assign weights to the words from a particular section of the
	 * publication
	 */
	protected Map<String, Float> sectionWeightMap;

	/**
	 * Contructor to initialize instance of {@link RccFinderSnglrCosSim} using the
	 * given general Non-Normalized word model and a Word2Vec custom model. The
	 * weightMap is set as the default {@link FIELD_WEIGHT_MAP}
	 * 
	 * @param genModel - {@link #genModel}
	 * @param memModel - {@link #memModel}
	 * @throws IOException
	 */
	/*
	 * public RccFinderSnglrCosSim(Word2VecModel genModel, GenWord2VecModel
	 * memModel) throws IOException { super(genModel, memModel); this.weightMap =
	 * FIELD_WEIGHT_MAP; }
	 */

	/**
	 * Contructor to initialize instance of {@link RfmSnglrCosSim} using the given
	 * general Non-Normalized word model and a Word2Vec custom model. The weightMap
	 * is set as the default {@link FIELD_WEIGHT_MAP}
	 * 
	 * @param genModel         - {@link #genModel}
	 * @param memModel         - {@link #memModel}
	 * @param wordSetExtractor - {@link #wordSetExtractor}
	 * @throws IOException
	 */
	public RfmSnglrCosSim(Word2VecModel genModel, GenWord2VecModel memModel,
			PublicationWordSetExtractor wordSetExtractor) throws IOException {
		super(genModel, memModel, wordSetExtractor);
		this.sectionWeightMap = FIELD_WEIGHT_MAP;
	}

	/**
	 * Contructor to initialize instance of {@link RfmSnglrCosSim} using the given
	 * general Normalized word model, a Word2Vec custom model and a WeightMap
	 * 
	 * @param genModel  - {@link #genModel}
	 * @param memModel  - {@link #memModel}
	 * @param weightMap - {@link #sectionWeightMap}
	 * @throws IOException
	 */
	public RfmSnglrCosSim(Word2VecModel genModel, GenWord2VecModel memModel,
			PublicationWordSetExtractor wordSetExtractor, Map<String, Float> weightMap) throws IOException {
		super(genModel, memModel, wordSetExtractor);
		this.sectionWeightMap = weightMap;
	}

	/**
	 * Method to find the closest research field for the given publication
	 * 
	 * @param pubFile - file for the given publication
	 * @return - Noun-Phrase and label pair with highest cosine similarity
	 * @throws IOException
	 */
	public List<RccNounPhraseLabelPair> findClosestResearchField(File pubFile) throws IOException {
		return findClosestResearchField(pubFile, this.wordSetExtractor);
	}

	/**
	 * Method to find the closest research field for the given publication
	 * 
	 * @param pubFile - file for the given publication
	 * @return - Noun-Phrase and label pair with highest cosine similarity
	 * @throws IOException
	 */
	public List<RccNounPhraseLabelPair> findClosestResearchField(File pubFile,
			PublicationWordSetExtractor wordSetExtractor) throws IOException {
		Map<String, List<String>> fldWordsMap = wordSetExtractor.extractPublicationWordSet(pubFile);
		List<String> wordList = new ArrayList<>();
		for (List<String> wordEntries : fldWordsMap.values()) {
			wordList.addAll(wordEntries);
		}
		List<RccNounPhraseLabelPair> phrsPairList = fetchSimilarRsrchFld(fldWordsMap, pubFile.getName());
		return phrsPairList;
	}

	/**
	 * Method to fetch the noun-phrase and label pair with highest cosine similarity
	 * between them
	 * 
	 * @param fldWordsMap - map of noun phrases mapped to their particular sections
	 *                    in the publication
	 * @return - Noun-Phrase and label pair with highest cosine similarity
	 */
	protected List<RccNounPhraseLabelPair> fetchSimilarRsrchFld(Map<String, List<String>> fldWordsMap,
			String fileLabel) {
		List<RccNounPhraseLabelPair> resPairList = null;
		List<RccNounPhraseLabelPair> pairList = new ArrayList<>();
		for (String fldLabel : fldWordsMap.keySet()) {
			Float wgth = sectionWeightMap.get(fldLabel);
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
					Double cosSim = Word2VecMath.cosineSimilarity(normSumVec, memModel.getW2VMap().get(closestWord));
					// cosSim *= wgth;
					RccNounPhraseLabelPair tmpPair = new RccNounPhraseLabelPair(nounPhrase, closestWord, cosSim, wgth);
					pairList.add(tmpPair);
				} else if (sumVec == null) {
					LOG.info("No sum vector found for the noun phrase: " + nounPhrase);
				}
			}

		}
		resPairList = applyTopFilter(pairList);
		return resPairList;
	}

	protected List<RccNounPhraseLabelPair> applyTopFilter(List<RccNounPhraseLabelPair> pairList) {
		List<RccNounPhraseLabelPair> resPairList = new ArrayList<>();
		Collections.sort(pairList, Collections.reverseOrder());
		// top unique logic
		Set<RccNounPhraseLabelPair> uniqueTopPairs = new TreeSet<>(new RccNplpLabelComparator());
		for (RccNounPhraseLabelPair labelPair : pairList) {
			if (labelPair.getCosineSim() > CSIM_CUTOFF || uniqueTopPairs.size() < TOP_RES_LENGTH) {
				uniqueTopPairs.add(labelPair);
			} else {
				break;
			}
		}
		resPairList.addAll(uniqueTopPairs);
		Collections.sort(resPairList, Collections.reverseOrder());
		return resPairList;
	}

	// getter and setter
	public Map<String, Float> getWeightMap() {
		return sectionWeightMap;
	}

	public void setWeightMap(Map<String, Float> weightMap) {
		this.sectionWeightMap = weightMap;
	}

}
