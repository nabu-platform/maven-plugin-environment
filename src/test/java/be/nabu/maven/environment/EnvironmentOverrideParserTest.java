package be.nabu.maven.environment;

import java.util.Map;
import org.junit.Assert;
import org.junit.Test;

public class EnvironmentOverrideParserTest {
	@Test
	public void parsesCanonicalOverride() {
		Map<String, AliasTarget> aliases = ArtifactAliases.resolveAliases("be.nabu.eai.module.jdbc.pool.JDBCPoolManager");
		EnvironmentOverride override = EnvironmentOverrideParser.parseForTest(
			"bebatOne.databases.main.connection:jdbcPool.xml:/jdbcPool/username=text",
			aliases
		);
		Assert.assertNotNull(override);
		Assert.assertEquals("bebatOne.databases.main.connection", override.getArtifactId());
		Assert.assertEquals("jdbcPool.xml", override.getFileName());
		Assert.assertEquals("/jdbcPool/username", override.getQuery());
		Assert.assertEquals("text", override.getValue());
	}

	@Test
	public void parsesAliasOverrideForJdbcPool() {
		Map<String, AliasTarget> aliases = ArtifactAliases.resolveAliases("be.nabu.eai.module.jdbc.pool.JDBCPoolManager");
		EnvironmentOverride override = EnvironmentOverrideParser.parseForTest(
			"bebatOne.databases.main.connection:username=bebatone_qlty",
			aliases
		);
		Assert.assertNotNull(override);
		Assert.assertEquals("bebatOne.databases.main.connection", override.getArtifactId());
		Assert.assertEquals("jdbcPool.xml", override.getFileName());
		Assert.assertEquals("/jdbcPool/username/text()", override.getQuery());
		Assert.assertEquals("bebatone_qlty", override.getValue());
	}

	@Test
	public void rejectsAliasWhenArtifactTypeDoesNotDefineIt() {
		Map<String, AliasTarget> aliases = ArtifactAliases.resolveAliases("be.nabu.eai.module.http.server.HTTPServerManager");
		try {
			EnvironmentOverrideParser.parseForTest(
				"bebatOne.databases.main.connection:username=bebatone_qlty",
				aliases
			);
			Assert.fail("Expected unknown alias to throw an exception");
		}
		catch (IllegalArgumentException e) {
			Assert.assertTrue(e.getMessage().contains("Unknown alias"));
		}
	}

	@Test
	public void parsesAliasOverrideForSmtpServer() {
		Map<String, AliasTarget> aliases = ArtifactAliases.resolveAliases("be.nabu.eai.module.smtp.SMTPClientManager");
		EnvironmentOverride override = EnvironmentOverrideParser.parseForTest(
			"myBebat.shared.smtpClientCubitec:username=help@cubitec.be",
			aliases
		);
		Assert.assertNotNull(override);
		Assert.assertEquals("smtp-server.xml", override.getFileName());
		Assert.assertEquals("/smtpServer/username/text()", override.getQuery());
	}

	@Test
	public void parsesExplicitFileQueryBeforeConsideringDynamicAliases() {
		Map<String, AliasTarget> aliases = ArtifactAliases.resolveAliases("be.nabu.eai.module.misc.features.FeatureSetManager");
		EnvironmentOverride override = EnvironmentOverrideParser.parseForTest(
			"myBebat.shared.featureSet:feature-set.xml:/features/disabled[1]/text()=TEST",
			aliases
		);
		Assert.assertNotNull(override);
		Assert.assertEquals("feature-set.xml", override.getFileName());
		Assert.assertEquals("/features/disabled[1]/text()", override.getQuery());
	}
}
