/*
 * Copyright (c) 2010-2016. Axon Framework
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.axonframework.eventhandling;

import org.axonframework.common.Registration;
import org.axonframework.common.io.IOUtils;
import org.axonframework.messaging.unitofwork.RollbackConfiguration;
import org.axonframework.messaging.unitofwork.RollbackConfigurationType;
import org.axonframework.monitoring.MessageMonitor;
import org.axonframework.monitoring.NoOpMessageMonitor;

import java.util.function.Consumer;

/**
 * Event processor implementation that {@link EventBus#subscribe(Consumer) subscribes} to the {@link EventBus} for
 * events. Events published on the event bus are supplied to this processor in the publishing thread.
 * <p>
 * Depending on the given {@link EventProcessingStrategy} the events are processed directly (in the publishing thread)
 * or asynchronously.
 *
 * @author Rene de Waele
 */
public class SubscribingEventProcessor extends AbstractEventProcessor {

    private final EventBus eventBus;
    private final EventProcessingStrategy processingStrategy;
    private volatile Registration eventBusRegistration;


    /**
     * Initializes an EventProcessor with given {@code name} that subscribes to the given {@code eventBus} for events.
     * Actual handling of event messages is deferred to the given {@code eventHandlerInvoker}.
     * <p>
     * The EventProcessor is initialized with a {@link DirectEventProcessingStrategy}, a {@link NoOpErrorHandler} and
     * a {@link RollbackConfigurationType#ANY_THROWABLE}.
     *
     * @param name                The name of the event processor
     * @param eventHandlerInvoker The component that handles the individual events
     * @param eventBus            The EventBus to which this event processor will subscribe
     */
    public SubscribingEventProcessor(String name, EventHandlerInvoker eventHandlerInvoker, EventBus eventBus) {
        this(name, eventHandlerInvoker, eventBus, NoOpMessageMonitor.INSTANCE);
    }

    /**
     * Initializes an EventProcessor with given {@code name} that subscribes to the given {@code eventBus} for events.
     * Actual handling of event messages is deferred to the given {@code eventHandlerInvoker}.
     * <p>
     * The EventProcessor is initialized with a {@link DirectEventProcessingStrategy}, a {@link NoOpErrorHandler} and
     * a {@link RollbackConfigurationType#ANY_THROWABLE}.
     *
     * @param name                The name of the event processor
     * @param eventHandlerInvoker The component that handles the individual events
     * @param eventBus            The EventBus to which this event processor will subscribe
     * @param messageMonitor      Monitor to be invoked before and after event processing
     */
    public SubscribingEventProcessor(String name, EventHandlerInvoker eventHandlerInvoker, EventBus eventBus,
                                     MessageMonitor<? super EventMessage<?>> messageMonitor) {
        this(name, eventHandlerInvoker, RollbackConfigurationType.ANY_THROWABLE, eventBus,
             DirectEventProcessingStrategy.INSTANCE, NoOpErrorHandler.INSTANCE, messageMonitor);
    }

    /**
     * Initializes an EventProcessor with given {@code name} that subscribes to the given {@code eventBus} for events.
     * Actual handling of event messages is deferred to the given {@code eventHandlerInvoker}.
     *
     * @param name                  The name of the event processor
     * @param eventHandlerInvoker   The component that handles the individual events
     * @param rollbackConfiguration Determines rollback behavior of the UnitOfWork while processing a batch of events
     * @param eventBus              The EventBus to which this event processor will subscribe
     * @param processingStrategy    Strategy that determines whether events are processed directly or asynchronously
     * @param errorHandler          Invoked when a UnitOfWork is rolled back during processing
     * @param messageMonitor        Monitor to be invoked before and after event processing
     */
    public SubscribingEventProcessor(String name, EventHandlerInvoker eventHandlerInvoker,
                                     RollbackConfiguration rollbackConfiguration, EventBus eventBus,
                                     EventProcessingStrategy processingStrategy, ErrorHandler errorHandler,
                                     MessageMonitor<? super EventMessage<?>> messageMonitor) {
        super(name, eventHandlerInvoker, rollbackConfiguration, errorHandler, messageMonitor);
        this.eventBus = eventBus;
        this.processingStrategy = processingStrategy;
    }

    /**
     * Start this processor. This will register the processor with the {@link EventBus}.
     */
    @Override
    public void start() {
        eventBusRegistration =
                eventBus.subscribe(eventMessages -> processingStrategy.handle(eventMessages, this::process));
    }

    /**
     * Shut down this processor. This will deregister the processor with the {@link EventBus}.
     */
    @Override
    public void shutDown() {
        IOUtils.closeQuietly(eventBusRegistration);
    }
}
