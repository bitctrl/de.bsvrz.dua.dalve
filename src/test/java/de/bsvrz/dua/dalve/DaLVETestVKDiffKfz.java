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

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.bitctrl.Constants;

import de.bsvrz.dav.daf.main.ClientDavInterface;
import de.bsvrz.dav.daf.main.ClientReceiverInterface;
import de.bsvrz.dav.daf.main.ClientSenderInterface;
import de.bsvrz.dav.daf.main.Data;
import de.bsvrz.dav.daf.main.DataDescription;
import de.bsvrz.dav.daf.main.ReceiveOptions;
import de.bsvrz.dav.daf.main.ReceiverRole;
import de.bsvrz.dav.daf.main.ResultData;
import de.bsvrz.dav.daf.main.SenderRole;
import de.bsvrz.dav.daf.main.config.SystemObject;
import de.bsvrz.dua.dalve.stoerfall.vkdiffkfz.VKDiffKfzStoerfallIndikator;
import de.bsvrz.dua.dalve.util.TestFahrstreifenImporter;
import de.bsvrz.dua.dalve.util.para.ParaAnaProgImportFS;
import de.bsvrz.dua.dalve.util.para.ParaAnaProgImportMQAlsFS;
import de.bsvrz.sys.funclib.application.StandardApplicationRunner;
import de.bsvrz.sys.funclib.bitctrl.dua.DUAKonstanten;
import de.bsvrz.sys.funclib.bitctrl.dua.DUAUtensilien;
import de.bsvrz.sys.funclib.bitctrl.dua.test.CSVImporter;
import de.bsvrz.sys.funclib.bitctrl.dua.test.DAVTest;

/**
 * Testet die Berechnung von <code>VKDiffKfz</code>.
 *
 * @author BitCtrl Systems GmbH, Thierfelder
 */
public class DaLVETestVKDiffKfz implements ClientSenderInterface, ClientReceiverInterface {

	/**
	 * Datenverteiler-Verbindung.
	 */
	private ClientDavInterface dav = null;

	/**
	 * dito.
	 */
	private String aktuellerStoerfallzustand = null;

	/**
	 * Ueberpruefte Instanz der Datenaufbereitung LVE.
	 */
	private final DatenaufbereitungLVE testInstanz = DatenaufbereitungLVE.getInstance();

	/**
	 * Vorbereitungen.
	 *
	 * @throws Exception
	 *             wird weitergereicht.
	 */
	@Before
	public void setUp() throws Exception {
		dav = DAVTest.getDav(DatenaufbereitungLVETest.CON_DATA.clone());

		/**
		 * Start einer Instanz der Testapplikation
		 */
		// this.testInstanz = new DatenaufbereitungLVE();
		StandardApplicationRunner.run(testInstanz, DatenaufbereitungLVETest.CON_DATA_APP);
	}

