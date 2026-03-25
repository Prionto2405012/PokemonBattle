# PokemonBattle

PokemonBattle is a JavaFX desktop game project with:
- Local battles (player vs AI)
- Online battles through a TCP server (server-authoritative battle state)
- "Local Player" battles implemented via localhost TCP matchmaking (not offline hot-seat)
- SQLite-backed authentication and player history
- Scene-based UI flow with animated transitions and overlays

This README is a living map of the codebase and workflow. It is designed to help new contributors quickly understand what runs where, what to change for each feature area, and how data moves through the app.

## 1) Tech Stack

- Language: Java 21
- UI: JavaFX 21 (FXML + CSS)
- Build: Maven
- Database: SQLite
- JSON parsing: Gson
- Networking: Java sockets with serializable message protocol

Primary build config: `pom.xml`
Module system: `src/main/java/module-info.java`

## 2) Repository Layout

Top-level structure:

```text
PokemonBattle/
  src/main/java/com/example/pokemonbattle/
    controller/   # UI controller logic
    model/        # Domain models and battle entities
    database/     # DB access and game data loading
    service/      # Application services
    server/       # TCP server + message protocol
    util/         # Shared managers/helpers
    security/     # Security-related classes
    HelloApplication.java
    Launcher.java
  src/main/resources/com/example/pokemonbattle/
    view/         # FXML screens
    css/          # Screen styling
    data/         # Data files (pokemon heights)
    database/     # JSON game data + SQL schema
    sprites/      # Pokemon/trainer assets
    audio/        # BGM/SFX
    fonts/        # UI fonts
    assets/       # Misc media/assets
```

## 3) Entry Points and Runtime Modes

### Desktop client (JavaFX)

- Main launcher class: `com.example.pokemonbattle.Launcher`
- JavaFX app class: `com.example.pokemonbattle.HelloApplication`
- Initial scene loaded: `intro.fxml`

### TCP battle server

- Primary server class: `com.example.pokemonbattle.server.BattleServer`
- Convenience launcher: `com.example.pokemonbattle.server.PokemonBattleServerLauncher`
- Default server port: 5555

## 4) Build and Run

Windows (from project root):

```powershell
.\mvnw.cmd clean compile
.\mvnw.cmd javafx:run
```

If wrapper is unavailable:

```powershell
mvn clean compile
mvn javafx:run
```

Run server (default port):

```powershell
mvn exec:java -Dexec.mainClass="com.example.pokemonbattle.server.BattleServer"
```

Run server (custom port):

```powershell
mvn exec:java -Dexec.mainClass="com.example.pokemonbattle.server.BattleServer" -Dexec.args="7777"
```

## 4.1) Build a No-Java-Required Windows App (.exe)

You can build a Windows package that includes its own runtime, so target machines do not need Java installed.

From project root:

```powershell
.\build-windows-package.bat
```

This script creates:
- Portable app executable: `dist/PokemonBattle/PokemonBattle.exe`
- Windows installer (optional): `dist/PokemonBattle-1.0.0.exe`

Installer prerequisite:
- To generate the installer `.exe`, install WiX Toolset and add `candle.exe` and `light.exe` to `PATH`.
- Without WiX, the script still builds the portable app image (`dist/PokemonBattle/PokemonBattle.exe`).

Distribution usage:
- If installer was generated, share `dist/PokemonBattle-1.0.0.exe` and install normally.
- Otherwise, share the `dist/PokemonBattle` folder and run `PokemonBattle.exe` directly.
- No separate Java installation is required on that machine.

Important platform note:
- Native packages are OS-specific.
- Build on Windows for Windows `.exe`, on macOS for `.dmg/.pkg`, and on Linux for `.deb/.rpm` (or app-image style bundles).

Important runtime note:

- `New Game -> Local Player` currently uses the same online pipeline over `localhost:5555`.
- If the battle server is not running, `waiting_online.fxml` will show a connection failure.
- For now, start `BattleServer` before trying both `Online` and `Local Player` multiplayer paths.

## 5) What Is Done Where

### Controllers (`src/main/java/com/example/pokemonbattle/controller`)

