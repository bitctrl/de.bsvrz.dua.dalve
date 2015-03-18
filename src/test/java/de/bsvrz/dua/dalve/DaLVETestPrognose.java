/*
 * Segment 4 Daten�bernahme und Aufbereitung (DUA), SWE 4.7 Datenaufbereitung LVE
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
 * Wei�enfelser Stra�e 67<br>
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

/**
 * �berpr�fung der Prognosewerte und St�rfallindikatoren.
 *
 * @author G�rlitz
 */
public class DaLVETestPrognose implements ClientSenderInterface {

	/** The Constant prStoerfall. */
	public static final boolean PR_STOERFALL = false;

	/** Logger. */
	private static final Debug LOGGER = Debug.getLogger();

	/** Datenverteilerverbindung. */
	private final ClientDavInterface dav;

	/** Testdatenverzeichnis. */
	private final String TEST_DATEN_VERZ;

	/** Zu pr�fender FS. */
	private final SystemObject FS1;

	/** Zu pr�fender MQ. */
	private final SystemObject MQ1;

	/** Sende-Datenbeschreibung f�r KZD (FS). */
	public static DataDescription DD_KZD_SEND_FS = null;

	/** Sende-Datenbeschreibung f�r KZD (QM). */
	public static DataDescription DD_KZD_SEND_MQ = null;

	/** Datenimporter (FS). */
	private final TestAnalyseFahrstreifenImporter importFS;

	/** Datenimporter (MQ). */
	private final TestAnalyseMessquerschnittImporter importMQ;

	/** Sollen Asserts genutzt werden. */
	private boolean useAssert;

	/** ID der Pr�ferklasse f�r Prognosedaten. */
	public static final int ID_PRUEFER_PROGNOSE = 0;

	/** ID der Pr�ferklasse f�r St�rfallIndikatoren. */
	public static final int ID_PRUEFER_STOERFALL = 1;

	/** Gibt an ob die Pr�ferklasse f�r Prognosedaten die Pr�fung abgeschlossen hat. */
	private boolean prPrognoseFertig = false;

	/** Gibt an, ob die St�rfallIndikatoren-Pr�fung abgeschlossen ist. */
	private boolean prStoerfallFertig = false;

	/** Die erlaubte Abweichung zwischen erwartetem und geliefertem Wert. */
	private int ergebnisWertToleranz = 0;

	/** Gibt an, ob eine �berpr�fung der St�rfallindikatoren durchgef�hrt werden soll. */
	private boolean testStoerfall = true;

