/*
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
import de.bsvrz.dua.dalve.util.TestFahrstreifenImporter;
import de.bsvrz.dua.dalve.util.para.ParaAnaProgImportFS;
import de.bsvrz.dua.dalve.util.pruef.PruefeDaLVEAnalyse;
import de.bsvrz.sys.funclib.bitctrl.dua.DUAKonstanten;
import de.bsvrz.sys.funclib.commandLineArgs.ArgumentList;
import de.bsvrz.sys.funclib.debug.Debug;

// TODO: Auto-generated Javadoc
/**
 * The Class DaLVETestAnalyse.
 */
public class DaLVETestAnalyse implements ClientSenderInterface {

	/** The logger. */
	protected Debug LOGGER;

	/** The dav. */
	private final ClientDavInterface dav;

	/** Testfahrstreifen KZD FS1, FS2, FS3. */
	public static SystemObject FS1 = null;

	/** The F s2. */
	public static SystemObject FS2 = null;

	/** The F s3. */
	public static SystemObject FS3 = null;

	/** The mq. */
	public static SystemObject MQ = null;

	/** Testfahrstreifenimporter für FS 1-3. */
	private final TestFahrstreifenImporter importFS;

	/** Sende-Datenbeschreibung für KZD. */
	public static DataDescription DD_KZD_SEND = null;

	/** Tesdatenverzeichnis. */
	private final String TEST_DATEN_VERZ;

	/** Benutze Asserts. */
	private boolean useAssert = false;

	/** Parameter Importer. */
	private final ParaAnaProgImportFS paraImport;

	/**
	 * Instantiates a new da lve test analyse.
	 *
	 * @param dav
	 *            the dav
	 * @param alLogger
	 *            the al logger
	 * @param TEST_DATEN_VERZ
	 *            the test daten verz
	 * @throws Exception
	 *             the exception
	 */
	public DaLVETestAnalyse(final ClientDavInterface dav, final ArgumentList alLogger,
			final String TEST_DATEN_VERZ) throws Exception {
		this.dav = dav;
		this.TEST_DATEN_VERZ = TEST_DATEN_VERZ;

		/*
		 * Initialisiere Logger
		 */
		Debug.init("DatenaufbereitungLVEAnalyse", alLogger); //$NON-NLS-1$
		LOGGER = Debug.getLogger();

		/*
		 * Meldet Sender für KZD unter dem Aspekt Messwertersetzung an
		 */
		FS1 = this.dav.getDataModel().getObject("AAA.Test.fs.kzd.1"); //$NON-NLS-1$
		FS2 = this.dav.getDataModel().getObject("AAA.Test.fs.kzd.2"); //$NON-NLS-1$
		FS3 = this.dav.getDataModel().getObject("AAA.Test.fs.kzd.3"); //$NON-NLS-1$

		MQ = this.dav.getDataModel().getObject("mq"); //$NON-NLS-1$

		DD_KZD_SEND = new DataDescription(this.dav.getDataModel().getAttributeGroup(
				DUAKonstanten.ATG_KZD), this.dav.getDataModel().getAspect(
				DUAKonstanten.ASP_MESSWERTERSETZUNG));

		this.dav.subscribeSender(this, new SystemObject[] { FS1, FS2, FS3 }, DD_KZD_SEND,
				SenderRole.source());

		/*
		 * Importiere Parameter
		 */
		paraImport = new ParaAnaProgImportFS(dav, new SystemObject[] { FS1, FS2, FS3 },
				TEST_DATEN_VERZ + "Parameter"); //$NON-NLS-1$
		paraImport.importiereParameterAnalyse(1);
		paraImport.importiereParameterAnalyse(2);
		paraImport.importiereParameterAnalyse(3);

		/*
		 * Initialisiert Testfahrstreifenimporter
		 */
		importFS = new TestFahrstreifenImporter(dav, TEST_DATEN_VERZ + "Messwerters_LVE"); //$NON-NLS-1$
	}

	/**
	 * Test analyse.
	 *
	 * @throws Exception
	 *             the exception
	 */
	public void testAnalyse() throws Exception {
		System.out.println("Prüfe Datenaufbereitung LVE - Analysewerte..."); //$NON-NLS-1$

		Data zeileFS1;
		Data zeileFS2;
		Data zeileFS3;

		// aktueller Prüfzeitstempel
		long aktZeit = System.currentTimeMillis();

		int csvIndex = 2;

		/*
		 * Prüferklasse Empfängt Daten und vergleicht mit SOLL-Wert
		 */
		final PruefeDaLVEAnalyse prDaLVEAnalyse = new PruefeDaLVEAnalyse(this, dav,
				new SystemObject[] { FS1, FS2, FS3 }, MQ, TEST_DATEN_VERZ + "Analysewerte"); //$NON-NLS-1$

		// Lese bei Importer und Prüfer den nächsten Datensatz ein
		importFS.importNaechsteZeile();
		prDaLVEAnalyse.naechsterDatensatz(aktZeit);

		// Prüfe solange Daten vorhanden
		while ((zeileFS1 = importFS.getDatensatz(1)) != null) {
			zeileFS2 = importFS.getDatensatz(2);
			zeileFS3 = importFS.getDatensatz(3);

			final ResultData resultat1 = new ResultData(FS1, DD_KZD_SEND, aktZeit, zeileFS1);
			final ResultData resultat2 = new ResultData(FS2, DD_KZD_SEND, aktZeit, zeileFS2);
			final ResultData resultat3 = new ResultData(FS3, DD_KZD_SEND, aktZeit, zeileFS3);

			System.out.println("Sende Daten: FS 1-3 -> Zeile: " + csvIndex + " - Zeit: " + aktZeit); //$NON-NLS-1$ //$NON-NLS-2$
			dav.sendData(resultat1);
			dav.sendData(resultat2);
			dav.sendData(resultat3);

			// Warte auf Prüfungsabschluss aller FS für diesen Datensatz
			//System.out.println("Warte auf Prüfung der FS 1-3..."); //$NON-NLS-1$
			doWait();

			csvIndex++;

			// setze neue Prüfzeit
			aktZeit = aktZeit + Constants.MILLIS_PER_MINUTE;

			// Lese bei Importer und Prüfer den nächsten Datensatz ein
			importFS.importNaechsteZeile();
			prDaLVEAnalyse.naechsterDatensatz(aktZeit);
		}
	}

	/**
	 * Lässt Thread warten.
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
	 * Weckt Thread.
	 */
	public void doNotify() {
		synchronized (this) {
			notify();
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void dataRequest(final SystemObject object, final DataDescription dataDescription,
			final byte state) {
		// VOID
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean isRequestSupported(final SystemObject object,
			final DataDescription dataDescription) {
		return false;
	}

	/**
	 * Sollen Asserts benutzt werden?.
	 *
	 * @return useAssert
	 */
	public boolean getUseAssert() {
		return useAssert;
	}

	/**
	 * Schaltet Asserts an und aus.
	 *
	 * @param useAssert
	 *            Sollen Asserts benutzt werden
	 */
	public void setUseAssert(final boolean useAssert) {
		this.useAssert = useAssert;
	}
}
