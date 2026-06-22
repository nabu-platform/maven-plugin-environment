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
import java.util.Map;

import javax.xml.xpath.XPath;

import org.w3c.dom.Document;

public class ChannelArtifactHandler extends AbstractXmlArtifactHandler {

	@Override
	public void apply(EnvironmentBuildContext context) throws ArtifactHandlerException {
		File input = new File(context.getProjectDirectory(), "channel.xml");
		if (!input.exists()) {
			context.getLog().debug("Skipping channel handler, file not found: " + input);
			return;
		}
		Document document = parse(input);
		XPath xpath = newXPath();
		for (Map.Entry<String, String> entry : context.getValues().entrySet()) {
			if (entry.getKey().contains(".") || entry.getKey().endsWith("]")) {
				continue;
			}
			replaceNodeValue(context, node(xpath, document, "/channel/properties/property[@key='" + entry.getKey() + "']/text()"), entry.getValue(), false);
		}
		write(document, new File(context.getOutputDirectory(), "channel.xml"));
	}
}
