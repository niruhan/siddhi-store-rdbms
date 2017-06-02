/*
 * Copyright (c) 2016, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.wso2.extension.siddhi.store.rdbms;

import org.apache.log4j.Logger;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.wso2.siddhi.core.ExecutionPlanRuntime;
import org.wso2.siddhi.core.SiddhiManager;
import org.wso2.siddhi.core.stream.input.InputHandler;

import java.sql.SQLException;
import java.util.List;

import static org.wso2.extension.siddhi.store.rdbms.RDBMSTableTestUtils.TABLE_NAME;
import static org.wso2.extension.siddhi.store.rdbms.RDBMSTableTestUtils.url;

public class InsertIntoRDBMSTableTestCase {
    private static final Logger log = Logger.getLogger(InsertIntoRDBMSTableTestCase.class);

    @BeforeClass
    public static void startTest() {
        log.info("== RDBMS Table INSERT tests started ==");
    }

    @AfterClass
    public static void shutdown() {
        log.info("== RDBMS Table INSERT tests completed ==");
    }

    @BeforeMethod
    public void init() {
        try {
            RDBMSTableTestUtils.clearDatabaseTable(TABLE_NAME);
        } catch (SQLException e) {
            log.info("Test case ignored due to " + e.getMessage());
        }
    }

    @Test
    public void insertIntoRDBMSTableTest1() throws InterruptedException, SQLException {
        //Configure siddhi to insert events data to RDBMS table only from specific fields of the stream.
        log.info("insertIntoRDBMSTableTest1");
        SiddhiManager siddhiManager = new SiddhiManager();
        String streams = "" +
                "define stream StockStream (symbol string, price float, volume long); " +
                "@Store(type=\"rdbms\", jdbc.url=\"" + url + "\", " +
                "username=\"root\", password=\"root\",field.length=\"symbol:100\"," +
                "pool.properties=\"driverClassName:org.h2.Driver\")\n" +
                "@PrimaryKey(\"symbol\")" +
                //"@Index(\"volume\")" +
                "define table StockTable (symbol string, volume long); ";

        String query1 = "" +
                "@info(name = 'query1') " +
                "from StockStream\n" +
                "select symbol, volume\n" +
                "insert into StockTable ;";
        ExecutionPlanRuntime executionPlanRuntime = siddhiManager.createExecutionPlanRuntime(streams + query1);
        InputHandler stockStream = executionPlanRuntime.getInputHandler("StockStream");
        executionPlanRuntime.start();

        stockStream.send(new Object[]{"WSO2", 55.6f, 100L});
        stockStream.send(new Object[]{"IBM", 75.6f, 100L});
        Thread.sleep(1000);

        List<List<Object>> recordsInTable = RDBMSTableTestUtils.getRecordsInTable(TABLE_NAME);
        Assert.assertEquals(recordsInTable.get(0).toArray(), new Object[]{"WSO2", 100L}, "Insertion failed");
        Assert.assertEquals(recordsInTable.get(1).toArray(), new Object[]{"IBM", 100L}, "Insertion failed");
        executionPlanRuntime.shutdown();
    }

    @Test
    public void insertIntoRDBMSTableTest2() throws InterruptedException, SQLException {
        //Testing table creation with a compound primary key (normal insertion)
        log.info("rdbmstabledefinitiontest2");
        SiddhiManager siddhiManager = new SiddhiManager();
        String streams = "" +
                "define stream StockStream (symbol string, price float, volume long); " +
                "@Store(type=\"rdbms\", jdbc.url=\"" + url + "\", " +
                "username=\"root\", password=\"root\",field.length=\"symbol:100\")\n" +
                "@PrimaryKey(\"symbol, price\")" +
                //"@Index(\"volume\")" +
                "define table StockTable (symbol string, price float, volume long); ";

        String query = "" +
                "@info(name = 'query1') " +
                "from StockStream   " +
                "insert into StockTable ;";

        ExecutionPlanRuntime executionPlanRuntime = siddhiManager.createExecutionPlanRuntime(streams + query);
        InputHandler stockStream = executionPlanRuntime.getInputHandler("StockStream");
        executionPlanRuntime.start();

        stockStream.send(new Object[]{"WSO2", 55.6F, 100L});
        stockStream.send(new Object[]{"IBM", 75.6F, 100L});
        stockStream.send(new Object[]{"MSFT", 57.6F, 100L});
        stockStream.send(new Object[]{"WSO2", 58.6F, 100L});
        Thread.sleep(1000);

        long totalRowsInTable = RDBMSTableTestUtils.getRowsInTable(TABLE_NAME);
        Assert.assertEquals(totalRowsInTable, 4, "Definition/Insertion failed");
        executionPlanRuntime.shutdown();
    }
}
