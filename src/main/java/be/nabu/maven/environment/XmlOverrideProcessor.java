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
import java.util.List;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

public final class XmlOverrideProcessor {
	private XmlOverrideProcessor() {}

	static Document parse(File file) throws ArtifactHandlerException {
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

	static void write(File file, Document document) throws ArtifactHandlerException {
		try {
			XmlUtils.write(document, file);
		}
		catch (Exception e) {
			throw new ArtifactHandlerException("Could not write xml file after overrides: " + file, e);
		}
	}

	static void applyOverride(EnvironmentBuildContext context, Document document, XPath xpath, EnvironmentOverride override) throws ArtifactHandlerException {
		String query = XmlUtils.normalizeElementPath(document, override.getQuery());
		String value = override.getValue();
		if (query.endsWith("[?]")) {
			append(document, xpath, query.substring(0, query.length() - 3), value);
			return;
		}
		boolean indexedTarget = query.matches(".*\\[[1-9][0-9]*\\].*");
		NodeList nodes = nodes(xpath, document, query);
		if (nodes.getLength() == 0) {
			if (value == null || value.trim().isEmpty()) {
				context.getLog().info("Skipping empty override because no xml nodes matched query '" + query + "' in " + override.getFileName() + " for artifact '" + override.getArtifactId() + "'");
				return;
			}
			Node created = XmlUtils.ensureElementPath(document, query);
			context.getLog().info("Created missing xml path for artifact '" + override.getArtifactId() + "' file '" + override.getFileName() + "' query '" + query + "'");
			applyValue(document, created, value, indexedTarget);
			return;
		}
		context.getLog().info("Applying override to " + nodes.getLength() + " node(s) for artifact '" + override.getArtifactId() + "' file '" + override.getFileName() + "' query '" + query + "'");
		for (int i = 0; i < nodes.getLength(); i++) {
			applyValue(document, nodes.item(i), value, indexedTarget);
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
		applyValue(document, node, value, false);
	}

	private static void applyValue(Document document, Node node, String value, boolean preserveSiblings) throws ArtifactHandlerException {
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
		if (isSimpleElement(node)) {
			if (preserveSiblings) {
				node.setTextContent(value);
			}
			else {
				replaceSimpleElementValue(node, value);
			}
			return;
		}
		node.setTextContent(value);
	}

	private static boolean isSimpleElement(Node node) {
		if (node.getNodeType() != Node.ELEMENT_NODE) {
			return false;
		}
		NodeList children = node.getChildNodes();
		if (children.getLength() == 0) {
			return true;
		}
		if (children.getLength() == 1 && children.item(0).getNodeType() == Node.TEXT_NODE) {
			return true;
		}
		return false;
	}

	private static void replaceSimpleElementValue(Node node, String value) {
		Node parent = node.getParentNode();
		if (parent != null) {
			NodeList siblings = parent.getChildNodes();
			for (int i = siblings.getLength() - 1; i >= 0; i--) {
				Node sibling = siblings.item(i);
				if (sibling != node && sibling.getNodeType() == Node.ELEMENT_NODE && sibling.getNodeName().equals(node.getNodeName())) {
					parent.removeChild(sibling);
				}
			}
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

	static void encryptKnownSecrets(EnvironmentBuildContext context, Document document, XPath xpath, List<AliasTarget> encryptedTargets) throws ArtifactHandlerException {
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
