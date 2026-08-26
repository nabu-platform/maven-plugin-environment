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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;
import org.w3c.dom.Document;

public final class AliasOverrideProcessor {
	private AliasOverrideProcessor() {}

	public static void apply(EnvironmentBuildContext context) throws ArtifactHandlerException {
		List<ArtifactDescriptor> artifacts = ArtifactIdResolver.resolveArtifacts(context.getProjectDirectory(), context.getRootArtifactId());
		Map<File, Document> documentsByFile = new LinkedHashMap<File, Document>();
		Map<File, List<AliasTarget>> encryptedTargetsByFile = new LinkedHashMap<File, List<AliasTarget>>();
		XPath xpath = XPathFactory.newInstance().newXPath();
		for (ArtifactDescriptor artifact : artifacts) {
			Map<String, AliasTarget> aliases = ArtifactAliases.resolveAliases(artifact.getArtifactType());
			ArtifactScopedEnvironmentBuildContext scopedContext = new ArtifactScopedEnvironmentBuildContext(context, artifact.getArtifactId(), artifact.getArtifactDirectory());
			applyDynamicAliases(context, artifact, scopedContext);
			if (aliases.isEmpty()) {
				continue;
			}
			for (Map.Entry<String, AliasTarget> entry : aliases.entrySet()) {
				String value = EnvironmentValues.scalar(scopedContext, entry.getKey());
				if (value == null) {
					continue;
				}
				File targetFile = new File(artifact.getArtifactDirectory(), entry.getValue().getFileName());
				if (!targetFile.exists()) {
					context.getLog().warn("Resolved alias target file does not exist for artifact '" + artifact.getArtifactId() + "': " + targetFile.getAbsolutePath());
					continue;
				}
				Document document = documentsByFile.get(targetFile);
				if (document == null) {
					document = XmlOverrideProcessor.parse(targetFile);
					documentsByFile.put(targetFile, document);
				}
				EnvironmentOverride override = new EnvironmentOverride(artifact.getArtifactId(), entry.getValue().getFileName(), entry.getValue().getQuery(), value);
				XmlOverrideProcessor.applyOverride(context, document, xpath, override);
				if (entry.getValue().isEncrypted()) {
					List<AliasTarget> encryptedTargets = encryptedTargetsByFile.get(targetFile);
					if (encryptedTargets == null) {
						encryptedTargets = new ArrayList<AliasTarget>();
						encryptedTargetsByFile.put(targetFile, encryptedTargets);
					}
					encryptedTargets.add(entry.getValue());
				}
			}
		}
		for (Map.Entry<File, Document> entry : documentsByFile.entrySet()) {
			List<AliasTarget> encryptedTargets = encryptedTargetsByFile.get(entry.getKey());
			if (encryptedTargets != null) {
				XmlOverrideProcessor.encryptKnownSecrets(context, entry.getValue(), xpath, encryptedTargets);
			}
			File outputFile = outputFile(context, entry.getKey());
			XmlOverrideProcessor.write(outputFile, entry.getValue());
		}
	}

	private static void applyDynamicAliases(EnvironmentBuildContext context, ArtifactDescriptor artifact, ArtifactScopedEnvironmentBuildContext scopedContext) throws ArtifactHandlerException {
		for (Map.Entry<String, String> entry : scopedContext.getProviderValues().entrySet()) {
			applyDynamicAlias(context, artifact, scopedContext, entry.getKey(), entry.getValue(), "provider");
		}
		for (Map.Entry<String, String> entry : scopedContext.getFixedValues().entrySet()) {
			if (scopedContext.getProviderValues().containsKey(entry.getKey())) {
				continue;
			}
			applyDynamicAlias(context, artifact, scopedContext, entry.getKey(), entry.getValue(), "fixed");
		}
	}

	private static void applyDynamicAlias(EnvironmentBuildContext context, ArtifactDescriptor artifact, ArtifactScopedEnvironmentBuildContext scopedContext, String key, String value, String source) throws ArtifactHandlerException {
		int firstColon = key.indexOf(':');
		if (firstColon <= 0 || !artifact.getArtifactId().equals(key.substring(0, firstColon).trim())) {
			return;
		}
		String remainder = key.substring(firstColon + 1).trim();
		if (remainder.isEmpty() || remainder.contains(":")) {
			return;
		}
		DynamicAliasSupport support = DynamicAliasRegistry.find(artifact, remainder);
		if (support == null) {
			return;
		}
		context.getLog().info("Applying dynamic " + source + " alias for artifact '" + artifact.getArtifactId() + "': " + remainder);
		ArtifactScopedEnvironmentBuildContext artifactOutputContext = new ArtifactScopedEnvironmentBuildContext(
			scopedContext.getProjectDirectory(),
			new File(scopedContext.getOutputDirectory(), scopedContext.getProjectDirectory().toPath().toAbsolutePath().normalize().relativize(artifact.getArtifactDirectory().toPath().toAbsolutePath().normalize()).toString()),
			scopedContext.getEnvironmentName(),
			scopedContext.getProviderValues(),
			scopedContext.getFixedValues(),
			scopedContext.getSecretCodec(),
			scopedContext.getOptions(),
			scopedContext.getLog(),
			scopedContext.getRootArtifactId(),
			artifact.getArtifactId(),
			artifact.getArtifactDirectory()
		);
		artifactOutputContext.getOutputDirectory().mkdirs();
		support.apply(artifactOutputContext, remainder, value);
	}

	private static File outputFile(EnvironmentBuildContext context, File sourceFile) {
		String relativePath = context.getProjectDirectory().toPath().toAbsolutePath().normalize().relativize(sourceFile.toPath().toAbsolutePath().normalize()).toString();
		return new File(context.getOutputDirectory(), relativePath);
	}
}
