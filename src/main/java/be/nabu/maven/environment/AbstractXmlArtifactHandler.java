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
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import org.w3c.dom.Document;
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

	protected String value(EnvironmentBuildContext context, String key) {
		return context.getValues().get(key);
	}
}
