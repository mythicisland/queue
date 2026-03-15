Groups API

Manage server groups programmatically
The Groups API lets you create, query, update, and delete server groups. Access it via api.group().
​
Get Groups

// Get all groups
api.group().getAllGroups()
    .thenAccept(groups -> groups.forEach(g -> System.out.println(g.getName())));

// Get all groups with query filters
api.group().getAllGroups(new GroupQuery())
    .thenAccept(groups -> { ... });

// Get group by name
api.group().getGroupByName("lobby")
    .thenAccept(group -> System.out.println(group.getId()));

// Get group by ID
api.group().getGroupById("group-uuid")
    .thenAccept(group -> System.out.println(group.getName()));

​
Create Group

CreateGroupRequest request = CreateGroupRequest.builder()
    .name("bedwars")
    .type(GroupServerType.SERVER)
    .minMemory(512)
    .maxMemory(1024)
    .maxPlayers(16)
    .minOnlineCount(1)
    .maxOnlineCount(10)
    .build();

api.group().createGroup(request)
    .thenAccept(group -> System.out.println("Created: " + group.getId()));

​
Update Group

UpdateGroupRequest update = UpdateGroupRequest.builder()
    .maxPlayers(32)
    .maxOnlineCount(20)
    .build();

api.group().updateGroup("group-uuid", update)
    .thenAccept(group -> System.out.println("Updated: " + group.getName()));

​
Delete Group

api.group().deleteGroup("group-uuid")
    .thenAccept(success -> System.out.println("Deleted: " + success));

Deleting a group doesn’t stop running servers. Stop servers first if needed.
​
Group Properties
Groups support custom key-value properties for metadata:

// Add or update properties (merges with existing)
Map<String, String> props = Map.of(
    "gameMode", "ADVENTURE",
    "region", "eu-west"
);
api.group().updateGroupProperties("group-uuid", props);

// Remove specific properties
api.group().deleteGroupProperties("group-uuid", List.of("region"));

Properties are available in servers as environment variables prefixed with SIMPLECLOUD_.
​
Group Model
Property	Type	Description
id	String	Unique identifier
name	String	Group name
type	GroupServerType	SERVER or PROXY
minMemory	int	Minimum memory (MB)
maxMemory	int	Maximum memory (MB)
maxPlayers	int	Player limit per server
minOnlineCount	int	Minimum servers running
maxOnlineCount	int	Maximum servers allowed
properties	Map<String, String>	Custom metadata
scalingConfig	ScalingConfig	Auto-scaling settings
deploymentConfig	DeploymentConfig	Host deployment settings
sourceConfig	SourceConfig	Blueprint or image source
​
GroupServerType
Value	Description
SERVER	Game server (Paper, Spigot, etc.)
PROXY	Proxy server (Velocity, BungeeCord)

Cloud API
Servers API

Query and manage running server instances
The Servers API lets you query, start, stop, and update running servers. Access it via api.server().
​
Get Servers

// Get all servers
api.server().getAllServers()
    .thenAccept(servers -> System.out.println("Total: " + servers.size()));

// Get servers with query filters
api.server().getAllServers(new ServerQuery())
    .thenAccept(servers -> { ... });

// Get server by ID
api.server().getServerById("server-uuid")
    .thenAccept(server -> System.out.println(server.getState()));

// Get servers in a group
api.server().getServersByGroup("lobby")
    .thenAccept(servers -> servers.forEach(s ->
        System.out.println(s.getServerId() + ": " + s.getState())));

// Get server by group and numerical ID (e.g., Lobby-1)
api.server().getServerByNumericalId("lobby", 1)
    .thenAccept(server -> System.out.println(server.getServerId()));

// Get current server (from within a running server)
api.server().getCurrentServer()
    .thenAccept(server -> System.out.println("Running on: " + server.getServerId()));

​
Start Server

StartServerRequest request = new StartServerRequest("group-uuid", "lobby");

api.server().startServer(request)
    .thenAccept(server -> System.out.println("Started: " + server.getServerId()));

​
Stop Server

api.server().stopServer("server-uuid")
    .thenAccept(success -> System.out.println("Stopped: " + success));

​
Update Server

