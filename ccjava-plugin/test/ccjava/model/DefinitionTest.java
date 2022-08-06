package ccjava.model;

import org.junit.Before;
import org.junit.Test;

import static org.mockito.Mockito.*;
import static org.junit.Assert.*;

public class DefinitionTest {
	private Definition d1;
	private Definition d2;
	private Definition d3;
	private Definition d1DifferentName;

	@Before
	public void setUp() {
		SourceFile sf1 = new SourceFile("sf1.java", "sf1.java", mock(LineToCharacterIntervalConverter.class));
		SourceFile sf2 = new SourceFile("sf2.java", "sf2.java", mock(LineToCharacterIntervalConverter.class));
		this.d1 = new Definition("var1", sf1, CharacterInterval.fromPositions(20, 200), "bindkey");
		this.d1DifferentName = new Definition("varrrr", sf1, CharacterInterval.fromPositions(20, 200), "bindkey");
		this.d2 = new Definition("var2", sf1, CharacterInterval.fromPositions(500, 555), "bindkey");
		this.d3 = new Definition("var1", sf2, CharacterInterval.fromPositions(20, 200), "bindkey");
	}
	
	@Test
	public void testEquals() {
		assertEquals(d1, d1);
	}
	
	@Test
	public void testEqualsDifferentName() {
		assertEquals(d1, d1DifferentName);
	}
	
	@Test
	public void testEqualsDifferentPosition() {
		assertNotEquals(d1, d2);
	}
	
	@Test
	public void testEqualsDifferentFile() {
		assertNotEquals(d1, d3);
	}
	
	@Test
	public void testHashCodeDifferentName() {
		assertEquals(d1.hashCode(), d1DifferentName.hashCode());
	}
	
	@Test
	public void testHashCodeConsistency() {
		assertEquals(d1.hashCode(), d1.hashCode());
		assertEquals(d2.hashCode(), d2.hashCode());
	}
}
