# 📊 PlaceholderAPI Support

SeriaGens provides a comprehensive list of placeholders for **PlaceholderAPI** that can be used in your holographic displays, menus, chat, and tab-lists.

---

## ⚡ Global Placeholders

These placeholders display general information about the generator system or the player's total stats.

| Placeholder | Description |
| :--- | :--- |
| `%seriagens_count%` | Current number of generators placed by the player. |
| `%seriagens_max%` | The player's maximum allowance of generators. |
| `%seriagens_active_event%` | The name of the currently running global event. |
| `%seriagens_event_time%` | Remaining time for the current event (MM:SS). |

---

## 🏗️ Ranking and Economy

Use these placeholders to show progress and capacity.

| Placeholder | Description |
| :--- | :--- |
| `%seriagens_multiplier%` | The active global sell/drop multiplier. |
| `%seriagens_joules%` | The player's total energy capacity across all generators. |
| `%seriagens_extra_slots%` | The number of permanent bonus slots added via commands. |

---

## 🔧 How to Use

Simply install **PlaceholderAPI** and ensure **SeriaGens** is running. No separate expansion download is required, as the plugin registers its own expansion automatically.

### Example Usage:

- **Bologram Above Player**: `"Gens: %seriagens_count%/%seriagens_max%"`
- **Scoreboard**: `"Current Event: %seriagens_active_event%"`
- **Tab List**: `"⚡ %seriagens_joules% J"`

---

## 🔗 External Placeholders

SeriaGens also supports reading placeholders from other plugins in the `generators.yml` **Upgrade Requirements** section, including:
- **`%auraskills_power%`**
- **`%player_level%`**
- **`%statistic_hours_played%`**

> [!TIP]
> Use these placeholders to create rewards and rank-up requirements for your Generators!
