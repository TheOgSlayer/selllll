# SellChest

`/sell` opens a double chest menu (54 slots). Put items in, then click the **green glass pane**
in the last slot to sell everything at once. Items that can't be sold are given back when you
close the menu.

* Everything is written in small caps, nothing is bold.
* Plays a sound when the menu opens, closes and when you sell.
* The pane shows the total value of what you put in, live.

Requires **Vault** and an economy plugin (e.g. EssentialsX).

## Install
1. Get `SellChest.jar` (see "Getting the jar") and put it in `plugins/`.
2. Start the server once; `plugins/SellChest/prices.yml` and `config.yml` are created.
3. Edit prices, then run `/sell reload`.

## Commands & permissions
| Command | Permission | Default |
|---|---|---|
| `/sell` | `sellchest.use` | everyone |
| `/sell reload` | `sellchest.reload` | op |

## prices.yml
```yaml
DIAMOND: 100.0     # money per item
COBBLESTONE: 0.5
```
Item names are [Bukkit material names](https://hub.spigotmc.org/javadocs/spigot/org/bukkit/Material.html).
Anything not listed (or set to 0) can't be sold.

## config.yml
```yaml
sounds:
  open: "block.chest.open"
  close: "block.chest.close"
  sell: "entity.experience_orb.pickup"
  volume: 1.0
  pitch: 1.0
button-material: GREEN_STAINED_GLASS_PANE   # or LIME_STAINED_GLASS_PANE
sell-modified-items: false                  # true = also sell named/enchanted/damaged items
```
By default named, enchanted or damaged items and **filled shulker boxes** are not sold
(they're returned), so nobody loses shulker contents by accident.

## Getting the jar (GitHub)
Every push is built by GitHub Actions: open the **Actions** tab, open the latest run, and
download the **SellChest** artifact (a zip containing `SellChest.jar`).
To build locally: `mvn package` (Java 21).
