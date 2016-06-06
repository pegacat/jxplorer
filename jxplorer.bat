rem # This version uses wild cards and is only suitable for Java 1.6 or better
rem # (use jxplorer_old_jvm.bat for previous versions)

java -classpath ".;jars/*;jasper/lib/*" -Dfile.encoding=utf-8 %JXOPTS% com.ca.directory.jxplorer.JXplorer %*

