package com.alibaba.ververica.connectors.hologres.jdbc.sink;

import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.table.api.EnvironmentSettings;
import org.apache.flink.table.api.bridge.java.StreamTableEnvironment;

import com.alibaba.ververica.connectors.hologres.jdbc.HologresTestBase;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

/**
 * Tests for Sink.
 */
public class HologresSinkTableITTest extends HologresTestBase {
	protected EnvironmentSettings streamSettings;
	protected StreamExecutionEnvironment env;
	protected StreamTableEnvironment tEnv;
	protected HologresTestBase testConfig = null;

	public HologresSinkTableITTest() throws IOException {
		super();
	}

	@Before
	public void prepareODPSTable() throws Exception {
		EnvironmentSettings.Builder streamBuilder = EnvironmentSettings.newInstance().inStreamingMode();
		this.streamSettings = streamBuilder.useBlinkPlanner().build();
		env = StreamExecutionEnvironment.getExecutionEnvironment();
		tEnv = StreamTableEnvironment.create(env, streamSettings);
		testConfig = new HologresTestBase();
	}

	@Test
	public void testSinkTable() throws Exception {
		// data in dim:
		//a |  b  |    c    | d |   e    |     f      |      g      |             h              |    i    |         j         |                 k                  |             l             |                    m                    |     n     |            o
		//---+-----+---------+---+--------+------------+-------------+----------------------------+---------+-------------------+------------------------------------+---------------------------+-----------------------------------------+-----------+--------------------------
		//1 | dim | 20.2007 | f | 652482 | 2020-07-08 | source_test | 2020-07-10 16:28:07.737+08 | 8.58965 | {464,98661,32489} | {8589934592,8589934593,8589934594} | {8.58967,96.4667,9345.16} | {587897.4646746,792343.646446,76.46464} | {t,t,f,t} | {monday,saturday,sunday}

		String sinkTable = "sinkTable";
		tEnv.executeSql(
				"create table " + sinkTable + "(\n" +
						"a int not null,\n" +
						"b STRING not null,\n" +
						"c double,\n" +
						"d boolean,\n" +
						"e bigint,\n" +
						"f date,\n" +
						"g varchar,\n" +
						"h TIMESTAMP,\n" +
						"i float,\n" +
						"j array<int> not null,\n" +
						"k array<bigint> not null,\n" +
						"l array<float>,\n" +
						"m array<double>,\n" +
						"n array<boolean>,\n" +
						"o array<STRING>,\n" +
						"p boolean,\n" +
						"q numeric(6,2)\n" +
						") with (" +
						"'connector'='hologres-jdbc',\n" +
						"'endpoint'='" + testConfig.endpoint + "',\n" +
						"'dbname'='" + testConfig.database + "',\n" +
						"'tablename'='" + testConfig.sinkTable + "',\n" +
						"'username'='" + testConfig.username + "',\n" +
						"'password'='" + testConfig.password + "'\n" +
						")");

		tEnv.sqlUpdate("INSERT INTO " +
				sinkTable + " (a,b,c,d,e,f,g,h,i,j,k,l,m,n,o,p,q) values (" +
				"1,'dim',cast(20.2007 as double),false,652482,cast('2020-07-08' as date),'source_test',cast('2020-07-10 16:28:07.737' as timestamp)," +
				"cast(8.58965 as float),cast(ARRAY [464,98661,32489] as array<int>),cast(ARRAY [8589934592,8589934593,8589934594] as array<bigint>)," +
				"ARRAY[cast(8.58967 as float),cast(96.4667 as float),cast(9345.16 as float)], ARRAY [cast(587897.4646746 as double),cast(792343.646446 as double),cast(76.46464 as double)]," +
				"cast(ARRAY [true,true,false,true] as array<boolean>),cast(ARRAY ['monday','saturday','sunday'] as array<STRING>),true,cast(8119.21 as numeric(6,2))" +
				")");

		tEnv.execute("insert");
	}

	@Test
	public void testSinkTableWithSchema() throws Exception {
		// data in dim:
		//a |  b  |    c    | d |   e    |     f      |      g      |             h              |    i    |         j         |                 k                  |             l             |                    m                    |     n     |            o
		//---+-----+---------+---+--------+------------+-------------+----------------------------+---------+-------------------+------------------------------------+---------------------------+-----------------------------------------+-----------+--------------------------
		//1 | dim | 20.2007 | f | 652482 | 2020-07-08 | source_test | 2020-07-10 16:28:07.737+08 | 8.58965 | {464,98661,32489} | {8589934592,8589934593,8589934594} | {8.58967,96.4667,9345.16} | {587897.4646746,792343.646446,76.46464} | {t,t,f,t} | {monday,saturday,sunday}

		String sinkTable = "sinkTable";
		tEnv.executeSql(
				"create table " + sinkTable + "(\n" +
						"a int not null,\n" +
						"b STRING not null,\n" +
						"c double,\n" +
						"d boolean,\n" +
						"e bigint,\n" +
						"f date,\n" +
						"g varchar,\n" +
						"h TIMESTAMP,\n" +
						"i float,\n" +
						"j array<int> not null,\n" +
						"k array<bigint> not null,\n" +
						"l array<float>,\n" +
						"m array<double>,\n" +
						"n array<boolean>,\n" +
						"o array<STRING>,\n" +
						"p boolean,\n" +
						"q numeric(6,2)\n" +
						") with (" +
						"'connector'='hologres-jdbc',\n" +
						"'endpoint'='" + testConfig.endpoint + "',\n" +
						"'dbname'='" + testConfig.database + "',\n" +
						"'tablename'='test." + testConfig.sinkTable + "',\n" +
						"'username'='" + testConfig.username + "',\n" +
						"'password'='" + testConfig.password + "'\n" +
						")");

		tEnv.sqlUpdate("INSERT INTO " +
				sinkTable + " (a,b,c,d,e,f,g,h,i,j,k,l,m,n,o,p,q) values (" +
				"1,'dim',cast(20.2007 as double),false,652482,cast('2020-07-08' as date),'source_test',cast('2020-07-10 16:28:07.737' as timestamp)," +
				"cast(8.58965 as float),cast(ARRAY [464,98661,32489] as array<int>),cast(ARRAY [8589934592,8589934593,8589934594] as array<bigint>)," +
				"ARRAY[cast(8.58967 as float),cast(96.4667 as float),cast(9345.16 as float)], ARRAY [cast(587897.4646746 as double),cast(792343.646446 as double),cast(76.46464 as double)]," +
				"cast(ARRAY [true,true,false,true] as array<boolean>),cast(ARRAY ['monday','saturday','sunday'] as array<STRING>),true,cast(8119.21 as numeric(6,2))" +
				")");

		tEnv.execute("insert");
	}

}
