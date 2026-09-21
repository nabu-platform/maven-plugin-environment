/*
* Copyright (C) 2026 Alexander Verbruggen
*
* This program is free software: you can redistribute it and/or modify
* it under the terms of the GNU Lesser General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
*/

package be.nabu.maven.environment;

import org.junit.Assert;
import org.junit.Test;

public class SecretCodecTest {

	@Test
	public void aes256RoundTrip() throws Exception {
		SecretCodec codec = new SecretCodec("test-secret");
		String encrypted = codec.encrypt("configuration secret");
		Assert.assertTrue(encrypted.startsWith("${encrypted:"));
		Assert.assertTrue(encrypted.indexOf('$') >= 0);
		Assert.assertEquals("configuration secret", codec.decrypt(encrypted));
	}

	@Test
	public void legacyDesDecrypt() throws Exception {
		SecretCodec codec = new SecretCodec("changeit");
		Assert.assertEquals("${DATABASE_PASSWORD}", codec.decrypt("${encrypted:Z6PaU/h1ICnmR+ZTdFqxlVgkFyr/Tf65}"));
	}

	@Test
	public void plainValueIsUnchanged() throws Exception {
		Assert.assertEquals("plain", new SecretCodec("test-secret").decrypt("plain"));
	}
}
