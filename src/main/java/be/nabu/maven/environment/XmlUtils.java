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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.namespace.QName;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public final class XmlUtils {
	private static final Pattern INDEXED_ELEMENT = Pattern.compile("^([^\\[\\]@()]+)\\[([1-9][0-9]*)\\]$");

	private XmlUtils() {}

	public static String readRootText(File input, String expression) throws Exception {
		Document document = parse(input);
		return (String) XPathFactory.newInstance().newXPath().evaluate(expression, document, XPathConstants.STRING);
	}

	public static String readRootAttribute(File input, String expression, String attributeName) throws Exception {
		Document document = parse(input);
		org.w3c.dom.Node node = (org.w3c.dom.Node) XPathFactory.newInstance().newXPath().evaluate(expression, document, XPathConstants.NODE);
		if (node == null || node.getAttributes() == null || node.getAttributes().getNamedItem(attributeName) == null) {
			return null;
		}
		return node.getAttributes().getNamedItem(attributeName).getNodeValue();
	}

	public static Document read(File input) throws ArtifactHandlerException {
		try {
			return parse(input);
		}
		catch (Exception e) {
			throw new ArtifactHandlerException("Could not parse xml file: " + input, e);
		}
	}

	public static Object evaluate(XPath xpath, Object item, String expression, QName returnType) throws ArtifactHandlerException {
		try {
			return xpath.evaluate(expression, item, returnType);
		}
		catch (Exception e) {
			throw new ArtifactHandlerException("Could not evaluate xpath: " + expression, e);
		}
	}

	public static String normalizeElementPath(Document document, String expression) throws ArtifactHandlerException {
		String normalized = expression == null ? "" : expression.trim();
		if (normalized.isEmpty()) {
			throw new ArtifactHandlerException("XML path can not be empty");
		}
		if (!normalized.startsWith("/")) {
			throw new ArtifactHandlerException("Explicit XML override queries must be absolute: " + expression);
		}
		if (!normalized.startsWith("//")) {
			int separator = normalized.indexOf('/', 1);
			String rootSegment = separator < 0 ? normalized.substring(1) : normalized.substring(1, separator);
			if (isElementName(rootSegment)) {
				requireRootElement(document, rootSegment);
			}
		}
		return normalized;
	}

	public static Node ensureElementPath(Document document, String expression) throws ArtifactHandlerException {
		String normalized = normalizeElementPath(document, expression);
		boolean textTarget = normalized.endsWith("/text()");
		String path = textTarget ? normalized.substring(0, normalized.length() - "/text()".length()) : normalized;
		String[] segments = path.substring(1).split("/");
		if (segments.length == 0) {
			throw new ArtifactHandlerException("Can not create missing node for xpath: " + expression);
		}
		Element root = requireRootElement(document);
		if (!root.getTagName().equals(segments[0])) {
			throw new ArtifactHandlerException("Expected root element '" + segments[0] + "' but found '" + root.getTagName() + "'");
		}
		Element current = root;
		for (int i = 1; i < segments.length; i++) {
			String segment = segments[i];
			Matcher indexedElement = INDEXED_ELEMENT.matcher(segment);
			String name = indexedElement.matches() ? indexedElement.group(1) : segment;
			if (!isElementName(name)) {
				throw new ArtifactHandlerException("Can not create missing node for xpath: " + expression);
			}
			int index = indexedElement.matches() ? Integer.parseInt(indexedElement.group(2)) : 1;
			Element child = indexedChild(current, name, index);
			while (child == null) {
				current.appendChild(document.createElement(name));
				child = indexedChild(current, name, index);
			}
			current = child;
		}
		if (!textTarget) {
			return current;
		}
		NodeList children = current.getChildNodes();
		for (int i = 0; i < children.getLength(); i++) {
			if (children.item(i).getNodeType() == Node.TEXT_NODE) {
				return children.item(i);
			}
		}
		return current.appendChild(document.createTextNode(""));
	}

	public static void requireRootElement(Document document, String expectedRootElement) throws ArtifactHandlerException {
		Element root = requireRootElement(document);
		if (!expectedRootElement.equals(root.getTagName())) {
			throw new ArtifactHandlerException("Expected root element '" + expectedRootElement + "' but found '" + root.getTagName() + "'");
		}
	}

	private static Element requireRootElement(Document document) throws ArtifactHandlerException {
		Element root = document.getDocumentElement();
		if (root == null) {
			throw new ArtifactHandlerException("XML document has no root element");
		}
		return root;
	}

	private static boolean isElementName(String segment) {
		return !segment.isEmpty() && !segment.contains("[") && !segment.contains("@") && !segment.contains("(");
	}

	private static Element indexedChild(Element parent, String name, int index) {
		NodeList children = parent.getChildNodes();
		int currentIndex = 0;
		for (int i = 0; i < children.getLength(); i++) {
			Node child = children.item(i);
			if (child.getNodeType() == Node.ELEMENT_NODE && name.equals(child.getNodeName())) {
				currentIndex++;
				if (currentIndex == index) {
					return (Element) child;
				}
			}
		}
		return null;
	}

	public static void write(Document document, File output) throws Exception {
		write(document, output, true);
	}

	public static void write(Document document, File output, boolean cleanupWhitespace) throws Exception {
		if (output.getParentFile() != null
				&& !output.getParentFile()
					.exists()
				&& !output.getParentFile()
					.mkdirs()) {
			throw new IllegalStateException("Could not create output directory for " + output);
		}
		try (OutputStream outputStream = Files.newOutputStream(output.toPath())) {
			prettyPrint(document, outputStream, cleanupWhitespace);
		}
	}

	public static void prettyPrint(Document document, OutputStream output, boolean cleanupWhitespace) throws Exception {
		if (cleanupWhitespace) {
			removeWhitespaceNodes(document);
		}
		Transformer transformer = TransformerFactory.newInstance().newTransformer();
		transformer.setOutputProperty(OutputKeys.ENCODING, StandardCharsets.UTF_8.name());
		transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
		transformer.setOutputProperty(OutputKeys.INDENT, "yes");
		transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
		ByteArrayOutputStream buffer = new ByteArrayOutputStream();
		transformer.transform(new DOMSource(document), new StreamResult(buffer));
		output.write(
			retabIndentation(new String(buffer.toByteArray(), StandardCharsets.UTF_8))
				.getBytes(StandardCharsets.UTF_8)
		);
	}

	private static Document parse(File input) throws Exception {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setNamespaceAware(false);
		factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
		factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
		factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
		return factory.newDocumentBuilder().parse(input);
	}

	private static void removeWhitespaceNodes(org.w3c.dom.Node node) {
		org.w3c.dom.NodeList childNodes = node.getChildNodes();
		for (int i = childNodes.getLength() - 1; i >= 0; i--) {
			org.w3c.dom.Node child = childNodes.item(i);
			if (child.getNodeType() == org.w3c.dom.Node.TEXT_NODE
					&& (child.getTextContent() == null
							|| child.getTextContent().trim().isEmpty())) {
				node.removeChild(child);
			}
			else {
				removeWhitespaceNodes(child);
			}
		}
	}

	private static String retabIndentation(String content) {
		StringBuilder builder = new StringBuilder();
		String[] lines = content.split("\\r?\\n", -1);
		for (int i = 0; i < lines.length; i++) {
			String line = lines[i];
			int index = 0;
			while (index < line.length() && line.charAt(index) == ' ') {
				index++;
			}
			for (int j = 0; j < index / 4; j++) {
				builder.append('\t');
			}
			builder.append(line.substring(index));
			if (i < lines.length - 1) {
				builder.append('\n');
			}
		}
		return builder.toString();
	}
}
