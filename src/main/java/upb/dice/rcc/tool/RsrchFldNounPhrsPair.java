package upb.dice.rcc.tool;

public class RsrchFldNounPhrsPair implements Comparable<RsrchFldNounPhrsPair> {

	private String nounPhrase;
	private String closestWord;
	private double cosineSim;

	public RsrchFldNounPhrsPair(String nounPhrase, String closestWord, double cosineSim) {
		super();
		this.nounPhrase = nounPhrase;
		this.closestWord = closestWord;
		this.cosineSim = cosineSim;
	}

	public String getNounPhrase() {
		return nounPhrase;
	}

	public void setNounPhrase(String nounPhrase) {
		this.nounPhrase = nounPhrase;
	}

	public String getClosestWord() {
		return closestWord;
	}

	public void setClosestWord(String closestWord) {
		this.closestWord = closestWord;
	}

	public double getCosineSim() {
		return cosineSim;
	}

	public void setCosineSim(double cosineSim) {
		this.cosineSim = cosineSim;
	}

	@Override
	public String toString() {
		return "RsrchFldNounPhrsPair [nounPhrase=" + nounPhrase + ", closestWord=" + closestWord + ", cosineSim="
				+ cosineSim + "]";
	}

	@Override
	public int compareTo(RsrchFldNounPhrsPair other) {
		double simDiff = this.cosineSim - other.cosineSim;
		int retVal = 0;
		if (simDiff > 0) {
			retVal = 1;
		} else if (simDiff < 0) {
			retVal = -1;
		}
		return retVal;
	}

}
