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

public class HttpServerArtifactHandler extends AbstractXmlArtifactHandler {
	@Override
	public void apply(EnvironmentBuildContext context) throws ArtifactHandlerException {
		File input = new File(context.getProjectDirectory(), "httpServer.xml");
		if (!input.exists()) {
			context.getLog().debug("Skipping HTTP server handler, file not found: " + input);
			return;
		}
		Document document = parse(input);
		requireRootElement(document, "httpServer");
		XPath xpath = newXPath();
		replaceNodeValue(context, document, xpath, "/httpServer/enabled/text()", value(context, "enabled"), false);
		replaceNodeValue(context, document, xpath, "/httpServer/port/text()", value(context, "port"), false);
		replaceNodeValue(
			context,
			document,
			xpath,
			"/httpServer/offlinePort/text()",
			value(context, "offlinePort"),
			false
		);
		replaceNodeValue(
			context,
			document,
			xpath,
			"/httpServer/conversationIdHeaderMapping/text()",
			value(context, "conversationIdHeaderMapping"),
			false
		);
		replaceNodeValue(
			context,
			document,
			xpath,
			"/httpServer/redirectTo/text()",
			value(context, "redirectTo"),
			false
		);
		replaceNodeValue(context, document, xpath, "/httpServer/proxied/text()", value(context, "proxied"), false);
		replaceNodeValue(context, document, xpath, "/httpServer/nabuProxy/text()", value(context, "nabuProxy"), false);
		replaceNodeValue(
			context,
			document,
			xpath,
			"/httpServer/proxySecure/text()",
			value(context, "proxySecure"),
			false
		);
		replaceNodeValue(context, document, xpath, "/httpServer/proxyPort/text()", value(context, "proxyPort"), false);
		replaceNodeValue(context, document, xpath, "/httpServer/keystore/text()", value(context, "keystore"), false);
		replaceNodeValue(
			context,
			document,
			xpath,
			"/httpServer/sslServerMode/text()",
			value(context, "sslServerMode"),
			false
		);
		replaceNodeValue(
			context,
			document,
			xpath,
			"/httpServer/maxSizePerRequest/text()",
			value(context, "maxSizePerRequest"),
			false
		);
		replaceNodeValue(
			context,
			document,
			xpath,
			"/httpServer/ioPoolSize/text()",
			value(context, "ioPoolSize"),
			false
		);
		replaceNodeValue(context, document, xpath, "/httpServer/poolSize/text()", value(context, "poolSize"), false);
		replaceNodeValue(
			context,
			document,
			xpath,
			"/httpServer/maxTotalConnections/text()",
			value(context, "maxTotalConnections"),
			false
		);
		replaceNodeValue(
			context,
			document,
			xpath,
			"/httpServer/maxConnectionsPerClient/text()",
			value(context, "maxConnectionsPerClient"),
			false
		);
		replaceNodeValue(
			context,
			document,
			xpath,
			"/httpServer/idleTimeout/text()",
			value(context, "idleTimeout"),
			false
		);
		replaceNodeValue(context, document, xpath, "/httpServer/lifetime/text()", value(context, "lifetime"), false);
		replaceNodeValue(
			context,
			document,
			xpath,
			"/httpServer/maxInitialLineLength/text()",
			value(context, "maxInitialLineLength"),
			false
		);
		replaceNodeValue(
			context,
			document,
			xpath,
			"/httpServer/maxHeaderSize/text()",
			value(context, "maxHeaderSize"),
			false
		);
		replaceNodeValue(
			context,
			document,
			xpath,
			"/httpServer/maxChunkSize/text()",
			value(context, "maxChunkSize"),
			false
		);
		replaceNodeValue(
			context,
			document,
			xpath,
			"/httpServer/readTimeout/text()",
			value(context, "readTimeout"),
			false
		);
		replaceNodeValue(
			context,
			document,
			xpath,
			"/httpServer/writeTimeout/text()",
			value(context, "writeTimeout"),
			false
		);
		write(document, new File(context.getOutputDirectory(), "httpServer.xml"));
	}
}
