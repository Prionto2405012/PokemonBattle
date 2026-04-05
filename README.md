# PokemonBattle

PokemonBattle is a desktop Pokemon-style battle game built with JavaFX.
You can play quick AI battles, or battle real players over a local network.

This README is written for two groups:
1. Players who just want to install and play.
2. Developers who want to build, run, and modify the project.

## 1) Player Guide (No Java or JavaFX Setup Needed)

If you install from the packaged Windows build, you do not need to install Java or JavaFX.

### 1.1 What You Need

1. A Windows PC.
2. One of these from the project maintainer:
   - PokemonBattle-1.0.0.exe installer, or
   - The full PokemonBattle portable folder.

### 1.2 Install and Launch

If you got the installer:
1. Double-click PokemonBattle-1.0.0.exe.
2. Follow the install wizard.
3. Launch from desktop or Start menu.

If you got the portable folder:
1. Open the folder.
2. Run PokemonBattle.exe.

Important:
- Keep the whole portable folder together.
- Do not copy only one .exe file out of that folder.

### 1.3 Which EXE Should You Open?

- PokemonBattle.exe
  - Starts the game client only.
  - Use for AI battles.
  - Use to join an online match.

- PokemonBattleHost.exe
  - Starts the game client and battle server together.
  - Use this on the host PC when you want friends on the same Wi-Fi to join.

### 1.4 How To Play

Quick single-player:
1. Open PokemonBattle.exe.
2. Go to New Game.
3. Choose AI opponent.
4. Build your team and start battle.

Local network online battle:
1. On one PC (host), open PokemonBattleHost.exe.
2. Keep the host app running.
3. On other PC(s), open PokemonBattle.exe.
4. In New Game, choose Online opponent.
5. Wait for matchmaking.

Notes:
- AI mode does not require a server.
- Online mode looks for a server on local network.

### 1.5 Common Problems and Easy Fixes

The app does not open:
1. Right-click and run once as administrator.
2. If Windows SmartScreen appears, click More info then Run anyway.

No server found:
1. Make sure the host opened PokemonBattleHost.exe.
2. Make sure all devices are on the same Wi-Fi.
3. Allow the app in Windows Firewall.
4. Keep the host app open while others connect.

Matchmaking keeps waiting:
1. Online mode needs at least two players searching.
2. If you want instant play, select AI mode.

## 2) Developer Quick Start

### 2.1 Tech Stack

- Java 21
- JavaFX 21
- Maven
- SQLite
- Gson
- Java sockets (server-authoritative online battles)

Primary config: pom.xml
Java module config: src/main/java/module-info.java

### 2.2 Build and Run Client (Windows)

From project root:

```powershell
.\mvnw.cmd clean compile
.\mvnw.cmd javafx:run
```

### 2.3 Run Server (Dev)

Default port (5555):

```powershell
mvn exec:java -Dexec.mainClass="com.example.pokemonbattle.server.BattleServer"
```

Custom port:

```powershell
mvn exec:java -Dexec.mainClass="com.example.pokemonbattle.server.BattleServer" -Dexec.args="7777"
```

### 2.4 Build Windows Package (No Java Required On Target Device)

From project root:

```powershell
.\build-windows-package.bat
```

Output:
- Portable app folder: dist/PokemonBattle/
- Client launcher: dist/PokemonBattle/PokemonBattle.exe
- Host launcher: dist/PokemonBattle/PokemonBattleHost.exe
- Optional installer: dist/PokemonBattle-1.0.0.exe (requires WiX toolset)

## 3) Project Structure

```text
src/main/java/com/example/pokemonbattle/
  controller/   UI controllers
  model/        Domain models and battle entities
  database/     Data access and loaders
  security/     Security-related classes
  server/       TCP server and message protocol
  service/      Business logic services
  util/         Shared utilities and managers
  HelloApplication.java
  Launcher.java
  HostLauncher.java

src/main/resources/com/example/pokemonbattle/
  view/         FXML screens
  css/          JavaFX styles
  database/     schema.sql, pokemon/move/item JSON data
  data/         additional data files
  sprites/      Pokemon and trainer assets
  audio/        music and SFX
  fonts/        custom fonts
  assets/       misc assets
```

