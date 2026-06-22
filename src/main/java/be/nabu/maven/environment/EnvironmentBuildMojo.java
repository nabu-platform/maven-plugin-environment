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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		if (environmentName == null || environmentName.trim().isEmpty()) {
			throw new MojoFailureException("Missing required parameter: environment.name");
		}
		if (!outputDirectory.exists() && !outputDirectory.mkdirs()) {
			throw new MojoExecutionException("Could not create output directory: " + outputDirectory);
		}
		Map<String, String> values = new LinkedHashMap<String, String>();
		EnvironmentValueProvider valueProvider = createProvider();
		if (valueProvider != null) {
			values.putAll(valueProvider.loadValues(environmentName));
		}
		List<ArtifactHandler> artifactHandlers = ArtifactHandlers.resolveHandlers(handlers);
		EnvironmentBuildContext context = new EnvironmentBuildContext(projectDirectory, outputDirectory, environmentName, values, new SecretCodec(secret), options, getLog());
		for (ArtifactHandler handler : artifactHandlers) {
			try {
				handler.apply(context);
			}
			catch (ArtifactHandlerException e) {
				throw new MojoExecutionException("Artifact handler failed: " + handler.getClass().getSimpleName(), e);
			}
		}
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
			((ConfigurableEnvironmentValueProvider) instance).configure(providerConfiguration == null ? new LinkedHashMap<String, String>() : providerConfiguration, getLog());
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
