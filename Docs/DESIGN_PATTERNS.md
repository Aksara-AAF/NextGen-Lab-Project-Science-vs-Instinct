# Design Patterns — NextGenLab: Science vs Instinct

Dokumen ini menjelaskan 8 design pattern yang diimplementasikan dalam proyek NextGenLab beserta lokasi kode dan alasan penggunaannya.

---

## 1. Facade Pattern

**Lokasi:** `Game/core/src/main/java/com/nextgenlab/game/facade/`

**Class:** `AssetFacade`, `AudioFacade`, `BackendFacade`

**Penjelasan:**  
Facade menyediakan antarmuka tunggal yang menyederhanakan akses ke subsistem yang kompleks. Daripada memanggil LibGDX API secara langsung dari berbagai class, semua akses dimediasi melalui Facade.

**Implementasi:**

```java
// TANPA Facade (❌ buruk — coupling langsung ke LibGDX)
Texture tex = new Texture(Gdx.files.internal("sprite.png"));

// DENGAN Facade (✅ baik — satu titik akses)
Texture tex = AssetFacade.getInstance().getTexture("sprite.png");
```

| Class | Subsistem yang disederhanakan |
|---|---|
| `AssetFacade` | LibGDX AssetManager, TiledMap loading |
| `AudioFacade` | LibGDX Sound/Music, volume control |
| `BackendFacade` | HTTP requests ke Spring Boot REST API |

`AssetFacade` dan `AudioFacade` juga mengimplementasikan **Singleton** — hanya satu instance yang dibuat selama runtime aplikasi.

---

## 2. Factory Pattern

**Lokasi:** `Game/core/src/main/java/com/nextgenlab/game/factory/EntityFactory.java`

**Penjelasan:**  
Factory Pattern memusatkan pembuatan objek. Daripada memanggil constructor entitas langsung, kita menggunakan factory method yang menangani inisialisasi penuh (animasi, strategi, dll).

**Implementasi:**

```java
// Factory membuat entitas dengan semua setup-nya
Researcher r = EntityFactory.createResearcher(startX, startY, map);
Monster    m = EntityFactory.createMonster(startX, startY, map);
Guard      g = EntityFactory.createGuard(spawnX, spawnY, map);
Chest      c = EntityFactory.createChest(x, y, rng);
```

Metode factory memanggil `entity.show()` (memuat tekstur) secara otomatis, sehingga pemanggil tidak perlu tahu detail inisialisasi.

---

## 3. Command Pattern

**Lokasi:** `Game/core/src/main/java/com/nextgenlab/game/command/`

**Class:** `MoveCommand` (interface), `InputHandler`

**Penjelasan:**  
Command Pattern mengenkapsulasi permintaan sebagai objek. Input pemain diubah menjadi command object yang dapat dieksekusi kapan saja, memisahkan logika input dari logika pergerakan.

**Implementasi:**

```java
// Interface Command
public interface MoveCommand {
    void execute(Researcher researcher, float delta);
}

// InputHandler meng-resolve input menjadi command
MoveCommand cmd = inputHandler.resolve(); // W/A/S/D → arah
if (cmd != null) cmd.execute(screen.researcher, delta);
```

**Manfaat:** Mudah menambah movement command baru (misalnya teleport, knockback) tanpa mengubah logika input.

---

## 4. State Pattern

**Lokasi:** `Game/core/src/main/java/com/nextgenlab/game/state/`

**Class:** `GameStateHandler` (interface), `PreparationState`, `DuelState`

**Penjelasan:**  
State Pattern memungkinkan objek mengubah perilakunya ketika state internalnya berubah. Game memiliki dua fase utama dengan logika yang sangat berbeda.

**Implementasi:**

```
GameScreen
    │
    ├── currentState: GameStateHandler
    │       │
    │       ├── PreparationState  (task completion, fog of war, guard AI)
    │       │
    │       └── DuelState         (combat, arena map, projectile combat)
    │
    └── transitionTo(newState) → exit + dispose lama, enter + init baru
```

