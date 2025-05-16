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
- Hints are sent randomly every 5-20 minutes.
- No database required. All state is in memory.

## For Developers

### Code Structure

- **Main Entry:** `InfoHubPlugin.kt`
- **Utility Classes:**
  - `ServerStats`: Gets server specs & uptime
  - `HintHandler`: Builds and sends color-coded random hints
  - `Color`: Converts random HSL to RGB and hex
  - `PlayerLogger`: Formats messages using MiniMessage
- **Config Classes:**
  - `Config`: Parsed plugin config
  - `HintConfig`: Internal use for hints + emojis

### Dependencies

- [CommandAPI](https://github.com/JorelAli/CommandAPI)
- [Kyori Adventure (MiniMessage)](https://docs.advntr.dev/)
- [oshi](https://github.com/oshi/oshi) (for system info)

### How Hints Work

- Colors: Randomly generated using HSL → RGB → HEX.
- Emojis: Pulled randomly from a list of icons.
- Messages: Styled with gradients and sent using MiniMessage.
- Scheduling:
  - Folia: Uses GlobalRegionScheduler.
  - Non-Folia: Falls back to Bukkit async tasks.

## Contribution Notes

- Keep commands async-safe.
- Follow MiniMessage format conventions.
- Log messages with `PlayerLogger` for consistency.
- Test with both Folia and non-Folia environments if possible.

Got questions? Open an issue!