UpdateServerRequest update = UpdateServerRequest.builder()
    .maxPlayers(64)
    .build();

api.server().updateServer("server-uuid", update)
    .thenAccept(server -> System.out.println("Updated: " + server.getMaxPlayers()));

​
Server Properties
Servers inherit properties from their group, but you can override them per-instance:

// Add or update properties (merges with existing)
Map<String, String> props = Map.of("map", "castle", "mode", "competitive");
api.server().updateServerProperties("server-uuid", props);

// Remove specific properties
api.server().deleteServerProperties("server-uuid", List.of("mode"));

​
Server Model
Property	Type	Description
serverId	String	Unique identifier
groupName	String	Parent group name
numericalId	long	Numerical ID within group (1, 2, 3…)
state	ServerState	Current lifecycle state
host	String	Server host machine
ip	String	IP address
port	int	Server port
minMemory	int	Minimum memory (MB)
maxMemory	int	Maximum memory (MB)
maxPlayers	int	Player limit
playerCount	int	Current online players
properties	Map<String, String>	Custom metadata
createdAt	Instant	Creation timestamp
​
ServerState
State	Description
PREPARING	Copying template files
STARTING	JVM process launching
AVAILABLE	Ready for players
INGAME	Has active players
STOPPING	Shutdown in progress
​
Environment Variables
Inside a running server, access server info without the API:

String group = System.getenv("SIMPLECLOUD_GROUP");
String uniqueId = System.getenv("SIMPLECLOUD_UNIQUE_ID");
String numericalId = System.getenv("SIMPLECLOUD_NUMERICAL_ID");
String host = System.getenv("SIMPLECLOUD_HOST");
String ip = System.getenv("SIMPLECLOUD_IP");
String port = System.getenv("SIMPLECLOUD_PORT");
String maxPlayers = System.getenv("SIMPLECLOUD_MAX_PLAYERS");
String maxMemory = System.getenv("SIMPLECLOUD_MAX_MEMORY");

Custom group properties are also available with SIMPLECLOUD_ prefix (uppercase, dashes become underscores).

Players API

Manage players and send cross-server messages
The Players API lets you query players, transfer them between servers, and send rich-text messages using Adventure. Access it via api.player().
​
Get Players

// Get player by UUID
api.player().get(uuid)
    .thenAccept(player -> {
        if (player != null) {
            System.out.println(player.getName() + " on " + player.getConnectedServerName());
        }
    });

// Get player by username
api.player().get("Steve")
    .thenAccept(player -> { ... });

// Get all online players
api.player().getOnlinePlayers()
    .thenAccept(players -> players.forEach(p ->
        System.out.println(p.getName())));

// Get online player count
api.player().getOnlinePlayerCount()
    .thenAccept(count -> System.out.println("Online: " + count));

​
Player Actions
​
Transfer Player

api.player().get(uuid).thenAccept(player -> {
    if (player != null) {
        player.connect("lobby-1").thenAccept(result -> {
            switch (result) {
                case SUCCESS -> System.out.println("Transferred!");
                case SERVER_NOT_FOUND -> System.out.println("Server doesn't exist");
                case ALREADY_CONNECTED -> System.out.println("Already on that server");
                case CONNECTION_FAILED -> System.out.println("Connection failed");
            }
        });
    }
});

​
Kick Player

player.kick(Component.text("Server restarting")
    .color(NamedTextColor.RED));

​
Adventure Integration
CloudPlayer implements Adventure’s Audience interface, giving you access to all Adventure messaging features:
​
Send Messages

// Plain text
player.sendMessage(Component.text("Hello!"));

// Colored text
player.sendMessage(Component.text("Success!")
    .color(NamedTextColor.GREEN));

// Styled text
player.sendMessage(Component.text("Important")
    .color(NamedTextColor.GOLD)
    .decorate(TextDecoration.BOLD));

// Clickable text
player.sendMessage(Component.text("Click here")
    .clickEvent(ClickEvent.runCommand("/help"))
    .hoverEvent(HoverEvent.showText(Component.text("Run /help"))));

​
Send Titles

