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
public class GCPauseScheduler {

    public static Map<String, Long> lastKnownPositions = new HashMap<>();
    Map<String, File> files = new HashMap<>();
    @Value("#{'${gc.log.file.path}'.split(',')}")
    private String[] logFilePath;
    @Value("${gc.pause.scheduler.enable}")
    private Boolean slowQuerySchedulerEnable;
    @Value("${gc.pause.file.path}")
    private String gcFilePath;
    @Value("#{'${hosts}'.split(',')}")
    private String[] hosts;

    public File getFile(String path) {
        if (files.containsKey(path)) {
            return files.get(path);
        } else {
            File temp = new File(path);
            files.put(path, temp);
            return temp;
        }


    }

    public Long getLastKnownPosition(String path) {
        if (lastKnownPositions.containsKey(path)) {
            return lastKnownPositions.get(path);
        } else {
            Long temp = new Long(0);
            lastKnownPositions.put(path, temp);
            return temp;
        }


    }


    @Scheduled(fixedRateString = "${gc.pause.scheduler.time}")
    /**
     *
     */
    public void perform() throws Exception {
        if (false) {
            ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(logFilePath.length);
            for (int i = 0; i < logFilePath.length; i++) {
                String logPath = logFilePath[i];
                String host = hosts[i];

                executor.submit(() -> {
                    try {
                        gcPauseData(logPath, host);
                    } catch (Exception ex) {
                        System.out.println(ex.getMessage());
                        ex.printStackTrace();
                    }
                });
            }

        }

    }

    /**
     *
     * @param logPath
     * @param host
     * @throws IOException
     * @throws MalformedObjectNameException
     * @throws InstanceNotFoundException
     * @throws IntrospectionException
     * @throws ReflectionException
     * @throws MBeanException
     * @throws AttributeNotFoundException
     * @throws InterruptedException
     */
    private void gcPauseData(String logPath, String host) throws IOException, MalformedObjectNameException, InstanceNotFoundException, IntrospectionException, ReflectionException, MBeanException, AttributeNotFoundException, InterruptedException {

        List<String> result = new ArrayList<>();


        getGCPauseMetric(result, logPath);


        if (!CollectionUtils.isEmpty(result)) {


            ObjectMapper mapper = new ObjectMapper();
            try {

                // Writing to a file
                String fileName = gcFilePath + host + ".json";
                File tempFile = new File(fileName);
                boolean exists = tempFile.exists();
                // Writing to a file
                if (exists) {
                    byte[] jsonData = Files.readAllBytes(Paths.get(fileName));
                    ObjectMapper objectMapper = new ObjectMapper();

                    List<String> gcPauses = objectMapper.readValue(jsonData, List.class);
                    gcPauses.addAll(result);
                    result = gcPauses;
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
     * @param result
     * @param logPath
     * @throws FileNotFoundException
     * @throws IOException
     */
    public void getGCPauseMetric(List<String> result, String logPath) throws FileNotFoundException, IOException {
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
                    if (logLine.contains("threads were stopped") || logLine.contains("real=")) {
                        //  String  temp= "2021-02-11T23:13:31.192-0530: 1.075: Total time for which application threads were stopped: 0.0001650 seconds, Stopping threads took: 0.0000274 seconds";
                        result.add(logLine);
                    }

                }
                lastKnownPosition = readWriteFileAccess.getFilePointer();
                lastKnownPositions.put(logPath, lastKnownPosition);

                readWriteFileAccess.close();
            }


        } catch (Exception e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
    }
}
