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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.project.MavenProject;
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

	@Parameter(defaultValue = "${project}", readonly = true, required = true)
	private MavenProject project;

	@Parameter(defaultValue = "${session}", readonly = true, required = true)
	private MavenSession session;


	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		if (environmentName == null || environmentName.trim().isEmpty()) {
			throw new MojoFailureException("Missing required parameter: environment.name");
		}
		if (!outputDirectory.exists() && !outputDirectory.mkdirs()) {
			throw new MojoExecutionException("Could not create output directory: " + outputDirectory);
		}
		MavenProject effectiveProject = resolveEffectiveProject();
		File effectiveProjectDirectory = normalizeProjectDirectory(effectiveProject.getBasedir(), effectiveProject.getArtifactId());
		String effectiveRootArtifactId = effectiveProject.getArtifactId();
		getLog().info("Injected Maven project artifact id: " + project.getArtifactId());
		getLog().info("Injected Maven project basedir: " + project.getBasedir());
		getLog().info("Effective Maven project artifact id: " + effectiveRootArtifactId);
		getLog().info("Effective Maven project basedir: " + effectiveProjectDirectory);
		Map<String, String> providerValues = new LinkedHashMap<String, String>();
		EnvironmentValueProvider valueProvider = createProvider();
		if (valueProvider != null) {
			providerValues.putAll(valueProvider.loadValues(environmentName));
		}
		Map<String, String> fixedValues = loadFixedValues(effectiveProjectDirectory);
		List<ArtifactHandler> artifactHandlers = ArtifactHandlers.resolveHandlers(handlers);
		EnvironmentBuildContext context = new EnvironmentBuildContext(
			effectiveProjectDirectory,
			outputDirectory,
			environmentName,
			providerValues,
			fixedValues,
			new SecretCodec(secret),
			options,
			getLog(),
			effectiveRootArtifactId
		);
		for (ArtifactHandler handler : artifactHandlers) {
			try {
				handler.apply(context);
			}
			catch (ArtifactHandlerException e) {
				throw new MojoExecutionException("Artifact handler failed: " + handler.getClass().getSimpleName(), e);
			}
		}
		try {
			XmlOverrideProcessor.apply(context);
		}
		catch (ArtifactHandlerException e) {
			throw new MojoExecutionException("Could not apply xml overrides", e);
		}
	}

	private Map<String, String> loadFixedValues(File effectiveProjectDirectory) throws MojoExecutionException {
		Map<String, String> values = new LinkedHashMap<String, String>();
		File effectiveConfigurationFile = configurationFile;
		if (effectiveConfigurationFile == null) {
			File defaultConfigurationFile = new File(effectiveProjectDirectory, ".nabu-config");
			if (!defaultConfigurationFile.exists()) {
				return values;
			}
			effectiveConfigurationFile = defaultConfigurationFile;
		}
		if (!effectiveConfigurationFile.exists()) {
			throw new MojoExecutionException("Configured environment.configurationFile does not exist: " + effectiveConfigurationFile);
		}
		try {
			getLog().info("Loading fixed environment configuration from " + effectiveConfigurationFile);
			values.putAll(EnvironmentConfigParser.parse(effectiveConfigurationFile));
		}
		catch (Exception e) {
			throw new MojoExecutionException("Could not read environment.configurationFile: " + effectiveConfigurationFile, e);
		}
		return values;
	}

	private MavenProject resolveEffectiveProject() {
		if (session != null && session.getCurrentProject() != null) {
			return session.getCurrentProject();
		}
		return project;
	}

	private File normalizeProjectDirectory(File basedir, String artifactId) {
		if (basedir == null || artifactId == null || artifactId.trim().isEmpty() || !basedir.isDirectory()) {
			return basedir;
		}
		if (matchesArtifactId(basedir, artifactId)) {
			return basedir;
		}
		List<File> candidates = new ArrayList<File>();
		collectCandidateDirectories(basedir, candidates);
		File bestCandidate = null;
		int bestDepth = Integer.MAX_VALUE;
		for (File candidate : candidates) {
			if (!matchesArtifactId(candidate, artifactId)) {
				continue;
			}
			List<ArtifactDescriptor> artifacts = ArtifactIdResolver.resolveArtifacts(candidate, artifactId);
			if (artifacts.isEmpty()) {
				continue;
			}
			int depth = basedir.toPath().toAbsolutePath().normalize().relativize(candidate.toPath().toAbsolutePath().normalize()).getNameCount();
			if (depth < bestDepth) {
				bestCandidate = candidate;
				bestDepth = depth;
			}
		}
		if (bestCandidate != null) {
			getLog().info("Normalized project directory from " + basedir + " to " + bestCandidate + " using inferred artifact root");
			return bestCandidate;
		}
		return basedir;
	}

	private void collectCandidateDirectories(File directory, List<File> candidates) {
		File[] children = directory.listFiles();
		if (children == null) {
			return;
		}
		for (File child : children) {
			if (!child.isDirectory()) {
				continue;
			}
			candidates.add(child);
			collectCandidateDirectories(child, candidates);
		}
	}

	private boolean matchesArtifactId(File candidateDirectory, String artifactId) {
		File candidatePom = new File(candidateDirectory, "pom.xml");
		if (!candidatePom.exists()) {
			return false;
		}
		try (FileInputStream input = new FileInputStream(candidatePom)) {
			Model model = new MavenXpp3Reader().read(input);
			return artifactId.equals(model.getArtifactId());
		}
		catch (Exception e) {
			getLog().warn("Could not inspect candidate pom for project normalization: " + candidatePom + " due to " + e.getMessage());
			return false;
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
