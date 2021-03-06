package com.lif314.gulimall.search;

import com.alibaba.fastjson.JSON;
import com.lif314.gulimall.search.config.GulimallElasticSearchConfig;
import lombok.Data;
import lombok.ToString;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.ml.job.results.Bucket;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.rest.RestStatus;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import org.elasticsearch.search.aggregations.metrics.Avg;
import org.elasticsearch.search.aggregations.metrics.AvgAggregationBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@SpringBootTest
class GulimallSearchApplicationTests {


    @Autowired
    private RestHighLevelClient client;

    @Test
    void contextLoads() {
        System.out.println(client);
    }


    @Data
    class User{
        private String userName;
        private String gender;
        private Integer age;
    }

    /**
     * ??????Index API
     */
    @Test
    public void IndexApi() throws IOException {
        // ???????????????json

        IndexRequest indexRequest = new IndexRequest("users");
        indexRequest.id("1");
        User  user = new User();
        user.setUserName("lilinfei");
        user.setGender("M");
        user.setAge(19);
        String jsonString = JSON.toJSONString(user);
        indexRequest.source(jsonString, XContentType.JSON);

        // ????????????
        IndexResponse indexResponse = client.index(indexRequest, GulimallElasticSearchConfig.COMMON_OPTIONS);
        System.out.println(indexResponse);

    }

    @Data
    @ToString
    static class Account {

        private int account_number;
        private int balance;
        private String firstname;
        private String lastname;
        private int age;
        private String gender;
        private String address;
        private String employer;
        private String email;
        private String city;
        private String state;
    }

    /**
     * ??????????????????
     */
    @Test
    public void searchApi() throws IOException {
        // 1?????????????????????
        SearchRequest searchRequest = new SearchRequest();
        // ????????????
        searchRequest.indices("bank");
        // 2?????????????????????
        // ??????DSL SearchSourceBuilder
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        // ??????
        searchSourceBuilder.query(QueryBuilders.matchQuery("address", "mill"));
        // ?????? -- ????????????????????????????????????
        TermsAggregationBuilder ageAgg = AggregationBuilders.terms("ageAgg").field("age").size(10);
        searchSourceBuilder.aggregation(ageAgg);
        // ????????????????????????
        AvgAggregationBuilder balanceAvg = AggregationBuilders.avg("balanceAvg").field("balance");
        searchSourceBuilder.aggregation(balanceAvg);


        System.out.println("???????????????" + searchSourceBuilder.toString());
        //  ?????????????????? DSL
        searchRequest.source(searchSourceBuilder);

        // 3?????????   ??????/??????
        SearchResponse searchResponse = client.search(searchRequest, GulimallElasticSearchConfig.COMMON_OPTIONS);

        // 4???????????????  searchResponse
        System.out.println(searchResponse.toString());
        // ?????????????????????
        SearchHits hits = searchResponse.getHits();
        // ????????????
        SearchHit[] searchHits = hits.getHits();
        for (SearchHit hit : searchHits) {
            String index = hit.getIndex();
            String sourceAsString = hit.getSourceAsString();
            // ????????????
            Account account = JSON.parseObject(sourceAsString, Account.class);
            System.out.println("account:" + account.toString());
        }
        // ??????????????????
        Aggregations aggregations = searchResponse.getAggregations();
        // ??????????????????????????????
        // Terms --  org.elasticsearch.search.aggregations.bucket.MultiBucketsAggregation
        Terms ageAgg1 = aggregations.get("ageAgg");
        for (Terms.Bucket bucket : ageAgg1.getBuckets()) {
            String keyAsString = bucket.getKeyAsString();
            System.out.println("?????????" + keyAsString + "===> " + bucket.getDocCount());
        }
        Avg balanceAvg1 = aggregations.get("balanceAvg");
        double value = balanceAvg1.getValue();
        System.out.println("???????????????" + value);

    }

}
