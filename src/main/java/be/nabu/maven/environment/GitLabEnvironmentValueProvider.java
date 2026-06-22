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

import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.classic.methods.HttpPut;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.HttpHeaders;
import org.apache.hc.core5.http.HttpStatus;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.net.URIBuilder;
import org.apache.maven.plugin.logging.Log;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

public class GitLabEnvironmentValueProvider implements EnvironmentValueWriter, ConfigurableEnvironmentValueProvider {

	private String baseUrl;
	private String projectId;
	private String token;
	private String variablePrefix;
	private Log log;
	private final ObjectMapper objectMapper = new ObjectMapper();

	@Override
	public void configure(Map<String, String> configuration, Log log) {
		this.baseUrl = required(configuration, "baseUrl");
		this.projectId = required(configuration, "projectId");
		this.token = required(configuration, "token");
		this.variablePrefix = configuration.get("variablePrefix");
		this.log = log;
	}

	@Override
	public Map<String, String> loadValues(String environmentName) {
		try (CloseableHttpClient client = newClient()) {
			HttpGet request = new HttpGet(variablesUrl());
			request.setHeader("PRIVATE-TOKEN", token);
			request.setHeader(HttpHeaders.ACCEPT, ContentType.APPLICATION_JSON.getMimeType());
			try (CloseableHttpResponse response = client.execute(request)) {
				String body = readEntity(response.getEntity());
				if (response.getCode() < 200 || response.getCode() >= 300) {
					throw new IllegalStateException("GitLab variable lookup failed with status " + response.getCode() + ": " + body);
				}
				Map<String, String> values = new LinkedHashMap<String, String>();
				List<Map<String, Object>> variables = objectMapper.readValue(body, new TypeReference<List<Map<String, Object>>>() { });
				for (Map<String, Object> variable : variables) {
					String scope = stringValue(variable.get("environment_scope"));
					if (!environmentMatches(environmentName, scope)) {
						continue;
					}
					String key = stringValue(variable.get("key"));
					if (key == null) {
						continue;
					}
					values.put(stripPrefix(key), stringValue(variable.get("value")));
				}
				return values;
			}
		}
		catch (Exception e) {
			throw new IllegalStateException("Failed to load GitLab environment values", e);
		}
	}

	@Override
	public void storeValues(String environmentName, Map<String, String> values) {
		for (Map.Entry<String, String> entry : values.entrySet()) {
			try {
				upsertVariable(environmentName, entry.getKey(), entry.getValue());
			}
			catch (Exception e) {
				throw new IllegalStateException("Failed to store GitLab variable: " + entry.getKey(), e);
			}
		}
	}

	private void upsertVariable(String environmentName, String key, String value) throws Exception {
		String scopedKey = applyPrefix(key);
		Map<String, Object> payload = new LinkedHashMap<String, Object>();
		payload.put("key", scopedKey);
		payload.put("value", value);
		payload.put("environment_scope", environmentName);
		payload.put("masked", Boolean.FALSE);
		payload.put("protected", Boolean.FALSE);
		try (CloseableHttpClient client = newClient()) {
			HttpPost create = new HttpPost(variablesUrl());
			create.setHeader("PRIVATE-TOKEN", token);
			create.setHeader(HttpHeaders.ACCEPT, ContentType.APPLICATION_JSON.getMimeType());
			create.setEntity(new StringEntity(objectMapper.writeValueAsString(payload), ContentType.APPLICATION_JSON));
			try (CloseableHttpResponse response = client.execute(create)) {
				String body = readEntity(response.getEntity());
				if (response.getCode() >= 200 && response.getCode() < 300) {
					return;
				}
				if (response.getCode() != HttpStatus.SC_BAD_REQUEST && response.getCode() != HttpStatus.SC_CONFLICT) {
					throw new IllegalStateException("GitLab variable create failed with status " + response.getCode() + ": " + body);
				}
			}
			payload.remove("key");
			HttpPut update = new HttpPut(variablesUrl() + "/" + encodePath(scopedKey));
			update.setHeader("PRIVATE-TOKEN", token);
			update.setHeader(HttpHeaders.ACCEPT, ContentType.APPLICATION_JSON.getMimeType());
			update.setEntity(new StringEntity(objectMapper.writeValueAsString(payload), ContentType.APPLICATION_JSON));
			try (CloseableHttpResponse response = client.execute(update)) {
				String body = readEntity(response.getEntity());
				if (response.getCode() < 200 || response.getCode() >= 300) {
					throw new IllegalStateException("GitLab variable update failed with status " + response.getCode() + ": " + body);
				}
			}
		}
	}

	private CloseableHttpClient newClient() {
		RequestConfig requestConfig = RequestConfig.custom().build();
		return HttpClients.custom().setDefaultRequestConfig(requestConfig).build();
	}

	private String variablesUrl() throws Exception {
		return trimTrailingSlash(baseUrl) + "/api/v4/projects/" + encodePath(projectId) + "/variables";
	}

	private boolean environmentMatches(String environmentName, String scope) {
		return scope == null || scope.isEmpty() || "*".equals(scope) || environmentName.equals(scope);
	}

	private String applyPrefix(String key) {
		return variablePrefix == null || variablePrefix.isEmpty() ? key : variablePrefix + key;
	}

	private String stripPrefix(String key) {
		if (variablePrefix != null && !variablePrefix.isEmpty() && key.startsWith(variablePrefix)) {
			return key.substring(variablePrefix.length());
		}
		return key;
	}

	private String required(Map<String, String> configuration, String key) {
		String value = configuration.get(key);
		if (value == null || value.trim().isEmpty()) {
			throw new IllegalArgumentException("Missing provider configuration: " + key);
		}
		return value;
	}

	private String readEntity(HttpEntity entity) throws Exception {
		return entity == null ? "" : EntityUtils.toString(entity);
	}

	private String encodePath(String value) throws Exception {
		return new URIBuilder().setPathSegments(value).build().getRawPath().replace("/", "%2F");
	}

	private String stringValue(Object value) {
		return value == null ? null : value.toString();
	}

	private String trimTrailingSlash(String value) {
		return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
	}
}
