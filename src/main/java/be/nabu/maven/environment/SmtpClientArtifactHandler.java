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

public class SmtpClientArtifactHandler extends AbstractXmlArtifactHandler {
	@Override
	public void apply(EnvironmentBuildContext context) throws ArtifactHandlerException {
		File input = new File(context.getProjectDirectory(), "smtp-server.xml");
		if (!input.exists()) {
			context.getLog().debug("Skipping SMTP client handler, file not found: " + input);
			return;
		}
		Document document = parse(input);
		requireRootElement(document, "smtpServer");
		XPath xpath = newXPath();
		replaceNodeValue(context, document, xpath, "/smtpServer/host/text()", value(context, "host"), false);
		replaceNodeValue(context, document, xpath, "/smtpServer/port/text()", value(context, "port"), false);
		replaceNodeValue(context, document, xpath, "/smtpServer/from/text()", value(context, "from"), false);
		replaceNodeValue(
			context,
			node(xpath, document, "/smtpServer/subjectTemplate/text()"),
			value(context, "subjectTemplate"),
			false
		);
		replaceNodeValue(
			context,
			node(xpath, document, "/smtpServer/clientHost/text()"),
			value(context, "clientHost"),
			false
		);
		replaceNodeValue(context, document, xpath, "/smtpServer/charset/text()", value(context, "charset"), false);
		replaceNodeValue(context, document, xpath, "/smtpServer/username/text()", value(context, "username"), false);
		replaceNodeValue(context, document, xpath, "/smtpServer/password/text()", value(context, "password"), true);
		replaceNodeValue(
			context,
			node(xpath, document, "/smtpServer/loginMethod/text()"),
			value(context, "loginMethod"),
			false
		);
		replaceNodeValue(
			context,
			node(xpath, document, "/smtpServer/implicitSSL/text()"),
			value(context, "implicitSSL"),
			false
		);
		replaceNodeValue(context, document, xpath, "/smtpServer/startTls/text()", value(context, "startTls"), false);
		replaceNodeValue(context, document, xpath, "/smtpServer/keystore/text()", value(context, "keystore"), false);
		replaceNodeValue(context, document, xpath, "/smtpServer/blacklist/text()", value(context, "blacklist"), false);
		replaceNodeValue(
			context,
			node(xpath, document, "/smtpServer/overrideToInMime/text()"),
			value(context, "overrideToInMime"),
			false
		);
		replaceNodeValue(
			context,
			node(xpath, document, "/smtpServer/connectionTimeout/text()"),
			value(context, "connectionTimeout"),
			false
		);
		replaceNodeValue(
			context,
			node(xpath, document, "/smtpServer/socketTimeout/text()"),
			value(context, "socketTimeout"),
			false
		);
		applyList(context, document, xpath, "bcc");
		applyList(context, document, xpath, "overrideTo");
		write(document, new File(context.getOutputDirectory(), "smtp-server.xml"));
	}

	private void applyList(EnvironmentBuildContext context, Document document, XPath xpath, String key) throws ArtifactHandlerException {
		List<String> list = EnvironmentValues.list(context, key, true);
		if (list == null) {
			return;
		}
		for (int i = 0; i < list.size(); i++) {
			replaceNodeValue(
				context,
				node(xpath, document, "/smtpServer/" + key + "[" + (i + 1) + "]/text()"),
				list.get(i),
				false
			);
		}
	}
}
