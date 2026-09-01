# Release build

Create `keystore.properties` in the project root using `keystore.properties.template` as a base.

Example:

```properties
storeFile=/absolute/path/to/three-way-hen-release.keystore
storePassword=********
keyAlias=threewayhen
keyPassword=********
```

Then run:

```bash
./gradlew assembleRelease
```
