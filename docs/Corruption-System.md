# 📖 The Corruption System

The **Corruption System** in SeriaGens is a mechanical breakage mechanic that introduces a sense of maintenance and progression to your Tycoon server.

---

## ⚙️ How It Works

Generators have a small probability of becoming "corrupted" (broken) during periodic waves.

- **Check Frequency**: The system runs a "Corruption Wave" every `X` minutes (default: 180m).
- **Offline Protection**: Corruption only affects players who are **currently online**.
- **Yield Stoppage**: A corrupted generator **will stop producing items** until it is repaired.
- **Repair Costs**: Each generator tier has a specific `repair_cost` defined in `generators.yml`.

---

## ⚠ Mechanical Breakdown

When a generator becomes corrupted, several things happen:
1.  **Visual Change**: If **FancyHolograms** is installed, a warning hologram appears above the generator.
2.  **Notification**: The player receives an alert message and an optional sound effect.
3.  **Interaction**: Left-clicking or right-clicking the block prompts the user to enter the repair menu.

---

## 🛠️ Repairing Generators

Players can repair their generators in two ways:
- **Direct Interaction**: Right-click the corrupted generator block and click the **Repair** button in the `/genset` GUI.
- **Command Management**: Open `/genset` from anywhere to see a list of all your generators and their statuses.

> [!IMPORTANT]
> Repairing requires the **SeriaGens Repair Cost** from the player's economy balance (Vault).

---

## 🔧 Configuration (`config.yml`)

You can tune the system for your own server:

```yaml
corruption:
  enabled: true
  interval: 180      # Waves run every 3 hours
  percentage: 10     # Each wave, 10% of online generators are tested
  broadcast:         # Warn the server when a wave occurs
    - '&c&l⚠ POWER OUTAGE'
    - '  &fDue to voltage spikes, &c{amount} &fgenerators'
    - '  &fhave been corrupted! Repair them now!'
```

---

## 🔗 Hologram Integration

If you use **FancyHolograms**, SeriaGens automatically creates dynamic floating text above corrupted blocks. This provides an immersive "factory floor" feel to your tycoon.

```yaml
hologram:
  corruption:
    enabled: true
    lines:
      - "&c&l⚠ GENERATOR RUSAK!"
      - "&7Cost: &c●{cost}"
    height: 2.0
```

> [!TIP]
> Higher tier generators generally have higher corruption chances (5% to 10%) but also higher repair costs, reflecting their increased mechanical complexity.
