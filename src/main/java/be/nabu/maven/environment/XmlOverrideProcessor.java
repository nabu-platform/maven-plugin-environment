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
import java.io.StringReader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

public final class XmlOverrideProcessor {
	private XmlOverrideProcessor() {}

	public static void apply(EnvironmentBuildContext context) throws ArtifactHandlerException {
		List<ArtifactDescriptor> artifacts = ArtifactIdResolver.resolveArtifacts(context.getProjectDirectory(), context.getRootArtifactId());
		Map<String, ArtifactDescriptor> artifactsById = new LinkedHashMap<String, ArtifactDescriptor>();
		context.getLog().info("Discovered " + artifacts.size() + " artifact(s) under project root " + context.getProjectDirectory().getAbsolutePath());
		for (ArtifactDescriptor artifact : artifacts) {
			artifactsById.put(artifact.getArtifactId(), artifact);
			context.getLog().info("Artifact descriptor: id='" + artifact.getArtifactId() + "' type='" + artifact.getArtifactType() + "' dir='" + artifact.getArtifactDirectory().getAbsolutePath() + "'");
		}

		Map<File, Document> documentsByFile = new LinkedHashMap<File, Document>();
		Map<File, List<AliasTarget>> encryptedTargetsByFile = new LinkedHashMap<File, List<AliasTarget>>();
		List<EnvironmentOverride> overrides = new ArrayList<EnvironmentOverride>();
		overrides.addAll(parseOverrides(context, artifactsById, context.getFixedValues(), "fixed"));
		overrides.addAll(parseOverrides(context, artifactsById, context.getProviderValues(), "provider"));

		XPath xpath = XPathFactory.newInstance().newXPath();
		for (EnvironmentOverride override : overrides) {
			ArtifactDescriptor artifact = artifactsById.get(override.getArtifactId());
			if (artifact == null) {
				context.getLog().warn("No artifact descriptor found for override artifact id '" + override.getArtifactId() + "'");
				continue;
			}
			File targetFile = new File(artifact.getArtifactDirectory(), override.getFileName());
			context.getLog().info("Resolved override target file for artifact '" + override.getArtifactId() + "': " + targetFile.getAbsolutePath() + " (exists=" + targetFile.exists() + ")");
			Document document = documentsByFile.get(targetFile);
			if (document == null) {
				document = parse(targetFile);
				documentsByFile.put(targetFile, document);
			}
			applyOverride(context, document, xpath, override);
			Map<String, AliasTarget> aliases = ArtifactAliases.resolveAliases(artifact.getArtifactType());
			for (AliasTarget aliasTarget : aliases.values()) {
				if (aliasTarget.isEncrypted() && override.getFileName().equals(aliasTarget.getFileName())) {
					List<AliasTarget> encryptedTargets = encryptedTargetsByFile.get(targetFile);
					if (encryptedTargets == null) {
						encryptedTargets = new ArrayList<AliasTarget>();
						encryptedTargetsByFile.put(targetFile, encryptedTargets);
					}
					encryptedTargets.add(aliasTarget);
				}
			}
		}

		for (Map.Entry<File, Document> entry : documentsByFile.entrySet()) {
			List<AliasTarget> encryptedTargets = encryptedTargetsByFile.get(entry.getKey());
			if (encryptedTargets != null) {
				encryptKnownSecrets(context, entry.getValue(), xpath, encryptedTargets);
			}
			write(entry.getKey(), entry.getValue());
		}
	}

	private static List<EnvironmentOverride> parseOverrides(EnvironmentBuildContext context, Map<String, ArtifactDescriptor> artifactsById, Map<String, String> values, String source) throws ArtifactHandlerException {
		List<EnvironmentOverride> overrides = new ArrayList<EnvironmentOverride>();
		for (Map.Entry<String, String> entry : values.entrySet()) {
			String key = entry.getKey();
			int firstColon = key.indexOf(':');
			if (firstColon <= 0 || firstColon == key.length() - 1) {
				continue;
			}
			String artifactId = key.substring(0, firstColon).trim();
			ArtifactDescriptor artifact = artifactsById.get(artifactId);
			if (artifact == null) {
				context.getLog().warn("Could not find artifact for " + source + " override key: " + key);
				continue;
			}
			Map<String, AliasTarget> aliases = ArtifactAliases.resolveAliases(artifact.getArtifactType());
			context.getLog().info("Alias keys for artifact '" + artifactId + "': " + aliases.keySet());
			String resolvedValue = "fixed".equals(source) ? EnvironmentValues.scalar(new ArtifactScopedEnvironmentBuildContext(context, artifactId, artifact.getArtifactDirectory()), key.substring(firstColon + 1).trim()) : entry.getValue();
			EnvironmentOverride override = EnvironmentOverrideParser.parseForTest(key + "=" + resolvedValue, aliases);
			if (override == null) {
				context.getLog().warn("Could not parse " + source + " override key: " + key);
				continue;
			}
			context.getLog().info("Parsed " + source + " override: " + key + " -> " + override.getFileName() + ":" + override.getQuery());
			overrides.add(override);
		}
		return overrides;
	}

