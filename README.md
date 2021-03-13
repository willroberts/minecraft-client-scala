# minecraft-client-scala

A client for the Minecraft RCON API, written in Scala 3.

## Library Usage

TBD

## Shell Utility

If you are looking for a tool rather than a library, run the `main` package with `sbt`:

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

The values for `Host`, `Port`, and `Password` are currently constants in [`Main.scala`](src/main/scala/Main.scala). The
default values will work when using the above Docker image.

## Starting a server for testing

```bash
$ docker pull itzg/minecraft-server
$ docker run --name=minecraft-server -p 25575:25575 -d -e EULA=TRUE itzg/minecraft-server
```

## Running Tests

After starting the test server in Docker:

```bash
$ sbt test
```

## Reference

- https://wiki.vg/Rcon