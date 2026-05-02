# Disabled: GameKryoServer + Kryo DTOs (Backend)

Server-side counterpart of `Game/core/disabled-kryo/`. See that README for full context.

## Files here

- `com/nextgenlab/backend/kryo/GameKryoServer.java` — `@Component` that bound TCP:54555 / UDP:54777
- `com/nextgenlab/backend/kryo/PositionUpdate.java` — server-side mirror DTO
- `com/nextgenlab/backend/kryo/ProjectileSpawn.java` — server-side mirror DTO

## To re-enable

See `Game/core/disabled-kryo/README.md` — same procedure (move files back, restore deps).
