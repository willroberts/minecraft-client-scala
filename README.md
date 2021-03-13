# minecraft-client-scala

This is an RCON shell for Minecraft servers, written in Scala 3.

## Starting the test server

```bash
$ docker pull itzg/minecraft-server
$ docker run --name=minecraft-server -p 25575:25575 -d -e EULA=TRUE itzg/minecraft-server
```

## Configuration

The values for `Host`, `Port`, and `Password` are currently constants in [`Main.scala`](src/main/scala/Main.scala). The
default values will work when using the above Docker image.

## Usage

```bash
$ sbt run
...
starting RCON shell, quit with 'exit', 'quit', or Ctrl-C
> seed
Seed: [4740948148837486117]
> list
There are 0 of a max of 20 players online:
> exit
```