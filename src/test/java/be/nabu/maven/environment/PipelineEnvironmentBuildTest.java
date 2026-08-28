package be.nabu.maven.environment;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Collections;
import java.util.Map;
import org.apache.maven.plugin.logging.SystemStreamLog;
import org.junit.Assert;
import org.junit.Test;

public class PipelineEnvironmentBuildTest {
	@Test
	public void appliesMergedConfigurationInPlaceForEachProjectIndependently() throws Exception {
		File workspace = Files.createTempDirectory("pipeline-environment").toFile();
		File myBebat = new File(workspace, "myBebat");
		File bebatOne = new File(workspace, "bebatOne");
		Assert.assertTrue(myBebat.mkdirs());
		Assert.assertTrue(bebatOne.mkdirs());

		artifact(myBebat, "shared/server", "be.nabu.eai.module.http.server.HTTPServerManager", "httpServer.xml",
			"<httpServer><enabled>true</enabled><port>8088</port></httpServer>");
		artifact(myBebat, "shared/smtpClientCubitec", "be.nabu.eai.module.smtp.SMTPClientManager", "smtp-server.xml",
			"<smtpServer><username>help@cubitec.be</username><password>source-password</password></smtpServer>");
		artifact(myBebat, "shared/featureSet", "be.nabu.eai.module.misc.features.FeatureSetManager", "feature-set.xml",
			"<features><features>EXISTING</features><disabled>TEST</disabled></features>");
		artifact(myBebat, "configuration/instance", "be.nabu.eai.module.configuration.ConfigurationManager", "configuration.xml",
			"<definition><links><portal>old</portal></links></definition>");

		artifact(bebatOne, "databases/main/connection", "be.nabu.eai.module.jdbc.pool.JDBCPoolManager", "jdbcPool.xml",
			"<jdbcPool><username>security_dev</username><password>source-password</password><maximumPoolSize>25</maximumPoolSize></jdbcPool>");
		artifact(bebatOne, "documents/host", "be.nabu.eai.module.http.virtual.VirtualHostManager", "virtual-host.xml",
			"<virtualHost><server>bebatOne.documents.server</server></virtualHost>");

		File myBebatConfig = config(myBebat,
			"myBebat.shared.server:enabled=false\n" +
			"myBebat.shared.server:maxSizePerRequest=20971520\n" +
			"myBebat.shared.smtpClientCubitec:username=noreply@cubitec.be\n" +
			"myBebat.shared.smtpClientCubitec:password=${SMTP_PASSWORD}\n" +
			"myBebat.shared.featureSet:TEST=true\n" +
			"myBebat.configuration.instance:configuration.xml:/definition/links/portal=https://portal.uat.example\n");
		File bebatOneConfig = config(bebatOne,
			"bebatOne.databases.main.connection:username=bebatone_qlty\n" +
			"bebatOne.databases.main.connection:password=${DATABASE_PASSWORD}\n" +
			"bebatOne.databases.main.connection:maximumPoolSize=100\n" +
			"bebatOne.documents.host:host=localhost\n");

		apply(myBebat, "myBebat", myBebatConfig, Collections.singletonMap("SMTP_PASSWORD", "smtp-secret"));
		apply(bebatOne, "bebatOne", bebatOneConfig, Collections.singletonMap("DATABASE_PASSWORD", "database-secret"));

		String httpServer = read(myBebat, "shared/server/httpServer.xml");
		Assert.assertTrue(httpServer.contains("<enabled>false</enabled>"));
		Assert.assertTrue(httpServer.contains("<maxSizePerRequest>20971520</maxSizePerRequest>"));

		String smtp = read(myBebat, "shared/smtpClientCubitec/smtp-server.xml");
		Assert.assertTrue(smtp.contains("<username>noreply@cubitec.be</username>"));
		Assert.assertFalse(smtp.contains("smtp-secret"));
		Assert.assertTrue(smtp.contains("${encrypted:"));

		String features = read(myBebat, "shared/featureSet/feature-set.xml");
		Assert.assertTrue(features.contains("<features>TEST</features>"));
		Assert.assertFalse(features.contains("<disabled>TEST</disabled>"));
		Assert.assertTrue(read(myBebat, "configuration/instance/configuration.xml").contains("<portal>https://portal.uat.example</portal>"));

		String jdbc = read(bebatOne, "databases/main/connection/jdbcPool.xml");
		Assert.assertTrue(jdbc.contains("<username>bebatone_qlty</username>"));
		Assert.assertTrue(jdbc.contains("<maximumPoolSize>100</maximumPoolSize>"));
		Assert.assertFalse(jdbc.contains("database-secret"));
		Assert.assertTrue(jdbc.contains("${encrypted:"));
		Assert.assertTrue(read(bebatOne, "documents/host/virtual-host.xml").contains("<host>localhost</host>"));

		Assert.assertFalse(httpServer.contains("bebatOne"));
		Assert.assertFalse(jdbc.contains("myBebat"));
	}

	private void apply(File project, String rootArtifactId, File configuration, Map<String, String> providerValues) throws Exception {
		Map<String, String> fixedValues = EnvironmentConfigParser.parse(configuration);
		EnvironmentBuildContext context = new EnvironmentBuildContext(
			project,
			project,
			"qlty",
			providerValues,
			fixedValues,
			new SecretCodec("pipeline-secret"),
			Collections.<String, String>emptyMap(),
			new SystemStreamLog(),
			rootArtifactId
		);
		EnvironmentOverrideEngine.apply(context);
	}

	private File config(File project, String content) throws Exception {
		File file = new File(project, ".nabu-config");
		Files.write(file.toPath(), content.getBytes(StandardCharsets.UTF_8));
		return file;
	}

	private void artifact(File project, String relativePath, String manager, String fileName, String xml) throws Exception {
		File directory = new File(project, relativePath);
		Assert.assertTrue(directory.mkdirs());
		Files.write(new File(directory, "node.xml").toPath(), ("<node artifactManager=\"" + manager + "\"/>").getBytes(StandardCharsets.UTF_8));
		Files.write(new File(directory, fileName).toPath(), xml.getBytes(StandardCharsets.UTF_8));
	}

	private String read(File project, String relativePath) throws Exception {
		return new String(Files.readAllBytes(new File(project, relativePath).toPath()), StandardCharsets.UTF_8);
	}
}
