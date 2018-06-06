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

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingQueue;

import com.johnsoft.listeners.ListenerFoundation;

/**
 * Loop async thread executor
 *
 * @author John Kenrinus Lee
 * @version 2016-07-17
 */
public final class TaskQueueListenerExecutor extends AbstractListenerExecutor {
    private final TaskThread thread;

    public TaskQueueListenerExecutor(ListenerFoundation foundation, BlockingQueue<Executable> queue) {
        super(foundation);
        if (queue == null) {
            queue = new LinkedBlockingQueue<>();
        }
        this.thread = new TaskThread(foundation, queue);
    }

    @Override
    public boolean doInitialize() {
        thread.start();
        return true;
    }

    @Override
    public void doDestroy() {
        thread.interrupt();
        try {
            thread.join(1000L);
        } catch (InterruptedException e) {
            foundation.catchThrowable(e);
        }
    }

    @Override
    public Cancelable doExecute(Executable executable) {
        if (thread.post(executable)) {
            return new TaskThreadCancelController(foundation, thread, executable);
        }
        return null;
    }

    private static final class TaskThreadCancelController implements Cancelable {
        private final ListenerFoundation foundation;
        private final TaskThread taskThread;
        private final Executable executable;

        TaskThreadCancelController(ListenerFoundation foundation,
                                          TaskThread taskThread, Executable executable) {
            this.foundation = foundation;
            this.taskThread = taskThread;
            this.executable = executable;
        }

        @Override
        public void cancel() {
            try {
                if (taskThread != null && executable != null) {
                    executable.cancel();
                    taskThread.cancel(executable);
                }
            } catch (Throwable e) {
                foundation.catchThrowable(e);
            }
        }
    }

    public static final class TaskThread extends Thread {
        private final ListenerFoundation foundation;
        private final BlockingQueue<Executable> queue;

        public TaskThread(ListenerFoundation foundation, BlockingQueue<Executable> queue) {
            this.foundation = foundation;
            this.queue = queue;
        }

        public boolean post(Executable executable) {
            return queue.offer(executable);
        }

        public boolean cancel(Executable executable) {
            return queue.remove(executable);
        }

        @Override
        public void run() {
            try {
                while (!isInterrupted()) {
                    final Executable task = queue.take();
                    if (task != null) {
                        try {
                            task.run();
                        } catch (Throwable e) {
                            foundation.catchThrowable(new ExecutionException(e));
                        }
                    }
                }
            } catch (InterruptedException e) {
                foundation.catchThrowable(e);
            }
        }
    }
}
