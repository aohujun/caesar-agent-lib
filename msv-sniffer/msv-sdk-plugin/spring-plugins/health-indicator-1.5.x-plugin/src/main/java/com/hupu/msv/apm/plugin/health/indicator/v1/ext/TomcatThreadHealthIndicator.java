package com.hupu.msv.apm.plugin.health.indicator.v1.ext;


import com.hupu.msv.apm.agent.core.logging.api.ILog;
import com.hupu.msv.apm.agent.core.logging.api.LogManager;
import com.hupu.msv.apm.util.RunnableWithExceptionProtection;
import org.springframework.boot.actuate.health.AbstractHealthIndicator;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;

import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import java.lang.management.ManagementFactory;

/**
 * @author: zhaoxudong
 * @date: 2020-01-16 11:48
 * @description: tomcat线程数检测
 */
public class TomcatThreadHealthIndicator extends AbstractHealthIndicator implements Runnable {
    private static final ILog logger = LogManager.getLogger(TomcatThreadHealthIndicator.class);
    private volatile Status tomcatStatus = Status.UP;
    private volatile String description = "tomcat线程数正常";
    private volatile int maxThreads;
    private volatile int currentThreadsBusy;


    public TomcatThreadHealthIndicator() {
        new Thread(new RunnableWithExceptionProtection(this, (t) -> {
            logger.error("统计tomcat线程数异常.", t);
        }), "tomcat threads statistics").start();
    }

    @Override
    protected void doHealthCheck(Health.Builder builder) throws Exception {
        builder.status(tomcatStatus).withDetail("maxThreads", maxThreads)
                .withDetail("currentThreadsBusy", currentThreadsBusy)
                .withDetail("description", description);
    }

    @Override
    public void run() {
        Status preStatus = tomcatStatus;
        MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
        ObjectName name = null;
        String desc = "tomcat最大线程数：%s,当前使用的线程数：%s";
        try {
            name = mBeanServer.queryNames(new ObjectName("Tomcat:type=ThreadPool,*"), null).iterator().next();
        } catch (MalformedObjectNameException e) {
            logger.error(e.getMessage(), e);
        }
        while (true) {
            try {
                //todo 决定是否多次观察。在一定时间范围内
                maxThreads = (int) mBeanServer.getAttribute(name, "maxThreads");
                currentThreadsBusy = (int) mBeanServer.getAttribute(name, "currentThreadsBusy");
                if (maxThreads == currentThreadsBusy) {
                    tomcatStatus = Status.DOWN;
                } else {
                    tomcatStatus = Status.UP;
                }
                description = String.format(desc, maxThreads, currentThreadsBusy);
                if (!preStatus.equals(tomcatStatus)) {
                    String log = String.format("%s tomcat线程数状态发生改变。之前状态：%s,当前状态：%s。%s"
                            , System.currentTimeMillis(), preStatus.getCode(), tomcatStatus.getCode(), description);
                    logger.warn(log);
                    preStatus = tomcatStatus;
                }
                Thread.sleep(1000);
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
        }
    }
}
