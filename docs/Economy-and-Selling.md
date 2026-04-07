# 📖 Economy & Selling

The **Economy System** in SeriaGens is designed for high-performance and flexibility, using Vault as the primary provider and a highly efficient NBT-based selling system.

---

## ⚡ The Selling System

SeriaGens provides multiple ways for players to convert their generator drops into currency.

### 1. The Sell Wand
A customizable tool to sell the contents of any container (chest, hopper, barrel, etc.) with a single click.

- **`Uses`**: Each wand can have a set number of uses or be set to "Unlimited".
- **`Multiplier`**: Wands can have as many as 3x or 4x multipliers, which stack with global events.
- **`Item`**: Define the wand's material and name in `config.yml`.

### 2. `/gensell` Command
A rapid sell command that scans the player's inventory and sells all recognized generator products instantly.

### 3. Integrated Pricing
Prices for each generator drop are defined in `generators.yml`.

```yaml
drops:
  '0':
    sell-value: 1 # Value per unit
    item:
      material: black_dye
```

---

## 🔗 High-Performance NBT Selling

Unlike traditional plugins that scan item names or lore (which are slow and prone to bugs), SeriaGens uses **NBT Tags** to store the value of drops.

- **`seriagens_value`**: Every item spawned by a generator has its price embedded in a persistent data container.
- **Accuracy**: This ensures that 100% of items are sold at the correct price, regardless of renames or lore changes.
- **Speed**: The sell engine is virtually instantaneous, even for large inventories.

---

## 🛍️ Price Fallback (ShopGUI+)

If an item in a chest **does not** have a SeriaGens value (e.g., vanilla items), the plugin can optionally fall back to **[ShopGUI+](https://www.spigotmc.org/resources/shopgui.6515/)** pricing.

- Enable this in `config.yml` under the sellwand section.
- This allows players to use the Sell Wand to empty entire mixed chests.

---

## 🔐 Permissions Reference

| Permission | Description | Default |
| :--- | :--- | :--- |
| `seriagens.sell` | Use the `/gensell` command | Player |
| `seriagens.wand` | Use and craft/buy the Sell Wand | Player |
| `seriagens.admin` | Give sell wands with custom multipliers | Staff |

> [!TIP]
> Use `/sgens sellwand <player> <multiplier> <uses>` to create custom wands for ranks or rewards!
