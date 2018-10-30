package org.aksw.word2vecrestful;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

public class BucketAssignmentTester {
	@Test
	public void testBucketAssignment() {
		int bucketCount = 15;
		Map<Double, Integer> csimMap = new HashMap<>();
		csimMap.put(-1d, 0);
		csimMap.put(1d, 14);
		csimMap.put(0d, 6);
		csimMap.put(0.0073779183, 6);
		for (Double cosSim : csimMap.keySet()) {
			int indx = getBucketIndex(cosSim, bucketCount);
			assertEquals((int) csimMap.get(cosSim), indx);
		}
	}

	private static int getBucketIndex(double cosineSimVal, int bucketCount) {
		double bucketSize = 2.0 / bucketCount;
		int index = (int) Math.round((cosineSimVal + 1.0) / (bucketSize));
		if (index == bucketCount) {
			--index;
		}
		return index;
	}

}
