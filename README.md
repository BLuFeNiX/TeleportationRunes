# Like my work?

Feel free to [buy me a coffee!](https://www.paypal.com/donate?hosted_button_id=GTDZ79S4FR7Y6)

# TeleportationRunes
A command-less teleportation plugin for Minecraft (compatible with Bukkit, Spigot, and Paper).

## Downloads

* [Latest stable build](https://github.com/BLuFeNiX/TeleportationRunes/releases/latest)
* Pre-release builds can be found at the top of [this page](https://github.com/BLuFeNiX/TeleportationRunes/releases). These will have newer features and fixes, but may not be stable or fuly tested.

## Building
```
mvn clean package
```

## Installation
Place `TeleportationRunes.jar` in your plugins folder, and restart your server.

## In-Game Usage

### Teleporters & Waypoints

1. Craft a **Book Of Ender**
![Book Of Ender](https://i.imgur.com/RlrPXBQ.png)
2. Build a **waypoint** out of 5 Lapiz Lazuli blocks and 4 blocks of your choice (these 4 blocks are called a **signature**).
In this example they are dirt, log, and glass blocks.
![Waypoint](https://i.imgur.com/5T5mWB2.png)
3. **While holding the Book Of Ender** right click the center block of your waypoint, in order to activate it.
4. Build a **teleporter** that matches the **signature** of your waypoint.
![Teleporter](https://i.imgur.com/KvzpKDG.png)
5. Walk onto the teleporter, and you will be transported!
![Teleportation](https://i.imgur.com/pOENySo.jpg)

> Note: You may leave the teleporter's immediate area, in order to cancel the teleportation.

### Teleportation Scrolls

1. Follow steps 1 through 3 in the section above, but do not build a teleporter.
2. Craft a **Scroll Of Warp**
![Scroll of Ender](https://i.imgur.com/j7EHPXp.png)
3. **While holding the Scroll Of Warp** right click the center block of your waypoint, in order to attune the scrolls.
4. From anywhere in the same world, you may now *right-click* while holding the scroll, in order to teleport.
![Teleport via scroll](https://i.imgur.com/4SU9JAi.jpg)

> Note: You must **crouch (hold shift) to confirm** the teleportation when using a scroll.

### Hiding Teleporters & Waypoints

You may cover the top of a teleporter or waypoint in a layer of blocks, and it will still function normally.

## Configuration & Customization

### Cost Formula
The EXP cost of teleportation may be changed via `config.yml`. You can enter simple math formulas here, or a hard-coded value.
For example:
```
# every 25 blocks costs 1 EXP
costFormula: (distance / 25)

# every teleport (no matter the distance) costs 10 EXP
costFormula: 10
```
### Teleporter & Waypoint Block Structure
The shape of teleporters and waypoints may be customized via `config.yml`. This is an advanced feature, and has only been minimally tested.
When reporting bugs, please specify if you have changed anything in these sections.

#### Rotation
Controls whether or not teleporters and waypoints may face any direction (North, South, East, or West).
Enabling this will allow for the creation of abnormally shapped structures, without the need to make sure they are always facing the same compass direction.
```
  enableRotation: false
```

#### Item Recipe Customization

The **Book of Ender** and **Scroll of Warp** recipes may be customized, as well as some mechanics of their use (for example, if the book is consumed when used). See [config.yml](https://github.com/BLuFeNiX/TeleportationRunes/blob/master/src/main/resources/config.yml) for examples.

#### Blueprint (shape)
The configuration below describes a 1-block high, 3x3 square for both the teleporter and waypoint. This matches the default configuration.
You may change the materials used, and the shape of the structure itself. Your new design *must* include the use of the four different signature blocks.
```
  blueprint:
    materials:
    
      $: ANY_BLOCK

      N: SIGNATURE_BLOCK_1
      S: SIGNATURE_BLOCK_2
      E: SIGNATURE_BLOCK_3
      W: SIGNATURE_BLOCK_4

      R: REDSTONE_BLOCK
      L: LAPIS_BLOCK
      D: DIAMOND_BLOCK
      I: IRON_BLOCK

    teleporter:
      layers:
        -
          - [R,N,R]
          - [W,R,E]
          - [R,S,R]

      clickableBlock:
          ==: Vector
          x: 1.0
          y: 0.0
          z: 1.0

    waypoint:
      layers:
        -
          - [L,N,L]
          - [W,L,E]
          - [L,S,L]

      clickableBlock:
          ==: Vector
          x: 1.0
          y: 0.0
          z: 1.0

```

##### Testing your blueprint
In order to see what your blueprint looks like in game, you can use `/tr mirage teleporter` or `/tr mirage waypoint` to display one of them in front of you. This will not actually change blocks in the world, but will temporarily render them in your client. Other players will not be able to see them. When rendered, a blueprint will use beacon blocks as placeholders for the 4 signature blocks.
