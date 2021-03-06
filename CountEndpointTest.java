package com.tistory.tkyoo.hbase.coprocessor.test;

import com.tistory.tkyoo.hbase.coprocessor.CountEndPoint;
import com.tistory.tkyoo.hbase.coprocessor.autogenerated.Count;
import com.tistory.tkyoo.hbase.coprocessor.autogenerated.Count.CountRequest;
import com.tistory.tkyoo.hbase.coprocessor.autogenerated.Count.CountResponse;
import com.tistory.tkyoo.hbase.coprocessor.autogenerated.Count.CountService;

import java.util.Map;
import java.io.IOException;

import org.apache.hadoop.conf.Configuration;

import org.apache.hadoop.hbase.client.coprocessor.Batch;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.client.ConnectionFactory;
import org.apache.hadoop.hbase.client.Table;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.TableName;

import org.apache.hadoop.hbase.ipc.BlockingRpcCallback;
import org.apache.hadoop.hbase.ipc.CoprocessorRpcChannel;

import com.google.protobuf.ServiceException;

public class CountEndpointTest {
    public static void main( String[] args ) {
        System.out.println( "HBase Endpoint Test: Count from RegionServer" );
        
        if (args.length < 3) {
            System.err.println("Usage: CountEndpointTest <Table Name>");
            System.exit(1);
        }

        try {
            Configuration conf = HBaseConfiguration.create();

            Connection connection = ConnectionFactory.createConnection(conf);
            TableName tableName = TableName.valueOf(args[0]);
            Table table = connection.getTable(tableName);

            final CountRequest request = CountRequest.newBuilder().build();

            Map<byte[], Long> results = table.coprocessorService(CountService.class, null, null, new Batch.Call<CountService, Long>() {
                @Override
                public Long call(CountService aggregate) throws IOException {
                    BlockingRpcCallback rpcCallback = new BlockingRpcCallback();
                    aggregate.getCount(null, request, rpcCallback);
                    CountResponse response = (CountResponse)rpcCallback.get();
                    return response.hasCnt() ? response.getCnt() : 0L;
                }
            });

            for (Long cnt : results.values()) {
                System.out.println("Count = " + cnt);
            }
        } catch (ServiceException e) {
            e.printStackTrace();
        } catch (Throwable e) {
            e.printStackTrace();
        }

        System.exit(0);
    }
}
