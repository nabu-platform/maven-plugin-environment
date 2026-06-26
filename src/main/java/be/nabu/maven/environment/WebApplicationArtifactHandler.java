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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.xml.xpath.XPath;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.w3c.dom.Document;

public class WebApplicationArtifactHandler extends AbstractXmlArtifactHandler {
	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

	@Override
	public void apply(EnvironmentBuildContext context) throws ArtifactHandlerException {
		applyWebArtifact(context);
		applyFragments(context);
		minifyPageJson(context);
	}

	private void applyWebArtifact(EnvironmentBuildContext context) throws ArtifactHandlerException {
		File input = new File(context.getProjectDirectory(), "webartifact.xml");
		if (!input.exists()) {
			context.getLog().debug("Skipping web application handler, file not found: " + input);
			return;
		}
		Document document = parse(input);
		XPath xpath = newXPath();
		replaceNodeValue(
			context,
			node(xpath, document, "/webartifact/virtualHost/text()"),
			value(context, "virtualHost"),
			false
		);
		replaceNodeValue(context, node(xpath, document, "/webartifact/path/text()"), value(context, "path"), false);
		write(document, new File(context.getOutputDirectory(), "webartifact.xml"));
	}

	private void applyFragments(EnvironmentBuildContext context) throws ArtifactHandlerException {
		File input = new File(context.getProjectDirectory(), "fragments.xml");
		if (!input.exists()) {
			context.getLog().debug("Skipping web application fragments, file not found: " + input);
			return;
		}
		Document document = parse(input);
		XPath xpath = newXPath();
		Map<String, DefinitionModel> definitions = loadFragmentDefinitions(context);
		Map<String, String> fragmentScalars = new LinkedHashMap<String, String>();
		for (Map.Entry<String, String> entry : context.getProviderValues()
			.entrySet()) {
			if (!entry.getKey().startsWith("fragment.")) {
				continue;
			}
			String property = entry.getKey().substring("fragment.".length());
			if (property.endsWith("]")) {
				continue;
			}
			fragmentScalars.put(property, entry.getValue());
		}
		for (Map.Entry<String, String> entry : context.getFixedValues()
			.entrySet()) {
			if (!entry.getKey().startsWith("fragment.")) {
				continue;
			}
			String property = entry.getKey().substring("fragment.".length());
			if (property.endsWith("]") || fragmentScalars.containsKey(property)) {
				continue;
			}
			fragmentScalars.put(property, entry.getValue());
		}
		for (Map.Entry<String, String> entry : fragmentScalars.entrySet()) {
			for (Map.Entry<String, DefinitionModel> definitionEntry : definitions.entrySet()) {
				DefinitionField field = definitionEntry.getValue().getField(entry.getKey());
				if (field == null || !field.isEnvironmentSpecific()) {
					continue;
				}
				if (field.isList()) {
					List<String> list = EnvironmentValues.list(context, "fragment." + entry.getKey(), true);
					if (list != null) {
						for (int i = 0; i < list.size(); i++) {
							replaceNodeValue(
								context,
								node(
									xpath,
									document,
									"/fragments/parts[type='" + definitionEntry.getKey() + "']/configuration/property[@key='"
										+ entry.getKey() + "'][" + (i + 1) + "]/@value"
								),
								list.get(i),
								false
							);
						}
					}
				}
				else {
					replaceNodeValue(
						context,
						node(
							xpath,
							document,
							"/fragments/parts[type='" + definitionEntry.getKey() + "']/configuration/property[@key='"
								+ entry.getKey() + "']/@value"
						),
						entry.getValue(),
						false
					);
				}
			}
		}
		write(document, new File(context.getOutputDirectory(), "fragments.xml"));
	}

	private void minifyPageJson(EnvironmentBuildContext context) throws ArtifactHandlerException {
		if (!context.isEnabled("webApplication.minifyPageJson")) {
			return;
		}
		File pagesDirectory = new File(context.getProjectDirectory(), "public/artifacts/pages");
		if (!pagesDirectory.isDirectory()) {
			context.getLog()
				.debug("Skipping web application page json minification, directory not found: " + pagesDirectory);
			return;
		}
		File outputPagesDirectory = new File(context.getOutputDirectory(), "public/artifacts/pages");
		File[] files = pagesDirectory.listFiles();
		if (files == null) {
			return;
		}
		for (File file : files) {
			if (!file.isFile() || !file.getName().endsWith(".json")) {
				continue;
			}
			minifyJsonFile(file, new File(outputPagesDirectory, file.getName()));
		}
	}

	private void minifyJsonFile(File input, File output) throws ArtifactHandlerException {
		try {
			Object parsed = OBJECT_MAPPER.readValue(input, Object.class);
			if (output.getParentFile() != null
					&& !output.getParentFile()
						.exists()
					&& !output.getParentFile()
						.mkdirs()) {
				throw new ArtifactHandlerException("Could not create output directory for " + output);
			}
			Files.write(output.toPath(), OBJECT_MAPPER.writeValueAsString(parsed).getBytes(StandardCharsets.UTF_8));
		}
		catch (ArtifactHandlerException e) {
			throw e;
		}
		catch (Exception e) {
			throw new ArtifactHandlerException("Could not minify page json file: " + input, e);
		}
	}

	private Map<String, DefinitionModel> loadFragmentDefinitions(EnvironmentBuildContext context) throws ArtifactHandlerException {
		Map<String, DefinitionModel> definitions = new LinkedHashMap<String, DefinitionModel>();
		File[] files = context.getProjectDirectory().listFiles();
		if (files == null) {
			return definitions;
		}
		for (File file : files) {
			String name = file.getName();
			if (!name.startsWith("definition-") || !name.endsWith(".xml")) {
				continue;
			}
			String type = name.substring("definition-".length(), name.length() - 4);
			definitions.put(type, DefinitionParser.parse(file));
		}
		return definitions;
	}
}
