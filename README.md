# Queue

A SimpleCloud droplet for queuing players into minigames.

## Architecture

### Queue Lifecycle

```mermaid
flowchart TD
    NEP[NOT_ENOUGH_PLAYERS] -->|min players reached| WC[WAITING_COUNTDOWN]
    WC -.->|players drop below min| NEP
    WC -->|countdown expired or full| SS[SEARCHING_SERVER]
    SS -->|server available| SR[SERVER_READY]
    SS -->|no server free| WFS[WAITING_FOR_SERVER]
    WFS -->|server becomes available| SR
    SR --> CD[COUNTDOWN]
    CD --> TP[TELEPORTING]
    TP --> FIN[FINISHED]
```

| Status               | Description                                                   |
|----------------------|---------------------------------------------------------------|
| `NOT_ENOUGH_PLAYERS` | Waiting for the minimum player count                          |
| `WAITING_COUNTDOWN`  | Minimum reached, counting down while waiting for more players |
| `SEARCHING_SERVER`   | Reserving an available game server                            |
| `WAITING_FOR_SERVER` | No server available yet, waiting for one                      |
| `SERVER_READY`       | Server reserved, starting the game countdown                  |
| `COUNTDOWN`          | Final countdown before teleport                               |
| `TELEPORTING`        | Transferring players to the game server                       |
| `FINISHED`           | Cleanup: free server, delete queue                            |

### Queue Type Configuration

Queue types are defined as YAML files in the types directory:

```yml
name: bedwars
group: bedwars
max-capacity: 6
min-capacity: 12
waiting-countdown-seconds: 30
countdown-seconds: 10
```

| Field                       | Description                                                    |
|-----------------------------|----------------------------------------------------------------|
| `name`                      | Unique identifier for this queue type                          |
| `group`                     | SimpleCloud server group to use for game servers               |
| `min-capacity`              | Minimum players required to start the waiting countdown        |
| `max-capacity`              | Maximum players per queue (starts immediately when full)       |
| `waiting-countdown-seconds` | Seconds to wait for more players after minimum is reached      |
| `countdown-seconds`         | Seconds to count down before teleporting after server is ready |