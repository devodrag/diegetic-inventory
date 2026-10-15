package net.diegetic.inventory.network;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Клиентский менеджер состояний голографических инвентарей других игроков.
 */
public class PeerStateManager {
    private static final PeerStateManager INSTANCE = new PeerStateManager();

    private final Map<UUID, PeerPlayerState> peers = new ConcurrentHashMap<>();

    private PeerStateManager() {}

    public static PeerStateManager getInstance() {
        return INSTANCE;
    }

    public PeerPlayerState getOrCreate(UUID uuid) {
        return peers.computeIfAbsent(uuid, PeerPlayerState::new);
    }

    public PeerPlayerState getPeer(UUID uuid) {
        return peers.get(uuid);
    }

    public void removePeer(UUID uuid) {
        peers.remove(uuid);
    }

    public void tick() {
        for (PeerPlayerState peer : peers.values()) {
            peer.tick();
        }
    }

    public void clear() {
        peers.clear();
    }
}
