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

import javax.xml.xpath.XPath;

import org.w3c.dom.Document;

public class ODataClientArtifactHandler extends AbstractXmlArtifactHandler {

	@Override
	public void apply(EnvironmentBuildContext context) throws ArtifactHandlerException {
		File input = new File(context.getProjectDirectory(), "odata-client.xml");
		if (!input.exists()) {
			context.getLog().debug("Skipping OData client handler, file not found: " + input);
			return;
		}
		Document document = parse(input);
		XPath xpath = newXPath();
		replaceNodeValue(context, node(xpath, document, "/odataClient/endpoint/text()"), value(context, "endpoint"), false);
		replaceNodeValue(context, node(xpath, document, "/odataClient/securityType/text()"), value(context, "securityType"), false);
		replaceNodeValue(context, node(xpath, document, "/odataClient/securityContext/text()"), value(context, "securityContext"), false);
		write(document, new File(context.getOutputDirectory(), "odata-client.xml"));
	}
}
