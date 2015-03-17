/**
 * Segment 4 Datenübernahme und Aufbereitung (DUA), SWE 4.7 Datenaufbereitung LVE
 * Copyright (C) 2007-2015 BitCtrl Systems GmbH
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 51
 * Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 *
 * Contact Information:<br>
 * BitCtrl Systems GmbH<br>
 * Weißenfelser Straße 67<br>
 * 04229 Leipzig<br>
 * Phone: +49 341-490670<br>
 * mailto: info@bitctrl.de
 */
package de.bsvrz.dua.dalve;

import com.bitctrl.Constants;

import de.bsvrz.dav.daf.main.ClientDavInterface;
import de.bsvrz.dav.daf.main.ClientSenderInterface;
import de.bsvrz.dav.daf.main.Data;
import de.bsvrz.dav.daf.main.DataDescription;
import de.bsvrz.dav.daf.main.ResultData;
import de.bsvrz.dav.daf.main.SenderRole;
import de.bsvrz.dav.daf.main.config.SystemObject;
import de.bsvrz.dua.dalve.util.TestAnalyseFahrstreifenImporter;
import de.bsvrz.dua.dalve.util.TestAnalyseMessquerschnittImporter;
import de.bsvrz.dua.dalve.util.para.ParaAnaProgImportFS;
import de.bsvrz.dua.dalve.util.para.ParaAnaProgImportMQ;
import de.bsvrz.dua.dalve.util.pruef.PruefeDaLVEPrognose;
import de.bsvrz.dua.dalve.util.pruef.PruefeDaLVEStoerfall;
import de.bsvrz.sys.funclib.bitctrl.dua.DUAKonstanten;
import de.bsvrz.sys.funclib.commandLineArgs.ArgumentList;
import de.bsvrz.sys.funclib.debug.Debug;

// TODO: Auto-generated Javadoc
/**
 * Überprüfung der Prognosewerte und Störfallindikatoren.
 *
 * @author Görlitz
 */
public class DaLVETestPrognose implements ClientSenderInterface {

	/** The Constant prStoerfall. */
	public static final boolean prStoerfall = false;
	
	/** Logger. */
	protected Debug LOGGER;
	
	/** Datenverteilerverbindung. */
	private ClientDavInterface dav;
	
	/** Testdatenverzeichnis. */
	private String TEST_DATEN_VERZ;
	
	/** Zu prüfender FS. */
	private SystemObject FS1;
	
	/** Zu prüfender MQ. */
	private SystemObject MQ1;
	
	/** Sende-Datenbeschreibung für KZD (FS). */
	public static DataDescription DD_KZD_SEND_FS = null;
	
	/** Sende-Datenbeschreibung für KZD (QM). */
	public static DataDescription DD_KZD_SEND_MQ = null;
	
	/** Datenimporter (FS). */
	private TestAnalyseFahrstreifenImporter importFS;

	/** Datenimporter (MQ). */
	private TestAnalyseMessquerschnittImporter importMQ;
	
	/** Sollen Asserts genutzt werden. */
	private boolean useAssert;
	
	/** ID der Prüferklasse für Prognosedaten. */
	public static final int ID_PRUEFER_PROGNOSE = 0;
	
	/** ID der Prüferklasse für StörfallIndikatoren. */
	public static final int ID_PRUEFER_STOERFALL = 1;

	/** Gibt an ob die Prüferklasse für Prognosedaten die Prüfung abgeschlossen hat. */
	private boolean prPrognoseFertig = false;
	
	/** Gibt an, ob die StörfallIndikatoren-Prüfung abgeschlossen ist. */
	private boolean prStoerfallFertig = false;
	
	/** Die erlaubte Abweichung zwischen erwartetem und geliefertem Wert. */
	private int ergebnisWertToleranz = 0;
	
