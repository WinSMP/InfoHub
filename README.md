# InfoHub

**InfoHub** is a lightweight Paper plugin that provides utility commands for server information, including player ping, hints, and more.

## Features (Overview)

- `/specs`: View system specs (CPU, RAM, OS).
- `/uptime`: Check how long the server has been running.
- `/ping [player]`: Check player ping.
- `/discord`, `/rules`, `/helpme`: General informational commands.
- `/hint enable|disable`: Control hint display per player.
- Periodic random hints with color + emoji support.
- Compatible with Folia via async scheduler fallback.
- Persistent storage with SQLite, MySQL, or PostgreSQL.
- Optional Redis caching for player preferences.

## For Server Admins

### Config Setup (`config.yml`)

Here are the keys you can configure:

```yaml
discord-link: "https://discord.gg/yourserver"
rules:
  - "Be respectful"
  - "No griefing"
  - "No cheating"
help-message: "Use /discord, /rules, or /help for more information!"
warn-user-ping: true
hint-list:
  - "Use /spawn to return to the hub."
  - "Remember to set a home with /sethome!"
storage:
  backupInterval: 5m # Optional: Only for MySQL and PostgreSQL
  redis:
    enabled: false
    uri: "redis://localhost:6379"
  database:
    type: "SQLITE" # SQLITE, MYSQL, or POSTGRESQL
    host: "localhost"
    port: 3306
    database: "minecraft"
    username: "root"
    password: "password"
    table: "player_choices"
```

### Commands for Players

| Command       | Description                          |
|---------------|--------------------------------------|
| `/discord`    | Shows your Discord link              |
| `/rules`      | Lists rules from config              |
| `/helpme`     | Displays the custom help message     |
| `/hint enable/disable` | Toggle personal hints   |
| `/ping [player]` | Shows ping (optional target)      |
| `/specs`      | Shows server hardware specs          |
| `/uptime`     | Shows server uptime since plugin load|

### Notes

- The plugin auto-detects Folia and uses its scheduler if available.
- Hints are sent randomly every 10-25 minutes.
- Player hint preferences are stored in a database (SQLite by default).
- Redis can be enabled for caching to reduce database load.

## For Developers

### Code Structure

- **Main Entry:** `InfoHubPlugin.kt`
- **Cache:** `PlayerCache.kt` (interface), `InMemoryPlayerCache.kt`, `RedisPlayerCache.kt`
- **Storage:** `HintPreferenceRepository.kt` (interface), `SqliteHintPreferenceRepository.kt`, `JdbcHintPreferenceRepository.kt`, `CachedHintPreferenceRepository.kt`
- **Utility Classes:**
  - `ServerStats`: Gets server specs & uptime
  - `HintHandler`: Builds and sends color-coded random hints
  - `Color`: Converts random HSL to RGB and hex
  - `PlayerLogger`: Formats messages using MiniMessage
- **Config Classes:**
  - `MainConfig`: Parsed plugin config
  - `StorageConfig`, `DatabaseConfig`, `RedisConfig`: Storage configuration
  - `HintConfig`: Internal use for hints + emojis

### Dependencies

- [CommandAPI](https://github.com/JorelAli/CommandAPI)
- [Kyori Adventure (MiniMessage)](https://docs.advntr.dev/)
- [oshi](https://github.com/oshi/oshi) (for system info)
- [HikariCP](https://github.com/brettwooldridge/HikariCP) (for database connection pooling)
- [Lettuce](https://lettuce.io/) (for Redis)

### How Hints Work

- Colors: Randomly generated using HSL → RGB → HEX.
- Emojis: Pulled randomly from a list of icons.
- Messages: Styled with gradients and sent using MiniMessage.
- Scheduling:
  - Folia: Uses async tasks.
  - Non-Folia: Falls back to Bukkit async tasks.

## Contribution Notes

- Keep commands async-safe.
- Follow MiniMessage format conventions.
- Log messages with `PlayerLogger` for consistency.
- Test with both Folia and non-Folia environments if possible.

Got questions? Open an issue!