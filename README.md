# minecraft-client-scala

A client for the Minecraft RCON API, written in Scala 3.

## Library Usage

```scala
// Create a new client and connect to the server.
val client = new MinecraftRCONClient("127.0.0.1", 25575)

// Send some commands.
client.Authenticate("minecraft") // returns Try[Message], check errors with match.
client.SendCommand("seed") // returns Try[Message], see Message.Body for response.

// Disconnect cleanly when finished.
client.Close
```

## Shell Utility

If you are looking for a tool rather than a library, run the `main` package with `sbt`:

```bash
$ sbt
sbt> run --host 127.0.0.1 --port 25575 --password minecraft
...
starting RCON shell, quit with 'exit', 'quit', or Ctrl-C
> seed
Seed: [4740948148837486117]
> list
There are 0 of a max of 20 players online:
> exit
```

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