	/** Gibt an, ob eine Überprüfung der Störfallindikatoren durchgeführt werden soll. */
	private boolean testStoerfall = true;
	
	
	/**
	 * Initialsiert die Überprüfung der Prognosewerte und Störfallindikatoren.
	 *
	 * @param dav Datenverteilerverbindung
	 * @param alLogger Loggerargumente
	 * @param TEST_DATEN_VERZ Testdatenverzeichnis
	 * @throws Exception the exception
	 */
	public DaLVETestPrognose(final ClientDavInterface dav, final ArgumentList alLogger, final String TEST_DATEN_VERZ)
	throws Exception {
		this.dav = dav;
		this.TEST_DATEN_VERZ = TEST_DATEN_VERZ;
		
		/*
		 * Initialisiere Logger
		 */
		Debug.init("DatenaufbereitungLVEPrognose", alLogger); //$NON-NLS-1$
		LOGGER = Debug.getLogger();
		
		/*
		 * Meldet Sender für KZD unter dem Aspekt Analyse an
		 */
		FS1 = this.dav.getDataModel().getObject("AAA.Test.fs.kzd.1"); //$NON-NLS-1$
		MQ1 = this.dav.getDataModel().getObject("mq.a100.0000"); //$NON-NLS-1$
		
		DD_KZD_SEND_FS = new DataDescription(this.dav.getDataModel().getAttributeGroup(DUAKonstanten.ATG_KURZZEIT_FS),
				  this.dav.getDataModel().getAspect(DUAKonstanten.ASP_ANALYSE));
		
		DD_KZD_SEND_MQ = new DataDescription(this.dav.getDataModel().getAttributeGroup(DUAKonstanten.ATG_KURZZEIT_MQ),
				  this.dav.getDataModel().getAspect(DUAKonstanten.ASP_ANALYSE));
		
		this.dav.subscribeSender(this, FS1, DD_KZD_SEND_FS, SenderRole.sender());
		this.dav.subscribeSender(this, MQ1, DD_KZD_SEND_MQ, SenderRole.sender());
		
		/*
		 * Importiere Parameter
		 */
		ParaAnaProgImportFS paraImportFS = new ParaAnaProgImportFS(dav, new SystemObject[]{FS1}, TEST_DATEN_VERZ + "Parameter"); //$NON-NLS-1$
		ParaAnaProgImportMQ paraImportMQ = new ParaAnaProgImportMQ(dav, new SystemObject[]{MQ1}, TEST_DATEN_VERZ + "Parameter"); //$NON-NLS-1$
		paraImportFS.importiereParameterPrognose();
		paraImportMQ.importiereParameterPrognose();
		paraImportFS.importiereParameterStoerfall(1);
		paraImportMQ.importiereParameterStoerfall(1);
		
		/*
		 * Initialisiert Testfahrstreifenimporter
		 */
		importFS = new TestAnalyseFahrstreifenImporter(dav, TEST_DATEN_VERZ + "Analysewerte"); //$NON-NLS-1$
		
		/*
		 * Initialisiert Test-MQ-Importer
		 */
		importMQ = new TestAnalyseMessquerschnittImporter(dav, TEST_DATEN_VERZ + "Analysewerte"); //$NON-NLS-1$
	}

	/**
	 * Test prognose.
	 *
	 * @throws Exception the exception
	 */
	public void testPrognose() throws Exception {
		System.out.println("Prüfe Datenaufbereitung LVE - Prognosewerte..."); //$NON-NLS-1$
		
		Data zeileFS1;
		Data zeileMQ1;
		
		//aktueller Prüfzeitstempel
		long aktZeit = System.currentTimeMillis();
		
		int csvIndex = 2;
		
		/*
		 * Prüferklasse Prognosewerte
		 * Empfängt Daten und vergleicht mit SOLL-Wert 
		 */
		PruefeDaLVEPrognose prDaLVEPrognose = new PruefeDaLVEPrognose(this, dav, new SystemObject[]{FS1}, TEST_DATEN_VERZ + "Prognose", useAssert); //$NON-NLS-1$

		/*
		 * Prüferklasse Prognosewerte
		 * Empfängt Daten und vergleicht mit SOLL-Wert 
		 */
		PruefeDaLVEStoerfall prDaLVEStoerfall = new PruefeDaLVEStoerfall(this, dav, FS1, MQ1, TEST_DATEN_VERZ + "Prognose", useAssert); //$NON-NLS-1$

		//Lese bei Importer und Prüfer den nächsten Datensatz ein
		importFS.importNaechsteZeile();
		importMQ.importNaechsteZeile();
		prDaLVEPrognose.naechsterDatensatz(aktZeit);
		if(testStoerfall) {
			prDaLVEStoerfall.naechsterDatensatz(aktZeit);
		}
		
		//Prüfe solange Daten vorhanden
		while((zeileFS1=importFS.getDatensatz(1)) != null) {
			zeileMQ1=importMQ.getDatensatz();
			
			ResultData resultat_FS = new ResultData(FS1, DD_KZD_SEND_FS, aktZeit, zeileFS1);
			ResultData resultat_MQ = new ResultData(MQ1, DD_KZD_SEND_MQ, aktZeit, zeileMQ1);
			
			System.out.println("Sende Analysedaten: FS|MQ 1 -> Zeile: " + csvIndex + " - Zeit: " + aktZeit); //$NON-NLS-1$ //$NON-NLS-2$
			LOGGER.info("Sende Analysedaten: FS|MQ 1 -> Zeile: " + csvIndex + " - Zeit: " + aktZeit);
			
			synchronized(this) {
				this.dav.sendData(resultat_FS);
				this.dav.sendData(resultat_MQ);
			
				//Warte auf Prüfungsabschluss aller FS für diesen Datensatz
				doWait();
			}
			
			csvIndex++;
			
			//setze neue Prüfzeit
			aktZeit = aktZeit + Constants.MILLIS_PER_MINUTE;
			
			//Lese bei Importer und Prüfer den nächsten Datensatz ein
			importFS.importNaechsteZeile();
			importMQ.importNaechsteZeile();
			prDaLVEPrognose.naechsterDatensatz(aktZeit);
			if(testStoerfall) {
				prDaLVEStoerfall.naechsterDatensatz(aktZeit);
			}
			
			prPrognoseFertig = false;
			prStoerfallFertig = false;
		}
	}
	
