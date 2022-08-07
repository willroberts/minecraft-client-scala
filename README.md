# minecraft-client-scala

[![License Badge]][License]
[![Build Badge]][Build]

A client for the Minecraft RCON protocol, written in Scala 3.

## Library Usage

```scala
// Create a new client and connect to the server.
val client = new MinecraftRCONClient("127.0.0.1", 25575)

// Send some commands.
client.Authenticate("password") match {
	case Success(_) => { }
	case Failure(f) => { /** handle authentication error */ }
}
client.SendCommand("seed") match {
	case Success(resp) => { println(resp.Body) } // "Seed: [1871644822592853811]"
	case Failure(f) => { /** handle error */ }
}

// Disconnect cleanly when finished.
client.Close()
```

## Shell Utility

If you are looking for a tool rather than a library, try the shell command:

```bash
$ sbt run --host 127.0.0.1 --port 25575 --password minecraft
...
starting RCON shell, quit with 'exit', 'quit', or Ctrl-C
> seed
Seed: [4740948148837486117]
> list
There are 0 of a max of 20 players online:
```

## Starting a server for testing

```
$ docker pull itzg/minecraft-server
$ docker run --name=minecraft-server -p 25575:25575 -d -e EULA=TRUE itzg/minecraft-server
```

## Running Tests

After starting the test server in Docker:

```
$ sbt test
```

## Reference

- https://wiki.vg/Rcon

[License]: https://www.gnu.org/licenses/gpl-3.0
[License Badge]: https://img.shields.io/badge/License-GPLv3-blue.svg
[Build]: https://github.com/willroberts/minecraft-client-scala/actions/workflows/build.yaml
[Build Badge]: https://github.com/willroberts/minecraft-client-scala/actions/workflows/build.yaml/badge.svg