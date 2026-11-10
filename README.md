# Add Server Pre-check

**Check before you add.**

Add Server Pre-check is a lightweight, client-side Fabric mod for Minecraft. It checks whether a multiplayer server responds before you save its address to your server list. If the check fails, the mod shows an error so you can understand what went wrong and choose what to do next.

![Minecraft 26.3](https://img.shields.io/badge/Minecraft-26.3-green) ![Fabric](https://img.shields.io/badge/Mod%20loader-Fabric-blue) ![Client-side](https://img.shields.io/badge/Environment-Client--side-purple) ![MIT License](https://img.shields.io/badge/License-MIT-yellow) ![Alpha](https://img.shields.io/badge/Status-Alpha-orange)

## Download

- [CurseForge](https://www.curseforge.com/minecraft/mc-mods/add-server-pre-check)
- [Modrinth](https://modrinth.com/mod/add-server-pre-check)
- [GitHub Releases](https://github.com/Blazelike0123/Add-Server-Pre-Check/releases)

Download the JAR that matches your Minecraft version. This project is currently in **alpha**, so you may encounter bugs or unexpected behaviour.

## Features

- **Check before adding:** Test whether a server responds before saving its address.
- **Clear error feedback:** See an error message when the connection check cannot be completed.
- **Add Anyway:** Choose to save the address even when the check fails.
- **Go Back:** Return to the form and review or edit the server details.
- **Vanilla-style interface:** A simple screen designed to fit Minecraft's familiar look, including a three-dot loading animation.

## Screenshots

### Checking a server

![Server connection check in progress](https://cdn.modrinth.com/data/cached_images/7aa152e9c75da76d104cdf396cc6dfaa130826ee_0.webp)

### Connection timeout

![Server connection timeout message](https://cdn.modrinth.com/data/cached_images/0cdcb74a7341f980ec31bc232c6f174ed2bc421c_0.webp)

## Compatibility

| Requirement | Supported version or setting |
|---|---|
| Minecraft | 26.3 |
| Mod loader | Fabric |
| Fabric Loader | 0.19.5 or newer |
| Java | 25 or newer |
| Environment | Client-side only |

**The server you want to add does not need to install this mod.** Add Server Pre-check runs on your Minecraft client and can be used when adding servers that run vanilla Minecraft or other compatible server software.

A successful pre-check does not guarantee that you will be able to join. The server may still reject the connection because of its Minecraft version, whitelist, authentication requirements, capacity, or other server-side rules.

## Installation

1. Install [Fabric Loader](https://fabricmc.net/use/installer/) for Minecraft 26.3.
2. Use Java 25 or newer.
3. Download the Add Server Pre-check JAR from [CurseForge](https://www.curseforge.com/minecraft/mc-mods/add-server-pre-check), [Modrinth](https://modrinth.com/mod/add-server-pre-check), or [GitHub Releases](https://github.com/Blazelike0123/Add-Server-Pre-Check/releases).
4. Put the downloaded JAR in your Minecraft `mods` folder.
5. Launch Minecraft with your Fabric profile.

## How to use

1. Open **Multiplayer** and select **Add Server**.
2. Enter the server name and address.
3. Start the pre-check.
4. If the server responds, continue adding it.
5. If the check fails, read the displayed error and choose **Add Anyway** or **Go Back**.

## Report a problem

Found a bug or something not working as expected? [Open an issue on GitHub](https://github.com/Blazelike0123/Add-Server-Pre-Check/issues).

When reporting a problem, include:

- Your Minecraft version and Fabric Loader version.
- The server address format you tested (remove any private information).
- What you expected to happen and what happened instead.
- Relevant game logs or a crash report, if available.

Please do not post passwords, access tokens, or other private server information.

## Source code

The source code is available in the [GitHub repository](https://github.com/Blazelike0123/Add-Server-Pre-Check).

## License

Add Server Pre-check is open source under the [MIT License](LICENSE). You may use, modify, and redistribute it according to the terms of that license.

---