```java
// Interface State
public interface GameStateHandler {
    void enter(GameScreen screen);
    void update(float delta, GameScreen screen);
    void render(GameScreen screen);
    void exit(GameScreen screen);
    void resize(GameScreen screen, int w, int h);
    void dispose();
}

// Transisi ke DuelState saat semua task selesai
if (tasksCompleted >= TOTAL_TASKS)
    screen.transitionTo(new DuelState());
```

---

## 5. Strategy Pattern

**Lokasi:** `Game/core/src/main/java/com/nextgenlab/game/strategy/`

**Class:** `MovementStrategy` (interface), `ChasePlayerStrategy`, `PatrolStrategy`, `GuardStrategy`, `AlertChaseStrategy`

**Penjelasan:**  
Strategy Pattern mendefinisikan keluarga algoritma yang dapat dipertukarkan. Digunakan untuk AI Monster dan Guard agar perilaku gerak dapat diganti saat runtime.

**Implementasi:**

```java
// Interface Strategy
public interface MovementStrategy {
    void move(Monster monster, float targetX, float targetY, float delta);
}

// Dua strategi berbeda untuk satu Monster
monster.setStrategy(new PatrolStrategy(waypoints));   // default
monster.setStrategy(new ChasePlayerStrategy());       // saat LOS terdeteksi

// AI Guard menggunakan strategi berbeda
guard.setMovementStrategy(new GuardPatrolStrategy());
guard.setMovementStrategy(new AlertChaseStrategy());  // saat Monster terdeteksi
```

---

## 6. Object Pool Pattern

**Lokasi:** `Game/core/src/main/java/com/nextgenlab/game/pool/`

**Class:** `ProjectilePool`, `Projectile`

**Penjelasan:**  
Object Pool Pattern menyimpan kumpulan objek yang siap pakai daripada membuat dan menghancurkan objek setiap saat. Kritis untuk proyektil yang ditembak berkali-kali per detik.

**Implementasi:**

```java
// Inisialisasi pool dengan kapasitas 16 proyektil
ProjectilePool guardPool = new ProjectilePool(16, Color.ORANGE);

// Ambil proyektil dari pool (tidak membuat objek baru)
Projectile bullet = guardPool.obtain();
if (bullet != null) bullet.init(x, y, dx, dy, "GUARD", 280f);

// Proyektil menandai dirinya inactive saat mengenai target
bullet.reset();  // dikembalikan ke pool untuk dipakai ulang

// Update dan render semua proyektil aktif
guardPool.updateAll(delta);
guardPool.renderAll(batch);
```

**Manfaat:** Menghindari Garbage Collection pressure pada heap Java saat banyak proyektil ditembak.

---

## 7. Template Method Pattern

**Lokasi:** `Game/core/src/main/java/com/nextgenlab/game/task/`

**Class:** `LabTask` (abstract), `WireTask`, `ServerHackTask`, `ReactorTask`, `ChemicalMixTask`, `BiometricLockTask`

**Penjelasan:**  
Template Method mendefinisikan kerangka algoritma dalam base class, dengan langkah-langkah tertentu didelegasikan ke subclass. Semua task memiliki struktur yang sama (inisialisasi → render → pengecekan penyelesaian → dispose), tapi UI-nya berbeda-beda.

**Implementasi:**

```java
// Base class menentukan kerangka
public abstract class LabTask {
    protected Stage stage;
    protected boolean isCompleted = false;

    // Template method — urutan tidak boleh diubah subclass
    public final void init() {
        stage = new Stage(...);
        buildUI();          // ← hook method — diimplementasikan subclass
    }

    // Hook method — wajib diimplementasikan subclass
    protected abstract void buildUI();

    // Metode konkret — sama untuk semua task
    public void render() { /* render border + stage */ }
    public void finishTask() { isCompleted = true; }
    public boolean isCompleted() { return isCompleted; }
    public void dispose() { stage.dispose(); }
}

// Subclass hanya mengimplementasikan buildUI()
public class WireTask extends LabTask {
    @Override
    protected void buildUI() {
        // UI khusus task "Rangkaian Kawat"
    }
}
```

---

## 8. Observer Pattern

