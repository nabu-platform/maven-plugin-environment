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

public class VirtualHostArtifactHandler extends AbstractXmlArtifactHandler {
	@Override
	public void apply(EnvironmentBuildContext context) throws ArtifactHandlerException {
		File input = new File(context.getProjectDirectory(), "virtual-host.xml");
		if (!input.exists()) {
			context.getLog().debug("Skipping virtual host handler, file not found: " + input);
			return;
		}
		Document document = parse(input);
		XPath xpath = newXPath();
		replaceNodeValue(context, node(xpath, document, "/virtualHost/host/text()"), value(context, "host"), false);
		replaceNodeValue(context, node(xpath, document, "/virtualHost/server/text()"), value(context, "server"), false);
		replaceNodeValue(context, node(xpath, document, "/virtualHost/keyAlias/text()"), value(context, "keyAlias"), false);
		replaceNodeValue(
			context,
			node(xpath, document, "/virtualHost/enableHsts/text()"),
			value(context, "enableHsts"),
			false
		);
		replaceNodeValue(
			context,
			node(xpath, document, "/virtualHost/hstsPreload/text()"),
			value(context, "hstsPreload"),
			false
		);
		replaceNodeValue(
			context,
			node(xpath, document, "/virtualHost/hstsSubDomains/text()"),
			value(context, "hstsSubDomains"),
			false
		);
		replaceNodeValue(
			context,
			node(xpath, document, "/virtualHost/hstsMaxAge/text()"),
			value(context, "hstsMaxAge"),
			false
		);
		replaceNodeValue(
			context,
			node(xpath, document, "/virtualHost/captureErrors/text()"),
			value(context, "captureErrors"),
			false
		);
		replaceNodeValue(
			context,
			node(xpath, document, "/virtualHost/captureSuccessful/text()"),
			value(context, "captureSuccessful"),
			false
		);
		replaceNodeValue(
			context,
			node(xpath, document, "/virtualHost/enableRangeSupport/text()"),
			value(context, "enableRangeSupport"),
			false
		);
		replaceNodeValue(
			context,
			node(xpath, document, "/virtualHost/enableCompression/text()"),
			value(context, "enableCompression"),
			false
		);
		applyList(context, document, xpath, "aliases");
		applyList(context, document, xpath, "redirectAliases");
		write(document, new File(context.getOutputDirectory(), "virtual-host.xml"));
	}

	private void applyList(EnvironmentBuildContext context, Document document, XPath xpath, String key) throws ArtifactHandlerException {
		List<String> list = EnvironmentValues.list(context, key, true);
		if (list == null) {
			return;
		}
		for (int i = 0; i < list.size(); i++) {
			replaceNodeValue(
				context,
				node(xpath, document, "/virtualHost/" + key + "[" + (i + 1) + "]/text()"),
				list.get(i),
				false
			);
		}
	}
}
