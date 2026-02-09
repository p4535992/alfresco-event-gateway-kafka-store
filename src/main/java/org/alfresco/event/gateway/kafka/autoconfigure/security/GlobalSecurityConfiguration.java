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
package org.alfresco.event.gateway.kafka.autoconfigure.security;

import java.util.Set;

import org.alfresco.core.handler.GroupsApiClient;
import org.alfresco.event.gateway.kafka.security.ACSAuthoritiesService;
import org.alfresco.event.gateway.kafka.security.HeadersForwardDelegatedAuthenticationProvider;
import org.alfresco.event.gateway.kafka.security.SubscriptionOwnerValidator;
import org.alfresco.event.gateway.kafka.subscription.EventSubscriptionService;
import org.alfresco.rest.sdk.feign.DelegatedAuthenticationProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class GlobalSecurityConfiguration {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health",
                                "/api-docs",
                                "/configuration/ui",
                                "/swagger-resources/**",
                                "/configuration/security",
                                "/swagger-ui.html",
                                "/webjars/**",
                                "/swagger-ui/**")
                        .permitAll()
                        .anyRequest()
                        .permitAll());
        return http.build();
    }

    @Bean
    public ACSAuthoritiesService acsAuthoritiesService(GroupsApiClient groupsApiClient) {
        return new ACSAuthoritiesService(groupsApiClient);
    }

    @Bean
    public DelegatedAuthenticationProvider headersForwardDelegatedAuthenticationProvider() {
        // Forward the value of the current request authorization header to the feign client template
        return new HeadersForwardDelegatedAuthenticationProvider(Set.of("authorization"));
    }

    @Bean
    public SubscriptionOwnerValidator subscriptionOwnerValidator(EventSubscriptionService eventSubscriptionService) {
        return new SubscriptionOwnerValidator(eventSubscriptionService);
    }
}