	/**
	 * Initialsiert die �berpr�fung der Prognosewerte und St�rfallindikatoren.
	 *
	 * @param dav
	 *            Datenverteilerverbindung
	 * @param alLogger
	 *            Loggerargumente
	 * @param TEST_DATEN_VERZ
	 *            Testdatenverzeichnis
	 * @throws Exception
	 *             the exception
	 */
	public DaLVETestPrognose(final ClientDavInterface dav, final ArgumentList alLogger,
			final String TEST_DATEN_VERZ) throws Exception {
		this.dav = dav;
		this.TEST_DATEN_VERZ = TEST_DATEN_VERZ;

		/*
		 * Initialisiere Logger
		 */
		Debug.init("DatenaufbereitungLVEPrognose", alLogger); //$NON-NLS-1$

		/*
		 * Meldet Sender f�r KZD unter dem Aspekt Analyse an
		 */
		FS1 = this.dav.getDataModel().getObject("AAA.Test.fs.kzd.1"); //$NON-NLS-1$
		MQ1 = this.dav.getDataModel().getObject("mq.a100.0000"); //$NON-NLS-1$

		DD_KZD_SEND_FS = new DataDescription(this.dav.getDataModel().getAttributeGroup(
				DUAKonstanten.ATG_KURZZEIT_FS), this.dav.getDataModel().getAspect(
						DUAKonstanten.ASP_ANALYSE));

		DD_KZD_SEND_MQ = new DataDescription(this.dav.getDataModel().getAttributeGroup(
				DUAKonstanten.ATG_KURZZEIT_MQ), this.dav.getDataModel().getAspect(
						DUAKonstanten.ASP_ANALYSE));

		this.dav.subscribeSender(this, FS1, DD_KZD_SEND_FS, SenderRole.sender());
		this.dav.subscribeSender(this, MQ1, DD_KZD_SEND_MQ, SenderRole.sender());

		/*
		 * Importiere Parameter
		 */
		final ParaAnaProgImportFS paraImportFS = new ParaAnaProgImportFS(dav,
				new SystemObject[] { FS1 }, TEST_DATEN_VERZ + "Parameter"); //$NON-NLS-1$
		final ParaAnaProgImportMQ paraImportMQ = new ParaAnaProgImportMQ(dav,
				new SystemObject[] { MQ1 }, TEST_DATEN_VERZ + "Parameter"); //$NON-NLS-1$
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
	 * @throws Exception
	 *             the exception
	 */
	public void testPrognose() throws Exception {
		System.out.println("Pr�fe Datenaufbereitung LVE - Prognosewerte..."); //$NON-NLS-1$

		Data zeileFS1;
		Data zeileMQ1;

		// aktueller Pr�fzeitstempel
		long aktZeit = System.currentTimeMillis();

		int csvIndex = 2;

		/*
		 * Pr�ferklasse Prognosewerte Empf�ngt Daten und vergleicht mit SOLL-Wert
		 */
		final PruefeDaLVEPrognose prDaLVEPrognose = new PruefeDaLVEPrognose(this, dav,
				new SystemObject[] { FS1 }, TEST_DATEN_VERZ + "Prognose", useAssert); //$NON-NLS-1$

		/*
		 * Pr�ferklasse Prognosewerte Empf�ngt Daten und vergleicht mit SOLL-Wert
		 */
		final PruefeDaLVEStoerfall prDaLVEStoerfall = new PruefeDaLVEStoerfall(this, dav, FS1, MQ1,
				TEST_DATEN_VERZ + "Prognose", useAssert); //$NON-NLS-1$

		// Lese bei Importer und Pr�fer den n�chsten Datensatz ein
		importFS.importNaechsteZeile();
		importMQ.importNaechsteZeile();
		prDaLVEPrognose.naechsterDatensatz(aktZeit);
		if (testStoerfall) {
			prDaLVEStoerfall.naechsterDatensatz(aktZeit);
		}

		// Pr�fe solange Daten vorhanden
		while ((zeileFS1 = importFS.getDatensatz(1)) != null) {
			zeileMQ1 = importMQ.getDatensatz();

			final ResultData resultatFS = new ResultData(FS1, DD_KZD_SEND_FS, aktZeit, zeileFS1);
			final ResultData resultatMQ = new ResultData(MQ1, DD_KZD_SEND_MQ, aktZeit, zeileMQ1);

			System.out
			.println("Sende Analysedaten: FS|MQ 1 -> Zeile: " + csvIndex + " - Zeit: " + aktZeit); //$NON-NLS-1$ //$NON-NLS-2$
			LOGGER.info("Sende Analysedaten: FS|MQ 1 -> Zeile: " + csvIndex + " - Zeit: " + aktZeit);

			synchronized (this) {
				dav.sendData(resultatFS);
				dav.sendData(resultatMQ);

				// Warte auf Pr�fungsabschluss aller FS f�r diesen Datensatz
				doWait();
			}

			csvIndex++;

			// setze neue Pr�fzeit
			aktZeit = aktZeit + Constants.MILLIS_PER_MINUTE;

			// Lese bei Importer und Pr�fer den n�chsten Datensatz ein
			importFS.importNaechsteZeile();
			importMQ.importNaechsteZeile();
			prDaLVEPrognose.naechsterDatensatz(aktZeit);
			if (testStoerfall) {
				prDaLVEStoerfall.naechsterDatensatz(aktZeit);
			}

			prPrognoseFertig = false;
			prStoerfallFertig = false;
		}
	}

	/**
	 * L�sst Thread warten.
	 *
	 * @throws Exception
	 *             the exception
	 */
	private void doWait() throws Exception {
		synchronized (this) {
			wait();
		}
	}

	/**
	 * Weckt Thread wenn Prognose- und St�rfalldaten verglichen wurden.
	 *
	 * @param id_Pruefer
	 *            the id_ pruefer
	 */
	public void doNotify(final int id_Pruefer) {

		switch (id_Pruefer) {
		case ID_PRUEFER_PROGNOSE: {
			prPrognoseFertig = true;
			break;
		}
		case ID_PRUEFER_STOERFALL: {
			prStoerfallFertig = true;
			break;
		}
		}

		if (!testStoerfall) {
			prStoerfallFertig = true;
		}

		if (prPrognoseFertig && prStoerfallFertig) {
			synchronized (this) {
				notify();
			}
		}
	}

	/**
	 * Sollen Asserts genutzt werden.
	 *
	 * @param useAssert
	 *            the use assert
	 */
	public void benutzeAssert(final boolean useAssert) {
		this.useAssert = useAssert;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * de.bsvrz.dav.daf.main.ClientSenderInterface#dataRequest(de.bsvrz.dav.daf.main.config.SystemObject
	 * , de.bsvrz.dav.daf.main.DataDescription, byte)
	 */
	@Override
	public void dataRequest(final SystemObject object, final DataDescription dataDescription,
			final byte state) {
		// VOID

	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * de.bsvrz.dav.daf.main.ClientSenderInterface#isRequestSupported(de.bsvrz.dav.daf.main.config
	 * .SystemObject, de.bsvrz.dav.daf.main.DataDescription)
	 */
	@Override
	public boolean isRequestSupported(final SystemObject object,
			final DataDescription dataDescription) {
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
	 * @param ergebnisWertToleranz
	 *            Erlaubte Abweichung zwischen erwartetem und berechnetem Prognosewert
	 */
	public void setErgebnisWertToleranz(final int ergebnisWertToleranz) {
		this.ergebnisWertToleranz = ergebnisWertToleranz;
	}

	/**
	 * Soll eine �berpr�fung der St�rfallindikatoren durchgef�hrt werden.
	 *
	 * @param testStoerfall
	 *            the new test stoerfall
	 */
	public void setTestStoerfall(final boolean testStoerfall) {
		this.testStoerfall = testStoerfall;
	}

}
