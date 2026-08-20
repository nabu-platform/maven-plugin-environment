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

	public static void apply(EnvironmentBuildContext context, Map<String, AliasTarget> aliases) throws ArtifactHandlerException {
		Map<String, List<EnvironmentOverride>> overridesByFile = groupByFile(context, aliases);
		for (Map.Entry<String, List<EnvironmentOverride>> entry : overridesByFile.entrySet()) {
			applyFile(context, entry.getKey(), entry.getValue(), aliases);
		}
	}

	private static Map<String, List<EnvironmentOverride>> groupByFile(EnvironmentBuildContext context, Map<String, AliasTarget> aliases) {
		Map<String, List<EnvironmentOverride>> grouped = new LinkedHashMap<String, List<EnvironmentOverride>>();
		List<EnvironmentOverride> overrides = new ArrayList<EnvironmentOverride>();
		overrides.addAll(EnvironmentOverrideParser.parse(context.getFixedValues(), aliases));
		overrides.addAll(EnvironmentOverrideParser.parse(context.getProviderValues(), aliases));
		for (EnvironmentOverride override : overrides) {
			if (!override.getArtifactId().equals(((ArtifactScopedEnvironmentBuildContext) context).getArtifactId())) {
				continue;
			}
			List<EnvironmentOverride> fileOverrides = grouped.get(override.getFileName());
			if (fileOverrides == null) {
				fileOverrides = new ArrayList<EnvironmentOverride>();
				grouped.put(override.getFileName(), fileOverrides);
			}
			fileOverrides.add(override);
		}
		return grouped;
	}

	private static void applyFile(EnvironmentBuildContext context, String fileName, List<EnvironmentOverride> overrides, Map<String, AliasTarget> aliases) throws ArtifactHandlerException {
		Document document = parse(context, fileName);
		XPath xpath = XPathFactory.newInstance().newXPath();
		for (EnvironmentOverride override : overrides) {
			applyOverride(context, document, xpath, override);
		}
		encryptKnownSecrets(context, document, xpath, fileName, aliases);
		write(context, document, fileName);
	}

	private static Document parse(EnvironmentBuildContext context, String fileName) throws ArtifactHandlerException {
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setNamespaceAware(false);
			factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
			factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
			factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
			return factory.newDocumentBuilder().parse(new java.io.File(context.getOutputDirectory(), fileName));
		}
		catch (Exception e) {
			throw new ArtifactHandlerException("Could not parse xml file for overrides: " + fileName, e);
		}
	}

	private static void write(EnvironmentBuildContext context, Document document, String fileName) throws ArtifactHandlerException {
		try {
			XmlUtils.write(document, new java.io.File(context.getOutputDirectory(), fileName));
		}
		catch (Exception e) {
			throw new ArtifactHandlerException("Could not write xml file after overrides: " + fileName, e);
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
			context.getLog().warn("No xml nodes matched override query: " + query + " in " + override.getFileName());
			return;
		}
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

	private static void encryptKnownSecrets(EnvironmentBuildContext context, Document document, XPath xpath, String fileName, Map<String, AliasTarget> aliases) throws ArtifactHandlerException {
		for (AliasTarget aliasTarget : aliases.values()) {
			if (!aliasTarget.isEncrypted() || !fileName.equals(aliasTarget.getFileName())) {
				continue;
			}
			NodeList nodes = nodes(xpath, document, aliasTarget.getQuery());
			for (int i = 0; i < nodes.getLength(); i++) {
				Node node = nodes.item(i);
				String current = node.getNodeValue();
				if (current == null && node.getNodeType() != Node.ATTRIBUTE_NODE && node.getFirstChild() != null && node.getFirstChild().getNodeType() == Node.TEXT_NODE) {
					current = node.getFirstChild().getNodeValue();
					node = node.getFirstChild();
				}
				if (current == null || current.trim().isEmpty() || current.startsWith("${encrypted:")) {
					continue;
				}
				try {
					node.setNodeValue(context.getSecretCodec().encrypt(current));
				}
				catch (Exception e) {
					throw new ArtifactHandlerException("Could not encrypt secret xml value for " + aliasTarget.getQuery(), e);
				}
			}
		}
	}

	private static Node node(XPath xpath, Document document, String expression) throws ArtifactHandlerException {
		try {
			return (Node) xpath.evaluate(expression, document, XPathConstants.NODE);
		}
		catch (Exception e) {
			throw new ArtifactHandlerException("Could not evaluate xpath: " + expression, e);
		}
	}

	private static NodeList nodes(XPath xpath, Document document, String expression) throws ArtifactHandlerException {
		try {
			return (NodeList) xpath.evaluate(expression, document, XPathConstants.NODESET);
		}
		catch (Exception e) {
			throw new ArtifactHandlerException("Could not evaluate xpath node set: " + expression, e);
		}
	}
}
