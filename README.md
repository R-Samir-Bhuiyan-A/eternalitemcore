<div align="center">

# ⚔️ EternalItemCore

**A powerful, highly-configurable Item Management & RPG Core plugin for Minecraft.**

[![Release](https://img.shields.io/github/v/release/Open-Trident/eternalitemcore?style=for-the-badge&color=blue)](https://github.com/Open-Trident/eternalitemcore/releases/latest)
[![CI/CD](https://img.shields.io/github/actions/workflow/status/Open-Trident/eternalitemcore/release.yml?style=for-the-badge)](https://github.com/Open-Trident/eternalitemcore/actions)
[![License](https://img.shields.io/badge/License-MIT-green.svg?style=for-the-badge)](LICENSE)

</div>

---

## 📖 Overview

**EternalItemCore** is an advanced RPG plugin that introduces leveling, customizable abilities, dynamic statistics, and epic kill-effects to your server's weapons and items. Built from the ground up to be seamless and impactful, the plugin empowers players with true endgame progression through uniquely powerful items.

This project is maintained by **Open-Trident** and provides a robust foundation for building completely custom items without writing a single line of Java code.

---

## ✨ Features

- 🗡️ **Dynamic Stat Tracking**: Track levels, XP, and arbitrary variables directly on the item's `PersistentDataContainer`.
- ⚡ **Custom Abilities**: Link "stat-cores" to specific weapons, unlocking abilities like *Glitch Walk*, *Blade Dance*, *Dash Strike*, and more.
- 🎇 **Cinematic Kill Effects**: 15+ cinematic kill effects including *Void Implosion*, *Blood Siphon*, *Cosmic Dust*, and *Soul Fire Pillar*.
- 🔧 **In-Game Admin GUI**: Fully featured intuitive GUI for admins to manage, test, and spawn items directly in-game.
- 🌐 **Global Broadcasts**: Configurable global broadcasts for when players achieve new item mastery levels.
- 🎨 **Player Preferences**: Players can toggle their kill-effects or global item broadcasts to suit their playstyle.

---

## 🚀 Installation & Setup

1. **Download**: Grab the latest `.jar` from the [Releases](https://github.com/Open-Trident/eternalitemcore/releases) page.
2. **Install**: Place `EternalItemCore-<version>.jar` into your server's `plugins/` directory.
3. **Restart**: Start or restart your server.
4. **Configure**: Open `plugins/EternalItemCore/config.yml` to set up your custom abilities and items.

*Tested and compiled for Paper API (JDK 21).*

---

## 💻 Commands & Permissions

### Player Commands

| Command | Permission | Description |
|---------|------------|-------------|
| `/eicore toggleeffects` | `eternalitemcore.player` | Completely toggle the kill effect for your held item on or off. |
| `/eicore viewstats` | `eternalitemcore.player` | View exact stats (Level / Value) mapped to your currently held item. |
| `/eicore togglebroadcast` | `eternalitemcore.player` | Hide/Show global level-up messages for your held item. |

### Admin Commands

| Command | Permission | Description |
|---------|------------|-------------|
| `/eicore edit` | `eternalitemcore.admin` | Open the Item Mastery Admin GUI. |
| `/eicore give <player> <core>` | `eternalitemcore.admin` | Give a specific ability core to a player. |
| `/eicore setlevel <player> <stat> <level>`| `eternalitemcore.admin` | Force set an item's stat/ability level. |
| `/eicore addstat <player> <stat> <amount>`| `eternalitemcore.admin` | Add raw stat value (XP) to the held item. |
| `/eicore clearstats <player>` | `eternalitemcore.admin` | Wipe all stats from a player's held item. |
| `/eicore reload` | `eternalitemcore.admin` | Reload config files from disk. |

---

## 🛠️ For Developers

### Compiling from Source

EternalItemCore uses Maven. To build the project yourself:

```bash
git clone https://github.com/Open-Trident/eternalitemcore.git
cd eternalitemcore
mvn clean package
```

The compiled plugin will be located in `/target/`.

### Automated Deployments

Our CI/CD pipeline runs on every push and pull request to the `main` branch.
When code is pushed to `main`, an automated GitHub Release is triggered, automatically attaching the new `.jar` artifact along with changelogs fetched directly from your commit messages!

---

## 🤝 Contributing

Contributions, issues, and feature requests are always welcome!
Feel free to check the [issues page](https://github.com/Open-Trident/eternalitemcore/issues) to start contributing.

---

*Powered by [Open-Trident](https://github.com/Open-Trident).*
