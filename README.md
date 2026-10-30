# Queue

Queue is a small and extensible matchmaker for Minecraft networks running on [SimpleCloud](https://simplecloud.app).
It puts players into matches, finds a free server for them and can be scaled in Kubernetes.

> [!NOTE]
> Queue is currently in beta and there is no real documentation yet. It works, but I can't recommend it for
> production networks yet.

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