player.showTitle(Title.title(
    Component.text("Welcome!").color(NamedTextColor.GOLD),
    Component.text("to the server").color(NamedTextColor.GRAY),
    Title.Times.times(
        Duration.ofMillis(500),   // fade in
        Duration.ofSeconds(3),    // stay
        Duration.ofMillis(500)    // fade out
    )
));

​
Send Action Bar

player.sendActionBar(Component.text("⚔ Combat Mode Enabled")
    .color(NamedTextColor.RED));

​
Play Sounds

player.playSound(Sound.sound(
    Key.key("entity.experience_orb.pickup"),
    Sound.Source.PLAYER,
    1.0f,  // volume
    1.0f   // pitch
));

​
Boss Bars

BossBar bar = BossBar.bossBar(
    Component.text("Event Progress"),
    0.5f,  // progress (0.0 to 1.0)
    BossBar.Color.PURPLE,
    BossBar.Overlay.PROGRESS
);

player.showBossBar(bar);

// Update progress
bar.progress(0.75f);

// Remove when done
player.hideBossBar(bar);

For more detailed examples in both Java and Kotlin, see Adventure Integration.
​
CloudPlayer Model
Property	Type	Description
uniqueId	UUID	Player’s UUID
name	String	Player’s username
connectedServerName	String	Current server name
connectedProxyName	String	Connected proxy name
​
ConnectResult
Value	Description
SUCCESS	Transfer successful
SERVER_NOT_FOUND	Target server doesn’t exist
ALREADY_CONNECTED	Player already on that server
PLAYER_NOT_FOUND	Player went offline
CONNECTION_FAILED	Transfer failed
​
Broadcast to All Players

api.player().getOnlinePlayers().thenAccept(players -> {
    Component message = Component.text("[Announcement] ")
        .color(NamedTextColor.GOLD)
        .append(Component.text("Server restarting in 5 minutes")
            .color(NamedTextColor.WHITE));

    players.forEach(player -> player.sendMessage(message));
});

Cloud API
Events API

Subscribe to real-time events from your cloud network
The Events API lets you react to changes in your network in real-time. Access it via api.event().
​
Event Categories
Method	Events
api.event().group()	Group created, updated, deleted
api.event().server()	Server started, stopped, state changed
api.event().persistentServer()	Persistent server events
api.event().blueprint()	Blueprint created, updated, deleted
​
Server Events
​
Server Started

Subscription sub = api.event().server().onStarted(event -> {
    System.out.println("Server started: " + event.getServerId());
    System.out.println("Group: " + event.getGroupName());
});

​
Server Stopped

api.event().server().onStopped(event -> {
    System.out.println("Server stopped: " + event.getServerId());
});

​
Server State Changed

api.event().server().onStateChanged(event -> {
    System.out.println(event.getServerId() + ": " +
        event.getOldState() + " → " + event.getNewState());
});

​
Server Updated

api.event().server().onUpdated(event -> {
    System.out.println("Server updated: " + event.getServerId());
});

​
Server Deleted

api.event().server().onDeleted(event -> {
    System.out.println("Server deleted: " + event.getServerId());
});

​
Group Events
​
Group Created

api.event().group().onCreated(event -> {
    System.out.println("Group created: " + event.getServerGroupId());
});

​
Group Updated

api.event().group().onUpdated(event -> {
    System.out.println("Group updated: " + event.getServerGroupId());
});

​
Group Deleted

api.event().group().onDeleted(event -> {
    System.out.println("Group deleted: " + event.getServerGroupId());
});

​
Persistent Server Events

// Created
api.event().persistentServer().onCreated(event -> { ... });

// Started
api.event().persistentServer().onStarted(event -> { ... });

// Stopped
api.event().persistentServer().onStopped(event -> { ... });

// Updated
api.event().persistentServer().onUpdated(event -> { ... });

// Deleted
api.event().persistentServer().onDeleted(event -> { ... });

​
Blueprint Events

// Created
api.event().blueprint().onCreated(event -> { ... });

// Updated
api.event().blueprint().onUpdated(event -> { ... });

// Deleted
api.event().blueprint().onDeleted(event -> { ... });

​
Managing Subscriptions
Subscriptions implement AutoCloseable. Always clean them up when done:
​
Manual Cleanup

