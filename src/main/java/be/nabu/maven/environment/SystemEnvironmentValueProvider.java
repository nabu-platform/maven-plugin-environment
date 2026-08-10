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
import org.apache.maven.plugin.logging.Log;

public class SystemEnvironmentValueProvider implements ConfigurableEnvironmentValueProvider {
	private String variablePrefix;
	private Log log;

	@Override
	public void configure(Map<String, String> configuration, Log log) {
		this.variablePrefix = configuration.get("variablePrefix");
		this.log = log;
	}

	@Override
	public Map<String, String> loadValues(String environmentName) {
		Map<String, String> values = new LinkedHashMap<String, String>();
		for (Map.Entry<String, String> entry : System.getenv().entrySet()) {
			String key = entry.getKey();
			if (variablePrefix != null && !variablePrefix.isEmpty()) {
				if (!key.startsWith(variablePrefix)) {
					continue;
				}
				key = key.substring(variablePrefix.length());
			}
			key = normalizeKey(key);
			if (key == null || key.isEmpty()) {
				continue;
			}
			values.put(key, entry.getValue());
		}
		if (log != null) {
			log.info("Loaded " + values.size() + " environment values from system environment");
		}
		return values;
	}

	private String normalizeKey(String key) {
		String normalized = key.trim();
		normalized = normalized.replace("__", ":");
		normalized = normalized.replace('_', '.');
		return normalized;
	}
}
