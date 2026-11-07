# PlayMe - Native Android Local Vertical Video Player

PlayMe is a high-performance native Android video player application designed for smooth, TikTok-style vertical playback of local video libraries, backed by Room persistence for instant indexing, full gesture controls, and a five-playlist management system with custom iconography.

---

## User Review & Critical Decisions

> [!IMPORTANT]
> The following user preferences were confirmed during clarification and are locked into the design:

- **Empty Folder / Initial Launch**: A "Muat Contoh Video Demo" (Load Demo Videos) action will be provided alongside the folder picker so users can test the TikTok vertical feed immediately, even on fresh emulators or before picking a device folder.
- **Playback Behavior**: Videos will seamlessly loop automatically until the user swipes up or down, replicating the familiar TikTok viewing experience.
- **Playlist Nomenclature**: The five playlists will remain strictly named **Playlist 1** through **Playlist 5**, matching the requested specifications.
- **Zero-Thumbnail RAM Protection**: The dashboard avoids bitmap generation and image loading entirely to ensure smooth operation across libraries containing thousands of video clips.

---

## 1. Overview & Core Concept

- **What It Does**: PlayMe allows users to select a device folder using the Android Storage Access Framework (SAF), index video files into an optimized local SQLite/Room database, and watch them in an immersive vertical swiping interface.
- **Target Audience**: Users with large collections of vertical shorts, mobile clips, or downloaded videos who desire a fast, lightweight, and clutter-free playback experience.
- **Key Value**: Instant startup without repetitive scanning, negligible memory footprint via pure text dashboard summaries and 3-player window pooling (previous, current, next), and single-tap playlist curation.

---

## 2. User Experience & Visual Design

### Key User Flows

1. **Dashboard / Library Management**:
   - Status header showing total video count, total file size (formatted in MB/GB), active folder URI/path, and last scan timestamp.
   - Primary action buttons:
     - `▶ Putar Semua Video`: Resumes playback from the last saved video index and position.
     - `🔄 Re-scan Folder`: Incremental background rescan to detect added/removed video files.
     - `📂 Ganti Folder Utama`: SAF folder picker for choosing a new video directory.
     - `🎬 Muat Contoh Video Demo`: Generates/loads sample local video clips for instant previewing.
   - 2-Column Playlist Grid: Cards for Playlist 1, 2, 3, 4, and 5 displaying current item counts. Tapping any card launches the vertical player filtered exclusively to that playlist.

2. **Vertical Video Player (TikTok Experience)**:
   - Full-bleed vertical pager (`VerticalPager` / ViewPager logic with ExoPlayer pooling).
   - **Gestures**:
     - *Swipe Up / Left*: Next video.
     - *Swipe Down / Right*: Previous video.
     - *Double Tap*: Play / Pause toggle with an animated pulsing icon (semi-transparent) in the center that smoothly fades out.
     - *Single Tap*: Toggles visibility of the On-Screen Display (OSD) overlay.
   - **Scrubber / Seeking Bar**:
     - Ultra-thin interactive progress line at the very bottom of the screen.
     - Draggable thumb for scrub precision with real-time timestamp preview.
   - **Top Right Mode Menu**:
     - Cycle button with badge: Random / Shuffle (Default) ➔ Name (A-Z) ➔ Date (Newest/Oldest) ➔ Duration ➔ File Size ➔ Repeat One.
   - **Right-Side Playlist Quick-Toggle Stack**:
     - Positioned on the right edge, ordered vertically with Playlist 1 at the top down to Playlist 5 at the bottom.
     - Custom Vector Icon: Two crossed carrots forming an "X" with the playlist digit (1–5) prominently centered.
     - State styling: Solid vibrant accent fill + pulse bounce when active for the current video; frosted translucent outline when inactive. Tapping toggles membership instantly in Room.

