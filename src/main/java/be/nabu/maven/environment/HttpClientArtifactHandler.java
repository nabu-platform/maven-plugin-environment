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

public class HttpClientArtifactHandler extends AbstractXmlArtifactHandler {
	@Override
	public void apply(EnvironmentBuildContext context) throws ArtifactHandlerException {
		File input = new File(context.getProjectDirectory(), "http-client.xml");
		if (!input.exists()) {
			context.getLog().debug("Skipping HTTP client handler, file not found: " + input);
			return;
		}
		Document document = parse(input);
		XPath xpath = newXPath();
		replaceNodeValue(context, node(xpath, document, "/httpClient/type/text()"), value(context, "type"), false);
		replaceNodeValue(context, node(xpath, document, "/httpClient/static/text()"), value(context, "static"), false);
		replaceNodeValue(context, node(xpath, document, "/httpClient/keystore/text()"), value(context, "keystore"), false);
		replaceNodeValue(
			context,
			node(xpath, document, "/httpClient/sslContextType/text()"),
			value(context, "sslContextType"),
			false
		);
		replaceNodeValue(
			context,
			node(xpath, document, "/httpClient/captureErrors/text()"),
			value(context, "captureErrors"),
			false
		);
		replaceNodeValue(
			context,
			node(xpath, document, "/httpClient/captureSuccessful/text()"),
			value(context, "captureSuccessful"),
			false
		);
		replaceNodeValue(
			context,
			node(xpath, document, "/httpClient/ioPoolSize/text()"),
			value(context, "ioPoolSize"),
			false
		);
		replaceNodeValue(
			context,
			node(xpath, document, "/httpClient/processPoolSize/text()"),
			value(context, "processPoolSize"),
			false
		);
		replaceNodeValue(
			context,
			node(xpath, document, "/httpClient/connectionTimeout/text()"),
			value(context, "connectionTimeout"),
			false
		);
		replaceNodeValue(
			context,
			node(xpath, document, "/httpClient/socketTimeout/text()"),
			value(context, "socketTimeout"),
			false
		);
		replaceNodeValue(
			context,
			node(xpath, document, "/httpClient/maxAmountOfConnectionsPerTarget/text()"),
			value(context, "maxAmountOfConnectionsPerTarget"),
			false
		);
		write(document, new File(context.getOutputDirectory(), "http-client.xml"));
	}
}
