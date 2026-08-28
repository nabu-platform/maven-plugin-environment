package be.nabu.maven.environment;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import org.junit.Assert;
import org.junit.Test;

public class ArtifactAliasResolutionTest {
	@Test
	public void resolvesJdbcAliasForNestedArtifact() throws Exception {
		File projectDirectory = Files.createTempDirectory("bebatOne").toFile();
		File artifactDirectory = new File(projectDirectory, "databases/main/connection");
		Assert.assertTrue(artifactDirectory.mkdirs());
		Files.write(new File(artifactDirectory, "node.xml").toPath(), (
			"<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
			"<node artifactManager=\"be.nabu.eai.module.jdbc.pool.JDBCPoolManager\"/>\n"
		).getBytes(StandardCharsets.UTF_8));

		List<ArtifactDescriptor> artifacts = ArtifactIdResolver.resolveArtifacts(projectDirectory, projectDirectory.getName());
		ArtifactDescriptor jdbc = null;
		for (ArtifactDescriptor artifact : artifacts) {
			if ((projectDirectory.getName() + ".databases.main.connection").equals(artifact.getArtifactId())) {
				jdbc = artifact;
				break;
			}
		}
		Assert.assertNotNull(jdbc);
		Map<String, AliasTarget> aliases = ArtifactAliases.resolveAliases(jdbc.getArtifactType());
		AliasTarget target = aliases.get("username");
		Assert.assertNotNull(target);
		Assert.assertEquals("jdbcPool.xml", target.getFileName());
		Assert.assertEquals("/jdbcPool/username/text()", target.getQuery());
	}
}
