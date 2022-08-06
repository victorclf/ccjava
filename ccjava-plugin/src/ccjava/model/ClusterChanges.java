package ccjava.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.Validate;
import org.jgrapht.alg.util.UnionFind;

import ccjava.DEFINES;

public class ClusterChanges {
	private Changeset changeset;
	private String relatedDiffExtractionLastLogMessage;
	
	private Set<RelatedDiffPair> relatedDiffs;
	private List<Partition> partitions;
	
	public ClusterChanges(Changeset changeset) {
		Validate.notNull(changeset);
		
		this.changeset = changeset;
	}
	
	public List<Partition> run() {
		this.relatedDiffs = extractRelatedDiffs();
		this.partitions = partitionDiffs(this.relatedDiffs);
		return partitions;
	}
	
	public Set<RelatedDiffPair> getRelatedDiffs() {
		if (relatedDiffs == null) {
			run();
		}
		
		return Collections.unmodifiableSet(relatedDiffs);
	}
	
	public List<Partition> getPartitions() {
		if (partitions == null) {
			run();
		}
		
		return Collections.unmodifiableList(partitions);
	}

	private List<Partition> partitionDiffs(Set<RelatedDiffPair> relatedDiffs) {
		UnionFind<DiffRegion> uf = new UnionFind<DiffRegion>(this.changeset.getDiffRegions());
		for (RelatedDiffPair rdp : relatedDiffs) {
			uf.union(rdp.getFirstDiffRegion(), rdp.getSecondDiffRegion());
		}
		
		Map<DiffRegion, Set<DiffRegion>> partitionMap = new HashMap<DiffRegion, Set<DiffRegion>>();
		for (DiffRegion dr : this.changeset.getDiffRegions()) {
			DiffRegion partitionRoot = uf.find(dr);
			if (!partitionMap.containsKey(partitionRoot)) {
				partitionMap.put(partitionRoot, new HashSet<DiffRegion>());
			}
			partitionMap.get(partitionRoot).add(dr);
		}
		
		List<Partition> partitions = new ArrayList<Partition>();
		for (Set<DiffRegion> drs : partitionMap.values()) {
			partitions.add(new Partition(drs));
		}
		
		logPartitions(partitions);
		
		return partitions;
	}

	private Set<RelatedDiffPair> extractRelatedDiffs() {
		Set<RelatedDiffPair> relatedDiffs = new HashSet<RelatedDiffPair>();
		
		if (DEFINES.LOG_RELATED_DIFFS) {
			System.out.println("\n***Extracting related diffs");
		}
		
		for (DiffRegion dr1 : this.changeset.getDiffRegions()) {
			for (DiffRegion dr2 : this.changeset.getDiffRegions()) {
				if (dr1 != dr2) {
					RelatedDiffPair rdp = null;
					if (defUseRelated(dr1, dr2)) { 
						rdp = new RelatedDiffPair(dr1, dr2, RelatedDiffPair.DiffRelationType.DEF_USE);
					} else if (useUseRelated(dr1, dr2)) {
						rdp = new RelatedDiffPair(dr1, dr2, RelatedDiffPair.DiffRelationType.USE_USE);
					} else if (sameEnclosingMethodRelated(dr1, dr2)) {
						rdp = new RelatedDiffPair(dr1, dr2, RelatedDiffPair.DiffRelationType.SAME_ENCLOSING_METHOD);
					}
					
					if (rdp != null) {
						boolean relationWasNotInTheSet = relatedDiffs.add(rdp);
					
						if (DEFINES.LOG_RELATED_DIFFS) {
							if (!relationWasNotInTheSet) {
								System.out.print(this.relatedDiffExtractionLastLogMessage);
							}
						}
					}
				}
			}
		}
		
		return relatedDiffs;
	}

	private boolean defUseRelated(DiffRegion dr1, DiffRegion dr2) {
		for (Use u : dr1.getUsesInside()) {
			if (dr2.contains(u.getAssociatedDefinition())) {
				logDefUseRelated(dr2, dr1, u.getAssociatedDefinition(), u);
				return true;
			}
		}
		
		for (Use u : dr2.getUsesInside()) {
			if (dr1.contains(u.getAssociatedDefinition())) {
				logDefUseRelated(dr1, dr2, u.getAssociatedDefinition(), u);
				return true;
			}
		}
		
		return false;
	}

	private boolean useUseRelated(DiffRegion dr1, DiffRegion dr2) {
		for (Use u1 : dr1.getUsesInside()) {
			if (u1.getAssociatedDefinition() != null && !u1.getAssociatedDefinition().isInsideADiffRegion()) {
				for (Use u2 : dr2.getUsesInside()) {
					if (u1.getAssociatedDefinition().equals(u2.getAssociatedDefinition())) {
						logUseUseRelated(dr1, dr2, u1, u2);
						return true;
					}
				}
			}
		}
		
		return false;
	}
	
	private boolean sameEnclosingMethodRelated(DiffRegion dr1, DiffRegion dr2) {
		if (dr1.sameEnclosingMethod(dr2)) {
			logSameEnclosingMethodRelated(dr1, dr2);
			return true;
		}
		return false;
	}
	
	private void logDefUseRelated(DiffRegion dr1, DiffRegion dr2, Definition d, Use u) {
		if (DEFINES.LOG_RELATED_DIFFS) {
			this.relatedDiffExtractionLastLogMessage = String.format("def-use relation: %s <-> %s / %s <-> %s%n", dr1, dr2, d, u);
		}
	}
	
	private void logUseUseRelated(DiffRegion dr1, DiffRegion dr2, Use u1, Use u2) {
		if (DEFINES.LOG_RELATED_DIFFS) {
			this.relatedDiffExtractionLastLogMessage = String.format("use-use relation: %s <-> %s / %s <-> %s%n", dr1, dr2, u1, u2);
		}
	}
	
	private void logSameEnclosingMethodRelated(DiffRegion dr1, DiffRegion dr2) {
		if (DEFINES.LOG_RELATED_DIFFS) {
			this.relatedDiffExtractionLastLogMessage = String.format("same enclosing method relation: %s <-> %s %n", dr1, dr2);
		}
	}
	
	private void logPartitions(List<Partition> partitions) {
		if (DEFINES.LOG_PARTITIONS) {
			System.out.println("\n***Calculating partitions");
			for (Partition p : partitions) {
				System.out.println(p);
			}
		}
	}
}
