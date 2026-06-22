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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ArtifactHandlers {
	private ArtifactHandlers() {}

	public static List<ArtifactHandler> resolveHandlers(List<String> names) {
		List<String> requested = names == null || names.isEmpty()
			? Arrays.asList(
				"jdbcPool",
				"webApplication",
				"configuration",
				"httpClient",
				"httpServer",
				"smtpClient",
				"restClient",
				"restEndpoint",
				"swaggerClient",
				"wsdlClient",
				"virtualHost",
				"executor",
				"featureSet",
				"compressor",
				"hazelcastCluster",
				"icapVirusScanner",
				"jwk",
				"channel",
				"waf",
				"odataClient",
				"wiki"
			)
			: names;
		Map<String, ArtifactHandler> available = availableHandlers();
		List<ArtifactHandler> handlers = new ArrayList<ArtifactHandler>();
		for (String name : requested) {
			ArtifactHandler handler = available.get(name);
			if (handler == null) {
				throw new IllegalArgumentException("Unknown artifact handler: " + name);
			}
			handlers.add(handler);
		}
		return handlers;
	}

	private static Map<String, ArtifactHandler> availableHandlers() {
		Map<String, ArtifactHandler> handlers = new LinkedHashMap<String, ArtifactHandler>();
		handlers.put("jdbcPool", new JdbcPoolArtifactHandler());
		handlers.put("webApplication", new WebApplicationArtifactHandler());
		handlers.put("configuration", new ConfigurationArtifactHandler());
		handlers.put("httpClient", new HttpClientArtifactHandler());
		handlers.put("httpServer", new HttpServerArtifactHandler());
		handlers.put("smtpClient", new SmtpClientArtifactHandler());
		handlers.put("restClient", new RestClientArtifactHandler());
		handlers.put("restEndpoint", new RestEndpointArtifactHandler());
		handlers.put("swaggerClient", new SwaggerClientArtifactHandler());
		handlers.put("wsdlClient", new WsdlClientArtifactHandler());
		handlers.put("virtualHost", new VirtualHostArtifactHandler());
		handlers.put("executor", new ExecutorArtifactHandler());
		handlers.put("featureSet", new FeatureSetArtifactHandler());
		handlers.put("compressor", new CompressorArtifactHandler());
		handlers.put("hazelcastCluster", new HazelcastClusterArtifactHandler());
		handlers.put("icapVirusScanner", new IcapVirusScannerArtifactHandler());
		handlers.put("jwk", new JwkArtifactHandler());
		handlers.put("channel", new ChannelArtifactHandler());
		handlers.put("waf", new WafArtifactHandler());
		handlers.put("odataClient", new ODataClientArtifactHandler());
		handlers.put("wiki", new WikiArtifactHandler());
		return handlers;
	}
}
