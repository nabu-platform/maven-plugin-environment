/*
* Copyright (C) 2026 Alexander Verbruggen
*
* This program is free software: you can redistribute it and/or modify
* it under the terms of the GNU Lesser General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
*/

package be.nabu.maven.environment;

public class ResolvedEnvironmentValue {
	private final String key;
	private final String value;
	private final String source;

	public ResolvedEnvironmentValue(String key, String value, String source) {
		this.key = key;
		this.value = value;
		this.source = source;
	}

	public String getKey() {
		return key;
	}

	public String getValue() {
		return value;
	}

	public String getSource() {
		return source;
	}
}
