package upb.dice.rcc.tool.finder;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.aksw.word2vecrestful.utils.Word2VecMath;
import org.aksw.word2vecrestful.word2vec.GenWord2VecModel;
import org.aksw.word2vecrestful.word2vec.Word2VecModel;

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
	/**
	 * @see RccFinder#RccFinder(Word2VecModel, GenWord2VecModel)
	 */
	public RccFinderSumAll(Word2VecModel genModel, GenWord2VecModel memModel) throws IOException {
		super(genModel, memModel);
	}

	@Override
	public RccNounPhraseLabelPair findClosestResearchField(File pubFile) throws IOException {
		Map<String, List<String>> fldWordsMap = wordSetExtractor.extractPublicationWordSet(pubFile);
		List<String> wordList = new ArrayList<>();
		for (List<String> wordEntries : fldWordsMap.values()) {
			wordList.addAll(wordEntries);
		}
		float[] sumVec = RccUtil.getSumVector(wordList, genModel);
		float[] normSumVec = Word2VecMath.normalize(sumVec);
		String idStr = memModel.getClosestEntry(normSumVec);
		Double cosSim = Word2VecMath.cosineSimilarityNormalizedVecs(normSumVec, memModel.getW2VMap().get(idStr));
		RccNounPhraseLabelPair tmpPair = new RccNounPhraseLabelPair(pubFile.getName(), idStr, cosSim);
		return tmpPair;
	}

}
