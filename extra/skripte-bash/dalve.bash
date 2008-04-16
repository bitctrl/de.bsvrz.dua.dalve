#!/bin/bash

# In das Verzeichnis des Skripts wechseln, damit relative Pfade funktionieren
cd `dirname $0`

# Allgemeine Einstellungen
source ../../../skripte-bash/einstellungen.sh

################################################################################
# SWE-Spezifische Parameter	(überprüfen und anpassen)                          #
################################################################################

kb="kb.MQ_Konfig_A5,kb.MQ_Konfig_A6,kb.MQ_Konfig_A656,kb.MQ_Konfig_A7,kb.MQ_Konfig_A8,kb.MQ_Konfig_A81,kb.MQ_Konfig_A831,kb.MQ_Konfig_A864,kb.MQ_Konfig_A96,kb.MQ_Konfig_A98,kb.MQ_Konfig_B10,kb.MQ_Konfig_B27,kb.MQ_Konfig_DZ"

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
	-KonfigurationsBereichsPid=$kb \
	-debugLevelStdErrText=ERROR \
	-debugLevelFileText=INFO \
	-debugLevelFileXML=OFF \
	-debugLevelFileExcel=OFF \
	-debugLevelFileHTML=OFF \
	&