### Visual Identity & Theme
- **Theme**: High-contrast Dark Mode with deep blacks (`#0D0E11`), elevated charcoal surfaces (`#181A20`), and vibrant carrot-orange accents (`#FF6D00` / `#FF9E40`) with crisp typography.
- **Typography**: Clean, readable sans-serif hierarchy with bold tabular figures for stats and durations.
- **Motion & Micro-interactions**: Haptic and scale spring animations on playlist toggle taps, fluid crossfade on OSD visibility, and smooth seekbar drag transitions.

---

## 3. Key Product Decisions & Trade-Offs

- **Decision 1: Text-Only Dashboard**:
  - *Chosen Approach*: Pure text metrics and metadata cards without thumbnail extraction.
  - *Why*: Extracting thousands of video thumbnails requires significant disk cache and causes Out-Of-Memory (OOM) lag on mobile devices. Text-only ensures instantaneous launch and 60fps scrolling.
- **Decision 2: 3-Player Window Management**:
  - *Chosen Approach*: Maintain at most 3 prepared ExoPlayer instances (previous, current, next) and release detached instances immediately.
  - *Why*: Mobile hardware decoders have strict limits on concurrent video codecs. Pre-buffering 1 ahead and 1 behind provides seamless swiping without decoder starvation.
- **Decision 3: Storage Access Framework (SAF) & Persistent Permissions**:
  - *Chosen Approach*: Use `ActivityResultContracts.OpenDocumentTree` and persist permissions with `takePersistableUriPermission`.
  - *Why*: Complies fully with Android 11+ scoped storage policies without requiring dangerous broad file system permissions.

---

## 4. Technical Architecture & Data Strategy

```
┌─────────────────────────────────────────────────────────────┐
│                       UI Layer (M3)                         │
│  ┌───────────────────────┐       ┌───────────────────────┐  │
│  │   DashboardScreen     │       │   VerticalPlayerScreen│  │
│  │ (Status, Stats, Grids)│       │ (Pager, OSD, Carrots) │  │
│  └───────────┬───────────┘       └───────────┬───────────┘  │
└──────────────┼───────────────────────────────┼──────────────┘
               ▼                               ▼
┌─────────────────────────────────────────────────────────────┐
│                 PlayMeViewModel (StateFlow)                 │
│  - Playback Order (Shuffle, Name, Date, Size, Repeat)       │
│  - Active Playlist Filter / Resume Index Tracking           │
│  - Folder Scanner Coroutine & Demo Clip Seeder              │
└──────────────┬───────────────────────────────┬──────────────┘
               ▼                               ▼
┌───────────────────────────────┐ ┌───────────────────────────┐
│     PlayMeRepository          │ │    Media3 ExoPlayer Pool  │
│  - Reactive Flow<List<Video>> │ │ - Current / Prev / Next   │
│  - Playlist Cross-Refs        │ │ - Loop & Playback Controls│
└──────────────┬────────────────┘ └───────────────────────────┘
               ▼
┌─────────────────────────────────────────────────────────────┐
│             Room Database (SQLite + KSP)                    │
│  - VideoEntity (id, uri, title, size, duration, dateAdded)  │
│  - PlaylistEntity (id, name, createdAt)                     │
│  - VideoPlaylistCrossRef (videoId, playlistId)              │
│  - AppStateEntity (lastFolderUri, lastScanTime, lastIndex)  │
└─────────────────────────────────────────────────────────────┘
```

### Data Entities
1. `VideoEntity`: `id` (auto PK), `uri` (unique path/URI), `fileName`, `fileSizeBytes`, `durationMs`, `dateModified`, `lastPositionMs`.
2. `PlaylistEntity`: 5 seeded rows (id 1 to 5, names "Playlist 1" to "Playlist 5").
3. `VideoPlaylistItem`: `videoId`, `playlistId` compound primary key.
4. `PlaybackSettings`: Stores current sort mode, active folder URI, last scan timestamp, and last played video ID.
