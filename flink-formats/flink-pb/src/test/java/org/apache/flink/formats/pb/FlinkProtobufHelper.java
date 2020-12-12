package org.apache.flink.formats.pb;

import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.formats.pb.serialize.PbRowSerializationSchema;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.table.api.EnvironmentSettings;
import org.apache.flink.table.api.Table;
import org.apache.flink.table.api.bridge.java.StreamTableEnvironment;
import org.apache.flink.table.data.RowData;
import org.apache.flink.table.data.util.DataFormatConverters;
import org.apache.flink.table.runtime.typeutils.InternalTypeInfo;
import org.apache.flink.table.types.DataType;
import org.apache.flink.table.types.logical.RowType;
import org.apache.flink.table.types.utils.TypeConversions;
import org.apache.flink.types.Row;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.apache.flink.table.types.utils.TypeConversions.fromLogicalToDataType;

public class FlinkProtobufHelper {
	public static RowData validateRow(RowData rowData, RowType rowType) throws Exception {
		StreamExecutionEnvironment env = StreamExecutionEnvironment.createLocalEnvironment();
		StreamTableEnvironment tableEnv = StreamTableEnvironment.create(
			env,
			EnvironmentSettings.newInstance().useBlinkPlanner().inStreamingMode().build());

		DataType rowDataType = fromLogicalToDataType(rowType);
		Row row = (Row) DataFormatConverters.getConverterForDataType(rowDataType)
			.toExternal(rowData);
		TypeInformation<Row> rowTypeInfo = (TypeInformation<Row>) TypeConversions.fromDataTypeToLegacyInfo(
			rowDataType);
		DataStream<Row> rows = env.fromCollection(
			Collections.singletonList(row),
			rowTypeInfo);

		Table table = tableEnv.fromDataStream(rows);
		tableEnv.createTemporaryView("t", table);
		table = tableEnv.sqlQuery("select * from t");
		List<RowData> resultRows = tableEnv.toAppendStream(table, InternalTypeInfo.of(rowType))
			.executeAndCollect(1);
		return resultRows.get(0);
	}

	public static byte[] rowToPbBytes(RowData row, Class messageClass) throws Exception {
		RowType rowType = PbRowTypeInformation.generateRowType(PbFormatUtils.getDescriptor(
			messageClass.getName()));
		row = validateRow(row, rowType);
		PbRowSerializationSchema serializationSchema = new PbRowSerializationSchema(
			rowType,
			messageClass.getName());
		byte[] bytes = serializationSchema.serialize(row);
		return bytes;
	}

	public static byte[] rowToPbBytesWithoutValidation(RowData row, Class messageClass) throws Exception {
		RowType rowType = PbRowTypeInformation.generateRowType(PbFormatUtils.getDescriptor(
			messageClass.getName()));
		PbRowSerializationSchema serializationSchema = new PbRowSerializationSchema(
			rowType,
			messageClass.getName());
		byte[] bytes = serializationSchema.serialize(row);
		return bytes;
	}

	public static <K, V> Map<K, V> mapOf(Object... keyValues) {
		Map<K, V> map = new HashMap<>();

		for (int index = 0; index < keyValues.length / 2; index++) {
			map.put((K) keyValues[index * 2], (V) keyValues[index * 2 + 1]);
		}

		return map;
	}

}
