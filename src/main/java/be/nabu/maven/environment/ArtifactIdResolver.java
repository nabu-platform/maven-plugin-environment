/*
* Copyright (C) 2026 Alexander Verbruggen
*
* This program is free software: you can redistribute it and/or modify
* it under the terms of the GNU Lesser General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
* GNU Lesser General Public License for more details.
*
* You should have received a copy of the GNU Lesser General Public License
* along with this program. If not, see <https://www.gnu.org/licenses/>.
*/

package be.nabu.maven.environment;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class ArtifactIdResolver {
	private ArtifactIdResolver() {}

	public static String resolve(File projectDirectory, File artifactDirectory) {
		if (projectDirectory == null || artifactDirectory == null) {
			return null;
		}
		Path projectPath = projectDirectory.toPath().toAbsolutePath().normalize();
		Path artifactPath = artifactDirectory.toPath().toAbsolutePath().normalize();
		Path relativePath = projectPath.relativize(artifactPath);
		StringBuilder builder = new StringBuilder(projectDirectory.getName());
		for (Path part : relativePath) {
			builder.append('.').append(part.toString());
		}
		return builder.toString();
	}

	public static String resolveArtifactType(File artifactDirectory) {
		if (artifactDirectory == null) {
			return null;
		}
		File nodeFile = new File(artifactDirectory, "node.xml");
		if (!nodeFile.exists()) {
			return null;
		}
		try {
			return XmlUtils.readRootAttribute(nodeFile, "/node", "artifactManager");
		}
		catch (Exception e) {
			throw new IllegalStateException("Could not resolve artifact type from: " + nodeFile, e);
		}
	}

	public static List<ArtifactDescriptor> resolveArtifacts(File projectDirectory) {
		List<ArtifactDescriptor> artifacts = new ArrayList<ArtifactDescriptor>();
		collectArtifacts(projectDirectory, projectDirectory, artifacts);
		return artifacts;
	}

	private static void collectArtifacts(File projectDirectory, File directory, List<ArtifactDescriptor> artifacts) {
		if (directory == null || !directory.isDirectory()) {
			return;
		}
		String artifactType = resolveArtifactType(directory);
		if (artifactType != null) {
			artifacts.add(new ArtifactDescriptor(resolve(projectDirectory, directory), directory, artifactType));
		}
		File[] children = directory.listFiles();
		if (children == null) {
			return;
		}
		for (File child : children) {
			if (child.isDirectory()) {
				collectArtifacts(projectDirectory, child, artifacts);
			}
		}
	}
}