	/**
	 * Test auf Genauigkeit von Wert <code>VKDiffKfz</code>.
	 *
	 * @throws Exception
	 *             wird weitergereicht.
	 */
	@Test
	public void testVKDiffKfz() throws Exception {
		final SystemObject fs1 = dav.getDataModel().getObject("fs.1"); //$NON-NLS-1$
		final SystemObject fs2 = dav.getDataModel().getObject("fs.2"); //$NON-NLS-1$
		final SystemObject fs3 = dav.getDataModel().getObject("fs.3"); //$NON-NLS-1$

		final SystemObject mq1 = dav.getDataModel().getObject("mq1"); //$NON-NLS-1$
		final SystemObject mq2 = dav.getDataModel().getObject("mq2"); //$NON-NLS-1$

		final SystemObject abschnitt = dav.getDataModel().getObject("abschnitt");

		/**
		 * Parametrieren
		 */
		final DataDescription ddParaAbschnitt = new DataDescription(
				dav.getDataModel().getAttributeGroup("atg.lokaleStörfallErkennungVKDiffKfz"),
				dav.getDataModel().getAspect("asp.parameterVorgabe"));
		final DataDescription ddParaMQ = new DataDescription(
				dav.getDataModel().getAttributeGroup("atg.fundamentalDiagramm"),
				dav.getDataModel().getAspect("asp.parameterVorgabe"));
		dav.subscribeSender(this, abschnitt, ddParaAbschnitt, SenderRole.sender());
		dav.subscribeSender(this, new SystemObject[] { mq1, mq2 }, ddParaMQ, SenderRole.sender());
		try {
			Thread.sleep(2000L);
		} catch (final InterruptedException ex) {
			ex.printStackTrace();
		}

		final Parameter parameter = new Parameter(
				DatenaufbereitungLVETest.TEST_DATEN_VERZ + "Parameter");
		final Data data = dav.createData(ddParaAbschnitt.getAttributeGroup());
		data.getItem("VKDiffKfz").getUnscaledValue("Ein")
				.set(Long.parseLong(parameter.get("VKDiffEin")));
		data.getItem("VKDiffKfz").getUnscaledValue("Aus")
				.set(Long.parseLong(parameter.get("VKDiffAus")));
		data.getItem("QKfzDiff").getUnscaledValue("Ein")
				.set(Long.parseLong(parameter.get("QKfzDiffEin")));
		data.getItem("QKfzDiff").getUnscaledValue("Aus")
				.set(Long.parseLong(parameter.get("QKfzDiffAus")));
		data.getUnscaledValue("tReise").set(15);
		dav.sendData(new ResultData(abschnitt, ddParaAbschnitt, System.currentTimeMillis(), data));

		final Data data1 = dav.createData(ddParaMQ.getAttributeGroup());
		data1.getUnscaledValue("Q0").set(Long.parseLong(parameter.get("Q0(1)")));
		data1.getUnscaledValue("K0").set(Long.parseLong(parameter.get("K0(1)")));
		data1.getUnscaledValue("V0").set(Long.parseLong(parameter.get("V0(1)")));
		data1.getUnscaledValue("VFrei").set(Long.parseLong(parameter.get("VFrei(1)")));
		dav.sendData(new ResultData(mq1, ddParaMQ, System.currentTimeMillis(), data1));

		final Data data2 = dav.createData(ddParaMQ.getAttributeGroup());
		data2.getUnscaledValue("Q0").set(Long.parseLong(parameter.get("Q0(2)")));
		data2.getUnscaledValue("K0").set(Long.parseLong(parameter.get("K0(2)")));
		data2.getUnscaledValue("V0").set(Long.parseLong(parameter.get("V0(2)")));
		data2.getUnscaledValue("VFrei").set(Long.parseLong(parameter.get("VFrei(2)")));
		dav.sendData(new ResultData(mq2, ddParaMQ, System.currentTimeMillis(), data2));

		try {
			Thread.sleep(2000L);
		} catch (final InterruptedException ex) {
			ex.printStackTrace();
		}

		/**
		 * Importiere Fahrstreifen-Parameter
		 */
		final ParaAnaProgImportFS paraImport = new ParaAnaProgImportFS(dav,
				new SystemObject[] { fs1, fs2, fs3 },
				DatenaufbereitungLVETest.TEST_DATEN_VERZ + "Parameter"); //$NON-NLS-1$
		paraImport.importiereParameterAnalyse(1);
		paraImport.importiereParameterAnalyse(2);
		paraImport.importiereParameterAnalyse(3);

		/**
		 * Importiere MQ-Parameter
		 */
		final ParaAnaProgImportMQAlsFS paraImportMQ = new ParaAnaProgImportMQAlsFS(dav,
				new SystemObject[] { mq1, mq2 },
				DatenaufbereitungLVETest.TEST_DATEN_VERZ + "Parameter"); //$NON-NLS-1$
		paraImportMQ.importiereParameterAnalyse(1);
		paraImportMQ.importiereParameterAnalyse(2);

		/**
		 * Anmeldung zum Senden der Fahrstreifen/MQ-Daten
		 */
		final DataDescription ddKzdSend = new DataDescription(
				dav.getDataModel().getAttributeGroup(DUAKonstanten.ATG_KZD),
				dav.getDataModel().getAspect(DUAKonstanten.ASP_MESSWERTERSETZUNG));
		dav.subscribeSender(this, new SystemObject[] { fs1, fs2, fs3 }, ddKzdSend,
				SenderRole.source());

		/**
		 * Anmeldung zum Empfang der Stoerfallsituation
		 */
		final DataDescription ddVkDiffKfz = new DataDescription(
				dav.getDataModel().getAttributeGroup(DUAKonstanten.ATG_STOERFALL_ZUSTAND),
				dav.getDataModel().getAspect("asp.störfallVerfahrenVKDiffKfz"));
		dav.subscribeReceiver(this, new SystemObject[] { abschnitt }, ddVkDiffKfz,
				ReceiveOptions.normal(), ReceiverRole.receiver());

		Data zeileFS1;
		Data zeileFS2;
		Data zeileFS3;

		long aktZeit = 0;

		int csvIndex = 2;

		final CSVImporter prognoseDatei = new CSVImporter(
				DatenaufbereitungLVETest.TEST_DATEN_VERZ + "Prognose");
		prognoseDatei.getNaechsteZeile();

		final TestFahrstreifenImporter importFS = new TestFahrstreifenImporter(dav,
				DatenaufbereitungLVETest.TEST_DATEN_VERZ + "Messwerters_LVE"); //$NON-NLS-1$
		importFS.importNaechsteZeile();

		while ((zeileFS1 = importFS.getDatensatz(1)) != null) {
			zeileFS2 = importFS.getDatensatz(2);
			zeileFS3 = importFS.getDatensatz(3);

			final ResultData resultat1 = new ResultData(fs1, ddKzdSend, aktZeit, zeileFS1);
			final ResultData resultat2 = new ResultData(fs2, ddKzdSend, aktZeit, zeileFS2);
			final ResultData resultat3 = new ResultData(fs3, ddKzdSend, aktZeit, zeileFS3);

			System.out.println(
					"Sende Daten: FS 1-3 -> Zeile: " + (csvIndex++) + " - Zeit: " + aktZeit); //$NON-NLS-1$ //$NON-NLS-2$

			synchronized (dav) {
				dav.sendData(resultat1);
				dav.sendData(resultat2);
				dav.sendData(resultat3);

				dav.wait();
			}

			final String[] soll = prognoseDatei.getNaechsteZeile();
			final double sollVkDiff = Double.parseDouble(soll[0].replaceAll(",", "."));
			final double istVkDiff = VKDiffKfzStoerfallIndikator.getTestVkDiffKfz();
			final String debug = "(Soll/Ist) --> VKDiffKfz = " + DUAUtensilien.runde(sollVkDiff, 5)
					+ "/" + DUAUtensilien.runde(istVkDiff, 5) + ", Situation: "
					+ soll[1].toLowerCase() + "/" + aktuellerStoerfallzustand.toLowerCase();

			System.out.println(debug);
			Assert.assertEquals("VKDiffKfz-Fehler: " + debug, DUAUtensilien.runde(sollVkDiff, 5),
					DUAUtensilien.runde(istVkDiff, 5));
			Assert.assertEquals("Stoerfall-Fehler: " + debug, soll[1].toLowerCase(),
					aktuellerStoerfallzustand.toLowerCase());

			/**
			 * Lese bei Importer und Prüfer den nächsten Datensatz ein
			 */
			importFS.importNaechsteZeile();

			// try {
			// Thread.sleep(1000L);
			// } catch (InterruptedException ex) {
			// ex.printStackTrace();
			// }

			/**
			 * Setze neue Prüfzeit
			 */
			aktZeit = aktZeit + Constants.MILLIS_PER_MINUTE;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void update(final ResultData[] resultate) {
		if (resultate != null) {
			for (final ResultData resultat : resultate) {
				if (resultat.getData() != null) {
					aktuellerStoerfallzustand = resultat.getData().getItem("Situation")
							.asTextValue().getText();
					synchronized (dav) {
						dav.notify();
					}
				}
			}
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void dataRequest(final SystemObject object, final DataDescription dataDescription,
			final byte state) {
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
	 * Parameter.
	 *
	 * @author BitCtrl Systems GmbH, Thierfelder
	 */
	private class Parameter extends CSVImporter {

		/**
		 * Inhalt.
		 */
		private final Map<String, String> map = new HashMap<>();

		/**
		 * Standardkonstruktor.
		 *
		 * @param dateiName
		 *            CSV-Datei
		 * @throws Exception
		 *             wird weitergereicht.
		 */
		Parameter(final String dateiName) throws Exception {
			super(new File(dateiName + ".csv"));
			String[] zeile = getNaechsteZeile();
			while ((zeile = getNaechsteZeile()) != null) {
				map.put(zeile[0], zeile[1]);
			}
		}

		/**
		 * Erfragt den Wert eines Schluessels.
		 *
		 * @param key
		 *            der Schluessel
		 * @return der Wert eines Schluessels.
		 */
		String get(final String key) {
			return map.get(key);
		}

	}
}
