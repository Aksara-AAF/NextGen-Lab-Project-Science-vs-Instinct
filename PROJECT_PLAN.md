# PROJECT_PLAN.md — NextGenLab: Science vs Instinct

> **Dokumen Perencanaan Komprehensif Proyek Game**
> Seleksi Oprec Netlab 2026 — Divisi Game Dev
> Versi: 1.9 — Tanggal: 2026-05-19
> Status proyek saat penulisan: **EventBus + SettingsScreen + Responsive UI Complete** (sprint S1–S17 selesai)

---
.
## Daftar Isi

1. [Executive Summary](#1-executive-summary)
2. [Penilaian — Checklist Compliance](#2-penilaian--checklist-compliance)
3. [Game Design Reference](#3-game-design-reference)
4. [Arsitektur Sistem (High-level)](#4-arsitektur-sistem-high-level)
5. [Prinsip OOP yang Diterapkan](#5-prinsip-oop-yang-diterapkan)
6. [Spesifikasi Gameplay Detail](#6-spesifikasi-gameplay-detail)
7. [Design Pattern Implementation (9 pattern)](#7-design-pattern-implementation-9-pattern)
8. [Backend Spring Boot](#8-backend-spring-boot)
9. [Database Schema (PostgreSQL)](#9-database-schema-postgresql)
10. [Frontend / Game Architecture (LibGDX)](#10-frontend--game-architecture-libgdx)
11. [Audio Plan](#11-audio-plan)
12. [Sprint Roadmap](#12-sprint-roadmap)
13. [Verification per Sprint](#13-verification-per-sprint)
14. [Risiko & Mitigasi](#14-risiko--mitigasi)
15. [Deliverables Final](#15-deliverables-final)

---

## 1. Executive Summary

### 1.1 Pitch (≤75 kata)

**NextGenLab: Science vs Instinct** adalah game 2D top-down asimetris bertema sci-fi horror. Pemain memilih peran sebagai **Peneliti** (menyelesaikan task lab + crafting senjata) atau **Monster** (memburu penjaga + sabotase + evolusi genetik) dalam balapan progres berbasis waktu yang berakhir di duel arena. Dibangun dengan LibGDX (frontend), Spring Boot + PostgreSQL (backend), dengan multiplayer real-time via WebSocket.

### 1.2 Status Saat Ini (per 2026-05-19)

Proyek sudah masuk ke fase **Friend System & Room Redesign Complete** — semua sistem utama Preparation + Duel berjalan lengkap dengan friend system, in-room role selection, visibility, kick, dan public room auto-refresh:
- Movement 8 arah + animasi idle/jalan/serang (Researcher & Monster) — format aset individual PNG per frame (8 arah × N frame)
- Tile-based collision dengan wall sliding; `TILE_SIZE = 32px`
- **5 lab task** (Preparation phase): WireTask (circuit grid 3×3), ReactorTask (dual slider drift), ChemicalMixTask (swing cursor), ServerHackTask (hex matrix 8×6), BiometricLockTask (3 fingerprint layer)
- State machine `PREPARATION → DUEL` (sinkron antar client di multiplayer); HP **carry-over** antar fase (tidak di-reset)
- Object Pool projectile + hit detection; **HP death-check** di Preparation (sudden-death)
- **Combat system:** Monster — LMB = melee (animasi `Basic_Melee_Attack`), RMB = ranged; Researcher — RMB = ranged + 5 senjata crafting
- **FLAG_HIT multiplayer sync:** killing blow langsung di-flush via `sendPosition()` tanpa tunggu `netSendTimer`
- HUD overlay — health bar + task progress bar + weapon slot bar (icon + label LMB/F) di Preparation; health bar di Duel; GameOverScreen (ENTER → Lobby, ESC → Menu)
- Lobby dengan kode room 6 karakter
- Single-player offline (vs AI) + Multiplayer real-time **via WebSocket** (KryoNet di-shelf di `disabled-kryo/`)
- Visual sync penuh: 8 arah, animasi, projectile spawn, taskProgress, equippedItemOrdinal, guardAliveMask lintas client
- **Auth & User module** — register/login/logout via JWT (HMAC-SHA384) + BCrypt; AuthScreen Scene2D; multiplayer endpoint terkunci di `authenticated()`, solo offline tetap jalan tanpa login
- **NPC Guard AI** — 4 guard spawn di map; Strategy pattern: `GuardPatrolStrategy` ↔ `AlertChaseStrategy`; LOS wall-aware (DDA raycast, last-seen position); guard bullets dihancurkan saat tembus tembok; guard projectile sync ke Researcher client via `weaponType = "GUARD"`
- **Stamina system** — Researcher Shift = sprint; drain/regen/cooldown; `StaminaBar` di HUD
- **Resource & Crafting** — 5 resource (Scrap/Battery/Chemical/Circuit/Bioplasma, weighted random); 12 chest dari `tileset_lab.png` (chest_closed/chest_open tiles); loot sync ke Monster client via `RES_CHEST` event; Workshop zone; `InventoryPanel` (I) + `CraftingPanel` (E near workshop); 8 recipe
- **Item system** — 5 senjata (Pistol/StunGun/AcidGrenade/Taser/RailGun) + 3 utility (HealKit/Trap/Decoy); semua berfungsi; Taser/Trap/Decoy sync ke Monster client via ProjectileSpawn event; icon dari Pixellab MCP (32×32px)
- **equippedItemOrdinal** sync: Researcher → Monster client untuk HUD indicator senjata lawan
- **Evolution Gene system (S12):** 10 gene aktif (PREDATOR_CLAWS, ADRENAL_SURGE, THICK_HIDE, FRENZY, ECHOLOCATION, ACIDIC_BLOOD, REGENERATION, TOXIC_AURA, PHASE_SHIFT, BERSERKER); XP dari guard kill; LevelUpScreen card-style dengan PixelLab icon; Monster-pause on level-up
- **Dash system (S12):** Shift = dash 96px/0.16s, 4s cooldown; FRENZY gene kurangi 1s; cooldown bar + icon di HUD Monster
- **Sabotage panels (S12):** 3 jenis efek (LIGHTS_OUT 10s, SLOW_FIELD 8s, SERUM_DRAIN −1 task); sync via GameEvent WebSocket; panel cooldown tersendiri
- **Gene ability keys:** Q = Echolocation (highlight + off-screen arrow); G = Phase Shift (tembus tembok 0.4s)
- **Gene panel HUD:** icon 10 gene dengan cooldown pie-arc indicator di sisi kanan layar Monster
- **XP bar di kedua sisi:** Monster level progress bar terlihat oleh Peneliti via PositionUpdate sync (monsterXp/monsterLevel); desain mirip task progress bar (200×18 px, top-right)
- Backend Spring Boot dengan REST API untuk room, match, dan auth
- **Ammo reserve system** — per-weapon magazine capacity (Pistol 12, StunGun 8, Taser 5, RailGun 4, AcidGrenade 3); stok awal = 2× kapasitas (1 magazine + 1 reserve); reload menguras reserve; tanpa senjata = tidak ada reload; craft AMMO_PACK (+12 reserve); HUD menampilkan `AMMO X/Y +N`
- **Utility stock system** — stok multiple per jenis (`Map<ItemType,Integer>`); `consumeEquippedUtility()`; `InventoryPanel` interaktif (klik pilih utility aktif, highlight CYAN)
- **Duel phase combat complete** — Researcher tembak ke arah mouse cursor; Monster ranged cooldown 1.5s; gene timers + Dash/Echolocation/PhaseShift aktif di Duel; F key utility (HealKit/Trap/Decoy) di Duel; trap+decoy render+collision di arena
- **Duel Arena** — `arena.tmx`, switch map saat transisi PREPARATION→DUEL; prep-winner buff (+3 HP Researcher atau +2 HP Monster); match timer 3 menit
- **Player Stats & ELO** — `PlayerStats` entity (ELO default 1000, W/L, per-role count, win rate); ELO K-factor 32 dihitung server-side; pessimistic DB lock + unique constraint mencegah double-count saat kedua client panggil `/finish` bersamaan
- **Match History** — `MatchHistory` entity; durasi match penuh (preparation + duel); profil menampilkan 10 match terakhir dengan tanggal+jam, role, hasil, ELO sebelum dan perubahan
- **ProfileScreen** — tampil dari MenuScreen `[P]` dan LobbyScreen tombol "PROFIL SAYA"; ESC kembali ke screen sebelumnya (callback pattern)
- **Leaderboard** — `GET /api/leaderboard` public; top-10 global ELO, top-10 Researcher wins, top-10 Monster wins; `LeaderboardScreen` async (MenuScreen `[L]`, LobbyScreen tombol)
- **Achievement system** — 10 achievement auto-unlock di server saat `finishMatch`; `AchievementScreen` dengan 10 pixel art icon (64×64, PixelLab AI) + status gold/gray; notifikasi popup di `GameOverScreen` saat unlock
- **Friend System** — `Friendship` entity + `FriendService` + `FriendController`; endpoint `/api/friends` (request/accept/list/delete/search); `FriendScreen.java` dengan tiga tab (TEMAN, PERMINTAAN, CARI); join room teman langsung dari tab TEMAN via `roomCode` di `UserSummaryDTO`
- **Room redesign — in-room role selection** — player join/create tanpa peran; klaim PENELITI atau MONSTER dari dalam `RoomScreen`; host lihat [MULAI] hanya jika kedua peran diklaim & keduanya SIAP; `player2UserId` tracking di `GameRoom`
- **Visibilitas room** — host dapat toggle PUBLIK/PRIVAT dari dalam room; room privat tidak muncul di public list; native SQL query `findPublicWaitingRooms` menghindari ambiguitas JPA naming `isXxx`
- **Kick player** — host dapat keluarkan player; kick detection di `RoomScreen` via flag `wasInRoom` + perbandingan username → redirect ke lobby dengan pesan
- **Public room list auto-refresh** — polling 3 detik dengan `fetching` guard; tidak lagi menampilkan data stale
- 9 design pattern sudah terimplementasi (Observer via EventBus selesai di S17)

### 1.3 Target Output Akhir

Hingga deadline, proyek akan mencapai **full vision** dengan:
- 9 design pattern lengkap (tambah Observer)
- Sistem autentikasi (login/register dengan JWT)
- 5 jenis lab task + 5 senjata + 3 utility hasil crafting
- 10 Evolution Gene + 3 sabotage type
- NPC Guards dengan AI Strategy (Patrol/AlertChase)
- Match history, player stats, achievement, leaderboard, friend system
- Audio lengkap (BGM + SFX dari aset CC0)
- Settings menu (volume, server, fullscreen, logout)
- Server address selector (localhost / LAN / Internet)
- Web build via TeaVM untuk itch.io browser-playable
- Dokumentasi lengkap + demo video

### 1.4 Kriteria Penilaian (Wajib + Bonus)

**Wajib:**
- Game bertemakan, berjalan tanpa error, pakai backend custom
- Minimal 6 design pattern (selain Game Loop)
- Backend Spring Boot + PostgreSQL + REST API tanpa error

**Bonus:**
- Upload itch.io playable di browser (bukan zip)
- Fitur tambahan di luar kriteria wajib

---

## 2. Penilaian — Checklist Compliance

| Kriteria | Status Saat Ini | Target | Section Plan |
|----------|-----------------|--------|--------------|
| Tema game (sci-fi horror lab) | ✅ | ✅ | §3, §6 |
| Berjalan tanpa error | ✅ MVP | ✅ smoke test wajib per sprint | §13 |
| Pakai backend custom (Spring Boot) | ✅ | ✅ + auth + history | §8 |
| Minimal 6 design pattern (no Game Loop) | ✅ 8 pattern | ✅ **9 pattern** | §7 |
| Spring Boot framework | ✅ 4.0.6 | ✅ | §8 |
| PostgreSQL | ✅ | ✅ + 10 tabel ternormalisasi | §9 |
| REST API tanpa error | ✅ /match, /room | ✅ + auth, stats, achievement, leaderboard, friends | §8.4 |
| **Bonus** itch.io playable di browser | ❌ | ✅ TeaVM HTML5 build | §12 (S21) |
| **Bonus** fitur tambahan | 🟡 progress: auth+JWT, WebSocket, 5 lab tasks, Guard AI, stamina, crafting (8 item + AMMO_PACK), 12 chest, multiplayer item sync, melee+ranged animations, ELO rating + match history + profile screen, **leaderboard (3 kategori) + 10 achievement + pixel art icons**, **friend system (add/accept/list/search/join room), in-room role selection, visibility toggle, kick player, public room auto-refresh** | ✅ 4v1 mode, audio cue, settings, narrative | §12 |

---

## 3. Game Design Reference

Dirangkum dari GDD utama. Untuk detail penuh lihat dokumen GDD pisah.

### 3.1 Genre, Audience, Setting

- **Genre:** 2D Asymmetrical Fighting / Strategy
- **Audience:** Pemain 15+ tahun yang menyukai game asimetris kompetitif (DBD-style), sci-fi horror
- **Setting:** NextGen-Lab — fasilitas penelitian bawah tanah canggih, atmosfer mencekam, palet warna biru neon (lab) vs merah alarm (bahaya)
- **Visual:** 2D pixel art top-down

### 3.2 Core Loop

| Peran | Loop |
|-------|------|
| **Peneliti** | Collect resources → Finish Tasks → Craft Weapons → Confrontation |
| **Monster** | Kill NPC Guards → Sabotage Facility → Evolve/Level up → Confrontation |

### 3.3 Phase

```
[ START ] ─→ PREPARATION ─→ DUEL ─→ [ END ]
                  │           │
                  └──> Sudden death (1 mati di luar arena = lawan menang)
```

**PREPARATION (default 6 menit):**
- Peneliti: kerjakan task untuk progres serum (0% → 100%)
- Monster: hunt guard untuk XP, sabotase, evolusi
- Bisa saling membunuh (sudden-death win)

**DUEL (otomatis trigger):**
- Salah satu pihak mencapai 100% **atau** timer habis
- Pemain dipindahkan ke peta Arena
- Combat projectile sampai HP lawan = 0

### 3.4 Karakter Utama

- **The Researcher:** Jenius namun rapuh; HP 3; senjata berbasis crafting; punya stamina sprint
- **The Specimen (Monster):** Kuat & adaptif; HP 5; dash/leap ability; evolusi genetik permanen per match

---

## 4. Arsitektur Sistem (High-level)

```
┌────────────────────────────────────────────────────────────────┐
│                       CLIENT (LibGDX 1.13.1)                    │
│                                                                 │
│   ┌──────────────────┐         ┌──────────────────┐            │
│   │  Desktop Build   │         │   Web Build      │            │
│   │  (LWJGL3)        │         │   (TeaVM HTML5)  │            │
│   └────────┬─────────┘         └────────┬─────────┘            │
│            └──────────────┬──────────────┘                     │
│                           ▼                                    │
│                ┌────────────────────────┐                      │
│                │    Game Core (shared)  │                      │
│                │  screen/state/entity/  │                      │
│                │  strategy/command/pool │                      │
│                └─────┬──────────────────┘                      │
│                      │                                         │
│       ┌──────────────┼──────────────┐                          │
│       ▼              ▼              ▼                          │
│  ┌────────┐   ┌─────────────┐  ┌──────────────────┐            │
│  │AssetFacade│ │BackendFacade│ │NetworkTransport│            │
│  │(Singleton)│ │(REST HTTP)  │ │(interface)     │            │
│  └────────┘   └──────┬──────┘  └────────┬───────┘            │
└─────────────────────┼─────────────────┼──────────────────────┘
                      │                 │
                      │ HTTP            │ WS / KryoNet
                      ▼                 ▼
┌─────────────────────────────────┐ ┌──────────────────────────┐
│   SPRING BOOT BACKEND (4.0.6)   │ │  REAL-TIME TRANSPORT     │
│                                 │ │                          │
│   controller / service /        │ │  • WebSocket (default)   │
│   repository / entity / dto     │ │  • KryoNet (fallback)    │
│              │                  │ │                          │
│              ▼                  │ └──────────────────────────┘
│   ┌────────────────────────┐    │
│   │  PostgreSQL (10 tabel) │    │
│   └────────────────────────┘    │
└─────────────────────────────────┘
```

### 4.1 Tanggung Jawab per Layer

| Layer | Tanggung Jawab |
|-------|----------------|
| **Game Core** | Logika gameplay, state machine, rendering, input handling, animasi |
| **AssetFacade** | Loading & caching aset (texture, map, audio) — Singleton |
| **BackendFacade** | Wrapping HTTP REST call ke Spring Boot |
| **NetworkTransport** | Real-time position sync — abstraksi transport (WebSocket/KryoNet) |
| **Spring Boot Backend** | Auth, room/match logic, stats, achievement, leaderboard |
| **PostgreSQL** | Persistensi data user, match history, achievement, friend |
| **WebSocket Server** | Broadcast PositionUpdate antar peer dalam matchId yang sama |

---

## 5. Prinsip OOP yang Diterapkan

Proyek mengikuti prinsip **OOP** dan **SOLID** secara konsisten:

### 5.1 Empat Pilar OOP

- **Encapsulation:** Field selalu `private`, akses lewat getter/setter. Entitas (Researcher, Monster) menyimpan state masing-masing tanpa exposure mentah.
- **Inheritance:** `LabTask` (abstract) → `WireTask`, `ReactorTask`, `ChemicalMixTask`, dll. Hierarki bersih dengan template method.
- **Polymorphism:** Interface `MovementStrategy`, `MoveCommand`, `GameStateHandler`, `NetworkTransport` memiliki banyak implementasi yang interchangeable di runtime.
- **Abstraction:** `AssetFacade`, `BackendFacade`, `AudioFacade` menyembunyikan detail LibGDX/HTTP/audio.

### 5.2 SOLID

- **S — Single Responsibility:** `Screen` ≠ `Entity` ≠ `Network`. `GameScreen` hanya orchestration; logika player ada di `Researcher`.
- **O — Open/Closed:** Tambah AI baru = tambah class `XYZStrategy implements MovementStrategy`. Tidak perlu ubah class lain.
- **L — Liskov Substitution:** Subclass `LabTask` apapun dapat ditaruh dimana base class diharapkan tanpa breaking caller.
- **I — Interface Segregation:** `NetworkTransport` minimal & fokus; tidak campur dengan `BackendFacade` (HTTP REST).
- **D — Dependency Inversion:** `GameScreen` depend ke abstract `GameStateHandler`, bukan ke `PreparationState` konkret.

### 5.3 Konvensi Implementasi

- Nama class: `PascalCase`; method/field: `camelCase`; konstanta: `UPPER_SNAKE_CASE`.
- Setiap entitas/screen memiliki `dispose()` eksplisit untuk resource GPU (LibGDX tidak GC otomatis).
- File di-organisasi by **package per pattern/role**: `entity/`, `state/`, `strategy/`, `command/`, `pool/`, `task/`, `screen/`, `facade/`, `factory/`, `network/`, `event/`, `ui/`, `crafting/`.

---

## 6. Spesifikasi Gameplay Detail

### 6.1 Peneliti (Researcher)

| Atribut | Nilai |
|---------|-------|
| HP | 3 |
| Speed (jalan) | 110 unit/sec |
| Speed (sprint) | 165 unit/sec (saat Shift, drain stamina) |
| Stamina max | 100 |
| Stamina drain | 30/sec saat sprint |
| Stamina regen | 15/sec saat tidak sprint, delay 1s |
| Hitbox | 30×20 px (di kaki, untuk efek 2.5D) |
| Sprite | 68×68 px, 8 arah (N/S/E/W/NE/NW/SE/SW) |

**Aksi:**
- WASD = movement 8 arah, normalisasi diagonal
- Shift = sprint (selama stamina > 0)
- E = interaksi (zona task, chest, workshop)
- Mouse click / Space = attack saat fase DUEL (proyektil dari senjata yang di-equip)

**Lab Task (5 jenis, masing-masing +20% serum):**
1. **WireTask** — sambungkan 3 kabel berwarna (sudah ada)
2. **ReactorTask** — atur 3 slider ke target (mini-game)
3. **ChemicalMixTask** — drag chemical sesuai resep (3-step puzzle)
4. **ServerHackTask** — type sequence acak dalam waktu terbatas
5. **BiometricLockTask** — pattern matching grid 4×4

**Crafting:**
- Buka panel di **Workshop** (zona khusus di map)
- Inventory menampilkan resource yang dikumpulkan
- Klik recipe → senjata/utility ditambahkan ke slot
- Maks 2 senjata + 2 utility carried

### 6.2 Monster (The Specimen)

| Atribut | Nilai |
|---------|-------|
| HP | 5 (max bisa naik dengan Thick Hide gene) |
| Speed | 130 unit/sec (lebih cepat daripada Researcher) |
| Dash distance | 3 tile (~96 px) |
| Dash cooldown | 4s (bisa dikurangi dengan Frenzy gene) |
| Hitbox | 26×20 px (lebih besar dari Researcher; disesuaikan ke tile 32px) |
| Sprite | 68×68 px, 8 arah, walk = 6-frame animation |

**Aksi:**
- WASD = movement 8 arah
- Shift = dash/leap (immune collision selama dash, ~0.2s)
- E = interaksi (sabotage panel)
- Mouse click = melee attack (range 1 tile, dmg 1)

**Sumber XP:**
- Membunuh NPC Guard: +25 XP
- Membunuh Peneliti (mode 4v1, di luar duel): +50 XP
- Sabotase berhasil: +10 XP

**Level up:**
- Threshold: 100 XP per level
- Saat level up: pause game, buka `LevelUpScreen`, pilih 1 dari 3 Evolution Gene acak
- Efek gene permanen sampai akhir match

### 6.3 NPC Guards

| Atribut | Nilai |
|---------|-------|
| HP | 2 |
| Damage | 1 (proyektil, range 2 tile) |
| Jumlah di map | 4 (lokasi tetap) |
| AI default | `PatrolStrategy` (bolak-balik 2 waypoint) |
| AI alert | `AlertChaseStrategy` (mengejar Monster jika dalam LOS 5 tile) |

**Logika:**
- Setiap frame, guard cek line-of-sight ke Monster (raycast tile-based)
- Jika terlihat → switch strategy ke `AlertChaseStrategy` selama 5 detik
- Jika hilang LOS → kembali ke `PatrolStrategy` setelah 3 detik

### 6.4 Evolution Genes (Tabel di DB: `evolution_library`)

| Gene Code | Nama | Efek |
|-----------|------|------|
| PREDATOR_CLAWS | Predator Claws | +1 melee damage |
| ADRENAL_SURGE | Adrenal Surge | +20% movement speed |
| THICK_HIDE | Thick Hide | +1 max HP |
| FRENZY | Frenzy | dash cooldown −1s |
| ECHOLOCATION | Echolocation | reveal Peneliti pos selama 3s, cooldown 30s |
| ACIDIC_BLOOD | Acidic Blood | proyektil pasif saat di-attack |
| REGENERATION | Regeneration | +0.5 HP/sec saat tidak combat |
| TOXIC_AURA | Toxic Aura | slow musuh dalam radius 2 tile |
| PHASE_SHIFT | Phase Shift | tembus wall sekali, cooldown 20s |
| BERSERKER | Berserker | +30% dmg saat HP < 2 |

Disimpan di tabel `evolution_library` dengan kolom `effect_json` untuk metadata efek (parser di game).

### 6.5 Sabotage System

**3 jenis sabotage** (Monster tekan E pada Sabotage Panel):

| Sabotage | Efek | Durasi | Cooldown |
|----------|------|--------|----------|
| **Lights Out** | Pandangan Peneliti dipersempit (overlay gelap) | 10s | 30s |
| **Slow Field** | Speed Peneliti −50% | 8s | 25s |
| **Serum Drain** | Progres serum −10% | instant | 60s |

Panel sabotase ada **3 lokasi** tetap di map. Cooldown global per panel.

### 6.6 Resource & Crafting

**5 jenis resource:**
| Resource | Spawn weight |
|----------|--------------|
| Scrap | 35% |
| Battery | 25% |
| Chemical | 20% |
| Circuit | 15% |
| Bioplasma | 5% (rare) |

**12 chest/locker** tersebar di map. Isi = 1–3 resource random (weighted).

**Recipe (5 senjata + 3 utility):**

| Item | Tipe | Bahan | Efek |
|------|------|-------|------|
| Pistol | Weapon | 2× Scrap | Proyektil dasar, dmg 1 |
| Stun Gun | Weapon | 1× Battery + 1× Scrap | Stun Monster 2s, dmg 0.5 |
| Acid Grenade | Weapon | 2× Chemical | Area damage, dmg 2 |
| Taser | Weapon | 2× Battery | Range short, dmg 1.5 + slow |
| Rail Gun | Weapon | 1× Bioplasma + 2× Circuit | Hi-damage 3, slow charge |
| Heal Kit | Utility | 1× Bioplasma + 1× Chemical | Heal +1 HP |
| Trap | Utility | 1× Scrap + 1× Battery | Stun 3s saat Monster step |
| Decoy | Utility | 1× Circuit + 1× Chemical | Lure guards/Monster ke titik |

Disimpan di tabel `resource_recipe` dengan kolom `ingredients_json`.

#### 6.5.1 Strategi Visual Senjata (Staged)

Untuk menghindari ledakan aset (5 senjata × 8 arah × 7 frame = 280 frame), implementasi visual senjata dilakukan **bertahap**:

**Phase 1 (S12) — Iconic / HUD-only:**
- Senjata yang sedang di-equip ditampilkan **hanya sebagai icon di HUD** (slot bar di bawah/samping screen)
- Karakter tidak menampilkan sprite senjata di tangan
- Saat tembak, proyektil langsung keluar dari posisi karakter
- **Aset baru:** 5 icon senjata 32×32 px + 3 icon utility = **8 sprite kecil**
- Tujuan: gameplay loop crafting + combat selesai dulu, fokus ke mekanik

**Phase 2 (S23 Polish) — Layered Rendering:**
- Upgrade ke layered render: karakter base + sprite senjata terpisah di-overlay di posisi tangan
- 1 sprite per senjata (16–24 px), di-rotasi/flip berdasarkan arah karakter, atau buat 4 varian arah (N/S/E/W) untuk tampilan natural
- Implementasi di `Researcher.render()`:
  ```
  1. Render base sprite karakter di (x, y)
  2. Hitung handOffset(direction, frameIndex) — posisi tangan per frame
  3. Render weapon sprite di posisi tangan dengan rotasi sesuai direction
  ```
- Field baru di `Researcher`: `Item equippedWeapon`, `Vector2 handOffset(Direction, int frame)`
- **Aset baru:** 5 senjata × 4 arah = **20 sprite kecil** (bukan 280 frame)

**Justifikasi:** Phase 1 → Phase 2 memungkinkan fokus ke gameplay dulu (S12), polish visual menyusul saat semua sistem stabil (S23). Tidak ada aset Phase 1 yang terbuang — icon HUD tetap dipakai di Phase 2.

### 6.7 Multiplayer Visual Sync (Wajib)

Saat multiplayer, **gerakan & state visual lawan harus terlihat sinkron** dengan input aktual pemain lain. Tidak cukup hanya sinkron posisi (x, y) — sprite, arah hadap, dan animasi juga harus sinkron sehingga pemain dapat membaca aksi lawan secara jelas dan adil.

**Yang harus tersinkronisasi (dikirim via `PositionUpdate`):**

| Field | Tipe | Penjelasan |
|-------|------|------------|
| `matchId` | long | Identifier match (untuk routing broadcast) |
| `role` | String | `"RESEARCHER"` atau `"MONSTER"` |
| `x`, `y` | float | Posisi world |
| **`direction`** | enum (8) | Arah hadap sprite: N/S/E/W/NE/NW/SE/SW |
| **`moving`** | boolean | `true` = animasi walking; `false` = idle frame |
| **`actionFlag`** | byte (bitmask) | Bit untuk: `DASHING`, `ATTACKING`, `HIT`, `SABOTAGING`, `INTERACTING` |
| `hp` | int | Sinkronisasi HP visual (untuk HP bar lawan, opsional) |
| `timestamp` | long | Untuk interpolasi & ordering |

**Konsekuensi rendering di sisi peer:**
- Saat `moving=true` & `direction=W` → render `walkAnimW` di sprite remote
- Saat `moving=false` & `direction=W` → render `idleW` (frame statis arah barat)
- Saat `actionFlag & DASHING` → render dash trail effect + sprite dash pose
- Saat `actionFlag & ATTACKING` → render attack frame + projectile origin

**Frekuensi & interpolasi:**
- Broadcast: **20 Hz** (setiap 50 ms) saat ada perubahan signifikan (pos berubah > 1 px atau direction/state berubah)
- Sisi penerima: **interpolasi linear** antara dua sample terakhir untuk gerakan smooth (bukan teleport)
- Toleransi network jitter: buffer 100 ms untuk extrapolasi jika packet drop

**Kriteria penerimaan (acceptance criteria):**
- Pemain 1 lari ke barat → di layar pemain 2: sprite Pemain 1 menampilkan animasi walking arah barat dalam ≤ 100 ms
- Pemain 1 berhenti → sprite di layar Pemain 2 berhenti animasi & menampilkan idle frame west
- Pemain 1 dash → sprite di layar Pemain 2 tampak meluncur dengan dash effect
- Pemain 1 menembak proyektil → proyektil terlihat oleh Pemain 2 (proyektil sync — saat ini belum, akan di-fix di S7)

### 6.8 Win Condition

**Sebelum DUEL (sudden-death):**
- Peneliti HP = 0 → **Monster menang**
- Monster HP = 0 → **Peneliti menang**

**Trigger DUEL:**
- Salah satu progres mencapai 100% → DUEL phase
- ATAU timer 6 menit habis → DUEL phase (kondisi: progres tertinggi dapat buff +HP)

**DUEL phase:**
- Map berubah ke `arena.tmx`
- Combat sampai HP lawan = 0
- Pemenang = lawan HP 0

---

## 7. Design Pattern Implementation (9 pattern)

> Sesuai keputusan, **9 pattern** ditampilkan untuk memenuhi minimal 6 + bonus.

### 7.1 Singleton — `AssetFacade`

**Lokasi:** `Game/core/.../facade/AssetFacade.java`

**Peran:** Satu instance global untuk loading & caching aset (texture, map, audio). Mencegah duplikasi load.

**Justifikasi:** Asset loading mahal (disk I/O + GPU upload). Dengan single instance, satu cache shared di seluruh game.

```java
public class AssetFacade {
    private static AssetFacade instance;
    private AssetManager manager;

    private AssetFacade() { manager = new AssetManager(); ... }

    public static AssetFacade getInstance() {
        if (instance == null) instance = new AssetFacade();
        return instance;
    }
}
```

### 7.2 Facade — `AssetFacade`, `BackendFacade`, `AudioFacade`

**Lokasi:** `facade/`

**Peran:** Sembunyikan kompleksitas API LibGDX/HTTP/audio behind interface sederhana.

**Contoh:** `BackendFacade.startMatch(callback)` menyembunyikan `Gdx.net.sendHttpRequest(...)` + parsing JSON + thread handling.

### 7.3 Factory — `EntityFactory`

**Lokasi:** `factory/EntityFactory.java`

**Peran:** Centralized entity creation. Caller tidak tahu detail constructor.

```java
public class EntityFactory {
    public static Researcher createResearcher(TiledMap map) { ... }
    public static Monster createMonster(TiledMap map) { ... }
    public static Guard createGuard(TiledMap map, Vector2 spawn) { ... } // NEW
    public static Chest createChest(Vector2 pos, ChestTier tier) { ... } // NEW
}
```

### 7.4 Command — `MoveCommand`, `InputHandler`

**Lokasi:** `command/`

**Peran:** Encapsulate input sebagai object yang bisa di-execute, queue, atau undo.

```java
public interface MoveCommand {
    void execute(Researcher r, float delta);
}
public class MoveNorthCommand implements MoveCommand { ... }
public class InputHandler {
    public MoveCommand resolve() { /* baca input, kembalikan Command */ }
}
```

**Manfaat tambahan:** ke depan bisa direkam untuk replay system.

### 7.5 State — `GameStateHandler`, `PreparationState`, `DuelState`

**Lokasi:** `state/`

**Peran:** Fase game sebagai object terpisah. Switching state = ganti object, bukan if-else cascade.

```java
public interface GameStateHandler {
    void enter(GameScreen screen);
    void update(float delta, GameScreen screen);
    void render(SpriteBatch batch, GameScreen screen);
    void exit(GameScreen screen);
}
```

`GameScreen` punya `currentState` dan delegate semua callback ke state aktif.

### 7.6 Strategy — `MovementStrategy`, `ChasePlayerStrategy`, `PatrolStrategy`, `AlertChaseStrategy`

**Lokasi:** `strategy/`

**Peran:** AI swap behavior runtime tanpa subclass.

```java
public interface MovementStrategy {
    void move(Monster m, float targetX, float targetY, float delta);
}
```

Guard dapat berpindah dari `PatrolStrategy` ke `AlertChaseStrategy` saat melihat Monster.

### 7.7 Object Pool — `Projectile`, `ProjectilePool`

**Lokasi:** `pool/`

**Peran:** Reuse 20 projectile pre-allocated. Hindari `new Projectile()` saat tembak (allocation cost + GC pressure).

```java
Projectile p = pool.obtain();
p.init(x, y, dirX, dirY);
// saat off-screen / hit:
p.deactivate(); // pool bisa reuse
```

### 7.8 Template Method — `LabTask` (abstract)

**Lokasi:** `task/LabTask.java`

**Peran:** Base class menyediakan kerangka (Stage Scene2D, completion flag, lifecycle), subclass implementasi `buildUI()`.

```java
public abstract class LabTask {
    protected Stage stage;
    protected boolean completed;
    public final void start() {
        stage = new Stage(...);
        buildUI();              // hook untuk subclass
    }
    protected abstract void buildUI();   // implement di subclass
    public final void finish() { completed = true; ... }
}
```

### 7.9 Observer — `EventBus`, `GameEventListener` (NEW di S17)

**Lokasi:** `event/`

**Peran:** Decouple event domain (e.g. `OnTaskCompleted`, `OnMonsterLevelUp`, `OnSerumProgress`) dari subscriber (HUD update, audio cue, network broadcast, achievement check).

```java
public interface GameEventListener<E> {
    void onEvent(E event);
}

public class EventBus {
    private Map<Class<?>, List<GameEventListener<?>>> listeners = new HashMap<>();

    public <E> void subscribe(Class<E> type, GameEventListener<E> l) { ... }
    public <E> void publish(E event) { /* dispatch */ }
}
```

**Use cases:**
- HUD subscribe `OnSerumProgress` → update progress bar
- AudioFacade subscribe `OnSerumProgress` → trigger alarm di 80%
- AchievementService subscribe `OnTaskCompleted` → cek achievement unlock
- KryoNetTransport subscribe `OnPlayerHit` → broadcast ke peer

---

## 8. Backend Spring Boot

### 8.1 Module Structure

```
backend/src/main/java/com/nextgenlab/backend/
├── BackendApplication.java
├── config/
│   ├── SecurityConfig.java       (BCrypt + JWT)
│   ├── WebSocketConfig.java
│   ├── KryoNetConfig.java
│   └── CorsConfig.java           (untuk web build itch.io)
├── controller/
│   ├── StatusController.java
│   ├── AuthController.java
│   ├── MatchController.java
│   ├── RoomController.java       (rename dari GameController)
│   ├── StatsController.java
│   ├── AchievementController.java
│   ├── LeaderboardController.java
│   └── FriendController.java
├── service/
│   ├── AuthService.java
│   ├── MatchService.java
│   ├── RoomService.java
│   ├── StatsService.java
│   ├── AchievementService.java
│   ├── LeaderboardService.java
│   ├── FriendService.java
│   └── EvolutionLibraryService.java
├── model/
│   ├── entity/
│   │   ├── User.java
│   │   ├── MatchSession.java
│   │   ├── MatchHistory.java
│   │   ├── PlayerStats.java
│   │   ├── Achievement.java
│   │   ├── UserAchievement.java
│   │   ├── Friendship.java
│   │   ├── EvolutionGene.java
│   │   ├── ResourceRecipe.java
│   │   └── GameRoom.java
│   └── dto/
│       ├── auth/   (RegisterRequest, LoginRequest, AuthResponse, UserDTO)
│       ├── match/  (MatchStartRequest, MatchStartResponse, ProgressUpdateRequest, MatchFinishRequest)
│       ├── room/   (RoomRequest, RoomResponse)
│       ├── stats/  (PlayerStatsDTO, MatchHistoryDTO)
│       └── ...
├── repository/   (one per entity)
├── security/
│   ├── JwtUtil.java
│   ├── JwtFilter.java
│   └── CustomUserDetailsService.java
├── seeder/
│   └── DataSeeder.java   (seed evolution_library, achievements, resource_recipe at startup)
├── kryo/
│   ├── PositionUpdate.java
│   └── GameKryoServer.java
└── websocket/
    ├── GameWebSocketConfig.java
    └── GameWebSocketHandler.java
```

### 8.2 Security

- **Password hashing:** `BCryptPasswordEncoder` (strength 10)
- **Token:** JWT via `io.jsonwebtoken:jjwt-api` (HS256, expiry 24h)
- **Header:** `Authorization: Bearer <token>` untuk endpoint protected
- **Filter:** `JwtFilter extends OncePerRequestFilter` → set `SecurityContextHolder`

**Endpoint public:**
- `GET  /api/status`
- `POST /api/auth/register`
- `POST /api/auth/login`

**Endpoint protected (perlu JWT):** semua endpoint lain.

### 8.3 Real-time Transport (DUAL)

#### WebSocket (default)

- Endpoint: `/ws/game`
- Handler: `GameWebSocketHandler extends TextWebSocketHandler`
- Protocol: JSON message dengan format `{ "matchId":1, "role":"RESEARCHER", "x":120.5, "y":85.0 }`
- Broadcast: handler maintain `Map<Long, Set<WebSocketSession>>` per matchId. Saat receive dari satu peer, broadcast ke semua peer lain di matchId yang sama.
- **Web-compatible:** browser native WebSocket API.

#### KryoNet (fallback)

- Sudah ada di `kryo/GameKryoServer.java` (TCP 54555, UDP 54777)
- Aktif paralel dengan WebSocket — pemain pilih mode di `LobbyScreen` (NETWORK_SELECT).
- Auto-disable pada web build (TeaVM) karena `java.net.Socket` tidak tersedia.

### 8.4 REST Endpoint List Lengkap

| Method | Path | Auth | Body | Response | Service |
|--------|------|------|------|----------|---------|
| GET | `/api/status` | ❌ | — | `{"status":"OK"}` | StatusController |
| POST | `/api/auth/register` | ❌ | `{email, username, password}` | `{userId, token}` | AuthService |
| POST | `/api/auth/login` | ❌ | `{email, password}` | `{userId, token}` | AuthService |
| GET | `/api/auth/me` | ✅ | — | `UserDTO` | AuthService |
| POST | `/api/match/start` | ✅ | `{role, mode}` | `{matchId}` | MatchService |
| POST | `/api/match/{id}/update` | ✅ | `{role, amount}` | `200 OK` | MatchService |
| GET | `/api/match/{id}/status` | ✅ | — | `{phase, researcherProg, monsterProg}` | MatchService |
| POST | `/api/match/{id}/finish` | ✅ | `{winner, durationSec, statsPayload}` | `200 OK` | MatchService |
| POST | `/api/room/create` | ✅ | `{role, isPrivate}` | `{roomCode}` | RoomService |
| POST | `/api/room/join/{code}` | ✅ | `{role}` | `{matchId}` | RoomService |
| GET | `/api/room/{code}/status` | ✅ | — | `{players, phase}` | RoomService |
| GET | `/api/stats/{userId}` | ✅ | — | `PlayerStatsDTO` | StatsService |
| GET | `/api/stats/{userId}/history?limit=20` | ✅ | — | `MatchHistoryDTO[]` | StatsService |
| GET | `/api/leaderboard?limit=10` | ✅ | — | `Top10[]` | LeaderboardService |
| GET | `/api/achievements` | ✅ | — | `AchievementWithStatus[]` | AchievementService |
| POST | `/api/achievements/{id}/claim` | ✅ | — | `200 OK` | AchievementService |
| POST | `/api/friends/request` | ✅ | `{targetUserId}` | `200 OK` | FriendService |
| POST | `/api/friends/accept/{requestId}` | ✅ | — | `200 OK` | FriendService |
| GET | `/api/friends` | ✅ | — | `FriendDTO[]` | FriendService |
| DELETE | `/api/friends/{userId}` | ✅ | — | `200 OK` | FriendService |
| GET | `/api/evolution-genes` | ✅ | — | `EvolutionGene[]` | EvolutionLibraryService |
| GET | `/api/recipes` | ✅ | — | `Recipe[]` | EvolutionLibraryService |

**Total: 22 endpoint REST.**

---

## 9. Database Schema (PostgreSQL)

### 9.1 Schema (10 tabel ternormalisasi)

```sql
-- 1. users
CREATE TABLE users (
    id              BIGSERIAL PRIMARY KEY,
    email           VARCHAR(120) UNIQUE NOT NULL,
    username        VARCHAR(50) NOT NULL,
    password_hash   VARCHAR(255) NOT NULL,
    created_at      TIMESTAMP DEFAULT NOW()
);

-- 2. match_sessions
CREATE TABLE match_sessions (
    id                   BIGSERIAL PRIMARY KEY,
    status               VARCHAR(20) DEFAULT 'PREPARATION', -- PREPARATION | DUEL | ENDED
    researcher_progress  INT DEFAULT 0,
    monster_progress     INT DEFAULT 0,
    started_at           TIMESTAMP DEFAULT NOW(),
    ended_at             TIMESTAMP
);

-- 3. match_history
CREATE TABLE match_history (
    id                BIGSERIAL PRIMARY KEY,
    match_id          BIGINT REFERENCES match_sessions(id),
    user_id           BIGINT REFERENCES users(id),
    role              VARCHAR(20),  -- RESEARCHER | MONSTER
    won               BOOLEAN,
    duration_sec      INT,
    kills             INT DEFAULT 0,
    tasks_completed   INT DEFAULT 0
);

-- 4. player_stats
CREATE TABLE player_stats (
    user_id          BIGINT PRIMARY KEY REFERENCES users(id),
    total_matches    INT DEFAULT 0,
    wins             INT DEFAULT 0,
    losses           INT DEFAULT 0,
    total_kills      INT DEFAULT 0,
    total_tasks      INT DEFAULT 0,
    elo_rating       INT DEFAULT 1000
);

-- 5. achievements (seeded)
CREATE TABLE achievements (
    id           BIGSERIAL PRIMARY KEY,
    code         VARCHAR(50) UNIQUE NOT NULL,
    name         VARCHAR(100),
    description  TEXT,
    icon         VARCHAR(255)
);

-- 6. user_achievements
CREATE TABLE user_achievements (
    user_id         BIGINT REFERENCES users(id),
    achievement_id  BIGINT REFERENCES achievements(id),
    unlocked_at     TIMESTAMP DEFAULT NOW(),
    PRIMARY KEY (user_id, achievement_id)
);

-- 7. friendships
CREATE TABLE friendships (
    id             BIGSERIAL PRIMARY KEY,
    requester_id   BIGINT REFERENCES users(id),
    addressee_id   BIGINT REFERENCES users(id),
    status         VARCHAR(20),  -- PENDING | ACCEPTED | BLOCKED
    created_at     TIMESTAMP DEFAULT NOW()
);

-- 8. evolution_library (seeded — 10 gene)
CREATE TABLE evolution_library (
    id           BIGSERIAL PRIMARY KEY,
    code         VARCHAR(50) UNIQUE NOT NULL,
    name         VARCHAR(100),
    description  TEXT,
    effect_json  JSONB
);

-- 9. resource_recipe (seeded — 8 recipe)
CREATE TABLE resource_recipe (
    id                BIGSERIAL PRIMARY KEY,
    code              VARCHAR(50) UNIQUE NOT NULL,
    name              VARCHAR(100),
    ingredients_json  JSONB,
    output_item       VARCHAR(50),
    output_qty        INT DEFAULT 1
);

-- 10. game_rooms
CREATE TABLE game_rooms (
    id                  BIGSERIAL PRIMARY KEY,
    room_code           VARCHAR(10) UNIQUE NOT NULL,
    match_session_id    BIGINT REFERENCES match_sessions(id),
    status              VARCHAR(20),  -- WAITING | PLAYING | ENDED
    host_user_id        BIGINT REFERENCES users(id),
    is_private          BOOLEAN DEFAULT FALSE,
    created_at          TIMESTAMP DEFAULT NOW()
);
```

### 9.2 ER Diagram (text-based)

```
┌─────────┐   1   ┌────────────────┐   M    ┌──────────────────┐
│  users  │───────│ match_history  │────────│ match_sessions    │
│         │       │                │        │                  │
└──┬──────┘       └────────────────┘        └──────────────────┘
   │                                                   │
   │ 1                                                 │ 1
   │                                                   │
   │ 1                                                 │
   ▼                                                   ▼ 1
┌──────────────┐                              ┌──────────────────┐
│ player_stats │                              │   game_rooms     │
└──────────────┘                              └──────────────────┘
   │
   │
┌──┴────────────────┐  ┌──────────┐  ┌──────────────────┐
│                    │  │          │  │                  │
▼                    ▼  ▼          ▼  ▼                  ▼
┌──────────────────┐ ┌─────────────────┐  ┌──────────────┐
│ user_achievements│ │   friendships   │  │ achievements │ (seeded)
└────────┬─────────┘ └─────────────────┘  └──────────────┘
         │
         ▼
   (M:N junction)

┌─────────────────┐  ┌─────────────────┐
│ evolution_library│  │ resource_recipe│  (seeded — independent)
└─────────────────┘  └─────────────────┘
```

### 9.3 Konfigurasi (`application.properties`)

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/nextgenlab_db
spring.datasource.username=postgres
spring.datasource.password=postgres
spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true

# JWT
app.jwt.secret=${JWT_SECRET:change-this-in-prod-please-use-a-long-random-string}
app.jwt.expiration-ms=86400000

# WebSocket
app.websocket.endpoint=/ws/game

# KryoNet
app.kryonet.tcp-port=54555
app.kryonet.udp-port=54777
```

---

## 10. Frontend / Game Architecture (LibGDX)

### 10.1 Module Structure

```
Game/
├── core/                           Java 8, shared logic
│   └── src/main/java/com/nextgenlab/game/
│       ├── NextGenLabGame.java
│       ├── screen/
│       ├── state/
│       ├── entity/
│       ├── strategy/
│       ├── command/
│       ├── pool/
│       ├── task/
│       ├── facade/
│       ├── factory/
│       ├── network/                (NetworkTransport interface + impl)
│       ├── event/                  (EventBus — Observer pattern)
│       ├── crafting/               (Resource, Recipe, RecipeLibrary)
│       └── ui/
├── lwjgl3/                         Desktop launcher
│   └── src/main/java/.../lwjgl3/Lwjgl3Launcher.java
└── teavm/                          NEW: Web launcher untuk itch.io (S21)
    └── src/main/java/.../teavm/TeaVMLauncher.java
```

### 10.2 Network Abstraction (kunci dual transport)

```java
// network/NetworkTransport.java
public interface NetworkTransport {
    void connect(String host, int port);
    void send(PositionUpdate update);
    void onReceive(Consumer<PositionUpdate> callback);
    void close();
    boolean isConnected();
}

// network/PositionUpdate.java
//   Field WAJIB untuk full visual sync (lihat §6.7):
//     long    matchId
//     String  role          ("RESEARCHER" | "MONSTER")
//     float   x, y
//     byte    direction     (enum 0-7: N/S/E/W/NE/NW/SE/SW)
//     boolean moving        (true = walking anim, false = idle anim)
//     byte    actionFlag    (bitmask: DASHING | ATTACKING | HIT | SABOTAGING | INTERACTING)
//     int     hp
//     long    timestamp     (untuk interpolasi & ordering)

// network/KryoNetTransport.java implements NetworkTransport
//   (refactor dari facade/GameKryoClient.java existing)
//   register(PositionUpdate.class, 10)

// network/WebSocketTransport.java implements NetworkTransport
//   - Desktop: Java-WebSocket library (org.java-websocket:Java-WebSocket)
//   - TeaVM: pakai jsobject WebSocket browser native via wrapper
//   - Encode PositionUpdate ke JSON: {"matchId":..., "role":..., "x":..., "y":..., "dir":..., "mov":..., "act":..., "hp":..., "ts":...}
```

**Sisi penerima (rendering remote player):**
```java
// Di Researcher.java / Monster.java — mode "remote-controlled":
public void applyRemoteUpdate(PositionUpdate u) {
    // Interpolasi posisi (smooth, bukan teleport)
    targetX = u.x; targetY = u.y;
    // Sinkron arah hadap
    lastDirection = Direction.values()[u.direction];
    // Sinkron animasi walk vs idle
    if (u.moving) currentAnimation = walkAnimFor(lastDirection);
    else          currentAnimation = idleAnimFor(lastDirection);
    // Sinkron action visual
    if ((u.actionFlag & FLAG_DASHING) != 0) showDashTrail();
    if ((u.actionFlag & FLAG_ATTACKING) != 0) playAttackFrame();
    if ((u.actionFlag & FLAG_HIT) != 0) flashRed();
}
```

`PreparationState` & `DuelState` — saat `applyMonsterInput()` (atau `applyResearcherInput()`) di sisi local, **panggil `kryoClient.sendPosition(...)` dengan field lengkap** (direction + moving + actionFlag), bukan hanya x/y.

`NextGenLabGame` menerima `NetworkTransport` via constructor. Pemilihan implementasi:
- Desktop launcher: dapat pilih KryoNet atau WebSocket di setting
- Web launcher: hardcoded WebSocket (KryoNet tidak compile)

### 10.3 Screen Flow

```
SplashScreen (logo, 2s)
   ↓
MenuScreen (title, "Press ENTER")
   ↓
AuthScreen (Login | Register | Guest)
   ↓
MainMenuScreen
   ├── Play → LobbyScreen
   │            ├── Network mode select (Localhost/LAN/Internet)
   │            ├── Create Room (pilih role: Researcher/Monster)
   │            ├── Join Room (input code + role)
   │            └── Single-player (offline AI)
   │                 ↓
   │            GameScreen
   │                ├── PreparationState
   │                └── DuelState
   │                       ↓
   │                 GameOverScreen → MainMenu
   ├── Profile → ProfileScreen (stats, match history)
   ├── Leaderboard → LeaderboardScreen
   ├── Achievement → AchievementScreen
   ├── Friends → FriendScreen
   └── Settings → SettingsScreen
```

### 10.4 Settings Menu

Item yang dapat dikonfigurasi:
- Volume Master (0–100)
- Volume Music (0–100)
- Volume SFX (0–100)
- Fullscreen toggle
- Server address (localhost / LAN-input-IP / Internet-input-IP)
- Network transport (WebSocket / KryoNet) — desktop only
- Logout
- (S22+) Key remap

Disimpan ke `~/.nextgenlab/settings.json` (user home).

### 10.5 In-game Hint Popup (Tutorial)

Ditampilkan once-per-session pada event:
- Pertama kali mendekati zona task → "Tekan E untuk mulai task"
- Pertama kali ke Workshop → "Tekan E untuk membuka panel crafting"
- Pertama kali HP ≤ 1 → "Hati-hati! Hindari Monster"
- Pertama kali Monster level up → "Pilih 1 Evolution Gene"

Tracked via flag di `EventBus` listener — store di session memory (tidak persistent).

---

## 11. Audio Plan

### 11.1 Source

- **freesound.org** — CC0/CC-BY (kredit di README)
- **opengameart.org** — CC0
- **Pixellab.ai** (jika ada generator audio) — atau eksternal tool

### 11.2 File List (placeholder filename)

| Kategori | File | Keterangan |
|----------|------|------------|
| BGM | `bgm_lab_ambient.ogg` | Loop tense ambient saat PREPARATION |
| BGM | `bgm_duel.ogg` | Intense combat track saat DUEL |
| BGM | `bgm_menu.ogg` | Menu screen background |
| SFX | `sfx_footstep_concrete.ogg` | Footstep variant 1 |
| SFX | `sfx_footstep_metal.ogg` | Footstep variant 2 |
| SFX | `sfx_footstep_grass.ogg` | Footstep variant 3 |
| SFX | `sfx_task_complete.ogg` | Task selesai |
| SFX | `sfx_alarm.ogg` | Saat progres mencapai 80% |
| SFX | `sfx_monster_growl.ogg` | Saat radius proximity Monster |
| SFX | `sfx_dash.ogg` | Monster dash |
| SFX | `sfx_hit.ogg` | Hit player |
| SFX | `sfx_levelup.ogg` | Monster level up |
| SFX | `sfx_button_click.ogg` | UI feedback |
| SFX | `sfx_button_hover.ogg` | UI feedback |

### 11.3 Integrasi (Sprint 18)

```java
// facade/AudioFacade.java
public class AudioFacade {
    private static AudioFacade instance;
    private Map<String, Sound> sfx;
    private Map<String, Music> bgm;
    private float masterVol, musicVol, sfxVol;

    public void playSfx(String name) { ... }
    public void playBgm(String name, boolean loop) { ... }
    public void stopBgm() { ... }
    public void setMasterVol(float v) { ... }
}
```

EventBus subscribe contoh:
- `OnSerumProgress` → jika progres ≥ 80% & flag belum trigger → play `sfx_alarm.ogg`
- `OnTaskCompleted` → play `sfx_task_complete.ogg`
- `OnMonsterLevelUp` → play `sfx_levelup.ogg`

---

## 12. Sprint Roadmap

> **Estimasi total:** 2.5–3 bulan kerja part-time. Setiap sprint diakhiri dengan smoke test + commit + push GitHub.

### Sprint Sudah Selesai (S1–S11) ✅

| Sprint | Fokus | Output Utama |
|--------|-------|--------------|
| **S1** ✅ | Refactor struktural | `manager → facade`, extract Researcher dari GameScreen, Factory pattern |
| **S2** ✅ | Task system (WireTask + zona interaksi) | WireTask 3 wires, integrasi GameScreen, Command pattern |
| **S3** ✅ | State pattern + backend status polling | PreparationState, DuelState, MatchService backend |
| **S4** ✅ | Monster + Strategy AI | Monster entity dengan dash, Chase/Patrol strategy |
| **S5** ✅ | Object Pool projectile + combat | ProjectilePool, hit detection, GameOverScreen |
| **S6** ✅ | HUD + multiplayer KryoNet | HudOverlay, KryoNet UDP/TCP, Lobby room code |
| **S7** ✅ | Network abstraction + WebSocket + Full Visual Sync | WebSocketTransport, PositionUpdate expand, 8-arah sync |
| **S8** ✅ | Auth & User module | JWT, BCrypt, AuthScreen, BackendFacade rewrite |
| **S9** ✅ | Map multi-room + 5 lab task | lab.tmx selesai, 5 task type, combat Preparation |
| **S10** ✅ | NPC Guards + Stamina system | Guard AI (Patrol/AlertChase), wall-aware LOS, stamina bar |
| **S11** ✅ | Resource + Crafting + Inventory | 8 item, 5 senjata efek, 12 chest (tileset), crafting panel, multiplayer sync |
| **S12** ✅ | Evolution Genes + Sabotage + Monster Dash | 10 gene, LevelUpScreen card UI, dash + 2 ability (Q/G), 3 sabotage panel, gene HUD panel, XP bar sync |
| **S13** ✅ | Arena Duel + Ammo + Utility Stock + Combat Balance | arena.tmx, match timer 3 mnt, prep buff, ammo system per-senjata, utility stok multi, InventoryPanel interaktif, mouse-cursor shoot, monster ranged CD, gene abilities di Duel, utility F key di Duel |
| **S14** ✅ | Stats + Match History + ELO + Profile | PlayerStats, MatchHistory entity, ELO K-32, finishMatch idempoten (pessimistic lock + unique constraint), durasi match penuh, ProfileScreen dari Menu & Lobby, ammo reserve system |
| **S15** ✅ | Leaderboard + Achievement | 10 achievement auto-unlock, AchievementService hook ke finishMatch, LeaderboardService (3 kategori), popup GameOverScreen, AchievementScreen + LeaderboardScreen, 10 pixel art icon PixelLab AI |
| **S16** ✅ | Friend System + Room Redesign + Visibility + Kick | Friendship entity, FriendService, FriendController, FriendScreen (3 tab), join room teman, in-room role claim, visibility toggle, kick + kick detection, public room auto-refresh |
| **S17** ✅ | EventBus + SettingsScreen + Responsive UI | Observer pattern (design pattern ke-9): EventBus singleton + 5 event class + publisher di PreparationState + subscriber di GameScreen; SettingsScreen (volume slider, server address, fullscreen toggle, logout, LibGDX Preferences persist); FitViewport(1280,720) migrasi 12 UI screen; window default 1280×720 |

### Sprint Tersisa (S7–S24)

#### **S7 — Network abstraction + WebSocket + Full Visual Sync** ✅ DONE (2026-05-02)

> **Catatan implementasi:** WebSocket transport berfungsi penuh (8-arah sync, animation full FPS, projectile spawn sync). KryoNet sempat diintegrasikan tapi gagal karena Kryo `Buffer underflow` di handshake — disable Unsafe via `-Dkryo.unsafe=false` JVM args tidak efektif (Kryo's static initializer sudah jalan). Code KryoNet dipindah ke `Game/core/disabled-kryo/` & `Backend/disabled-kryo/` untuk debug masa depan. WebSocket cukup memenuhi kebutuhan multiplayer + sudah siap untuk S21 (TeaVM browser build).
>
> **Hotfix 2026-05-02 — sync progress task & transisi fase:** Ditemukan dua bug pasca-test multi-window:
> 1. Progress bar task di sisi Monster tidak sinkron (mentok di N-1/N) — `tasksCompleted` hanya di-track lokal di sisi Researcher, dan paket terakhir tidak terkirim karena `phaseComplete=true` early-return sebelum `netSendTimer` berikutnya.
> 2. HP Monster tidak berkurang saat ditembak Researcher — akar masalah sama: Monster belum masuk `DuelState`, sehingga `pollProjectile` & collision check tidak jalan di sisi Monster.
>
> **Solusi:** Tambah field `int taskProgress` di `PositionUpdate`. `PreparationState` (sisi Researcher) carry `tasksCompleted` di setiap snapshot **dan** force-send snapshot tepat saat task increment biar paket final pasti nyampe. Sisi Monster: saat polling, kalau `u.taskProgress > tasksCompleted` → update lokal + set `phaseComplete=true` jika ≥ TOTAL_TASKS. Sebagai safety net, snapshot `DuelState` carry `taskProgress=Integer.MAX_VALUE` agar Monster tetap auto-transisi walau miss paket terakhir dari PreparationState.
>
> **File hotfix:** `network/PositionUpdate.java`, `network/WebSocketTransport.java`, `state/PreparationState.java`, `state/DuelState.java`.
- Buat `network/NetworkTransport.java` interface
- **Expand `PositionUpdate`** dengan field: `direction` (byte 0–7), `moving` (boolean), `actionFlag` (byte bitmask: DASHING/ATTACKING/HIT/SABOTAGING/INTERACTING), `hp` (int), `timestamp` (long) — lihat §6.7 & §10.2
- Refactor `facade/GameKryoClient` → `network/KryoNetTransport implements NetworkTransport`
- Buat `network/WebSocketTransport implements NetworkTransport` (Java-WebSocket library)
- Backend: rewrite `websocket/GameWebSocketHandler.java` (sebelumnya placeholder), broadcast PositionUpdate full payload ke peer dalam matchId
- Backend: tambah `spring-boot-starter-websocket` ke `build.gradle`
- `LobbyScreen` tambah toggle transport (WebSocket default)
- **Refactor sisi pengirim:** di `PreparationState` & `DuelState`, saat panggil `sendPosition(...)`, ikutkan direction + moving + actionFlag (saat ini hanya x,y)
- **Refactor sisi penerima:** `Researcher.applyRemoteUpdate(...)` dan `Monster.applyRemoteUpdate(...)` baru, terapkan: posisi (interpolasi 100ms), direction → switch anim arah, moving → walk vs idle, actionFlag → dash trail / attack frame / hit flash
- **Sinkronisasi proyektil:** broadcast `ProjectileSpawnEvent` saat tembak, peer spawn proyektil di pool lokal mereka
- **Verifikasi (acceptance test wajib):**
  - Run 2 instance di mesin yang sama (1 Researcher + 1 Monster)
  - Pemain 1 jalan ke 4 arah utama + 4 diagonal → di layar pemain 2: sprite menampilkan animasi walking arah yang benar dalam ≤ 100 ms
  - Pemain 1 berhenti → sprite di layar pemain 2 berhenti animasi & tampil idle frame arah terakhir
  - Pemain 1 (Monster) dash → sprite di layar pemain 2 meluncur dengan dash effect
  - Pemain 1 tembak proyektil → proyektil terlihat & damage Pemain 2 di kedua layar
  - Latency lokal: visual delay < 100 ms; lag spike packet drop: interpolasi tetap smooth tanpa teleport
- **File:** `network/NetworkTransport.java`, `network/PositionUpdate.java`, `network/KryoNetTransport.java`, `network/WebSocketTransport.java`, `LobbyScreen.java`, `NextGenLabGame.java`, `GameWebSocketHandler.java`, `Researcher.java`, `Monster.java`, `PreparationState.java`, `DuelState.java`, `Backend/build.gradle`, `Game/core/build.gradle`

#### **S8 — Auth & User module** ✅ DONE (2026-05-02)

> **Catatan implementasi:**
> - `BCryptPasswordEncoder` + JWT HMAC-SHA384 (jjwt 0.12.6, key Base64 di `application.properties`, expiry 24 jam).
> - `SecurityConfig` stateless; permitAll: `/api/status`, `/api/auth/register`, `/api/auth/login`, `/ws/**`. Authenticated: `/api/match/**`, `/api/room/**`, `/api/auth/me`.
> - `JwtFilter extends OncePerRequestFilter` — parse `Authorization: Bearer <token>` → set `UsernamePasswordAuthenticationToken` dengan `userId` (Long) sebagai principal.
> - `BackendFacade` simpan `authToken`, `currentUserId`, `currentUsername`, `currentEmail` in-memory; helper `post()`/`get()` auto-inject `Authorization` header bila token ada.
> - `AuthScreen` Scene2D dengan tab LOGIN/REGISTER, validasi password ≥ 6 karakter di sisi client; error dari backend ditampilkan di label merah.
> - `MenuScreen` punya `[L]` toggle login/logout; `LobbyScreen` gating: Create/Join Room minta login, Solo Offline tetap jalan tanpa login (dilakukan refactor `startOffline()` agar tidak panggil `/api/match/start` yang sekarang protected).
> - **AuthFacade tidak dibuat terpisah** — auth logic langsung di `BackendFacade` (lebih sederhana, hindari duplikasi state).
>
> **Hotfix 2026-05-02 — GameOverScreen:** ESC dulu nutup app via default LibGDX behavior. Sekarang **ENTER** → `LobbyScreen` (main lagi), **ESC** → `MenuScreen` (menu utama); kedua jalur memanggil `game.resetSession()` untuk close WebSocket transport sebelum pindah screen.
>
> **Smoke test passed:** register baru, login dengan email/password, GET /me return UserDTO, /api/match/start tanpa token → 403, dengan token valid → 200.
>
> **File:** `User.java`, `UserRepository.java`, `AuthService.java`, `AuthController.java`, `SecurityConfig.java`, `JwtUtil.java`, `JwtFilter.java`, 4 DTO (`RegisterRequest`, `LoginRequest`, `UserDTO`, `AuthResponse`), `BackendFacade.java` (rewrite), `AuthScreen.java`, `MenuScreen.java`, `LobbyScreen.java`, `GameOverScreen.java`, `application.properties`, `Backend/build.gradle`.

#### **S9 — Map multi-room + 5 lab task** ✅ DONE (2026-05-11)

> **Catatan implementasi (2026-04 – 2026-05-11):**
> - **5 task type selesai:** WireTask (redesign: circuit grid 3×3 dengan `CircuitNodeActor`, STRAIGHT-symmetry fix, initRot pre-solve fix, EndpointActor alignment fix), ReactorTask (dual Slider drift + hold-zone 3s), ChemicalMixTask (`SwingCursorActor` + DROP timing), ServerHackTask (hex matrix 8×6, click sequence), BiometricLockTask (3 `FingerprintLayerActor` rotate, angle reachability fix). Masing-masing +20% serum.
> - **`lab.tmx` selesai:** multi-room layout dengan Object Layer `Spawn` (`kind=taskZone`). `PreparationState.loadTaskZones()` membaca zona dari map; 5 task triggerable di lokasi masing-masing. Diverifikasi bekerja penuh 2026-05-11.
> - **Combat Preparation:** Monster AI (`ChasePlayerStrategy`) + Monster player — **LMB = melee, RMB = ranged**. Researcher — **RMB = ranged**. Animasi `Basic_Melee_Attack-3524e0a3` (Monster) dan `Basic_Ranged_Attack-7127fbe1` (Researcher) terintegrasi. Aset format baru: individual PNG per frame (bukan spritesheet).
> - **HP system:** `TILE_SIZE` diupdate ke 32px; death-check di Preparation (HP=0 → `GameOverScreen` langsung/sudden-death); HP **carry-over** ke Duel — tidak di-reset. `MAP_BOUND` diupdate ke 1280f di `Projectile.java`.
> - **HUD:** health bar (cyan Researcher + merah Monster) di Preparation dan Duel. Task progress bar di top-left selama Preparation.
> - **FLAG_HIT multiplayer sync:** melee hit langsung di-flush via `transport.sendPosition()` tanpa tunggu `netSendTimer` (fix 2026-05-11: killing blow tidak terkirim karena client switch ke `GameOverScreen` sebelum timer tick berikutnya).
> - **TaskUiTheme:** palette + factory texture terpusat untuk semua task UI.
>
> **File dimodifikasi/dibuat:** `assets/maps/lab.tmx`, `Researcher.java`, `Monster.java`, `Projectile.java`, `task/TaskUiTheme.java`, `task/LabTask.java`, `task/WireTask.java`, `task/ReactorTask.java`, `task/ChemicalMixTask.java`, `task/ServerHackTask.java`, `task/BiometricLockTask.java`, `state/PreparationState.java`, `state/DuelState.java`, `ui/HudOverlay.java`, `command/InputHandler.java`.

#### **S10 — NPC Guards + Stamina system** ✅ DONE (2026-05-16)

> **Catatan implementasi:**
> - `entity/Guard.java`: HP 2, patrol waypoint 2 titik, AlertChase saat LOS ke Monster. Field `lastSeenX/lastSeenY` disimpan saat LOS clear — guard mengejar posisi terakhir Monster saat LOS terputus (bukan posisi real-time), mencegah "tembus tembok" behavior.
> - `strategy/GuardPatrolStrategy.java`: bolak-balik antara `startX/Y` ↔ `patrolTargetX/Y`, kecepatan 60 unit/s.
> - `strategy/AlertChaseStrategy.java`: chase Monster dengan kecepatan 90 unit/s. `hasLOS()` menggunakan DDA half-tile step raycast cek tile `Foreground` — wall-aware. Deaggro setelah 3 detik (DEAGGRO_DELAY) jika Monster tidak kelihatan.
> - Guard bisa die (HP → 0), respawn terjadwal (RESPAWN_DELAY = 20s). Respawn sync via `guardRespawnEvent` encoding di `PositionUpdate` (Monster client authoritative).
> - Guard projectiles: `ProjectilePool` ORANGE, sync ke Researcher client via `weaponType = "GUARD"` di `ProjectileSpawn`. Bullet dihancurkan saat masuk tile `Foreground` (`isBulletInWall()` di `PreparationState`).
> - **Guard AI decoy override:** jika ada `DecoyObject` aktif di `screen.decoys`, guard AI target posisi decoy bukan Monster.
> - **Stamina system:** `Researcher` punya field `stamina`, `maxStamina = 100`, drain 30/s saat sprint (Shift), regen 15/s setelah delay 1s stop sprint. `StaminaBar.java` render bar kuning di HUD.
>
> **File dibuat/dimodifikasi:** `entity/Guard.java`, `strategy/GuardPatrolStrategy.java`, `strategy/AlertChaseStrategy.java`, `factory/EntityFactory.java` (createGuard), `ui/StaminaBar.java`, `ui/HudOverlay.java`, `entity/Researcher.java` (stamina), `state/PreparationState.java` (guard AI, wall bullet), `screen/GameScreen.java` (guards list, guardSpawnX/Y, spawnInitialGuards), `network/ProjectileSpawn.java` (weaponType field), `network/WebSocketTransport.java` (weaponType serialize).

#### **S11 — Resource + Crafting + Inventory (Visual Phase 1: Iconic)** ✅ DONE (2026-05-16)

> **Catatan implementasi:**
> - **Data model:** `crafting/Resource.java` (enum 5 item + weighted random), `crafting/ItemType.java` (8 item enum: isWeapon flag, iconPath), `crafting/Item.java` (wraps ItemType + consumed flag), `crafting/Recipe.java` (canCraft check), `crafting/RecipeLibrary.java` (8 static recipe).
> - **Chest:** `entity/Chest.java` menggunakan `TextureRegion` dari `tileset_lab.png` — GID 31 (`chest_closed`: col 6, row 3 = pixel 192,96) dan GID 32 (`chest_open`: col 7, row 3 = pixel 224,96). Tileset texture di-share static (ref-counted) agar tidak load 12× untuk 12 chest. Chest sync ke Monster client via `"RES_CHEST"` event di `ProjectileSpawn`.
> - **Utility objects:** `entity/TrapObject.java` (TRIGGER_RADIUS 20px, applyStun 3s), `entity/DecoyObject.java` (DURATION 5s, blink di detik terakhir).
> - **Monster status effects:** `applyStun(float)`, `applySlow(float)`, `getSlowFactor()` (0.5× saat slow), `updateStatusEffects(delta)`. `applyMovement()` dan `applyVelocity()` mengalikan speed dengan `getSlowFactor()`.
> - **Researcher inventory:** `EnumMap<Resource, Integer>`, `equippedWeapon/equippedUtility`, `railGunCharge`, `addResource()`, `consumeIngredients()`, `equipItem()`, `heal()`.
> - **UI:** `InventoryPanel.java` (Scene2D, Stage, I key toggle) + `CraftingPanel.java` (Scene2D, 8 recipe button, Craft → consume + equip). Programmatic `TextButtonStyle` tanpa atlas/skin.
> - **HUD weapon slots:** `HudOverlay.renderWeaponSlots()` — 2 slot 48×48px di pojok kanan bawah (slotY = 86px dari bawah, di atas label Monster HP). Icon dari file `items/icon_*.png`, fallback warna jika tidak ada file.
> - **Weapon effects (semua berfungsi):** Pistol (proyektil YELLOW 420f, dmg 1), StunGun (proyektil 350f, applyStun 2s), AcidGrenade (proyektil 250f, splash 48px, dmg 2×), Taser (instant range 80px — multiplayer: kirim `"RES_TASER"` event, Monster client apply damage/slow; singleplayer: lokal), RailGun (hold LMB 0.8s, auto-fire 800f, dmg 3×), HealKit (F, heal +1, consumed), Trap (F, TrapObject di posisi researcher, max 2 — multiplayer: sync `"RES_TRAP"` ke Monster client), Decoy (F, DecoyObject di mouse position — multiplayer: sync `"RES_DECOY"` ke Monster client).
> - **Multiplayer sync:** `equippedItemOrdinal` di `PositionUpdate` (Researcher → Monster client). `"RES_CHEST"`, `"RES_TASER"`, `"RES_TRAP"`, `"RES_DECOY"` via `ProjectileSpawn.weaponType`.
> - **Spawn:** 12 chest deterministik dari `matchId ^ 0xC4E57L` seed (kedua client hasilkan set yang sama). Workshop zone dari object `"workshop"` di Spawn layer `lab.tmx`.
> - **Aset:** 9 sprite 32×32px via Pixellab MCP API (8 item icon + chest.png) di `assets/items/`.
>
> **File dibuat:** `crafting/Resource.java`, `crafting/ItemType.java`, `crafting/Item.java`, `crafting/Recipe.java`, `crafting/RecipeLibrary.java`, `entity/Chest.java`, `entity/TrapObject.java`, `entity/DecoyObject.java`, `ui/InventoryPanel.java`, `ui/CraftingPanel.java`, `assets/items/icon_pistol.png` (+ 7 lainnya), `assets/items/chest.png`.
> **File dimodifikasi:** `entity/Researcher.java`, `entity/Monster.java`, `ui/HudOverlay.java`, `factory/EntityFactory.java`, `screen/GameScreen.java`, `state/PreparationState.java`, `network/PositionUpdate.java` (equippedItemOrdinal), `network/WebSocketTransport.java` (serialize equippedItemOrdinal).

#### **S12 — Evolution Genes + Sabotage + Monster Dash** ✅ DONE (2026-05-17)

> **Catatan implementasi:**
> - **XP & Level system:** Monster dapat 25 XP per guard kill; 5 level dengan threshold ×1.5 per level (100→150→225→337→506); `Monster.addXp()` return true = level up trigger.
> - **LevelUpScreen:** Scene2D card-style; 3 gene dipilih acak dari yang belum dipilih; setiap card punya PixelLab icon (48×48) + nama + deskripsi; klik card = pilih; Monster-pause saat pilih, Peneliti bebas gerak (hanya dapat notifikasi HUD).
> - **10 Gene aktif:** PREDATOR_CLAWS (+1 melee dmg), ADRENAL_SURGE (speed ×1.2), THICK_HIDE (+1 maxHP + sync via PositionUpdate.maxHp), FRENZY (dash CD −1s), ECHOLOCATION (highlight Peneliti 3s + off-screen arrow, Q, 30s CD), ACIDIC_BLOOD (return-fire projectile saat kena hit), REGENERATION (+0.5 HP/s out of combat), TOXIC_AURA (slow Peneliti 64px range), PHASE_SHIFT (tembus tembok 0.4s, G, 20s CD), BERSERKER (+30% dmg saat HP<2).
> - **Dash:** Shift = dash 96px / 0.16s / DASH_SPEED=600; cooldown 4s (3s dengan FRENZY); bar + PixelLab icon (40×40) di HUD Monster; barY=50 agar tidak overlap HP bar.
> - **Sabotage panels:** 3 panel di lab.tmx (sabotage_lights/slow/drain); E = interact saat dekat; efek via GameEvent WebSocket: SAB_LIGHTS (gelap 10s Peneliti), SAB_SLOW (slowTimer 8s), SAB_DRAIN (tasksCompleted−1 + dispose+recreate LabTask agar tidak instant-complete).
> - **Gene panel HUD:** icon kolom kanan layar Monster; cooldown ditampilkan sebagai pie-arc hitam semi-transparan searah jarum jam + teks "Xs"; hanya ECHOLOCATION & PHASE_SHIFT punya cooldown.
> - **XP bar sync ke Peneliti:** `PositionUpdate.monsterXp + monsterLevel` baru; Peneliti render bar yang sama (200×18 px, top-right, desain mirip task progress bar); single-player langsung baca dari Monster object.
> - **Bugs fixed:** SAB_DRAIN sync (pollPosition selalu sync tasksCompleted); task redo setelah drain (dispose+createTask); "Peneliti" label dipindah di atas "Stamina"; stamina/weapon-slot Peneliti tidak tampil di sisi Monster.
> - **PixelLab assets:** 11 gambar (10 gene 48×48 + dash_icon 32×32) di `assets/evolution/`.

> **File dibuat:** `network/GameEvent.java`, `entity/SabotagePanel.java`, `screen/LevelUpScreen.java`, `assets/evolution/*.png` (11 file).
> **File dimodifikasi:** `entity/Monster.java` (XP/level/gene/dash/echolocation/phase-shift), `entity/Researcher.java` (slowTimer), `factory/EntityFactory.java` (createSabotagePanel), `screen/GameScreen.java` (sabotagePanels, layer visibility), `state/PreparationState.java` (XP award, level-up, gene input, sabotage E, handleGameEvent, echolocation arrow, XP sync), `ui/HudOverlay.java` (renderXpBar redesign, renderDashCooldown, renderGenePanel, stamina hide Monster), `network/PositionUpdate.java` (+monsterXp/monsterLevel), `network/WebSocketTransport.java` (serialize monsterXp/Level).

- ~~Backend: buat `EvolutionGene` entity + repository + service + endpoint `/api/evolution-genes`~~
- ~~Backend: `DataSeeder.java` seed 10 gene saat startup~~
- Frontend: `Monster.java` tambah `int xp, level`, `List<Gene> activeGenes`
- `screen/LevelUpScreen.java`: muncul saat level up, pilih 1 dari 3 gene acak
- Apply efek gene (modifier ke movement speed, damage, etc.)
- `entity/SabotagePanel.java`: 3 panel di map, 3 jenis sabotage
- **Verifikasi:** Monster bunuh guard 4x → level up screen muncul → pilih gene → efek terasa; sabotage panel functional
- **File:** `EvolutionGene.java` (entity), `EvolutionGeneRepository.java`, `EvolutionLibraryService.java`, `DataSeeder.java`, `Monster.java`, `LevelUpScreen.java`, `SabotagePanel.java`

#### **S13 — Arena Duel + Ammo System + Utility Stock + Combat Balance** ✅ DONE (2026-05-17)

> **Catatan implementasi:**
> - **Duel Arena:** `assets/maps/arena.tmx` (arena terbuka 20×20 tile). `GameScreen.switchMap()` dipanggil di `DuelState.enter()` — dispose map lab, load arena, reposisi entitas. Prep-winner buff: Researcher menang prep → +3 HP saat masuk duel; Monster menang prep → +2 HP.
> - **Match timer:** `duelTimerLocal = 180f` (3 menit) di Duel; `prepTimerLocal` di Preparation; keduanya render via `hud.renderMatchTimer()`. Tie-break via HP saat timer habis.
> - **Ammo system (kedua fase):** Per-weapon capacity: Pistol=12, StunGun=8, Taser=5, RailGun=4, AcidGrenade=3, tanpa senjata=0. Auto-reload saat ammo=0; R key manual reload (2s). `maxAmmoFor(null)=0` sehingga tanpa senjata tampil 0/0. Equip senjata baru → ammo langsung penuh (reset ke maxAmmo). HUD `renderAmmo()` di kedua fase.
> - **Utility stock system:** Ganti single `Item equippedUtility` → `Map<ItemType,Integer> utilityStock` + `ItemType selectedUtilityType` di `Researcher`. `equipItem()` merge stok; `consumeEquippedUtility()` decrement + auto-pilih next jika habis; `getEquippedUtility()` return transient Item dari stok.
> - **InventoryPanel interaktif:** `rebuild()` pattern — TextButton per utility type di stok, CYAN highlight untuk yang terpilih, klik → `setSelectedUtilityType()` → rebuild. Mirip pola `CraftingPanel`.
> - **Duel Researcher tembak ke mouse cursor:** Ganti `dirVec(lastDirection)` → `screen.camera.unproject(new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0))` untuk arah tembak.
> - **Monster ranged cooldown:** `monsterRangedTimer` field; RMB ranged di-gate 1.5s antar tembakan di Duel.
> - **Gene abilities di Duel:** `DuelState.applyMonsterInput()` tambah `updateCooldownTimer(delta)`; Shift=Dash, Q=Echolocation, G=PhaseShift berfungsi di Duel. Monster else-branch tambah `updateStatusEffects(delta)`, `updateGeneTimers(delta)`, ToxicAura slow logic.
> - **Utility F key di Duel:** `handleUtilityUseDuel()` — HealKit (+1 HP), Trap (max 2), Decoy (di mouse position); semua consume stok. Trap+decoy render, update, collision di arena.
> - **Bug fixes:** melee cooldown Monster di Duel kini berjalan (updateCooldownTimer); ammo 0/0 tanpa senjata (bukan 6/6); equip senjata baru → ammo penuh (bukan carry-over).
>
> **File dibuat/dimodifikasi:** `entity/Researcher.java` (utility stock system), `ui/InventoryPanel.java` (interaktif rebuild), `state/PreparationState.java` (ammo system lengkap, consumeEquippedUtility, maxAmmoFor helper), `state/DuelState.java` (mouse shoot, ranged CD, gene timers, F key utility, trap/decoy), `assets/maps/arena.tmx`.

#### **S14 — Stats + Match History + ELO + Profile** ✅ DONE (2026-05-18)

> **Catatan implementasi:**
> - **PlayerStats entity:** `player_stats` tabel; field: `userId` (unique), `totalMatches`, `wins`, `losses`, `researcherMatches`, `researcherWins`, `monsterMatches`, `monsterWins`, `elo` (default 1000). `findOrCreate()` pattern di `StatsService`.
> - **MatchHistory entity:** `match_history` tabel; field: `matchSessionId`, `userId`, `role`, `won`, `durationSeconds`, `eloBefore`, `eloAfter`, `playedAt` (Instant). `@UniqueConstraint(match_session_id, user_id)` di level DB.
> - **ELO K-32:** `expected = 1/(1+10^((oppElo-myElo)/400))`, `newElo = myElo + 32*(actual-expected)`. Keduanya dihitung server-side dalam satu transaksi `@Transactional`.
> - **Idempotency tripple-layer:** (1) `@Lock(PESSIMISTIC_WRITE)` di `findByIdForUpdate()` — hanya satu transaction yang dapat lock secara bersamaan; (2) `existsByMatchSessionIdAndUserId()` sebelum `saveHistory()` — application-level guard; (3) `@UniqueConstraint` di DB — backstop final. Mencegah double-count saat kedua client panggil `/api/match/{id}/finish` secara bersamaan.
> - **Durasi match penuh:** `startMs = match.getPrepStartedAt()` (bukan `duelStartedAt`). `RoomService.createRoom()` sekarang juga set `prepStartedAt` — sebelumnya hanya `MatchService.startMatch()` yang set, sehingga room-based match selalu 0s.
> - **researcherUserId / monsterUserId tracking:** `GameController.createRoom()` & `joinRoom()` inject `Authentication auth`, extract `(Long) auth.getPrincipal()`, teruskan ke `RoomService` agar `MatchSession` menyimpan userId kedua pemain.
> - **Ammo reserve system:** Ganti `bonusAmmo` → `ammoReserve` di `Researcher`. Craft senjata menambah `magazineFor(type)` ke reserve; craft `AMMO_PACK` tambah 12 ke reserve. Reload mengambil dari reserve (partial jika reserve < needed); auto-reload dan R-key hanya aktif jika `maxAmmo > 0 && reserve > 0`. HUD tampil `AMMO X/Y  +N`.
> - **ProfileScreen:** async load (dua callback paralel `getStats` + `getMatchHistory`; `synchronized tryRebuild()` post ke GL thread). Tampil ELO, W/L, win rate, per-role stats, tabel 10 match terakhir (tanggal+jam, role, hasil, ELO sebelum, delta ELO, durasi). Bisa dibuka dari `MenuScreen` (`[P]`) dan `LobbyScreen` ("PROFIL SAYA"). ESC callback ke screen asal (constructor menerima `Runnable onBack`).
>
> **File dibuat:** `PlayerStats.java`, `MatchHistory.java`, `PlayerStatsRepository.java`, `MatchHistoryRepository.java`, `StatsService.java`, `StatsController.java`, `FinishMatchRequest.java`, `PlayerStatsDTO.java`, `MatchHistoryDTO.java`, `screen/ProfileScreen.java`.
> **File dimodifikasi:** `MatchSession.java` (+researcherUserId/monsterUserId/winner/finishedAt), `MatchSessionRepository.java` (+findByIdForUpdate PESSIMISTIC_WRITE), `GameController.java` (+Authentication inject), `RoomService.java` (+userId store + prepStartedAt), `SecurityConfig.java` (+/api/stats/**), `BackendFacade.java` (+finishMatch/getStats/getMatchHistory), `GameOverScreen.java` (+finishMatch call), `MenuScreen.java` (+P key profil), `LobbyScreen.java` (+PROFIL SAYA button), `Researcher.java` (bonusAmmo→ammoReserve), `HudOverlay.java` (+ammoReserve param renderAmmo), `PreparationState.java` (+reserve reload logic), `DuelState.java` (+reserve reload logic, maxAmmoFor null→0).

#### **S15 — Leaderboard + Achievement** ✅ DONE (2026-05-18)

> **Catatan implementasi:**
> - 10 achievement di-seed via `DataSeeder.seedAchievements()` (`@EventListener(ApplicationReadyEvent.class)`), guard `count() > 0` mencegah duplikasi
> - `AchievementService.checkAndUnlock(match, userId, stats)` dievaluasi untuk tiap caller setelah `finishMatch` commit — kedua player mendapat notifikasi masing-masing (pessimistic lock tetap menjaga serialisasi)
> - `StatsService.finishMatch()` signature berubah: `List<String> finishMatch(Long matchId, String winner, Long callerUserId)` — return nama achievement yang baru unlock
> - `StatsController.finishMatch()` inject `Authentication`, return `ResponseEntity<List<String>>`
> - `/api/leaderboard` → public (no auth); `/api/achievements/**` → authenticated
> - `LeaderboardService` sort via Spring Data `Sort.by(field).descending()` — 3 kueri: elo, researcherWins, monsterWins
> - `LeaderboardScreen` dan `AchievementScreen`: async load + manual JSON parse (pola ProfileScreen); `Runnable onBack` callback untuk navigasi dari Menu maupun Lobby
> - 10 pixel art icon 64×64 di-generate via PixelLab AI (`create_object` directions=1, n_frames=1), disimpan di `Game/assets/achievements/icon_<code>.png`; ditampilkan 32×32 di `AchievementScreen`
> - `GameOverScreen` popup "ACHIEVEMENT BARU:" muncul saat callback `finishMatch` kembali dengan list non-empty
> - `BackendFacade.finishMatch()` signature berubah ke `finishMatch(Long, String, Consumer<String>)`
>
> **Achievement (10):** FIRST_WIN (wins≥1), VETERAN (totalMatches≥10), MONSTER_HUNTER (researcherWins≥5), LAB_DEFENDER (monsterWins≥5), SPEED_DEMON (won && duration<180s), SURVIVOR (wins≥10), SHARPSHOOTER (researcherMatches≥20), APEX_PREDATOR (monsterMatches≥20), ELO_MASTER (elo≥1200), LEGEND (elo≥1500)
>
> **File dibuat:** `Achievement.java`, `UserAchievement.java`, `AchievementDTO.java`, `LeaderboardEntryDTO.java`, `AchievementRepository.java`, `UserAchievementRepository.java`, `AchievementService.java`, `LeaderboardService.java`, `AchievementController.java`, `LeaderboardController.java`, `LeaderboardScreen.java`, `AchievementScreen.java`, `assets/achievements/icon_*.png` (×10)
>
> **File dimodifikasi:** `StatsService.java`, `StatsController.java`, `SecurityConfig.java`, `DataSeeder.java`, `BackendFacade.java`, `GameOverScreen.java`, `MenuScreen.java` ([L]/[A]), `LobbyScreen.java` (tombol LEADERBOARD+ACHIEVEMENT)

#### **S16 — Friend / invite system + Room Redesign** ✅ DONE (2026-05-19)

> **Catatan implementasi:**
> - **Friendship entity & service:** `Friendship.java` entity dengan `requesterId`, `addresseeId`, `status` ("PENDING"/"ACCEPTED"); `FriendshipRepository` — `findPendingForUser`, `findBetween`, `findByUserIdAndStatus`; `FriendService` (sendRequest, acceptRequest, removeFriend, getMyFriends, getPendingRequests, searchUsers); `FriendController` (/request, /accept/{id}, GET /, /pending, DELETE /{userId}).
> - **FriendScreen.java:** Tiga tab — TEMAN (daftar teman, HAPUS, JOIN jika teman di joinable room), PERMINTAAN (pending requests, TERIMA), CARI (search user, TAMBAH). Bug fix: `parseLongField` untuk ID numerik JSON (bukan `parseStr` yang hanya cocok untuk string berquote).
> - **Room redesign — in-room role selection:** `GameRoom` ditambah field `player2UserId` dan `publicRoom` (boolean, kolom `is_public`). `RoomRequest` dihapus field `role`. Player join/create tanpa role → masuk `RoomScreen` → klaim PENELITI atau MONSTER di dalam room. `RoomService.claimRole()` endpoint baru; `createRoom()` dan `joinRoom()` tanpa role.
> - **Visibilitas room:** Host dapat toggle PUBLIK/PRIVAT dari `RoomScreen`. Backend: `RoomService.setVisibility()` + endpoint `/{code}/visibility`. `getWaitingRooms()` menggunakan native SQL query (`findPublicWaitingRooms`) untuk filter `is_public = true` — menghindari ambiguitas JPA field naming `isXxx` yang menyebabkan Jackson serialisasi sebagai `"public"` bukan `"isPublic"`.
> - **Kick player:** `RoomService.kickPlayer()` + endpoint `/{code}/kick/{userId}`. Kick detection di `RoomScreen` menggunakan flag `wasInRoom` + perbandingan `getCurrentUsername()` vs username di respons poll; jika was-in-room dan nama tidak ditemukan di room → `buildKickedUI()` + timer 3s redirect ke `LobbyScreen`.
> - **Public room list auto-refresh:** `PublicRoomListScreen` polling setiap 3 detik dengan `fetching` guard mencegah concurrent request; `active` flag mencegah update setelah screen di-hide.
> - **GameRoom JPA fix:** Field `isPublic` di-rename ke `publicRoom` (getter tetap `isPublic()`) untuk menghindari ambiguitas Jackson serialisasi yang menyebabkan `is_public` kolom selalu dibaca sebagai `false`.
> - **PlayModeScreen simplification:** Hapus fase `HOST_ROLE`; create room langsung pilih PUBLIK/PRIVAT, lalu masuk `RoomScreen` tanpa memilih role di awal.
> - **FriendService inject GameRoomRepository:** `getMyFriends()` menyertakan `roomCode` jika teman punya joinable room (host user && player2 null && status WAITING). `UserSummaryDTO` ditambah field `roomCode`.
>
> **File dibuat:** `Friendship.java`, `FriendshipRepository.java`, `FriendService.java`, `FriendController.java`, `screen/FriendScreen.java`.
> **File dimodifikasi:** `GameRoom.java` (+player2UserId, +publicRoom), `GameRoomRepository.java` (+findJoinableRoomByHost, +findPublicWaitingRooms native query), `RoomService.java` (rewrite createRoom/joinRoom, +claimRole/kickPlayer/setVisibility, fix getWaitingRooms), `GameController.java` (+/claim, /visibility, /kick/{userId}, update toDetailMap), `SecurityConfig.java` (+/api/friends/**), `UserSummaryDTO.java` (+roomCode), `PublicRoomDTO.java` (+playerCount, hapus neededRole), `RoomRequest.java` (hapus role), `BackendFacade.java` (+claimRole/setVisibility/kickPlayer, update createRoom/joinRoom signatures), `screen/RoomScreen.java` (rewrite: in-room role claim UI, visibility, kick, kick detection, wasInRoom flag), `screen/FriendScreen.java` (bug fix parseLongField, JOIN button di TEMAN tab), `screen/PlayModeScreen.java` (hapus HOST_ROLE phase, buildHostPublicUI), `screen/PublicRoomListScreen.java` (auto-refresh 3s, fetching guard, active flag).

#### **S17 — Settings menu + Observer pattern + Responsive UI** ✅ DONE (2026-05-19)

> **Catatan implementasi:**
>
> **Observer / EventBus (design pattern ke-9):** `event/EventBus.java` — Singleton dengan `Map<Class<?>, List<GameEventListener<?>>>`. Interface `event/GameEventListener<E>` `@FunctionalInterface`. 5 event POJO:
> - `OnTaskCompleted(int totalCompleted)` — publish di `PreparationState` setelah `tasksCompleted++`
> - `OnSerumProgress(int progress)` — publish di `PreparationState` setelah sync progress dari server
> - `OnMonsterLevelUp(String gene)` — publish di `PreparationState` setelah `monster.applyGene(gene)`
> - `OnPlayerHit(String role, int damage)` — publish di `PreparationState` saat researcher kena hit
> - `OnGuardKilled()` — publish di `PreparationState` saat guard HP = 0
>
> `GameScreen` subscribe semua 5 event di `show()` (log ke console); unsubscribe di `dispose()`. Subscriber menyimpan referensi listener sebagai field agar unsubscribe akurat. S18 akan ganti log dengan `AudioFacade.playSound(...)`.
>
> **SettingsScreen:** `Gdx.app.getPreferences("nextgenlab")` — prefs key: `"volume"` (float), `"server"` (String), `"fullscreen"` (boolean). UI: slider volume + TextField server + toggle FULLSCREEN/WINDOWED + [SIMPAN] + [KELUAR AKUN] + [KEMBALI]. `applySettings()` di `NextGenLabGame.create()` baca prefs, set `serverHost`, reinit `BackendFacade`, toggle fullscreen. Tombol [SETELAN] di `LobbyScreen`. Bug fix: state fullscreen dibaca dari `Gdx.graphics.isFullscreen()` saat `show()`, bukan dari prefs saat setiap rebuild — sehingga label toggle akurat sebelum disimpan.
>
> **Responsive UI — FitViewport(1280, 720):** Semua 12 UI screen dimigrasi dari `ScreenViewport` ke `FitViewport(1280, 720)`. Window default `Lwjgl3Launcher`: 640×480 → 1280×720. Setiap screen punya `resize()` dengan `stage.getViewport().update(w, h, true)`. Scale up button & font:
>
> | Elemen | Lama | Baru |
> |--------|------|------|
> | `solidTex(w, h)` button | 260×44 | 380×60 |
> | Main action buttons | `width(220).height(44)` | `width(300).height(55)` |
> | Claim role / secondary | `width(160).height(36)` | `width(220).height(48)` |
> | Small / kick buttons | `width(55).height(28)` | `width(80).height(36)` |
> | Font scale title | `2f` | `3f` |
> | Font scale label | `1f` (default) | `1.5f` (`font.getData().setScale(1.5f)`) |
> | Font scale room code | `1.8f` | `2.5f` |
>
> **File dibuat:** `event/EventBus.java`, `event/GameEventListener.java`, `event/OnTaskCompleted.java`, `event/OnSerumProgress.java`, `event/OnMonsterLevelUp.java`, `event/OnPlayerHit.java`, `event/OnGuardKilled.java`, `screen/SettingsScreen.java`.
>
> **File dimodifikasi:** `NextGenLabGame.java` (+applySettings()), `lwjgl3/Lwjgl3Launcher.java` (640×480 → 1280×720), `state/PreparationState.java` (5× publish EventBus), `screen/GameScreen.java` (subscribe/unsubscribe 5 listener), `screen/LobbyScreen.java` (+tombol [SETELAN], scale), semua 11 UI screen bestehend (`MenuScreen`, `AuthScreen`, `PlayModeScreen`, `RoomScreen`, `PublicRoomListScreen`, `FriendScreen`, `ProfileScreen`, `LeaderboardScreen`, `AchievementScreen`, `LevelUpScreen`) — ScreenViewport → FitViewport + font scale + button scale.

#### **S18 — Audio integration**
- Download aset CC0 sesuai list di §11.2
- Buat `facade/AudioFacade.java`
- Subscribe EventBus untuk play sfx pada event
- Background music switch otomatis: menu → bgm_menu, prep → bgm_lab_ambient, duel → bgm_duel
- **Verifikasi:** semua sfx terdengar, volume slider berpengaruh, alarm di 80% serum trigger
- **File:** `AudioFacade.java`, `assets/audio/*`, hook ke `MenuScreen`, `GameScreen`

#### **S19 — Narrative — intro/outro + dialog popup**
- Buat `screen/StoryScreen.java` (intro/outro text-based dengan typewriter effect)
- Backstory di intro (Project Apex, spesimen 10-X memberontak)
- Outro by winner: 3 variasi text (Researcher win, Monster win, draw)
- `ui/DialogPopup.java`: muncul saat in-match event (alarm 80%, monster spotted, dll.)
- **Verifikasi:** intro saat new match start, outro saat game over, dialog popup trigger
- **File:** `StoryScreen.java`, `DialogPopup.java`, hook di `GameScreen`, `GameOverScreen`

#### **S20 — Server address selector** *(dijadwalkan sebelum web build S21 — persiapan deployment)*
- `LobbyScreen.java`: tambah fase `NETWORK_SELECT` (Localhost / LAN / Internet)
- Input TextField IP untuk LAN/Internet
- `NextGenLabGame.java`: field `String serverHost` (default "localhost")
- `BackendFacade` & `NetworkTransport`: terima host via constructor
- Bonus UX: saat host LAN, tampilkan local IP via `InetAddress.getLocalHost()`
- **Verifikasi:** dua mesin di LAN bisa konek dengan IP host yang diinput
- **File:** `LobbyScreen.java`, `NextGenLabGame.java`, `BackendFacade.java`, transport classes

#### **S21 — TeaVM web build**
- Tambah module `Game/teavm/` dengan `build.gradle` (TeaVM plugin)
- Buat `TeaVMLauncher.java` (mirip Lwjgl3Launcher)
- Disable KryoNet di web build (compile-time flag atau interface fallback)
- Test build: `./gradlew teavm:build` → output static HTML+JS
- Setup CORS di Spring Boot agar web client bisa akses API
- Deploy ke itch.io: zip output TeaVM, enable "playable in browser"
- **Verifikasi:** game bisa dimainkan di browser dari itch.io URL, multiplayer WebSocket working
- **File:** `Game/teavm/build.gradle`, `TeaVMLauncher.java`, `CorsConfig.java` backend

#### **S22 — 4v1 expansion (bonus)**
- `LobbyScreen.java`: ubah dari 2-slot ke 5-slot (4 Researcher + 1 Monster)
- Backend: `RoomService` allow up to 5 players per room
- `PositionUpdate` jadi `MultiPositionUpdate` (set of positions per role)
- WebSocket broadcast: server forward semua player position ke semua client
- Balancing: Monster HP buff (5 → 8), guard count tambah (4 → 8)
- **Verifikasi:** 5 instance konek, semua bisa lihat posisi masing-masing, gameplay seimbang
- **File:** `LobbyScreen.java`, `RoomService.java`, `GameWebSocketHandler.java`, `PositionUpdate.java`, balance value adjustment

#### **S23 — Polish + cutscene 2D + Visual Senjata Phase 2 (Layered)**
- Cutscene intro: pixel art 5–7 frame slideshow (fade in/out)
- Cutscene outro per winner
- Particle effect: hit spark, dash trail, sabotage explosion (LibGDX `ParticleEffect`)
- Screen shake on hit (camera offset random kecil)
- **Visual senjata Phase 2 (Layered Rendering)** — upgrade dari Phase 1 iconic:
  - Buat 5 sprite senjata kecil (16–24 px) × 4 varian arah (N/S/E/W) = **20 sprite**
  - `Researcher.java`: tambah `Vector2 handOffset(Direction dir, int frameIndex)` — return posisi tangan untuk arah & frame anim tertentu
  - `Researcher.render()`: render base sprite, lalu render weapon sprite di posisi `position + handOffset`, dengan rotasi/flip sesuai direction
  - Sinkronisasi multiplayer: tambah field `byte equippedWeaponId` di `PositionUpdate` agar peer juga bisa render senjata yang dipegang lawan
- Balancing pass: playtest 5–10 match, adjust value
- **File:** assets cutscene baru, `assets/items/weapon_sprite_*.png` (20 sprite), `Researcher.java`, `PositionUpdate.java` (extend), `StoryScreen.java`, particle integration di entity

#### **S24 — Testing & deploy**
- End-to-end test scenario:
  1. Register → Login → Lobby Create → Lobby Join (peer) → Match → Finish → Stats updated → Logout
  2. Achievement unlock flow
  3. Friend add & invite
  4. Web build itch.io load + play
- Buat README.md komprehensif (setup, run, screenshot, design pattern table)
- Demo video 3–5 menit (gameplay walkthrough + tour pattern di code)
- itch.io page: cover image, description, controls, tags
- Backend deploy: opsi Railway/Render free tier (bonus)
- **File:** README.md, demo video, itch.io setup

---

## 13. Verification per Sprint

Untuk **setiap sprint**, jalankan & dokumentasikan langkah-langkah berikut. Hasilnya dicatat di section "Verifikasi" tiap sprint di file `PROJECT_STATUS.md`.

### 13.1 Build Check

```powershell
# Game build
cd Game
.\gradlew build                   # harus sukses, exit code 0
.\gradlew lwjgl3:run              # smoke test golden path

# Backend build
cd ..\Backend
.\gradlew build                   # harus sukses + test pass
.\gradlew bootRun                 # cek http://localhost:8080/api/status return 200

# DB verifikasi
psql -U postgres -d nextgenlab_db -c "\dt"   # lihat tabel
psql -U postgres -d nextgenlab_db -c "SELECT * FROM users LIMIT 5;"

# Web build (mulai S21)
cd ..\Game
.\gradlew teavm:build
# serve build/teavm/dist dan buka di browser http://localhost:8000
```

### 13.2 Manual Test Golden Path (per sprint)

Format: setiap sprint punya checklist 3–5 langkah test yang harus pass sebelum commit.

**Contoh template (per sprint):**
- [ ] Build `Game` sukses tanpa error
- [ ] Build `Backend` sukses tanpa error  
- [ ] Game bisa run end-to-end (menu → match → game over)
- [ ] Fitur baru sprint berfungsi sesuai spec
- [ ] Tidak ada exception/crash di console saat play 5 menit
- [ ] Commit dengan pesan format `feat: <fitur>` atau `refactor: <area>`

### 13.3 Continuous Integration (Bonus)

Setelah S8 (auth), bisa setup GitHub Actions:
- Trigger: push ke main
- Job 1: `./gradlew build` Game
- Job 2: `./gradlew test` Backend
- Optional: deploy backend Railway saat tag `v*`

---

## 14. Risiko & Mitigasi

| # | Risiko | Probabilitas | Dampak | Mitigasi |
|---|--------|--------------|--------|----------|
| 1 | Sinkronisasi WebSocket lag di internet | Medium | Medium | Fallback ke KryoNet via abstraksi `NetworkTransport`; client-side prediction; reduce broadcast freq dari 20 Hz ke 10 Hz |
| 2 | Kompleksitas TeaVM web build (KryoNet/Spring incompatible) | Medium | Medium | Compile-time flag disable KryoNet di module teavm; backend tetap server-side, web client hanya konsumen API |
| 3 | Balancing 4v1 sulit | High | Low (bonus) | Implementasi 1v1 dulu (S7–S20), 4v1 ditunda ke S22; jika tidak sempat, tetap dapat nilai wajib |
| 4 | Aset audio CC0 tidak fit theme | Low | Low | Iterasi pilih, jangan investasi besar di S19; default hanya BGM + 5 SFX wajib |
| 5 | Scope > 2 bulan tetap molor | Medium | Medium | Sprint S22+ dilabel "bonus" — bisa di-skip. Prioritas: S7–S20 (wajib) > S21 (itch.io bonus) > S22+ (extra bonus) |
| 6 | Bug regresi saat refactor besar | Medium | High | Smoke test wajib per sprint; commit per sprint; gunakan branch git per sprint |
| 7 | DB migration error (ddl-auto=update) | Low | High | Backup DB sebelum sprint dengan schema change besar (S8, S15, S16); fallback `ddl-auto=create` di dev |
| 8 | Konflik dependency Spring/JJWT/WebSocket | Low | Medium | Lock versi Spring Boot 4.0.6, JJWT 0.12.x, gunakan `dependencyManagement` |
| 9 | Latency LAN buruk di jaringan congested | Medium | Low | Server address selector mendukung Localhost untuk demo lokal jika LAN buruk |
| 10 | Itch.io reject build (asset license) | Low | Medium | Pastikan semua aset CC0/CC-BY dengan kredit di README |

---

## 15. Deliverables Final

### 15.1 Yang Akan Disubmit

1. **Repo GitHub publik** dengan:
   - README lengkap (setup, cara run, screenshot, design pattern explanation table, kredit aset)
   - Branch `main` stable
   - Tag release `v1.0` saat S24 selesai

2. **Build executable:**
   - `Game/lwjgl3/build/libs/NextGenLab-desktop-v1.0.jar` (desktop fat JAR)
   - `Game/teavm/build/dist/` (TeaVM web build untuk itch.io)

3. **Page itch.io publik:**
   - URL: `https://<username>.itch.io/nextgenlab`
   - Cover image, screenshot, description, controls
   - Embed game playable di browser
   - Tags: `asymmetric`, `multiplayer`, `pixel-art`, `sci-fi`, `horror`

4. **PROJECT_PLAN.md** (file ini)

5. **PROJECT_STATUS.md** (snapshot kondisi sprint terakhir, di-update tiap sprint)

6. **Demo video 3–5 menit:**
   - Gameplay walkthrough (1 match Researcher win + 1 Monster win)
   - Tour singkat code untuk showcase 9 design pattern
   - Upload YouTube unlisted, link di README

7. **Backend deployed (bonus):**
   - Lokal cukup; bonus jika di-host di Railway/Render free tier
   - Pastikan PostgreSQL tetap accessible

### 15.2 Folder Struktur Final yang Diharapkan

```
NextGenLab_Project/
├── Backend/                 (Spring Boot)
├── Game/
│   ├── core/                (logic shared)
│   ├── lwjgl3/              (desktop launcher)
│   └── teavm/               (web launcher — S21)
├── docs/                    (NEW: GDD, screenshot, demo video link)
├── PROJECT_PLAN.md          (file ini)
├── PROJECT_STATUS.md        (snapshot terkini)
├── README.md                (lengkap dengan setup + screenshot)
├── CLAUDE.md                (sudah ada — guide untuk Claude AI assistant)
├── LICENSE                  (MIT atau Apache 2.0)
└── .github/workflows/       (CI — bonus)
```

---

## Appendix A: Checklist Eksekusi Awal

Setelah dokumen ini disetujui, langkah pertama adalah:

1. ✅ Save PROJECT_PLAN.md ke root proyek (file ini)
2. ✅ Update `PROJECT_STATUS.md` agar referensi ke file ini
3. ✅ Sprint **S7 (Network abstraction + WebSocket)** — DONE 2026-05-02
4. ✅ Sprint **S8 (Auth & User module)** — DONE 2026-05-02
5. ✅ Sprint **S9 (Map multi-room + 5 lab task)** — DONE 2026-05-11
6. ✅ Sprint **S10 (NPC Guards + Stamina)** — DONE 2026-05-16
7. ✅ Sprint **S11 (Resource + Crafting + Inventory)** — DONE 2026-05-16
8. ✅ Sprint **S12 (Evolution Genes + Sabotage + Monster Dash)** — DONE 2026-05-17
9. ✅ Sprint **S13 (Arena Duel + Ammo + Utility Stock + Combat Balance)** — DONE 2026-05-17
10. ✅ Sprint **S14 (Stats + Match History + ELO + Profile)** — DONE 2026-05-18
11. ✅ Sprint **S15 (Leaderboard + Achievement)** — DONE 2026-05-18
12. ✅ Sprint **S16 (Friend System + Room Redesign)** — DONE 2026-05-19
13. ✅ Sprint **S17 (EventBus + SettingsScreen + Responsive UI)** — DONE 2026-05-19
14. 🟡 Sprint **S18 (Audio)** — NEXT
11. Setiap sprint diakhiri:
   - Run verifikasi (§13)
   - Update `PROJECT_STATUS.md` Section "Sprint Log" (centang sprint, tulis catatan)
   - Commit + push GitHub
   - Update bagian Section 12 di file ini (centang sprint)

## Appendix B: Catatan Living Document

Dokumen ini adalah **dokumen hidup**. Update saat:
- Sprint selesai → update tabel status §12
- Keputusan baru muncul → tambah ke "Keputusan yang Sudah Dikonfirmasi"
- Risiko baru muncul → tambah ke §14
- Scope berubah → revisi §6 dan §12

Versi terakhir disimpan di git history. Tag release di GitHub menandai milestone besar (v1.0 = full release).

---

*Dokumen ini dibuat 2026-05-01 sebagai panduan implementasi NextGenLab: Science vs Instinct untuk seleksi Oprec Netlab 2026 — Divisi Game Dev. Dipersembahkan untuk fokus implementasi yang konsisten dari awal sampai akhir proyek.*

*Update terakhir: 2026-05-19 — S17 complete (EventBus Observer pattern + SettingsScreen dengan volume/server/fullscreen persist + FitViewport(1280,720) migrasi 12 UI screen + window default 1280×720 + bug fix fullscreen toggle). S18 (Audio integration) berikutnya.*