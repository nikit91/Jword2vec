package upb.dice.rcc.multscr.finder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.aksw.word2vecrestful.utils.Word2VecMath;
import org.aksw.word2vecrestful.word2vec.GenWord2VecModel;
import org.aksw.word2vecrestful.word2vec.Word2VecModel;

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
public class RfmSnglrCosSimIdf extends RfmSnglrCosSim {

	protected Map<String, Double> idfWeightMap;
	protected double defaultIdfVal;
	
	public static final int TOP_RES_LENGTH = 10;
	/**
	 * Contructor to initialize instance of {@link RfmSnglrCosSim} using the given
	 * general Normalized word model, a Word2Vec custom model and a WeightMap
	 * 
	 * @param genModel  - {@link #genModel}
	 * @param memModel  - {@link #memModel}
	 * @param sectionWeightMap - {@link #sectionWeightMap}
	 * @throws IOException
	 */
	public RfmSnglrCosSimIdf(Word2VecModel genModel, GenWord2VecModel memModel,
			PublicationWordSetExtractor wordSetExtractor, Map<String, Float> sectionWeightMap, Map<String, Double> idfWeightMap, double defaultIdfVal) throws IOException {
		super(genModel, memModel, wordSetExtractor, sectionWeightMap);
		this.idfWeightMap = idfWeightMap;
		this.defaultIdfVal = defaultIdfVal;
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
					double specWgth = wgth+getIdfWgth(closestWord);
					float[] normSumVec = Word2VecMath.normalize(sumVec);
					Double cosSim = Word2VecMath.cosineSimilarity(normSumVec, memModel.getW2VMap().get(closestWord));
					RccNounPhraseLabelPair tmpPair = new RccNounPhraseLabelPair(nounPhrase, closestWord, cosSim, specWgth);
					pairList.add(tmpPair);
				} else if (sumVec == null) {
					LOG.info("No sum vector found for the noun phrase: " + nounPhrase);
				}
			}

		}
		resPairList = applyTopFilter(pairList);
		return resPairList;
	}
	@Override
	protected List<RccNounPhraseLabelPair> applyTopFilter(List<RccNounPhraseLabelPair> pairList) {
		List<RccNounPhraseLabelPair> resPairList = new ArrayList<>();
		Collections.sort(pairList, Collections.reverseOrder());
		// top unique logic
		Set<RccNounPhraseLabelPair> uniqueTopPairs = new TreeSet<>(new RccNplpLabelComparator());
		for (RccNounPhraseLabelPair labelPair : pairList) {
			if (uniqueTopPairs.size() < TOP_RES_LENGTH) {
				uniqueTopPairs.add(labelPair);
			} else {
				break;
			}
		}
		resPairList.addAll(uniqueTopPairs);
		Collections.sort(resPairList, Collections.reverseOrder());
		return resPairList;
	}
	
	protected Double getIdfWgth(String word) {
		Double resVal = idfWeightMap.get(word);
		if(resVal == null) {
			resVal = defaultIdfVal;
		}
		return resVal;
	}
	
}
