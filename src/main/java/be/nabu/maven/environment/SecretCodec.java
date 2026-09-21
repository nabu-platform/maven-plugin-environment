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
import java.security.AlgorithmParameters;
import java.security.SecureRandom;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.KeySpec;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;

public class SecretCodec {
	private static final String aesEncryptionAlgorithm = "PBEWithHmacSHA256AndAES_256";
	private static final String legacyEncryptionAlgorithm = "PBEWithMD5AndDES";
	private static final String encryptedPrefix = "${encrypted:";
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
		byte[] aesSalt = new byte[8];
		new SecureRandom().nextBytes(aesSalt);
		Cipher cipher = getCipher(aesEncryptionAlgorithm, Cipher.ENCRYPT_MODE, new PBEParameterSpec(aesSalt, 1024));
		byte[] encrypted = cipher.doFinal(value.getBytes(StandardCharsets.UTF_8));
		byte[] parameters = cipher.getParameters().getEncoded();
		return encryptedPrefix + Base64.getEncoder().encodeToString(parameters) + "$" + Base64.getEncoder().encodeToString(encrypted) + "}";
	}

	public String decrypt(String value) throws Exception {
		if (value == null) {
			return null;
		}
		if (!value.startsWith(encryptedPrefix) || !value.endsWith("}")) {
			return value;
		}
		String encrypted = value.substring(encryptedPrefix.length(), value.length() - 1);
		int separator = encrypted.indexOf('$');
		byte[] decrypted;
		if (separator >= 0) {
			AlgorithmParameters parameters = AlgorithmParameters.getInstance(aesEncryptionAlgorithm);
			parameters.init(Base64.getDecoder().decode(encrypted.substring(0, separator)));
			decrypted = getCipher(aesEncryptionAlgorithm, Cipher.DECRYPT_MODE, parameters).doFinal(Base64.getDecoder().decode(encrypted.substring(separator + 1)));
		}
		else {
			decrypted = getCipher(legacyEncryptionAlgorithm, Cipher.DECRYPT_MODE, parameterSpec).doFinal(Base64.getDecoder().decode(encrypted));
		}
		return new String(decrypted, StandardCharsets.UTF_8);
	}

	private Cipher getCipher(String algorithm, int mode, AlgorithmParameterSpec parameters) throws Exception {
		SecretKeyFactory factory = SecretKeyFactory.getInstance(algorithm);
		KeySpec keySpec = new PBEKeySpec(secret.toCharArray());
		SecretKey key = factory.generateSecret(keySpec);
		Cipher cipher = Cipher.getInstance(algorithm);
		cipher.init(mode, key, parameters);
		return cipher;
	}

	private Cipher getCipher(String algorithm, int mode, AlgorithmParameters parameters) throws Exception {
		SecretKeyFactory factory = SecretKeyFactory.getInstance(algorithm);
		KeySpec keySpec = new PBEKeySpec(secret.toCharArray());
		SecretKey key = factory.generateSecret(keySpec);
		Cipher cipher = Cipher.getInstance(algorithm);
		cipher.init(mode, key, parameters);
		return cipher;
	}
}
