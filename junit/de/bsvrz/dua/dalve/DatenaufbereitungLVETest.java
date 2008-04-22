package de.bsvrz.dua.dalve;

import org.junit.Before;
import org.junit.Test;

import de.bsvrz.dav.daf.main.ClientDavInterface;
import de.bsvrz.sys.funclib.bitctrl.dua.test.DAVTest;
import de.bsvrz.sys.funclib.commandLineArgs.ArgumentList;

/**
 * Automatisierter Test nach Prüfspezifikation für SWE Datenaufbereitung LVE
 * 
 * @author Görlitz
 *
 */
public class DatenaufbereitungLVETest {
	
	/**
	 * Verbindungsdaten
	 */
	public static final String[] CON_DATA = new String[] {
			"-datenverteiler=localhost:8083", //$NON-NLS-1$ 
			"-benutzer=Tester", //$NON-NLS-1$
			"-authentifizierung=c:\\passwd" }; //$NON-NLS-1$
	
	/**
	 * Verzeichnis, in dem sich die CSV-Dateien mit den Testdaten befinden
	 */
//	protected static final String TEST_DATEN_VERZ = ".\\extra\\testDaten\\V_2.7.2 (12.03.08)\\"; //$NON-NLS-1$
//	protected static final String TEST_DATEN_VERZ = ".\\extra\\testDaten\\V_2.7.3 (20.03.08)\\"; //$NON-NLS-1$
//	protected static final String TEST_DATEN_VERZ = ".\\extra\\testDaten\\V_2.7.4 (27.03.08)\\"; //$NON-NLS-1$
//	protected static final String TEST_DATEN_VERZ = ".\\extra\\testDaten\\V_2.7.5 (28.03.08)\\"; //$NON-NLS-1$
//	protected static final String TEST_DATEN_VERZ = ".\\extra\\testDaten\\V_2.7.6 (01.04.08)\\"; //$NON-NLS-1$
//	protected static final String TEST_DATEN_VERZ = ".\\extra\\testDaten\\V_2.7.7 (02.04.08)\\"; //$NON-NLS-1$
//	protected static final String TEST_DATEN_VERZ = ".\\extra\\testDaten\\V_2.7.7 (03.04.08)\\"; //$NON-NLS-1$
//	protected static final String TEST_DATEN_VERZ = ".\\extra\\testDaten\\V_2.7.8 (04.04.08)\\"; //$NON-NLS-1$
//	protected static final String TEST_DATEN_VERZ = ".\\extra\\testDaten\\V_2.7.9 (05.04.08)\\"; //$NON-NLS-1$
//	protected static final String TEST_DATEN_VERZ = ".\\extra\\testDaten\\V_2.8 (09.04.08)\\"; //$NON-NLS-1$
//	protected static final String TEST_DATEN_VERZ = ".\\extra\\testDaten\\V_2.8.1 (12.04.08)\\"; //$NON-NLS-1$
//	protected static final String TEST_DATEN_VERZ = ".\\extra\\testDaten\\V_2.8.2 (14.04.08)\\"; //$NON-NLS-1$
	protected static final String TEST_DATEN_VERZ = ".\\extra\\testDaten\\V_2.8.3 (22.04.08)\\"; //$NON-NLS-1$
	
	/**
	 * Logger und Loggerargument
	 * 
	 * Pfadangabe mit Argument: -debugFilePath=[Pfad]
	 */
	private String[] argumente = new String[] {"-debugLevelFileText=ALL"}; //$NON-NLS-1$
	private ArgumentList alLogger = new ArgumentList(argumente);
	
	/**
	 * Datenverteiler-Verbindung
	 */
	private ClientDavInterface dav = null;
	
	/**
	 * Vorbereitungen
	 */
	@Before
	public void setUp() throws Exception {
		this.dav = DAVTest.getDav(CON_DATA);
	}
	
	/**
	 * Test Analysewerte
	 * @throws Exception
	 */
	@Test
	public void testAnalyse() throws Exception {
//		new DaLVETestAnalyse(dav, alLogger, TEST_DATEN_VERZ);
	}
	
	/**
	 * Test Prognosewerte
	 * @throws Exception
	 */
	@Test
	public void testPrognose() throws Exception {
		DaLVETestPrognose prognoseTest = new DaLVETestPrognose(dav, alLogger, TEST_DATEN_VERZ);
		prognoseTest.benutzeAssert(false);
		prognoseTest.testPrognose();
	}
}
