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
	public void createsMissingParentsForExactAbsoluteScalarPath() throws Exception {
		File projectDirectory = artifact("configuration", "<configuration><existing>true</existing></configuration>");
		Map<String, String> values = new LinkedHashMap<String, String>();
		values.put("test.configuration:configuration.xml:/configuration/links/impersonateEndpoint", "https://example.test/impersonate/{id}");

		EnvironmentOverrideEngine.apply(context(projectDirectory, values));

		String output = read(new File(projectDirectory, "out/configuration/configuration.xml"));
		Assert.assertTrue(output.contains("<links>"));
		Assert.assertTrue(output.contains("<impersonateEndpoint>https://example.test/impersonate/{id}</impersonateEndpoint>"));
	}

	@Test
	public void rejectsRelativeExplicitQuery() throws Exception {
		File projectDirectory = artifact("configuration", "<configuration/>");
		Map<String, String> values = new LinkedHashMap<String, String>();
		values.put("test.configuration:configuration.xml:links/impersonateEndpoint", "https://example.test/impersonate/{id}");

		try {
			EnvironmentOverrideEngine.apply(context(projectDirectory, values));
			Assert.fail("Expected relative explicit query to fail");
		}
		catch (ArtifactHandlerException e) {
			Assert.assertTrue(e.getMessage().contains("Explicit XML override queries must be absolute"));
		}
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

	@Test
	public void centralAliasProcessorCreatesMissingVirtualHostScalar() throws Exception {
		File projectDirectory = Files.createTempDirectory("alias-overrides").toFile();
		File artifactDirectory = new File(projectDirectory, "documents/host");
		Assert.assertTrue(artifactDirectory.mkdirs());
		Files.write(
			new File(artifactDirectory, "node.xml").toPath(),
			"<node artifactManager=\"be.nabu.eai.module.http.virtual.VirtualHostManager\"/>".getBytes(StandardCharsets.UTF_8)
		);
		Files.write(
			new File(artifactDirectory, "virtual-host.xml").toPath(),
			"<virtualHost><server>bebatOne.documents.server</server></virtualHost>".getBytes(StandardCharsets.UTF_8)
		);
		Map<String, String> values = new LinkedHashMap<String, String>();
		values.put("test.documents.host:host", "localhost");

		EnvironmentOverrideEngine.apply(context(projectDirectory, values));

		String output = read(new File(new File(projectDirectory, "out/documents/host"), "virtual-host.xml"));
		Assert.assertTrue(output.contains("<host>localhost</host>"));
	}

	@Test
	public void centralAliasProcessorSupportsDynamicFeatureAliases() throws Exception {
		File projectDirectory = Files.createTempDirectory("dynamic-feature").toFile();
		File artifactDirectory = new File(projectDirectory, "shared/featureSet");
		Assert.assertTrue(artifactDirectory.mkdirs());
		Files.write(
			new File(artifactDirectory, "node.xml").toPath(),
			"<node artifactManager=\"be.nabu.eai.module.misc.features.FeatureSetManager\"/>".getBytes(StandardCharsets.UTF_8)
		);
		Files.write(
			new File(artifactDirectory, "feature-set.xml").toPath(),
			"<features><disabled>ALLOW_AUTHORIZED_REPRESENTATIVE_CREATION</disabled></features>".getBytes(StandardCharsets.UTF_8)
		);
		Map<String, String> values = new LinkedHashMap<String, String>();
		values.put("test.shared.featureSet:ALLOW_AUTHORIZED_REPRESENTATIVE_CREATION", "true");

		EnvironmentBuildContext context = context(projectDirectory, values);
		EnvironmentOverrideEngine.apply(context);

		String output = read(new File(new File(projectDirectory, "out/shared/featureSet"), "feature-set.xml"));
		Assert.assertTrue(output.contains("<features>ALLOW_AUTHORIZED_REPRESENTATIVE_CREATION</features>"));
		Assert.assertFalse(output.contains("<disabled>ALLOW_AUTHORIZED_REPRESENTATIVE_CREATION</disabled>"));
	}

	@Test
	public void qualifiedFixedValueReportsItsSource() throws Exception {
		File projectDirectory = Files.createTempDirectory("resolved-source").toFile();
		Map<String, String> fixedValues = new LinkedHashMap<String, String>();
		fixedValues.put("test.connection:username", "configured-user");
		EnvironmentBuildContext parent = context(projectDirectory, Collections.<String, String>emptyMap());
		EnvironmentBuildContext context = new EnvironmentBuildContext(
			parent.getProjectDirectory(),
			parent.getOutputDirectory(),
			parent.getEnvironmentName(),
			parent.getProviderValues(),
			fixedValues,
			parent.getSecretCodec(),
			parent.getOptions(),
			parent.getLog(),
			parent.getRootArtifactId()
		);
		ResolvedEnvironmentValue resolved = EnvironmentValues.resolveScalar(
			new ArtifactScopedEnvironmentBuildContext(context, "test.connection", projectDirectory),
			"username"
		);

		Assert.assertNotNull(resolved);
		Assert.assertEquals("test.connection:username", resolved.getKey());
		Assert.assertEquals("configured-user", resolved.getValue());
		Assert.assertEquals("qualified fixed", resolved.getSource());
	}

	private String read(File file) throws Exception {
		return new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
	}
}
