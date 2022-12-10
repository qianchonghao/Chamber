package com.alibaba;

import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.search.builder.SearchSourceBuilder;

/**
 * @author qch
 * @since 2022/11/11 12:50 下午
 * ES操作分为两个部分
 *  1. searchRequest构建
 *  2. HighLevelClient执行search操作
 */
public class ElasticRequestBuilder {

    public class ElasticRequest{
        private SearchRequest request;
        private SearchSourceBuilder sourceBuilder;

    }
}
