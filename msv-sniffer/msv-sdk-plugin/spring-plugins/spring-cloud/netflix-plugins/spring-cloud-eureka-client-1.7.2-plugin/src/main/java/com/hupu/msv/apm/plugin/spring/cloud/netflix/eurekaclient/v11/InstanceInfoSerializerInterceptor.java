package com.hupu.msv.apm.plugin.spring.cloud.netflix.eurekaclient.v11;

import com.fasterxml.jackson.core.JsonGenerator;
import com.hupu.msv.apm.agent.core.logging.api.ILog;
import com.hupu.msv.apm.agent.core.logging.api.LogManager;
import com.hupu.msv.apm.agent.core.plugin.interceptor.enhance.EnhancedInstance;
import com.hupu.msv.apm.agent.core.plugin.interceptor.enhance.InstanceMethodsAroundInterceptor;
import com.hupu.msv.apm.agent.core.plugin.interceptor.enhance.MethodInterceptResult;
import com.netflix.appinfo.InstanceInfo;
import com.netflix.discovery.converters.Auto;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Map;


/**
 * @author: liaowenqiang
 * @date: 2020-02-13
 * @description: 获取eurka服务列表
 */
public class InstanceInfoSerializerInterceptor implements InstanceMethodsAroundInterceptor {
    private static final ILog logger = LogManager.getLogger(InstanceInfoSerializerInterceptor.class);



    public static final String NODE_LEASE = "leaseInfo";
    public static final String NODE_METADATA = "metadata";
    public static final String NODE_DATACENTER = "dataCenterInfo";
    public static final String NODE_APP = "application";

    protected static final String ELEM_INSTANCE = "instance";
    protected static final String ELEM_OVERRIDDEN_STATUS = "overriddenstatus";
    protected static final String ELEM_HOST = "hostName";
    protected static final String ELEM_INSTANCE_ID = "instanceId";
    protected static final String ELEM_APP = "app";
    protected static final String ELEM_IP = "ipAddr";
    protected static final String ELEM_SID = "sid";
    protected static final String ELEM_STATUS = "status";
    protected static final String ELEM_PORT = "port";
    protected static final String ELEM_SECURE_PORT = "securePort";
    protected static final String ELEM_COUNTRY_ID = "countryId";
    protected static final String ELEM_IDENTIFYING_ATTR = "identifyingAttribute";
    protected static final String ELEM_HEALTHCHECKURL = "healthCheckUrl";
    protected static final String ELEM_SECHEALTHCHECKURL = "secureHealthCheckUrl";
    protected static final String ELEM_APPGROUPNAME = "appGroupName";
    protected static final String ELEM_HOMEPAGEURL = "homePageUrl";
    protected static final String ELEM_STATUSPAGEURL = "statusPageUrl";
    protected static final String ELEM_VIPADDRESS = "vipAddress";
    protected static final String ELEM_SECVIPADDRESS = "secureVipAddress";
    protected static final String ELEM_ISCOORDINATINGDISCSOERVER = "isCoordinatingDiscoveryServer";
    protected static final String ELEM_LASTUPDATEDTS = "lastUpdatedTimestamp";
    protected static final String ELEM_LASTDIRTYTS = "lastDirtyTimestamp";
    protected static final String ELEM_ACTIONTYPE = "actionType";
    protected static final String ELEM_ASGNAME = "asgName";
    protected static final String ELEM_NAME = "name";
    protected static final String DATACENTER_METADATA = "metadata";

    protected static final String VERSIONS_DELTA_TEMPLATE = "versions_delta";
    protected static final String APPS_HASHCODE_TEMPTE = "apps_hashcode";

    // For backwards compatibility
    public static final String METADATA_COMPATIBILITY_KEY = "@class";
    public static final String METADATA_COMPATIBILITY_VALUE = "java.util.Collections$EmptyMap";
    protected static final Object EMPTY_METADATA = Collections.singletonMap(METADATA_COMPATIBILITY_KEY, METADATA_COMPATIBILITY_VALUE);

    @Override
    public void beforeMethod(EnhancedInstance objInst, Method method, Object[] allArguments, Class<?>[] argumentsTypes, MethodInterceptResult result) throws Throwable {
        InstanceInfo info = (InstanceInfo) allArguments[0];
        JsonGenerator jgen = (JsonGenerator) allArguments[1];


        jgen.writeStartObject();

        if (info.getInstanceId() != null) {
            jgen.writeStringField(ELEM_INSTANCE_ID, info.getInstanceId());
        }
        jgen.writeStringField(ELEM_HOST, info.getHostName());
        jgen.writeStringField(ELEM_APP, info.getAppName());
        jgen.writeStringField(ELEM_IP, info.getIPAddr());

        if (!("unknown".equals(info.getSID()) || "na".equals(info.getSID()))) {
            jgen.writeStringField(ELEM_SID, info.getSID());
        }

        jgen.writeStringField(ELEM_OVERRIDDEN_STATUS, info.getOverriddenStatus().name());

        jgen.writeFieldName(ELEM_PORT);
        jgen.writeStartObject();
        jgen.writeNumberField("$", info.getPort());
        jgen.writeStringField("@enabled", Boolean.toString(info.isPortEnabled(InstanceInfo.PortType.UNSECURE)));
        jgen.writeEndObject();

        jgen.writeFieldName(ELEM_SECURE_PORT);
        jgen.writeStartObject();
        jgen.writeNumberField("$", info.getSecurePort());
        jgen.writeStringField("@enabled", Boolean.toString(info.isPortEnabled(InstanceInfo.PortType.SECURE)));
        jgen.writeEndObject();

        jgen.writeNumberField(ELEM_COUNTRY_ID, info.getCountryId());

        if (info.getDataCenterInfo() != null) {
            jgen.writeObjectField(NODE_DATACENTER, info.getDataCenterInfo());
        }
        if (info.getLeaseInfo() != null) {
            jgen.writeObjectField(NODE_LEASE, info.getLeaseInfo());
        }

        Map<String, String> metadata = info.getMetadata();
        if (metadata != null) {
            if (metadata.isEmpty()) {
                jgen.writeObjectField(NODE_METADATA, EMPTY_METADATA);
            } else {
                jgen.writeObjectField(NODE_METADATA, metadata);
            }
        }
        autoMarshalEligible(info, jgen);
        jgen.writeStringField(ELEM_STATUS, info.getStatus().name());


        jgen.writeEndObject();
        result.defineReturnValue(null);

    }

    protected void autoMarshalEligible(Object o, JsonGenerator jgen) {
        try {
            Class c = o.getClass();
            Field[] fields = c.getDeclaredFields();
            Annotation annotation;
            for (Field f : fields) {
                annotation = f.getAnnotation(Auto.class);
                if (annotation != null) {
                    f.setAccessible(true);
                    if (f.get(o) != null) {
                        jgen.writeStringField(f.getName(), String.valueOf(f.get(o)));
                    }

                }
            }
        } catch (Throwable th) {
            logger.error("Error in marshalling the object", th);
        }
    }

    @Override
    public Object afterMethod(EnhancedInstance objInst, Method method, Object[] allArguments, Class<?>[] argumentsTypes, Object ret) throws Throwable {

        return ret;
    }

    @Override
    public void handleMethodException(EnhancedInstance objInst, Method method, Object[] allArguments, Class<?>[] argumentsTypes, Throwable t) {


    }
}
