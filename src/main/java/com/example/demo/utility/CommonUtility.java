package com.example.demo.utility;

import org.springframework.stereotype.Component;

import javax.management.MBeanServerConnection;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import java.io.IOException;

@Component
public class CommonUtility {

    public static MBeanServerConnection getmBeanServerConnection(String host, String port) throws IOException {
        String url = "service:jmx:rmi:///jndi/rmi://" + host + ":" + port + "/jmxrmi";
        JMXServiceURL serviceUrl = new JMXServiceURL(url);
        JMXConnector jmxConnector = JMXConnectorFactory.connect(serviceUrl, null);
        return jmxConnector.getMBeanServerConnection();
    }

}
