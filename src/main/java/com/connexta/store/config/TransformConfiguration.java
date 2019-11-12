/*
 * Copyright (c) 2019 Connexta, LLC
 *
 * Released under the GNU Lesser General Public License version 3; see
 * https://www.gnu.org/licenses/lgpl-3.0.html
 */
package com.connexta.store.config;

import com.connexta.poller.service.Poller;
import com.connexta.poller.service.StatusService;
import java.net.URI;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class TransformConfiguration {
  @Bean
  public String transformApiVersion(
      @NotBlank @Value("${endpoints.transform.version}") String transformApiVersion) {
    return transformApiVersion;
  }

  @Bean("input")
  public BlockingQueue<URI> transferQueue() {
    return new LinkedBlockingQueue<>(100000);
  }

  @Bean
  public ExecutorService executorService() {
    return Executors.newFixedThreadPool(64);
  }

  @Bean
  public StatusService statusService(@NotNull final ExecutorService executorService) {
    return new StatusService(executorService, WebClient.create());
  }

  @Bean
  public Poller pollerService(
      @NotNull final StatusService statusService,
      @NotNull @Qualifier("input") final BlockingQueue transferQueue) {
    return new Poller(statusService, transferQueue);
  }
}