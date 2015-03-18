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

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import de.bsvrz.dav.daf.main.ClientDavInterface;
import de.bsvrz.sys.funclib.bitctrl.dua.test.DAVTest;
import de.bsvrz.sys.funclib.commandLineArgs.ArgumentList;

/**
 * Automatisierter Test nach Prüfspezifikation für SWE Datenaufbereitung LVE.
 *
 * @author Görlitz
 */
@Ignore("Verbindung zum Testsystem prüfen")
public class DatenaufbereitungLVETest {

	/** Verbindungsdaten. */
	public static final String[] CON_DATA = new String[] { "-datenverteiler=localhost:8083", //$NON-NLS-1$
			"-benutzer=Tester", //$NON-NLS-1$
			"-authentifizierung=passwd" }; //$NON-NLS-1$

	/** Verbindungsdaten. */
	public static final String[] CON_DATA_APP = new String[] { CON_DATA[0], CON_DATA[1],
			CON_DATA[2], "-debugLevelStdErrText=ERROR", "-debugLevelFileText=OFF",
			"-KonfigurationsBereichsPid=kb.duaTestObjekte2" };

	/** Verzeichnis, in dem sich die CSV-Dateien mit den Testdaten befinden. */
	// protected static final String TEST_DATEN_VERZ =
	// ".\\extra\\testDaten\\V_2.7.2 (12.03.08)\\"; //$NON-NLS-1$
	// protected static final String TEST_DATEN_VERZ =
	// ".\\extra\\testDaten\\V_2.7.3 (20.03.08)\\"; //$NON-NLS-1$
	// protected static final String TEST_DATEN_VERZ =
	// ".\\extra\\testDaten\\V_2.7.4 (27.03.08)\\"; //$NON-NLS-1$
	// protected static final String TEST_DATEN_VERZ =
	// ".\\extra\\testDaten\\V_2.7.5 (28.03.08)\\"; //$NON-NLS-1$
	// protected static final String TEST_DATEN_VERZ =
	// ".\\extra\\testDaten\\V_2.7.6 (01.04.08)\\"; //$NON-NLS-1$
	// protected static final String TEST_DATEN_VERZ =
	// ".\\extra\\testDaten\\V_2.7.7 (02.04.08)\\"; //$NON-NLS-1$
	// protected static final String TEST_DATEN_VERZ =
	// ".\\extra\\testDaten\\V_2.7.7 (03.04.08)\\"; //$NON-NLS-1$
	// protected static final String TEST_DATEN_VERZ =
	// ".\\extra\\testDaten\\V_2.7.8 (04.04.08)\\"; //$NON-NLS-1$
	// protected static final String TEST_DATEN_VERZ =
	// ".\\extra\\testDaten\\V_2.7.9 (05.04.08)\\"; //$NON-NLS-1$
	// protected static final String TEST_DATEN_VERZ =
	// ".\\extra\\testDaten\\V_2.8 (09.04.08)\\"; //$NON-NLS-1$
	// protected static final String TEST_DATEN_VERZ =
	// ".\\extra\\testDaten\\V_2.8.1 (12.04.08)\\"; //$NON-NLS-1$
	// protected static final String TEST_DATEN_VERZ =
	// ".\\extra\\testDaten\\V_2.8.2 (14.04.08)\\"; //$NON-NLS-1$
	// protected static final String TEST_DATEN_VERZ =
	// ".\\extra\\testDaten\\V_2.8.3 (22.04.08)\\"; //$NON-NLS-1$
	// protected static final String TEST_DATEN_VERZ =
	// ".\\extra\\testDaten\\V_2.8.4 (24.04.08)\\"; //$NON-NLS-1$
	// protected static final String TEST_DATEN_VERZ =
	// ".\\extra\\testDaten\\V_2.8.5 (28.04.08)\\"; //$NON-NLS-1$
	// protected static final String TEST_DATEN_VERZ =
	// ".\\extra\\testDaten\\V_2.8.6 (08.05.08)\\"; //$NON-NLS-1$
	// protected static final String TEST_DATEN_VERZ =
	// ".\\extra\\testDaten\\V_2.8.7 (09.05.08)\\"; //$NON-NLS-1$
	// protected static final String TEST_DATEN_VERZ =
	// ".\\extra\\testDaten\\V_2.8.8 (15.05.08)\\"; //$NON-NLS-1$
	// protected static final String TEST_DATEN_VERZ =
	// ".\\extra\\testDaten\\V_2.8.9 (15.05.08)\\"; //$NON-NLS-1$
	// protected static final String TEST_DATEN_VERZ =
	// ".\\extra\\testDaten\\V_2.9.1 (19.05.08)\\"; //$NON-NLS-1$
	// protected static final String TEST_DATEN_VERZ = ".\\extra\\testDaten\\V_2.9.2 (20.05.08)\\"; //$NON-NLS-1$
	//protected static final String TEST_DATEN_VERZ = ".\\extra\\testDaten\\V_2.9.3 (24.05.08)\\"; //$NON-NLS-1$

	protected static final String TEST_DATEN_VERZ = "../testDaten/V_2.9.3 (24.05.08)/";

	/** Verzeichnis, in dem sich die CSV-Datei mit den Testdaten fuer virtuelle MQs befinden. */
	//protected static final String TEST_DATEN_VERZ_VIRTUELL = ".\\extra\\testDaten\\mqVirtuell\\"; //$NON-NLS-1$

	protected static final String TEST_DATEN_VERZ_VIRTUELL = "../testDaten/mqVirtuell/"; //$NON-NLS-1$

	/** Logger und Loggerargument Pfadangabe mit Argument: -debugFilePath=[Pfad]. */
	private final String[] argumente = new String[] { "-debugLevelFileText=ALL" }; //$NON-NLS-1$

	/** The al logger. */
	private final ArgumentList alLogger = new ArgumentList(argumente);

	/** Datenverteiler-Verbindung. */
	private ClientDavInterface dav = null;

	/**
	 * Vorbereitungen.
	 *
	 * @throws Exception
	 *             the exception
	 */
	@Before
	public void setUp() throws Exception {
		dav = DAVTest.getDav(CON_DATA.clone());
	}

	/**
	 * Test Analysewerte.
	 *
	 * @throws Exception
	 *             the exception
	 */
	@Test
	public void testAnalyse() throws Exception {
		final DaLVETestAnalyse analzseTest = new DaLVETestAnalyse(dav, alLogger, TEST_DATEN_VERZ);
		analzseTest.setUseAssert(false);
		analzseTest.testAnalyse();
	}

	/**
	 * Test Prognosewerte.
	 *
	 * @throws Exception
	 *             the exception
	 */
	@Test
	public void testPrognose() throws Exception {
		final DaLVETestPrognose prognoseTest = new DaLVETestPrognose(dav, alLogger, TEST_DATEN_VERZ);
		prognoseTest.benutzeAssert(false);
		prognoseTest.setErgebnisWertToleranz(1);
		prognoseTest.setTestStoerfall(true);
		prognoseTest.testPrognose();
	}

}
