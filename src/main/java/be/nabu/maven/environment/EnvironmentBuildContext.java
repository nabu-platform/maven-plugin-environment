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
import java.util.Collections;
import java.util.Map;
import org.apache.maven.plugin.logging.Log;

public class EnvironmentBuildContext {
	private final File projectDirectory;
	private final File outputDirectory;
	private final String environmentName;
	private final Map<String, String> providerValues;
	private final Map<String, String> fixedValues;
	private final SecretCodec secretCodec;
	private final Map<String, String> options;
	private final Log log;

	public EnvironmentBuildContext(
			File projectDirectory,
			File outputDirectory,
			String environmentName,
			Map<String, String> providerValues,
			Map<String, String> fixedValues,
			SecretCodec secretCodec,
			Map<String, String> options,
			Log log) {
		this.projectDirectory = projectDirectory;
		this.outputDirectory = outputDirectory;
		this.environmentName = environmentName;
		this.providerValues = providerValues == null ? Collections.<String, String>emptyMap() : providerValues;
		this.fixedValues = fixedValues == null ? Collections.<String, String>emptyMap() : fixedValues;
		this.secretCodec = secretCodec;
		this.options = options == null ? Collections.<String, String>emptyMap() : options;
		this.log = log;
	}

	public File getProjectDirectory() {
		return projectDirectory;
	}

	public File getOutputDirectory() {
		return outputDirectory;
	}

	public String getEnvironmentName() {
		return environmentName;
	}

	public Map<String, String> getProviderValues() {
		return providerValues;
	}

	public Map<String, String> getFixedValues() {
		return fixedValues;
	}

	public SecretCodec getSecretCodec() {
		return secretCodec;
	}

	public Map<String, String> getOptions() {
		return options;
	}

	public boolean isEnabled(String option) {
		String value = options.get(option);
		return value != null && Boolean.parseBoolean(value);
	}

	public Log getLog() {
		return log;
	}
}
