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

public class JwkArtifactHandler extends AbstractXmlArtifactHandler {
	@Override
	public void apply(EnvironmentBuildContext context) throws ArtifactHandlerException {
		File input = new File(context.getProjectDirectory(), "jwk.xml");
		if (!input.exists()) {
			context.getLog().debug("Skipping JWK handler, file not found: " + input);
			return;
		}
		Document document = parse(input);
		XPath xpath = newXPath();
		List<String> uris = EnvironmentValues.list(context, "uris", true);
		if (uris != null) {
			for (int i = 0; i < uris.size(); i++) {
				replaceNodeValue(context, node(xpath, document, "/jwk/uris[" + (i + 1) + "]/text()"), uris.get(i), false);
			}
		}
		write(document, new File(context.getOutputDirectory(), "jwk.xml"));
	}
}
