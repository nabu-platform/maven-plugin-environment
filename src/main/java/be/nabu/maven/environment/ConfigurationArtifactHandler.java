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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.xml.xpath.XPath;
import org.w3c.dom.Document;

public class ConfigurationArtifactHandler extends AbstractXmlArtifactHandler {
	@Override
	public void apply(EnvironmentBuildContext context) throws ArtifactHandlerException {
		File configuration = new File(context.getProjectDirectory(), "configuration.xml");
		File definition = new File(context.getProjectDirectory(), "definition.xml");
		if (!configuration.exists() || !definition.exists()) {
			context.getLog()
				.debug("Skipping configuration handler, required files not found in " + context.getProjectDirectory());
			return;
		}
		Document document = parse(configuration);
		DefinitionModel definitionModel = DefinitionParser.parse(definition);
		XPath xpath = newXPath();
		Map<String, String> scalarValues = new LinkedHashMap<String, String>();
		for (Map.Entry<String, String> entry : context.getProviderValues()
			.entrySet()) {
			String key = entry.getKey();
			if (key.contains(".") || key.endsWith("]") || key.contains(":")) {
				continue;
			}
			scalarValues.put(key, entry.getValue());
		}
		for (Map.Entry<String, String> entry : context.getFixedValues()
			.entrySet()) {
			String key = entry.getKey();
			if (key.contains(".") || key.endsWith("]") || key.contains(":") || scalarValues.containsKey(key)) {
				continue;
			}
			scalarValues.put(key, entry.getValue());
		}
		for (Map.Entry<String, String> entry : scalarValues.entrySet()) {
			DefinitionField field = definitionModel.getField(entry.getKey());
			if (field == null || !field.isEnvironmentSpecific()) {
				continue;
			}
			if (field.isList()) {
				List<String> list = EnvironmentValues.list(context, entry.getKey(), true);
				if (list != null) {
					for (int i = 0; i < list.size(); i++) {
						replaceNodeValue(
							context,
							node(xpath, document, "/configuration/" + entry.getKey() + "[" + (i + 1) + "]/text()"),
							list.get(i),
							false
						);
					}
				}
			}
			else {
				replaceNodeValue(
					context,
					node(xpath, document, "/configuration/" + entry.getKey() + "/text()"),
					entry.getValue(),
					false
				);
			}
		}
		write(document, new File(context.getOutputDirectory(), "configuration.xml"));
	}
}
