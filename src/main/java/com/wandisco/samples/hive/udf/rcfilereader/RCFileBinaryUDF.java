package com.wandisco.samples.hive.udf.rcfilereader;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Properties;

import org.apache.commons.lang.StringUtils;
import org.apache.hadoop.hive.ql.exec.MapredContext;
import org.apache.hadoop.hive.ql.exec.UDFArgumentException;
import org.apache.hadoop.hive.ql.io.CombineHiveInputFormat;
import org.apache.hadoop.hive.ql.io.CombineHiveInputFormat.CombineHiveInputSplit;
import org.apache.hadoop.hive.ql.metadata.HiveException;
import org.apache.hadoop.hive.ql.udf.generic.GenericUDF;
import org.apache.hadoop.hive.serde.serdeConstants;
import org.apache.hadoop.hive.serde2.SerDeException;
import org.apache.hadoop.hive.serde2.columnar.BytesRefArrayWritable;
import org.apache.hadoop.hive.serde2.columnar.ColumnarSerDeBase;
import org.apache.hadoop.hive.serde2.columnar.ColumnarStructBase;
import org.apache.hadoop.hive.serde2.columnar.LazyBinaryColumnarSerDe;
import org.apache.hadoop.hive.serde2.objectinspector.ObjectInspector;
import org.apache.hadoop.hive.serde2.objectinspector.primitive.PrimitiveObjectInspectorFactory;
import org.apache.hadoop.hive.shims.CombineHiveKey;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapred.InputSplit;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.RecordReader;

public class RCFileBinaryUDF extends GenericUDF {

	private MapredContext context = null;

	private Text text = new Text();

	private int count = 0;

	@Override
	public void configure(MapredContext context) {
		System.out.println("Inside configure()");
		this.context = context;

	}

	@Override
	public Object evaluate(DeferredObject[] args) throws HiveException {
		if (count == 0) {
			if (context != null) {

				System.out.println("Is it map?:" + context.isMap());

				JobConf conf = new JobConf(context.getJobConf());

				Iterator<Entry<String, String>> iter = conf.iterator();
				while (iter.hasNext()) {
					Entry<String, String> val = iter.next();
					System.out.println("JobConf: " + val.getKey() + " = "
							+ val.getValue());
				}

				String[] types = StringUtils.split(conf.get("columns.types"),
						',');
				String[] colNames = new String[types.length - 2];
				String[] colTypes = new String[types.length - 2];
				String[] colIds = new String[types.length - 2];
				for (int i = 0; i < types.length - 2; i++) {
					colTypes[i] = types[i];
					colIds[i] = Integer.toString(i);
					colNames[i] = "col" + i;
				}

				conf.setStrings("hive.io.file.readcolumn.ids",
						StringUtils.join(colIds, ','));

				if (context.getReporter() != null) {
					InputSplit inputSplit = context.getReporter()
							.getInputSplit();

					if (inputSplit != null) {
						System.out.println("IS: " + inputSplit);
						System.out.println("Conf: " + conf);

						try {
							CombineHiveInputSplit chis = (CombineHiveInputSplit) inputSplit;
							CombineHiveInputFormat chif = new CombineHiveInputFormat();
							chif.configure(conf);
							RecordReader reader = chif.getRecordReader(chis,
									conf, context.getReporter());

							ColumnarSerDeBase serde = new LazyBinaryColumnarSerDe();
							Properties props = new Properties();
							props.setProperty(serdeConstants.LIST_COLUMNS,
									StringUtils.join(colNames, ','));
							props.setProperty(serdeConstants.LIST_COLUMN_TYPES,
									StringUtils.join(colTypes, ','));
							serde.initialize(conf, props);

							System.out.println("Reading: " + chis);
							int cnt = 1;
							CombineHiveKey key = (CombineHiveKey) reader
									.createKey();
							BytesRefArrayWritable value = (BytesRefArrayWritable) reader
									.createValue();
							while (reader.next(key, value)) {
								System.out.println(cnt + ":"
										+ printRecord(value, serde));
								cnt++;
							}
							System.out.println("Rows: " + cnt);
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						} catch (SerDeException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				}

			}
		}

		String[] cols = new String[args.length];
		for (int i = 0; i < args.length; i++) {
			cols[i] = args[i].get().toString();
		}
		text.set("Evaluating row #" + count + ": "
				+ StringUtils.join(cols, ','));

		count++;
		return text;
	}

	private String printRecord(BytesRefArrayWritable value,
			ColumnarSerDeBase serde) throws IOException, SerDeException {
		ColumnarStructBase o = (ColumnarStructBase) serde.deserialize(value);
		return StringUtils.join(o.getFieldsAsList(), ',');
	}

	@Override
	public String getDisplayString(String[] args) {
		return "rc_udf(" + StringUtils.join(args, ',') + ')';
	}

	@Override
	public ObjectInspector initialize(ObjectInspector[] args)
			throws UDFArgumentException {
		return PrimitiveObjectInspectorFactory.writableStringObjectInspector;
	}

}