- `WcController`: Login/signup flow, validation feedback, auth handoff
- `MenuController`: Main menu navigation and settings entry
- `NewGameController`: Battle setup (mode/opponent/team), route to local or online path
- `BattleController`: Local battle screen and local battle progression
- `OnlineBattleController`: Online battle UI, server message handling, turn state
- `WaitingController`: Matchmaking/wait flow while searching for online opponent
- `SettingsController`: Game settings overlay logic
- `PokemonSelectionOverlayController`: Team builder overlay interactions
- `AvatarSelectionController`: Avatar picker
- `IntroController`, `StartController`, `LoadingScreenController`: startup/transition screens

### Models (`src/main/java/com/example/pokemonbattle/model`)

Core domain types such as:
- `User`, `Player`
- `PokemonSpecies`, `PokemonInstance`, `Move`
- Battle-related classes (`Battle`, `Action`, etc.)
- History records (`BattleRecord`)

### Database (`src/main/java/com/example/pokemonbattle/database`)

- DB connectivity and initialization
- Game data loading from JSON
- SQL schema and data query orchestration

### Services (`src/main/java/com/example/pokemonbattle/service`)

- `AuthService`: register/login business logic and validation
- `BattleHistoryManager`: history retrieval and aggregation
- `PokemonSearchService`: Pokemon lookup/search utilities

### Server (`src/main/java/com/example/pokemonbattle/server`)

- `BattleServer`: connection handling + matchmaking queue + active battles
- `ClientHandler`: per-client processing thread
- `OnlineBattle`: server-side authoritative battle resolution
- `ServerConnection`: client-side network bridge
- Message protocol classes: `GameMessage` and subclasses (`LoginRequest`, `MoveMessage`, `DamageMessage`, etc.)

### Utilities (`src/main/java/com/example/pokemonbattle/util`)

- `SceneManager`: scene switching, loading flow, cross-scene data
- `PlayerSession`: authenticated user/session state
- `MusicManager`, `MediaCache`: audio/media lifecycle and reuse
- Battle/UI animation helpers (`BattleAnimationManager`, `CurtainTransitionManager`, `PokeballOverlay`, `GifCanvas`)

### Resources (`src/main/resources/com/example/pokemonbattle`)

- `view/`: FXML layouts
- `css/`: screen-level styles
- `database/`: `schema.sql`, `pokemon_gen4.json`, `moves_gen4.json`, `battle_items.json`
- `data/`: `pokemon_heights.json`
- `sprites/`, `audio/`, `fonts/`, `assets/`: runtime media

## 6) Workflow: End-to-End Application Flow

### A) Startup and Navigation

1. `Launcher` starts `HelloApplication`.
2. `HelloApplication` initializes `SceneManager`, fonts, media cache.
3. First scene is switched to `intro.fxml`.
4. Intro-to-start transition passes temporary overlay state through scene-switch payload data.
5. Navigation between screens is centralized through `SceneManager`.

### B) Authentication Flow

1. UI input handled in `WcController`.
2. `WcController` calls `AuthService` for register/login.
3. `AuthService` validates and delegates persistence to database/DAO layer.
4. On success, user state is stored in `PlayerSession` and app navigates to menu/new game flows.

### C) Local Battle Flow

1. `NewGameController` collects mode/opponent/team choices.
2. Player and AI teams are prepared from species/move datasets.
3. Scene transitions to `battle.fxml` and `BattleController` runs local battle rounds.
4. Result is persisted to history and shown in UI.

### D) Online Battle Flow

1. Client connects via `ServerConnection`.
2. Login and matchmaking messages are sent (`LoginRequest`, `FindOpponentRequest`).
3. `BattleServer` pairs players and starts an `OnlineBattle` instance.
4. During turns, clients submit actions/moves and receive authoritative updates (`DamageMessage`, `TurnReadyMessage`, `BattleEndMessage`, etc.).
5. `OnlineBattleController` updates UI state based only on server messages.

### E) Local Player (Same-Machine Multiplayer) Flow

1. `NewGameController` stores `connectionMode=LOCAL` and routes to `waiting_online.fxml`.
2. `WaitingController` connects to `localhost:5555` using `ServerConnection`.
3. Matchmaking and battle progression then use the same server-authoritative message flow as online mode.
4. If no server is listening on port `5555`, the connection attempt fails and the waiting screen reports it.

## 7) Data and State Ownership

- UI transient state: screen controllers
- Cross-scene state: `SceneManager` data map
- Session identity/preferences: `PlayerSession`
- Authoritative online battle state: server (`OnlineBattle`)
- Persistent user/game history: SQLite database
- Local-player multiplayer authority: same TCP server stack (`localhost`), not offline in-process battle authority

