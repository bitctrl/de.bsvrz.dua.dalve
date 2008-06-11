#!/bin/bash

# In das Verzeichnis des Skripts wechseln, damit relative Pfade funktionieren
cd `dirname $0`

# Allgemeine Einstellungen
source ../../../skripte-bash/einstellungen.sh

################################################################################
# SWE-Spezifische Parameter	(überprüfen und anpassen)                          #
################################################################################

kb="kb.duaTestObjekte2"

################################################################################
# Folgende Parameter müssen überprüft und evtl. angepasst werden               #
################################################################################

# Parameter für den Java-Interpreter, als Standard werden die Einstellungen aus # einstellungen.sh verwendet.
#jvmArgs="-Dfile.encoding=ISO-8859-1"

# Parameter für den Datenverteiler, als Standard werden die Einstellungen aus # einstellungen.sh verwendet.
#dav1="-datenverteiler=localhost:8083 -benutzer=Tester -authentifizierung=passwd -debugFilePath=.."

jconPort="10407"

if [ "$testlauf" ]; then
	jvmArgs=$jvmArgs" -Dcom.sun.management.jmxremote -Dcom.sun.management.jmxremote.port="$jconPort" -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.ssl=false"
fi

################################################################################
# Ab hier muss nichts mehr angepasst werden                                    #
################################################################################

# Applikation starten
$java $jvmArgs -jar ../de.bsvrz.dua.dalve-runtime.jar \
	$dav1 \
	-KonfigurationsBereichsPid=$kb \
	-gueteFaktor=0.97 \
	-benutzer=Tester \
	-debugLevelStdErrText=ERROR \
	-debugLevelFileText=INFO \
	-debugLevelFileXML=OFF \
	-debugLevelFileExcel=OFF \
	-debugLevelFileHTML=OFF \
	&
