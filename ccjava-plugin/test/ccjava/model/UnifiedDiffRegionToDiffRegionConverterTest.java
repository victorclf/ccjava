package ccjava.model;

import java.nio.file.Paths;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

import ccjava.diffparser.GitDiffParser;
import ccjava.diffparser.UnifiedDiffRegion;
import ccjava.javaparser.Parser;

public class UnifiedDiffRegionToDiffRegionConverterTest {
	@Test
	public void testToyExample() throws Exception {
		Changeset cs = parseSourceAndDiffs("test/examples/toy_example");
		Set<DiffRegion> drs = cs.getDiffRegions();
		
		for (DiffRegion dr : drs) {
			Assert.assertTrue(dr.getSourceFile().getPath().startsWith("somecompany/"));
		}
		
		Assert.assertTrue(containsDiffRegion(drs, "Person.java", 115, 135));
		Assert.assertTrue(containsDiffRegion(drs, "Person.java", 331, 355));
		Assert.assertTrue(containsDiffRegion(drs, "Person.java", 363, 450));
		Assert.assertTrue(containsDiffRegion(drs, "Person.java", 451, 503));
		Assert.assertTrue(containsDiffRegion(drs, "Person.java", 504, 561));
	}
	
	@Test
	public void testHibernateExample() throws Exception {
		Changeset cs = parseSourceAndDiffs("test/examples/hibernate-pull-req-894");
		Set<DiffRegion> drs = cs.getDiffRegions();
		
		Assert.assertFalse(containsDiffRegion(drs, "internal/EntitiesConfigurator.java", 1102, 1286));
		Assert.assertFalse(containsDiffRegion(drs, "internal/EntitiesConfigurator.java", 2009, 2096));
		Assert.assertFalse(containsDiffRegion(drs, "internal/EntitiesConfigurator.java", 2323, 2514));
		Assert.assertTrue(containsDiffRegion(drs, "internal/EntitiesConfigurator.java", 2852, 2914));
		Assert.assertTrue(containsDiffRegion(drs, "internal/EntitiesConfigurator.java", 4327, 4429));
		Assert.assertTrue(containsDiffRegion(drs, "internal/EntitiesConfigurator.java", 5653, 5807));
		Assert.assertTrue(containsDiffRegion(drs, "internal/EntitiesConfigurator.java", 5955, 6104));
		Assert.assertTrue(containsDiffRegion(drs, "internal/EntitiesConfigurator.java", 6367, 6509));
	}
	
	@Test
	public void testIrrelevantDiffRegions() throws Exception {
		Changeset cs = parseSourceAndDiffs("test/examples/irrelevantDiffRegions/");
		Set<DiffRegion> drs = cs.getDiffRegions();
		
		System.err.println("### DIFFS ###");
		for (DiffRegion dr : drs) {
			System.err.printf("l(%d, %d) / c(%d, %d) @ %s%n", dr.getLineSpan().getFirstLineNumber(),
					dr.getLineSpan().getLastLineNumber(), dr.getCharacterSpan().getFirstCharacterPosition(),
					dr.getCharacterSpan().getLastCharacterPosition(), dr.getSourceFile().getPath());
		}
		
		Assert.assertTrue(containsDiffRegion(drs, "com/cool/C.java", 49, 184));
		Assert.assertTrue(containsDiffRegion(drs, "com/cool/C.java", 185, 339));
		Assert.assertTrue(containsDiffRegion(drs, "com/cool/C.java", 340, 367));
		Assert.assertFalse(containsDiffRegionIntersecting(drs, "com/cool/C.java", 0, 48));
		
		Assert.assertTrue(containsDiffRegion(drs, "com/cool/I.java", 220, 257));
		Assert.assertFalse(containsDiffRegionIntersecting(drs, "com/cool/I.java", 0, 219));
		Assert.assertFalse(containsDiffRegionIntersecting(drs, "com/cool/I.java", 258, 260));
		
		Assert.assertFalse(containsDiffRegionIntersecting(drs, "com/cool/Testable.java", 0, 165));
	}
	
	private Changeset parseSourceAndDiffs(String sourceDir) throws Exception {
		Changeset changeset = new Parser(sourceDir).parse();
		
		List<UnifiedDiffRegion> udrList = new GitDiffParser(Paths.get(sourceDir)).parse();
		
		changeset.addDiffRegions(udrList);
		return changeset;
	}
	
	private boolean containsDiffRegionIntersecting(Collection<DiffRegion> drs, String partialPath, int startPos, int endPos) {
		CharacterInterval interval = CharacterInterval.fromPositions(startPos,  endPos);
		
		for (DiffRegion dr : drs) {
			if (dr.getSourceFile().getPath().contains(partialPath)
					&& dr.getCharacterSpan().intersects(interval)) {
				return true;
			}
		}
		
		return false;
	}
	
	private boolean containsDiffRegion(Collection<DiffRegion> drs, String partialPath, int startPos, int endPos) {
		for (DiffRegion dr : drs) {
			if (dr.getSourceFile().getPath().contains(partialPath)
					&& startPos == dr.getCharacterSpan().getFirstCharacterPosition() 
					&& endPos == dr.getCharacterSpan().getLastCharacterPosition()) {
				return true;
			}
		}
		
		return false;
	}
}
