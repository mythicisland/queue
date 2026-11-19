# Queue

A lightweight and extensible matchmaking queue for Minecraft networks running on [SimpleCloud](https://simplecloud.app).

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