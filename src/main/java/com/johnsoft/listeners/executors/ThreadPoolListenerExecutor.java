/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache license, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the license for the specific language governing permissions and
 * limitations under the license.
 */
package com.johnsoft.listeners.executors;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import com.johnsoft.listeners.ListenerFoundation;

/**
 * Thread pool executor
 *
 * @author John Kenrinus Lee
 * @version 2016-07-17
 */
public final class ThreadPoolListenerExecutor extends AbstractListenerExecutor {
    private final ExecutorService executorService;

    public ThreadPoolListenerExecutor(ListenerFoundation foundation, ExecutorService executorService) {
        super(foundation);
        if (executorService == null) {
            this.executorService = Executors.newCachedThreadPool();
        } else {
            this.executorService = executorService;
        }
    }

    @Override
    protected boolean doInitialize() {
        return true;
    }

    @Override
    public void doDestroy() {
        if (executorService != null && !executorService.isShutdown() && !executorService.isTerminated()) {
            executorService.shutdown();
            boolean force;
            try {
                force = !executorService.awaitTermination(1000L, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                force = true;
            }
            if (force && !executorService.isTerminated()) {
                executorService.shutdownNow();
                try {
                    executorService.awaitTermination(500L, TimeUnit.MILLISECONDS);
                } catch (InterruptedException ignored) {
                    // await silently
                }
            }
        }
    }

    @Override
    public Cancelable doExecute(Executable executable) {
        final FutureCancelController controller = new FutureCancelController(foundation, executable);
        final Future<?> future = executorService.submit(controller);
        controller.setFuture(future);
        return controller;
    }

    private static final class FutureCancelController implements Executable {
        private final ListenerFoundation foundation;
        private final Executable executable;
        private volatile Future<?> future;

        FutureCancelController(ListenerFoundation foundation,
                                      Executable executable) {
            this.foundation = foundation;
            this.executable = executable;
        }

        void setFuture(Future<?> future) {
            this.future = future;
        }

        @Override
        public void cancel() {
            try {
                if (executable != null) {
                    executable.cancel();
                }
                if (future != null && !future.isCancelled() && !future.isDone()) {
                    future.cancel(true);
                }
            } catch (Throwable e) {
                foundation.catchThrowable(e);
            }
        }

        @Override
        public void run() {
            if (executable != null) {
                try {
                    executable.run();
                } catch (Throwable e) {
                    foundation.catchThrowable(new ExecutionException(e));
                }
            }
        }
    }
}
