# PickAndThrow 插件

**中文** | [English](README.md)

一个功能丰富的 Minecraft 插件，允许玩家举起并抛出生物，支持高级自定义选项。

## ✨ 功能特性

- 🎯 **举起和抛出** 生物和玩家
- 📚 **无限叠加** - 将生物叠罗汉
- 🎨 **自定义举起工具** - 设置任何物品（支持完整NBT）作为举起工具
- 🚫 **生物过滤器** - 白名单/黑名单系统控制可举起的生物
- 🎮 **两种抛出模式** - 一次抛一个或一次抛全部
- ⚡ **两种力度模式** - 固定速度或蓄力抛出
- 📊 **两种蓄力显示** - Boss血条或动作栏（彩虹色）
- 🔄 **蓄力循环模式** - 进度条循环往复，精准把控时机
- 🌍 **多语言支持** - 社区友好的语言文件系统（zh-CN, en-UK 等）
- 🔐 **权限系统** - 控制谁可以使用插件
- 🌐 **全服务器兼容** - 支持 Folia、Paper、Spigot 和 Bukkit

## 📦 安装

1. 下载 `PickAndThrow-1.0.0.jar`
2. 放入服务器的 `plugins/` 文件夹
3. 重启服务器
4. 配置文件将自动生成在 `plugins/PickAndThrow/` 目录

**支持的服务器：**
- Folia 1.20+
- Paper 1.20+
- Spigot 1.20+
- Bukkit 1.20+

## 🎮 使用方法

### 基础操作

**默认模式**（空手）：
1. 右键生物 → 举起
2. 右键另一个生物 → 叠加到头上
3. 左键 → 抛出

**蓄力模式**：
1. 右键生物 → 举起
2. 按住Shift → 开始蓄力（Boss血条/动作栏显示进度）
3. 按住Shift的同时左键 → 用当前蓄力值抛出
4. 松开Shift后左键 → 用最小力度抛出（0%蓄力）

### 蓄力模式操作流程

1. 按住Shift → 开始蓄力（显示进度条）
2. **保持按住Shift的同时**左键 → 用当前蓄力值抛出
3. 松开Shift后左键 → 用最小力度抛出（0%蓄力）

💡 **技巧**：想要最大力度，就按住Shift等到100%再左键！

### 自定义举起工具（可选）

1. 手持任何物品（支持带NBT的自定义物品）
2. 使用 `/pat sethand` → 设置为举起工具
3. 空手使用 `/pat sethand` → 清除设置（恢复空手模式）

## 📝 命令

所有命令需要 `pickandthrow.admin` 权限（默认：OP）

| 命令 | 说明 |
|------|------|
| `/pat` | 显示插件信息和帮助 |
| `/pat reload` | 重载所有配置文件 |
| `/pat sethand` | 设置/清除手持物品为举起工具 |

## 🔑 权限节点

| 权限 | 默认值 | 说明 |
|------|--------|------|
| `pickandthrow.use` | `true` | 允许使用举起和抛出功能 |
| `pickandthrow.admin` | `op` | 允许使用管理命令 |

## ⚙️ 配置

### 主要设置

```yaml
# 语言文件（zh-CN, en-UK, zh-TW, ja-JP 等）
language: zh-CN

# 举起工具（null = 仅空手）
pickup-item: null

# 最大叠加数量（-1 = 无限制）
max-entities: -1

# 生物过滤模式
entity-filter-mode: blacklist
entity-filter-list:
  - ENDER_DRAGON

# 抛出模式："one"（一个个）或 "all"（全部）
throw-mode: one

# 抛出冷却时间（毫秒）
throw-cooldown: 300

# 力度模式："fixed"（固定）或 "charge"（蓄力）
throw-power-mode: charge

# 显示类型："bossbar"（血条）或 "actionbar"（动作栏）
charge-display-type: bossbar

# 蓄力循环（100% -> 0% -> 100%）
charge-loop: false

# Boss血条颜色
bossbar-colors:
  low: RED       # 0-33%
  medium: YELLOW # 33-66%
  high: GREEN    # 66-100%
```

