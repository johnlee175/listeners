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

import com.johnsoft.listeners.ListenerExecutor;

/**
 * @author John Kenrinus Lee
 * @version 2016-07-17
 */
public final class TaskQueueListenerExecutor extends AbstractListenerExecutor {
    private final TaskThread thread;

    public TaskQueueListenerExecutor(BlockingQueue<Runnable> queue, ListenerExecutor.Mode mode, boolean isCoverUnexectuedMode) {
        super(mode, isCoverUnexectuedMode);
        this.thread = new TaskThread(queue);
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
            e.printStackTrace();
        }
    }

    @Override
    public CancelController doExecute(Runnable runnable) {
        if (thread.post(runnable)) {
            return new TaskThreadCancelController(thread, runnable);
        }
        return null;
    }

    public static final class TaskThreadCancelController implements CancelController {
        private final TaskThread taskThread;
        private final Runnable runnable;

        public TaskThreadCancelController(TaskThread taskThread, Runnable runnable) {
            this.taskThread = taskThread;
            this.runnable = runnable;
        }

        @Override
        public void cancel() {
            try {
                if (taskThread != null && runnable != null) {
                    taskThread.cancel(runnable);
                }
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }
    }

    public static final class TaskThread extends Thread {
        private final BlockingQueue<Runnable> queue;

        public TaskThread(BlockingQueue<Runnable> queue) {
            this.queue = queue;
        }

        public boolean post(Runnable runnable) {
            return queue.offer(runnable);
        }

        public boolean cancel(Runnable runnable) {
            return queue.remove(runnable);
        }

        @Override
        public void run() {
            try {
                while (!isInterrupted()) {
                    final Runnable task = queue.take();
                    if (task != null) {
                        try {
                            task.run();
                        } catch (Throwable e) {
                            e.printStackTrace();
                        }
                    }
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
