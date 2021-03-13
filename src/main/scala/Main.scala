/** This is a Minecraft RCON client written in Scala, using Java stdlib networking.
  * Running this code connects to a server, authenticates, and retrieves the world seed. */
package minecraft

import scala.io.StdIn.readLine
import scala.util.{Success, Failure}
import scala.util.control.Breaks

/** MinecraftRCONShell provides an interactive RCON shell for Minecraft servers. */
object MinecraftRCONShell {
        /** Constants. */
        val DefaultHost: String = "127.0.0.1"
        val DefaultPort: Int = 25575
        val DefaultPassword: String = "minecraft"

        /** Main function runs an interactive shell for RCON commands. */
        @main def shell(args: String*): Unit = {
                var host: String = DefaultHost
                var port: Int = DefaultPort
                var password: String = DefaultPassword

                /** Parse command-line arguments if provided */
                for ( i <- 0 until args.length ) {
                        args(i) match {
                                case "--host" => { host = args(i+1) }
                                case "--port" => { port = args(i+1).toInt }
                                case "--password" => { password = args(i+1) }
                                case _ => { }
                        }
                }

                /** Connect to the server and authenticate */
                val client = new MinecraftRCONClient(host, port)
                client.Authenticate(password) match {
                        case Success(_) => {}
                        case Failure(f) => {
                                println("failed to authenticate")
                                System.exit(1)
                        }
                }

                /** Start the RCON shell */
                println("starting RCON shell, quit with 'exit', 'quit', or Ctrl-C")
                val loop = new Breaks;
                loop.breakable {
                        while(true) {
                                print("> ")
                                val input = readLine()
                                if (Array("exit", "quit").contains(input)) {
                                        loop.break
                                }
                                client.SendCommand(input) match {
                                        case Success(resp) => { println((resp.Body.map(_.toChar)).mkString) }
                                        case Failure(f) => { println("error for command '%s': %s".format(input, f.getMessage())) }
                                }
                        }
                }
                System.exit(0)
        }
}