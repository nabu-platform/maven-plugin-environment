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
import java.util.List;
import javax.xml.xpath.XPath;
import org.w3c.dom.Document;

public class SwaggerClientArtifactHandler extends AbstractXmlArtifactHandler {
	@Override
	public void apply(EnvironmentBuildContext context) throws ArtifactHandlerException {
		File input = new File(context.getProjectDirectory(), "swagger-client.xml");
		if (!input.exists()) {
			context.getLog().debug("Skipping Swagger client handler, file not found: " + input);
			return;
		}
		Document document = parse(input);
		XPath xpath = newXPath();
		replaceNodeValue(context, node(xpath, document, "/swaggerClient/host/text()"), value(context, "host"), false);
		replaceNodeValue(
			context,
			node(xpath, document, "/swaggerClient/basePath/text()"),
			value(context, "basePath"),
			false
		);
		replaceNodeValue(context, node(xpath, document, "/swaggerClient/scheme/text()"), value(context, "scheme"), false);
		replaceNodeValue(context, node(xpath, document, "/swaggerClient/charset/text()"), value(context, "charset"), false);
		replaceNodeValue(
			context,
			node(xpath, document, "/swaggerClient/httpClient/text()"),
			value(context, "httpClient"),
			false
		);
		replaceNodeValue(
			context,
			node(xpath, document, "/swaggerClient/username/text()"),
			value(context, "username"),
			false
		);
		replaceNodeValue(
			context,
			node(xpath, document, "/swaggerClient/password/text()"),
			value(context, "password"),
			false
		);
		replaceNodeValue(
			context,
			node(xpath, document, "/swaggerClient/allowDomain/text()"),
			value(context, "allowDomain"),
			false
		);
		replaceNodeValue(
			context,
			node(xpath, document, "/swaggerClient/apiHeaderName/text()"),
			value(context, "apiHeaderName"),
			false
		);
		replaceNodeValue(
			context,
			node(xpath, document, "/swaggerClient/apiQueryName/text()"),
			value(context, "apiQueryName"),
			false
		);
		replaceNodeValue(
			context,
			node(xpath, document, "/swaggerClient/apiQueryKey/text()"),
			value(context, "apiQueryKey"),
			false
		);
		replaceNodeValue(
			context,
			node(xpath, document, "/swaggerClient/apiHeaderKey/text()"),
			value(context, "apiHeaderKey"),
			false
		);
		replaceNodeValue(
			context,
			node(xpath, document, "/swaggerClient/bearerToken/text()"),
			value(context, "bearerToken"),
			false
		);
		replaceNodeValue(
			context,
			node(xpath, document, "/swaggerClient/supportGzip/text()"),
			value(context, "supportGzip"),
			false
		);
		applySecurity(context, document, xpath);
		write(document, new File(context.getOutputDirectory(), "swagger-client.xml"));
	}

	private void applySecurity(EnvironmentBuildContext context, Document document, XPath xpath) throws ArtifactHandlerException {
		List<String> security = EnvironmentValues.list(context, "security", true);
		if (security == null) {
			return;
		}
		for (int i = 0; i < security.size(); i++) {
			replaceNodeValue(
				context,
				node(xpath, document, "/swaggerClient/security[" + (i + 1) + "]/text()"),
				security.get(i),
				false
			);
		}
	}
}
