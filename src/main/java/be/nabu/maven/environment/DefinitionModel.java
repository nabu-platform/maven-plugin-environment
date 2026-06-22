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

public class DefinitionModel {

	private final Map<String, DefinitionField> fields = new LinkedHashMap<String, DefinitionField>();

	public Map<String, DefinitionField> getFields() {
		return fields;
	}

	public DefinitionField getField(String path) {
		return fields.get(path);
	}

	public void addField(DefinitionField field) {
		fields.put(field.getPath(), field);
	}
}
