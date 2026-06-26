# maven-plugin-environment

Core Maven plugin for environment-aware artifact rewriting.

## Current shape

- `EnvironmentBuildMojo`: entry point for environment builds
- `EnvironmentValueProvider`: read-only provider interface for environment values
- `EnvironmentValueWriter`: write-capable extension that also extends the provider interface
- `ArtifactHandler`: artifact-aware rewrite contract
- `AbstractXmlArtifactHandler`: shared XML handling base class
- `SecretCodec`: mirrors current encrypted parameter behavior using `EncryptionXmlAdapter`

## Design intent

The plugin core is intentionally provider-agnostic.
GitLab support is implemented as a provider class, not as core plugin configuration.

## GitLab provider configuration

Use the GitLab provider by configuring the provider class and provider configuration in your project `pom.xml`.
Do not hardcode the token in the `pom.xml`.

Recommended model:

- plain `mvn package` must not do environment replacement
- environment replacement should only run when an environment-specific profile is activated
- the plugin goal should be bound inside the profile, not in the default build

Primary recommendation: one reusable profile with a dynamic environment toggle.

```xml
<profiles>
	<profile>
		<id>environment-build</id>
		<build>
			<plugins>
				<plugin>
					<groupId>nabu</groupId>
					<artifactId>maven-plugin-environment</artifactId>
					<version>1.0-SNAPSHOT</version>
					<executions>
						<execution>
							<id>build-selected-environment</id>
							<phase>package</phase>
							<goals>
								<goal>build-environment</goal>
							</goals>
						</execution>
					</executions>
					<configuration>
						<environmentName>${environment.name}</environmentName>
						<providerClass>be.nabu.maven.environment.GitLabEnvironmentValueProvider</providerClass>
						<providerConfiguration>
							<baseUrl>https://gitlab.example.com</baseUrl>
							<projectId>1234</projectId>
							<token>${env.GITLAB_TOKEN}</token>
						</providerConfiguration>
					</configuration>
				</plugin>
			</plugins>
		</build>
	</profile>
</profiles>
```

Usage:

```bash
mvn package
mvn package -Penvironment-build -Denvironment.name=qlty
mvn package -Penvironment-build -Denvironment.name=prd
```

This is the recommended default because the environment-specific behavior stays centralized and does not need to be duplicated across many nearly identical Maven profiles.

Optional convenience model: one profile per environment.

```xml
<profiles>
	<profile>
		<id>qlty</id>
		<properties>
			<environment.name>qlty</environment.name>
		</properties>
		<build>
			<plugins>
				<plugin>
					<groupId>nabu</groupId>
					<artifactId>maven-plugin-environment</artifactId>
					<version>1.0-SNAPSHOT</version>
					<executions>
						<execution>
							<id>build-qlty-environment</id>
							<phase>package</phase>
							<goals>
								<goal>build-environment</goal>
							</goals>
						</execution>
					</executions>
					<configuration>
						<environmentName>${environment.name}</environmentName>
						<providerClass>be.nabu.maven.environment.GitLabEnvironmentValueProvider</providerClass>
						<providerConfiguration>
							<baseUrl>https://gitlab.example.com</baseUrl>
							<projectId>1234</projectId>
							<token>${env.GITLAB_TOKEN}</token>
						</providerConfiguration>
					</configuration>
				</plugin>
			</plugins>
		</build>
	</profile>

	<profile>
		<id>prd</id>
		<properties>
			<environment.name>prd</environment.name>
		</properties>
		<build>
			<plugins>
				<plugin>
					<groupId>nabu</groupId>
					<artifactId>maven-plugin-environment</artifactId>
					<version>1.0-SNAPSHOT</version>
					<executions>
						<execution>
							<id>build-prd-environment</id>
							<phase>package</phase>
							<goals>
								<goal>build-environment</goal>
							</goals>
						</execution>
					</executions>
					<configuration>
						<environmentName>${environment.name}</environmentName>
						<providerClass>be.nabu.maven.environment.GitLabEnvironmentValueProvider</providerClass>
						<providerConfiguration>
							<baseUrl>https://gitlab.example.com</baseUrl>
							<projectId>1234</projectId>
							<token>${env.GITLAB_TOKEN}</token>
						</providerConfiguration>
					</configuration>
				</plugin>
			</plugins>
		</build>
	</profile>
</profiles>
```

Usage:

```bash
mvn package -Pqlty
mvn package -Pprd
```

The standard handlers should cover the normal artifact fields automatically.
If you want to limit the build to a subset of built-in handlers, you can explicitly list them:

