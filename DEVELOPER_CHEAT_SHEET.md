# Developer Cheat Sheet - PokemonBattle

Use this as a fast map when changing features during development or interviews.

## ENTRY POINT

### Client App (JavaFX)
- Main launcher: src/main/java/com/example/pokemonbattle/Launcher.java
- JavaFX app class: src/main/java/com/example/pokemonbattle/HelloApplication.java
- First scene loaded: intro.fxml

### Online Server (TCP)
- Server class: src/main/java/com/example/pokemonbattle/server/BattleServer.java
- Server launcher: src/main/java/com/example/pokemonbattle/server/PokemonBattleServerLauncher.java
- Default port: 5555

## MAIN UI FLOW

Client flow:
1. HelloApplication starts and initializes SceneManager.
2. intro.fxml -> start.fxml -> wc.fxml (login/signup).
3. wc.fxml -> menu.fxml.
4. menu.fxml -> new_game.fxml.
5. new_game.fxml:
- AI battle path: battle.fxml
- Online/local-player TCP path: waiting_online.fxml -> online_battle.fxml

Core FXML map:
- intro.fxml -> IntroController
- start.fxml -> StartController
- wc.fxml -> WcController
- menu.fxml -> MenuController
- new_game.fxml -> NewGameController
- battle.fxml -> BattleController
- waiting_online.fxml -> WaitingController
- online_battle.fxml -> OnlineBattleController
- settings.fxml -> SettingsController
- pokemon_selection_overlay.fxml -> PokemonSelectionOverlayController
- avatar_selection.fxml -> AvatarSelectionController
- loading_screen.fxml -> LoadingScreenController

## SCENE SWITCHING

Central scene switch utility:
- src/main/java/com/example/pokemonbattle/util/SceneManager.java

Main APIs:
- switchScene(...): direct switch
- switchSceneWithData(...): switch + pass data to next scene
- switchSceneWithLoading(...): show loading_screen.fxml first, then target scene
- getData(key)/setData(key, value): cross-scene data map

Where scene transitions are triggered:
- Startup: HelloApplication
- Intro/start/auth/menu/new game controllers
- Battle controllers (go back/restart)
- WaitingController (when match is found)

## CONTROLLERS

Directory:
- src/main/java/com/example/pokemonbattle/controller/

Key responsibility split:
- IntroController, StartController, LoadingScreenController: startup and transitions
- WcController: login/signup UI + AuthService handoff
- MenuController: main navigation + settings overlay
- NewGameController: battle setup, team generation/selection, route to AI or online
- BattleController: local battle UI and battle progression callbacks
- WaitingController: server connect/login/matchmaking waiting logic
- OnlineBattleController: server-authoritative online battle UI + chat + forfeit flow
- SettingsController: toggles, volume, language, sign out
- PokemonSelectionOverlayController: custom team overlay search/select
- AvatarSelectionController: avatar picker overlay

## DATA MODELS

Directory:
- src/main/java/com/example/pokemonbattle/model/

Most-used models:
- User: authenticated account identity
- Player: trainer + active team state
- PokemonSpecies: static species data
- PokemonInstance: runtime Pokemon state (HP, level, moves)
- Move: move metadata and battle properties
- Battle: local battle engine + listener callbacks
- BattleRecord: persisted history entry

Data loading/persistence:
- src/main/java/com/example/pokemonbattle/database/
- SQLite schema/data JSON: src/main/resources/com/example/pokemonbattle/database/

## EVENT HANDLING

### UI Events (JavaFX)
- FXML handlers use onAction="#methodName" and @FXML methods in controllers.
- Many buttons are also wired programmatically with setOnAction(...) inside initialize().
- Overlay click-to-close is handled with background click checks (target == root overlay).

### Battle Events (Local)
- BattleController implements Battle.BattleListener.
- Important callbacks: damage dealt, switch, faint, heal, battle end.

### Network Events (Online)
- OnlineBattleController receives GameMessage updates from ServerConnection.
- Typical messages: DAMAGE, SWITCH_NOTIFY, FORCE_SWITCH, TURN_READY, BATTLE_END, BATTLE_CHAT.
- Server is authoritative for online state updates.

## QUICK INSTRUCTIONS

To change UI -> edit src/main/resources/com/example/pokemonbattle/view/*.fxml and src/main/resources/com/example/pokemonbattle/css/*.css, then adjust matching controller fields/handlers in src/main/java/com/example/pokemonbattle/controller/.

To change logic -> edit:
- Local battle rules: src/main/java/com/example/pokemonbattle/model/Battle.java
- Screen behavior: src/main/java/com/example/pokemonbattle/controller/*.java
- Auth/session/services: src/main/java/com/example/pokemonbattle/service/*.java and src/main/java/com/example/pokemonbattle/util/*.java
- Online authority/protocol: src/main/java/com/example/pokemonbattle/server/*.java

To add feature -> modify:
1. FXML + CSS for UI
2. Controller handlers/workflow
3. Model/service/server layers for business logic
4. SceneManager route if new screen is introduced
5. Data files/DAO if new persisted or static game data is required

## Fast File Targets

- New screen: src/main/resources/com/example/pokemonbattle/view/
- Navigation changes: src/main/java/com/example/pokemonbattle/util/SceneManager.java
- Battle setup rules: src/main/java/com/example/pokemonbattle/controller/NewGameController.java
- Local battle behavior: src/main/java/com/example/pokemonbattle/controller/BattleController.java and src/main/java/com/example/pokemonbattle/model/Battle.java
- Online battle behavior: src/main/java/com/example/pokemonbattle/controller/OnlineBattleController.java and src/main/java/com/example/pokemonbattle/server/OnlineBattle.java
- Matchmaking/connectivity: src/main/java/com/example/pokemonbattle/controller/WaitingController.java and src/main/java/com/example/pokemonbattle/server/BattleServer.java
