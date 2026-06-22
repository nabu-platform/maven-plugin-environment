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

import java.nio.charset.StandardCharsets;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.KeySpec;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;

public class SecretCodec {
	private static final String encryptionAlgorithm = "PBEWithMD5AndDES";
	private static final byte[] salt = {
		(byte) 0xc7,
		(byte) 0x73,
		(byte) 0x21,
		(byte) 0x8c,
		(byte) 0x7e,
		(byte) 0xc8,
		(byte) 0xee,
		(byte) 0x99
	};
	private static final AlgorithmParameterSpec parameterSpec = new PBEParameterSpec(salt, 1024);

	private final String secret;

	public SecretCodec(String secret) {
		this.secret = secret;
	}

	public String encrypt(String value) throws Exception {
		if (value == null) {
			return null;
		}
		byte[] encrypted = getCipher(Cipher.ENCRYPT_MODE).doFinal(value.getBytes(StandardCharsets.UTF_8));
		return "${encrypted:" + Base64.getEncoder().encodeToString(encrypted) + "}";
	}

	public String decrypt(String value) throws Exception {
		if (value == null) {
			return null;
		}
		if (!value.startsWith("${encrypted:") || !value.endsWith("}")) {
			return value;
		}
		byte[] decoded = Base64.getDecoder().decode(value.substring(12, value.length() - 1).getBytes(StandardCharsets.US_ASCII));
		byte[] decrypted = getCipher(Cipher.DECRYPT_MODE).doFinal(decoded);
		return new String(decrypted, StandardCharsets.UTF_8);
	}

	private Cipher getCipher(int mode) throws Exception {
		SecretKeyFactory factory = SecretKeyFactory.getInstance(encryptionAlgorithm);
		KeySpec keySpec = new PBEKeySpec(secret.toCharArray());
		SecretKey key = factory.generateSecret(keySpec);
		Cipher cipher = Cipher.getInstance(encryptionAlgorithm);
		cipher.init(mode, key, parameterSpec);
		return cipher;
	}
}
