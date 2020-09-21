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
import org.junit.Test;
import org.mockito.Mockito;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

public class RepeatFlowTest {

    @Test
    public void testRepeatUntil() throws Exception{
        // given
        Work work = Mockito.mock(Work.class);
        WorkContext workContext = Mockito.mock(WorkContext.class);
        WorkReportPredicate predicate = WorkReportPredicate.ALWAYS_FALSE;
        RepeatFlow repeatFlow = RepeatFlow.Builder.aNewRepeatFlow()
                .repeat(work)
                .until(predicate)
                .withWorkContext(workContext)
                .build();

        // when
        when(work.withWorkContext(workContext)).thenReturn(work);
        repeatFlow.call();

        // then
        Mockito.verify(work, Mockito.times(1)).call();
    }

    @Test
    public void testRepeatTimes() throws Exception{
        // given
        Work work = Mockito.mock(Work.class);
        WorkContext workContext = Mockito.mock(WorkContext.class);
        RepeatFlow repeatFlow = RepeatFlow.Builder.aNewRepeatFlow()
                .repeat(work)
                .times(3)
                .withWorkContext(workContext)
                .build();

        // when
        when(work.withWorkContext(workContext)).thenReturn(work);
        repeatFlow.withWorkContext(workContext).call();

        // then
        Mockito.verify(work, Mockito.times(3)).call();
    }

    @Test
    public void testRepeatAndTerminate() throws Exception{
        // given
        Work work = new FiveSecondsDelayWork();
        RepeatFlow repeatFlow = RepeatFlow.Builder.aNewRepeatFlow()
                .repeat(work)
                .times(3)
                .build();

        // when
        ExecutorService executor = Executors.newFixedThreadPool(1);
        Future<WorkReport> futureWorkReport = executor.submit(repeatFlow);
        Thread.sleep(6000);
        repeatFlow.terminate(true);
        Thread.sleep(5000);

        // then
        assertThat(futureWorkReport.isCancelled());
        assertThat(FiveSecondsDelayWork.executions == 2);
    }

    static class FiveSecondsDelayWork extends Work {

        public static int executions = 0;

        @Override
        public String getName() {
            return "Five seconds delay work";
        }

        @Override
        public WorkReport call() throws InterruptedException {
            executions++;
            System.out.println("Start working...");
            Thread.sleep(5000);
            System.out.println("Finished working...");
            return new DefaultWorkReport(WorkStatus.COMPLETED, workContext);
        }
    }

}
