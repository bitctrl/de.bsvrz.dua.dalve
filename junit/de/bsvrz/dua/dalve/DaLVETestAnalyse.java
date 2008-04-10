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
import de.bsvrz.dua.dalve.util.para.ParaAnaProgImport;
import de.bsvrz.dua.dalve.util.pruef.PruefeDaLVEAnalyse;
import de.bsvrz.sys.funclib.bitctrl.dua.DUAKonstanten;
import de.bsvrz.sys.funclib.commandLineArgs.ArgumentList;
import de.bsvrz.sys.funclib.debug.Debug;

public class DaLVETestAnalyse implements ClientSenderInterface {

	protected Debug LOGGER;
	private ClientDavInterface dav;
	
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
	
	private String TEST_DATEN_VERZ;
	
	/**
	 * Parameter Importer
	 */
	private ParaAnaProgImport paraImport;
	
	public DaLVETestAnalyse(final ClientDavInterface dav, final ArgumentList alLogger, final String TEST_DATEN_VERZ)
	throws Exception {
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
		
		DD_KZD_SEND = new DataDescription(this.dav.getDataModel().getAttributeGroup(DUAKonstanten.ATG_KZD),
				  this.dav.getDataModel().getAspect(DUAKonstanten.ASP_MESSWERTERSETZUNG),
				  (short)0);
		
		this.dav.subscribeSender(this, new SystemObject[]{FS1, FS2, FS3}, 
				DD_KZD_SEND, SenderRole.source());
		
		/*
		 * Importiere Parameter
		 */
		paraImport = new ParaAnaProgImport(dav, new SystemObject[]{FS1, FS2, FS3}, TEST_DATEN_VERZ + "Parameter"); //$NON-NLS-1$
		paraImport.importiereParameterAnalyse(1);
		paraImport.importiereParameterAnalyse(2);
		paraImport.importiereParameterAnalyse(3);

		/*
		 * Initialisiert Testfahrstreifenimporter
		 */
		importFS = new TestFahrstreifenImporter(dav, TEST_DATEN_VERZ + "Messwerters_LVE"); //$NON-NLS-1$
		
		testAnalyse();
	}
	
	private void testAnalyse() throws Exception {
		System.out.println("Prüfe Datenaufbereitung LVE - Analysewerte..."); //$NON-NLS-1$
		
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
		PruefeDaLVEAnalyse prDaLVEAnalyse = new PruefeDaLVEAnalyse(this, dav, new SystemObject[]{FS1, FS2, FS3}, TEST_DATEN_VERZ + "Analysewerte"); //$NON-NLS-1$

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
			
			System.out.println("Sende Daten: FS 1-3 -> Zeile: " + csvIndex + " - Zeit: " + aktZeit); //$NON-NLS-1$ //$NON-NLS-2$
			this.dav.sendData(resultat1);
			this.dav.sendData(resultat2);
			this.dav.sendData(resultat3);
			
			//Warte auf Prüfungsabschluss aller FS für diesen Datensatz
			System.out.println("Warte auf Prüfung der FS 1-3..."); //$NON-NLS-1$
			doWait();
			
			csvIndex++;
			
			//setze neue Prüfzeit
			aktZeit = aktZeit + Constants.MILLIS_PER_MINUTE;
			
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
