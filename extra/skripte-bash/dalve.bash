#!/bin/bash

source ../../../skripte-bash/einstellungen.sh

################################################################################
# SWE-Spezifische Parameter	(überprüfen und anpassen)                          #
################################################################################

kb="-KonfigurationsBereichsPid=kb.duaTestObjekte"

################################################################################
# Folgende Parameter müssen überprüft und evtl. angepasst werden               #
################################################################################

# Parameter für den Java-Interpreter, als Standard werden die Einstellungen aus # einstellungen.sh verwendet.
#jvmArgs="-Dfile.encoding=ISO-8859-1"

# Parameter für den Datenverteiler, als Standard werden die Einstellungen aus # einstellungen.sh verwendet.
#dav1="-datenverteiler=localhost:8083 -benutzer=Tester -authentifizierung=passwd -debugFilePath=.."

################################################################################
# Ab hier muss nichts mehr angepasst werden                                    #
################################################################################

# Applikation starten
java $jvmArgs -jar ../de.bsvrz.dua.dalve-runtime.jar \
	$dav1 \
	$kb \
	-debugLevelFileText=all \
	-debugLevelStdErrText=:error \
	-debugSetLoggerAndLevel=:none \
	-debugSetLoggerAndLevel=de.bsvrz.iav:config \
	&
