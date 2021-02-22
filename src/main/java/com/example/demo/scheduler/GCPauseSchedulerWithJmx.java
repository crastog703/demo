package com.example.demo.scheduler;

import com.example.demo.model.Stopwatch;
import com.example.demo.utility.CommonUtility;
import com.example.demo.utility.Constants;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.management.GarbageCollectorMXBean;
import com.sun.management.GcInfo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import javax.management.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Component
/**
 *
 */
public class GCPauseSchedulerWithJmx {

    static Map<Object, Object> resultMap = new HashMap<>();
    @Value("#{'${gc.log.file.path}'.split(',')}")
    private String[] logFilePath;
    @Value("${gc.pause.scheduler.jmx.enable}")
    private Boolean gcPauseWithJMXEnable;
    @Value("${gc.pause.file.path}")
    private String gcFilePath;
    @Value("#{'${hosts}'.split(',')}")
    private String[] hosts;
    @Value("#{'${ports}'.split(',')}")
    private String[] ports;
    @Value("${gc.pause.scheduler.time}")
    private String gcPauseSchedulerTime;

    @Value("${gc.pause.duration.limit}")
    private String gcPauseLimit;

    @Scheduled(fixedRateString = "${gc.pause.scheduler.time}")
    public void perform() throws Exception {
        if (gcPauseWithJMXEnable) {
            ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(hosts.length);
            for (int i = 0; i < hosts.length; i++) {
                String port = ports[i];
                String host = hosts[i];

                executor.submit(() -> {
                    try {
                        gcPauseData(host, port);
                    } catch (Exception e) {
                        e.printStackTrace();
                        System.out.println(e.getMessage());
                    }
                });
            }

        }

    }

    /**
     *
     * @param host
     * @param port
     * @throws IOException
     * @throws MalformedObjectNameException
     * @throws InstanceNotFoundException
     * @throws IntrospectionException
     * @throws ReflectionException
     * @throws MBeanException
     * @throws AttributeNotFoundException
     * @throws InterruptedException
     */
    private void gcPauseData(String host, String port) throws IOException, MalformedObjectNameException, InstanceNotFoundException, IntrospectionException, ReflectionException, MBeanException, AttributeNotFoundException, InterruptedException {
        MBeanServerConnection mbeanConn = CommonUtility.getmBeanServerConnection(host, port);
        List<GcInfo> result = new ArrayList<>();

        Stopwatch timer1 = new Stopwatch();
        while (timer1.elapsedTime() < Long.parseLong(gcPauseSchedulerTime)) {
            getGcPauseMetric(mbeanConn, result, host + port);
            TimeUnit.NANOSECONDS.sleep(1);
        }


        if (!CollectionUtils.isEmpty(result)) {


            ObjectMapper mapper = new ObjectMapper();
            try {

                // Writing to a file
                String fileName = gcFilePath + host + port + "--" + ".json";
                File tempFile = new File(fileName);
                boolean exists = tempFile.exists();
                // Writing to a file
                if (exists) {
                    byte[] jsonData = Files.readAllBytes(Paths.get(fileName));
                    ObjectMapper objectMapper = new ObjectMapper();

                    List<GcInfo> compositeData = objectMapper.readValue(jsonData, List.class);
                    compositeData.addAll(result);
                    result = compositeData;
                }
                mapper.writeValue(new File(fileName), result);

            } catch (Exception e) {
                System.out.println(e.getMessage());
                e.printStackTrace();
            }

        }
    }

    /**
     *
     * @param mbeanConn
     * @param result
     * @param node
     * @throws MalformedObjectNameException
     * @throws InstanceNotFoundException
     * @throws IntrospectionException
     * @throws ReflectionException
     * @throws IOException
     * @throws MBeanException
     * @throws AttributeNotFoundException
     */
    public void getGcPauseMetric(MBeanServerConnection mbeanConn, List<GcInfo> result, String node) throws MalformedObjectNameException, InstanceNotFoundException, IntrospectionException, ReflectionException, IOException, MBeanException, AttributeNotFoundException {
        try {

            ObjectName mbeanConcurrentMarkSweep = new ObjectName("java.lang:type=GarbageCollector,name=ConcurrentMarkSweep");
            ObjectName mbeanParNew = new ObjectName("java.lang:type=GarbageCollector,name=ParNew");
            Set<ObjectName> beanSetConcurrentMarkSweep = mbeanConn.queryNames(mbeanConcurrentMarkSweep, null);
            final ObjectName beanConcurrentMarkSweep = beanSetConcurrentMarkSweep.iterator().next();
            GarbageCollectorMXBean gcBeanConcurrentMarkSweep = JMX.newMXBeanProxy(mbeanConn, beanConcurrentMarkSweep, GarbageCollectorMXBean.class);
            GcInfo concurrentMarkSweep = gcBeanConcurrentMarkSweep.getLastGcInfo();

            Set<ObjectName> beanSetParNew = mbeanConn.queryNames(mbeanParNew, null);
            final ObjectName beanParNew = beanSetParNew.iterator().next();
            GarbageCollectorMXBean gcBeanParNew = JMX.newMXBeanProxy(mbeanConn, beanParNew, GarbageCollectorMXBean.class);
            GcInfo parNew = gcBeanParNew.getLastGcInfo();

            processConcurrentMarkSweep(concurrentMarkSweep, result, node);
            processParNew(parNew, result, node);


        } catch (Exception e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     *
     * @param concurrentMarkSweep
     * @param result
     * @param node
     */
    private void processConcurrentMarkSweep(GcInfo concurrentMarkSweep, List<GcInfo> result, String node) {
        if (concurrentMarkSweep != null) {

            if (resultMap.containsKey(node + Constants.PREVIOUS_CONCURRENT_MARK_SWEEP)) {
                Long id = (Long) resultMap.get(node + Constants.PREVIOUS_CONCURRENT_MARK_SWEEP);
                Long tempId = (Long) concurrentMarkSweep.getId();
                if (!tempId.equals(id)) {
                    resultMap.put(node + Constants.PREVIOUS_CONCURRENT_MARK_SWEEP, concurrentMarkSweep.getId());
                    if (concurrentMarkSweep.getDuration() > Long.parseLong(gcPauseLimit)) {

                        result.add(concurrentMarkSweep);


                    }

                }
            } else {
                resultMap.put(node + Constants.PREVIOUS_CONCURRENT_MARK_SWEEP, concurrentMarkSweep.getId());
                if (concurrentMarkSweep.getDuration() > Long.parseLong(gcPauseLimit)) {
                    result.add(concurrentMarkSweep);

                }
            }
        }

    }

    /**
     *
     * @param parNew
     * @param result
     * @param node
     */
    private void processParNew(GcInfo parNew, List<GcInfo> result, String node) {
        if (parNew != null) {


            if (resultMap.containsKey(node + Constants.PREVIOUSPAR_NEW)) {
                Long id = (Long) resultMap.get(node + Constants.PREVIOUSPAR_NEW);
                Long tempId = parNew.getId();
                if (!tempId.equals(id)) {
                    resultMap.put(node + Constants.PREVIOUSPAR_NEW, parNew.getId());
                    if (parNew.getDuration() > Long.parseLong(gcPauseLimit)) {
                        result.add(parNew);
                    }

                }
            } else {
                resultMap.put(node + Constants.PREVIOUSPAR_NEW, parNew.getId());
                if (parNew.getDuration() > Long.parseLong(gcPauseLimit)) {
                    result.add(parNew);

                }

            }

        }
    }
}
