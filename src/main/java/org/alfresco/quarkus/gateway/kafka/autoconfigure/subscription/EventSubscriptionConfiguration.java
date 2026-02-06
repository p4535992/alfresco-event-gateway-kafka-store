/*
 * Copyright 2021-2021 Alfresco Software, Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.alfresco.quarkus.gateway.kafka.autoconfigure.subscription;

import static org.alfresco.quarkus.gateway.kafka.subscription.filter.EventFilterConfigurationConstants.EVENT_TYPE;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.alfresco.quarkus.gateway.kafka.SubscriptionConfigurationConstants;
import org.alfresco.quarkus.gateway.kafka.bootstrapping.SystemBootstrapChecker;
import org.alfresco.quarkus.gateway.kafka.consumption.EventConsumerRegistry;
import org.alfresco.quarkus.gateway.kafka.subscription.DefaultEventSubscriptionFactory;
import org.alfresco.quarkus.gateway.kafka.subscription.DefaultEventSubscriptionRegistry;
import org.alfresco.quarkus.gateway.kafka.subscription.EventSubscriptionFactory;
import org.alfresco.quarkus.gateway.kafka.subscription.EventSubscriptionRegistry;
import org.alfresco.quarkus.gateway.kafka.subscription.EventSubscriptionService;
import org.alfresco.quarkus.gateway.kafka.subscription.EventSubscriptionServiceImpl;
import org.alfresco.quarkus.gateway.kafka.subscription.RepoEventToJsonTransformer;
import org.alfresco.quarkus.gateway.kafka.subscription.SubscriptionPublisherFactory;
import org.alfresco.quarkus.gateway.kafka.subscription.filter.EventFilterFactory;
import org.alfresco.quarkus.gateway.kafka.subscription.filter.EventTypeFilterFactory;
import org.alfresco.quarkus.gateway.kafka.subscription.handling.SubscriptionDisableUserDeletionHandler;
import org.alfresco.quarkus.gateway.kafka.subscription.jms.JmsActiveMQSubscriptionPublisherFactory;
import org.alfresco.quarkus.gateway.kafka.subscription.jms.SpringPropertiesBrokerConfigResolver;
import org.alfresco.quarkus.gateway.kafka.subscription.jms.destination.ActiveMQDestinationResolver;
import org.alfresco.quarkus.gateway.kafka.subscription.jms.destination.JmsDestinationValidator;
import org.alfresco.quarkus.gateway.kafka.subscription.rest.EventSubscriptionController;
import org.alfresco.quarkus.gateway.kafka.subscription.rest.SubscriptionPatchValidator;
import org.alfresco.quarkus.gateway.kafka.subscription.scheduled.ScheduledSubscriptionStatusTask;
import org.alfresco.quarkus.gateway.kafka.subscription.storage.EventSubscriptionStorage;
import org.alfresco.quarkus.gateway.kafka.subscription.transformation.EventTransformation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.integration.dsl.context.IntegrationFlowContext;

import com.fasterxml.jackson.databind.ObjectMapper;

@Configuration
public class EventSubscriptionConfiguration {

    SubscriptionPublisherFactory jmsActiveMQSubscriptionPublisherFactory(IntegrationFlowContext integrationFlowContext,
            RepoEventToJsonTransformer repoEventToJsonTransformer, Environment environment,
            SystemBootstrapChecker systemBootstrapChecker) {
        return new JmsActiveMQSubscriptionPublisherFactory(integrationFlowContext, repoEventToJsonTransformer,
                new ActiveMQDestinationResolver(new JmsDestinationValidator(), systemBootstrapChecker),
                new SpringPropertiesBrokerConfigResolver(environment));
    }

    @Bean
    Map<String, SubscriptionPublisherFactory> subscriptionPublisherFactoryMap(
            IntegrationFlowContext integrationFlowContext,
            RepoEventToJsonTransformer repoEventToJsonTransformer, Environment environment,
            SystemBootstrapChecker systemBootstrapChecker) {
        Map<String, SubscriptionPublisherFactory> subscriptionPublisherFactoryMap = new HashMap<>();
        subscriptionPublisherFactoryMap.put(SubscriptionConfigurationConstants.SUBSCRIPTION_TYPE_JMS_ACTIVEMQ,
                jmsActiveMQSubscriptionPublisherFactory(integrationFlowContext, repoEventToJsonTransformer, environment,
                        systemBootstrapChecker));

        return subscriptionPublisherFactoryMap;
    }

    @Bean(EVENT_TYPE)
    EventTypeFilterFactory eventTypeFilterFactory() {
        return new EventTypeFilterFactory();
    }

    @Bean
    EventSubscriptionFactory eventSubscriptionFactory(
            Map<String, SubscriptionPublisherFactory> subscriptionPublisherFactoryMap,
            Map<String, EventFilterFactory> eventFilterFactoryMap,
            List<EventTransformation> eventTransformations) {
        return new DefaultEventSubscriptionFactory(subscriptionPublisherFactoryMap, eventFilterFactoryMap,
                eventTransformations);
    }

    @Bean
    RepoEventToJsonTransformer repoEventToJsonTransformer(ObjectMapper objectMapper) {
        return new RepoEventToJsonTransformer(objectMapper);
    }

    @Bean
    EventSubscriptionService eventSubscriptionService(EventSubscriptionStorage eventSubscriptionStorage,
            EventSubscriptionFactory eventSubscriptionFactory, EventConsumerRegistry eventConsumerRegistry,
            EventSubscriptionRegistry eventSubscriptionRegistry) {
        return new EventSubscriptionServiceImpl(eventSubscriptionStorage, eventSubscriptionFactory,
                eventConsumerRegistry, eventSubscriptionRegistry);
    }

    @Bean
    SubscriptionPatchValidator subscriptionPatchValidation() {
        return new SubscriptionPatchValidator();
    }

    @Bean
    EventSubscriptionController eventSubscriptionController(EventSubscriptionService eventSubscriptionService,
            ObjectMapper objectMapper,
            SubscriptionPatchValidator subscriptionPatchValidator) {
        return new EventSubscriptionController(eventSubscriptionService, objectMapper, subscriptionPatchValidator);
    }

    @Bean
    EventSubscriptionRegistry eventSubscriptionRegistry() {
        return new DefaultEventSubscriptionRegistry();
    }

    @Bean
    @ConditionalOnProperty(name = "scheduled.subscription.status.enabled", havingValue = "true", matchIfMissing = true)
    ScheduledSubscriptionStatusTask scheduledSubscriptionStatusTask(EventSubscriptionStorage eventSubscriptionStorage,
            EventSubscriptionService eventSubscriptionService,
            @Value("${scheduled.subscription.limit.timestamp:86400000}") long limitTime) {
        return new ScheduledSubscriptionStatusTask(eventSubscriptionStorage, eventSubscriptionService, limitTime);
    }

    @Bean
    SubscriptionDisableUserDeletionHandler subscriptionDisableUserDeletionHandler(
            EventSubscriptionService eventSubscriptionService) {
        return new SubscriptionDisableUserDeletionHandler(eventSubscriptionService);
    }
}
