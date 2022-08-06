package ccjava.model;

import java.util.Collections;
import java.util.Set;

import org.apache.commons.lang3.Validate;

public class Partition {
	private static int NEXT_ID = 1;
	private int id;
	private Set<DiffRegion> diffRegions;

	public Partition(Set<DiffRegion> diffRegions) {
		Validate.notEmpty(diffRegions);
		
		this.diffRegions = diffRegions;
	}
	
	public int getId() {
		if (id <= 0) {
			this.id = NEXT_ID++;
		}
		
		return this.id;
	}
	
	public Set<DiffRegion> getDiffRegions() {
		return Collections.unmodifiableSet(this.diffRegions);
	}
	
	public boolean isTrivial() {
		return diffRegions.size() <= 1 
				|| allDiffRegionsHaveTheSameEnclosingMethod();
	}

	private boolean allDiffRegionsHaveTheSameEnclosingMethod() {
		DiffRegion firstDiffRegion = null;
		for (DiffRegion dr : this.diffRegions) {
			if (firstDiffRegion == null) {
				firstDiffRegion = dr;
			} else {
				if (!dr.sameEnclosingMethod(firstDiffRegion)) {
					return false;
				}
			}
		}
		
		return true;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((diffRegions == null) ? 0 : diffRegions.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Partition other = (Partition) obj;
		if (diffRegions == null) {
			if (other.diffRegions != null)
				return false;
		} else if (!diffRegions.equals(other.diffRegions))
			return false;
		return true;
	}

	@Override
	public String toString() {
		StringBuilder partitionStr = new StringBuilder();
		partitionStr.append(String.format("Partition %d (%s):%n", getId(), isTrivial() ? "trivial" : "non-trivial"));
		for (DiffRegion d : this.diffRegions) {
			partitionStr.append(d).append(String.format("%n"));
		}
		return partitionStr.toString();
	}
}
