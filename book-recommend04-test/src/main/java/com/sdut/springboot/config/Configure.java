package com.sdut.springboot.config;

import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.TransportAddress;
import org.elasticsearch.transport.client.PreBuiltTransportClient;
import redis.clients.jedis.Jedis;

import java.net.InetAddress;
import java.net.UnknownHostException;


@Configuration
public class Configure {
    @Value("${es.cluster.name}")
    private String esClusterName;

    @Value("${es.host}")
    private String esHost;

    @Value("${es.port}")
    private int esPort;

    @Bean
    public TransportClient transportClient() throws UnknownHostException {
        // 一定要注意,9300为elasticsearch的tcp端口
        TransportAddress master = new InetSocketTransportAddress(InetAddress.getByName("localhost"), 9300);

        // 集群名称
        Settings settings = Settings.builder().put("cluster.name", esClusterName).build();
        TransportClient esClient = new PreBuiltTransportClient(settings);
        // 添加
        esClient.addTransportAddresses(master);
        return esClient;
    }

    @Bean(name = "jedis")
    public Jedis getRedisClient() {
        Jedis jedis = new Jedis("localhost",6379);

        return jedis;
    }

}
