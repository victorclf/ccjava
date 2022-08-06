package ccjava.parser;

import org.junit.Assert;
import org.junit.Test;

import ccjava.DEFINES;
import ccjava.javaparser.Parser;
import ccjava.javaparser.exception.InvalidPackageNameException;

public class ParserTest {
	@Test()
	public void testSourceCodeWithInvalidPackageName() {
		if (DEFINES.FAIL_ON_ERROR) {
			try {
				new Parser("test/examples/invalidPackageName").parse();
				Assert.fail("no exception was thrown!");
			} catch (InvalidPackageNameException e) {
				// ok
			} catch(Exception e) {
				Assert.fail();
			}
		}
	}
}
