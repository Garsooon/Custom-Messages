# CustomMessages
[![GitHub Release](https://img.shields.io/github/v/release/Garsooon/Custom-Messages?label=release)](https://github.com/Garsooon/Custom-Messages/releases/latest)
[![Downloads](https://img.shields.io/github/downloads/Garsooon/Custom-Messages/total.svg?style=flat)](https://github.com/Garsooon/Custom-Messages/releases)

CustomMessages is a Beta 1.7.3 Minecraft Project Poseidon plugin that allows players and admins to set custom join and leave messages, with advanced filtering options.

## Features

- Set your own join or leave message using `/setjoin` or `/setleave`
- Admins can view, set or reset messages for any player
- Supports color codes (ie. `&6` for gold)
- Configurable regex-based blacklist for blocking words or patterns
- Notifies admins in game and in console when players try to use blacklisted words

## Commands

- `/setjoin <message>` — Set your custom join message (must include `%player%`)
- `/setleave <message>` — Set your custom leave message (must include `%player%`)
- `/viewmessages <player>` — View a player’s join and leave messages
- `/setplayerjoin <player> <message>` — Set another player’s join message
- `/setplayerleave <player> <message>` — Set another player’s leave message
- `/resetmessages <player>` — Reset a player’s messages to default

## Permissions

- `custommessages.setjoin` — Allows setting your join message
- `custommessages.setleave` — Allows setting your leave message
- `custommessages.admin` — Allows setting/resetting messages for any player (`default: op`)

## Configuration

- Place your blacklist regex patterns in `plugins/CustomMessages/blacklist.txt`  
  Example:
  ```
  (?i)\bt[e3]+st\b
  (?i)\btr[e3]+e\b
  ```
- Player join & leave Messages are stored in `plugins/CustomMessages/messages.properties`

## Setup

1. Drop the plugin jar into your server’s `plugins` folder
2. Start the server to generate config files
3. Edit `blacklist.txt` to specify disallowed words/patterns
4. Add permission nodes as needed to groups or players
