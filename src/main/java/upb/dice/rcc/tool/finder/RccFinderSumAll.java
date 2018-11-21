package upb.dice.rcc.tool.finder;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.aksw.word2vecrestful.utils.Word2VecMath;
import org.aksw.word2vecrestful.word2vec.GenWord2VecModel;
import org.aksw.word2vecrestful.word2vec.Word2VecModel;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import upb.dice.rcc.tool.PublicationWordSetExtractor;
import upb.dice.rcc.tool.RccNounPhraseLabelPair;
import upb.dice.rcc.tool.RccUtil;

/**
 * Implementation for {@link RccFinder}. This class sums up vectors of all the
 * noun phrases in the publication and then finds the closest entry in the
 * custom model to that vector.
 * 
 * @author nikitsrivastava
 *
 */
public class RccFinderSumAll extends RccFinder {

	public static Logger LOG = LogManager.getLogger(RccFinderSumAll.class);
	/**
	 * @see RccFinder#RccFinder(Word2VecModel, GenWord2VecModel)
	 */
	/*
	 * public RccFinderSumAll(Word2VecModel genModel, GenWord2VecModel memModel)
	 * throws IOException { super(genModel, memModel); }
	 */

	/**
	 * @see RccFinder#RccFinder(Word2VecModel, GenWord2VecModel,
	 *      PublicationWordSetExtractor)
	 */
	public RccFinderSumAll(Word2VecModel genModel, GenWord2VecModel memModel,
			PublicationWordSetExtractor wordSetExtractor) throws IOException {
		super(genModel, memModel, wordSetExtractor);
	}

	@Override
	public RccNounPhraseLabelPair findClosestResearchField(File pubFile) throws IOException {
		return this.findClosestResearchField(pubFile, this.wordSetExtractor);
	}

	@Override
	public RccNounPhraseLabelPair findClosestResearchField(File pubFile, PublicationWordSetExtractor wordSetExtractor)
			throws IOException {
		Map<String, List<String>> fldWordsMap = wordSetExtractor.extractPublicationWordSet(pubFile);
		List<String> wordList = new ArrayList<>();
		for (List<String> nounPhraseList : fldWordsMap.values()) {
			for (String nounPhrase : nounPhraseList) {
				wordList.addAll(RccUtil.fetchAllWordTokens(nounPhrase, genModel));
			}
		}
		RccNounPhraseLabelPair tmpPair = null;
		float[] sumVec = RccUtil.getSumVector(wordList, genModel);
		if (sumVec != null) {
			float[] normSumVec = Word2VecMath.normalize(sumVec);
			String idStr = memModel.getClosestEntry(normSumVec);
			Double cosSim = Word2VecMath.cosineSimilarityNormalizedVecs(normSumVec, memModel.getW2VMap().get(idStr));
			tmpPair = new RccNounPhraseLabelPair(pubFile.getName(), idStr, cosSim, 1);
		} else {
			LOG.info("No sum vector found for the words: " + wordList);
		}
		return tmpPair;
	}

}
