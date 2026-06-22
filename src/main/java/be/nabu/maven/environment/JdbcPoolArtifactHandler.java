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

public class JdbcPoolArtifactHandler extends AbstractXmlArtifactHandler {
	@Override
	public void apply(EnvironmentBuildContext context) throws ArtifactHandlerException {
		File input = new File(context.getProjectDirectory(), "jdbcPool.xml");
		if (!input.exists()) {
			context.getLog().debug("Skipping JDBC pool handler, file not found: " + input);
			return;
		}
		Document document = parse(input);
		XPath xpath = newXPath();
		replaceNodeValue(context, node(xpath, document, "/jdbcPool/poolProxy/text()"), value(context, "poolProxy"), false);
		replaceNodeValue(context, node(xpath, document, "/jdbcPool/jdbcUrl/text()"), value(context, "jdbcUrl"), false);
		replaceNodeValue(
			context,
			node(xpath, document, "/jdbcPool/driverClassName/text()"),
			value(context, "driverClassName"),
			false
		);
		replaceNodeValue(context, node(xpath, document, "/jdbcPool/dialect/text()"), value(context, "dialect"), false);
		replaceNodeValue(context, node(xpath, document, "/jdbcPool/username/text()"), value(context, "username"), false);
		replaceNodeValue(context, node(xpath, document, "/jdbcPool/password/text()"), value(context, "password"), true);
		replaceNodeValue(
			context,
			node(xpath, document, "/jdbcPool/maximumPoolSize/text()"),
			value(context, "maximumPoolSize"),
			false
		);
		replaceNodeValue(
			context,
			node(xpath, document, "/jdbcPool/minimumIdle/text()"),
			value(context, "minimumIdle"),
			false
		);
		replaceNodeValue(
			context,
			node(xpath, document, "/jdbcPool/connectionTimeout/text()"),
			value(context, "connectionTimeout"),
			false
		);
		replaceNodeValue(
			context,
			node(xpath, document, "/jdbcPool/idleTimeout/text()"),
			value(context, "idleTimeout"),
			false
		);
		write(document, new File(context.getOutputDirectory(), "jdbcPool.xml"));
	}
}
