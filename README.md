# Queue

A matchmaking service written in Kotlin.

Players join a queue with a ticket. Queue puts tickets together 
into a match, asks the controller for a game server, counts down and then
connects everyone to that server.

## Tech Stack

- **Game Server Allocation:** SimpleCloud
- **Communication:** gRPC, NATS
- **Language:** Kotlin

## Usage

The easiest way is the `docker-compose.yml` in this repository.

```bash
docker compose up -d
```

Before the first start you should configure the service.

## Configuration

Every option is technically optional and has a default. You can set them in three ways:

1. as a command line flag, for example `--grpc-port 4564`
2. as an environment variable
3. in a `queue.properties` file (recommend)

| Environment variable  | Flag                    | Default                              | What it does                                           |
|-----------------------|-------------------------|--------------------------------------|--------------------------------------------------------|
| `GRPC_PORT`           | `--grpc-port`           | `4564`                               | Port the gRPC server starts on.                        |
| `NATS_URL`            | `--nats-url`            | `nats://localhost:4222`              | NATS server used to publish events.                    |
| `NATS_USER`           | `--nats-user`           | `admin`                              | User for the NATS connection.                          |
| `NATS_SECRET`         | `--nats-secret`         | `sup3rS3cr3t`                        | Password for the NATS connection.                      |
| `TYPE_PATH`           | `--types-path`          | `types`                              | Folder the queue types are read from.                  |
| `AUTH_KEY_PATH`       | `--auth-key-path`       | `.secrets/auth.key`                  | File holding the token every gRPC call has to send.    |
| `NETWORK_ID`          | `--network-id`          | `default`                            | Your SimpleCloud network id.                           |
| `NETWORK_SECRET`      | `--network-secret`      | `sup3rS3cr3t`                        | Your SimpleCloud network secret.                       |
| `CONTROLLER_URL`      | `--controller-url`      | `https://controller.simplecloud.app` | URL of your SimpleCloud controller.                    |
| `CONTROLLER_NATS_URL` | `--controller-nats-url` | `wss://nats.simplecloud.app:443`     | URL of the NATS server of your SimpleCloud controller. |

On the first start Queue creates a random token at `AUTH_KEY_PATH` if the file does not exist yet.

## Queue types

A queue type is a configuration to start matches.

`types/battle.yml`:

```yaml
name: battle
group: battle
min-players: 2
max-players: 8
waiting-duration-seconds: 30
countdown-duration-seconds: 10
```

| Option                       | Default | What it does                                                          |
|------------------------------|---------|-----------------------------------------------------------------------|
| `name`                       | —       | Name of the queue type. Clients use this name when they queue.        |
| `group`                      | —       | The SimpleCloud group the game servers are started in.                |
| `min-players`                | —       | How many players are needed before a match may start.                 |
| `max-players`                | —       | How many players a match can hold.                                    |
| `waiting-duration-seconds`   | `30`    | How long to wait for more players once `min-players` is reached.      |
| `countdown-duration-seconds` | `10`    | How long the countdown runs after a server was reserved.              |

A match starts as soon as `max-players` is reached, or after `waiting-duration-seconds` once there are at least `min-players`.

## API

The `queue-api` module is published to `https://repo.mythicisland.net/public`
and lets you create tickets, query matches and listen to queue events from Java
or Kotlin. Add the repository and the dependency:

```kotlin
repositories {
    maven("https://repo.mythicisland.net/public")
}

dependencies {
    implementation("net.mythicisland.queue:queue-api:2.0.5")
}
```

## Building

```bash
./gradlew build
```

## Contributing

We welcome contributions! Please feel free to submit a Pull Request. For major changes, please open an issue first to discuss what you would like to change.

1. Fork the repository
2. Create your feature branch (`git checkout -b feat/your-feature`)
3. Commit your changes (`git commit -m "feat: adds a amazing feature"`)
4. Push to the branch (`git push origin feat/your-feature`)
5. Open a Pull Request

## License

This project is licensed under the Apache 2.0 License - see the [LICENSE](LICENSE) file for details.