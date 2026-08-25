package be.nabu.maven.environment;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import org.apache.maven.plugin.logging.SystemStreamLog;
import org.junit.Assert;
import org.junit.Test;

public class XmlOverrideProcessorTest {
	@Test
	public void createsMissingParentsForRootRelativeScalarPath() throws Exception {
		File projectDirectory = artifact("configuration", "<configuration><existing>true</existing></configuration>");
		Map<String, String> values = new LinkedHashMap<String, String>();
		values.put("test.configuration:configuration.xml:links/impersonateEndpoint", "https://example.test/impersonate/{id}");

		XmlOverrideProcessor.apply(context(projectDirectory, values));

		String output = read(new File(projectDirectory, "configuration/configuration.xml"));
		Assert.assertTrue(output.contains("<links>"));
		Assert.assertTrue(output.contains("<impersonateEndpoint>https://example.test/impersonate/{id}</impersonateEndpoint>"));
	}

	@Test
	public void absoluteQueryWithoutMatchingRootIsTreatedAsRootRelativePath() throws Exception {
		File projectDirectory = artifact("configuration", "<configuration/>");
		Map<String, String> values = new LinkedHashMap<String, String>();
		values.put("test.configuration:configuration.xml:/links/impersonateEndpoint", "https://example.test/impersonate/{id}");

		XmlOverrideProcessor.apply(context(projectDirectory, values));

		String output = read(new File(projectDirectory, "configuration/configuration.xml"));
		Assert.assertTrue(output.contains("<links>"));
		Assert.assertTrue(output.contains("<impersonateEndpoint>https://example.test/impersonate/{id}</impersonateEndpoint>"));
	}

	private File artifact(String name, String xml) throws Exception {
		File projectDirectory = Files.createTempDirectory("xml-overrides").toFile();
		File artifactDirectory = new File(projectDirectory, name);
		Assert.assertTrue(artifactDirectory.mkdirs());
		Files.write(
			new File(artifactDirectory, "node.xml").toPath(),
			"<node artifactManager=\"test.ConfigurationManager\"/>".getBytes(StandardCharsets.UTF_8)
		);
		Files.write(new File(artifactDirectory, "configuration.xml").toPath(), xml.getBytes(StandardCharsets.UTF_8));
		return projectDirectory;
	}

	private EnvironmentBuildContext context(File projectDirectory, Map<String, String> values) {
		return new EnvironmentBuildContext(
			projectDirectory,
			new File(projectDirectory, "out"),
			"test",
			values,
			Collections.<String, String>emptyMap(),
			new SecretCodec("test-secret"),
			Collections.<String, String>emptyMap(),
			new SystemStreamLog(),
			"test"
		);
	}

	private String read(File file) throws Exception {
		return new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
	}
}
