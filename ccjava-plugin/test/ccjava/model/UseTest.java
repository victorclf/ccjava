package ccjava.model;

import org.junit.Before;
import org.junit.Test;

import static org.mockito.Mockito.*;
import static org.junit.Assert.*;

public class UseTest {
	private Use u1;
	private Use u2;
	private Use u3;
	private Use u1DifferentName;

	@Before
	public void setUp() {
		SourceFile sf1 = new SourceFile("sf1.java", "sf1.java", mock(LineToCharacterIntervalConverter.class));
		SourceFile sf2 = new SourceFile("sf2.java", "sf2.java", mock(LineToCharacterIntervalConverter.class));
		this.u1 = new Use("var1", sf1, CharacterInterval.fromPositions(20, 200), "bindkey", null);
		this.u1DifferentName = new Use("varrrr", sf1, CharacterInterval.fromPositions(20, 200), "bindkey", null);
		this.u2 = new Use("var2", sf1, CharacterInterval.fromPositions(500, 555), "bindkey", null);
		this.u3 = new Use("var1", sf2, CharacterInterval.fromPositions(20, 200), "bindkey", null);
	}
	
	@Test
	public void testEquals() {
		assertEquals(u1, u1);
	}
	
	@Test
	public void testEqualsDifferentName() {
		assertEquals(u1, u1DifferentName);
	}
	
	@Test
	public void testEqualsDifferentPosition() {
		assertNotEquals(u1, u2);
	}
	
	@Test
	public void testEqualsDifferentFile() {
		assertNotEquals(u1, u3);
	}
	
	@Test
	public void testHashCodeDifferentName() {
		assertEquals(u1.hashCode(), u1DifferentName.hashCode());
	}
	
	@Test
	public void testHashCodeConsistency() {
		assertEquals(u1.hashCode(), u1.hashCode());
		assertEquals(u2.hashCode(), u2.hashCode());
	}
}
