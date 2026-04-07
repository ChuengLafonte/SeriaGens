# 📖 Installation & Setup

Installing **SeriaGens** is straightforward, but it requires a few essential dependencies to function correctly.

---

## 🏗️ Requirements

### Mandatory Dependencies
- **[Vault](https://www.spigotmc.org/resources/vault.3431/)**: Required for all currency-related features (buying and selling generators).
- **Economy Provider**: An economy plugin like [EssentialsX](https://www.spigotmc.org/resources/essentialsx.9089/) is required for Vault to work.
- **Java 21**: The plugin is built with Java 21 features and will not run on older versions.

### Recommended (Optional) Dependencies
- **[BentoBox](https://github.com/BentoBoxWorld/BentoBox)**: For Skyblock island protection and management.
- **[PlaceholderAPI](https://www.spigotmc.org/resources/placeholderapi.6245/)**: For custom placeholders in menus and chat.
- **[FancyHolograms](https://www.spigotmc.org/resources/fancyholograms.107247/)**: For floating text above corrupted generators.
- **[ShopGUI+](https://www.spigotmc.org/resources/shopgui.6515/)**: For falling back to item prices defined in your shop.

---

## 🚀 Step-by-Step Installation

1.  **Download Dependencies**: Download and install Vault and your economy plugin into your `/plugins` folder.
2.  **Upload SeriaGens**: Place the `SeriaGens.jar` file into your server's `/plugins` directory.
3.  **Start Server**: Launch your server. This will generate the default configuration files.
4.  **Database Configuration**:
    *   By default, the plugin uses **SQLite** (no setup required).
    *   To use **MySQL**, edit `config.yml` and set `type: MYSQL`, then fill in your database credentials.
5.  **Restart/Reload**: If you changed any configurations, restart the server or use `/sgens reload`.

---

## ⚙️ Initial Configuration

Once the plugin is installed, review the following files:
- `config.yml`: Global settings like database, corruption, and BentoBox integration.
- `generators.yml`: Definitions of each generator tier.
- `fuel.yml`: Fuel items and their joule values.
- `gui.yml`: Customization of all menu interfaces.
- `command.yml`: Customizable messages for all commands.

---

## 🔐 Permissions Setup

Make sure to give your players the basic permissions:
- `seriagens.shop`: Access to `/genshop`.
- `seriagens.place`: Ability to place generators.
- `seriagens.break`: Ability to break their own generators.
- `seriagens.genset`: Access to `/genset` management menu.
- `seriagens.sell`: Access to `/gensell`.
