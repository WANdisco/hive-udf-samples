HADOOP_HOME=${HADOOP_HOME:-~/Work/hadoop-2.3.0}
HIVE_HOME=${HIVE_HOME:-~/Work/hive-0.13}
echo "HADOOP_HOME = $HADOOP_HOME"
echo "HIVE_HOME = $HIVE_HOME"

( cd ../..; mvn clean package )
rm -rf $HADOOP_HOME/logs/userlogs
$HIVE_HOME/bin/hive -f test_rcfilereader.q
rm derby.log