### 生物过滤示例

**黑名单**（禁止举起BOSS）：
```yaml
entity-filter-mode: blacklist
entity-filter-list:
  - ENDER_DRAGON  # 末影龙
  - WITHER        # 凋灵
  - WARDEN        # 监守者
```

**白名单**（只允许农场动物）：
```yaml
entity-filter-mode: whitelist
entity-filter-list:
  - COW      # 牛
  - SHEEP    # 羊
  - PIG      # 猪
  - CHICKEN  # 鸡
```

### 语言系统

**内置语言：**
- `zh-CN.yml` - 简体中文
- `en-UK.yml` - 英文（英国）

**创建自定义语言：**
1. 复制 `zh-CN.yml` 或 `en-UK.yml`
2. 重命名（例如：`zh-TW.yml`, `ja-JP.yml`, `fr-FR.yml`）
3. 翻译所有消息
4. 在 config.yml 中设置 `language: zh-TW`
5. 如果找不到文件，自动回退到 `en-UK`

欢迎社区贡献翻译文件！

## 🔧 从源码构建

需要 Maven 3.8+ 和 Java 17+

```bash
mvn clean package
```

输出：`target/PickAndThrow-1.0.0.jar`

## 📖 技术细节

- **实体叠加**：使用 Bukkit passenger 系统
- **抛出方向**：完全跟随玩家视线方向（俯仰角和偏航角）
- **蓄力显示**：每tick更新（每秒20次）
  - BossBar：可配置颜色
  - ActionBar：彩虹渐变30个竖线 `||||||||||||||||||||||||||||||`
- **智能射线检测**：处理大型生物碰撞箱（如蜘蛛）
- **冷却系统**：防止双重触发问题（如马的骑乘冲突）
- **NBT匹配**：完整的ItemStack比对，支持其他插件的自定义物品
- **服务器检测**：自动检测Folia并使用相应的调度器API
- **静默权限**：无权限时不显示任何提示，不打扰游戏体验

## 📊 配置说明

| 配置项 | 说明 |
|--------|------|
| `language` | 语言文件（zh-CN, en-UK等），找不到自动回退到en-UK |
| `pickup-item` | 举起工具物品（null=仅空手，使用 /pat sethand 设置） |
| `max-entities` | 最大叠加数量（-1=无限制） |
| `entity-filter-mode` | 过滤模式（whitelist=白名单 / blacklist=黑名单） |
| `entity-filter-list` | 生物类型列表（空列表：黑名单=允许所有，白名单=禁止所有） |
| `throw-mode` | 抛出模式（one=一个个，all=全部） |
| `throw-cooldown` | 抛出冷却（毫秒，0=禁用，防止双重触发） |
| `throw-power-mode` | 力度模式（fixed=固定，charge=蓄力） |
| `charge-display-type` | 显示类型（bossbar=血条，actionbar=动作栏） |
| `charge-loop` | 蓄力循环（true=100%后返回0%，可精准把握时机） |
| `bossbar-colors` | Boss血条颜色（low/medium/high，可选：RED, PINK, BLUE, GREEN, YELLOW, PURPLE, WHITE） |

## 🎯 默认配置

- ✅ 空手举起生物
- ✅ 黑名单模式，禁止末影龙
- ✅ 一次抛出全部
- ✅ 蓄力模式
- ✅ Boss血条显示
- ✅ 300毫秒冷却

## 💡 使用技巧

1. **蓄力循环模式** (`charge-loop: true`)：适合PVP，可以在最高点精准抛出
2. **ActionBar彩虹条**：更简洁美观，不遮挡视野
3. **自定义物品**：可以设置其他插件的特殊物品（如自定义武器）作为举起工具
4. **白名单模式**：小游戏服务器可以限制只能举起特定生物
5. **冷却时间**：建议设置200-500毫秒，防止误触

## 📄 许可证

本插件仅供教育和个人使用。

## 👨‍💻 作者

**Mokzern**

---

**版本：** 1.0.0  
**支持：** 在 GitHub 上报告问题
