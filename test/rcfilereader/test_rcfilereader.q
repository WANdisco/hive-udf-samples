add jar /home/do/Work/hive-udf/hive-udf-samples/target/hive-udf-samples-0.0.1-SNAPSHOT.jar;
create temporary function udaf as 'com.wandisco.samples.hive.udf.rcfilereader.RCFileBinaryUDF';

-- Create tables

drop table if exists csv_table;
drop table if exists rcfile_table;

create table csv_table (col1 int, col2 double, col3 int, col4 double) row format delimited fields terminated by ",";
load data local inpath 'data.csv' overwrite into table csv_table;

SET hive.exec.compress.output=true;
SET mapred.max.split.size=256000000;
SET mapred.output.compression.type=BLOCK;
SET mapred.output.compression.codec=org.apache.hadoop.io.compress.SnappyCodec;

create table rcfile_table (col1 int, col2 double, col3 int, col4 double) stored as rcfile ;
-- ROW FORMAT SERDE "org.apache.hadoop.hive.serde2.columnar.ColumnarSerDe"
INSERT OVERWRITE TABLE rcfile_table SELECT * FROM csv_table;

-- Run UDF

SELECT udaf(col1) FROM rcfile_table;

-- Cleanup

drop table csv_table;
drop table rcfile_table;
