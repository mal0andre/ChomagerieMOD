<div align="center">

# 🧀 Chomagerie

**Automatic shulker-based item refill for Fabric**

🔁 Smart item refill
📦 Shulker-powered storage
🧩 Includes a custom crafting datapack

</div>

---

## 🚀 What is Chomagerie?

**Chomagerie** is a Fabric mod that automatically refills consumed item stacks using shulker boxes stored in your *
*inventory** or **ender chest**.

When a stack reaches **zero through normal gameplay**, it is instantly refilled — no GUI, no clicks, no interruptions.

The project also includes a **datapack providing custom crafting recipes**, designed to integrate cleanly with vanilla
gameplay.

---

## ✨ Key Features

### 🔁 Automatic Refill

* Refills an item **only when fully consumed**
* Works with block placement and item usage
* Refills directly into the **same hotbar slot**
* No trigger on manual inventory actions

### 🧠 Smart & Efficient

* Searches **inventory first**, then **ender chest**
* Avoids unnecessary refills
* Respects vanilla stack size limits

### 🧩 Custom Crafting Datapack

* Adds **custom recipes** related to Chomagerie
* Fully vanilla-compatible
* Can be enabled or disabled per world
* Works in singleplayer and multiplayer

### 🖥️ Server-Oriented

* Designed primarily for **server-side usage**
* Clients must install the mod to benefit
* Fabric-compatible environment

---

## ⚙️ Configuration

Configure the mod using:

* 🧩 **ModMenu** *(optional)*
* ⌨️ **Commands**
* 📄 **Config file**

### Commands

```
/chomagerie shulkerrefill toggle
/chomagerie shulkerrefill enable
/chomagerie shulkerrefill disable
/chomagerie shulkerrefill status
```

### Config File

```
config/chomagerie.json
```

Options:

* `enabled` — Enable / disable the refill system
* `showRefillMessages` — Toggle refill messages

---

## 🧩 Datapack Installation

The datapack is included with the project and must be enabled **per world**.

### Singleplayer

1. Open your world folder
2. Place the datapack in `datapacks/`
3. Run `/reload`

### Server

1. Place the datapack in:

   ```
   world/datapacks/
   ```
2. Run `/reload` or restart the server

---

## 📦 Requirements

* Minecraft **1.21+**
* Fabric Loader
* Fabric API
* ModMenu *(optional)*
* Cloth Config *(optional)*

---

## 📥 Installation

1. Download the mod `.jar`
2. Drop it into your `mods/` folder
3. Install Fabric API
4. Launch the game 🚀

---

## 🧪 Example Use Case

* You place your last block
* The stack reaches **0**
* A shulker box contains more of the same item
* ✨ The stack is instantly refilled

---

## 📜 License

**All Rights Reserved**

* Source code is public for transparency
* ✅ Modpack usage allowed with attribution
* ❌ No redistribution, modification, or reuse
* Pull requests accepted without redistribution rights

---

## 🤝 Contributing

Issues and pull requests are welcome 💙
Please include clear reproduction steps when reporting bugs 🐞

