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
import java.util.Map;

public final class ArtifactAliases {
	private ArtifactAliases() {}

	public static Map<String, AliasTarget> resolveAliases(String artifactType) {
		Map<String, AliasTarget> aliases = new LinkedHashMap<String, AliasTarget>();
		System.out.println("[environment-plugin] resolving aliases for artifactType=" + artifactType);
		if (artifactType == null) {
			System.out.println("[environment-plugin] no artifactType available, alias map is empty");
			return aliases;
		}
		if ("be.nabu.eai.module.jdbc.pool.JDBCPoolManager".equals(artifactType)) {
			jdbcPool(aliases);
		}
		else if ("be.nabu.eai.module.http.server.HTTPServerManager".equals(artifactType)) {
			httpServer(aliases);
		}
		else if ("be.nabu.eai.module.http.virtual.VirtualHostManager".equals(artifactType)) {
			virtualHost(aliases);
		}
		else if ("be.nabu.eai.module.swagger.client.SwaggerClientManager".equals(artifactType)) {
			swaggerClient(aliases);
		}
		else if ("be.nabu.eai.module.rest.client.RESTClientManager".equals(artifactType)) {
			restClient(aliases);
		}
		else if ("be.nabu.eai.module.rest.provider.RESTEndpointManager".equals(artifactType)) {
			restEndpoint(aliases);
		}
		else if ("be.nabu.eai.module.http.client.HTTPClientManager".equals(artifactType)) {
			httpClient(aliases);
		}
		else if ("be.nabu.eai.module.smtp.SMTPClientManager".equals(artifactType)) {
			smtpClient(aliases);
		}
		else if ("be.nabu.eai.module.misc.executor.ExecutorManager".equals(artifactType)) {
			executor(aliases);
		}
		else if ("be.nabu.eai.module.misc.features.FeatureSetManager".equals(artifactType)) {
			featureSet(aliases);
		}
		else if ("be.nabu.eai.module.web.application.WebApplicationManager".equals(artifactType)) {
			webApplication(aliases);
		}
		else if ("be.nabu.eai.module.http.virtual.WAFManager".equals(artifactType)) {
			waf(aliases);
		}
		else if ("be.nabu.eai.module.jwk.JWKManager".equals(artifactType)) {
			jwk(aliases);
		}
		else if ("be.nabu.eai.module.misc.compressor.CompressorManager".equals(artifactType)) {
			compressor(aliases);
		}
		else if ("be.nabu.eai.module.http.icap.ICAPVirusScannerManager".equals(artifactType)) {
			icapVirusScanner(aliases);
		}
		else if ("be.nabu.eai.module.http.wiki.WikiManager".equals(artifactType)) {
			wiki(aliases);
		}
		else if ("be.nabu.eai.module.odata.client.ODataClientManager".equals(artifactType)) {
			oDataClient(aliases);
		}
		else if ("be.nabu.eai.module.hazelcast.HazelcastClusterManager".equals(artifactType)) {
			hazelcastCluster(aliases);
		}
		else if ("be.nabu.eai.module.channel.ChannelManager".equals(artifactType)) {
			channel(aliases);
		}
		else if ("be.nabu.eai.module.wsdl.client.WSDLClientManager".equals(artifactType)) {
			wsdlClient(aliases);
		}
		System.out.println("[environment-plugin] alias keys for artifactType " + artifactType + " = " + aliases.keySet());
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

	private static void restEndpoint(Map<String, AliasTarget> aliases) {
		alias(aliases, "host", "rest-endpoint.xml", "/restEndpoint/host/text()", false);
		alias(aliases, "basePath", "rest-endpoint.xml", "/restEndpoint/basePath/text()", false);
		alias(aliases, "secure", "rest-endpoint.xml", "/restEndpoint/secure/text()", false);
		alias(aliases, "httpClient", "rest-endpoint.xml", "/restEndpoint/httpClient/text()", false);
		alias(aliases, "username", "rest-endpoint.xml", "/restEndpoint/username/text()", false);
		alias(aliases, "password", "rest-endpoint.xml", "/restEndpoint/password/text()", false);
		alias(aliases, "apiHeaderName", "rest-endpoint.xml", "/restEndpoint/apiHeaderName/text()", false);
		alias(aliases, "apiQueryName", "rest-endpoint.xml", "/restEndpoint/apiQueryName/text()", false);
		alias(aliases, "apiQueryKey", "rest-endpoint.xml", "/restEndpoint/apiQueryKey/text()", false);
		alias(aliases, "apiHeaderKey", "rest-endpoint.xml", "/restEndpoint/apiHeaderKey/text()", false);
		alias(aliases, "gzip", "rest-endpoint.xml", "/restEndpoint/gzip/text()", false);
	}

	private static void swaggerClient(Map<String, AliasTarget> aliases) {
		alias(aliases, "host", "swagger-client.xml", "/swaggerClient/host/text()", false);
		alias(aliases, "basePath", "swagger-client.xml", "/swaggerClient/basePath/text()", false);
		alias(aliases, "scheme", "swagger-client.xml", "/swaggerClient/scheme/text()", false);
		alias(aliases, "charset", "swagger-client.xml", "/swaggerClient/charset/text()", false);
		alias(aliases, "httpClient", "swagger-client.xml", "/swaggerClient/httpClient/text()", false);
		alias(aliases, "username", "swagger-client.xml", "/swaggerClient/username/text()", false);
		alias(aliases, "password", "swagger-client.xml", "/swaggerClient/password/text()", false);
		alias(aliases, "security", "swagger-client.xml", "/swaggerClient/security", false);
		alias(aliases, "allowDomain", "swagger-client.xml", "/swaggerClient/allowDomain/text()", false);
		alias(aliases, "apiHeaderName", "swagger-client.xml", "/swaggerClient/apiHeaderName/text()", false);
		alias(aliases, "apiQueryName", "swagger-client.xml", "/swaggerClient/apiQueryName/text()", false);
		alias(aliases, "apiQueryKey", "swagger-client.xml", "/swaggerClient/apiQueryKey/text()", false);
		alias(aliases, "apiHeaderKey", "swagger-client.xml", "/swaggerClient/apiHeaderKey/text()", false);
		alias(aliases, "bearerToken", "swagger-client.xml", "/swaggerClient/bearerToken/text()", false);
		alias(aliases, "supportGzip", "swagger-client.xml", "/swaggerClient/supportGzip/text()", false);
	}

	private static void restClient(Map<String, AliasTarget> aliases) {
		alias(aliases, "host", "rest-client.xml", "/restClient/host/text()", false);
		alias(aliases, "path", "rest-client.xml", "/restClient/path/text()", false);
		alias(aliases, "secure", "rest-client.xml", "/restClient/secure/text()", false);
		alias(aliases, "httpClient", "rest-client.xml", "/restClient/httpClient/text()", false);
		alias(aliases, "username", "rest-client.xml", "/restClient/username/text()", false);
		alias(aliases, "password", "rest-client.xml", "/restClient/password/text()", false);
		alias(aliases, "gzip", "rest-client.xml", "/restClient/gzip/text()", false);
	}

	private static void httpClient(Map<String, AliasTarget> aliases) {
		alias(aliases, "type", "http-client.xml", "/httpClient/type/text()", false);
		alias(aliases, "static", "http-client.xml", "/httpClient/static/text()", false);
		alias(aliases, "keystore", "http-client.xml", "/httpClient/keystore/text()", false);
		alias(aliases, "sslContextType", "http-client.xml", "/httpClient/sslContextType/text()", false);
		alias(aliases, "captureErrors", "http-client.xml", "/httpClient/captureErrors/text()", false);
		alias(aliases, "captureSuccessful", "http-client.xml", "/httpClient/captureSuccessful/text()", false);
		alias(aliases, "ioPoolSize", "http-client.xml", "/httpClient/ioPoolSize/text()", false);
		alias(aliases, "processPoolSize", "http-client.xml", "/httpClient/processPoolSize/text()", false);
		alias(aliases, "connectionTimeout", "http-client.xml", "/httpClient/connectionTimeout/text()", false);
		alias(aliases, "socketTimeout", "http-client.xml", "/httpClient/socketTimeout/text()", false);
		alias(aliases, "maxAmountOfConnectionsPerTarget", "http-client.xml", "/httpClient/maxAmountOfConnectionsPerTarget/text()", false);
	}

	private static void smtpClient(Map<String, AliasTarget> aliases) {
		alias(aliases, "host", "smtp-server.xml", "/smtpServer/host/text()", false);
		alias(aliases, "port", "smtp-server.xml", "/smtpServer/port/text()", false);
		alias(aliases, "from", "smtp-server.xml", "/smtpServer/from/text()", false);
		alias(aliases, "subjectTemplate", "smtp-server.xml", "/smtpServer/subjectTemplate/text()", false);
		alias(aliases, "clientHost", "smtp-server.xml", "/smtpServer/clientHost/text()", false);
		alias(aliases, "charset", "smtp-server.xml", "/smtpServer/charset/text()", false);
		alias(aliases, "username", "smtp-server.xml", "/smtpServer/username/text()", false);
		alias(aliases, "password", "smtp-server.xml", "/smtpServer/password/text()", true);
		alias(aliases, "loginMethod", "smtp-server.xml", "/smtpServer/loginMethod/text()", false);
		alias(aliases, "implicitSSL", "smtp-server.xml", "/smtpServer/implicitSSL/text()", false);
		alias(aliases, "startTls", "smtp-server.xml", "/smtpServer/startTls/text()", false);
		alias(aliases, "keystore", "smtp-server.xml", "/smtpServer/keystore/text()", false);
		alias(aliases, "blacklist", "smtp-server.xml", "/smtpServer/blacklist/text()", false);
		alias(aliases, "overrideToInMime", "smtp-server.xml", "/smtpServer/overrideToInMime/text()", false);
		alias(aliases, "connectionTimeout", "smtp-server.xml", "/smtpServer/connectionTimeout/text()", false);
		alias(aliases, "socketTimeout", "smtp-server.xml", "/smtpServer/socketTimeout/text()", false);
	}

	private static void executor(Map<String, AliasTarget> aliases) {
		alias(aliases, "poolSize", "executor.xml", "/executor/poolSize/text()", false);
	}

	private static void featureSet(Map<String, AliasTarget> aliases) {
	}

	private static void webApplication(Map<String, AliasTarget> aliases) {
		alias(aliases, "virtualHost", "webartifact.xml", "/webartifact/virtualHost/text()", false);
		alias(aliases, "path", "webartifact.xml", "/webartifact/path/text()", false);
	}

	private static void waf(Map<String, AliasTarget> aliases) {
		alias(aliases, "baseHost", "waf.xml", "/waf/baseHost/text()", false);
		alias(aliases, "server", "waf.xml", "/waf/server/text()", false);
		alias(aliases, "redirectServer", "waf.xml", "/waf/redirectServer/text()", false);
	}

	private static void jwk(Map<String, AliasTarget> aliases) {
	}

	private static void compressor(Map<String, AliasTarget> aliases) {
		alias(aliases, "enabled", "compressor.xml", "/compressor/enabled/text()", false);
		alias(aliases, "charset", "compressor.xml", "/compressor/charset/text()", false);
	}

	private static void icapVirusScanner(Map<String, AliasTarget> aliases) {
		alias(aliases, "host", "virus-scanner.xml", "/virusScanner/host/text()", false);
		alias(aliases, "path", "virus-scanner.xml", "/virusScanner/path/text()", false);
		alias(aliases, "secure", "virus-scanner.xml", "/virusScanner/secure/text()", false);
		alias(aliases, "keystore", "virus-scanner.xml", "/virusScanner/keystore/text()", false);
	}

	private static void wiki(Map<String, AliasTarget> aliases) {
		alias(aliases, "source", "wiki.xml", "/wiki/source/text()", false);
		alias(aliases, "charset", "wiki.xml", "/wiki/charset/text()", false);
	}

	private static void oDataClient(Map<String, AliasTarget> aliases) {
		alias(aliases, "endpoint", "odata-client.xml", "/odataClient/endpoint/text()", false);
		alias(aliases, "securityType", "odata-client.xml", "/odataClient/securityType/text()", false);
		alias(aliases, "securityContext", "odata-client.xml", "/odataClient/securityContext/text()", false);
	}

	private static void hazelcastCluster(Map<String, AliasTarget> aliases) {
		alias(aliases, "port", "hazelcast-cluster.xml", "/hazelcastCluster/port/text()", false);
		alias(aliases, "hazelcastPort", "hazelcast-cluster.xml", "/hazelcastCluster/hazelcastPort/text()", false);
		alias(aliases, "amazonTagKey", "hazelcast-cluster.xml", "/hazelcastCluster/amazonTagKey/text()", false);
		alias(aliases, "amazonTagValue", "hazelcast-cluster.xml", "/hazelcastCluster/amazonTagValue/text()", false);
		alias(aliases, "amazonRegion", "hazelcast-cluster.xml", "/hazelcastCluster/amazonRegion/text()", false);
	}

	private static void channel(Map<String, AliasTarget> aliases) {
	}

	private static void wsdlClient(Map<String, AliasTarget> aliases) {
		alias(aliases, "endpoint", "wsdl-client.xml", "/wsdlClient/endpoint/text()", false);
		alias(aliases, "charset", "wsdl-client.xml", "/wsdlClient/charset/text()", false);
		alias(aliases, "httpClient", "wsdl-client.xml", "/wsdlClient/httpClient/text()", false);
		alias(aliases, "username", "wsdl-client.xml", "/wsdlClient/username/text()", false);
		alias(aliases, "password", "wsdl-client.xml", "/wsdlClient/password/text()", false);
	}

	private static void alias(Map<String, AliasTarget> aliases, String alias, String fileName, String query, boolean encrypted) {
		aliases.put(alias, new AliasTarget(fileName, query, encrypted));
	}
}
