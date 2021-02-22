package com.example.demo.scheduler;

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
public class ErrorRateSchedulerWithConfig {
    private static final Map<Object, Object> errorMap = new HashMap<>();
    @Value("#{'${hosts}'.split(',')}")
    private String[] hosts;
    @Value("#{'${ports}'.split(',')}")
    private String[] ports;
    @Value("${error.rate.scheduler.with.conf.enable}")
    private Boolean errorRateSchedulerEnable;

    @Value("${error.with.config.file.path}")
    private String errorFilePath;

    @Value("#{'${error.rate.metrics}'.split(';')}")
    private List<String> errorMetrics;
    private Map<String, String> errorMetricsAttribute;


    @Value("#{${error.metric.attributes}}")
    public void setErrorMetricAttributes(Map<String, String> errorMetricsAttribute) {
        setErrorMetricsAttribute(errorMetricsAttribute);
    }

    private void setErrorMetricsAttribute(Map<String, String> errorMetricsAttribute) {
        this.errorMetricsAttribute = errorMetricsAttribute;
    }

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
        List<Map<Object, Object>> result = new ArrayList<>();
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

                    List<Map<Object, Object>> errorRate = objectMapper.readValue(jsonData, List.class);
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

    private void getFailureMetric(MBeanServerConnection mbeanConn, List<Map<Object, Object>> result, Map<Object, Object> resultMap, String node) throws MalformedObjectNameException, InstanceNotFoundException, IntrospectionException, ReflectionException, IOException, MBeanException, AttributeNotFoundException {

        Map<Object, Object> resultMetric = new HashMap<>();


        try {
            for (String errorMetric : errorMetrics) {

                ObjectName mbean = new ObjectName(errorMetric);


                if (!resultMap.containsKey(node + Constants.PREVIOUS + errorMetricsAttribute.get(errorMetric))) {
                    resultMap.put(node + Constants.PREVIOUS + errorMetricsAttribute.get(errorMetric), mbeanConn.getAttribute(mbean, Constants.COUNT));

                } else {

                    Long count = (Long) mbeanConn.getAttribute(mbean, Constants.COUNT);

                    Long previousCount = (Long) resultMap.get(node + Constants.PREVIOUS + errorMetricsAttribute.get(errorMetric));
                    Long countResult = Math.abs(count - previousCount);
                    resultMetric.put(errorMetricsAttribute.get(errorMetric), countResult);
                    resultMetric.put(errorMetricsAttribute.get(errorMetric) + Constants.MEAN_RATE, mbeanConn.getAttribute(mbean, Constants.MEAN_RATE));
                    resultMap.put(node + Constants.PREVIOUS + errorMetricsAttribute.get(errorMetric), count);
                    resultMetric.put(Constants.EPOCH, System.currentTimeMillis());
                }
            }
            if (!resultMetric.isEmpty()) {
                result.add(resultMetric);
            }


        } catch (Exception e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
    }
}
