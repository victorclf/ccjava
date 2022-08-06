package ccjava.diffparser;

import org.junit.Assert;
import org.junit.Test;

public class ChangeHunkHeaderTest {
	@Test
	public void testConstructor() {
		LineInterval s1 = LineInterval.fromNumberAndLength(2, 4);
		LineInterval s2 = LineInterval.fromNumberAndLength(2, 7);
		ChangeHunkHeader ch = new ChangeHunkHeader(s1, s2);
		Assert.assertEquals(s1, ch.getOriginalSpan());
		Assert.assertEquals(s2, ch.getNewSpan());
	}
	
	@Test
	public void testNoHeading() {
		// @@ -l,s +l,s @@ optional section heading
		String s = "@@ -4,8 +4,11 @@";
		ChangeHunkHeader ch = ChangeHunkHeader.parseChangeHunkHeader(s);
		Assert.assertEquals(LineInterval.fromNumberAndLength(4, 8), ch.getOriginalSpan());
		Assert.assertEquals(LineInterval.fromNumberAndLength(4, 11), ch.getNewSpan());
		Assert.assertEquals("", ch.getHeader());
	}
	
	@Test
	public void testNoComma0() {
		// @@ -l,s +l,s @@ optional section heading
		String s = "@@ -4 +4,11 @@";
		ChangeHunkHeader ch = ChangeHunkHeader.parseChangeHunkHeader(s);
		Assert.assertEquals(LineInterval.fromNumberAndLength(4, 1), ch.getOriginalSpan());
		Assert.assertEquals(LineInterval.fromNumberAndLength(4, 11), ch.getNewSpan());
		Assert.assertEquals("", ch.getHeader());
	}
	
	@Test
	public void testNoComma1() {
		// @@ -l,s +l,s @@ optional section heading
		String s = "@@ -4,8 +5 @@";
		ChangeHunkHeader ch = ChangeHunkHeader.parseChangeHunkHeader(s);
		Assert.assertEquals(LineInterval.fromNumberAndLength(4, 8), ch.getOriginalSpan());
		Assert.assertEquals(LineInterval.fromNumberAndLength(5, 1), ch.getNewSpan());
		Assert.assertEquals("", ch.getHeader());
	}
	
	@Test
	public void testNoComma2() {
		// @@ -l,s +l,s @@ optional section heading
		String s = "@@ -2 +3 @@";
		ChangeHunkHeader ch = ChangeHunkHeader.parseChangeHunkHeader(s);
		Assert.assertEquals(LineInterval.fromNumberAndLength(2, 1), ch.getOriginalSpan());
		Assert.assertEquals(LineInterval.fromNumberAndLength(3, 1), ch.getNewSpan());
		Assert.assertEquals("", ch.getHeader());
	}
	
	@Test
	public void testWithHeading() {
		String s = "@@ -23,8 +25,124 @@ public void ha()";
		ChangeHunkHeader ch = ChangeHunkHeader.parseChangeHunkHeader(s);
		Assert.assertEquals(LineInterval.fromNumberAndLength(23, 8), ch.getOriginalSpan());
		Assert.assertEquals(LineInterval.fromNumberAndLength(25, 124), ch.getNewSpan());
		Assert.assertEquals("public void ha()", ch.getHeader());
	}
}
