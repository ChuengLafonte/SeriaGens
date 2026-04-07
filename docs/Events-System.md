# 📖 The Events System

SeriaGens features a powerful global **Eventsystem** to boost player engagement and drive your economy.

---

## ⚡ Global Multipliers

Events allow you to apply complex modifiers to all online generators and player actions for a set duration.

| Modifier | Description |
| :--- | :--- |
| `Speed` | Reduces the interval (increases the speed) of generators. |
| `Drops` | Multiplies the number of items dropped per cycle. |
| `Sell Value` | Multiplies the amount of money received from selling drops. |
| `Tier Boost` | Temporarily upgrades generators by 1 or more tiers. |
| `Mixed Mode` | Randomizes the drops of all generators. |

---

## 🏗️ Creating Custom Events (`events.yml`)

Each event is defined in the `events.yml` file.

### Example Configuration

```yaml
double_speed:
  display-name: "&#f39c12⚡ DOUBLE SPEED EVENT"
  duration: 3600 # Duration in seconds (1 hour)
  speed-multiplier: 50.0 # 50% faster interval
  drop-multiplier: 1
  sell-multiplier: 1.0
  tier-boost: 0
  mixed-up-mode: false
  start-messages:
    - "&e&lEVENT: &fGenerator speed is now &6DOUBLEED &ffor 1 hour!"
  end-messages:
    - "&e&lEVENT: &fSpeed event has ended. Back to normal production."
```

---

## 🛡️ Managing Events

Administrators can control events using the following sub-commands:

### 1. Start an Event
- `/event start <event_id>`: Launches a pre-defined event from `events.yml`.

### 2. Stop an Active Event
- `/event stop`: Ends the currently running event and sends the end message.

### 3. Event Information
- `/event list`: Shows all available event IDs.
- `/event info`: Shows the current status, active multiplier, and remaining time.

---

## 🎁 Mixed Mode (Experimental)

When `mixed-up-mode` is set to `true`, generators no longer drop their specific items. Instead, every generator on the server will drop a **randomly selected item** from all defined drops in `generators.yml`. This is perfect for high-chaos, limited-time events!

> [!TIP]
> Use `sell-multiplier` during holidays or weekends to encourage players to sell their stored-up generator drops!
