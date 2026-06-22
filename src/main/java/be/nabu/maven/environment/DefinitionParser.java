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

public final class DefinitionParser {
	private DefinitionParser() {}

	public static DefinitionModel parse(File definitionFile) throws ArtifactHandlerException {
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setNamespaceAware(false);
			factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
			factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
			factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
			Document document = factory.newDocumentBuilder().parse(definitionFile);
			DefinitionModel model = new DefinitionModel();
			walk(document.getDocumentElement(), null, false, model, newXPath());
			return model;
		}
		catch (Exception e) {
			throw new ArtifactHandlerException("Could not parse definition: " + definitionFile, e);
		}
	}

	private static void walk(
			Node node,
			String parentPath,
			boolean inheritedEnvironmentSpecific,
			DefinitionModel model,
			XPath xpath) throws Exception {
		String name = attribute(node, "name");
		String path = name == null || name.isEmpty() ? parentPath : parentPath == null ? name : parentPath + "/" + name;
		boolean list = isList(node);
		boolean environmentSpecific = inheritedEnvironmentSpecific || isEnvironmentSpecific(node);
		if (path != null && !path.isEmpty()) {
			model.addField(new DefinitionField(path, list, environmentSpecific));
		}
		NodeList children = (NodeList) xpath.evaluate(
			"./*[local-name()='element' or local-name()='field' or local-name()='entry']",
			node,
			XPathConstants.NODESET
		);
		for (int i = 0; i < children.getLength(); i++) {
			walk(children.item(i), path, environmentSpecific, model, xpath);
		}
	}

	private static XPath newXPath() {
		return XPathFactory.newInstance().newXPath();
	}

	private static boolean isList(Node node) {
		String maxOccurs = attribute(node, "maxOccurs");
		if (maxOccurs == null || maxOccurs.isEmpty()) {
			return false;
		}
		return "unbounded".equalsIgnoreCase(maxOccurs) || parseInteger(maxOccurs) > 1;
	}

	private static boolean isEnvironmentSpecific(Node node) {
		String environmentSpecific = attribute(node, "environmentSpecific");
		return "true".equalsIgnoreCase(environmentSpecific);
	}

	private static String attribute(Node node, String name) {
		return node.getAttributes() == null
				|| node.getAttributes().getNamedItem(name) == null
			? null
			: node.getAttributes().getNamedItem(name).getNodeValue();
	}

	private static int parseInteger(String value) {
		try {
			return Integer.parseInt(value);
		}
		catch (NumberFormatException e) {
			return 0;
		}
	}
}
