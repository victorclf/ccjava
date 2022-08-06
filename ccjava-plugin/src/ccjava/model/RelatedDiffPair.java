package ccjava.model;

import org.apache.commons.lang3.Validate;

public class RelatedDiffPair {
	public static enum DiffRelationType {
		DEF_USE,
		USE_USE,
		SAME_ENCLOSING_METHOD,
	}
	
	private static int NEXT_ID = 1;
	private int id;
	private DiffRegion dr1;
	private DiffRegion dr2;
	private DiffRelationType relationType;
	
	public RelatedDiffPair(DiffRegion dr1, DiffRegion dr2, DiffRelationType relationType) {
		Validate.notNull(dr1);
		Validate.notNull(dr2);
		
		this.dr1 = dr1;
		this.dr2 = dr2;
		this.relationType = relationType;
	}
	
	public int getId() {
		if (id <= 0) {
			this.id = NEXT_ID++;
		}
		
		return this.id;
	}

	public DiffRegion getFirstDiffRegion() {
		return dr1;
	}

	public DiffRegion getSecondDiffRegion() {
		return dr2;
	}
	
	public DiffRelationType getRelationType() {
		return relationType;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		int dr1Hashcode = (dr1 == null) ? 0 : dr1.hashCode();
		int dr2Hashcode = (dr2 == null) ? 0 : dr2.hashCode();
		result = prime * result + (dr1Hashcode + dr2Hashcode);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		
		if (obj == null) {
			return false;
		}
		
		if (!(obj instanceof RelatedDiffPair)) {
			return false;
		}
		
		RelatedDiffPair other = (RelatedDiffPair) obj;
		
		return equalsDiffRegions(dr1, dr2, other.dr1, other.dr2) 
				|| equalsDiffRegions(dr1, dr2, other.dr2, other.dr1);
	}
	
	private boolean equalsDiffRegions(DiffRegion dr1, DiffRegion dr2, DiffRegion otherDR1, DiffRegion otherDR2) {
		if (dr1 == null) {
			if (otherDR1 != null) {
				return false;
			}
		} else if (!dr1.equals(otherDR1)) {
			return false;
		}
		
		if (dr2 == null) {
			if (otherDR2 != null) {
				return false;
			}
		} else if (!dr2.equals(otherDR2)) {
			return false;
		}
		
		return true;
	}
}
