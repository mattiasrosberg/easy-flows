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
import org.mockito.InOrder;
import org.mockito.Mockito;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

public class SequentialFlowTest {

    @Test
    public void call() throws Exception{
        // given
        Work work1 = Mockito.mock(Work.class);
        Work work2 = Mockito.mock(Work.class);
        Work work3 = Mockito.mock(Work.class);
        WorkContext workContext = Mockito.mock(WorkContext.class);
        SequentialFlow sequentialFlow = SequentialFlow.Builder.aNewSequentialFlow()
                .named("testFlow")
                .execute(work1)
                .then(work2)
                .then(work3)
                .withWorkContext(workContext)
                .build();

        // when
        when(work1.withWorkContext(workContext)).thenReturn(work1);
        when(work2.withWorkContext(workContext)).thenReturn(work2);
        when(work3.withWorkContext(workContext)).thenReturn(work3);
        sequentialFlow.call();

        // then
        InOrder inOrder = Mockito.inOrder(work1, work2, work3);
        inOrder.verify(work1, Mockito.times(1)).call();
        inOrder.verify(work2, Mockito.times(1)).call();
        inOrder.verify(work3, Mockito.times(1)).call();
    }

    @Test
    public void callAndTerminate() throws Exception{
        // given
        Work work1 = new FiveSecondsDelayWork();
        Work work2 = new FiveSecondsDelayWork();
        Work work3 = new FiveSecondsDelayWork();
        SequentialFlow sequentialFlow = SequentialFlow.Builder.aNewSequentialFlow()
                .named("testFlow")
                .execute(work1)
                .then(work2)
                .then(work3)
                .build();

        // when
        ExecutorService executor = Executors.newFixedThreadPool(1);
        Future<WorkReport> futureWorkReport = executor.submit(sequentialFlow);
        Thread.sleep(6000);
        sequentialFlow.terminate(true);
        Thread.sleep(5000);

        // then
        assertThat(sequentialFlow.getFutureWorkReportList().get(0).isDone()).isTrue();
        assertThat(sequentialFlow.getFutureWorkReportList().get(1).isCancelled()).isTrue();
        assertThat(sequentialFlow.getFutureWorkReportList().get(2).isCancelled()).isTrue();
    }

    static class FiveSecondsDelayWork extends Work {

        @Override
        public String getName() {
            return "Five seconds delay work";
        }

        @Override
        public WorkReport call() throws InterruptedException {
            System.out.println("Start working...");
            Thread.sleep(5000);
            System.out.println("Finished working...");
            return new DefaultWorkReport(WorkStatus.COMPLETED, workContext);
        }
    }

}
