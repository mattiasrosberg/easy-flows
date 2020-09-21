/*
 * The MIT License
 *
 *  Copyright (c) 2020, Mahmoud Ben Hassine (mahmoud.benhassine@icloud.com)
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in
 *  all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *  THE SOFTWARE.
 */
package org.jeasy.flows.workflow;

import org.jeasy.flows.work.Work;
import org.jeasy.flows.work.WorkContext;
import org.jeasy.flows.work.WorkReport;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;

/**
 * A parallel flow executes a set of work units in parallel. A {@link ParallelFlow}
 * requires a {@link ExecutorService} to run work units in parallel using multiple
 * threads.
 *
 * <strong>It is the responsibility of the caller to manage the lifecycle of the
 * executor service.</strong>
 *
 * The status of a parallel flow execution is defined as:
 *
 * <ul>
 *     <li>{@link org.jeasy.flows.work.WorkStatus#COMPLETED}: If all work units have successfully completed</li>
 *     <li>{@link org.jeasy.flows.work.WorkStatus#FAILED}: If one of the work units has failed</li>
 * </ul>
 *
 * @author Mahmoud Ben Hassine (mahmoud.benhassine@icloud.com)
 */
public class ParallelFlow extends AbstractWorkFlow {

    private List<Work> works = new ArrayList<>();
    private ParallelFlowExecutor workExecutor;

    ParallelFlow(String name, List<Work> works, ParallelFlowExecutor parallelFlowExecutor, WorkContext workContext) {
        super(name, workContext);
        this.works.addAll(works);
        this.workExecutor = parallelFlowExecutor;
    }

    /**
     * {@inheritDoc}
     */
    public ParallelFlowReport call() {
        ParallelFlowReport workFlowReport = new ParallelFlowReport();
        List<WorkReport> workReports = workExecutor.executeInParallel(works, workContext);
        workFlowReport.addAll(workReports);
        return workFlowReport;
    }

    public static class Builder {

        private String name;
        private List<Work> works;
        private ExecutorService executorService;
        private WorkContext workContext;

        private Builder(ExecutorService executorService) {
            this.name = UUID.randomUUID().toString();
            this.works = new ArrayList<>();
            this.executorService = executorService;
            this.workContext = new WorkContext();
        }

        /**
         *  Create a new {@link ParallelFlow} builder. A {@link ParallelFlow}
         *  requires a {@link ExecutorService} to run work units in parallel
         *  using multiple threads.
         *
         *  <strong>It is the responsibility of the caller to manage the lifecycle
         *  of the executor service.</strong>
         *
         * @param executorService to use to run work units in parallel
         * @return a new {@link ParallelFlow} builder
         */
        public static ParallelFlow.Builder aNewParallelFlow(ExecutorService executorService) {
            return new ParallelFlow.Builder(executorService);
        }

        public ParallelFlow.Builder named(String name) {
            this.name = name;
            return this;
        }

        public ParallelFlow.Builder execute(Work... works) {
            this.works.addAll(Arrays.asList(works));
            return this;
        }

        public ParallelFlow.Builder withContext(WorkContext workContext) {
            this.workContext = workContext;
            return this;
        }

        public ParallelFlow build() {
            return new ParallelFlow(name, works, new ParallelFlowExecutor(executorService), workContext);
        }
    }
}
