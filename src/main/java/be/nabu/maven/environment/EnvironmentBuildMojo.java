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
import java.io.FileInputStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

@Mojo(name = "build-environment", defaultPhase = LifecyclePhase.PACKAGE, threadSafe = true)
public class EnvironmentBuildMojo extends AbstractMojo {
	@Parameter(defaultValue = "${project.basedir}", required = true, readonly = true)
	private File projectDirectory;

	@Parameter(defaultValue = "${project.build.directory}/environment-build", required = true)
	private File outputDirectory;

	@Parameter(property = "environment.name", required = true)
	private String environmentName;

	@Parameter(property = "environment.secret", defaultValue = "changeit")
	private String secret;

	@Parameter
	private List<String> handlers;

	@Parameter(property = "environment.providerClass")
	private String providerClass;

	@Parameter
	private Map<String, String> providerConfiguration;

	@Parameter
	private Map<String, String> options;

	@Parameter(property = "environment.configurationFile")
	private File configurationFile;

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		if (environmentName == null || environmentName.trim().isEmpty()) {
			throw new MojoFailureException("Missing required parameter: environment.name");
		}
		if (!outputDirectory.exists() && !outputDirectory.mkdirs()) {
			throw new MojoExecutionException("Could not create output directory: " + outputDirectory);
		}
		Map<String, String> providerValues = new LinkedHashMap<String, String>();
		EnvironmentValueProvider valueProvider = createProvider();
		if (valueProvider != null) {
			providerValues.putAll(valueProvider.loadValues(environmentName));
		}
		Map<String, String> fixedValues = loadFixedValues();
		List<ArtifactHandler> artifactHandlers = ArtifactHandlers.resolveHandlers(handlers);
		EnvironmentBuildContext context = new ArtifactScopedEnvironmentBuildContext(
			projectDirectory,
			outputDirectory,
			environmentName,
			providerValues,
			fixedValues,
			new SecretCodec(secret),
			options,
			getLog(),
			ArtifactIdResolver.resolve(projectDirectory)
		);
		for (ArtifactHandler handler : artifactHandlers) {
			try {
				handler.apply(context);
			}
			catch (ArtifactHandlerException e) {
				throw new MojoExecutionException("Artifact handler failed: " + handler.getClass().getSimpleName(), e);
			}
		}
	}

	private Map<String, String> loadFixedValues() throws MojoExecutionException {
		Map<String, String> values = new LinkedHashMap<String, String>();
		File effectiveConfigurationFile = configurationFile;
		if (effectiveConfigurationFile == null) {
			File defaultConfigurationFile = new File(projectDirectory, ".nabu-config");
			if (!defaultConfigurationFile.exists()) {
				return values;
			}
			effectiveConfigurationFile = defaultConfigurationFile;
		}
		if (!effectiveConfigurationFile.exists()) {
			throw new MojoExecutionException("Configured environment.configurationFile does not exist: " + effectiveConfigurationFile);
		}
		Properties properties = new Properties();
		try (FileInputStream input = new FileInputStream(effectiveConfigurationFile)) {
			getLog().info("Loading fixed environment configuration from " + effectiveConfigurationFile);
			properties.load(input);
		}
		catch (Exception e) {
			throw new MojoExecutionException("Could not read environment.configurationFile: " + effectiveConfigurationFile, e);
		}
		for (String key : properties.stringPropertyNames()) {
			values.put(key, properties.getProperty(key));
		}
		return values;
	}

	private EnvironmentValueProvider createProvider() throws MojoExecutionException {
		if (providerClass == null || providerClass.trim().isEmpty()) {
			return null;
		}
		try {
			Class<?> implementationClass = Class.forName(providerClass);
			Object instance = implementationClass.getDeclaredConstructor().newInstance();
			if (!(instance instanceof ConfigurableEnvironmentValueProvider)) {
				throw new MojoExecutionException("Configured provider does not implement ConfigurableEnvironmentValueProvider: " + providerClass);
			}
			((ConfigurableEnvironmentValueProvider) instance).configure(
				providerConfiguration == null ? new LinkedHashMap<String, String>() : providerConfiguration,
				getLog()
			);
			return (EnvironmentValueProvider) instance;
		}
		catch (MojoExecutionException e) {
			throw e;
		}
		catch (Exception e) {
			throw new MojoExecutionException("Could not instantiate provider: " + providerClass, e);
		}
	}
}
