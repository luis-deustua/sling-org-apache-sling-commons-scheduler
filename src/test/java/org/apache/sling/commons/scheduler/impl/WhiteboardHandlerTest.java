/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.commons.scheduler.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.lang.reflect.Field;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;

import org.apache.sling.commons.scheduler.Scheduler;
import org.apache.sling.testing.mock.osgi.MockOsgi;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.SchedulerException;
import org.quartz.impl.matchers.GroupMatcher;

public class WhiteboardHandlerTest {
    private WhiteboardHandler handler;
    private BundleContext context;
    private QuartzScheduler quartzScheduler;

    @Before
    public void setUp() throws Exception {
        context = MockOsgi.newBundleContext();
        handler = new WhiteboardHandler();

        //Getting private field through injection
        Field schedulerField = WhiteboardHandler.class.getDeclaredField("scheduler");
        schedulerField.setAccessible(true);

        // Creating quartz scheduler for private field and activating it
        quartzScheduler = ActivatedQuartzSchedulerFactory.create(context, "testName");

        //Injecting quartzScheduler to WhiteboardHandler
        schedulerField.set(handler, quartzScheduler);
    }

    @Test
    public void testAddingService() throws SchedulerException {
        Thread service = new Thread();
        String schedulerName = "testScheduler";
        Long period = 1L;
        Integer times = 2;

        Dictionary<String, Object> serviceProps = new Hashtable<>();
        serviceProps.put(Scheduler.PROPERTY_SCHEDULER_RUN_ON, Scheduler.VALUE_RUN_ON_LEADER);
        serviceProps.put(Scheduler.PROPERTY_SCHEDULER_CONCURRENT, Boolean.FALSE);
        serviceProps.put(Scheduler.PROPERTY_SCHEDULER_IMMEDIATE, Boolean.FALSE);
        serviceProps.put(Scheduler.PROPERTY_SCHEDULER_NAME, schedulerName);
        serviceProps.put(Scheduler.PROPERTY_SCHEDULER_PERIOD, period);
        serviceProps.put(Scheduler.PROPERTY_SCHEDULER_TIMES, times);
        serviceProps.put(Constants.SERVICE_PID, "1");
        serviceProps.put(Constants.SERVICE_ID, 1L);

        final ServiceRegistration<?> reg = context.registerService(Runnable.class.getName(), service, serviceProps);
        final ServiceReference<?> reference = reg.getReference();
        handler.register(reference, service);
        JobKey jobKey = JobKey.jobKey(schedulerName);

        assertNotNull(quartzScheduler.getSchedulers().get("testName").getScheduler().getJobDetail(jobKey));
    }

    @Test
    public void testUnregisterService() throws SchedulerException {
        Thread service = new Thread();
        String schedulerName = "testScheduler";
        Long period = 1L;
        Integer times = 2;

        Dictionary<String, Object> serviceProps = new Hashtable<>();
        serviceProps.put(Scheduler.PROPERTY_SCHEDULER_RUN_ON, Scheduler.VALUE_RUN_ON_LEADER);
        serviceProps.put(Scheduler.PROPERTY_SCHEDULER_EXPRESSION, "0 * * * * ?");
        serviceProps.put(Scheduler.PROPERTY_SCHEDULER_CONCURRENT, Boolean.FALSE);
        serviceProps.put(Scheduler.PROPERTY_SCHEDULER_IMMEDIATE, Boolean.FALSE);
        serviceProps.put(Scheduler.PROPERTY_SCHEDULER_NAME, schedulerName);
        serviceProps.put(Scheduler.PROPERTY_SCHEDULER_PERIOD, period);
        serviceProps.put(Scheduler.PROPERTY_SCHEDULER_TIMES, times);
        serviceProps.put(Constants.SERVICE_PID, "1");
        serviceProps.put(Constants.SERVICE_ID, 1L);

        //Register service only to get a reference of it.
        //This reference is needed to test our method.
        final ServiceRegistration<?> reg = context.registerService(Runnable.class.getName(), service, serviceProps);
        ServiceReference<?> reference = reg.getReference();
        handler.register(reference, service);
        JobKey jobKey = JobKey.jobKey(schedulerName);

        assertNotNull(quartzScheduler.getSchedulers().get("testName").getScheduler().getJobDetail(jobKey));

        reg.unregister();
        handler.unregister(reference);
        assertNull(quartzScheduler.getSchedulers().get("testName").getScheduler().getJobDetail(jobKey));
    }

