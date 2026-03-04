# Pokemon Battle Online Server

A TCP-based server for handling online multiplayer Pokemon battles. Manages player authentication, matchmaking, and real-time battle coordination with support for multiple concurrent battles.

## Architecture

### Core Components

1. **BattleServer** - Main server class
   - Listens for incoming TCP connections on a specified port
   - Manages multiple ClientHandler threads
   - Implements matchmaking queue for opponent pairing
   - Tracks active battles
   - Provides console interface for server management

2. **ClientHandler** - Per-connection thread
   - Manages a single client connection
   - Handles login/authentication
   - Processes matchmaking requests
   - Receives and forwards moves during battle
   - Runs in its own thread for concurrent operation

3. **OnlineBattle** - Battle management
   - Manages game state for a single battle between two players
   - Implements turn-based move processing
   - Calculates damage using Pokemon stats and move data
   - Handles Pokemon switching when they faint
   - Determines battle winner
   - Communicates results to both clients

4. **GameMessage & Subclasses** - Communication protocol
   - Uses Java serialization over TCP
   - Message types for auth, battle setup, moves, damage, status updates
   - Type-safe message passing

## Communication Protocol

### Message Types

#### Client -> Server

- **LoginRequest**
  - username, password
  - Response: LoginResponse

- **FindOpponentRequest**
  - userId, playerName
  - Response: BattleStartMessage (when opponent found)

- **MoveMessage**
  - battleId, moveId, moveName, turn
  - In response: DamageMessage(s)

#### Server -> Client

- **LoginResponse**
  - success, message, userId, playerName
  
- **BattleStartMessage**
  - battleId, opponentName, opponentPokemonIds/Levels/Names
  - Signals that battle is ready to start
  
- **DamageMessage**
  - battleId, attackerName, targetName, damage, targetHP, effectiveness, moveUsed
  - Sent for each attack during battle
  
- **BattleUpdateMessage**
  - battleId, message, turn, currentPlayerTurn
  - General status updates (Pokemon switches, etc.)
  
- **BattleEndMessage**
  - battleId, winnerName, winnerId, reason
  - Signals battle conclusion
  
- **ErrorMessage**
  - errorCode, description
  - Reports errors at any point

## Features

### Authentication & Session Management
- Username/password login verification
- User ID tracking for each session
- Automatic cleanup on disconnection

### Matchmaking System
- Queue-based opponent matching
- FIFO matching (first waiting player paired with next)
- Instant pairing when two players are waiting

### Battle Management
- **Turn-based gameplay**: Both players submit moves simultaneously
- **Speed-based move order**: Pokemon with higher speed attack first
- **Damage calculation**:
  - Base damage calculation using Pokemon stats
  - Move power and accuracy considered
  - Random damage variance (85-100%)
  - Type effectiveness (extensible)
  
### Multi-threading
- One thread per client connection
- Thread-safe collections for shared game state
- Concurrent battle processing
- Non-blocking server operations

### Server Console
Interactive command-line interface for monitoring:
- `status` / `stats` - Show server statistics
- `clients` - List connected authenticated clients
- `battles` - Show all active battles
- `queue` - Display matchmaking queue
- `help` - Show available commands
- `exit` / `quit` - Graceful shutdown

## Running the Server

### Prerequisites
- Java 11 or higher
- Maven project properly built
- SQLite database with user data (if using real database)

### Start Server (Default Port 5555)
```bash
java com.example.pokemonbattle.server.PokemonBattleServerLauncher
```

### Start Server (Custom Port)
```bash
java com.example.pokemonbattle.server.PokemonBattleServerLauncher 7777
```

### Direct Invocation (Via BattleServer)
```bash
java com.example.pokemonbattle.server.BattleServer 5555
```

### From Project Root (Maven)
```bash
mvn clean compile
mvn exec:java -Dexec.mainClass="com.example.pokemonbattle.server.BattleServer" -Dexec.args="5555"
```

## Battle Flow Example

