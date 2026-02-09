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
package org.alfresco.event.gateway.kafka.autoconfigure.storage;

import org.alfresco.event.gateway.kafka.consumption.EventConsumerRegistry;
import org.alfresco.event.gateway.kafka.storage.KafkaEventStore;
import org.alfresco.event.gateway.kafka.subscription.RepoEventToJsonTransformer;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.dsl.context.IntegrationFlowContext;

@Configuration
public class EventStorageConfiguration {

    @ConditionalOnExpression("'${alfresco.event.gateway.storage.kafka.bootstrapServers:}' != '' || '${spring.kafka.bootstrap-servers:}' != ''")
    @Bean
    KafkaEventStore kafkaEventConsumer(EventConsumerRegistry eventConsumerRegistry,
            IntegrationFlowContext integrationFlowContext,
            RepoEventToJsonTransformer repoEventToJsonTransformer,
            @Value("${alfresco.event.gateway.storage.kafka.bootstrapServers:}") final String gatewayBootstrapServers,
            @Value("${spring.kafka.bootstrap-servers:}") final String springBootstrapServers,
            @Value("${alfresco.event.gateway.storage.kafka.topic:alfresco-event-gateway}") final String topic) {
        String bootstrapServers = StringUtils.isNotBlank(gatewayBootstrapServers) ? gatewayBootstrapServers
                : springBootstrapServers;
        return new KafkaEventStore(eventConsumerRegistry, integrationFlowContext, repoEventToJsonTransformer,
                bootstrapServers, topic);
    }
}
