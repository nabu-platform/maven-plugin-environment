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
import java.util.Map;
import org.apache.maven.plugin.logging.Log;

public class ArtifactScopedEnvironmentBuildContext extends EnvironmentBuildContext {
	private final String artifactId;

	public ArtifactScopedEnvironmentBuildContext(EnvironmentBuildContext parent, String artifactId) {
		super(
			parent.getProjectDirectory(),
			parent.getOutputDirectory(),
			parent.getEnvironmentName(),
			parent.getProviderValues(),
			parent.getFixedValues(),
			parent.getSecretCodec(),
			parent.getOptions(),
			parent.getLog()
		);
		this.artifactId = artifactId;
	}

	public ArtifactScopedEnvironmentBuildContext(
			File projectDirectory,
			File outputDirectory,
			String environmentName,
			Map<String, String> providerValues,
			Map<String, String> fixedValues,
			SecretCodec secretCodec,
			Map<String, String> options,
			Log log,
			String artifactId) {
		super(projectDirectory, outputDirectory, environmentName, providerValues, fixedValues, secretCodec, options, log);
		this.artifactId = artifactId;
	}

	public String getArtifactId() {
		return artifactId;
	}
}
