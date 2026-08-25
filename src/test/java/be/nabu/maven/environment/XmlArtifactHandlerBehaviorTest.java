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

public class XmlArtifactHandlerBehaviorTest {
	@Test
	public void smtpHandlerReplacesSmtpServerRootValues() throws Exception {
		File projectDirectory = Files.createTempDirectory("smtp-handler").toFile();
		Files.write(
			new File(projectDirectory, "smtp-server.xml").toPath(),
			("<smtpServer><host>old</host><username>old</username><password>old</password><subjectTemplate>[OLD]</subjectTemplate></smtpServer>")
				.getBytes(StandardCharsets.UTF_8)
		);
		EnvironmentBuildContext context = context(projectDirectory, mapOf(
			"host", "smtp.gmail.com",
			"username", "help@cubitec.be",
			"password", "secret",
			"subjectTemplate", "[DEV]"
		));

		new SmtpClientArtifactHandler().apply(context);

		String output = new String(Files.readAllBytes(new File(context.getOutputDirectory(), "smtp-server.xml").toPath()), StandardCharsets.UTF_8);
		Assert.assertTrue(output.contains("<host>smtp.gmail.com</host>"));
		Assert.assertTrue(output.contains("<username>help@cubitec.be</username>"));
		Assert.assertTrue(output.contains("<subjectTemplate>[DEV]</subjectTemplate>"));
		Assert.assertFalse(output.contains("<password>old</password>"));
	}

	@Test
	public void smtpHandlerFailsOnUnexpectedRoot() throws Exception {
		File projectDirectory = Files.createTempDirectory("smtp-root").toFile();
		Files.write(
			new File(projectDirectory, "smtp-server.xml").toPath(),
			"<smtpClient><host>old</host></smtpClient>".getBytes(StandardCharsets.UTF_8)
		);
		EnvironmentBuildContext context = context(projectDirectory, Collections.singletonMap("host", "smtp.gmail.com"));

		try {
			new SmtpClientArtifactHandler().apply(context);
			Assert.fail("Expected root mismatch to fail");
		}
		catch (ArtifactHandlerException e) {
			Assert.assertTrue(e.getMessage().contains("Expected root element 'smtpServer'"));
		}
	}

	@Test
	public void replaceNodeValueCreatesMissingOptionalNodesForNonEmptyValues() throws Exception {
		File projectDirectory = Files.createTempDirectory("http-server").toFile();
		Files.write(
			new File(projectDirectory, "httpServer.xml").toPath(),
			"<httpServer><enabled>false</enabled></httpServer>".getBytes(StandardCharsets.UTF_8)
		);
		EnvironmentBuildContext context = context(projectDirectory, Collections.singletonMap("maxSizePerRequest", "20971520"));

		new HttpServerArtifactHandler().apply(context);

		String output = new String(Files.readAllBytes(new File(context.getOutputDirectory(), "httpServer.xml").toPath()), StandardCharsets.UTF_8);
		Assert.assertTrue(output.contains("<maxSizePerRequest>20971520</maxSizePerRequest>"));
	}

	@Test
	public void featureSetHandlerSupportsFeaturesRoot() throws Exception {
		File projectDirectory = Files.createTempDirectory("feature-set").toFile();
		Files.write(
			new File(projectDirectory, "feature-set.xml").toPath(),
			"<features><disabled>DEV</disabled></features>".getBytes(StandardCharsets.UTF_8)
		);
		EnvironmentBuildContext context = context(projectDirectory, Collections.singletonMap("DEV", "true"));

		new FeatureSetArtifactHandler().apply(context);

		String output = new String(Files.readAllBytes(new File(context.getOutputDirectory(), "feature-set.xml").toPath()), StandardCharsets.UTF_8);
		Assert.assertTrue(output.contains("<features>DEV</features>"));
		Assert.assertFalse(output.contains("<disabled>DEV</disabled>"));
	}

	private EnvironmentBuildContext context(File projectDirectory, Map<String, String> providerValues) {
		return new EnvironmentBuildContext(
			projectDirectory,
			new File(projectDirectory, "out"),
			"test",
			providerValues,
			Collections.<String, String>emptyMap(),
			new PassthroughSecretCodec("test-secret"),
			Collections.<String, String>emptyMap(),
			new SystemStreamLog(),
			null
		);
	}

	private Map<String, String> mapOf(String... values) {
		Map<String, String> map = new LinkedHashMap<String, String>();
		for (int i = 0; i < values.length; i += 2) {
			map.put(values[i], values[i + 1]);
		}
		return map;
	}

	private static class PassthroughSecretCodec extends SecretCodec {
		PassthroughSecretCodec(String secret) {
			super(secret);
		}

		@Override
		public String encrypt(String value) {
			return value;
		}
	}
}
