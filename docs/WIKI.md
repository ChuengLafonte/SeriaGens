# SeriaGens Wiki

Documentation for managing generators and events in SeriaGens.

## 🛠 Commands
| Command | Alias | Permission | Description |
|---------|-------|------------|-------------|
| `/genshop` | `/gshop` | `seriagens.shop` | Open generator shop |
| `/genset` | `/mygens` | `seriagens.genset` | View/Manage your generators |
| `/gensell` | `/gsell` | `seriagens.sell` | Sell inventory gen items |
| `/genevent start <id>` | | `seriagens.admin` | Start a generator event |

## 🔑 Permissions
- `seriagens.admin`: Full access to all commands.
- `seriagens.max.<number>`: Set custom generator limits for players.
- `seriagens.shop`: Access to the `/genshop`.

## 📊 Placeholders
Integrated with PlaceholderAPI:
- `%seriagens_count%`: Current number of generators placed.
- `%seriagens_max%`: Maximum generators allowed.
- `%seriagens_event_active%`: Returns 'Aktif' or 'Tidak Ada'.
- `%seriagens_total_joules%`: Total energy in the player's global grid.

## ⚙️ Configuration

### generators.yml
```yaml
generators:
  COAL:
    display_name: "<gray>Coal Generator"
    item: COAL
    production_speed: 10 # Seconds
    joules_per_cycle: 1
    max_level: 5
```

### fuel.yml
```yaml
fuel:
  COAL_FUEL:
    item: COAL
    joules_value: 10
    efficiency: 1.0
```

## ⚡ Energy System (Joules)
Generators in SeriaGens operate on a Joules system.
- Some generators **produce** Joules as a byproduct.
- High-tier generators **consume** Joules to maintain 100% efficiency.
- If a player runs out of Joules in their global grid, production speeds are halved.
