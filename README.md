# AltAuth

AltAuth is a solution to allow players with Mojang account, (untested) banned accounts and (untested) Multiplayer
disabled accounts to join online mode Minecraft servers depending on client and server support.
This works by redirecting the session server requests to a proxy specified by the server.

In comparison to the trustless authentication method proposed by Aizistral
https://github.com/Aizistral-Studios/Trustless-Authentication this authentication method enables joining of Mojang
account players, prevents man in the middle attacks (like vanilla authentication) and is compatible to clients without
AltAuth, but requires a trusted third/second party.

## Usage

The build/libs/AltAuth-*-all.jar (produced by running `./gradlew build`) is a valid Fabric (client and server 1.19.2),
Bukkit/Spigot/Paper (1.8 - 1.19.2) and BungeeCord/Waterfall mod/plugin. The server needs to be in online mode and
specify the domain or ip address of an AltAuth web proxy.

Since https is a requirement and the example proxy `proxy.py` not yet supporting https the proxy has to run behind a
https proxy currently. Configuration of the proxy can be done in the first lines of the proxy script.
Banned and multiplayer disabled user support has not been tested yet and is therefore disabled by default.

## AltAuth Protocol and Proxy behaviour

![Sequence diagram describing protocol and principle](AltAuth.png "Sequence diagram describing protocol and principle")