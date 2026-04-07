# 📜 Commands & Permissions

SeriaGens provides an extensive command suite for both players and administrators.

---

## 👤 Player Commands

| Command | Alias | Description | Permission |
| :--- | :--- | :--- | :--- |
| `/sgens help` | `/gen` | Show help message | `seriagens.help` |
| `/genshop` | `/gshop` | Open generator shop GUI | `seriagens.shop` |
| `/genset` | `/mygens` | Manage your generators | `seriagens.genset` |
| `/gensell` | `/gsell` | Sell all generator items | `seriagens.sell` |

---

## 🛡️ Admin Commands

| Command | Description | Permission |
| :--- | :--- | :--- |
| `/sgens reload` | Reload the plugin configuration | `seriagens.reload` |
| `/sgens give <player> <type> [amount]` | Give a generator to a player | `seriagens.give` |
| `/sgens sellwand <player> <multiplier> <uses>` | Give a sell wand to a player | `seriagens.wand` |
| `/sgens restore` | Force restore generator blocks | `seriagens.restore` |
| `/sgens corrupt` | (Debug) Force corrupt a generator | `seriagens.admin` |
| `/sgens pickup <player>` | Pick up all generators for a player | `seriagens.admin` |
| `/sgens addslot <player> <amount>` | Add extra generator slots permanently | `seriagens.admin` |
| `/event <start\|stop\|list\|info>` | Manage global generator events | `seriagens.admin` |

---

## 📊 Permission Reference

### General Permissions
- **`seriagens.admin`**: Access to all administrative commands (default: OP).
- **`seriagens.place`**: Allows placing a generator (default: TRUE).
- **`seriagens.break`**: Allows breaking their own generator (default: TRUE).

### Generator Limit Permissions
These permissions define the maximum number of generators a player can have. If multiple permissions are given, the highest value is used.

- **`seriagens.max.10`**: Default limit (default: TRUE).
- **`seriagens.max.vip`**: Increases limit based on `config.yml`.
- **`seriagens.max.premium`**: Second tier.
- **`seriagens.max.mvp`**: Third tier.
- **`seriagens.max.legend`**: Fourth tier.
- **`seriagens.max.titan`**: Fifth tier.
- **`seriagens.max.unlimited`**: Allows placing as many generators as desired.

---

## 🏗️ Command Examples

- **Give a Coal Generator**: `/sgens give PlayerName coal_gens 1`
- **Give an Unlimited 2x Sell Wand**: `/sgens sellwand PlayerName 2.0 -1`
- **Start a Speed Event**: `/event start double_speed`
- **Add 5 Slots to a Player**: `/sgens addslot PlayerName 5`

> [!TIP]
> Use tab-completion for all `/sgens` and `/event` commands to discover sub-commands and arguments!
