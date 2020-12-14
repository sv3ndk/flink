package org.apache.flink.formats.pb;

import org.apache.flink.formats.pb.serialize.PbRowSerializationSchema;
import org.apache.flink.formats.pb.testproto.SimpleTest;
import org.apache.flink.table.data.GenericRowData;
import org.apache.flink.table.data.RowData;
import org.apache.flink.table.data.StringData;
import org.apache.flink.table.types.logical.RowType;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class RowToSimpleProtoBytesTest {
	@Test
	public void testSimple() throws Exception {
		RowData row = GenericRowData.of(
			1,
			2L,
			false,
			0.1f,
			0.01,
			StringData.fromString("hello"),
			new byte[]{1},
			StringData.fromString("IMAGES"));

		RowType rowType = PbRowTypeInformation.generateRowType(SimpleTest.getDescriptor());
		row = ProtobufTestHelper.validateRow(row, rowType);

		PbRowSerializationSchema serializationSchema = new PbRowSerializationSchema(
			rowType,
			SimpleTest.class.getName());

		byte[] bytes = serializationSchema.serialize(row);
		SimpleTest simpleTest = SimpleTest.parseFrom(bytes);
		assertTrue(simpleTest.hasA());
		assertEquals(1, simpleTest.getA());
		assertEquals(2L, simpleTest.getB());
		assertFalse(simpleTest.getC());
		assertEquals(Float.valueOf(0.1f), Float.valueOf(simpleTest.getD()));
		assertEquals(Double.valueOf(0.01d), Double.valueOf(simpleTest.getE()));
		assertEquals("hello", simpleTest.getF());
		assertEquals(1, simpleTest.getG().byteAt(0));
		assertEquals(SimpleTest.Corpus.IMAGES, simpleTest.getH());
	}

	@Test
	public void testNull() throws Exception {
		RowData row = GenericRowData.of(
			null,
			2L,
			false,
			0.1f,
			0.01,
			StringData.fromString("hello"),
			null,
			null);

		RowType rowtype = PbRowTypeInformation.generateRowType(SimpleTest.getDescriptor());
		row = ProtobufTestHelper.validateRow(row, rowtype);

		PbRowSerializationSchema serializationSchema = new PbRowSerializationSchema(
			rowtype,
			SimpleTest.class.getName());

		byte[] bytes = serializationSchema.serialize(row);
		SimpleTest simpleTest = SimpleTest.parseFrom(bytes);
		assertFalse(simpleTest.hasA());
		assertFalse(simpleTest.hasG());
		assertFalse(simpleTest.hasH());
	}
}
