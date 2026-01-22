# QQCHA reset to Origin 
# Maze Action Game

## Project Overview
This project is a **top-down maze-based action game** developed for course submission.

The core gameplay loop focuses on **navigating procedurally generated mazes**, **real-time combat**, and **progressing through multiple floors (levels)**. Players defeat enemies, collect keys, unlock exits, and advance deeper into the maze.

---

## Project Structure

```
core/
├── screen/                 # Game flow and UI screens
│   ├── MenuScreen
│   ├── GameScreen          # Main maze gameplay
│   ├── BossFightScreen     # Boss encounter screen
│   ├── EndlessScreen       # Endless mode
│   ├── SettlementScreen    # Score & result screen
│
├── entity/                 # In-game objects and characters
│   ├── GameObject          # Base class for all world objects
│   ├── Player
│   ├── Enemy
│   ├── EnemyE01 ~ E04
│   ├── Boss
│   ├── Key / ExitDoor
│   ├── Treasure / Heart
│
├── maze/
│   ├── MazeGenerator       # Procedural maze generation
│   ├── MazeRenderer        # Maze rendering and wall grouping
│
├── combat/
│   ├── Ability             # Base ability class
│   ├── MeleeAttackAbility
│   ├── DashAbility
│   ├── MagicAbility
│   ├── CombatEffect
│   ├── CombatEffectManager
│
├── system/
│   ├── GameManager         # Core gameplay coordinator
│   ├── AbilityManager      # Ability slots and upgrades
│   ├── ScoreManager
│   ├── AchievementManager
│   ├── CameraManager
│
├── ui/
│   ├── HUD                 # In-game UI (health, mana, abilities)
│   ├── AchievementPopup
│
├── config/
│   ├── DifficultyConfig    # Difficulty scaling per mode
│   ├── BossTimeline.json   # Data-driven boss behavior
│
└── utils/                  # Utilities and helpers
```

---

## UML Design Overview

UML class diagrams were generated using **IntelliJ IDEA** to illustrate the overall architecture.

The diagrams focus on **structural relationships**, not exhaustive class listings.

### Key Design Layers

```
GameObject
 ├── Player
 ├── Enemy
 │    ├── EnemyE01
 │    ├── EnemyE02
 │    ├── EnemyE03
 │    └── EnemyE04
 ├── Trap
 ├── Treasure
 └── ExitDoor
```

```
Ability
 ├── MeleeAttackAbility
 ├── DashAbility
 └── MagicAbility
```

```
GameScreen / BossFightScreen / EndlessScreen
        │
   GameManager
        │
 ┌──────┼─────────┐
 │      │         │
Player  Enemy   MazeGenerator
```

Design principles:
- **GameObject** provides a unified interface for collision, rendering, and interaction
- Combat logic is separated into **Ability** and **Effect** systems
- **GameManager** acts as the central coordinator, avoiding excessive coupling

---

## ▶ How to Run the Game

### Requirements
- Java JDK 17+
- Gradle
- Desktop environment (Windows / macOS / Linux)

### Run
```bash
git clone <repository-url>
cd project-root
./gradlew desktop:run
```

---

## Controls

| Key        | Action                           |
|------------|----------------------------------|
| W A S D    | Move                             |
| Space      | Attack                           |
| Shift      | Dash                             |
| E          | Interact                         |
| Esc        | Pause                            |
| Arrow Keys | Move(2-player mode)           |
| Left Mouse Button      | Aim and Attack(2-player mode) |
| Right Mouse Button        | Dash(2-player mode)    |

---

## Core Game Mechanics

### 1. Maze Progression & Exploration
- Each level generates a **new maze layout** using procedural generation, consisting of walls, walkable paths, and interactive objects.
- Every maze contains **one entry point** and **at least one exit**, which are visually and functionally distinguishable from walls.
- Players must explore the maze to locate **key(s)** required to unlock exit doors.
- Attempting to exit without the required key will result in the exit behaving as a blocked wall.
- Mazes may exceed the screen size; when the player approaches the edge of the screen, the **camera smoothly moves** to reveal new areas.
- Difficulty scales across modes (**Easy / Normal / Hard**) by adjusting maze size, enemy density, traps, and damage values.

### 2. Real-Time Combat & Enemies
- Combat is **real-time** and occurs directly within the maze environment.
- Enemies are constrained to walkable paths and **never pass through walls**.
- Multiple enemy types are implemented, each featuring **distinct behaviors**, such as:
    - Patrol
    - Chase / Attack
    - Area control or pressure
- Enemies use basic **pathfinding and steering logic** to navigate the maze, avoid obstacles, and engage the player intelligently.
- Damage can occur through **direct contact**, **projectiles**, or **area-based effects**.
- Combat feedback includes hit flashes, particles, screen shake, and sound effects for clarity and responsiveness.

### 3. Obstacles, Traps, and Interactive Objects
- The maze includes **multiple types of obstacles and traps**, such as:
    - Environmental traps that deal damage or slow the player
    - Moving or dynamic obstacles that affect navigation
- Traps require the player to **evade, time movement, or react strategically** to progress safely.
- Collectible objects include:
    - Keys (required for exit access)
    - Treasures and power-ups (e.g., movement speed boosts, defensive effects)
    - Health-related pickups (hearts / life containers)

### 4. Ability System (Upgradeable, Non-Roguelike)
- Players are equipped with a **fixed set of core abilities**, including:
    - Melee attack
    - Dash
    - Magic ability (available in 2-player mode)
- Abilities can be **upgraded during gameplay**, improving attributes such as damage, cooldown, duration, or utility.
- Upgrades provide **incremental progression** and do not replace abilities or form random build-based gameplay.
- The system is designed to support maze traversal and combat efficiency rather than roguelike-style random builds.

### 5. Boss Encounters
- Boss fights take place in a **dedicated boss screen** distinct from standard maze levels.
- Boss behavior is **scripted and data-driven**, using timeline definitions loaded from external JSON files.
- Boss encounters emphasize pattern recognition, positioning, and survival under pressure.
- Certain boss phases dynamically interact with the maze or restrict player movement to increase challenge.

### 6. Game Modes
- **Normal Maze Mode**: Progress through a sequence of procedurally generated floors with increasing difficulty.
- **Endless / Survival Mode**: Face infinite waves of enemies with procedurally generated patterns and exponential difficulty scaling.
- **Boss Mode**: Focused encounters centered on story progression and boss mechanics.

### 7. HUD, Controls, and Game Flow
- The HUD displays critical gameplay information, including:
    - Player health (HP)
    - Key possession status
    - Ability cooldowns and status
    - Directional indicator pointing toward the nearest exit
- Players can pause the game at any time and access a menu to resume, restart, or exit.
- Victory is achieved by exiting the maze without losing all lives; defeat occurs when all lives are lost.
- In both cases, gameplay stops and the player may return to the main menu.

---


## Features Beyond Minimum Requirements
- Procedural maze generation with validation
- Data-driven boss timelines
- Dynamic maze rebuilding during boss fights
- Modular ability and effect system
- Centralized game management architecture
- Multiple game modes (normal, endless, boss)

---

## Notes for Reviewers

- UML diagrams focus on the core gameplay architecture rather than the complete class set, as the full diagram would be excessively large and reduce readability.
- Supporting systems (including story flow, audio, visual effects, save/load logic, and achievement tracking) are intentionally omitted from the UML diagrams to maintain clarity and focus.
- The project emphasizes a clean separation of concerns, with modular systems designed for maintainability and future extensibility.

---

Thank you for reviewing this project.

