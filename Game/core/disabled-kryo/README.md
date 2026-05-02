# Disabled: KryoNetTransport (Game/core)

This folder contains the KryoNet client transport implementation that was disabled at Sprint S7 (2026-05-02) due to Kryo serializer compatibility issues between Java 8 (Game client `sourceCompatibility`) and Java 17 (Backend toolchain).

## Symptom encountered

```
com.esotericsoftware.kryo.KryoException: Buffer underflow.
  at com.esotericsoftware.kryo.serializers.UnsafeCacheFields$UnsafeIntField.read
[KRYO] Connection failed: Connected, but timed out during TCP registration.
```

`-Dkryo.unsafe=false` JVM property was attempted but did not take effect (Kryo's `Util.unsafe` flag is set in a static initializer that ran before the property was applied).

## To re-enable

1. Move `KryoNetTransport.java` back to `core/src/main/java/com/nextgenlab/game/network/`.
2. Move backend counterparts (`GameKryoServer.java`, `kryo/PositionUpdate.java`, `kryo/ProjectileSpawn.java`) from `Backend/disabled-kryo/` back to `Backend/src/main/java/com/nextgenlab/backend/`.
3. Add Kryo deps back:
   - `Game/core/build.gradle`: `implementation "com.esotericsoftware:kryonet:2.22.0-RC1"`
   - `Backend/build.gradle`: `implementation "com.esotericsoftware:kryonet:2.22.0-RC1"` + JVM args `--add-opens=java.base/java.nio=ALL-UNNAMED --add-opens=java.base/sun.nio.ch=ALL-UNNAMED -Dkryo.unsafe=false`
4. Restore the KRYO/WS toggle UI in `LobbyScreen.buildMainUI()`.
5. Restore the branching in `LobbyScreen.startMultiplayer()`.

## Possible fixes for the underlying Kryo issue (when reactivating)

- Programmatically `System.setProperty("kryo.unsafe", "false")` before any KryoNet code is loaded — must run in a `static {}` block of the very first class touched.
- Alternative: configure Kryo to use plain `FieldSerializer` with `setUseAsm(true)` — but framework messages (RegisterTCP) are pre-registered by KryoNet, so this only helps user classes.
- Another alternative: align Game `sourceCompatibility = 17` (matches Backend) to remove the JVM mismatch. Requires updating `Lwjgl3Launcher` and verifying LibGDX runs fine on Java 17.
- Or: switch to a different Kryo version (older 4.x has different binary format).
