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

public class RestClientArtifactHandler extends AbstractXmlArtifactHandler {

	@Override
	public void apply(EnvironmentBuildContext context) throws ArtifactHandlerException {
		File input = new File(context.getProjectDirectory(), "rest-client.xml");
		if (!input.exists()) {
			context.getLog().debug("Skipping REST client handler, file not found: " + input);
			return;
		}
		Document document = parse(input);
		XPath xpath = newXPath();
		replaceNodeValue(context, node(xpath, document, "/restClient/host/text()"), value(context, "host"), false);
		replaceNodeValue(context, node(xpath, document, "/restClient/path/text()"), value(context, "path"), false);
		replaceNodeValue(context, node(xpath, document, "/restClient/secure/text()"), value(context, "secure"), false);
		replaceNodeValue(context, node(xpath, document, "/restClient/httpClient/text()"), value(context, "httpClient"), false);
		replaceNodeValue(context, node(xpath, document, "/restClient/username/text()"), value(context, "username"), false);
		replaceNodeValue(context, node(xpath, document, "/restClient/password/text()"), value(context, "password"), false);
		replaceNodeValue(context, node(xpath, document, "/restClient/gzip/text()"), value(context, "gzip"), false);
		write(document, new File(context.getOutputDirectory(), "rest-client.xml"));
	}
}
