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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

public final class EnvironmentConfigParser {
	private EnvironmentConfigParser() {}

	public static Map<String, String> parse(File file) throws Exception {
		Map<String, String> values = new LinkedHashMap<String, String>();
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
			String currentKey = null;
			StringBuilder currentValue = null;
			String line;
			while ((line = reader.readLine()) != null) {
				String trimmed = line.trim();
				if (currentKey == null) {
					if (trimmed.isEmpty() || trimmed.startsWith("#") || trimmed.startsWith("!")) {
						continue;
					}
					int separator = line.indexOf('=');
					if (separator < 0) {
						continue;
					}
					currentKey = line.substring(0, separator).trim();
					currentValue = new StringBuilder(line.substring(separator + 1));
					if (endsWithContinuation(currentValue)) {
						stripContinuation(currentValue);
					}
					else {
						values.put(currentKey, currentValue.toString());
						currentKey = null;
						currentValue = null;
					}
				}
				else {
					currentValue.append('\n').append(line);
					if (endsWithContinuation(currentValue)) {
						stripContinuation(currentValue);
					}
					else {
						values.put(currentKey, currentValue.toString());
						currentKey = null;
						currentValue = null;
					}
				}
			}
			if (currentKey != null) {
				values.put(currentKey, currentValue == null ? "" : currentValue.toString());
			}
		}
		return values;
	}

	private static boolean endsWithContinuation(StringBuilder builder) {
		int length = builder.length();
		if (length == 0 || builder.charAt(length - 1) != '\\') {
			return false;
		}
		int slashCount = 0;
		for (int i = length - 1; i >= 0 && builder.charAt(i) == '\\'; i--) {
			slashCount++;
		}
		return slashCount % 2 == 1;
	}

	private static void stripContinuation(StringBuilder builder) {
		builder.deleteCharAt(builder.length() - 1);
	}
}