	private static Document parse(File file) throws ArtifactHandlerException {
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setNamespaceAware(false);
			factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
			factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
			factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
			return factory.newDocumentBuilder().parse(file);
		}
		catch (Exception e) {
			throw new ArtifactHandlerException("Could not parse xml file for overrides: " + file, e);
		}
	}

	private static void write(File file, Document document) throws ArtifactHandlerException {
		try {
			XmlUtils.write(document, file);
		}
		catch (Exception e) {
			throw new ArtifactHandlerException("Could not write xml file after overrides: " + file, e);
		}
	}

	private static void applyOverride(EnvironmentBuildContext context, Document document, XPath xpath, EnvironmentOverride override) throws ArtifactHandlerException {
		String query = override.getQuery();
		String value = override.getValue();
		if (query.endsWith("[?]")) {
			append(document, xpath, query.substring(0, query.length() - 3), value);
			return;
		}
		NodeList nodes = nodes(xpath, document, query);
		if (nodes.getLength() == 0) {
			context.getLog().warn("No xml nodes matched override query: " + query + " in " + override.getFileName() + " for artifact '" + override.getArtifactId() + "'");
			return;
		}
		context.getLog().info("Applying override to " + nodes.getLength() + " node(s) for artifact '" + override.getArtifactId() + "' file '" + override.getFileName() + "' query '" + query + "'");
		for (int i = 0; i < nodes.getLength(); i++) {
			applyValue(document, nodes.item(i), value);
		}
	}

	private static void append(Document document, XPath xpath, String query, String value) throws ArtifactHandlerException {
		Node parent = node(xpath, document, query);
		if (parent == null) {
			throw new ArtifactHandlerException("Could not append xml fragment, parent query did not match: " + query);
		}
		if (value == null || value.trim().isEmpty()) {
			return;
		}
		for (Node child : parseFragment(document, value)) {
			parent.appendChild(child);
		}
	}

	private static void applyValue(Document document, Node node, String value) throws ArtifactHandlerException {
		if (value == null || value.trim().isEmpty()) {
			remove(node);
			return;
		}
		if (node.getNodeType() == Node.ATTRIBUTE_NODE) {
			((Attr) node).setValue(value);
			return;
		}
		if (value.trim().startsWith("<")) {
			replaceWithFragment(document, node, value);
			return;
		}
		if (node.getNodeType() == Node.TEXT_NODE) {
			node.setNodeValue(value);
			return;
		}
		if (node.getChildNodes().getLength() == 1 && node.getFirstChild().getNodeType() == Node.TEXT_NODE) {
			node.getFirstChild().setNodeValue(value);
			return;
		}
		node.setTextContent(value);
	}

	private static void remove(Node node) {
		if (node.getNodeType() == Node.ATTRIBUTE_NODE) {
			((Attr) node).getOwnerElement().removeAttributeNode((Attr) node);
		}
		else if (node.getParentNode() != null) {
			node.getParentNode().removeChild(node);
		}
	}

	private static void replaceWithFragment(Document document, Node node, String xml) throws ArtifactHandlerException {
		Node parent = node.getParentNode();
		if (parent == null) {
			throw new ArtifactHandlerException("Could not replace xml node without parent");
		}
		List<Node> replacements = parseFragment(document, xml);
		for (Node replacement : replacements) {
			parent.insertBefore(replacement, node);
		}
		parent.removeChild(node);
	}

	private static List<Node> parseFragment(Document document, String xml) throws ArtifactHandlerException {
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setNamespaceAware(false);
			factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
			factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
			factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
			Document fragmentDocument = factory.newDocumentBuilder().parse(new InputSource(new StringReader("<root>" + xml + "</root>")));
			DocumentFragment fragment = document.createDocumentFragment();
			List<Node> nodes = new ArrayList<Node>();
			NodeList children = fragmentDocument.getDocumentElement().getChildNodes();
			for (int i = 0; i < children.getLength(); i++) {
				Node imported = document.importNode(children.item(i), true);
				fragment.appendChild(imported);
				nodes.add(imported);
			}
			return nodes;
		}
		catch (Exception e) {
			throw new ArtifactHandlerException("Could not parse xml fragment", e);
		}
	}

	private static void encryptKnownSecrets(EnvironmentBuildContext context, Document document, XPath xpath, List<AliasTarget> encryptedTargets) throws ArtifactHandlerException {
		for (AliasTarget aliasTarget : encryptedTargets) {
			NodeList nodes = nodes(xpath, document, aliasTarget.getQuery());
			for (int i = 0; i < nodes.getLength(); i++) {
				Node node = nodes.item(i);
				String currentValue = node.getTextContent();
				if (currentValue == null || currentValue.trim().isEmpty() || currentValue.startsWith("${encrypted:")) {
					continue;
				}
				try {
					node.setTextContent(context.getSecretCodec().encrypt(currentValue));
				}
				catch (Exception e) {
					throw new ArtifactHandlerException("Could not encrypt secret value for query: " + aliasTarget.getQuery(), e);
				}
			}
		}
	}

	private static NodeList nodes(XPath xpath, Document document, String expression) throws ArtifactHandlerException {
		try {
			return (NodeList) xpath.evaluate(expression, document, XPathConstants.NODESET);
		}
		catch (Exception e) {
			throw new ArtifactHandlerException("Invalid xpath expression: " + expression, e);
		}
	}

	private static Node node(XPath xpath, Document document, String expression) throws ArtifactHandlerException {
		try {
			return (Node) xpath.evaluate(expression, document, XPathConstants.NODE);
		}
		catch (Exception e) {
			throw new ArtifactHandlerException("Invalid xpath expression: " + expression, e);
		}
	}
}