**Lokasi:** `Game/core/src/main/java/com/nextgenlab/game/event/`

**Class:** `EventBus` (Singleton Subject), `GameEventListener<E>` (@FunctionalInterface Observer), `OnTaskCompleted`, `OnGuardKilled`, `OnMonsterLevelUp`, `OnPlayerHit`, `OnSerumProgress`

**Penjelasan:**  
Observer Pattern mendefinisikan hubungan satu-ke-banyak antar objek: ketika satu objek (Subject) berubah state, semua objek yang bergantung padanya (Observer) diberi notifikasi secara otomatis. Dalam NextGenLab, `EventBus` bertindak sebagai Subject terpusat berbasis pub-sub, memungkinkan komponen game saling berkomunikasi tanpa coupling langsung.

**Implementasi:**

```java
// Observer interface — @FunctionalInterface agar bisa pakai lambda
@FunctionalInterface
public interface GameEventListener<E> {
    void onEvent(E event);
}

// Subject — Singleton EventBus
public class EventBus {
    private static EventBus instance;
    private final Map<Class<?>, List<GameEventListener<?>>> listeners = new HashMap<>();

    public static EventBus getInstance() { ... }

    // Subscribe: daftarkan listener untuk tipe event tertentu
    public <E> void subscribe(Class<E> type, GameEventListener<E> listener) {
        listeners.computeIfAbsent(type, k -> new ArrayList<>()).add(listener);
    }

    // Publish: notifikasi semua listener yang terdaftar
    public <E> void publish(E event) {
        List<GameEventListener<?>> list = listeners.get(event.getClass());
        if (list != null)
            for (GameEventListener<?> l : new ArrayList<>(list))
                ((GameEventListener<E>) l).onEvent(event);
    }
}

// Contoh penggunaan — subscribe di PreparationState
EventBus.getInstance().subscribe(OnTaskCompleted.class, e -> {
    tasksCompleted++;
    roundNotifMsg   = "Task selesai! (" + tasksCompleted + "/" + TOTAL_TASKS + ")";
    roundNotifTimer = 2.5f;
});

// Contoh publish — saat task selesai
EventBus.getInstance().publish(new OnTaskCompleted(taskIndex));
```

| Event Class | Dipublikasikan Oleh | Didengarkan Oleh |
|---|---|---|
| `OnTaskCompleted` | `PreparationState` (saat task selesai) | Achievement system, UI notif |
| `OnGuardKilled` | `PreparationState` (saat guard HP = 0) | Achievement system |
| `OnMonsterLevelUp` | `Monster.levelUp()` | Achievement system, UI notif |
| `OnPlayerHit` | `DuelState` (saat serangan mengenai) | HP bar, feedback visual |
| `OnSerumProgress` | `PreparationState` (serum task update) | UI progress bar |

**Manfaat:** Komponen game tidak perlu saling referensi langsung — `Monster` tidak perlu tahu bahwa `AchievementSystem` ada, cukup publish event `OnMonsterLevelUp`.

---

## Ringkasan

| # | Pattern | Package | Class Utama | Tujuan |
|---|---|---|---|---|
| 1 | Facade | `facade/` | AssetFacade, AudioFacade, BackendFacade | Sederhanakan akses subsistem |
| 2 | Factory | `factory/` | EntityFactory | Pusatkan pembuatan entitas |
| 3 | Command | `command/` | MoveCommand, InputHandler | Enkapsulasi input pergerakan |
| 4 | State | `state/` | PreparationState, DuelState | Manajemen fase game |
| 5 | Strategy | `strategy/` | ChasePlayerStrategy, PatrolStrategy | AI yang dapat dipertukarkan |
| 6 | Object Pool | `pool/` | ProjectilePool, Projectile | Efisiensi memori proyektil |
| 7 | Template Method | `task/` | LabTask dan 5 subclass | Kerangka task dengan UI bervariasi |
| 8 | Observer | `event/` | EventBus, GameEventListener | Notifikasi antar komponen tanpa coupling |
| + | Singleton | `facade/` | AssetFacade, AudioFacade | Satu instance per runtime |
