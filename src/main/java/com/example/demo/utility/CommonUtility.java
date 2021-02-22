package com.example.demo.utility;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.management.MBeanServerConnection;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Component
public class CommonUtility {
    private  static boolean jmxAuthEnabled = false;

    @Value("${jmx.auth.enable}")
    public void setjmxAuthEnabled(boolean jmxAuthEnabled) {
        jmxAuthEnabled = jmxAuthEnabled;

    }
    private static String userName;

    @Value("${jmx.auth.username}")
    public void setUserName(String userName) {
        this.userName = userName;
    }

    private static String password;

    @Value("${jmx.auth.pass}")
    public void setJmxPassword(String password) {
        this.password = password;
    }
    public static MBeanServerConnection getmBeanServerConnection(String host, String port) throws IOException {
        String url = "service:jmx:rmi:///jndi/rmi://" + host + ":" + port + "/jmxrmi";
        JMXServiceURL serviceUrl = new JMXServiceURL(url);
        Map<String, String[]> env = new HashMap<>();
        String[] credentials = {userName, password};
        env.put(JMXConnector.CREDENTIALS, credentials);
        JMXConnector jmxConnector = JMXConnectorFactory.connect(serviceUrl, jmxAuthEnabled?env:null);
        return jmxConnector.getMBeanServerConnection();
    }

}
