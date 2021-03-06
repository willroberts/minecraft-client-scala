# minecraft-client-scala

This is an RCON shell for Minecraft servers, written in Scala 3.

## Usage

```bash
$ docker pull itzg/minecraft-server
$ docker run --name=minecraft-server -p 25575:25575 -d -e EULA=TRUE itzg/minecraft-server
$ sbt run
...
starting minecraft client
authenticating
authenticated successfully
starting RCON shell, quit with 'exit', 'quit', or Ctrl-C
> seed
Seed: [4740948148837486117]
> list
There are 0 of a max of 20 players online:
> exit
```