Rule of thumb:
- If the change affects battle fairness/validation in online mode, implement it on server side first.
- If the change is visual/navigation only, keep it in controller/FXML/CSS layers.

## 8) Key Screens to Their Controllers

FXML to controller mapping:

- `wc.fxml` -> `WcController`
- `menu.fxml` -> `MenuController`
- `new_game.fxml` -> `NewGameController`
- `battle.fxml` -> `BattleController`
- `online_battle.fxml` -> `OnlineBattleController`
- `waiting_online.fxml` -> `WaitingController`
- `settings.fxml` -> `SettingsController`
- `pokemon_selection_overlay.fxml` -> `PokemonSelectionOverlayController`
- `avatar_selection.fxml` -> `AvatarSelectionController`
- `intro.fxml` -> `IntroController`
- `start.fxml` -> `StartController`
- `loading_screen.fxml` -> `LoadingScreenController`

## 9) Online Protocol Overview

Representative message lifecycle:

1. `LoginRequest` -> `LoginResponse`
2. `FindOpponentRequest` -> `BattleStartMessage`
3. `ActionMessage`/`MoveMessage` -> `DamageMessage` and `BattleUpdateMessage`
4. Turn synchronization -> `TurnReadyMessage`
5. End condition -> `BattleEndMessage`
6. Exceptional path -> `ErrorMessage` or `ForfeitMessage`

All protocol classes are in `src/main/java/com/example/pokemonbattle/server`.

## 10) Database and Game Data

Primary data files:
- `src/main/resources/com/example/pokemonbattle/database/schema.sql`
- `src/main/resources/com/example/pokemonbattle/database/pokemon_gen4.json`
- `src/main/resources/com/example/pokemonbattle/database/moves_gen4.json`
- `src/main/resources/com/example/pokemonbattle/database/battle_items.json`
- `src/main/resources/com/example/pokemonbattle/data/pokemon_heights.json`

The app uses SQLite and loads game data from JSON resources into model objects used by setup and battle systems.

## 11) Existing Deep-Dive Docs

Use these for subsystem details:
- `AUTHENTICATION_INTEGRATION.md`
- `AUTHENTICATION_SUMMARY.md`
- `AUTH_API_REFERENCE.md`
- `BATTLE_SETUP_IMPLEMENTATION.md`
- `TCP_SERVER_README.md`
- `TCP_SERVER_QUICKSTART.md`
- `LOADING_SCREEN_INTEGRATION.md`
- `LOADING_SCREEN_USAGE.md`

## 12) Living README Update Protocol (Always Updated)

This README should be updated in every PR that changes architecture, flow, ownership, protocol, or data files.

### Required update triggers

Update this README when any of the following changes:
- New controller/service/model/server class added
- Scene flow/navigation path changed
- New FXML/CSS screen added or renamed
- Server message protocol changed
- Database schema/data-file locations changed
- Build/run commands changed

### PR checklist item

Add this to your PR checklist:

- [ ] README updated for codebase/workflow changes

### Suggested maintenance routine

1. Before coding: read Sections 5-7 for ownership and flow.
2. During coding: keep names and paths aligned with existing conventions.
3. Before merge: verify Section 2 (layout), Section 5 (what is done where), and Section 6 (workflow) still match code.
4. If flow changed: update both this README and the relevant deep-dive doc.

## 13) Contributor Quick Guide

If you want to change...

- Login/signup behavior: start in `WcController` and `AuthService`
- Team building/new game setup: start in `NewGameController` and `PokemonSelectionOverlayController`
- Local battle mechanics/UI: start in `BattleController` and battle models
- Online match flow/protocol: start in `OnlineBattleController`, `ServerConnection`, and `server/*`
- Scene transitions/loading behavior: start in `SceneManager` and `LoadingScreenController`
- Theme/styling: start in `src/main/resources/com/example/pokemonbattle/css`

## 14) Notes and Conventions

- Keep online battle logic server-authoritative.
- Keep scene transitions centralized through `SceneManager`.
- Keep persistent state in SQLite/session helpers, not in ad-hoc static UI state.
- Prefer package-level cohesion: controller for UI orchestration, service for business logic, server for network protocol/authority.

---

If you are onboarding to this repo, follow this order:
1. Read Sections 2, 5, and 6 in this README.
2. Run the client and server commands in Section 4.
3. Open the specific deep-dive doc from Section 11 for the feature you are modifying.