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

import org.jeasy.flows.work.DefaultWorkReport;
import org.jeasy.flows.work.Work;
import org.jeasy.flows.work.WorkContext;
import org.jeasy.flows.work.WorkReport;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.jeasy.flows.work.WorkStatus.*;

/**
 * A sequential flow executes a set of work units in sequence.
 * <p>
 * If a init of work fails, next work units in the pipeline will be skipped.
 *
 * @author Mahmoud Ben Hassine (mahmoud.benhassine@icloud.com)
 */
public class SequentialFlow extends AbstractWorkFlow {

    private static final Logger LOGGER = Logger.getLogger(SequentialFlow.class.getName());

    private List<Work> works = new ArrayList<>();

    private List<Future<WorkReport>> futureWorkReportList = new ArrayList<>();

    SequentialFlow(String name, List<Work> works, WorkContext workContext) {
        super(name, workContext);
        this.works.addAll(works);
    }

    /**
     * {@inheritDoc}
     */
    public WorkReport call() {
        WorkReport workReport = null;
        try {
            ExecutorService executor = Executors.newFixedThreadPool(1);

            works.forEach(work -> futureWorkReportList.add(executor.submit(work.withWorkContext(workContext))));

            for (int a = 0; a < futureWorkReportList.size(); a++) {
                try {
                    workReport = futureWorkReportList.get(a).get();
                    if (FAILED.equals(futureWorkReportList.get(a).get().getStatus())) {
                        LOGGER.log(Level.INFO, "Work unit ''{0}'' has failed, skipping subsequent work units", works.get(a).getName());
                        break;
                    }
                } catch (CancellationException ce) {
                    LOGGER.log(Level.INFO, "Type: " + ce.getClass().getName() + "   Message: " + ce.getMessage());
                    workReport = new DefaultWorkReport(TERMINATED, workContext, ce);
                    return workReport;
                } catch (Exception e) {
                    LOGGER.log(Level.INFO, "Work unit ''{0}'' has failed, skipping subsequent work units...", "TODO");
                    workReport = new DefaultWorkReport(FAILED, workContext, e);
                    return workReport;
                }
            }
            return workReport;
        } catch (Exception e) {
            LOGGER.log(Level.INFO, "Work unit ''{0}'' has failed, skipping subsequent work units...", "TODO");
            workReport = new DefaultWorkReport(FAILED, workContext, e);
            return workReport;
        }
    }

    public void terminate(boolean mayInterruptIfRunning) {
        futureWorkReportList.forEach(futureWorkReport -> futureWorkReport.cancel(mayInterruptIfRunning));
    }

    public List<Future<WorkReport>> getFutureWorkReportList() {
        return futureWorkReportList;
    }

    public static class Builder {

        private String name;
        private List<Work> works;
        private WorkContext workContext;

        private Builder() {
            this.name = UUID.randomUUID().toString();
            this.works = new ArrayList<>();
            this.workContext = new WorkContext();
        }

        public static SequentialFlow.Builder aNewSequentialFlow() {
            return new SequentialFlow.Builder();
        }

        public SequentialFlow.Builder named(String name) {
            this.name = name;
            return this;
        }

        public SequentialFlow.Builder withWorkContext(WorkContext workContext) {
            this.workContext = workContext;
            return this;
        }

        public SequentialFlow.Builder execute(Work work) {
            this.works.add(work);
            return this;
        }

        public SequentialFlow.Builder then(Work work) {
            this.works.add(work);
            return this;
        }

        public SequentialFlow build() {
            return new SequentialFlow(name, works, workContext);
        }
    }
}
