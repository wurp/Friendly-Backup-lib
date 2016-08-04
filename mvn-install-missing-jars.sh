mvn install:install-file -Dfile=lib/onion-common.jar -DgroupId=com.onionnetworks.fec -DartifactId=onion-common -Dversion=1.0 -Dpackaging=jar
mvn install:install-file -Dfile=lib/onion-fec.jar -DgroupId=com.onionnetworks.fec -DartifactId=onion-fec -Dversion=1.0 -Dpackaging=jar
mvn install:install-file -Dfile=lib/fec-linux-x86.jar -DgroupId=com.onionnetworks.fec -DartifactId=fec-linux-x86 -Dversion=1.0 -Dpackaging=jar
#pom.xml:			<systemPath>${project.basedir}/lib/fec-win32.jar
