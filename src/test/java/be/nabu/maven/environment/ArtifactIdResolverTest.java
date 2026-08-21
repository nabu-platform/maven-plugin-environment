package be.nabu.maven.environment;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;

public class ArtifactIdResolverTest {
	@Test
	public void resolvesArtifactIdFromFilesystemPath() throws Exception {
		File projectDirectory = Files.createTempDirectory("env-plugin-project").toFile();
		File artifactDirectory = new File(projectDirectory, "databases/main/connection");
		Assert.assertTrue(artifactDirectory.mkdirs());
		Files.write(new File(artifactDirectory, "node.xml").toPath(), (
			"<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
			"<node artifactManager=\"be.nabu.eai.module.jdbc.pool.JDBCPoolManager\"/>\n"
		).getBytes(StandardCharsets.UTF_8));

		Assert.assertEquals(
			projectDirectory.getName() + ".databases.main.connection",
			ArtifactIdResolver.resolve(projectDirectory, artifactDirectory)
		);
	}

	@Test
	public void resolvesArtifactTypeFromNodeXml() throws Exception {
		File projectDirectory = Files.createTempDirectory("env-plugin-project").toFile();
		File artifactDirectory = new File(projectDirectory, "databases/main/connection");
		Assert.assertTrue(artifactDirectory.mkdirs());
		Files.write(new File(artifactDirectory, "node.xml").toPath(), (
			"<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
			"<node artifactManager=\"be.nabu.eai.module.jdbc.pool.JDBCPoolManager\"/>\n"
		).getBytes(StandardCharsets.UTF_8));

		Assert.assertEquals(
			"be.nabu.eai.module.jdbc.pool.JDBCPoolManager",
			ArtifactIdResolver.resolveArtifactType(artifactDirectory)
		);
	}

	@Test
	public void discoversNestedArtifactsFromProjectTree() throws Exception {
		File projectDirectory = Files.createTempDirectory("bebatOne").toFile();
		File jdbcArtifact = new File(projectDirectory, "databases/main/connection");
		File hostArtifact = new File(projectDirectory, "documents/host");
		Assert.assertTrue(jdbcArtifact.mkdirs());
		Assert.assertTrue(hostArtifact.mkdirs());
		Files.write(new File(jdbcArtifact, "node.xml").toPath(), (
			"<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
			"<node artifactManager=\"be.nabu.eai.module.jdbc.pool.JDBCPoolManager\"/>\n"
		).getBytes(StandardCharsets.UTF_8));
		Files.write(new File(hostArtifact, "node.xml").toPath(), (
			"<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
			"<node artifactManager=\"be.nabu.eai.module.http.virtual.VirtualHostManager\"/>\n"
		).getBytes(StandardCharsets.UTF_8));

		List<ArtifactDescriptor> artifacts = ArtifactIdResolver.resolveArtifacts(projectDirectory);
		Assert.assertEquals(2, artifacts.size());
		ArtifactDescriptor jdbc = find(artifacts, projectDirectory.getName() + ".databases.main.connection");
		ArtifactDescriptor host = find(artifacts, projectDirectory.getName() + ".documents.host");
		Assert.assertNotNull(jdbc);
		Assert.assertNotNull(host);
		Assert.assertEquals("be.nabu.eai.module.jdbc.pool.JDBCPoolManager", jdbc.getArtifactType());
		Assert.assertEquals("be.nabu.eai.module.http.virtual.VirtualHostManager", host.getArtifactType());
	}

	private ArtifactDescriptor find(List<ArtifactDescriptor> artifacts, String artifactId) {
		for (ArtifactDescriptor artifact : artifacts) {
			if (artifactId.equals(artifact.getArtifactId())) {
				return artifact;
			}
		}
		return null;
	}
}
