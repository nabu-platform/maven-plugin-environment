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
import java.util.Map;
import javax.xml.xpath.XPath;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class FeatureSetArtifactHandler extends AbstractXmlArtifactHandler {
	@Override
	public void apply(EnvironmentBuildContext context) throws ArtifactHandlerException {
		File input = new File(context.getProjectDirectory(), "feature-set.xml");
		if (!input.exists()) {
			context.getLog().debug("Skipping feature set handler, file not found: " + input);
			return;
		}
		Document document = parse(input);
		XPath xpath = newXPath();
		List<String> enabled = values(xpath, document, "/featureSet/features/text()");
		List<String> disabled = values(xpath, document, "/featureSet/disabled/text()");
		for (Map.Entry<String, String> entry : context.getValues()
			.entrySet()) {
			String setting = entry.getValue();
			if (!"true".equalsIgnoreCase(setting) && !"false".equalsIgnoreCase(setting)) {
				continue;
			}
			enabled.remove(entry.getKey());
			disabled.remove(entry.getKey());
			if (Boolean.parseBoolean(setting)) {
				enabled.add(entry.getKey());
			}
			else {
				disabled.add(entry.getKey());
			}
		}
		rewriteList(document, enabled, "/featureSet/features", "features");
		rewriteList(document, disabled, "/featureSet/disabled", "disabled");
		write(document, new File(context.getOutputDirectory(), "feature-set.xml"));
	}

	private List<String> values(XPath xpath, Document document, String expression) throws ArtifactHandlerException {
		NodeList nodes = nodes(xpath, document, expression);
		List<String> values = new ArrayList<String>();
		for (int i = 0; i < nodes.getLength(); i++) {
			String value = nodes.item(i).getNodeValue();
			if (value != null && !value.trim().isEmpty()) {
				values.add(value.trim());
			}
		}
		return values;
	}

	private void rewriteList(Document document, List<String> values, String path, String elementName) throws ArtifactHandlerException {
		try {
			Node parent = document.getDocumentElement();
			NodeList existing = nodes(newXPath(), document, path);
			for (int i = existing.getLength() - 1; i >= 0; i--) {
				parent.removeChild(existing.item(i));
			}
			for (String value : values) {
				Node created = document.createElement(elementName);
				created.setTextContent(value);
				parent.appendChild(created);
			}
		}
		catch (Exception e) {
			throw new ArtifactHandlerException("Could not rewrite feature set list", e);
		}
	}
}
