package com.abs.cmn.oia.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import com.solacesystems.jcsmp.EndpointProperties;
import com.solacesystems.jcsmp.JCSMPChannelProperties;
import com.solacesystems.jcsmp.JCSMPProperties;

//import async.controller.config.SessionConfiguration;
import lombok.Getter;

@Getter
@Component
public class SessionConfiguration {
    private static final Logger log = LoggerFactory.getLogger(SessionConfiguration.class);
    private static SessionConfiguration instance;
    Environment env;

    @Value("${ap.info.group}")
    private String groupName;
    @Value("${ap.info.site}")
    private String siteName;
    @Value("${ap.info.env}")
    private String envType;
    @Value("${ap.info.sequence}")
    private String processSeq;
    @Value("${ap.interface.solace.host}")
    private String host;
    @Value("${ap.interface.solace.vpn}")
    private String vpn;
    @Value("${ap.interface.solace.client.username}")
    private String userId;
    @Value("${ap.interface.solace.client.password}")
    private String userPassword;
    @Value("${ap.interface.solace.connect.trial-count}")
    private int connectTrialCount;
    @Value("${ap.interface.solace.reconnect.trial-count}")
    private int reconnectTrialCount;
    @Value("${ap.interface.solace.reconnect.per-host}")
    private int reconnectPerHost;

    private String clientName;

    public static SessionConfiguration createSessionConfiguration(Environment env) {
        if (instance == null) {
            instance = new SessionConfiguration(env);
        }
        return instance;
    }

    public static SessionConfiguration getSessionConfiguration() {
        log.info("@@ - check instance : "+instance.toString());
        return instance;
    }

    public SessionConfiguration(Environment env) {
        this.env = env;
        instance = this;
    }

    public JCSMPProperties getProperty(String postfixClientName){

        String clientName = this.generateClientName();

        this.clientName = clientName;

        JCSMPProperties properties = new JCSMPProperties();

        properties.setProperty(JCSMPProperties.HOST, host);
        //solace msgVpn명
        properties.setProperty(JCSMPProperties.VPN_NAME, vpn);
        //solace msgVpn에 접속할 클라이언트사용자명
        properties.setProperty(JCSMPProperties.USERNAME, userId);
        //solace msgVpn에 접속할 클라이언트사용자 패스워드(생략 가능)
        if(userPassword != null && !userPassword.isEmpty())
            properties.setProperty(JCSMPProperties.PASSWORD, userPassword);
        //Allication client name 설정 - 동일 vpn 내에서 uniq 해야 함
        properties.setProperty(JCSMPProperties.CLIENT_NAME, clientName + "-" + postfixClientName);
        //endpoint에 등록되어 있는 subscription으로 인해 발생하는 에러 무시
        properties.setProperty(JCSMPProperties.IGNORE_DUPLICATE_SUBSCRIPTION_ERROR, true);

        JCSMPChannelProperties chProp = new JCSMPChannelProperties();
        chProp.setReconnectRetries(reconnectTrialCount); // 세션 다운 시 재 연결 트라이 횟수
        chProp.setConnectRetriesPerHost(reconnectPerHost); // 세션 리트라이 간격
        chProp.setConnectRetries(connectTrialCount);

        properties.setProperty(JCSMPChannelProperties.RECONNECT_RETRIES, chProp);

        return properties;
    }

    public EndpointProperties getEndpoint(){
        /*
         * EndPoint 설정
         * - SolAdmin에서 설정이 되어 있는 경우 Applicaiton에서는 사용하지 않아도 됨(사용할 경우 SolAdmin 화면과 동일하게 구성)
         * - SolAdmin에 설정이 없는 경우 Application에서 설정한 값으로 설정됨
         */
        EndpointProperties endpointProps = new EndpointProperties();
        /* Endpoint(queue, topic) 설정 - solAdmin 화면에서 설정한 값과 동일 */
        //Endpoint(Queue) 권한 설정
        endpointProps.setPermission(EndpointProperties.PERMISSION_DELETE);
        //Endpoint(Queue) accesstype 설정
        endpointProps.setAccessType(EndpointProperties.ACCESSTYPE_NONEXCLUSIVE);
        //Endpoint(Queue) 용량 설정
        endpointProps.setQuota(100);
        //Endpoint provisioning - solAdmin 에 생성된 Endpoint 가 있으므로 "FLAG_IGNORE_ALREADY_EXISTS" 사용)
        return endpointProps;
    }

    private String generateClientName(){
        return String.format("%s-%s-%s-", this.groupName, this.siteName, this.envType) + String.format("%04d", Integer.valueOf(this.processSeq) );
    }

}
