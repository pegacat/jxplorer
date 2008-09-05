#!/bin/sh
# OpenDirectory  jxstart.sh $Revision: 1.13 $  $Date: 2006/04/18 04:12:12 $

if [ -x $JAVA_HOME/bin/java ]; then
   JAVA_LOC=$JAVA_HOME/bin/java
elif [ -x /opt/jre/bin/java ]; then
    JAVA_LOC=/opt/jre/bin/java
elif [ -x /opt/ca/jre/bin/java ]; then
    JAVA_LOC=/opt/ca/jre/bin/java
elif [ -x /opt/ca/etrustdirectory/jre/bin/java ]; then
    JAVA_LOC=/opt/ca/etrustdirectory/jre/bin/java
elif [ -x /opt/CA/eTrustDirectory/jre/bin/java ]; then
    JAVA_LOC=/opt/CA/eTrustDirectory/jre/bin/java
else
    JAVA_LOC=java
fi

# Find directory of JRE
${JAVA_LOC} -version  >/dev/null 2>&1
if [ "$?" != "0" ] ; then
	OPTJX=/opt/jxplorer

	# $OPTJX MUST be the JXplorer install directory, or a link to it, and contain the JRE

	if [ ! -d $OPTJX -o ! -h $OPTJX ] ; then
		echo "Either java must be in the path, or"
		echo "$OPTJX MUST be the JXplorer install directory, or a link to it, and contain the JRE"
		exit 1
	fi

	cd $OPTJX
        JAVAV=/opt/jxplorer/jre/bin/java
else
        JAVAV=${JAVA_LOC}
fi

echo "starting TCPMon..."

    $JAVAV -cp .:jars/jxplorer.jar org.apache.axis.utils.TCPInterceptor 