Subscription sub = api.event().server().onStarted(event -> { ... });

// Later, when shutting down
sub.close();

​
Try-with-Resources

try (Subscription sub = api.event().server().onStarted(event -> { ... })) {
    // Subscription active within this block
    Thread.sleep(60000);
} // Automatically closed

​
Plugin Lifecycle

public class MyPlugin extends JavaPlugin {
    private final List<Subscription> subscriptions = new ArrayList<>();
    private CloudApi api;

    @Override
    public void onEnable() {
        api = CloudApi.create();

        subscriptions.add(api.event().server().onStarted(this::handleServerStart));
        subscriptions.add(api.event().server().onStopped(this::handleServerStop));
    }

    @Override
    public void onDisable() {
        subscriptions.forEach(Subscription::close);
    }

    private void handleServerStart(ServerStartedEvent event) {
        getLogger().info("Server started: " + event.getServerId());
    }

    private void handleServerStop(ServerStoppedEvent event) {
        getLogger().info("Server stopped: " + event.getServerId());
    }
}

​
Event Reference
​
Server Events
Event	Properties
ServerStartedEvent	serverId, groupName
ServerStoppedEvent	serverId, groupName
ServerStateChangedEvent	serverId, oldState, newState
ServerUpdatedEvent	serverId
ServerDeletedEvent	serverId
​
Group Events
Event	Properties
GroupCreatedEvent	serverGroupId
GroupUpdatedEvent	serverGroupId
GroupDeletedEvent	serverGroupId
​
Persistent Server Events
Event	Properties
PersistentServerCreatedEvent	serverId
PersistentServerStartedEvent	serverId
PersistentServerStoppedEvent	serverId
PersistentServerUpdatedEvent	serverId
PersistentServerDeletedEvent	serverId
​
Blueprint Events
Event	Properties
BlueprintCreatedEvent	blueprintId
BlueprintUpdatedEvent	blueprintId
BlueprintDeletedEvent	blueprintId

Was this page helpful?

Guides
Transfer Players Between Servers

Move players across your network programmatically
This guide shows how to transfer players between servers, with proper error handling and user feedback.
​
Basic Transfer

public void transferPlayer(UUID playerUuid, String targetServer) {
    api.player().get(playerUuid).thenAccept(player -> {
        if (player == null) {
            System.out.println("Player not found or offline");
            return;
        }

        player.connect(targetServer).thenAccept(result -> {
            switch (result) {
                case SUCCESS -> System.out.println("Transfer successful");
                case SERVER_NOT_FOUND -> System.out.println("Server doesn't exist");
                case ALREADY_CONNECTED -> System.out.println("Already on that server");
                case CONNECTION_FAILED -> System.out.println("Transfer failed");
            }
        });
    });
}

​
Transfer to Best Server in Group
Find the server with the most available slots:

public void transferToGroup(UUID playerUuid, String groupName) {
    api.server().getServersByGroup(groupName).thenAccept(servers -> {
        // Find server with most available space
        Server bestServer = servers.stream()
            .filter(s -> s.getState() == ServerState.AVAILABLE || s.getState() == ServerState.INGAME)
            .filter(s -> s.getPlayerCount() < s.getMaxPlayers())
            .max(Comparator.comparingInt(s -> s.getMaxPlayers() - s.getPlayerCount()))
            .orElse(null);

        if (bestServer == null) {
            System.out.println("No available servers in group");
            return;
        }

        api.player().get(playerUuid).thenAccept(player -> {
            if (player != null) {
                player.connect(bestServer.getServerId());
            }
        });
    });
}

​
Transfer with User Feedback
Send messages to the player during transfer:

public void transferWithFeedback(UUID playerUuid, String targetServer) {
    api.player().get(playerUuid).thenCompose(player -> {
        if (player == null) {
            return CompletableFuture.completedFuture(null);
        }

        // Notify player
        player.sendMessage(Component.text("Connecting to " + targetServer + "...")
            .color(NamedTextColor.YELLOW));

        return player.connect(targetServer).thenApply(result -> {
            if (result != CloudPlayer.ConnectResult.SUCCESS) {
                player.sendMessage(Component.text("Failed to connect: " + result)
                    .color(NamedTextColor.RED));
            }
            return result;
        });
    });
}

