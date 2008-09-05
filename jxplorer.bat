rem # Use this version if limited by 256 character max batch file length (earlier windows versions)
rem # java -Djava.ext.dirs=jars;dsml/jars com.ca.directory.jxplorer.JXplorer

rem # This version is slightly preferable, as it doesn't override the standard java extensions directory
rem # (note that 'classes' is only used if you are compiling the code yourself; otherwise jars/jxplorer.jar is used.)

java -classpath .;jars/jxplorer.jar;jars/help.jar;jars/jhall.jar;jars/junit.jar;jars/ldapsec.jar;jars/log4j.jar;jars/dsml/activation.jar;jars/dsml/commons-logging.jar;jars/dsml/dom4j.jar;jars/dsml/dsmlv2.jar;jars/dsml/mail.jar;jars/dsml/saaf-api.jar;jars/dsml/saaj-ri.jar com.ca.directory.jxplorer.JXplorer
