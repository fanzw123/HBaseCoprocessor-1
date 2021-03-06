package com.tistory.tkyoo.hbase.coprocessor;

import com.tistory.tkyoo.hbase.coprocessor.autogenerated.Count;
import com.tistory.tkyoo.hbase.coprocessor.autogenerated.Count.CountRequest;
import com.tistory.tkyoo.hbase.coprocessor.autogenerated.Count.CountResponse;
import com.tistory.tkyoo.hbase.coprocessor.autogenerated.Count.CountService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.hadoop.hbase.Coprocessor;
import org.apache.hadoop.hbase.CoprocessorEnvironment;

import org.apache.hadoop.hbase.Cell;
import org.apache.hadoop.hbase.client.Scan;
import org.apache.hadoop.hbase.coprocessor.CoprocessorException;
import org.apache.hadoop.hbase.coprocessor.CoprocessorService;
import org.apache.hadoop.hbase.coprocessor.RegionCoprocessorEnvironment;
import org.apache.hadoop.hbase.protobuf.ResponseConverter;
import org.apache.hadoop.hbase.regionserver.InternalScanner;
import org.apache.hadoop.hbase.util.Bytes;

import com.google.protobuf.RpcCallback;
import com.google.protobuf.RpcController;
import com.google.protobuf.Service;

public class CountEndPoint extends CountService implements Coprocessor, CoprocessorService {
    private RegionCoprocessorEnvironment env;

    @Override
    public Service getService() {
        return this;
    }

    @Override
    public void start(CoprocessorEnvironment env) throws IOException {
        if (env instanceof RegionCoprocessorEnvironment) {
            this.env = (RegionCoprocessorEnvironment)env;
        } else {
            throw new CoprocessorException("Must be loaded on a table region!");
        }
    }

    @Override
    public void stop(CoprocessorEnvironment env) throws IOException {
        // do nothing
    }

    @Override
    public void getCount(RpcController controller, CountRequest request, RpcCallback done) {
        Scan scan = new Scan();
        CountResponse response = null;
        InternalScanner scanner = null;

        try {
            scanner = env.getRegion().getScanner(scan);
            ArrayList<Cell> results = new ArrayList<Cell>();
            boolean hasMore = false;
            long cnt = 0L;
            byte[] latestRowKey = null;
            int rowLength = 0;

            do {
                hasMore = scanner.next(results);
                for (Cell c : results) {
                    if (latestRowKey == null) {
                        latestRowKey = Bytes.copy(c.getRowArray(), c.getRowOffset(), c.getRowLength());
                        rowLength = c.getRowLength();
                        cnt++;
                    } else if (!Bytes.equals(latestRowKey, 0, rowLength, c.getRowArray(), c.getRowOffset(), c.getRowLength())) {
                        latestRowKey = Bytes.copy(c.getRowArray(), c.getRowOffset(), c.getRowLength());
                        rowLength = c.getRowLength();
                        cnt++;
                    }
                }
                results.clear();
            } while (hasMore);

            response = CountResponse.newBuilder().setCnt(cnt).build();

        } catch ( IOException ioe) {
            ResponseConverter.setControllerException(controller, ioe);
        } finally {
            if (scanner != null) {
                try {
                    scanner.close();
                } catch (IOException ignored) {
                }
            }
        }

        done.run(response);
    }
}
