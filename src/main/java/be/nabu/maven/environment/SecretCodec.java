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

import be.nabu.utils.security.EncryptionXmlAdapter;

public class SecretCodec {
	private final String secret;

	public SecretCodec(String secret) {
		this.secret = secret;
	}

	public String encrypt(String value) throws Exception {
		if (value == null) {
			return null;
		}
		String original = System.getProperty(EncryptionXmlAdapter.CONFIGURATION_CRYPT_KEY);
		try {
			System.setProperty(EncryptionXmlAdapter.CONFIGURATION_CRYPT_KEY, secret);
			return new EncryptionXmlAdapter().marshal(value);
		}
		finally {
			restore(original);
		}
	}

	public String decrypt(String value) throws Exception {
		if (value == null) {
			return null;
		}
		String original = System.getProperty(EncryptionXmlAdapter.CONFIGURATION_CRYPT_KEY);
		try {
			System.setProperty(EncryptionXmlAdapter.CONFIGURATION_CRYPT_KEY, secret);
			return new EncryptionXmlAdapter().unmarshal(value);
		}
		finally {
			restore(original);
		}
	}

	private void restore(String original) {
		if (original == null) {
			System.clearProperty(EncryptionXmlAdapter.CONFIGURATION_CRYPT_KEY);
		}
		else {
			System.setProperty(EncryptionXmlAdapter.CONFIGURATION_CRYPT_KEY, original);
		}
	}
}
