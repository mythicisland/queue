# Queue

Queue ist unser Matchmaker welcher gRPC und Nats mit Protobuf nutzt

## Concept

### Queue Types
Eine Queue braucht immer einen typen in dem typen sind sachen wie name max capcity definiret usw typen werden in einer .yml datei definiert

Example Queue Type Config (minekart.yml)
```yml
name: minekart

group: minekart
min-capacity: 6
max-capacity: 12
```

### Visualizer
Ein Visualizer zeigt den derzeitgen Status an in der Queue in der man gerade ist zumbeispiel "Waiting for Player <3/12>" Zum beipspiel der actiobar viszlaiter
zeigt das in der actiobar an

### Queue Workflow