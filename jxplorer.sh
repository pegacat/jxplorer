#!/bin/sh
# OpenDirectory  jxstart.sh $Revision: 1.14 $  $Date: 2008/08/31 00:05:44 $

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
    basename=`basename $0`
    dirname=`dirname $0`
    cd ${dirname}
    dirname=`pwd`
    echo "Using new directory finding code"
    OPTJX=${dirname}
	#OPTJX=/opt/jxplorer

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

JXOPTS=""
case $(uname) in
   Darwin*)  
   JXOPTS="-Xdock:name=\"JXplorer\" -Dcom.apple.macos.useScreenMenuBar=true" 
   echo "runing OSX verison";;
esac

echo "starting JXplorer..."

FAIL=0
if [ "$1" = "console" ] ; then
    echo  "$JAVAV $JXOPTS  -cp .:jars/jxplorer.jar:jars/help.jar:jars/jhall.jar:jars/junit.jar com.ca.directory.jxplorer.JXplorer"
    $JAVAV $JXOPTS  -cp .:jars/jxplorer.jar:jars/help.jar:jars/jhall.jar:jars/junit.jar com.ca.directory.jxplorer.JXplorer

    if [ "$?" != "0" ]; then
        FAIL=1
    fi
else
    echo "Use \"jxstart.sh console\" if you want logging to the console"
    $JAVAV $JXOPTS -cp .:jars/jxplorer.jar:jars/help.jar:jars/jhall.jar:jars/junit.jar com.ca.directory.jxplorer.JXplorer  >/dev/null 2>&1

    if [ "$?" != "0" ]; then
        FAIL=1
    fi
fi

# Check for success
if [ $FAIL = 0 ]; then
    exit 0
fi

cat <<-!

=========================
JXplorer failed to start
=========================
Please ensure that you have appropriate "xhost" access to the machine you are
running this from. Make sure the DISPLAY environment variable is set correctly.
Otherwise, ask your Unix Systems Administrator for more information on running
X Windows applications.

If you require more information run "$0 console" and check the
error produced.
!

exit 1