```xml
<handlers>
	<handler>jdbcPool</handler>
	<handler>webApplication</handler>
	<handler>configuration</handler>
</handlers>
```

Web application page JSON minification is available as an explicit option and is disabled by default.
When enabled, it only minifies `.json` files in `public/artifacts/pages`.

```xml
<options>
	<webApplication.minifyPageJson>true</webApplication.minifyPageJson>
</options>
```

Current built-in handlers include the full checked-out merge-script-based artifact set:

- `jdbcPool`
- `webApplication`
- `configuration`
- `httpClient`
- `httpServer`
- `smtpClient`
- `restClient`
- `restEndpoint`
- `swaggerClient`
- `wsdlClient`
- `virtualHost`
- `executor`
- `featureSet`
- `compressor`
- `hazelcastCluster`
- `icapVirusScanner`
- `jwk`
- `channel`
- `waf`
- `odataClient`
- `wiki`

## Variable naming and list policy

Variable keys should use the logical parameter names from the merge scripts and handlers.
When you need to target a specific artifact, use the artifact-qualified form `<artifactId>:<field>`.
Examples:

- `jdbcUrl`
- `driverClassName`
- `password`
- `virtualHost`
- `path`
- `fragment.roles`
- `fragment.endpoint`
- `my.main.pool:jdbcUrl`
- `my.main.pool:password`
- `my.web.application:fragment.roles`

Lookup order is:

1. provider value for the exact key, for example `my.main.pool:jdbcUrl`
2. fixed configuration file value for the exact key
3. provider value for the plain fallback key, for example `jdbcUrl`
4. fixed configuration file value for the plain fallback key

Environment separation should come from the provider, for example GitLab `environment_scope`, not from prefixes.
An optional prefix can still be used for namespacing, but it is not the primary separation mechanism.

Lists are detected only from artifact definitions or from built-in handler knowledge, never from the raw value shape.

## Optional fixed configuration file

You can optionally pass a local properties file with `environment.configurationFile`.
If no file is configured explicitly, the plugin automatically uses `${project.basedir}/.nabu-config` when that file exists.
This file is intended for versioned non-secret defaults and placeholder-based secret references.

Example:

```properties
my.main.pool:jdbcUrl=jdbc:postgresql://qlty-db:5432/app
my.main.pool:username=app_user
my.main.pool:password=${DB_PASSWORD}
DB_PASSWORD=
```

Fixed configuration values are meta-processed for placeholders using provider values.
This allows a fixed file to reference provider-managed secrets without storing the secret value itself.
If a placeholder is not present in the provider values, it is left unchanged.

The plugin checks the configured provider first and only falls back to the fixed configuration file when the provider does not contain the requested key.
The canonical new format for list values is a JSON array in the base key:

```text
roles=["admin","user"]
fragment.roles=["admin","user"]
```

Legacy fallback support is still required when users import older value sets.
Handlers should read older formats in this order where applicable:

1. JSON array in the base key
2. legacy indexed values such as `roles[0]`, `roles[1]`
3. legacy comma-separated values for older known handlers

So for example all of these should still be accepted during migration:

```text
roles=["admin","user"]
roles[0]=admin
roles[1]=user
roles=admin,user
```

The canonical documented and preferred format remains the JSON array.

For local development, prefer resolving the token from `~/.m2/settings.xml` or from an environment variable.
A simple setup is to keep the token in an environment variable and let Maven interpolate it:

```xml
<settings>
	<profiles>
		<profile>
			<id>gitlab-environment</id>
			<properties>
				<gitlab.token>${env.GITLAB_TOKEN}</gitlab.token>
			</properties>
		</profile>
	</profiles>
	<activeProfiles>
		<activeProfile>gitlab-environment</activeProfile>
	</activeProfiles>
</settings>
```

Then use it in the plugin configuration instead of a literal token:

```xml
<token>${gitlab.token}</token>
```

For GitLab CI, store `GITLAB_TOKEN` as a masked and protected CI/CD variable and do not commit it anywhere.

## Current defaults

The plugin currently enables the full checked-out merge-script handler set by default.
That set currently covers 21 artifact types, matching the reviewed merge scripts:

- `jdbcPool`
- `webApplication`
- `configuration`
- `httpClient`
- `httpServer`
- `smtpClient`
- `restClient`
- `restEndpoint`
- `swaggerClient`
- `wsdlClient`
- `virtualHost`
- `executor`
- `featureSet`
- `compressor`
- `hazelcastCluster`
- `icapVirusScanner`
- `jwk`
- `channel`
- `waf`
- `odataClient`
- `wiki`
