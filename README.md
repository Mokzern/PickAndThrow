# PickAndThrow Plugin

[中文文档](README_zh-CN.md) | **English**

A feature-rich Minecraft plugin that allows players to pick up and throw entities with advanced customization options.

## ✨ Features

- 🎯 **Pick up & throw** entities and players
- 📚 **Unlimited stacking** - Stack entities on top of each other
- 🎨 **Customizable pickup item** - Set any item (with NBT support) as pickup tool
- 🚫 **Entity filter** - Whitelist/Blacklist system for entity types
- 🎮 **Two throw modes** - Throw one at a time or all at once
- ⚡ **Two power modes** - Fixed velocity or charge-by-sneaking
- 📊 **Two charge displays** - BossBar or ActionBar with rainbow colors
- 🔄 **Charge loop mode** - Precise timing with cycling progress
- 🌍 **Multi-language** - Community-friendly locale system (zh-CN, en-UK, etc.)
- 🔐 **Permission system** - Control who can use the plugin
- 🌐 **Universal compatibility** - Works on Folia, Paper, Spigot, and Bukkit

## 📦 Installation

1. Download the plugin jar
2. Place it in your server's `plugins/` folder
3. Restart the server
4. Configuration files will be auto-generated in `plugins/PickAndThrow/`

## 🎮 How to Use

### Basic Usage

**Default mode** (empty hand):
1. Right-click entity → Pick up
2. Right-click another entity → Stack on top
3. Left-click → Throw

**Charge mode**:
1. Right-click entity → Pick up
2. Hold Shift → Start charging (BossBar/ActionBar shows progress)
3. While holding Shift, Left-click → Throw with current charge power
4. Release Shift and Left-click → Throw with minimum power

### Custom Pickup Item (Optional)

1. Hold any item
2. Use `/pat sethand` → Set as pickup tool
3. Use `/pat sethand` with empty hand → Clear (back to empty hand mode)

## 📝 Commands

All commands require `pickandthrow.admin` permission (default: OP)

| Command | Description |
|---------|-------------|
| `/pat` | Show plugin info and help |
| `/pat reload` | Reload all configuration files |
| `/pat sethand` | Set/clear held item as pickup tool |

## 🔑 Permissions

| Permission | Default | Description |
|------------|---------|-------------|
| `pickandthrow.use` | `true` | Allows using pickup and throw features |
| `pickandthrow.admin` | `op` | Allows using admin commands |

## ⚙️ Configuration

### Main Settings

```yaml
# Language (zh-CN, en-UK, zh-TW, ja-JP, etc.)
language: zh-CN

# Pickup tool (null = empty hand only)
pickup-item: null

# Max entities (-1 = unlimited)
max-entities: -1

# Entity filter mode
entity-filter-mode: blacklist
entity-filter-list:
  - ENDER_DRAGON

# Throw mode: "one" or "all"
throw-mode: one

# Pickup range (blocks)
pickup-range: 4.5

# Throw cooldown (ms)
throw-cooldown: 300

# Power mode: "fixed" or "charge"
throw-power-mode: charge

# Display type: "bossbar" or "actionbar"
charge-display-type: bossbar

# Charge loop (100% -> 0% -> 100%)
charge-loop: false

# BossBar colors
bossbar-colors:
  low: RED       # 0-33%
  medium: YELLOW # 33-66%
  high: GREEN    # 66-100%
```

### Entity Filter Examples

**Blacklist** (prevent picking up bosses):
```yaml
entity-filter-mode: blacklist
entity-filter-list:
  - ENDER_DRAGON
  - WITHER
  - WARDEN
```

**Whitelist** (only allow farm animals):
```yaml
entity-filter-mode: whitelist
entity-filter-list:
  - COW
  - SHEEP
  - PIG
  - CHICKEN
```

### Language System

**Built-in:**
- `zh-CN.yml` - Simplified Chinese
- `en-UK.yml` - English (UK)

**Create custom:**
1. Copy `zh-CN.yml` or `en-UK.yml`
2. Rename (e.g., `ja-JP.yml`, `fr-FR.yml`)
3. Translate messages
4. Set `language: ja-JP` in config
5. Auto-fallback to `en-UK` if not found

Community translations are welcome!

## 🔧 Building from Source

Requires Maven 3.8+ and Java 17+

```bash
mvn clean package
```

Output: `target/PickAndThrow-1.0.0.jar`

## 📖 Technical Details

- **Throw direction**: Follows exact player look direction
- **Charge display**: Updates every tick
  - BossBar: Configurable colors
  - ActionBar: Rainbow gradient with 30 bars
- **Smart raycast**: Handles large entity hitboxes
- **Cooldown system**: Prevents double-trigger issues
- **NBT matching**: Full ItemStack comparison
- **Server detection**: Auto-detects Folia and uses appropriate scheduler API

## 📄 License

This project is licensed under the GNU General Public License v3.0 - see the [LICENSE](LICENSE) file for details.
