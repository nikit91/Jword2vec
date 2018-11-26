package upb.dice.rcc.tool;

import java.util.Comparator;

public class RccNplpLabelComparator implements Comparator<RccNounPhraseLabelPair>{

	@Override
	public int compare(RccNounPhraseLabelPair o1, RccNounPhraseLabelPair o2) {
		return o1.getClosestWord().compareTo(o2.getClosestWord());
	}

}
