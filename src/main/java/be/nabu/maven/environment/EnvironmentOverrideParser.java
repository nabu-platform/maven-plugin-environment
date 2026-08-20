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
import java.util.List;
import java.util.Map;

public final class EnvironmentOverrideParser {
	private EnvironmentOverrideParser() {}

	public static List<EnvironmentOverride> parse(Map<String, String> values, Map<String, AliasTarget> aliases) {
		List<EnvironmentOverride> overrides = new ArrayList<EnvironmentOverride>();
		for (Map.Entry<String, String> entry : values.entrySet()) {
			EnvironmentOverride override = parse(entry.getKey(), entry.getValue(), aliases);
			if (override != null) {
				overrides.add(override);
			}
		}
		return overrides;
	}

	private static EnvironmentOverride parse(String key, String value, Map<String, AliasTarget> aliases) {
		int firstColon = key.indexOf(':');
		if (firstColon <= 0 || firstColon == key.length() - 1) {
			return null;
		}
		String artifactId = key.substring(0, firstColon).trim();
		String remainder = key.substring(firstColon + 1).trim();
		if (artifactId.isEmpty() || remainder.isEmpty()) {
			return null;
		}
		int secondColon = remainder.indexOf(':');
		if (secondColon > 0 && secondColon < remainder.length() - 1) {
			String fileName = remainder.substring(0, secondColon).trim();
			String query = remainder.substring(secondColon + 1).trim();
			if (!fileName.isEmpty() && !query.isEmpty()) {
				return new EnvironmentOverride(artifactId, fileName, query, value);
			}
		}
		AliasTarget aliasTarget = aliases.get(remainder);
		if (aliasTarget == null) {
			return null;
		}
		return new EnvironmentOverride(artifactId, aliasTarget.getFileName(), aliasTarget.getQuery(), value);
	}
}