    // SLING-7037
    @Test
    public void testAddingServiceWithProvidedName() throws SchedulerException {
        Thread service = new Thread();
        String schedulerName = "testScheduler";
        Long period = 1L;
        Integer times = 2;

        Dictionary<String, Object> serviceProps = new Hashtable<>();
        serviceProps.put(Scheduler.PROPERTY_SCHEDULER_RUN_ON, Scheduler.VALUE_RUN_ON_LEADER);
        serviceProps.put(Scheduler.PROPERTY_SCHEDULER_CONCURRENT, Boolean.FALSE);
        serviceProps.put(Scheduler.PROPERTY_SCHEDULER_IMMEDIATE, Boolean.FALSE);
        serviceProps.put(Scheduler.PROPERTY_SCHEDULER_NAME, schedulerName);
        serviceProps.put(Scheduler.PROPERTY_SCHEDULER_PERIOD, period);
        serviceProps.put(Scheduler.PROPERTY_SCHEDULER_TIMES, times);
        serviceProps.put(Constants.SERVICE_PID, "1");

        final ServiceRegistration<?> reg = context.registerService(Runnable.class.getName(), service, serviceProps);
        final ServiceReference<?> reference = reg.getReference();
        handler.register(reference, service);
        JobKey jobKey = JobKey.jobKey(schedulerName);

        JobDetail jobDetail = quartzScheduler.getSchedulers().get("testName").getScheduler().getJobDetail(jobKey);
        assertNotNull(jobDetail);
        assertEquals(schedulerName, jobDetail.getJobDataMap().getString(QuartzScheduler.DATA_MAP_PROVIDED_NAME));
        assertEquals(schedulerName,
                jobDetail.getJobDataMap().getString(QuartzScheduler.DATA_MAP_NAME));
    }

    @Test
    public void testAddingServiceWithoutProvidedName() throws SchedulerException {
        Thread service = new Thread();
        Long period = 1L;
        Integer times = 2;

        Dictionary<String, Object> serviceProps = new Hashtable<>();
        serviceProps.put(Scheduler.PROPERTY_SCHEDULER_RUN_ON, Scheduler.VALUE_RUN_ON_LEADER);
        serviceProps.put(Scheduler.PROPERTY_SCHEDULER_CONCURRENT, Boolean.FALSE);
        serviceProps.put(Scheduler.PROPERTY_SCHEDULER_IMMEDIATE, Boolean.FALSE);
        serviceProps.put(Scheduler.PROPERTY_SCHEDULER_PERIOD, period);
        serviceProps.put(Scheduler.PROPERTY_SCHEDULER_TIMES, times);
        serviceProps.put(Constants.SERVICE_PID, "1");

        final ServiceRegistration<?> reg = context.registerService(Runnable.class.getName(), service, serviceProps);
        final ServiceReference<?> reference = reg.getReference();
        handler.register(reference, service);

        JobDetail jobDetail = null;
        org.quartz.Scheduler scheduler = quartzScheduler.getSchedulers().get("testName").getScheduler();
        final List<String> groups = scheduler.getJobGroupNames();
        for(final String group : groups) {
            final Set<JobKey> keys = scheduler.getJobKeys(GroupMatcher.jobGroupEquals(group));
            for(final JobKey key : keys) {
                final JobDetail detail = scheduler.getJobDetail(key);
                if ( detail != null && detail.getJobDataMap().get(QuartzScheduler.DATA_MAP_SERVICE_ID).equals(reg.getReference().getProperty(Constants.SERVICE_ID))) {
                    jobDetail = detail;
                    break;
                }
            }
        }
        assertNotNull(jobDetail);
        assertNull(jobDetail.getJobDataMap().getString(QuartzScheduler.DATA_MAP_PROVIDED_NAME));
        assertNotNull(jobDetail.getJobDataMap().getString(QuartzScheduler.DATA_MAP_NAME));
    }

    @After
    public void deactivateScheduler() {
        quartzScheduler.deactivate(context);
    }
}
