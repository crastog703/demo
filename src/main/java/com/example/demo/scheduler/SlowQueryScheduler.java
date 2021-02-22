package com.example.demo.scheduler;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import javax.management.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
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
 *
 */
public class SlowQueryScheduler {

    public static final String SLOW_QUERY_FILTER = "slow timeout";
    public static Map<String, Long> lastKnownPositions = new HashMap<>();
    Map<String, File> files = new HashMap<>();
    @Value("#{'${log.file.path}'.split(',')}")
    private String[] debugLogFilePaths;
    @Value("${slow.query.scheduler.enable}")
    private Boolean slowQuerySchedulerEnable;
    @Value("${slowquery.file.path}")
    private String slowQueryFilePath;

    public File getFile(String path) {
        if (files.containsKey(path)) {
            return files.get(path);
        } else {
            File temp = new File(path);
            files.put(path, temp);
            return temp;
        }


    }

    /**
     *
     * @param path
     * @return
     */
    public Long getLastKnownPosition(String path) {
        if (lastKnownPositions.containsKey(path)) {
            return lastKnownPositions.get(path);
        } else {
            Long temp = new Long(0);
            lastKnownPositions.put(path, temp);
            return temp;
        }


    }

    /**
     *
     * @throws Exception
     */
    @Scheduled(fixedRateString = "${slow.query.scheduler.time}")
    public void perform() throws Exception {
        if (slowQuerySchedulerEnable) {
            ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(debugLogFilePaths.length);
            for (int i = 0; i < debugLogFilePaths.length; i++) {
                String logPath = debugLogFilePaths[i];

                executor.submit(() -> {
                    try {
                        slowQueryData(logPath);
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
     * @param logPath
     * @throws IOException
     * @throws MalformedObjectNameException
     * @throws InstanceNotFoundException
     * @throws IntrospectionException
     * @throws ReflectionException
     * @throws MBeanException
     * @throws AttributeNotFoundException
     * @throws InterruptedException
     */
    private void slowQueryData(String logPath) throws IOException, MalformedObjectNameException, InstanceNotFoundException, IntrospectionException, ReflectionException, MBeanException, AttributeNotFoundException, InterruptedException {

        List<String> result = new ArrayList<>();


        getSlowQueryMetric(result, logPath);


        if (!CollectionUtils.isEmpty(result)) {


            ObjectMapper mapper = new ObjectMapper();
            try {

                // Writing to a file
                String fileName = slowQueryFilePath + "slowquery" + ".json";
                File tempFile = new File(fileName);
                boolean exists = tempFile.exists();
                // Writing to a file
                if (exists) {
                    byte[] jsonData = Files.readAllBytes(Paths.get(fileName));
                    ObjectMapper objectMapper = new ObjectMapper();

                    List<String> errorRate = objectMapper.readValue(jsonData, List.class);
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

    /**
     *
     * @param result
     * @param logPath
     * @throws FileNotFoundException
     * @throws IOException
     */
    public void getSlowQueryMetric(List<String> result, String logPath) throws FileNotFoundException, IOException {
        try {
            File file = getFile(logPath);
            long lastKnownPosition = getLastKnownPosition(logPath);


            long fileLength = file.length();
            if (fileLength > lastKnownPosition) {

                // Reading and writing file
                RandomAccessFile readWriteFileAccess = new RandomAccessFile(file, "rw");
                readWriteFileAccess.seek(lastKnownPosition);
                String logLine = "";
                while ((logLine = readWriteFileAccess.readLine()) != null) {
                    if (logLine.contains(SLOW_QUERY_FILTER)) {
                        result.add(logLine);
                    }

                }
                lastKnownPosition = readWriteFileAccess.getFilePointer();
                lastKnownPositions.put(logPath, lastKnownPosition);

                readWriteFileAccess.close();
            }


        } catch (Exception e) {
            e.printStackTrace();
            System.out.println(e.getMessage());
        }
    }
}
