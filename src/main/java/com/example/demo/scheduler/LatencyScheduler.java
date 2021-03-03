package com.example.demo.scheduler;

import com.example.demo.model.Latency;
import com.example.demo.utility.CommonUtility;
import com.example.demo.utility.Constants;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import javax.management.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

@Component
/**
 *This scheduler measure the latency of read, write and range
 */
public class LatencyScheduler {
    public static Map<Object, Object> resultMap = new HashMap<>();
    @Value("#{'${hosts}'.split(',')}")
    private String[] hosts;
    @Value("#{'${ports}'.split(',')}")
    private String[] ports;
    @Value("${latency.scheduler.enable}")
    private Boolean latencySchedulerEnable;
    @Value("${latency.file.path}")
    private String latencyFilePath;
    @Value("${latency.time.unit:ms}")
    private String timeUnit;


    @Scheduled(fixedRateString = "${latency.scheduler.time}")
    public void perform() throws Exception {
        if (latencySchedulerEnable) {
            ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(hosts.length);
            for (int i = 0; i < hosts.length; i++) {
                String host = hosts[i];
                String port = ports[i];
                executor.submit(() -> {
                    try {
                        writeLatencyData(host, port);
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
    private void writeLatencyData(String host, String port) throws IOException, MalformedObjectNameException, InstanceNotFoundException, IntrospectionException, ReflectionException, MBeanException, AttributeNotFoundException, InterruptedException {
        MBeanServerConnection mbeanConn = CommonUtility.getmBeanServerConnection(host, port);
        List<Latency> result = new ArrayList<>();
        getLatencyMetric(mbeanConn, result, host + port);

        if (!CollectionUtils.isEmpty(result)) {
            ObjectMapper mapper = new ObjectMapper();
            try {

                // Writing to a file
                String fileName = latencyFilePath + host + port + "--" + ".json";
                File tempFile = new File(fileName);
                boolean exists = tempFile.exists();
                // Writing to a file
                if (exists) {
                    byte[] jsonData = Files.readAllBytes(Paths.get(fileName));
                    ObjectMapper objectMapper = new ObjectMapper();

                    List<Latency> errorRate = objectMapper.readValue(jsonData, List.class);
                    errorRate.addAll(result);
                    result = errorRate;
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
    public void getLatencyMetric(MBeanServerConnection mbeanConn, List<Latency> result, String node) throws MalformedObjectNameException, InstanceNotFoundException, IntrospectionException, ReflectionException, IOException, MBeanException, AttributeNotFoundException {
        try {

            ObjectName mbeanReadLatency = new ObjectName("org.apache.cassandra.metrics:type=ClientRequest,scope=Read,name=Latency");
            ObjectName mbeanWriteLatency = new ObjectName("org.apache.cassandra.metrics:type=ClientRequest,scope=Write,name=Latency");
            ObjectName mbeanTotalReadLatency = new ObjectName("org.apache.cassandra.metrics:type=ClientRequest,scope=Read,name=TotalLatency");
            ObjectName mbeanTotalWriteLatency = new ObjectName("org.apache.cassandra.metrics:type=ClientRequest,scope=Write,name=TotalLatency");
            ObjectName mbeanRangeLatency = new ObjectName("org.apache.cassandra.metrics:type=ClientRequest,scope=RangeSlice,name=Latency");
            ObjectName mbeanTotalRangeLatency = new ObjectName("org.apache.cassandra.metrics:type=ClientRequest,scope=RangeSlice,name=TotalLatency");

            if (resultMap.isEmpty() || (!resultMap.containsKey(node + Constants.PREVIOUS_READ_LATENCY))) {
                resultMap.put(node + Constants.PREVIOUS_READ_LATENCY, new Long( (Long) mbeanConn.getAttribute(mbeanReadLatency, Constants.COUNT)).doubleValue());

                resultMap.put(node + Constants.PREVIOUS_TOTAL_READ_LATENCY, new Long( (Long) mbeanConn.getAttribute(mbeanTotalReadLatency, Constants.COUNT)).doubleValue());

                resultMap.put(node + Constants.PREVIOUS_WRITE_LATENCY, new Long( (Long) mbeanConn.getAttribute(mbeanWriteLatency, Constants.COUNT)).doubleValue());

                resultMap.put(node + Constants.PREVIOUS_TOTAL_WRITE_LATENCY, new Long( (Long) mbeanConn.getAttribute(mbeanTotalWriteLatency, Constants.COUNT)).doubleValue());


                resultMap.put(node + Constants.PREVIOUS_RANGE_LATENCY, new Long( (Long) mbeanConn.getAttribute(mbeanRangeLatency, Constants.COUNT)).doubleValue());

                resultMap.put(node + Constants.PREVIOUS_TOTAL_RANGE_LATENCY, new Long( (Long)mbeanConn.getAttribute(mbeanTotalRangeLatency, Constants.COUNT)).doubleValue());

                return;
            }
            Latency latency = new Latency();

            getReadLatencyData(mbeanConn, node, mbeanReadLatency, mbeanTotalReadLatency, latency);


            getWriteLatencyData(mbeanConn, node, mbeanWriteLatency, mbeanTotalWriteLatency, latency);


            getRangeLatencyData(mbeanConn, node, mbeanRangeLatency, mbeanTotalRangeLatency, latency);

            latency.setEpoch(System.currentTimeMillis());

            result.add(latency);

        } catch (Exception ex) {
            System.out.println(ex.getMessage());
        }
    }

    /**
     *
     * @param mbeanConn
     * @param node
     * @param mbeanRangeLatency
     * @param mbeanTotalRangeLatency
     * @param latency
     * @throws MBeanException
     * @throws AttributeNotFoundException
     * @throws InstanceNotFoundException
     * @throws ReflectionException
     * @throws IOException
     */
    private void getRangeLatencyData(MBeanServerConnection mbeanConn, String node, ObjectName mbeanRangeLatency, ObjectName mbeanTotalRangeLatency, Latency latency) throws MBeanException, AttributeNotFoundException, InstanceNotFoundException, ReflectionException, IOException {
        Double rangeLatency = new Long( (Long) mbeanConn.getAttribute(mbeanRangeLatency, Constants.COUNT)).doubleValue();
        Double totalRangeLatency =  new Long( (Long) mbeanConn.getAttribute(mbeanTotalRangeLatency, Constants.COUNT)).doubleValue();
        Double previousRangeLatency = (Double) resultMap.get(node + Constants.PREVIOUS_RANGE_LATENCY);
        Double previousTotalRangeLatency = (Double) resultMap.get(node + Constants.PREVIOUS_TOTAL_RANGE_LATENCY);
        Double rangeLatencyResult = 0d;
        if (rangeLatency - previousRangeLatency != 0) {
            rangeLatencyResult = Math.abs(totalRangeLatency - previousTotalRangeLatency) / (rangeLatency - previousRangeLatency);
        }
        Double totalRangeLatencyResult = Math.abs(totalRangeLatency - previousTotalRangeLatency);
        latency.setRangeLatency(rangeLatencyResult * Constants.TIME_UNIT.get(timeUnit));
        latency.setTotalRangeLatency(totalRangeLatencyResult * Constants.TIME_UNIT.get(timeUnit));
        resultMap.put(node + Constants.PREVIOUS_RANGE_LATENCY, rangeLatency);
        resultMap.put(node + Constants.PREVIOUS_TOTAL_RANGE_LATENCY, totalRangeLatency);
    }

    /**
     *
     * @param mbeanConn
     * @param node
     * @param mbeanWriteLatency
     * @param mbeanTotalWriteLatency
     * @param latency
     * @throws MBeanException
     * @throws AttributeNotFoundException
     * @throws InstanceNotFoundException
     * @throws ReflectionException
     * @throws IOException
     */
    private void getWriteLatencyData(MBeanServerConnection mbeanConn, String node, ObjectName mbeanWriteLatency, ObjectName mbeanTotalWriteLatency, Latency latency) throws MBeanException, AttributeNotFoundException, InstanceNotFoundException, ReflectionException, IOException {
        Double writeLatency = new Long( (Long) mbeanConn.getAttribute(mbeanWriteLatency, Constants.COUNT)).doubleValue();
        Double totalWriteLatency = new Long( (Long) mbeanConn.getAttribute(mbeanTotalWriteLatency, Constants.COUNT)).doubleValue();
        Double previousWriteLatency = (Double) resultMap.get(node + Constants.PREVIOUS_WRITE_LATENCY);
        Double previousTotalWriteLatency = (Double) resultMap.get(node + Constants.PREVIOUS_TOTAL_WRITE_LATENCY);
        Double writeLatencyResult = 0d;
        if (writeLatency - previousWriteLatency != 0) {
            writeLatencyResult = Math.abs(totalWriteLatency - previousTotalWriteLatency) / (writeLatency - previousWriteLatency);

        }
        Double totalWriteLatencyResult = Math.abs(totalWriteLatency - previousTotalWriteLatency);
        latency.setWriteLatency(writeLatencyResult * Constants.TIME_UNIT.get(timeUnit));
        latency.setTotalWriteLatency(totalWriteLatencyResult * Constants.TIME_UNIT.get(timeUnit));
        resultMap.put(node + Constants.PREVIOUS_WRITE_LATENCY, writeLatency * Constants.TIME_UNIT.get(timeUnit));
        resultMap.put(node + Constants.PREVIOUS_TOTAL_WRITE_LATENCY, totalWriteLatency);
    }

    /**
     *
     * @param mbeanConn
     * @param node
     * @param mbeanReadLatency
     * @param mbeanTotalReadLatency
     * @param latency
     * @throws MBeanException
     * @throws AttributeNotFoundException
     * @throws InstanceNotFoundException
     * @throws ReflectionException
     * @throws IOException
     */
    private void getReadLatencyData(MBeanServerConnection mbeanConn, String node, ObjectName mbeanReadLatency, ObjectName mbeanTotalReadLatency, Latency latency) throws MBeanException, AttributeNotFoundException, InstanceNotFoundException, ReflectionException, IOException {
        Double readLatency = new Long( (Long) mbeanConn.getAttribute(mbeanReadLatency, Constants.COUNT)).doubleValue();
        Double totalReadLatency = new Long( (Long) mbeanConn.getAttribute(mbeanTotalReadLatency, Constants.COUNT)).doubleValue();
        Double previousReadLatency = (Double) resultMap.get(node + Constants.PREVIOUS_READ_LATENCY);
        Double previousTotalReadLatency = (Double) resultMap.get(node + Constants.PREVIOUS_TOTAL_READ_LATENCY);
        Double readLatencyResult = 0d;
        if (readLatency - previousReadLatency != 0) {
            readLatencyResult = Math.abs(totalReadLatency - previousTotalReadLatency) / (readLatency - previousReadLatency);
        }
        Double totalReadLatencyResult = Math.abs(totalReadLatency - previousTotalReadLatency);
        latency.setReadLatency(readLatencyResult * Constants.TIME_UNIT.get(timeUnit));
        latency.setTotalReadLatency(totalReadLatencyResult * Constants.TIME_UNIT.get(timeUnit));
        resultMap.put(node + Constants.PREVIOUS_READ_LATENCY, readLatency);
        resultMap.put(node + Constants.PREVIOUS_TOTAL_READ_LATENCY, totalReadLatency);
    }
}
