package ccjava.diffparser;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

public class GitDiffParserTest {
	@Test
	public void testToyExample() throws IOException {
		List<UnifiedDiffRegion> udrList = new GitDiffParser(Paths.get("test/examples/toy_example")).parse();
		
		for (UnifiedDiffRegion udr : udrList) {
			Assert.assertTrue(udr.getPath().startsWith("somecompany/"));
		}
		
		Assert.assertEquals(3, udrList.size());
		Assert.assertTrue(containsUnifiedDiffRegion(udrList, "Person.java", LineInterval.fromNumbers(6, 6)));
		Assert.assertTrue(containsUnifiedDiffRegion(udrList, "Person.java", LineInterval.fromNumbers(17, 17)));
		Assert.assertTrue(containsUnifiedDiffRegion(udrList, "Person.java", LineInterval.fromNumbers(20, 29)));
	}
	
	@Test
	public void testHibernateExample() throws IOException {
		List<UnifiedDiffRegion> udrList = new GitDiffParser(Paths.get("test/examples/hibernate-pull-req-894")).parse();
		
		Assert.assertFalse(udrList.isEmpty());
		
		for (UnifiedDiffRegion udr : udrList) {
			Assert.assertTrue(udr.getPath().startsWith("hibernate-envers/"));
		}
		
		Assert.assertTrue(containsUnifiedDiffRegion(udrList, "configuration/EnversSettings.java", LineInterval.fromNumbers(97, 102)));
		Assert.assertTrue(containsUnifiedDiffRegion(udrList, "configuration/EnversSettings.java", LineInterval.fromNumbers(140, 144)));
	}
	
	@Test
	public void testCreatedFileExample() throws IOException {
		List<UnifiedDiffRegion> udrList = new GitDiffParser(Paths.get("test/examples/hibernate-pull-req-894")).parse();
		
		Assert.assertTrue(containsUnifiedDiffRegion(udrList, "main/java/org/hibernate/envers/internal/entities/mappergenerator/CollectionMapperResolver.java", LineInterval.fromNumberAndLength(1, 17)));
	}
	
	private boolean containsUnifiedDiffRegion(List<UnifiedDiffRegion> udrList, String partialPath, LineInterval lines) {
		for (UnifiedDiffRegion udr : udrList) {
			if (udr.getPath().contains(partialPath) && udr.getLines().equals(lines)) {
				return true;
			}
		}
		
		return false;
	}
}