​
Transfer All Players from Server
Evacuate all players before server shutdown:

public void evacuateServer(String serverId, String targetServer) {
    api.server().getServerById(serverId).thenAccept(server -> {
        if (server == null) return;

        api.player().getOnlinePlayers().thenAccept(players -> {
            players.stream()
                .filter(p -> p.getConnectedServerName().equals(server.getServerId()))
                .forEach(player -> {
                    player.sendMessage(Component.text("Server shutting down, transferring...")
                        .color(NamedTextColor.GOLD));
                    player.connect(targetServer);
                });
        });
    });
}

​
Start Server if None Available
Start a new server and transfer the player:

public void transferOrStartServer(UUID playerUuid, String groupName) {
    api.server().getServersByGroup(groupName).thenAccept(servers -> {
        Server available = servers.stream()
            .filter(s -> s.getState() == ServerState.AVAILABLE)
            .filter(s -> s.getPlayerCount() < s.getMaxPlayers())
            .findFirst()
            .orElse(null);

        if (available != null) {
            // Transfer to existing server
            api.player().get(playerUuid).thenAccept(player -> {
                if (player != null) player.connect(available.getServerId());
            });
        } else {
            // Start new server, then transfer
            api.group().getGroupByName(groupName).thenAccept(group -> {
                if (group == null) return;

                api.server().startServer(new StartServerRequest(group.getId(), groupName))
                    .thenAccept(newServer -> {
                        // Wait for server to be ready
                        waitForServerReady(newServer.getServerId(), () -> {
                            api.player().get(playerUuid).thenAccept(player -> {
                                if (player != null) player.connect(newServer.getServerId());
                            });
                        });
                    });
            });
        }
    });
}

private void waitForServerReady(String serverId, Runnable callback) {
    api.event().server().onStateChanged(event -> {
        if (event.getServerId().equals(serverId) &&
            event.getNewState() == ServerState.AVAILABLE) {
            callback.run();
        }
    });
}

Was this page helpful?

Manage Servers Programmatically

Start, stop, and monitor servers with the Cloud API
This guide shows common patterns for managing servers programmatically.
​
Start Servers on Demand
​
Start Single Server

public CompletableFuture<Server> startServer(String groupName) {
    return api.group().getGroupByName(groupName)
        .thenCompose(group -> {
            if (group == null) {
                return CompletableFuture.failedFuture(
                    new IllegalArgumentException("Group not found: " + groupName));
            }
            return api.server().startServer(new StartServerRequest(group.getId(), groupName));
        });
}

​
Start Multiple Servers

public void ensureMinimumServers(String groupName, int minimum) {
    api.server().getServersByGroup(groupName).thenAccept(servers -> {
        long running = servers.stream()
            .filter(s -> s.getState() != ServerState.STOPPING)
            .count();

        if (running < minimum) {
            int toStart = (int) (minimum - running);
            api.group().getGroupByName(groupName).thenAccept(group -> {
                if (group == null) return;

                for (int i = 0; i < toStart; i++) {
                    api.server().startServer(new StartServerRequest(group.getId(), groupName))
                        .thenAccept(server -> System.out.println("Started: " + server.getServerId()));
                }
            });
        }
    });
}

​
Stop Servers
​
Stop Single Server

public void stopServer(String serverId) {
    api.server().stopServer(serverId)
        .thenAccept(success -> System.out.println("Stopped: " + success));
}

​
Stop Empty Servers

public void stopEmptyServers(String groupName, int keepMinimum) {
    api.server().getServersByGroup(groupName).thenAccept(servers -> {
        List<Server> emptyServers = servers.stream()
            .filter(s -> s.getState() == ServerState.AVAILABLE)
            .filter(s -> s.getPlayerCount() == 0)
            .sorted(Comparator.comparingLong(Server::getNumericalId).reversed())
            .toList();

        // Keep minimum servers running
        int toStop = Math.max(0, emptyServers.size() - keepMinimum);

        emptyServers.stream()
            .limit(toStop)
            .forEach(server -> api.server().stopServer(server.getServerId()));
    });
}

​
Graceful Shutdown with Player Evacuation