	/**
	 * Lässt Thread warten.
	 *
	 * @throws Exception the exception
	 */
	private void doWait() throws Exception {
		synchronized(this) {
			wait();
		}
	}
	
	/**
	 * Weckt Thread wenn Prognose- und Störfalldaten verglichen wurden.
	 *
	 * @param id_Pruefer the id_ pruefer
	 */
	public void doNotify(final int id_Pruefer) {
		
		switch(id_Pruefer) {
			case ID_PRUEFER_PROGNOSE: {
				prPrognoseFertig = true;
				break;
			}
			case ID_PRUEFER_STOERFALL: {
				prStoerfallFertig = true;
				break;
			}
		}
		
		if(!testStoerfall) {
			prStoerfallFertig = true;
		}
		
		if(prPrognoseFertig && prStoerfallFertig) {
			synchronized(this) {
				notify();
			}
		}
	}
	
	/**
	 * Sollen Asserts genutzt werden.
	 *
	 * @param useAssert the use assert
	 */
	public void benutzeAssert(boolean useAssert) {
		this.useAssert = useAssert;
	}
		
	/* (non-Javadoc)
	 * @see de.bsvrz.dav.daf.main.ClientSenderInterface#dataRequest(de.bsvrz.dav.daf.main.config.SystemObject, de.bsvrz.dav.daf.main.DataDescription, byte)
	 */
	public void dataRequest(SystemObject object, DataDescription dataDescription, byte state) {
		//VOID
		
	}

	/* (non-Javadoc)
	 * @see de.bsvrz.dav.daf.main.ClientSenderInterface#isRequestSupported(de.bsvrz.dav.daf.main.config.SystemObject, de.bsvrz.dav.daf.main.DataDescription)
	 */
	public boolean isRequestSupported(SystemObject object, DataDescription dataDescription) {
		return false;
	}

	/**
	 * Lifert die erlaubte Abweichung zwischen erwartetem und berechnetem Prognosewert.
	 *
	 * @return Die erlaubte Abweichung zwischen erwartetem und berechnetem Prognosewert
	 */
	public int getErgebnisWertToleranz() {
		return ergebnisWertToleranz;
	}

	/**
	 * Setzt die erlaubte Abweichung zwischen erwartetem und berechnetem Prognosewert.
	 *
	 * @param ergebnisWertToleranz Erlaubte Abweichung zwischen erwartetem und berechnetem Prognosewert
	 */
	public void setErgebnisWertToleranz(int ergebnisWertToleranz) {
		this.ergebnisWertToleranz = ergebnisWertToleranz;
	}

	/**
	 * Soll eine Überprüfung der Störfallindikatoren durchgeführt werden.
	 *
	 * @param testStoerfall the new test stoerfall
	 */
	public void setTestStoerfall(boolean testStoerfall) {
		this.testStoerfall = testStoerfall;
	}
	
}
