package ccjava.diffparser;

import org.junit.Assert;

import org.junit.Test;

public class LineIntervalTest {
	@Test
	public void testFromNumbers() {
		LineInterval l = LineInterval.fromNumbers(1, 1);
		Assert.assertEquals(1, l.getFirstLineNumber());
		Assert.assertEquals(1, l.getLastLineNumber());
		l = LineInterval.fromNumbers(4, 8);
		Assert.assertEquals(4, l.getFirstLineNumber());
		Assert.assertEquals(8, l.getLastLineNumber());
		l = LineInterval.fromNumbers(8, 8);
		Assert.assertEquals(8, l.getFirstLineNumber());
		Assert.assertEquals(8, l.getLastLineNumber());
		l = LineInterval.fromNumbers(1, 2);
		Assert.assertEquals(1, l.getFirstLineNumber());
		Assert.assertEquals(2, l.getLastLineNumber());
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testFromNumbersInvalidInterval0() {
		LineInterval.fromNumbers(0, 1);
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testFromNumbersInvalidInterval1() {
		LineInterval.fromNumbers(0, 0);
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testFromNumbersInvalidInterval2() {
		LineInterval.fromNumbers(2, 1);
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testFromNumbersInvalidInterval3() {
		LineInterval.fromNumbers(-1, 3);
	}
	
	@Test
	public void testFromNumbersAndLength() {
		LineInterval l = LineInterval.fromNumberAndLength(1, 1);
		Assert.assertEquals(1, l.getFirstLineNumber());
		Assert.assertEquals(1, l.getLastLineNumber());
		l = LineInterval.fromNumberAndLength(1, 2);
		Assert.assertEquals(1, l.getFirstLineNumber());
		Assert.assertEquals(2, l.getLastLineNumber());
		l = LineInterval.fromNumberAndLength(4, 1);
		Assert.assertEquals(4, l.getFirstLineNumber());
		Assert.assertEquals(4, l.getLastLineNumber());
		l = LineInterval.fromNumberAndLength(4, 3);
		Assert.assertEquals(4, l.getFirstLineNumber());
		Assert.assertEquals(6, l.getLastLineNumber());
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testFromNumberAndLengthInvalidInterval0() {
		LineInterval.fromNumberAndLength(7, 0);
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testFromNumberAndLengthInvalidInterval1() {
		LineInterval.fromNumberAndLength(-1, 3);
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testFromNumberAndLengthInvalidInterval2() {
		LineInterval.fromNumberAndLength(2, -1);
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testFromNumberAndLengthInvalidInterval3() {
		LineInterval.fromNumberAndLength(0, 1);
	}
	
	@Test
	public void testContains() {
		Assert.assertTrue(LineInterval.fromNumbers(1, 2).contains(LineInterval.fromNumbers(1, 1)));
		Assert.assertTrue(LineInterval.fromNumbers(1, 2).contains(LineInterval.fromNumbers(2, 2)));
		Assert.assertTrue(LineInterval.fromNumbers(3, 5).contains(LineInterval.fromNumbers(4, 4)));
		Assert.assertTrue(LineInterval.fromNumbers(3, 5).contains(LineInterval.fromNumbers(3, 5)));
		
		Assert.assertFalse(LineInterval.fromNumbers(1, 2).contains(LineInterval.fromNumbers(2, 3)));
		Assert.assertFalse(LineInterval.fromNumbers(1, 2).contains(LineInterval.fromNumbers(2, 4)));
		Assert.assertFalse(LineInterval.fromNumbers(7, 11).contains(LineInterval.fromNumbers(6, 7)));
	}
	
	@Test
	public void testIntersects() {
		Assert.assertTrue(LineInterval.fromNumbers(1, 2).intersects(LineInterval.fromNumbers(2, 2)));
		Assert.assertTrue(LineInterval.fromNumbers(1, 2).intersects(LineInterval.fromNumbers(2, 5)));
		Assert.assertTrue(LineInterval.fromNumbers(4, 7).intersects(LineInterval.fromNumbers(7, 7)));
		Assert.assertTrue(LineInterval.fromNumbers(4, 7).intersects(LineInterval.fromNumbers(7, 9)));
		Assert.assertTrue(LineInterval.fromNumbers(3, 9).intersects(LineInterval.fromNumbers(8, 8)));
		Assert.assertTrue(LineInterval.fromNumbers(3, 9).intersects(LineInterval.fromNumbers(8, 9)));
		Assert.assertTrue(LineInterval.fromNumbers(3, 9).intersects(LineInterval.fromNumbers(8, 10)));
		Assert.assertTrue(LineInterval.fromNumbers(3, 9).intersects(LineInterval.fromNumbers(5, 5)));
		
		Assert.assertFalse(LineInterval.fromNumbers(1, 1).intersects(LineInterval.fromNumbers(2, 2)));
		Assert.assertFalse(LineInterval.fromNumbers(1, 1).intersects(LineInterval.fromNumbers(2, 6)));
		Assert.assertFalse(LineInterval.fromNumbers(3, 5).intersects(LineInterval.fromNumbers(6, 8)));
		Assert.assertFalse(LineInterval.fromNumbers(3, 5).intersects(LineInterval.fromNumbers(6, 6)));
		Assert.assertFalse(LineInterval.fromNumbers(6, 8).intersects(LineInterval.fromNumbers(5, 5)));
		Assert.assertFalse(LineInterval.fromNumbers(6, 8).intersects(LineInterval.fromNumbers(1, 5)));
	}
	
	@Test
	public void testEquals() {
		Assert.assertTrue(LineInterval.fromNumbers(1, 2).equals(LineInterval.fromNumbers(1, 2)));
		Assert.assertFalse(LineInterval.fromNumbers(1, 2).equals(LineInterval.fromNumbers(1, 3)));
	}
}
