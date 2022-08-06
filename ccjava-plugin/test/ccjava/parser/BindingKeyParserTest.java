package ccjava.parser;

import org.junit.Assert;
import org.junit.Test;

import ccjava.javaparser.BindingKeyParser;

public class BindingKeyParserTest {
	@Test()
	public void testParseRemovesGenericsInformation() {
		Assert.assertEquals("qwertyazerty", BindingKeyParser.parse("qwerty<asdas<szza<>>f>azerty"));
		Assert.assertEquals("23temp", BindingKeyParser.parse("23<23<21389721szza<>>>temp"));
		Assert.assertEquals("Lccjava.test", BindingKeyParser.parse("Lccjava.test"));
		Assert.assertEquals("doom", BindingKeyParser.parse("do<12345>o<67890>m"));
		Assert.assertEquals("eclipse", BindingKeyParser.parse("ecl<<45>>ips<<01>>e"));
		Assert.assertEquals("1234567", BindingKeyParser.parse("123<d<ef<g>>h>45<<k>>6<>7"));
	}
}
