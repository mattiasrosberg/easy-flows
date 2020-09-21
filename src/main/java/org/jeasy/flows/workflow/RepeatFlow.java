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

import org.jeasy.flows.work.*;

import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * A repeat flow executes a work repeatedly until its report satisfies a given predicate.
 *
 * @author Mahmoud Ben Hassine (mahmoud.benhassine@icloud.com)
 */
public class RepeatFlow extends AbstractWorkFlow {

    private Work work;
    private WorkReportPredicate predicate;
    private Future<WorkReport> workReportFuture;

    RepeatFlow(String name, Work work, WorkReportPredicate predicate, WorkContext workContext) {
        super(name, workContext);
        this.work = work;
        this.predicate = predicate;
    }

    /**
     * {@inheritDoc}
     */
    public WorkReport call() {
        WorkReport workReport = null;
        ExecutorService executor = Executors.newFixedThreadPool(1);
        try {
            do {
                workReportFuture = executor.submit(work.withWorkContext(workContext));
                workReport = workReportFuture.get();
            } while (predicate.apply(workReport));
            return workReport;
        } catch (Exception e) {
            return new DefaultWorkReport(WorkStatus.FAILED, workContext, e);
        }
    }

    public void terminate(boolean mayInterruptIfRunning) {
        System.out.println("Terminating");
        if(workReportFuture != null) {
            workReportFuture.cancel(mayInterruptIfRunning);
        }
    }

    public static class Builder {

        private String name;
        private Work work;
        private WorkReportPredicate predicate;
        private WorkContext workContext;

        private Builder() {
            this.name = UUID.randomUUID().toString();
            this.work = new NoOpWork();
            this.predicate = WorkReportPredicate.ALWAYS_FALSE;
            this.workContext = new WorkContext();
        }

        public static RepeatFlow.Builder aNewRepeatFlow() {
            return new RepeatFlow.Builder();
        }

        public RepeatFlow.Builder named(String name) {
            this.name = name;
            return this;
        }

        public RepeatFlow.Builder repeat(Work work) {
            this.work = work;
            return this;
        }

        public RepeatFlow.Builder times(int times) {
            return until(WorkReportPredicate.TimesPredicate.times(times));
        }

        public RepeatFlow.Builder until(WorkReportPredicate predicate) {
            this.predicate = predicate;
            return this;
        }

        public RepeatFlow.Builder withWorkContext(WorkContext workContext) {
            this.workContext = workContext;
            return this;
        }

        public RepeatFlow build() {
            return new RepeatFlow(name, work, predicate, workContext);
        }
    }
}
