# Diegetic Inventory 🎒✨

![Minecraft](https://img.shields.io/badge/Minecraft-1.21.4-blue?style=flat-square&logo=minecraft)
![Fabric](https://img.shields.io/badge/ModLoader-Fabric-lightgrey?style=flat-square&logo=fabric)
![Java](https://img.shields.io/badge/Java-21+-orange?style=flat-square&logo=java)
![License](https://img.shields.io/badge/License-MIT-green?style=flat-square)

**Diegetic Inventory** brings true visual immersion to Minecraft multiplayer! 

Whenever a player opens their inventory (`E`), a diegetic floating 2D panel appears directly behind their back in the world. Other players can see their slots, items, cursor movements, and dragged stacks in real time — perfect for team survival, trading, roleplay, and SMP servers!

---

## ✨ Features

- 👥 **Real-Time Multiplayer Sync**:
  - Friends see exactly what you are doing in your inventory.
  - Smoothly animated cursor tracking shows where you're pointing.
  - Dragged stacks follow your mouse in the 3D world as you sort your items.

- 🎨 **Clean & Crisp 2D Visuals**:
  - **Flat 2D Item Sprites**: Clean, static icons without bulky 3D blocks.
  - **Legible Stack Counts**: Stack quantities (64, 16, etc.) and item durability bars are cleanly displayed on top of slots.
  - **Flat 2D Player Avatar**: Renders your custom skin (both base and jacket/hat/pants overlays) inside the preview window.

- 🧘 **Zero Disruption for the Local Player**:
  - Your local inventory screen is the standard, 100% vanilla Minecraft GUI.
  - Native mouse input, quick shift-clicks, recipe book, and crafting grids work without custom raycast jank.
  - Full compatibility with custom resource packs and GUI texture packs!

- ⚡ **Lightweight & High-Performance**:
  - Throttled network packets (20 TPS) with state delta tracking to prevent bandwidth overload.
  - Custom vertex batching and direct GPU flushes — zero FPS drops or entity lag.
  - Works seamlessly on both LAN worlds and dedicated servers.

---

## 🛠️ Installation

1. Install **[Fabric Loader](https://fabricmc.net/)** for Minecraft **1.21.4**.
2. Install **[Fabric API](https://modrinth.com/mod/fabric-api)**.
3. Download `diegetic-inventory-1.0.0.jar` from [Releases](https://github.com/devodrag/diegetic-inventory/releases) or Modrinth.
4. Drop the `.jar` into your Minecraft `.minecraft/mods` folder.
5. (Recommended) Install on both client and server for multiplayer synchronization.

---

## 💻 Building from Source

To compile the mod yourself:

```bash
# Clone the repository
git clone https://github.com/devodrag/diegetic-inventory.git
cd diegetic-inventory

# Build with Gradle (requires Java 21)
./gradlew build
```

The compiled mod JAR will be generated in `build/libs/diegetic-inventory-1.0.0.jar`.

---

## 📜 License

This project is licensed under the **MIT License**. Feel free to use it in any modpack!
