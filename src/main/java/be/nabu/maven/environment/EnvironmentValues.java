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
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

public final class EnvironmentValues {
	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
	private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\$\\{([^}]+)\\}");

	private EnvironmentValues() {}

	public static String scalar(EnvironmentBuildContext context, String key) {
		String qualifiedKey = qualifiedKey(context, key);
		String providerValue = qualifiedKey == null ? null : context.getProviderValues().get(qualifiedKey);
		if (providerValue == null) {
			providerValue = context.getProviderValues().get(key);
		}
		if (providerValue != null) {
			return providerValue;
		}
		String fixedValue = qualifiedKey == null ? null : context.getFixedValues().get(qualifiedKey);
		if (fixedValue == null) {
			fixedValue = context.getFixedValues().get(key);
		}
		return fixedValue == null ? null : resolvePlaceholders(context, fixedValue);
	}

	public static List<String> list(EnvironmentBuildContext context, String key, boolean allowCommaSeparatedFallback) throws ArtifactHandlerException {
		String value = scalar(context, key);
		if (value != null) {
			String trimmed = value.trim();
			if (trimmed.startsWith("[")) {
				try {
					return OBJECT_MAPPER.readValue(trimmed, new TypeReference<List<String>>() {});
				}
				catch (Exception e) {
					throw new ArtifactHandlerException("Invalid JSON array for key: " + key, e);
				}
			}
		}
		Map<Integer, String> indexed = indexedValues(context, key);
		if (!indexed.isEmpty()) {
			List<Map.Entry<Integer, String>> entries = new ArrayList<Map.Entry<Integer, String>>(indexed.entrySet());
			Collections.sort(
				entries,
				new Comparator<Map.Entry<Integer, String>>() {
					@Override
					public int compare(Map.Entry<Integer, String> left, Map.Entry<Integer, String> right) {
						return left.getKey().compareTo(right.getKey());
					}
				}
			);
			List<String> result = new ArrayList<String>();
			for (Map.Entry<Integer, String> entry : entries) {
				result.add(entry.getValue());
			}
			return result;
		}
		if (value != null && allowCommaSeparatedFallback) {
			return splitCommaSeparated(value.trim());
		}
		return null;
	}

	public static Map<Integer, String> indexedValues(EnvironmentBuildContext context, String key) {
		String qualifiedKey = qualifiedKey(context, key);
		Map<Integer, String> result = qualifiedKey == null ? new LinkedHashMap<Integer, String>() : indexedValues(context.getProviderValues(), qualifiedKey);
		if (result.isEmpty()) {
			result = indexedValues(context.getProviderValues(), key);
		}
		if (!result.isEmpty()) {
			return result;
		}
		Map<Integer, String> fixedValues = qualifiedKey == null ? new LinkedHashMap<Integer, String>() : indexedValues(context.getFixedValues(), qualifiedKey);
		if (fixedValues.isEmpty()) {
			fixedValues = indexedValues(context.getFixedValues(), key);
		}
		if (fixedValues.isEmpty()) {
			return fixedValues;
		}
		Map<Integer, String> resolved = new LinkedHashMap<Integer, String>();
		for (Map.Entry<Integer, String> entry : fixedValues.entrySet()) {
			resolved.put(entry.getKey(), resolvePlaceholders(context, entry.getValue()));
		}
		return resolved;
	}

	private static Map<Integer, String> indexedValues(Map<String, String> values, String key) {
		Map<Integer, String> result = new LinkedHashMap<Integer, String>();
		String prefix = key + "[";
		for (Map.Entry<String, String> entry : values.entrySet()) {
			if (!entry.getKey().startsWith(prefix) || !entry.getKey().endsWith("]")) {
				continue;
			}
			String indexString = entry.getKey().substring(prefix.length(), entry.getKey().length() - 1);
			try {
				result.put(Integer.parseInt(indexString), entry.getValue());
			}
			catch (NumberFormatException e) {
				// ignore invalid legacy index names
			}
		}
		return result;
	}

	private static String qualifiedKey(EnvironmentBuildContext context, String key) {
		if (!(context instanceof ArtifactScopedEnvironmentBuildContext)) {
			return null;
		}
		String artifactId = ((ArtifactScopedEnvironmentBuildContext) context).getArtifactId();
		if (artifactId == null || artifactId.trim().isEmpty()) {
			return null;
		}
		return artifactId + ":" + key;
	}

	private static String resolvePlaceholders(EnvironmentBuildContext context, String value) {
		Matcher matcher = PLACEHOLDER_PATTERN.matcher(value);
		StringBuffer buffer = new StringBuffer();
		while (matcher.find()) {
			String replacement = context.getProviderValues().get(matcher.group(1));
			matcher.appendReplacement(buffer, Matcher.quoteReplacement(replacement == null ? matcher.group(0) : replacement));
		}
		matcher.appendTail(buffer);
		return buffer.toString();
	}

	private static List<String> splitCommaSeparated(String value) {
		if (value == null || value.trim().isEmpty()) {
			return new ArrayList<String>();
		}
		String[] split = value.split("[\\s]*,[\\s]*");
		List<String> result = new ArrayList<String>();
		for (String single : split) {
			if (single != null && !single.isEmpty()) {
				result.add(single);
			}
		}
		return result;
	}
}