## 4) Runtime Entry Points

Client:
- com.example.pokemonbattle.Launcher
- com.example.pokemonbattle.HelloApplication

Host mode:
- com.example.pokemonbattle.HostLauncher
- Starts battle server and JavaFX client together.

Dedicated server:
- com.example.pokemonbattle.server.BattleServer

## 5) Feature Ownership Map

Controllers:
- WcController authentication flow
- MenuController main menu navigation
- NewGameController mode/opponent/team setup
- BattleController AI/local battle screen logic
- OnlineBattleController online battle UI updates
- WaitingController network discovery, login, matchmaking wait
- SettingsController settings overlay
- PokemonSelectionOverlayController team selection overlay
- AvatarSelectionController avatar selection
- IntroController, StartController, LoadingScreenController startup transitions

Services:
- AuthService login/signup business logic
- BattleHistoryManager battle history aggregation
- PokemonSearchService pokemon lookup/search helpers

Server:
- BattleServer client connections, queue, active battles
- ClientHandler per-client server thread
- OnlineBattle server-authoritative turn resolution
- ServerConnection client-side network bridge
- GameMessage and subclasses for protocol messaging

Utilities:
- SceneManager scene routing and cross-scene data
- PlayerSession logged-in user/session state
- MusicManager, MediaCache media lifecycle
- battle visual helpers and transition managers

## 6) End-to-End Flow

Startup:
1. Launcher launches HelloApplication.
2. SceneManager initializes and loads startup assets.
3. App enters intro/start flow.

Authentication:
1. WcController captures input.
2. AuthService validates and persists via DB layer.
3. PlayerSession stores active user.

AI Battle:
1. User selects AI opponent in NewGameController.
2. Teams are prepared from game data.
3. BattleController runs battle progression.

Online Battle:
1. User selects Online opponent.
2. WaitingController discovers/connects to server.
3. Login and matchmaking messages are exchanged.
4. Server runs authoritative battle in OnlineBattle.
5. OnlineBattleController renders server updates.

## 7) Data and Ownership Rules

- UI transient state: controllers
- Cross-scene state: SceneManager
- Session identity: PlayerSession
- Online battle authority: server (OnlineBattle)
- Persistent user/history data: SQLite

Rule of thumb:
- Fairness and validation changes for online mode belong on server side.
- Navigation and visuals belong in controllers/FXML/CSS.

## 8) FXML to Controller Map

- wc.fxml -> WcController
- menu.fxml -> MenuController
- new_game.fxml -> NewGameController
- battle.fxml -> BattleController
- online_battle.fxml -> OnlineBattleController
- waiting_online.fxml -> WaitingController
- settings.fxml -> SettingsController
- pokemon_selection_overlay.fxml -> PokemonSelectionOverlayController
- avatar_selection.fxml -> AvatarSelectionController
- intro.fxml -> IntroController
- start.fxml -> StartController
- loading_screen.fxml -> LoadingScreenController

## 9) Core Data Files

- src/main/resources/com/example/pokemonbattle/database/schema.sql
- src/main/resources/com/example/pokemonbattle/database/pokemon_gen4.json
- src/main/resources/com/example/pokemonbattle/database/moves_gen4.json
- src/main/resources/com/example/pokemonbattle/database/battle_items.json
- src/main/resources/com/example/pokemonbattle/data/pokemon_heights.json

## 10) Additional Documentation

- AUTHENTICATION_INTEGRATION.md
- AUTHENTICATION_SUMMARY.md
- AUTH_API_REFERENCE.md
- BATTLE_SETUP_IMPLEMENTATION.md
- TCP_SERVER_README.md
- TCP_SERVER_QUICKSTART.md
- LOADING_SCREEN_INTEGRATION.md
- LOADING_SCREEN_USAGE.md

## 11) README Maintenance Rule

Update this README whenever any of these change:
- Scene flow/navigation
- Controller/service/server ownership
- Message protocol
- Data file locations
- Build/package commands

PR checklist suggestion:
- [ ] README updated for architecture or workflow changes

If you are new to this project, start with:
1. Section 1 (Player Guide) for practical runtime behavior.
2. Sections 3 to 6 for architecture and workflow.
3. Section 10 deep-dive docs for your specific feature area.
