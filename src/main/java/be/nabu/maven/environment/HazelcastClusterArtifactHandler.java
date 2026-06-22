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

public class HazelcastClusterArtifactHandler extends AbstractXmlArtifactHandler {

	@Override
	public void apply(EnvironmentBuildContext context) throws ArtifactHandlerException {
		File input = new File(context.getProjectDirectory(), "hazelcast-cluster.xml");
		if (!input.exists()) {
			context.getLog().debug("Skipping hazelcast cluster handler, file not found: " + input);
			return;
		}
		Document document = parse(input);
		XPath xpath = newXPath();
		replaceNodeValue(context, node(xpath, document, "/hazelcastCluster/port/text()"), value(context, "port"), false);
		replaceNodeValue(context, node(xpath, document, "/hazelcastCluster/hazelcastPort/text()"), value(context, "hazelcastPort"), false);
		replaceNodeValue(context, node(xpath, document, "/hazelcastCluster/amazonTagKey/text()"), value(context, "amazonTagKey"), false);
		replaceNodeValue(context, node(xpath, document, "/hazelcastCluster/amazonTagValue/text()"), value(context, "amazonTagValue"), false);
		replaceNodeValue(context, node(xpath, document, "/hazelcastCluster/amazonRegion/text()"), value(context, "amazonRegion"), false);
		write(document, new File(context.getOutputDirectory(), "hazelcast-cluster.xml"));
	}
}
