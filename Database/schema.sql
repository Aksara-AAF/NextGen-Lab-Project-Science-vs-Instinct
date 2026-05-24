-- ============================================================
-- NextGenLab: Science vs Instinct
-- Database Schema Dump
-- PostgreSQL 14+
-- ============================================================

-- Drop all tables (untuk fresh install)
DROP TABLE IF EXISTS user_achievements CASCADE;
DROP TABLE IF EXISTS match_history CASCADE;
DROP TABLE IF EXISTS friendships CASCADE;
DROP TABLE IF EXISTS game_rooms CASCADE;
DROP TABLE IF EXISTS match_sessions CASCADE;
DROP TABLE IF EXISTS player_stats CASCADE;
DROP TABLE IF EXISTS achievements CASCADE;
DROP TABLE IF EXISTS evolution_library CASCADE;
DROP TABLE IF EXISTS users CASCADE;

-- ============================================================
-- TABLE: users
-- ============================================================
CREATE TABLE users (
    id            BIGSERIAL PRIMARY KEY,
    email         VARCHAR(255) NOT NULL UNIQUE,
    username      VARCHAR(64)  NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    created_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- ============================================================
-- TABLE: player_stats
-- Satu baris per user, menyimpan statistik kumulatif
-- ============================================================
CREATE TABLE player_stats (
    id                  BIGSERIAL PRIMARY KEY,
    user_id             BIGINT NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    total_matches       INT NOT NULL DEFAULT 0,
    wins                INT NOT NULL DEFAULT 0,
    losses              INT NOT NULL DEFAULT 0,
    elo                 INT NOT NULL DEFAULT 1000,
    researcher_matches  INT NOT NULL DEFAULT 0,
    researcher_wins     INT NOT NULL DEFAULT 0,
    monster_matches     INT NOT NULL DEFAULT 0,
    monster_wins        INT NOT NULL DEFAULT 0
);

-- ============================================================
-- TABLE: match_sessions
-- Satu baris per pertandingan
-- ============================================================
CREATE TABLE match_sessions (
    id                   BIGSERIAL PRIMARY KEY,
    researcher_progress  INT NOT NULL DEFAULT 0 CHECK (researcher_progress BETWEEN 0 AND 100),
    monster_progress     INT NOT NULL DEFAULT 0 CHECK (monster_progress    BETWEEN 0 AND 100),
    status               VARCHAR(32)  NOT NULL DEFAULT 'PREPARATION'
                             CHECK (status IN ('PREPARATION', 'DUEL', 'FINISHED')),
    prep_started_at      BIGINT,
    duel_started_at      BIGINT,
    prep_winner          VARCHAR(16)
                             CHECK (prep_winner IN ('RESEARCHER', 'MONSTER') OR prep_winner IS NULL),
    researcher_user_id   BIGINT REFERENCES users(id) ON DELETE SET NULL,
    monster_user_id      BIGINT REFERENCES users(id) ON DELETE SET NULL,
    winner               VARCHAR(16)
                             CHECK (winner IN ('RESEARCHER', 'MONSTER') OR winner IS NULL),
    finished_at          BIGINT
);

-- ============================================================
-- TABLE: match_history
-- Satu baris per pemain per pertandingan
-- ============================================================
CREATE TABLE match_history (
    id                BIGSERIAL PRIMARY KEY,
    match_session_id  BIGINT NOT NULL REFERENCES match_sessions(id) ON DELETE CASCADE,
    user_id           BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role              VARCHAR(16) NOT NULL CHECK (role IN ('RESEARCHER', 'MONSTER')),
    won               BOOLEAN NOT NULL DEFAULT FALSE,
    duration_seconds  INT NOT NULL DEFAULT 0,
    elo_before        INT NOT NULL DEFAULT 1000,
    elo_after         INT NOT NULL DEFAULT 1000,
    played_at         TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    UNIQUE (match_session_id, user_id)
);

-- ============================================================
-- TABLE: game_rooms
-- Room multiplayer, dibuat sebelum match dimulai
-- ============================================================
CREATE TABLE game_rooms (
    id                  BIGSERIAL PRIMARY KEY,
    room_code           VARCHAR(8)  NOT NULL UNIQUE,
    match_session_id    BIGINT REFERENCES match_sessions(id) ON DELETE SET NULL,
    status              VARCHAR(16) NOT NULL DEFAULT 'WAITING'
                            CHECK (status IN ('WAITING', 'STARTING', 'IN_PROGRESS', 'FINISHED', 'DISBANDED')),
    host_user_id        BIGINT REFERENCES users(id) ON DELETE SET NULL,
    player2_user_id     BIGINT REFERENCES users(id) ON DELETE SET NULL,
    researcher_user_id  BIGINT REFERENCES users(id) ON DELETE SET NULL,
    monster_user_id     BIGINT REFERENCES users(id) ON DELETE SET NULL,
    researcher_ready    BOOLEAN NOT NULL DEFAULT FALSE,
    monster_ready       BOOLEAN NOT NULL DEFAULT FALSE,
    starting_at         BIGINT,
    public_room         BOOLEAN NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- ============================================================
-- TABLE: achievements
-- Master data achievement
-- ============================================================
CREATE TABLE achievements (
    id          BIGSERIAL PRIMARY KEY,
    code        VARCHAR(64)  NOT NULL UNIQUE,
    name        VARCHAR(128) NOT NULL,
    description VARCHAR(512)
);

-- ============================================================
-- TABLE: user_achievements
-- Achievement yang sudah diraih tiap user
-- ============================================================
CREATE TABLE user_achievements (
    id             BIGSERIAL PRIMARY KEY,
    user_id        BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    achievement_id BIGINT NOT NULL REFERENCES achievements(id) ON DELETE CASCADE,
    unlocked_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    UNIQUE (user_id, achievement_id)
);

-- ============================================================
-- TABLE: friendships
-- Pertemanan antar user
-- ============================================================
CREATE TABLE friendships (
    id           BIGSERIAL PRIMARY KEY,
    requester_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    addressee_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    status       VARCHAR(16) NOT NULL DEFAULT 'PENDING'
                     CHECK (status IN ('PENDING', 'ACCEPTED')),
    created_at   TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    UNIQUE (requester_id, addressee_id)
);

-- ============================================================
-- TABLE: evolution_library
-- Master data gen evolusi Monster
-- ============================================================
CREATE TABLE evolution_library (
    id          BIGSERIAL PRIMARY KEY,
    code        VARCHAR(64)  NOT NULL UNIQUE,
    name        VARCHAR(128) NOT NULL,
    description VARCHAR(512),
    effect_json TEXT
);

-- ============================================================
-- INDEXES
-- ============================================================
CREATE INDEX idx_player_stats_user_id    ON player_stats(user_id);
CREATE INDEX idx_match_history_user_id   ON match_history(user_id);
CREATE INDEX idx_match_history_played_at ON match_history(played_at DESC);
CREATE INDEX idx_game_rooms_status       ON game_rooms(status);
CREATE INDEX idx_game_rooms_room_code    ON game_rooms(room_code);
CREATE INDEX idx_friendships_requester   ON friendships(requester_id);
CREATE INDEX idx_friendships_addressee   ON friendships(addressee_id);
CREATE INDEX idx_user_achievements_user  ON user_achievements(user_id);

-- ============================================================
-- SEED DATA: Achievements
-- ============================================================
INSERT INTO achievements (code, name, description) VALUES
    ('FIRST_WIN',           'Kemenangan Pertama',     'Menangkan pertandingan pertama kali'),
    ('RESEARCHER_WIN_5',    'Ilmuwan Sejati',          'Menang sebagai Peneliti sebanyak 5 kali'),
    ('MONSTER_WIN_5',       'Predator Alam',           'Menang sebagai Monster sebanyak 5 kali'),
    ('ALL_TASKS_DONE',      'Tugas Selesai',           'Selesaikan semua 5 task dalam satu pertandingan'),
    ('MAX_LEVEL_MONSTER',   'Evolusi Sempurna',        'Capai level maksimum sebagai Monster'),
    ('WIN_10_MATCHES',      'Veteran',                 'Menangkan 10 pertandingan'),
    ('PLAY_20_MATCHES',     'Pejuang Lab',             'Mainkan 20 pertandingan'),
    ('KILL_10_GUARDS',      'Pembunuh Bayaran',        'Bunuh 10 Guard dalam satu pertandingan'),
    ('FIRST_FRIEND',        'Tidak Sendirian',         'Tambahkan teman pertama'),
    ('ELO_1200',            'Rated Player',            'Capai ELO 1200 atau lebih');

-- ============================================================
-- SEED DATA: Evolution Library
-- ============================================================
INSERT INTO evolution_library (code, name, description, effect_json) VALUES
    ('PREDATOR_CLAWS', 'Cakar Predator',    'Melee damage meningkat +1',        '{"melee_damage_bonus": 1}'),
    ('ADRENAL_SURGE',  'Lonjakan Adrenalin','Kecepatan gerak meningkat 20%',     '{"speed_mult": 1.2}'),
    ('THICK_HIDE',     'Kulit Tebal',       'HP maksimum meningkat +1',          '{"max_hp_bonus": 1}'),
    ('FRENZY',         'Kegilaan',          'Damage meningkat saat HP rendah',   '{"frenzy": true}'),
    ('ECHOLOCATION',   'Ekolokasi',         'Deteksi posisi pemain dalam radius','{"echolocation": true}'),
    ('ACIDIC_BLOOD',   'Darah Asam',        'Aura damage di sekitar monster',    '{"toxic_aura": true}'),
    ('REGENERATION',   'Regenerasi',        'Pulihkan HP saat tidak combat',     '{"regeneration": true}'),
    ('PHASE_SHIFT',    'Pergeseran Fase',   'Menembus dinding sementara',        '{"phase_shift": true}'),
    ('BERSERKER',      'Berserk',           'Damage ×1.3 saat HP < 2',           '{"berserker": true}');
