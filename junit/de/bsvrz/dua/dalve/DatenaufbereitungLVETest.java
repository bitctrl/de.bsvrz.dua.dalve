package de.bsvrz.dua.dalve;

import org.junit.Before;
import org.junit.Test;

import de.bsvrz.dua.dalve.para.ParaAnaProgImport;
import de.bsvrz.dua.dalve.pruef.PruefeDaLVEAnalyse;
import de.bsvrz.dua.dalve.util.TestFahrstreifenImporter;
import de.bsvrz.sys.funclib.bitctrl.dua.DUAKonstanten;
import de.bsvrz.sys.funclib.bitctrl.dua.test.DAVTest;
import de.bsvrz.sys.funclib.bitctrl.konstante.Konstante;

import stauma.dav.clientside.ClientDavInterface;
import stauma.dav.clientside.ClientSenderInterface;
import stauma.dav.clientside.Data;
import stauma.dav.clientside.DataDescription;
import stauma.dav.clientside.ResultData;
import stauma.dav.clientside.SenderRole;
import stauma.dav.configuration.interfaces.SystemObject;
import sys.funclib.ArgumentList;
import sys.funclib.debug.Debug;

/**
 * Automatisierter Test nach Prüfspezifikation für SWE Datenaufbereitung LVE
 * 
 * @author Görlitz
 *
 */
public class DatenaufbereitungLVETest
implements ClientSenderInterface {
	
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
	protected static final String TEST_DATEN_VERZ = ".\\testDaten\\"; //$NON-NLS-1$
	
	/**
	 * Logger und Loggerargument
	 * 
	 * Pfadangabe mit Argument: -debugFilePath=[Pfad]
	 */
	private String[] argumente = new String[] {"-debugLevelFileText=ALL"};
	private ArgumentList alLogger = new ArgumentList(argumente);
	protected Debug LOGGER;
	
	/**
	 * Datenverteiler-Verbindung
	 */
	private ClientDavInterface dav = null;
	
	/**
	 * Testfahrstreifen KZD FS1, FS2, FS3
	 */
	public static SystemObject FS1 = null;
	public static SystemObject FS2 = null;
	public static SystemObject FS3 = null;
	
	/**
	 * Testfahrstreifenimporter für FS 1-3
	 */
	TestFahrstreifenImporter importFS;
	
	/**
	 * Sende-Datenbeschreibung für KZD
	 */
	public static DataDescription DD_KZD_SEND = null;
	
	/**
	 * Parameter Importer
	 */
	private ParaAnaProgImport paraImport;
	
	
	/**
	 * {@inheritDoc}
	 */
	@Before
	public void setUp() throws Exception {
		this.dav = DAVTest.getDav(CON_DATA);

		/*
		 * Meldet Sender für KZD unter dem Aspekt Messwertersetzung an
		 */
		FS1 = this.dav.getDataModel().getObject("AAA.Test.fs.kzd.1"); //$NON-NLS-1$
		FS2 = this.dav.getDataModel().getObject("AAA.Test.fs.kzd.2"); //$NON-NLS-1$
		FS3 = this.dav.getDataModel().getObject("AAA.Test.fs.kzd.3"); //$NON-NLS-1$
		
		DD_KZD_SEND = new DataDescription(this.dav.getDataModel().getAttributeGroup(DUAKonstanten.ATG_KZD),
				  this.dav.getDataModel().getAspect(DUAKonstanten.ASP_MESSWERTERSETZUNG),
				  (short)0);
		
		this.dav.subscribeSender(this, new SystemObject[]{FS1, FS2, FS3}, 
				DD_KZD_SEND, SenderRole.source());
		
		/*
		 * Importiere Parameter
		 */
		paraImport = new ParaAnaProgImport(dav, new SystemObject[]{FS1, FS2, FS3}, TEST_DATEN_VERZ + "Parameter");
		paraImport.importiereParameter(1);
		paraImport.importiereParameter(2);
		paraImport.importiereParameter(3);

		/*
		 * Initialisiert Testfahrstreifenimporter
		 */
		importFS = new TestFahrstreifenImporter(dav, TEST_DATEN_VERZ + "Messwerters.PL-gepr");
	}
	
	/**
	 * Test Analysewerte
	 * @throws Exception
	 */
	@Test
	public void testAnalyse() throws Exception {
		/*
		 * Initialisiere Logger
		 */
		Debug.init("DatenaufbereitungLVEAnalyse", alLogger); //$NON-NLS-1$
		LOGGER = Debug.getLogger();
		LOGGER.info("Prüfe Datenaufbereitung LVE - Analysewerte...");
		
		Data zeileFS1;
		Data zeileFS2;
		Data zeileFS3;
		
		//aktueller Prüfzeitstempel
		long aktZeit = System.currentTimeMillis();
		
		int csvIndex = 2;
		
		/*
		 * Prüferklasse
		 * Empfängt Daten und vergleicht mit SOLL-Wert 
		 */
		PruefeDaLVEAnalyse prDaLVEAnalyse = new PruefeDaLVEAnalyse(this, dav, new SystemObject[]{FS1, FS2, FS3}, TEST_DATEN_VERZ + "Analysewerte");

		//Lese bei Importer und Prüfer den nächsten Datensatz ein
		importFS.importNaechsteZeile();
		prDaLVEAnalyse.naechsterDatensatz(aktZeit);
		
		//Prüfe solange Daten vorhanden
		while((zeileFS1=importFS.getDatensatz(1)) != null) {
			zeileFS2 = importFS.getDatensatz(2);
			zeileFS3 = importFS.getDatensatz(3);

			ResultData resultat1 = new ResultData(FS1, DD_KZD_SEND, aktZeit, zeileFS1);
			ResultData resultat2 = new ResultData(FS2, DD_KZD_SEND, aktZeit, zeileFS2);
			ResultData resultat3 = new ResultData(FS3, DD_KZD_SEND, aktZeit, zeileFS3);
			
			LOGGER.info("Sende Daten: FS 1-3 -> Zeile: "+csvIndex+" - Zeit: "+aktZeit);
			this.dav.sendData(resultat1);
			this.dav.sendData(resultat2);
			this.dav.sendData(resultat3);
			
			//Warte auf Prüfungsabschluss aller FS für diesen Datensatz
			//TODO
			LOGGER.info("Warte auf Prüfung der FS 1-3...");
			//doWait();
			
			csvIndex++;
			
			//setze neue Prüfzeit
			aktZeit = aktZeit + Konstante.MINUTE_IN_MS;
			
			//Lese bei Importer und Prüfer den nächsten Datensatz ein
			importFS.importNaechsteZeile();
			prDaLVEAnalyse.naechsterDatensatz(aktZeit);
		}
	}

	/**
	 * Lässt Thread warten
	 * @throws Exception
	 */
	private void doWait() throws Exception {
		synchronized(this) {
			wait();
		}
	}
	
	/**
	 * Weckt Thread
	 *
	 */
	public void doNotify() {
		synchronized(this) {
			notify();
		}
	}
	
	/**
	 * {@inheritDoc}
	 */
	public void dataRequest(SystemObject object, DataDescription dataDescription, byte state) {
		//VOID
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean isRequestSupported(SystemObject object, DataDescription dataDescription) {
		return false;
	}
}
