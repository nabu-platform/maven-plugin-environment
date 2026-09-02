package be.nabu.maven.environment;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Map;
import org.junit.Assert;
import org.junit.Test;

public class EnvironmentConfigParserTest {
	@Test
	public void preservesEqualsSignsInsideXpathPredicates() throws Exception {
		File file = Files.createTempFile("nabu-config", ".properties").toFile();
		String key = "test.application:fragments.xml:/webFragmentConfigurations/parts[type='password']/configuration/property[@key='password']/text()";
		Files.write(file.toPath(), (key + "=secret=value\n").getBytes(StandardCharsets.UTF_8));

		Map<String, String> values = EnvironmentConfigParser.parse(file);

		Assert.assertEquals("secret=value", values.get(key));
	}

	@Test
	public void preservesMultilineContinuationValues() throws Exception {
		File file = Files.createTempFile("nabu-config", ".properties").toFile();
		Files.write(
			file.toPath(),
			("test.artifact:file.xml:/root/value=first line\\\nsecond line\\\nthird line\nother=value\n").getBytes(StandardCharsets.UTF_8)
		);

		Map<String, String> values = EnvironmentConfigParser.parse(file);

		Assert.assertEquals("first line\nsecond line\nthird line", values.get("test.artifact:file.xml:/root/value"));
		Assert.assertEquals("value", values.get("other"));
	}
}