public void gracefulStop(String serverId, String fallbackServer) {
    api.server().getServerById(serverId).thenAccept(server -> {
        if (server == null) return;

        // Evacuate players first
        api.player().getOnlinePlayers().thenAccept(players -> {
            List<CompletableFuture<CloudPlayer.ConnectResult>> transfers = players.stream()
                .filter(p -> p.getConnectedServerName().equals(serverId))
                .map(player -> {
                    player.sendMessage(Component.text("Server shutting down...")
                        .color(NamedTextColor.YELLOW));
                    return player.connect(fallbackServer);
                })
                .toList();

            // Wait for all transfers, then stop
            CompletableFuture.allOf(transfers.toArray(new CompletableFuture[0]))
                .thenRun(() -> api.server().stopServer(serverId));
        });
    });
}

​
Monitor Servers
​
Watch Server Count

public void monitorServerCount(String groupName) {
    api.event().server().onStarted(event -> {
        if (event.getGroupName().equals(groupName)) {
            updateServerCount(groupName);
        }
    });

    api.event().server().onStopped(event -> {
        if (event.getGroupName().equals(groupName)) {
            updateServerCount(groupName);
        }
    });
}

private void updateServerCount(String groupName) {
    api.server().getServersByGroup(groupName).thenAccept(servers -> {
        System.out.println(groupName + " servers: " + servers.size());
    });
}

​
Track Server States

public void trackServerStates() {
    api.event().server().onStateChanged(event -> {
        System.out.println(event.getServerId() + ": " +
            event.getOldState() + " → " + event.getNewState());

        // React to state changes
        if (event.getNewState() == ServerState.AVAILABLE) {
            System.out.println("Server ready for players: " + event.getServerId());
        }
    });
}

​
Update Server Properties
​
Set Game Mode

public void setServerGameMode(String serverId, String mode) {
    api.server().updateServerProperties(serverId, Map.of("gameMode", mode))
        .thenAccept(server -> System.out.println("Updated: " + server.getServerId()));
}

​
Mark Server as Joinable

public void setJoinable(String serverId, boolean joinable) {
    api.server().updateServerProperties(serverId, Map.of("joinable", String.valueOf(joinable)));
}

​
Find Servers
​
Find Server with Lowest Player Count

public CompletableFuture<Server> findBestServer(String groupName) {
    return api.server().getServersByGroup(groupName)
        .thenApply(servers -> servers.stream()
            .filter(s -> s.getState() == ServerState.AVAILABLE || s.getState() == ServerState.INGAME)
            .filter(s -> s.getPlayerCount() < s.getMaxPlayers())
            .min(Comparator.comparingInt(Server::getPlayerCount))
            .orElse(null));
}

​
Find Servers by Property

public CompletableFuture<List<Server>> findByProperty(String groupName, String key, String value) {
    return api.server().getServersByGroup(groupName)
        .thenApply(servers -> servers.stream()
            .filter(s -> value.equals(s.getProperties().get(key)))
            .toList());
}

Guides
Handle Events in Real-Time

React to server, group, and player changes with the Events API
This guide shows patterns for handling real-time events in your plugins.
​
Basic Event Handling
​
Subscribe to Events

// Subscribe returns a Subscription object
Subscription sub = api.event().server().onStarted(event -> {
    System.out.println("Server started: " + event.getServerId());
});

// Remember to close when done
sub.close();

​
Plugin Lifecycle Management

public class MyPlugin extends JavaPlugin {
    private CloudApi api;
    private final List<Subscription> subscriptions = new ArrayList<>();

    @Override
    public void onEnable() {
        api = CloudApi.create();
        registerEvents();
    }

    @Override
    public void onDisable() {
        // Clean up all subscriptions
        subscriptions.forEach(Subscription::close);
        subscriptions.clear();
    }

    private void registerEvents() {
        subscriptions.add(api.event().server().onStarted(this::onServerStarted));
        subscriptions.add(api.event().server().onStopped(this::onServerStopped));
        subscriptions.add(api.event().group().onUpdated(this::onGroupUpdated));
    }

    private void onServerStarted(ServerStartedEvent event) {
        getLogger().info("Server started: " + event.getServerId());
    }

