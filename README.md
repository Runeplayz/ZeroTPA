# ZeroTPA

ZeroTPA is a lightweight teleport request plugin for Minecraft servers, featuring a GUI-based request system, customizable countdowns, and useful teleport controls.

## Features
- **TPA Requests** — Request to teleport to another player.
- **TPA Here** — Ask another player to teleport to you.
- **Request GUI** — A clean inventory menu with accept and deny buttons.
- **Teleport Countdown** — 5-second countdown before teleporting.
- **Movement Detection** — Teleportation can be cancelled if the player moves.
- **Safe Teleportation** — Searches for a safe destination.
- **Request Cooldown** — Configurable cooldown between requests.
- **Request Expiration** — Requests expire automatically after 60 seconds.
- **Request Limits** — Supports up to 3 outgoing requests by default.
- **Toggle Requests** — Enable or disable incoming teleport requests.
- **Auto Accept** — Automatically accept incoming requests.
- **World Restrictions** — Disable teleport requests in selected worlds.
- **Combat Tagging** — Optional built-in combat tagging.
- **Customizable Messages** — Edit plugin messages, GUI settings, and sounds.
- **Admin Controls** — Reload the plugin configuration with a command.

## Commands

| Command | Description |
|---|---|
| `/tpa <player>` | Request to teleport to a player |
| `/tpa help` | Display help |
| `/tpahere <player>` | Ask a player to teleport to you |
| `/tpaccept [player]` | Accept a teleport request |
| `/tpdeny [player]` | Deny a teleport request |
| `/tpcancel` | Cancel outgoing requests |
| `/tpatoggle` | Toggle incoming requests |
| `/tpauto` | Toggle automatic request acceptance |
| `/tpareload` | Reload the configuration |

**Alias:** `/tprequest` for `/tpa`

## Permissions

| Permission | Default |
|---|---|
| `zerotpa.tpa` | Everyone |
| `zerotpa.tpahere` | Everyone |
| `zerotpa.accept` | Everyone |
| `zerotpa.deny` | Everyone |
| `zerotpa.toggle` | Everyone |
| `zerotpa.bypasscooldown` | Operators |
| `zerotpa.bypassmovement` | Operators |
| `zerotpa.admin` | Operators |

## Configuration

ZeroTPA includes editable configuration files for:

- `config.yml` — Request limits, cooldowns, countdowns, and teleport settings.
- `gui.yml` — Menu title, layout, items, and button positions.
- `messages.yml` — Plugin messages and help text.
- `sounds.yml` — Sound settings.

## Installation

1. Download the ZeroTPA plugin JAR.
2. Place it in your server's `plugins` folder.
3. Restart your server.
4. Configure the plugin files to your preference.

## Compatibility

Designed for Minecraft servers using the Bukkit plugin API, with `api-version: 1.21` declared in the plugin.

## Developer

**Rune Playz** 
**Zero Studios**

Simple tools for better Minecraft servers.
