package ccjava.model;

import java.util.HashSet; 

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import static org.mockito.Mockito.*;

import ccjava.diffparser.LineInterval;

public class RelatedDiffPairTest {
	private DiffRegion dr1;
	private DiffRegion dr2;
	private RelatedDiffPair rdp1;
	private RelatedDiffPair rdp2;

	@Before
	public void setUp() {
		SourceFile sf1 = new SourceFile("blah.java", "blah.java", mock(LineToCharacterIntervalConverter.class));
		
		this.dr1 = new DiffRegion(sf1, CharacterInterval.fromPositions(5, 40),
				LineInterval.fromNumbers(2, 7), new HashSet<Definition>(), new HashSet<Use>());
		this.dr2 = new DiffRegion(sf1, CharacterInterval.fromPositions(120, 317),
				LineInterval.fromNumbers(12, 18), new HashSet<Definition>(), new HashSet<Use>());
		this.rdp1 = new RelatedDiffPair(dr1, dr2, RelatedDiffPair.DiffRelationType.USE_USE);
		this.rdp2  = new RelatedDiffPair(dr2, dr1, RelatedDiffPair.DiffRelationType.USE_USE);
	}
	
	@Test
	public void testEqualsDiffRegionOrderDoesNotMatter() {
		Assert.assertEquals(rdp1, rdp2);
	}
	
	@Test
	public void testEqualsDifferentDiffRegions() {
		RelatedDiffPair rdp = new RelatedDiffPair(dr1, dr1, RelatedDiffPair.DiffRelationType.USE_USE);
		Assert.assertNotEquals(rdp1, rdp);
	}
	
	@Test
	public void testHashCodeDiffRegionOrderDoesNotMatter() {
		Assert.assertEquals(rdp1.hashCode(), rdp2.hashCode());
	}
	
	@Test
	public void testHashCodeConsistency() {
		Assert.assertEquals(rdp1.hashCode(), rdp1.hashCode());
		Assert.assertEquals(rdp2.hashCode(), rdp2.hashCode());
	}
}