    private void onServerStopped(ServerStoppedEvent event) {
        getLogger().info("Server stopped: " + event.getServerId());
    }

    private void onGroupUpdated(GroupUpdatedEvent event) {
        getLogger().info("Group updated: " + event.getServerGroupId());
    }
}

​
Common Event Patterns
​
Auto-Start Servers When Group Empty

public void setupAutoStart(String groupName, int minimum) {
    api.event().server().onStopped(event -> {
        if (!event.getGroupName().equals(groupName)) return;

        api.server().getServersByGroup(groupName).thenAccept(servers -> {
            long running = servers.stream()
                .filter(s -> s.getState() != ServerState.STOPPING)
                .count();

            if (running < minimum) {
                api.group().getGroupByName(groupName).thenAccept(group -> {
                    if (group != null) {
                        api.server().startServer(new StartServerRequest(group.getId(), groupName));
                    }
                });
            }
        });
    });
}

​
Log All State Changes

public void setupStateLogging() {
    api.event().server().onStateChanged(event -> {
        String message = String.format("[%s] %s → %s",
            event.getServerId(),
            event.getOldState(),
            event.getNewState());

        // Log to file, database, or external service
        logger.info(message);
    });
}

​
Notify Staff on Server Crash

public void setupCrashNotifications() {
    api.event().server().onStopped(event -> {
        api.server().getServerById(event.getServerId()).thenAccept(server -> {
            // If server stopped unexpectedly (not graceful shutdown)
            // This is a simplified check - adjust based on your needs

            notifyStaff(Component.text("[Alert] Server stopped: " + event.getServerId())
                .color(NamedTextColor.RED));
        });
    });
}

private void notifyStaff(Component message) {
    api.player().getOnlinePlayers().thenAccept(players -> {
        players.stream()
            .filter(p -> hasStaffPermission(p))
            .forEach(p -> p.sendMessage(message));
    });
}

​
Sync Data When Server Starts

public void setupDataSync() {
    api.event().server().onStateChanged(event -> {
        if (event.getNewState() == ServerState.AVAILABLE) {
            // Server is ready, sync data
            syncDataToServer(event.getServerId());
        }
    });
}

private void syncDataToServer(String serverId) {
    // Your data synchronization logic
    System.out.println("Syncing data to: " + serverId);
}

​
Filtering Events
​
Filter by Group

public void watchGroup(String groupName) {
    api.event().server().onStarted(event -> {
        if (event.getGroupName().equals(groupName)) {
            handleGroupServerStart(event);
        }
    });
}

​
Filter by Server Type

public void watchProxies() {
    api.event().server().onStarted(event -> {
        api.group().getGroupByName(event.getGroupName()).thenAccept(group -> {
            if (group != null && group.getType() == GroupServerType.PROXY) {
                handleProxyStart(event);
            }
        });
    });
}

​
One-Time Events
​
Wait for Specific Server

public CompletableFuture<Void> waitForServer(String serverId) {
    CompletableFuture<Void> future = new CompletableFuture<>();

    Subscription sub = api.event().server().onStateChanged(event -> {
        if (event.getServerId().equals(serverId) &&
            event.getNewState() == ServerState.AVAILABLE) {
            future.complete(null);
        }
    });

    // Clean up subscription when future completes
    future.whenComplete((result, error) -> sub.close());

    return future;
}

// Usage
waitForServer("lobby-1").thenRun(() -> {
    System.out.println("Server is ready!");
});

​
Timeout Handling

public CompletableFuture<Void> waitForServerWithTimeout(String serverId, Duration timeout) {
    CompletableFuture<Void> future = new CompletableFuture<>();

    Subscription sub = api.event().server().onStateChanged(event -> {
        if (event.getServerId().equals(serverId) &&
            event.getNewState() == ServerState.AVAILABLE) {
            future.complete(null);
        }
    });

    // Timeout after duration
    CompletableFuture.delayedExecutor(timeout.toMillis(), TimeUnit.MILLISECONDS)
        .execute(() -> future.completeExceptionally(
            new TimeoutException("Server did not start in time")));

    future.whenComplete((result, error) -> sub.close());

    return future;
}
