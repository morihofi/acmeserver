package de.morihofi.acmeserver.plugin.notification;

import de.morihofi.acmeserver.types.events.AbstractEvent;
import de.morihofi.acmeserver.types.events.EventSubscriber;
import de.morihofi.acmeserver.types.events.ServerStartedEvent;
import de.morihofi.acmeserver.types.intf.IServerInstance;
import de.morihofi.acmeserver.types.intf.IServerPlugin;
import de.morihofi.acmeserver.types.plugin.PluginProperty;
import lombok.extern.slf4j.Slf4j;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Simple plugin that posts a notification to a configured webhook when the server starts.
 */
@Slf4j
public class NotificationPlugin implements IServerPlugin, EventSubscriber {

    private static final String PROP_URL = "webhookUrl";
    private OkHttpClient client;
    private String webhookUrl;

    @Override
    public void initialize(IServerInstance serverInstance, Map<String, PluginProperty> properties) {
        client = new OkHttpClient();
        if (properties.containsKey(PROP_URL)) {
            webhookUrl = properties.get(PROP_URL).asString();
        }
        serverInstance.getEventBus().register(this);
    }

    @Override
    public String getPluginId() {
        return "notification";
    }

    @Override
    public long getPluginVersion() {
        return 1;
    }

    @Override
    public List<Class<? extends AbstractEvent>> canHandle() {
        return List.of(ServerStartedEvent.class);
    }

    @Override
    public void onEvent(AbstractEvent event) {
        if (webhookUrl == null || webhookUrl.isEmpty()) {
            return;
        }
        if (event instanceof ServerStartedEvent) {
            try {
                Request req = new Request.Builder()
                        .url(webhookUrl)
                        .post(RequestBody.create("ACME Server started", MediaType.parse("text/plain")))
                        .build();
                try (Response r = client.newCall(req).execute()) {
                    if (!r.isSuccessful()) {
                        log.warn("Webhook returned status {}", r.code());
                    }
                }
            } catch (IOException e) {
                log.warn("Failed sending webhook", e);
            }
        }
    }
}
