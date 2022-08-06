package ccjava.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.nio.file.Paths;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.junit.Test;

import ccjava.TestUtil;
import ccjava.diffparser.GitDiffParser;
import ccjava.diffparser.LineInterval;
import ccjava.diffparser.UnifiedDiffRegion;
import ccjava.javaparser.Parser;

public class ClusterChangesTest {
	private static class DiffRegionMatcher {
		public String partialPath;
		public LineInterval lineSpan;
		
		public DiffRegionMatcher(String partialPath, int firstLineNumber, int lastLineNumber) {
			this.partialPath = partialPath;
			this.lineSpan = LineInterval.fromNumbers(firstLineNumber, lastLineNumber);
		}
		
		public boolean match(DiffRegion d) {
			return d.getSourceFile().getPath().contains(partialPath) 
					&& this.lineSpan.equals(d.getLineSpan());
		}
	}
	
	@Test
	public void testPartitioningToyExample() throws Exception {
		List<Partition> p = partition("test/examples/toy_example");
		
		assertTrue(p.size() == 2);
		assertTrue(containsPartition(p, dm("Person", 6, 6), dm("Person", 24, 27), dm("Person", 28, 29) ));
		assertTrue(containsPartition(p, dm("Person", 17, 17), dm("Person", 20, 23) ));
	}
	
	@Test
	public void testPartitioningToyExampleMultiFile2() throws Exception {
		List<Partition> p = partition("test/examples/toy_example_multi_file_2");
		
//		assertEquals(11, p.size());
		
		// passport
		assertTrue(containsPartition(p, dm("Person", 7, 7), dm("Person", 28, 33), 
				dm("Passport", 6, 6), dm("Passport", 7, 7) ));
		assertFalse(containsPartition(p, dm("Person", 28, 33), 
				dm("Passport", 6, 6), dm("Passport", 7, 7) ));
		assertFalse(containsPartition(p, dm("Person", 7, 7)));
		assertFalse(containsPartition(p, dm("Person", 7, 365)));
		
		// entity lastname
		assertTrue(containsPartition(p, dm("Person", 9, 9), dm("Person", 24, 25), 
				dm("Entity", 6, 10), dm("Entity", 3, 5)  ));
		
		assertTrue(containsPartition(p, dm("Person", 20, 20) ));
		assertFalse(containsPartition(p, dm("Country", 1, 2) ));
		assertTrue(containsPartition(p, dm("Country", 3, 7) ));
		assertFalse(containsPartition(p, dm("Entity", 1, 1) ));
		assertFalse(containsPartition(p, dm("Passport", 1, 4) ));
		assertTrue(containsPartition(p, dm("Passport", 5, 5) ));
		assertTrue(containsPartition(p, dm("Passport", 8, 8) ));
		assertTrue(containsPartition(p, dm("Passport", 9, 10) ));
		assertFalse(containsPartition(p, dm("Person", 3, 5) ));
	}
	
	@Test
	public void testExtractRelatedDiffsToyExample() throws Exception {
		Set<RelatedDiffPair> rds = extractRelatedDiffs("test/examples/toy_example");
		
		assertEquals(3, rds.size());
		assertTrue(containsRelatedDiffPair(rds, "Person.java", ln(17, 17), "Person.java", ln(20, 23)));
		assertTrue(containsRelatedDiffPair(rds, "Person.java", ln(6, 6), "Person.java", ln(24, 27)));
		assertTrue(containsRelatedDiffPair(rds, "Person.java", ln(6, 6), "Person.java", ln(28, 29)));
	}

	@Test
	public void testExtractRelatedDiffsHibernateExampleDefUse() throws Exception {
		Set<RelatedDiffPair> rds = extractRelatedDiffs("test/examples/hibernate-pull-req-894");
		
		assertTrue(containsRelatedDiffPair(rds, "MiddleRelatedComponentMapper.java", ln(37, 37),
				"MiddleRelatedComponentMapper.java", ln(59, 69)));
		assertTrue(containsRelatedDiffPair(rds, "MiddleRelatedComponentMapper.java", ln(37, 37),
				"MiddleRelatedComponentMapper.java", ln(70, 72)));
		assertFalse(containsRelatedDiffPair(rds, "MiddleRelatedComponentMapper.java", ln(37, 38),
				"MiddleRelatedComponentMapper.java", ln(70, 72)));
		assertFalse(containsRelatedDiffPair(rds, "MiddleRelatedComponentMapper.java", ln(37, 37),
				"MiddleRelatedComponentMapper.java", ln(70, 73)));
	}
	
