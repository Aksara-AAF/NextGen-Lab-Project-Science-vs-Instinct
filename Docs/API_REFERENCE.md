# API Reference — NextGenLab Backend

Base URL: `http://localhost:8080`  
Autentikasi: Bearer JWT Token (dikirim via header `Authorization: Bearer <token>`)

---

## Auth

### POST `/api/auth/register`
Mendaftarkan akun baru.

**Request Body:**
```json
{
  "email": "user@example.com",
  "username": "pemain123",
  "password": "password123"
}
```

**Response 200:**
```json
{
  "token": "eyJhbGc...",
  "user": {
    "id": 1,
    "username": "pemain123",
    "email": "user@example.com"
  }
}
```

**Response 400:**
```json
{ "error": "Email sudah terdaftar" }
```

---

### POST `/api/auth/login`
Login dan mendapat JWT token.

**Request Body:**
```json
{
  "email": "user@example.com",
  "password": "password123"
}
```

**Response 200:** sama seperti register.

---

### GET `/api/auth/me`
Mendapat info user yang sedang login. **(Auth required)**

**Response 200:**
```json
{
  "id": 1,
  "username": "pemain123",
  "email": "user@example.com"
}
```

---

## Match

### POST `/api/match/start`
Membuat sesi match baru. **(Auth required)**

**Response 200:**
```json
{ "id": 42 }
```

---

### POST `/api/match/{id}/update`
Update progres peneliti atau monster. **(Auth required)**

**Request Body:**
```json
{
  "role": "RESEARCHER",
  "amount": 10
}
```

- `role`: `"RESEARCHER"` atau `"MONSTER"`
- `amount`: jumlah penambahan progres (1–100)

**Response 200:** MatchSession terbaru

---

### GET `/api/match/{id}/status`
Cek status match saat ini.

**Response 200:**
```json
{ "status": "PREPARATION" }
```

Status: `PREPARATION`, `DUEL`, `FINISHED`

---

### POST `/api/match/{id}/duel-start`
Notifikasi bahwa fase Duel dimulai (idempotent, bisa dipanggil keduanya).

**Response 200:** OK

---

### POST `/api/match/{id}/finish`
Selesaikan match, hitung ELO dan achievement. **(Auth required)**

**Request Body:**
```json
{ "winner": "RESEARCHER" }
```

**Response 200:** Array kode achievement baru yang didapat
```json
["FIRST_WIN", "ALL_TASKS_DONE"]
```

---

## Room

### POST `/api/room/create`
Buat game room baru. **(Auth required)**

**Request Body:**
```json
{ "isPublic": true }
```

**Response 200:**
```json
{
  "roomCode": "A3F7K2",
  "matchSessionId": 42,
  "status": "WAITING"
}
```

---

### POST `/api/room/join/{code}`
Bergabung ke room dengan kode. **(Auth required)**

**Response 200:** Detail room

---

### GET `/api/room/list`
Daftar room publik yang masih menunggu pemain.

**Response 200:**
```json
[
  {
    "roomCode": "A3F7K2",
    "hostUsername": "pemain123",
    "status": "WAITING"
  }
]
```

---

### GET `/api/room/{code}`
Detail lengkap room beserta username pemain.

**Response 200:**
```json
{
  "roomCode": "A3F7K2",
  "status": "WAITING",
  "hostUserId": 1,
  "player2UserId": 2,
  "researcherUserId": 1,
  "monsterUserId": 2,
  "researcherReady": false,
  "monsterReady": false,
  "publicRoom": true,
  "matchSessionId": 42,
  "hostUsername": "pemain123",
  "player2Username": "musuh456"
}
```

---

### POST `/api/room/{code}/claim`
Klaim role dalam room. **(Auth required)**

**Request Body:**
```json
{ "role": "RESEARCHER" }
```

---

### POST `/api/room/{code}/ready`
Tandai pemain siap. **(Auth required)**

### POST `/api/room/{code}/unready`
Batalkan kesiapan. **(Auth required)**

### POST `/api/room/{code}/start`
Mulai countdown (hanya host). **(Auth required)**

### POST `/api/room/{code}/abort`
Batalkan countdown. **(Auth required)**

### POST `/api/room/{code}/leave`
Keluar dari room. **(Auth required)**

### POST `/api/room/{code}/reset`
Reset room ke kondisi awal. **(Auth required)**

### POST `/api/room/{code}/visibility`
Ubah visibilitas room. **(Auth required)**

**Request Body:**
```json
{ "isPublic": false }
```

### POST `/api/room/{code}/kick/{userId}`
Kick pemain dari room (hanya host). **(Auth required)**

---

## Stats & Leaderboard

### GET `/api/stats/{userId}`
Statistik pemain.

**Response 200:**
```json
{
  "userId": 1,
  "username": "pemain123",
  "totalMatches": 25,
  "wins": 14,
  "losses": 11,
  "elo": 1120,
  "researcherMatches": 12,
  "researcherWins": 8,
  "monsterMatches": 13,
  "monsterWins": 6
}
```

---

### GET `/api/stats/{userId}/history?page=0`
Riwayat match (10 per halaman, diurutkan terbaru).

**Response 200:**
```json
[
  {
    "matchSessionId": 42,
    "role": "RESEARCHER",
    "won": true,
    "durationSeconds": 187,
    "eloBefore": 1080,
    "eloAfter": 1120,
    "playedAt": "2026-05-24T14:30:00Z"
  }
]
```

---

### GET `/api/leaderboard`
Top 10 pemain terbaik per role.

**Response 200:**
```json
{
  "researcher": [
    { "rank": 1, "username": "pemain123", "elo": 1320, "wins": 45 }
  ],
  "monster": [
    { "rank": 1, "username": "musuh456", "elo": 1280, "wins": 38 }
  ]
}
```

---

## Achievements

### GET `/api/achievements/user/{userId}`
Daftar achievement yang sudah diraih.

**Response 200:**
```json
[
  {
    "code": "FIRST_WIN",
    "name": "Kemenangan Pertama",
    "description": "Menangkan pertandingan pertama kali",
    "unlockedAt": "2026-05-24T10:00:00Z"
  }
]
```

---

## Friends

### POST `/api/friends/request`
Kirim permintaan pertemanan. **(Auth required)**

**Request Body:**
```json
{ "targetUserId": 2 }
```

### PUT `/api/friends/accept/{friendshipId}`
Terima permintaan pertemanan. **(Auth required)**

### GET `/api/friends`
Daftar teman. **(Auth required)**

**Response 200:**
```json
[
  { "id": 2, "username": "musuh456" }
]
```

### GET `/api/friends/pending`
Daftar permintaan pertemanan masuk. **(Auth required)**

**Response 200:**
```json
[
  { "friendshipId": 5, "fromUserId": 3, "fromUsername": "orang_baru" }
]
```

### DELETE `/api/friends/{targetUserId}`
Hapus pertemanan. **(Auth required)**

---

## Users

### GET `/api/users/search?q={query}`
Cari pemain berdasarkan username.

**Response 200:**
```json
[
  { "id": 3, "username": "orang_baru" }
]
```

---

## Status

### GET `/api/status`
Health check server.

**Response 200:**
```json
{
  "status": "UP",
  "message": "NextGenLab Backend is running",
  "version": "1.0.0"
}
```
