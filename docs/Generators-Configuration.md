# 📖 Configuring Generators

This guide will help you define your own custom generator tiers and configure their progression.

---

## 🛠️ The `generators.yml` File

Each generator is defined as a top-level key in the `generators.yml` file.

### Basic Configuration Example

```yaml
iron_gens:
  display-name: "&#bdc3c7⛏ Iron Generator"
  interval: 45
  max-joules: 500
  corrupted:
    enabled: true
    cost: 100
    chance: 5.0
  upgrade:
    next-tier: copper_gens
    cost: 5000
  upgrade-requirements:
    '0':
      type: PLACEHOLDER
      message: "&c&l⚠ ERROR: &fRequires Mining Level 10 to upgrade!"
      placeholder: "%auraskills_power%"
      value: "10"
  item:
    material: iron_ore
    display-name: "&#bdc3c7&l⛏ IRON GENERATOR"
    lore:
      - "&8Tier II — Industrial Metal"
  drops:
    '0':
      chance: 100
      sell-value: 4
      item:
        material: light_gray_dye
        display-name: "&#afb5a8Iron Chunk"
```

---

## 📋 Field Definitions

| Field | Type | Description |
| :--- | :--- | :--- |
| `display-name` | String | The name shown in GUIs and holograms. |
| `interval` | Integer | The frequency of item drops in ticks (20 ticks = 1 second). |
| `max-joules` | Integer | Maximum power capacity for this generator tier. |
| `corrupted` | Section | Configuration for the mechanical corruption system. |
| `upgrade` | Section | Defines the cost and ID of the next tier. |
| `upgrade-requirements` | List | Optional prerequisites for upgrading (e.g., skill levels). |
| `item` | Section | Defines the visual appearance of the generator as a block. |
| `drops` | List | A collection of weighted item drops. |

---

## 📈 Upgrade Requirements

The `upgrade-requirements` section allows you to use **PlaceholderAPI** to create complex progression. In the example above, a player needs **Mining Level 10** (using AuraSkills) to upgrade their Iron Generator to Copper.

- **`type`**: Currently only `PLACEHOLDER` is supported.
- **`placeholder`**: Any valid PlaceholderAPI string.
- **`value`**: The target value for comparison.
- **`message`**: The error message shown to the player if the requirement is not met.

---

## 🪙 Configurable Drops

The `drops` section uses a weight-based system. Each drop has a `chance` (weight), a `sell-value` (sell price), and a full `item` definition (material, name, lore).

- If the total chance of all drops is 100, the individual values represent percentages.
- Items are spawned **above the generator block** in the world.
- NBT data is automatically added to drops to ensure high-performance selling.

---

## ⚡ Global Power Consumption

Each generator consumes **joules** based on its configuration in `fuel.yml`, relative to the player's total energy capacity. Make sure the `max-joules` setting is sufficient for the energy grid's demands.

> [!TIP]
> Use shorter intervals for higher tiers (e.g., 20s or 15s) to make them feel significantly more powerful than the base Coal tiers (50s).
