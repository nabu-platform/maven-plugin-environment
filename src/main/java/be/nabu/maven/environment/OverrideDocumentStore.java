/*
* Copyright (C) 2026 Alexander Verbruggen
*
* This program is free software: you can redistribute it and/or modify
* it under the terms of the GNU Lesser General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
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

public class OverrideDocumentStore {
	private final EnvironmentBuildContext context;
	private final Map<File, Document> documents = new LinkedHashMap<File, Document>();
	private final Map<File, List<AliasTarget>> encryptedTargets = new LinkedHashMap<File, List<AliasTarget>>();
	private final XPath xpath = XPathFactory.newInstance().newXPath();

	public OverrideDocumentStore(EnvironmentBuildContext context) {
		this.context = context;
	}

	public Document document(ArtifactDescriptor artifact, String fileName) throws ArtifactHandlerException {
		File file = sourceFile(artifact, fileName);
		Document document = documents.get(file);
		if (document == null) {
			document = XmlOverrideProcessor.parse(file);
			documents.put(file, document);
		}
		return document;
	}

	public void apply(ArtifactDescriptor artifact, EnvironmentOverride override, boolean encrypted) throws ArtifactHandlerException {
		File file = sourceFile(artifact, override.getFileName());
		XmlOverrideProcessor.applyOverride(context, document(artifact, override.getFileName()), xpath, override);
		if (encrypted) {
			addEncryptedTarget(file, new AliasTarget(override.getFileName(), override.getQuery(), true));
		}
	}

	public void addEncryptedTargets(ArtifactDescriptor artifact, String fileName, Iterable<AliasTarget> targets) {
		File file = sourceFile(artifact, fileName);
		for (AliasTarget target : targets) {
			if (target.isEncrypted() && fileName.equals(target.getFileName())) {
				addEncryptedTarget(file, target);
			}
		}
	}

	public void writeAll() throws ArtifactHandlerException {
		for (Map.Entry<File, Document> entry : documents.entrySet()) {
			List<AliasTarget> targets = encryptedTargets.get(entry.getKey());
			if (targets != null) {
				XmlOverrideProcessor.encryptKnownSecrets(context, entry.getValue(), xpath, targets);
			}
			File output = outputFile(entry.getKey());
			XmlOverrideProcessor.write(output, entry.getValue());
			context.getLog().info("Wrote environment XML file '" + output.getAbsolutePath() + "'");
		}
	}

	private File sourceFile(ArtifactDescriptor artifact, String fileName) {
		return new File(artifact.getArtifactDirectory(), fileName).getAbsoluteFile();
	}

	private File outputFile(File sourceFile) {
		String relativePath = context.getProjectDirectory().toPath().toAbsolutePath().normalize().relativize(sourceFile.toPath().toAbsolutePath().normalize()).toString();
		return new File(context.getOutputDirectory(), relativePath);
	}

	private void addEncryptedTarget(File file, AliasTarget target) {
		List<AliasTarget> targets = encryptedTargets.get(file);
		if (targets == null) {
			targets = new ArrayList<AliasTarget>();
			encryptedTargets.put(file, targets);
		}
		targets.add(target);
	}
}
