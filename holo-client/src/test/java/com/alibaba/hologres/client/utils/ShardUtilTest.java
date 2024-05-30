/*
 * Copyright (c) 2021. Alibaba Group Holding Limited
 */

package com.alibaba.hologres.client.utils;

import com.alibaba.hologres.client.HoloClient;
import com.alibaba.hologres.client.HoloClientTestBase;
import com.alibaba.hologres.client.HoloConfig;
import com.alibaba.hologres.client.Put;
import com.alibaba.hologres.client.exception.HoloClientException;
import com.alibaba.hologres.client.impl.collector.shard.DistributionKeyShardPolicy;
import com.alibaba.hologres.client.impl.util.ShardUtil;
import com.alibaba.hologres.client.model.Record;
import com.alibaba.hologres.client.model.TableSchema;
import com.alibaba.hologres.client.model.WriteMode;
import org.postgresql.core.BaseConnection;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static com.alibaba.hologres.client.utils.DataTypeTestUtil.ALL_TYPE_DATA;

/**
 * ShardUtil单元测试用例.
 */
public class ShardUtilTest extends HoloClientTestBase {

	@Test
	public void testSplit001() {
		int[][] shardCount = ShardUtil.split(2);
		Assert.assertEquals(2, shardCount.length);
		Assert.assertEquals(0, shardCount[0][0]);
		Assert.assertEquals(32768, shardCount[0][1]);
		Assert.assertEquals(32768, shardCount[1][0]);
		Assert.assertEquals(65536, shardCount[1][1]);
	}

	@Test
	public void testBytes() {
		byte[] a = new byte[]{1, 2};
		byte[] b = new byte[]{1, 2};
		int shardCount = ShardUtil.hash(b);
		System.out.println(shardCount);
		Assert.assertEquals(ShardUtil.hash(a), ShardUtil.hash(b));
	}

	@Test
	public void testIntArray() {
		int[] a = new int[]{1, 2};
		int[] b = new int[]{1, 2};
		int shardCount = ShardUtil.hash(b);
		System.out.println(shardCount);
		Assert.assertEquals(ShardUtil.hash(a), ShardUtil.hash(b));
	}

	@DataProvider(name = "typeCaseData")
	public Object[][] createData() {
		DataTypeTestUtil.TypeCaseData[] typeToTest;
		typeToTest = ALL_TYPE_DATA;
		Object[][] ret = new Object[typeToTest.length][];
		for (int i = 0; i < typeToTest.length; ++i) {
			ret[i] = new Object[]{typeToTest[i]};
		}
		return ret;
	}

	/**
	 * allType.
	 * Method: put(Put put).
	 */
	@Test(dataProvider = "typeCaseData")
	public void testALLTypeInsert(DataTypeTestUtil.TypeCaseData typeCaseData) throws Exception {
		if (properties == null) {
			return;
		}

		HoloConfig config = buildConfig();
		config.setUseFixedFe(false);

		final int totalCount = 10;
		String typeName = typeCaseData.getName();
		String type = typeCaseData.getColumnType();
		if (typeName.equals("jsonb")) {
			// skip jsonb
			return;
		}
		config.setAppName("testALLTypeDistributionKey");
		config.setWriteMode(WriteMode.INSERT_OR_REPLACE);

		// config.setUseLegacyPutHandler(true);
		try (Connection conn = buildConnection(); HoloClient client = new HoloClient(config)) {
			String tableName = "\"" + "holo_client_distribution_key_type_" + typeName + "\"";
			String dropSql = "drop table if exists " + tableName;
			String createSql = String.format("create table %s (id text, col1 %s) with (distribution_key=\"id,col1\")", tableName, type);
			execute(conn, new String[]{dropSql});
			try {
				execute(conn, new String[]{createSql});
			} catch (SQLException e) {
				/*
				 * decimal,timestamp,time: is not supported yet when shard function is HashShardFunction
				 * float4,double,interval,timetz,uuid: cannot be used as the distribution key
				 * array(int[]): a column of array type as distribution_key is not supported
				 * inet,bit,varbit: 能够建表，但写入时报错(PlStmt Translation: Distribution key is type of imprecise not supported)
				 */
				if (e.getMessage().contains("type as distribution_key is not supported")
						|| e.getMessage().contains("is not supported yet when shard function is HashShardFunction")
						|| e.getMessage().contains("cannot be used as the distribution key")) {
					LOG.info("{} type as distribution_key is not supported.", typeName);
					return;
				} else {
					throw new RuntimeException(e);
				}
			}
			try {
				TableSchema schema = client.getTableSchema(tableName, true);
				Map<String, Integer> expected = new HashMap<>(totalCount);
				int shardCount = getShardCount(client, schema);
				DistributionKeyShardPolicy shardPolicy = new DistributionKeyShardPolicy();
				shardPolicy.init(shardCount);
				try {
					for (int i = 0; i < totalCount; ++i) {
						Record record = Record.build(schema);
						String id = UUID.randomUUID().toString();
						record.setObject(0, id);
						record.setObject(1, typeCaseData.getSupplier().apply(i, conn.unwrap(BaseConnection.class)));
						expected.put(id, shardPolicy.locate(record));
						client.put(new Put(record));
					}
					client.flush();
				} catch (HoloClientException e) {
					if (e.getMessage().contains("PlStmt Translation: Distribution key is type of imprecise not supported")) {
						return;
					} else {
						throw e;
					}
				}

				int count = 0;
				try (Statement stat = conn.createStatement()) {
					LOG.info("current type:{}", typeName);
					String sql = "select hg_shard_id, id from " + tableName;

					try (ResultSet rs = stat.executeQuery(sql)) {
						while (rs.next()) {
							String id = rs.getString(2);
							Assert.assertEquals(rs.getInt(1), expected.get(id));
							++count;
						}
					}
					Assert.assertEquals(count, totalCount);
				}
			} finally {
				execute(conn, new String[]{dropSql});
			}
		}
	}
}