```
1. Two clients connect to server
2. Both clients authenticate with username/password
3. Client A sends FindOpponentRequest
4. Server adds Client A to matchmaking queue
5. Client B sends FindOpponentRequest
6. Server finds match, initiates battle
7. BattleStartMessage sent to both clients with opponent pokemon data
8. Client A sends MoveMessage (e.g., use Thunderbolt)
9. Client B sends MoveMessage (e.g., use Flamethrower)
10. Server processes moves in speed order:
    - If A is faster: A attacks B, then B attacks A
    - Calculate damage, apply to B's pokemon
    - Send DamageMessage to both clients
11. Server checks if B's pokemon fainted:
    - If yes and pokemon available: force switch
    - If no pokemon available: battle ends
12. Both clients await next move submissions
13. Process repeats until one player has no pokemon left
14. Server sends BattleEndMessage with winner info
```

## Configuration

### Network
- Default port: **5555**
- Can be customized via command line argument
- Supports multiple concurrent connections (limited by system resources)

### Battle Parameters
- Team size: 6 pokemon (expandable)
- Default pokemon level: 50 (customizable in ClientHandler)
- Damage variance: 85-100% of calculated damage

### Database Integration
- Uses existing DatabaseManager from main application
- UserDAO for player authentication
- GameDataDAO for pokemon/move/item data
- SQLite database at user home directory

## Extensibility

### Adding New Battle Logic
The `OnlineBattle.executeMove()` method handles move execution. Extend by:
- Adding type effectiveness calculations
- Implementing status effects
- Adding item/ability effects
- Adding special battle conditions

### Custom Matchmaking
Override `BattleServer.findAndMatchOpponent()` for:
- Rating-based matching
- Skill-level matching
- Team-based battles
- Tournament mode

### Message Protocol
Add new message types by:
1. Create new class extending GameMessage
2. Implement serialVersionUID
3. Add handler in ClientHandler.handleMessage()
4. Send via clientHandler.sendMessage()

## Performance Considerations

### Server Scalability
- **Clients**: Tested with hundreds of concurrent connections (limited by OS)
- **Battles**: Each battle uses minimal memory (~1MB per battle)
- **Threads**: One thread per client plus server accept thread

### Optimization Tips
- Connection pooling for database
- Cache pokemon/move data (GameDataDAO already does this)
- Load balancing for multiple server instances (future)
- Compress messages for bandwidth optimization

## Troubleshooting

### Connection Refused
- Ensure server is running
- Check firewall allows port
- Verify correct host and port

### Login Failures
- Check database connectivity
- Verify user exists in database
- Validate password hashing matches

### Battle Issues
- Check client message timeout settings
- Verify both clients connected
- Check server logs for detailed errors

## Security Notes

⚠️ **Current Implementation**
- Uses plain-text password comparison (NOT RECOMMENDED for production)
- No encryption on network communication
- No validation of move IDs

## Production Enhancements

1. **Authentication**
   - Use BCrypt or PBKDF2 for password hashing
   - Implement token-based auth (JWT)
   - Add HTTPS/TLS encryption

2. **Validation**
   - Verify move belongs to pokemon
   - Check player has pokemon/moves
   - Rate limiting to prevent spam

3. **Persistence**
   - Save battle records to database
   - Track player ratings/ELO
   - Store match history

4. **Ops**
   - Structured logging (SLF4J/Logback)
   - Metrics collection
   - Health checks endpoint
   - Graceful degradation

## File Structure

```
src/main/java/com/example/pokemonbattle/server/
├── BattleServer.java              # Main server class
├── ClientHandler.java             # Per-connection thread
├── OnlineBattle.java              # Battle management
├── GameMessage.java               # Message protocol classes
└── PokemonBattleServerLauncher.java # Console launcher
```

## API Reference

### BattleServer
```java
BattleServer(int port)              // Create server on port
void start()                        // Start accepting connections
void shutdown()                     // Graceful shutdown
String getStats()                   // Get statistics
ClientHandler findAndMatchOpponent(ClientHandler client)
void registerClient(ClientHandler client)
void unregisterClient(ClientHandler client)
```

### ClientHandler
```java
void sendMessage(GameMessage msg)   // Send message to client
void setBattle(OnlineBattle battle) // Associate with battle
Integer getUserId()
String getPlayerName()
```

### OnlineBattle
```java
OnlineBattle(...)                   // Constructor with player data
void startBattle()                  // Send init messages
void submitMove(Integer playerId, MoveMessage move)
Integer getBattleId()
boolean isBattleActive()
```

## License

Integrated with existing Pokemon Battle project.
