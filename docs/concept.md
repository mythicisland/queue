# Queue

## Concept

### Queue Type (types/minekart.yml)
```yml
name: minekart

group: minekart
min-capacity: 6
max-capacity: 12

waiting-countdown-millis: 30
countdown-millis: 10
```

### QueueVisualizer

Ein Queue Visualizer ist einfach dafür da um Spielern die in einer Queue sind den Status der Queue anzuzeigen zumbeispiel über die Actionbar

Hier wäre mal ein Beispiel für die Actionbar welches der Reconciler derzeitig aus nutzt
```kt
package net.mythicisland.queue.runtime.queue.visualizer

import app.simplecloud.api.player.PlayerApi
import build.buf.gen.mythicisland.queue.v1.QueueStatus
import kotlinx.coroutines.future.await
import net.mythicisland.queue.runtime.queue.Queue
import net.mythicisland.queue.runtime.queue.QueueType

class ActionbarVisualizer(
    private val api: PlayerApi
) : QueueVisualizer {

    override suspend fun send(queue: Queue, type: QueueType, status: QueueStatus) {
        queue.players.forEach { player ->
            api.get(player).await().sendActionBar(QueueVisualizer.createQueueText(queue, type, status))
        }

    }

}
```

Die nachrichten für den Queue Visualizer für die verschiedenen Statuses befinden sich in C:\Dev\mythicisland\queue\queue-runtime\src\main\kotlin\net\mythicisland\queue\runtime\queue\message\Messages.kt

### Queue Reconciler
Der QueueStatusReconciler ist das Herz vom Queue Droplet er managed den lifecylce von queues also deren statues usw managed countdowns cleannupt das ganze also wirklich das herzstückt

### Workflow
Für den Workflow nutze ich einfach mal den Queue Type minekart wie oben beschrieben.

1. Spieler gibt in der Lobby den Command /queue <type> ein welcher jetzt minekart zbs. wäre.
2. Das Velocity Plugin sendet einen enqueue gRPC Request für den Spieler
3. Der Queue Microservice schaut jetzt erstmal ob eine Queue vom Typen minekart überhaupt exisiteirt usw
4. Wenn der Typ existiert wird nach einer queue vom status NOT_ENOUGH_PLAYERS oder WAITING_COUNTDOWN gesucht wenn es keine queue gibt wo er reinpassr wird eine neue erstellt
5. Jetzt ist der Spieler in einer neuen Queue und muss auf spieler warten sobald 6 Spieler in der Queue sind geht der waiting countdown los dort wird noch auf spieler gewartet so lange wie der countdown geht
6. Sobald die 30 Sekunden um sind oder die Queue voll ist geht es zum SEARCHING_SERVER Status und der Queue Microservice sucht nach einem freien server der gruppe minekart und wenn es keinen gibt wird ein neuer gestartet
7. Falls ein neuer server gestartet werden muss geht es zum WAITING_FOR_SERVER Status weiter und dann zum SERVER_READY Status wenn ein freier Server verfügbar ist geht die Queue direkt zu SERVER_READY
8. Sobald ein Server Ready ist geht der Countdown los sobald dieser zu ende ist werden die Spieler zum Reservierten Server gebracht.
9. Sobald das alles Erfolgreich geklappt hat geht es zum FINISHED Status dieser cleanupt die queue und löscht sie dann halt im endeffekt