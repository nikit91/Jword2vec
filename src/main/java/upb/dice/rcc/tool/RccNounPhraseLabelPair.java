package upb.dice.rcc.tool;

public class RccNounPhraseLabelPair implements Comparable<RccNounPhraseLabelPair> {

	private String nounPhrase;
	private String closestWord;
	private double cosineSim;
	private double wgthVal;
	private double cosineSimWgthd;
	private float[] sumVec;

	public RccNounPhraseLabelPair(String nounPhrase, String closestWord, double cosineSim, double wgthVal) {
		super();
		this.nounPhrase = nounPhrase;
		this.closestWord = closestWord;
		this.cosineSim = cosineSim > 1 ? 1.0 : cosineSim;
		this.wgthVal = wgthVal;
		this.cosineSimWgthd = this.cosineSim * this.wgthVal;
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
		return "RccNounPhraseLabelPair [nounPhrase=" + nounPhrase + ", closestWord=" + closestWord + ", cosineSim="
				+ cosineSim + ", wgthVal=" + wgthVal + ", cosineSimWgthd=" + cosineSimWgthd + "]";
	}

	public double getWgthVal() {
		return wgthVal;
	}

	public void setWgthVal(double wgthVal) {
		this.wgthVal = wgthVal;
	}

	public double getCosineSimWgthd() {
		return cosineSimWgthd;
	}

	public void setCosineSimWgthd(double cosineSimWgthd) {
		this.cosineSimWgthd = cosineSimWgthd;
	}

	@Override
	public int compareTo(RccNounPhraseLabelPair other) {
		double simDiff = this.cosineSimWgthd - other.cosineSimWgthd;
		int retVal = 0;
		if (simDiff > 0) {
			retVal = 1;
		} else if (simDiff < 0) {
			retVal = -1;
		}
		return retVal;
	}

	public float[] getSumVec() {
		return sumVec;
	}

	public void setSumVec(float[] sumVec) {
		this.sumVec = sumVec;
	}

}
