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
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class FeatureSetDynamicAliasSupport implements DynamicAliasSupport {
	@Override
	public boolean supports(ArtifactDescriptor artifact) {
		return "be.nabu.eai.module.misc.features.FeatureSetManager".equals(artifact.getArtifactType());
	}

	@Override
	public boolean supportsAlias(String alias) {
		return alias != null && !alias.contains(":") && !alias.trim().isEmpty();
	}

	@Override
	public void apply(ArtifactScopedEnvironmentBuildContext context, String alias, String value) throws ArtifactHandlerException {
		if (!"true".equalsIgnoreCase(value) && !"false".equalsIgnoreCase(value)) {
			return;
		}
		File input = new File(context.getArtifactDirectory(), "feature-set.xml");
		if (!input.exists()) {
			context.getLog().debug("Skipping dynamic feature alias, file not found: " + input);
			return;
		}
		Document document = XmlUtils.read(input);
		XmlUtils.requireRootElement(document, "features");
		XPath xpath = XPathFactory.newInstance().newXPath();
		List<String> enabled = values(xpath, document, "/features/features/text()");
		List<String> disabled = values(xpath, document, "/features/disabled/text()");
		enabled.remove(alias);
		disabled.remove(alias);
		if (Boolean.parseBoolean(value)) {
			enabled.add(alias);
		}
		else {
			disabled.add(alias);
		}
		rewriteList(document, enabled, "/features/features", "features");
		rewriteList(document, disabled, "/features/disabled", "disabled");
		try {
			XmlUtils.write(document, new File(context.getOutputDirectory(), "feature-set.xml"));
		}
		catch (Exception e) {
			throw new ArtifactHandlerException("Could not write feature set artifact", e);
		}
	}

	private List<String> values(XPath xpath, Document document, String expression) throws ArtifactHandlerException {
		NodeList nodes = (NodeList) XmlUtils.evaluate(xpath, document, expression, javax.xml.xpath.XPathConstants.NODESET);
		List<String> values = new ArrayList<String>();
		for (int i = 0; i < nodes.getLength(); i++) {
			String current = nodes.item(i).getNodeValue();
			if (current != null && !current.trim().isEmpty()) {
				values.add(current.trim());
			}
		}
		return values;
	}

	private void rewriteList(Document document, List<String> values, String path, String elementName) throws ArtifactHandlerException {
		try {
			Node parent = document.getDocumentElement();
			NodeList existing = (NodeList) XmlUtils.evaluate(XPathFactory.newInstance().newXPath(), document, path, javax.xml.xpath.XPathConstants.NODESET);
			for (int i = existing.getLength() - 1; i >= 0; i--) {
				parent.removeChild(existing.item(i));
			}
			for (String current : values) {
				Node created = document.createElement(elementName);
				created.setTextContent(current);
				parent.appendChild(created);
			}
		}
		catch (Exception e) {
			throw new ArtifactHandlerException("Could not rewrite feature set list", e);
		}
	}
}
