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
 * A conditional flow is defined by 4 artifacts:
 *
 * <ul>
 *     <li>The work to execute first</li>
 *     <li>A predicate for the conditional logic</li>
 *     <li>The work to execute if the predicate is satisfied</li>
 *     <li>The work to execute if the predicate is not satisfied (optional)</li>
 * </ul>
 *
 * @see ConditionalFlow.Builder
 *
 * @author Mahmoud Ben Hassine (mahmoud.benhassine@icloud.com)
 */
public class ConditionalFlow extends AbstractWorkFlow {

    private Work toExecute, nextOnPredicateSuccess, nextOnPredicateFailure;
    private WorkReportPredicate predicate;
    private Future<WorkReport> futureWorkReport;

    ConditionalFlow(String name, Work toExecute, Work nextOnPredicateSuccess, Work nextOnPredicateFailure, WorkReportPredicate predicate, WorkContext workContext) {
        super(name, workContext);
        this.toExecute = toExecute;
        this.nextOnPredicateSuccess = nextOnPredicateSuccess;
        this.nextOnPredicateFailure = nextOnPredicateFailure;
        this.predicate = predicate;
        this.workContext = workContext;
    }

    /**
     * {@inheritDoc}
     */
    public WorkReport call() {
        try {
            ExecutorService executor = Executors.newFixedThreadPool(1);
            futureWorkReport = executor.submit(toExecute.withWorkContext(workContext));
            WorkReport jobReport = futureWorkReport.get();
            if (predicate.apply(jobReport)) {
                futureWorkReport = executor.submit(nextOnPredicateSuccess.withWorkContext(workContext));
                jobReport = futureWorkReport.get();
            } else {
                if (nextOnPredicateFailure != null && !(nextOnPredicateFailure instanceof NoOpWork)) { // else is optional
                    futureWorkReport = executor.submit(nextOnPredicateFailure.withWorkContext(workContext));
                    jobReport = futureWorkReport.get();
                }
            }
            return jobReport;
        }catch (Exception e){
            return new DefaultWorkReport(WorkStatus.FAILED, workContext, e);
        }
    }

    public void terminate(boolean mayInterruptIfRunning) {
        System.out.println("Terminating");
        if(futureWorkReport != null) {
            futureWorkReport.cancel(mayInterruptIfRunning);
        }
    }

    public static class Builder {

        private String name;
        private Work toExecute, nextOnPredicateSuccess, nextOnPredicateFailure;
        private WorkReportPredicate predicate;
        private WorkContext workContext;

        private Builder() {
            this.name = UUID.randomUUID().toString();
            this.toExecute = new NoOpWork();
            this.nextOnPredicateSuccess = new NoOpWork();
            this.nextOnPredicateFailure = new NoOpWork();
            this.predicate = WorkReportPredicate.ALWAYS_FALSE;
            this.workContext = new WorkContext();
        }

        public static ConditionalFlow.Builder aNewConditionalFlow() {
            return new ConditionalFlow.Builder();
        }

        public ConditionalFlow.Builder named(String name) {
            this.name = name;
            return this;
        }

        public ConditionalFlow.Builder execute(Work work) {
            this.toExecute = work;
            return this;
        }

        public ConditionalFlow.Builder when(WorkReportPredicate predicate) {
            this.predicate = predicate;
            return this;
        }

        public ConditionalFlow.Builder then(Work work) {
            this.nextOnPredicateSuccess = work;
            return this;
        }

        public ConditionalFlow.Builder otherwise(Work work) {
            this.nextOnPredicateFailure = work;
            return this;
        }

        public ConditionalFlow.Builder withWorkContext(WorkContext workContext) {
            this.workContext = workContext;
            return this;
        }

        public ConditionalFlow build() {
            return new ConditionalFlow(name, toExecute, nextOnPredicateSuccess, nextOnPredicateFailure, predicate, workContext);
        }
    }
}
