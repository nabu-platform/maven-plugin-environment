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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ArtifactAliases {
	private ArtifactAliases() {}

	public static Map<String, AliasTarget> resolveAliases(List<ArtifactHandler> handlers) {
		Map<String, AliasTarget> aliases = new LinkedHashMap<String, AliasTarget>();
		for (ArtifactHandler handler : handlers) {
			if (handler instanceof JdbcPoolArtifactHandler) {
				jdbcPool(aliases);
			}
			else if (handler instanceof HttpServerArtifactHandler) {
				httpServer(aliases);
			}
			else if (handler instanceof VirtualHostArtifactHandler) {
				virtualHost(aliases);
			}
			else if (handler instanceof SwaggerClientArtifactHandler) {
				swaggerClient(aliases);
			}
		}
		return aliases;
	}

	private static void jdbcPool(Map<String, AliasTarget> aliases) {
		alias(aliases, "poolProxy", "jdbcPool.xml", "/jdbcPool/poolProxy/text()", false);
		alias(aliases, "jdbcUrl", "jdbcPool.xml", "/jdbcPool/jdbcUrl/text()", false);
		alias(aliases, "driverClassName", "jdbcPool.xml", "/jdbcPool/driverClassName/text()", false);
		alias(aliases, "dialect", "jdbcPool.xml", "/jdbcPool/dialect/text()", false);
		alias(aliases, "username", "jdbcPool.xml", "/jdbcPool/username/text()", false);
		alias(aliases, "password", "jdbcPool.xml", "/jdbcPool/password/text()", true);
		alias(aliases, "maximumPoolSize", "jdbcPool.xml", "/jdbcPool/maximumPoolSize/text()", false);
		alias(aliases, "minimumIdle", "jdbcPool.xml", "/jdbcPool/minimumIdle/text()", false);
		alias(aliases, "connectionTimeout", "jdbcPool.xml", "/jdbcPool/connectionTimeout/text()", false);
		alias(aliases, "idleTimeout", "jdbcPool.xml", "/jdbcPool/idleTimeout/text()", false);
	}

	private static void httpServer(Map<String, AliasTarget> aliases) {
		alias(aliases, "enabled", "httpServer.xml", "/httpServer/enabled/text()", false);
		alias(aliases, "port", "httpServer.xml", "/httpServer/port/text()", false);
		alias(aliases, "offlinePort", "httpServer.xml", "/httpServer/offlinePort/text()", false);
		alias(aliases, "conversationIdHeaderMapping", "httpServer.xml", "/httpServer/conversationIdHeaderMapping/text()", false);
		alias(aliases, "redirectTo", "httpServer.xml", "/httpServer/redirectTo/text()", false);
		alias(aliases, "proxied", "httpServer.xml", "/httpServer/proxied/text()", false);
		alias(aliases, "nabuProxy", "httpServer.xml", "/httpServer/nabuProxy/text()", false);
		alias(aliases, "proxySecure", "httpServer.xml", "/httpServer/proxySecure/text()", false);
		alias(aliases, "proxyPort", "httpServer.xml", "/httpServer/proxyPort/text()", false);
		alias(aliases, "keystore", "httpServer.xml", "/httpServer/keystore/text()", false);
		alias(aliases, "sslServerMode", "httpServer.xml", "/httpServer/sslServerMode/text()", false);
		alias(aliases, "maxSizePerRequest", "httpServer.xml", "/httpServer/maxSizePerRequest/text()", false);
		alias(aliases, "ioPoolSize", "httpServer.xml", "/httpServer/ioPoolSize/text()", false);
		alias(aliases, "poolSize", "httpServer.xml", "/httpServer/poolSize/text()", false);
		alias(aliases, "maxTotalConnections", "httpServer.xml", "/httpServer/maxTotalConnections/text()", false);
		alias(aliases, "maxConnectionsPerClient", "httpServer.xml", "/httpServer/maxConnectionsPerClient/text()", false);
		alias(aliases, "idleTimeout", "httpServer.xml", "/httpServer/idleTimeout/text()", false);
		alias(aliases, "lifetime", "httpServer.xml", "/httpServer/lifetime/text()", false);
		alias(aliases, "maxInitialLineLength", "httpServer.xml", "/httpServer/maxInitialLineLength/text()", false);
		alias(aliases, "maxHeaderSize", "httpServer.xml", "/httpServer/maxHeaderSize/text()", false);
		alias(aliases, "maxChunkSize", "httpServer.xml", "/httpServer/maxChunkSize/text()", false);
		alias(aliases, "readTimeout", "httpServer.xml", "/httpServer/readTimeout/text()", false);
		alias(aliases, "writeTimeout", "httpServer.xml", "/httpServer/writeTimeout/text()", false);
	}

	private static void virtualHost(Map<String, AliasTarget> aliases) {
		alias(aliases, "host", "virtual-host.xml", "/virtualHost/host/text()", false);
		alias(aliases, "server", "virtual-host.xml", "/virtualHost/server/text()", false);
		alias(aliases, "keyAlias", "virtual-host.xml", "/virtualHost/keyAlias/text()", false);
		alias(aliases, "enableHsts", "virtual-host.xml", "/virtualHost/enableHsts/text()", false);
		alias(aliases, "hstsPreload", "virtual-host.xml", "/virtualHost/hstsPreload/text()", false);
		alias(aliases, "hstsSubDomains", "virtual-host.xml", "/virtualHost/hstsSubDomains/text()", false);
		alias(aliases, "hstsMaxAge", "virtual-host.xml", "/virtualHost/hstsMaxAge/text()", false);
		alias(aliases, "captureErrors", "virtual-host.xml", "/virtualHost/captureErrors/text()", false);
		alias(aliases, "captureSuccessful", "virtual-host.xml", "/virtualHost/captureSuccessful/text()", false);
		alias(aliases, "enableRangeSupport", "virtual-host.xml", "/virtualHost/enableRangeSupport/text()", false);
		alias(aliases, "enableCompression", "virtual-host.xml", "/virtualHost/enableCompression/text()", false);
	}

	private static void swaggerClient(Map<String, AliasTarget> aliases) {
		alias(aliases, "host", "swagger-client.xml", "/swaggerClient/host/text()", false);
		alias(aliases, "basePath", "swagger-client.xml", "/swaggerClient/basePath/text()", false);
		alias(aliases, "scheme", "swagger-client.xml", "/swaggerClient/scheme/text()", false);
		alias(aliases, "charset", "swagger-client.xml", "/swaggerClient/charset/text()", false);
		alias(aliases, "httpClient", "swagger-client.xml", "/swaggerClient/httpClient/text()", false);
		alias(aliases, "username", "swagger-client.xml", "/swaggerClient/username/text()", false);
		alias(aliases, "password", "swagger-client.xml", "/swaggerClient/password/text()", false);
		alias(aliases, "allowDomain", "swagger-client.xml", "/swaggerClient/allowDomain/text()", false);
		alias(aliases, "apiHeaderName", "swagger-client.xml", "/swaggerClient/apiHeaderName/text()", false);
		alias(aliases, "apiQueryName", "swagger-client.xml", "/swaggerClient/apiQueryName/text()", false);
		alias(aliases, "apiQueryKey", "swagger-client.xml", "/swaggerClient/apiQueryKey/text()", false);
		alias(aliases, "apiHeaderKey", "swagger-client.xml", "/swaggerClient/apiHeaderKey/text()", false);
		alias(aliases, "bearerToken", "swagger-client.xml", "/swaggerClient/bearerToken/text()", false);
		alias(aliases, "supportGzip", "swagger-client.xml", "/swaggerClient/supportGzip/text()", false);
	}

	private static void alias(Map<String, AliasTarget> aliases, String alias, String fileName, String query, boolean encrypted) {
		if (!aliases.containsKey(alias)) {
			aliases.put(alias, new AliasTarget(fileName, query, encrypted));
		}
	}
}
