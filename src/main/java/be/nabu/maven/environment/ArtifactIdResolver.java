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
import java.util.ArrayList;
import java.util.List;

public final class ArtifactIdResolver {
	private ArtifactIdResolver() {}

	public static String resolve(File artifactDirectory) {
		if (artifactDirectory == null) {
			return null;
		}
		File meta = new File(artifactDirectory, "artifact.xml");
		if (meta.exists()) {
			try {
				return XmlUtils.readRootText(meta, "/artifact/id/text()");
			}
			catch (Exception e) {
				throw new IllegalStateException("Could not resolve artifact id from: " + meta, e);
			}
		}
		return artifactDirectory.getName();
	}

	public static String resolveArtifactType(File artifactDirectory) {
		if (artifactDirectory == null) {
			return null;
		}
		if (new File(artifactDirectory, "jdbcPool.xml").exists()) {
			return "jdbcPool";
		}
		if (new File(artifactDirectory, "httpServer.xml").exists()) {
			return "httpServer";
		}
		if (new File(artifactDirectory, "virtual-host.xml").exists()) {
			return "virtualHost";
		}
		if (new File(artifactDirectory, "swagger-client.xml").exists()) {
			return "swaggerClient";
		}
		return null;
	}

	public static List<ArtifactDescriptor> resolveArtifacts(File projectDirectory) {
		List<ArtifactDescriptor> artifacts = new ArrayList<ArtifactDescriptor>();
		collectArtifacts(projectDirectory, artifacts);
		return artifacts;
	}

	private static void collectArtifacts(File directory, List<ArtifactDescriptor> artifacts) {
		if (directory == null || !directory.isDirectory()) {
			return;
		}
		String artifactType = resolveArtifactType(directory);
		if (artifactType != null) {
			artifacts.add(new ArtifactDescriptor(resolve(directory), directory, artifactType));
		}
		File[] children = directory.listFiles();
		if (children == null) {
			return;
		}
		for (File child : children) {
			if (child.isDirectory()) {
				collectArtifacts(child, artifacts);
			}
		}
	}
}
