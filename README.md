# Queue

A lightweight and extensible matchmaking queue for Minecraft networks running on [SimpleCloud](https://simplecloud.app).

> [!NOTE]
> Queue is currently in beta and there is no real documentation yet. It works, but I can't recommend it for
> production networks yet.

## Features

- [x] **Queue types**: Configure each gamemode in YAML.
- [x] **Matchmaking**: Match players with similar values like rating and widen the search the longer they wait.
- [x] **Teams**: Split every match into balanced teams.
- [x] **Multiple players per ticket**: Queue several players together and search in multiple queue types at once.
- [x] **Server allocation**: Find a server for every match through SimpleCloud and start new ones when needed.
- [x] **High availability**: Run several instances on Kubernetes, if one goes down another one takes over.
- [x] **Monitoring**: Export metrics to any OpenTelemetry collector.
- [x] **Ticket expiry**: Remove tickets after a maximum wait time or too many failed matches.
- [ ] **Backfill**: Fill running matches with players from the queue, e.g. to replace players who left.
- [ ] **Wait time estimates**: Show players how long they will probably wait.
- [ ] **Filters**: Only let tickets into a queue type that match fixed values, e.g. a region or client version.

## Requirements

- A SimpleCloud network
- NATS with JetStream enabled
- Docker

## Getting started

The easiest way is the compose file in [`deployment`](deployment). It starts the queue and NATS:

```sh
cd deployment
docker compose up -d
```