	@Test
	public void testExtractRelatedDiffsHibernateExampleSameEnclosingMethod() throws Exception {
		Set<RelatedDiffPair> rds = extractRelatedDiffs("test/examples/hibernate-pull-req-894");
		
		assertTrue(containsRelatedDiffPair(rds, "EntitiesConfigurator.java", ln(125, 127),
				"EntitiesConfigurator.java", ln(131, 133)));
		assertTrue(containsRelatedDiffPair(rds, "EntitiesConfigurator.java", ln(125, 127),
				"EntitiesConfigurator.java", ln(146, 148)));
		assertTrue(containsRelatedDiffPair(rds, "EntitiesConfigurator.java", ln(131, 133),
				"EntitiesConfigurator.java", ln(146, 148)));
	}
	
	// Requires at least -Xmx1024m (512m is not enough)
//	@Test
//	public void testExtractRelatedDiffsHibernateExampleComplicatedDirStructure() throws Exception {
//		Set<RelatedDiffPair> rds = extractRelatedDiffs("test/examples/hibernate-pull-req-738");
//	}
	
	@Test
	public void testExtractRelatedDiffsMockitoExampleDefUse() throws Exception {
		Set<RelatedDiffPair> rds = extractRelatedDiffs("test/examples/mockito-pull-req-192");
	}
	
	private LineInterval ln(int a, int b) {
		return LineInterval.fromNumbers(a, b);		
	}
	
	private DiffRegionMatcher dm(String partialPath, int firstLineNumber, int lastLineNumber) {
		return new DiffRegionMatcher(partialPath, firstLineNumber, lastLineNumber);
	}
	
	private boolean containsRelatedDiffPair(Collection<RelatedDiffPair> relatedDiffs, String partialPath1, LineInterval span1, String partialPath2, LineInterval span2) {
		return containsRelatedDiffPairOrdered(relatedDiffs, partialPath1, span1, partialPath2, span2)
				|| containsRelatedDiffPairOrdered(relatedDiffs, partialPath2, span2, partialPath1, span1);
	}
	
	private boolean containsRelatedDiffPairOrdered(Collection<RelatedDiffPair> relatedDiffs, String partialPath1, LineInterval span1, String partialPath2, LineInterval span2) {
		for (RelatedDiffPair rdp : relatedDiffs) {
			if (rdp.getFirstDiffRegion().getSourceFile().getPath().contains(partialPath1)
				&& rdp.getSecondDiffRegion().getSourceFile().getPath().contains(partialPath2)
				&& rdp.getFirstDiffRegion().getLineSpan().equals(span1) 
				&& rdp.getSecondDiffRegion().getLineSpan().equals(span2)) {
					return true;
				}
		}
		
		return false;
	}
	
	private boolean containsPartition(List<Partition> partitions, DiffRegionMatcher... dms) {
		for (Partition p : partitions) {
			if (partitionContainsOneDiffRegion(p, dms)) {
				return partitionContainsAllDiffRegions(p, dms);
			}
		}
		
		return false;
	}
	
	private boolean partitionContainsAllDiffRegions(Partition p, DiffRegionMatcher[] dms) {
		if (dms.length != p.getDiffRegions().size()) {
			return false;
		}
		
		diffRegionMatcherLoop:
		for (DiffRegionMatcher dm : dms) {
			for (DiffRegion dr : p.getDiffRegions()) {
				if (dm.match(dr)) {
					continue diffRegionMatcherLoop; // dm is inside the partition, check next dm
				}
			}
			
			return false; // dm isnt inside the partition, abort search
		}
		
		return true; // all dms are inside the partition
	}

	private boolean partitionContainsOneDiffRegion(Partition p, DiffRegionMatcher[] drIds) {
		for (DiffRegionMatcher dm : drIds) {
			for (DiffRegion dr : p.getDiffRegions()) {
				if (dm.match(dr)) {
					return true;
				}
			}
		}
		
		return false;
	}

	private List<Partition> partition(String sourceDir) throws Exception {
		Changeset cs = parseSourceAndDiffs(sourceDir);
		List<Partition> partition = new ClusterChanges(cs).run();
		return partition;
	}
	
	@SuppressWarnings("unchecked")
	private Set<RelatedDiffPair> extractRelatedDiffs(String sourceDir) throws Exception {
		Changeset cs = parseSourceAndDiffs(sourceDir);
		ClusterChanges cc = new ClusterChanges(cs);
		Set<RelatedDiffPair> relatedDiffs = (Set<RelatedDiffPair>) TestUtil.invokeMethod(cc, "extractRelatedDiffs", 0);
		return relatedDiffs;
	}
	
	private Changeset parseSourceAndDiffs(String sourceDir) throws Exception {
		Changeset changeset = new Parser(sourceDir).parse();
		List<UnifiedDiffRegion> udrList = new GitDiffParser(Paths.get(sourceDir)).parse();
		changeset.addDiffRegions(udrList);
		return changeset;
	}
}
