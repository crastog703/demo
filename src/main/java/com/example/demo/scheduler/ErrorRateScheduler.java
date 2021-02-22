package com.example.demo.scheduler;

import com.example.demo.model.ErrorRate;
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
public class ErrorRateScheduler {
    public static Map<Object, Object> errorMap = new HashMap<>();
    @Value("#{'${hosts}'.split(',')}")
    private String[] hosts;
    @Value("#{'${ports}'.split(',')}")
    private String[] ports;
    @Value("${error.rate.scheduler.enable}")
    private Boolean errorRateSchedulerEnable;

    @Value("${error.file.path}")
    private String errorFilePath;

    @Scheduled(fixedRateString = "${error.scheduler.time}")
    public void perform() throws Exception {
        if (errorRateSchedulerEnable) {


            ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(hosts.length);
            for (int i = 0; i < hosts.length; i++) {
                String host = hosts[i];
                String port = ports[i];
                executor.submit(() -> {
                    try {
                        errorRateData(host, port);
                    } catch (Exception ex) {
                        System.out.println(ex.getMessage());
                        ex.printStackTrace();
                    }
                });
            }


        }
    }

    private void errorRateData(String host, String port) throws IOException, MalformedObjectNameException, InstanceNotFoundException, IntrospectionException, ReflectionException, MBeanException, AttributeNotFoundException, InterruptedException {
        MBeanServerConnection mbeanConn = CommonUtility.getmBeanServerConnection(host, port);
        List<ErrorRate> result = new ArrayList<>();
        getFailureMetric(mbeanConn, result, errorMap, host + port);


        if (!CollectionUtils.isEmpty(result)) {
            ObjectMapper mapper = new ObjectMapper();
            try {
                String fileName = errorFilePath + host + port + ".json";

                File tempFile = new File(fileName);
                boolean exists = tempFile.exists();
                // Writing to a file
                if (exists) {
                    byte[] jsonData = Files.readAllBytes(Paths.get(fileName));
                    ObjectMapper objectMapper = new ObjectMapper();

                    List<ErrorRate> errorRate = objectMapper.readValue(jsonData, List.class);
                    errorRate.addAll(result);
                    result = errorRate;
                }

                mapper.writeValue(new File(fileName), result);

            } catch (Exception e) {
                e.printStackTrace();
                System.out.println(e.getMessage());
            }

        }

    }


    public void getFailureMetric(MBeanServerConnection mbeanConn, List<ErrorRate> result, Map<Object, Object> resultMap, String node) throws MalformedObjectNameException, InstanceNotFoundException, IntrospectionException, ReflectionException, IOException, MBeanException, AttributeNotFoundException {
        try {

            ObjectName mbeanReadFailure = new ObjectName("org.apache.cassandra.metrics:type=ClientRequest,scope=Read,name=Failures");
            ObjectName mbeanWriteFailure = new ObjectName("org.apache.cassandra.metrics:type=ClientRequest,scope=Write,name=Failures");

            if (resultMap.isEmpty() || (!resultMap.containsKey(node + Constants.PREVIOUS_READ_FAILURE))) {
                resultMap.put(node + Constants.PREVIOUS_READ_FAILURE, (Long) mbeanConn.getAttribute(mbeanReadFailure, Constants.COUNT));


                resultMap.put(node + Constants.PREVIOUS_WRITE_FAILURE, (Long) mbeanConn.getAttribute(mbeanWriteFailure, Constants.COUNT));


                return;
            }
            ErrorRate errorRate = new ErrorRate();
            getReadFailureData(mbeanConn, resultMap, node, mbeanReadFailure, mbeanWriteFailure, errorRate);


            getWriteFailureData(mbeanConn, resultMap, node, mbeanWriteFailure, errorRate);
            errorRate.setEpoch(System.currentTimeMillis());

            result.add(errorRate);

        } catch (Exception e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
    }

    private void getWriteFailureData(MBeanServerConnection mbeanConn, Map<Object, Object> resultMap, String node, ObjectName mbeanWriteFailure, ErrorRate errorRate) throws MBeanException, AttributeNotFoundException, InstanceNotFoundException, ReflectionException, IOException {
        Long writeFailure = (Long) mbeanConn.getAttribute(mbeanWriteFailure, Constants.COUNT);

        Long previousWriteFailure = (Long) resultMap.get(node + Constants.PREVIOUS_WRITE_FAILURE);
        Long writeFailureResult = Math.abs(writeFailure - previousWriteFailure);
        errorRate.setWriteError(writeFailureResult);
        errorRate.setMeanWriteErrorRate((Double) mbeanConn.getAttribute(mbeanWriteFailure, Constants.MEAN_RATE));
        resultMap.put(node + Constants.PREVIOUS_WRITE_FAILURE, writeFailure);
    }

    private void getReadFailureData(MBeanServerConnection mbeanConn, Map<Object, Object> resultMap, String node, ObjectName mbeanReadFailure, ObjectName mbeanWriteFailure, ErrorRate errorRate) throws MBeanException, AttributeNotFoundException, InstanceNotFoundException, ReflectionException, IOException {
        Long readFailure = (Long) mbeanConn.getAttribute(mbeanReadFailure, Constants.COUNT);

        Long previousReadFailure = (Long) resultMap.get(node + Constants.PREVIOUS_READ_FAILURE);
        Long readFailureResult = Math.abs(readFailure - previousReadFailure);
        errorRate.setReadError(readFailureResult);
        errorRate.setMeanReadErrorRate((Double) mbeanConn.getAttribute(mbeanWriteFailure, Constants.MEAN_RATE));
        resultMap.put(node + Constants.PREVIOUS_READ_FAILURE, readFailure);
    }
}
