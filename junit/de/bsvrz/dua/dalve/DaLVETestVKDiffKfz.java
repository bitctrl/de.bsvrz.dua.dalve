/**
 * Segment 4 Datenübernahme und Aufbereitung (DUA), SWE 4.7 Datenaufbereitung LVE
 * Copyright (C) 2007 BitCtrl Systems GmbH 
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

import org.junit.Before;
import org.junit.Test;

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
import de.bsvrz.dua.dalve.util.para.ParaAnaProgImportMQAlsFS;
import de.bsvrz.sys.funclib.bitctrl.dua.DUAKonstanten;
import de.bsvrz.sys.funclib.bitctrl.dua.test.DAVTest;

/**
 * Testet die Berechnung der virtuellen Messquerschnitte auf Basis der
 * Attributgruppe <code>atg.messQuerschnittVirtuellVLage</code>.
 * 
 * @author BitCtrl Systems GmbH, Thierfelder
 * 
 * @version $Id$
 */
public class DaLVETestVKDiffKfz implements ClientSenderInterface {

	/**
	 * Datenverteiler-Verbindung.
	 */
	private ClientDavInterface dav = null;

	/**
	 * Testfahrstreifen KZD FS1, FS2, FS3.
	 */
	public static SystemObject FS1 = null;
	public static SystemObject FS2 = null;
	public static SystemObject FS3 = null;

	/**
	 * Testfahrstreifenimporter für FS 1-3.
	 */
	private TestFahrstreifenImporter importFS;

	/**
	 * Vorbereitungen.
	 * 
	 * @throws Exception
	 *             wird weitergereicht.
	 */
	@Before
	public void setUp() throws Exception {
		this.dav = DAVTest.getDav(DatenaufbereitungLVETest.CON_DATA.clone());
	}

	/**
	 * Test Analysewerte.
	 * 
	 * @throws Exception
	 *             wird weitergereicht.
	 */
	@Test
	public void testMQVirtuell() throws Exception {

		/*
		 * Meldet Sender für KZD unter dem Aspekt Messwertersetzung an
		 */
		FS1 = this.dav.getDataModel().getObject("fs.1"); //$NON-NLS-1$
		FS2 = this.dav.getDataModel().getObject("fs.2"); //$NON-NLS-1$
		FS3 = this.dav.getDataModel().getObject("fs.3"); //$NON-NLS-1$

		SystemObject MQ1 = this.dav.getDataModel().getObject("mq1"); //$NON-NLS-1$
		SystemObject MQ2 = this.dav.getDataModel().getObject("mq2"); //$NON-NLS-1$
				
		/*
		 * Importiere Parameter
		 */
		ParaAnaProgImportMQAlsFS paraImportFS = new ParaAnaProgImportMQAlsFS(dav, new SystemObject[]{MQ1, MQ2}, DatenaufbereitungLVETest.TEST_DATEN_VERZ + "Parameter"); //$NON-NLS-1$
		paraImportFS.importiereParameterAnalyse(1);
		paraImportFS.importiereParameterAnalyse(2);
		
		DataDescription ddKzdSend = new DataDescription(this.dav.getDataModel()
				.getAttributeGroup(DUAKonstanten.ATG_KZD), this.dav
				.getDataModel().getAspect(DUAKonstanten.ASP_MESSWERTERSETZUNG),
				(short) 0);

		this.dav.subscribeSender(this, new SystemObject[] { FS1, FS2, FS3 },
				ddKzdSend, SenderRole.source());

		/*
		 * Importiere Parameter
		 */
		ParaAnaProgImportFS paraImport = new ParaAnaProgImportFS(dav,
				new SystemObject[] { FS1, FS2, FS3 },
				DatenaufbereitungLVETest.TEST_DATEN_VERZ + "Parameter"); //$NON-NLS-1$
		paraImport.importiereParameterAnalyse(1);
		paraImport.importiereParameterAnalyse(2);
		paraImport.importiereParameterAnalyse(3);

		Data zeileFS1;
		Data zeileFS2;
		Data zeileFS3;

		long aktZeit = 0;

		int csvIndex = 2;

		
		importFS = new TestFahrstreifenImporter(dav,
				DatenaufbereitungLVETest.TEST_DATEN_VERZ_VKDIFFKFZ
						+ "Messwerters_LVE"); //$NON-NLS-1$
		importFS.importNaechsteZeile();

		while ((zeileFS1 = importFS.getDatensatz(1)) != null) {
			zeileFS2 = importFS.getDatensatz(2);
			zeileFS3 = importFS.getDatensatz(3);

			ResultData resultat1 = new ResultData(FS1, ddKzdSend, aktZeit,
					zeileFS1);
			ResultData resultat2 = new ResultData(FS2, ddKzdSend, aktZeit,
					zeileFS2);
			ResultData resultat3 = new ResultData(FS3, ddKzdSend, aktZeit,
					zeileFS3);

			System.out
					.println("Sende Daten: FS 1-3 -> Zeile: " + (csvIndex++) + " - Zeit: " + aktZeit); //$NON-NLS-1$ //$NON-NLS-2$
			this.dav.sendData(resultat1);
			this.dav.sendData(resultat2);
			this.dav.sendData(resultat3);

			// Lese bei Importer und Prüfer den nächsten Datensatz ein
			importFS.importNaechsteZeile();

			try {
				Thread.sleep(2000L);
			} catch (InterruptedException ex) {
				ex.printStackTrace();
			}

			// setze neue Prüfzeit
			aktZeit = aktZeit + Constants.MILLIS_PER_MINUTE;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public void dataRequest(SystemObject object,
			DataDescription dataDescription, byte state) {
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean isRequestSupported(SystemObject object,
			DataDescription dataDescription) {
		return false;
	}

}
