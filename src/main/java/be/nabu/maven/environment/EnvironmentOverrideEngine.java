/*
* Copyright (C) 2026 Alexander Verbruggen
*
* This program is free software: you can redistribute it and/or modify
* it under the terms of the GNU Lesser General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
*/

package be.nabu.maven.environment;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class EnvironmentOverrideEngine {
	private EnvironmentOverrideEngine() {}

	public static void apply(EnvironmentBuildContext context) throws ArtifactHandlerException {
		List<ArtifactDescriptor> artifacts = ArtifactIdResolver.resolveArtifacts(context.getProjectDirectory(), context.getRootArtifactId());
		Map<String, ArtifactDescriptor> artifactsById = new LinkedHashMap<String, ArtifactDescriptor>();
		for (ArtifactDescriptor artifact : artifacts) {
			artifactsById.put(artifact.getArtifactId(), artifact);
		}
		OverrideDocumentStore store = new OverrideDocumentStore(context);
		Map<String, ResolvedEnvironmentValue> entries = new LinkedHashMap<String, ResolvedEnvironmentValue>();
		for (Map.Entry<String, String> entry : context.getFixedValues().entrySet()) {
			entries.put(entry.getKey(), new ResolvedEnvironmentValue(entry.getKey(), entry.getValue(), "fixed"));
		}
		for (Map.Entry<String, String> entry : context.getProviderValues().entrySet()) {
			entries.put(entry.getKey(), new ResolvedEnvironmentValue(entry.getKey(), entry.getValue(), "provider"));
		}
		for (ResolvedEnvironmentValue entry : entries.values()) {
			applyEntry(context, store, artifactsById, entry);
		}
		store.writeAll();
	}

	private static void applyEntry(EnvironmentBuildContext context, OverrideDocumentStore store, Map<String, ArtifactDescriptor> artifactsById, ResolvedEnvironmentValue resolved) throws ArtifactHandlerException {
		String key = resolved.getKey();
		int firstColon = key.indexOf(':');
		if (firstColon <= 0 || firstColon == key.length() - 1) {
			return;
		}
		String artifactId = key.substring(0, firstColon).trim();
		ArtifactDescriptor artifact = artifactsById.get(artifactId);
		if (artifact == null) {
			context.getLog().warn("Could not find artifact for environment override key: " + key);
			return;
		}
		String remainder = key.substring(firstColon + 1).trim();
		if ("fixed".equals(resolved.getSource())) {
			resolved = new ResolvedEnvironmentValue(key, EnvironmentValues.resolvePlaceholders(context, key, resolved.getValue()), resolved.getSource());
		}
		int secondColon = remainder.indexOf(':');
		if (secondColon > 0 && secondColon < remainder.length() - 1) {
			applyExplicit(context, store, artifact, key, remainder, resolved);
			return;
		}
		Map<String, AliasTarget> aliases = ArtifactAliases.resolveAliases(artifact.getArtifactType());
		AliasTarget alias = aliases.get(remainder);
		if (alias != null) {
			applyAlias(context, store, artifact, remainder, alias, resolved);
			return;
		}
		DynamicAliasSupport dynamic = DynamicAliasRegistry.find(artifact, remainder);
		if (dynamic != null) {
			context.getLog().info("Applying dynamic alias key='" + resolved.getKey() + "' source='" + resolved.getSource() + "' value=" + displayValue(resolved.getValue(), false));
			dynamic.apply(context, store, artifact, remainder, resolved.getValue());
			return;
		}
		throw new ArtifactHandlerException("Unknown alias in override key '" + key + "': '" + remainder + "'");
	}

	private static void applyAlias(EnvironmentBuildContext context, OverrideDocumentStore store, ArtifactDescriptor artifact, String aliasName, AliasTarget alias, ResolvedEnvironmentValue resolved) throws ArtifactHandlerException {
		EnvironmentOverride override = new EnvironmentOverride(artifact.getArtifactId(), alias.getFileName(), alias.getQuery(), resolved.getValue());
		context.getLog().info("Applying alias key='" + resolved.getKey() + "' source='" + resolved.getSource() + "' value=" + displayValue(resolved.getValue(), alias.isEncrypted()) + " target='" + artifact.getArtifactId() + ":" + alias.getFileName() + ":" + alias.getQuery() + "'");
		store.apply(artifact, override, alias.isEncrypted());
	}

	private static void applyExplicit(EnvironmentBuildContext context, OverrideDocumentStore store, ArtifactDescriptor artifact, String key, String remainder, ResolvedEnvironmentValue resolved) throws ArtifactHandlerException {
		int separator = remainder.indexOf(':');
		String fileName = remainder.substring(0, separator).trim();
		String query = remainder.substring(separator + 1).trim();
		if (fileName.isEmpty() || query.isEmpty()) {
			throw new ArtifactHandlerException("Invalid explicit override key: " + key);
		}
		EnvironmentOverride override = new EnvironmentOverride(artifact.getArtifactId(), fileName, query, resolved.getValue());
		context.getLog().info("Applying explicit key='" + resolved.getKey() + "' source='" + resolved.getSource() + "' target='" + artifact.getArtifactId() + ":" + fileName + ":" + query + "'");
		store.apply(artifact, override, false);
		store.addEncryptedTargets(artifact, fileName, ArtifactAliases.resolveAliases(artifact.getArtifactType()).values());
	}

	private static String displayValue(String value, boolean encrypted) {
		if (encrypted) {
			return "<redacted>";
		}
		if (value == null) {
			return "<null>";
		}
		return "'" + value.replace("\r", "\\r").replace("\n", "\\n") + "'";
	}
}
