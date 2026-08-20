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

public final class ArtifactIdResolver {
	private ArtifactIdResolver() {}

	public static String resolve(File projectDirectory) {
		File artifactDirectory = resolveArtifactDirectory(projectDirectory);
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

	public static File resolveArtifactDirectory(File projectDirectory) {
		if (projectDirectory == null) {
			return null;
		}
		File current = projectDirectory;
		while (current != null) {
			if (new File(current, "artifact.xml").exists()) {
				return current;
			}
			if (new File(current, "node.xml").exists()) {
				return current;
			}
			if (new File(current, "jdbcPool.xml").exists()) {
				return current;
			}
			if (new File(current, "httpServer.xml").exists()) {
				return current;
			}
			if (new File(current, "virtual-host.xml").exists()) {
				return current;
			}
			if (new File(current, "swagger-client.xml").exists()) {
				return current;
			}
			current = current.getParentFile();
		}
		return projectDirectory;
	}
}
