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
import java.util.List;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public abstract class AbstractXmlArtifactHandler implements ArtifactHandler {
	protected Document parse(File input) throws ArtifactHandlerException {
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setNamespaceAware(false);
			factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
			factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
			factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
			return factory.newDocumentBuilder().parse(input);
		}
		catch (Exception e) {
			throw new ArtifactHandlerException("Could not parse xml file: " + input, e);
		}
	}

	protected void write(Document document, File output) throws ArtifactHandlerException {
		try {
			XmlUtils.write(document, output);
		}
		catch (Exception e) {
			throw new ArtifactHandlerException("Could not write xml file: " + output, e);
		}
	}

	protected XPath newXPath() {
		return XPathFactory.newInstance().newXPath();
	}

	protected Node node(XPath xpath, Document document, String expression) throws ArtifactHandlerException {
		try {
			return (Node) xpath.evaluate(expression, document, XPathConstants.NODE);
		}
		catch (Exception e) {
			throw new ArtifactHandlerException("Could not evaluate xpath: " + expression, e);
		}
	}

	protected NodeList nodes(XPath xpath, Document document, String expression) throws ArtifactHandlerException {
		try {
			return (NodeList) xpath.evaluate(expression, document, XPathConstants.NODESET);
		}
		catch (Exception e) {
			throw new ArtifactHandlerException("Could not evaluate xpath node set: " + expression, e);
		}
	}

	protected void replaceNodeValue(EnvironmentBuildContext context, Node node, String value, boolean encrypted) throws ArtifactHandlerException {
		if (node == null || value == null) {
			return;
		}
		try {
			node.setNodeValue(encrypted ? context.getSecretCodec().encrypt(value) : value);
		}
		catch (Exception e) {
			throw new ArtifactHandlerException("Could not replace xml node value", e);
		}
	}

	protected void replaceNodeValue(EnvironmentBuildContext context, Document document, XPath xpath, String expression, String value, boolean encrypted) throws ArtifactHandlerException {
		if (value == null) {
			return;
		}
		Node node = node(xpath, document, expression);
		if (node == null) {
			node = ensureNode(document, expression, value);
		}
		replaceNodeValue(context, node, value, encrypted);
	}

	protected void requireRootElement(Document document, String expectedRootElement) throws ArtifactHandlerException {
		Element root = document.getDocumentElement();
		if (root == null) {
			throw new ArtifactHandlerException("XML document has no root element");
		}
		if (!expectedRootElement.equals(root.getTagName())) {
			throw new ArtifactHandlerException("Expected root element '" + expectedRootElement + "' but found '" + root.getTagName() + "'");
		}
	}

	private Node ensureNode(Document document, String expression, String value) throws ArtifactHandlerException {
		if (value.trim().isEmpty()) {
			return null;
		}
		if (!expression.startsWith("/") || !expression.endsWith("/text()")) {
			throw new ArtifactHandlerException("Can not create missing node for xpath: " + expression);
		}
		String path = expression.substring(1, expression.length() - "/text()".length());
		String[] rawSegments = path.split("/");
		if (rawSegments.length < 2) {
			throw new ArtifactHandlerException("Can not create missing node for xpath: " + expression);
		}
		requireRootElement(document, rawSegments[0]);
		Element current = document.getDocumentElement();
		for (int i = 1; i < rawSegments.length; i++) {
			String segment = rawSegments[i];
			if (segment.isEmpty() || segment.contains("[") || segment.contains("@") || segment.contains("(")) {
				throw new ArtifactHandlerException("Can not create missing node for xpath: " + expression);
			}
			Element child = firstChild(current, segment);
			if (child == null) {
				child = document.createElement(segment);
				current.appendChild(child);
			}
			current = child;
		}
		return current.getFirstChild() == null ? current.appendChild(document.createTextNode("")) : current.getFirstChild();
	}

	private Element firstChild(Element parent, String name) {
		List<Element> matches = new ArrayList<Element>();
		NodeList childNodes = parent.getChildNodes();
		for (int i = 0; i < childNodes.getLength(); i++) {
			Node child = childNodes.item(i);
			if (child.getNodeType() == Node.ELEMENT_NODE && name.equals(child.getNodeName())) {
				matches.add((Element) child);
			}
		}
		return matches.isEmpty() ? null : matches.get(0);
	}

	protected String value(EnvironmentBuildContext context, String key) throws ArtifactHandlerException {
		return EnvironmentValues.scalar(context, key);
	}
}
