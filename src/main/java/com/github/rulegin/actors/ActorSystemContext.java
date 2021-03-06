
package com.github.rulegin.actors;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Scheduler;
import com.github.rulegin.actors.service.ActorService;
import com.github.rulegin.actors.service.cluster.ServerAddress;
import com.github.rulegin.actors.service.cluster.discovery.DiscoveryService;
import com.github.rulegin.actors.service.cluster.routing.ClusterRoutingService;
import com.github.rulegin.actors.service.cluster.rpc.ClusterRpcService;
import com.github.rulegin.actors.service.component.ComponentDiscoveryService;
import com.github.rulegin.common.data.DataConstants;
import com.github.rulegin.common.data.Event;
import com.github.rulegin.common.data.component.ComponentLifecycleEvent;
import com.github.rulegin.common.data.id.EntityId;
import com.github.rulegin.core.handlers.plugin.PluginWebSocketMsgEndpoint;
import com.github.rulegin.dao.components.DefineComponentService;
import com.github.rulegin.dao.event.EventService;
import com.github.rulegin.dao.plugin.PluginService;
import com.github.rulegin.dao.rule.RuleService;
import com.github.rulegin.dao.timeseries.TimeseriesService;
import com.github.rulegin.dao.user.UserService;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Optional;

@Component
public class ActorSystemContext {
    private static final String AKKA_CONF_FILE_NAME = "actor-system.conf";

    protected final ObjectMapper mapper = new ObjectMapper();

    @Getter @Setter private ActorService actorService;

    @Autowired
    @Getter private DiscoveryService discoveryService;

    @Autowired
    @Getter @Setter private ComponentDiscoveryService componentService;

    @Autowired
    @Getter private ClusterRoutingService routingService;

    @Autowired
    @Getter private ClusterRpcService rpcService;


/*
    @Autowired
    @Getter private DeviceAuthService deviceAuthService;

    @Autowired
    @Getter private AssetService assetService;

    @Autowired
    @Getter private TenantService tenantService;
*/

    @Autowired
    @Getter private UserService customerService;

    @Autowired
    @Getter private RuleService ruleService;

    @Autowired
    @Getter private DefineComponentService defineComponentService;

    @Autowired
    @Getter private PluginService pluginService;

    @Autowired
    @Getter private TimeseriesService tsService;

    //@Autowired
    //@Getter private AttributesService attributesService;

    @Autowired
    @Getter private EventService eventService;

    //@Autowired
    //@Getter private AlarmService alarmService;

    @Autowired
    @Getter @Setter private PluginWebSocketMsgEndpoint wsMsgEndpoint;

    @Value("${actors.session.sync.timeout}")
    @Getter private long syncSessionTimeout;

    @Value("${actors.plugin.termination.delay}")
    @Getter private long pluginActorTerminationDelay;

    @Value("${actors.plugin.processing.timeout}")
    @Getter private long pluginProcessingTimeout;

    @Value("${actors.plugin.error_persist_frequency}")
    @Getter private long pluginErrorPersistFrequency;

    @Value("${actors.rule.termination.delay}")
    @Getter private long ruleActorTerminationDelay;

    @Value("${actors.rule.error_persist_frequency}")
    @Getter private long ruleErrorPersistFrequency;

    @Value("${actors.statistics.enabled}")
    @Getter private boolean statisticsEnabled;

    @Value("${actors.statistics.persist_frequency}")
    @Getter private long statisticsPersistFrequency;

    @Getter @Setter private ActorSystem actorSystem;

    @Getter @Setter private ActorRef appActor;

    @Getter @Setter private ActorRef sessionManagerActor;

    @Getter @Setter private ActorRef statsActor;

    @Getter private final Config config;

    public ActorSystemContext() {
        config = ConfigFactory.parseResources(AKKA_CONF_FILE_NAME).withFallback(ConfigFactory.load());
    }

    public Scheduler getScheduler() {
        return actorSystem.scheduler();
    }

    public void persistError(EntityId entityId, String method, Exception e) {
        Event event = new Event();

        event.setEntityId(entityId);
        event.setType(DataConstants.ERROR);
        event.setBody(toBodyJson(discoveryService.getCurrentServer().getServerAddress(), method, toString(e)));
        persistEvent(event);
    }

    public void persistLifecycleEvent(EntityId entityId, ComponentLifecycleEvent lcEvent, Exception e) {
        Event event = new Event();
        event.setEntityId(entityId);
        event.setType(DataConstants.LC_EVENT);
        event.setBody(toBodyJson(discoveryService.getCurrentServer().getServerAddress(), lcEvent, Optional.ofNullable(e)));
        persistEvent(event);
    }

    private void persistEvent(Event event) {
        eventService.save(event);
    }

    private String toString(Exception e) {
        StringWriter sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }

    private JsonNode toBodyJson(ServerAddress server, ComponentLifecycleEvent event, Optional<Exception> e) {
        ObjectNode node = mapper.createObjectNode().put("server", server.toString()).put("event", event.name());
        if (e.isPresent()) {
            node = node.put("success", false);
            node = node.put("error", toString(e.get()));
        } else {
            node = node.put("success", true);
        }
        return node;
    }

    private JsonNode toBodyJson(ServerAddress server, String method, String body) {
        return mapper.createObjectNode().put("server", server.toString()).put("method", method).put("error", body);
    }
}
