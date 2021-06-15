/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.dtstack.flinkx.connector.emqx.table;

import org.apache.flink.api.common.serialization.DeserializationSchema;
import org.apache.flink.configuration.ConfigOption;
import org.apache.flink.configuration.ReadableConfig;
import org.apache.flink.table.api.TableSchema;
import org.apache.flink.table.connector.format.DecodingFormat;
import org.apache.flink.table.connector.sink.DynamicTableSink;
import org.apache.flink.table.connector.source.DynamicTableSource;
import org.apache.flink.table.data.RowData;
import org.apache.flink.table.factories.DeserializationFormatFactory;
import org.apache.flink.table.factories.DynamicTableSinkFactory;
import org.apache.flink.table.factories.DynamicTableSourceFactory;
import org.apache.flink.table.factories.FactoryUtil;
import org.apache.flink.table.utils.TableSchemaUtils;

import com.dtstack.flinkx.connector.emqx.conf.EmqxConf;
import com.dtstack.flinkx.connector.emqx.sink.EmqxDynamicTableSink;
import com.dtstack.flinkx.connector.emqx.source.EmqxDynamicTableSource;

import java.util.HashSet;
import java.util.Set;

import static com.dtstack.flinkx.connector.emqx.option.EmqxOptions.BROKER;
import static com.dtstack.flinkx.connector.emqx.option.EmqxOptions.FORMAT;
import static com.dtstack.flinkx.connector.emqx.option.EmqxOptions.ISCLEANSESSION;
import static com.dtstack.flinkx.connector.emqx.option.EmqxOptions.PASSWORD;
import static com.dtstack.flinkx.connector.emqx.option.EmqxOptions.QOS;
import static com.dtstack.flinkx.connector.emqx.option.EmqxOptions.TOPIC;
import static com.dtstack.flinkx.connector.emqx.option.EmqxOptions.USERNAME;

/**
 * @author chuixue
 * @create 2021-06-01 20:04
 * @description
 */
public class EmqxDynamicTableFactory implements DynamicTableSourceFactory, DynamicTableSinkFactory {

    /** 通过该值查找具体插件 */
    private static final String IDENTIFIER = "emqx-x";

    @Override
    public DynamicTableSink createDynamicTableSink(Context context) {
        final FactoryUtil.TableFactoryHelper helper =
                FactoryUtil.createTableFactoryHelper(this, context);
        // 1.所有的requiredOptions和optionalOptions参数
        final ReadableConfig config = helper.getOptions();

        // 2.参数校验
        helper.validate();

        // 3.封装参数
        TableSchema physicalSchema =
                TableSchemaUtils.getPhysicalSchema(context.getCatalogTable().getSchema());

        return new EmqxDynamicTableSink(physicalSchema, getEmqxConf(config));
    }

    @Override
    public DynamicTableSource createDynamicTableSource(Context context) {
        final FactoryUtil.TableFactoryHelper helper =
                FactoryUtil.createTableFactoryHelper(this, context);
        // 1.所有的requiredOptions和optionalOptions参数
        final ReadableConfig config = helper.getOptions();

        // 2.参数校验
        helper.validate();

        // 3.封装参数
        TableSchema physicalSchema =
                TableSchemaUtils.getPhysicalSchema(context.getCatalogTable().getSchema());
        // 加载json format，目前只支持json格式
        final DecodingFormat<DeserializationSchema<RowData>> valueDecodingFormat =
                getValueDecodingFormat(helper);

        return new EmqxDynamicTableSource(physicalSchema, getEmqxConf(config), valueDecodingFormat);
    }

    @Override
    public String factoryIdentifier() {
        return IDENTIFIER;
    }

    @Override
    public Set<ConfigOption<?>> requiredOptions() {
        Set<ConfigOption<?>> requiredOptions = new HashSet<>();
        requiredOptions.add(BROKER);
        requiredOptions.add(TOPIC);
        return requiredOptions;
    }

    @Override
    public Set<ConfigOption<?>> optionalOptions() {
        Set<ConfigOption<?>> optionalOptions = new HashSet<>();
        optionalOptions.add(BROKER);
        optionalOptions.add(TOPIC);

        optionalOptions.add(ISCLEANSESSION);
        optionalOptions.add(QOS);
        optionalOptions.add(USERNAME);
        optionalOptions.add(PASSWORD);
        optionalOptions.add(FORMAT);
        return optionalOptions;
    }

    private EmqxConf getEmqxConf(ReadableConfig readableConfig) {
        EmqxConf emqxConf = new EmqxConf();
        emqxConf.setBroker(readableConfig.get(BROKER));
        emqxConf.setTopic(readableConfig.get(TOPIC));
        emqxConf.setUsername(readableConfig.get(USERNAME));
        emqxConf.setPassword(readableConfig.get(PASSWORD));
        emqxConf.setCleanSession(readableConfig.get(ISCLEANSESSION));
        emqxConf.setQos(readableConfig.get(QOS));
        return emqxConf;
    }

    private static DecodingFormat<DeserializationSchema<RowData>> getValueDecodingFormat(
            FactoryUtil.TableFactoryHelper helper) {
        return helper.discoverOptionalDecodingFormat(DeserializationFormatFactory.class, FORMAT)
                .orElseGet(
                        () ->
                                helper.discoverDecodingFormat(
                                        DeserializationFormatFactory.class, FORMAT));
    }
}
