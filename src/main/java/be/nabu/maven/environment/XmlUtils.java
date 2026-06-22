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

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;

public final class XmlUtils {

	private XmlUtils() {
	}

	public static void write(Document document, File output) throws Exception {
		write(document, output, true);
	}

	public static void write(Document document, File output, boolean cleanupWhitespace) throws Exception {
		if (output.getParentFile() != null && !output.getParentFile().exists() && !output.getParentFile().mkdirs()) {
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
		output.write(retabIndentation(new String(buffer.toByteArray(), StandardCharsets.UTF_8)).getBytes(StandardCharsets.UTF_8));
	}

	private static void removeWhitespaceNodes(org.w3c.dom.Node node) {
		org.w3c.dom.NodeList childNodes = node.getChildNodes();
		for (int i = childNodes.getLength() - 1; i >= 0; i--) {
			org.w3c.dom.Node child = childNodes.item(i);
			if (child.getNodeType() == org.w3c.dom.Node.TEXT_NODE && (child.getTextContent() == null || child.getTextContent().trim().isEmpty())) {
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
