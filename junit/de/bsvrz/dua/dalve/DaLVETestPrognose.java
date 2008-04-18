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
import de.bsvrz.dua.dalve.util.para.ParaAnaProgImport;
import de.bsvrz.dua.dalve.util.pruef.PruefeDaLVEPrognose;
import de.bsvrz.dua.dalve.util.pruef.PruefeDaLVEStoerfall;
import de.bsvrz.sys.funclib.bitctrl.dua.DUAKonstanten;
import de.bsvrz.sys.funclib.commandLineArgs.ArgumentList;
import de.bsvrz.sys.funclib.debug.Debug;

/**
 * 
 * @author Görlitz
 */
public class DaLVETestPrognose implements ClientSenderInterface {

	public static final boolean prStoerfall = true;
	
	/**
	 * Logger
	 */
	protected Debug LOGGER;
	
	/**
	 * Datenverteilerverbindung
	 */
	private ClientDavInterface dav;
	
	/**
	 * Testdatenverzeichnis
	 */
	private String TEST_DATEN_VERZ;
	
	/**
	 * Zu prüfender FS
	 */
	private SystemObject FS1;
	
	/**
	 * Sende-Datenbeschreibung für KZD
	 */
	public static DataDescription DD_KZD_SEND = null;
	
	/**
	 * Parameter Importer
	 */
	private TestAnalyseFahrstreifenImporter importFS;
	
	/**
	 * Sollen Asserts genutzt werden
	 */
	private boolean useAssert;
	
	/**
	 * ID der Prüferklasse für Prognosedaten
	 */
	public static final int ID_PRUEFER_PROGNOSE = 0;
	
	/**
	 * ID der Prüferklasse für StörfallIndikatoren
	 */
	public static final int ID_PRUEFER_STOERFALL = 1;

	/**
	 * Gibt an ob die Prüferklasse für Prognosedaten die Prüfung abgeschlossen hat
	 */
	private boolean prPrognoseFertig = false;
	
	/**
	 * Gibt an, ob die StörfallIndikatoren-Prüfung abgeschlossen ist
	 */
	private boolean prStoerfallFertig = false;
	
	
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
		
		DD_KZD_SEND = new DataDescription(this.dav.getDataModel().getAttributeGroup(DUAKonstanten.ATG_KURZZEIT_FS),
				  this.dav.getDataModel().getAspect(DUAKonstanten.ASP_ANALYSE),
				  (short)0);
		
		this.dav.subscribeSender(this, FS1, DD_KZD_SEND, SenderRole.sender());
		
		/*
		 * Importiere Parameter
		 */
		ParaAnaProgImport paraImport = new ParaAnaProgImport(dav, new SystemObject[]{FS1}, TEST_DATEN_VERZ + "Parameter"); //$NON-NLS-1$
		paraImport.importiereParameterPrognose();
		paraImport.importiereParameterStoerfall();
		
		/*
		 * Initialisiert Testfahrstreifenimporter
		 */
		importFS = new TestAnalyseFahrstreifenImporter(dav, TEST_DATEN_VERZ + "Analysewerte"); //$NON-NLS-1$
	}

	public void testPrognose() throws Exception {
		System.out.println("Prüfe Datenaufbereitung LVE - Prognosewerte..."); //$NON-NLS-1$
		
		Data zeileFS1;
		
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
		PruefeDaLVEStoerfall prDaLVEStoerfall = new PruefeDaLVEStoerfall(this, dav, new SystemObject[]{FS1}, TEST_DATEN_VERZ + "Prognose", useAssert); //$NON-NLS-1$

		//Lese bei Importer und Prüfer den nächsten Datensatz ein
		importFS.importNaechsteZeile();
		prDaLVEPrognose.naechsterDatensatz(aktZeit);
		prDaLVEStoerfall.naechsterDatensatz(aktZeit);
		
		//Prüfe solange Daten vorhanden
		while((zeileFS1=importFS.getDatensatz(1)) != null) {
			ResultData resultat1 = new ResultData(FS1, DD_KZD_SEND, aktZeit, zeileFS1);
			
			System.out.println("Sende Analysedaten: FS 1 -> Zeile: " + csvIndex + " - Zeit: " + aktZeit); //$NON-NLS-1$ //$NON-NLS-2$
			LOGGER.info("Sende Analysedaten: FS 1 -> Zeile: " + csvIndex + " - Zeit: " + aktZeit);
			
			synchronized(this) {
				this.dav.sendData(resultat1);
			
				//Warte auf Prüfungsabschluss aller FS für diesen Datensatz
				doWait();
			}
			
			csvIndex++;
			
			//setze neue Prüfzeit
			aktZeit = aktZeit + Constants.MILLIS_PER_MINUTE;
			
			//Lese bei Importer und Prüfer den nächsten Datensatz ein
			importFS.importNaechsteZeile();
			prDaLVEPrognose.naechsterDatensatz(aktZeit);
			prDaLVEStoerfall.naechsterDatensatz(aktZeit);
			
			prPrognoseFertig = false;
			prStoerfallFertig = false;
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
	 * Weckt Thread wenn Prognose- und Störfalldaten verglichen wurden
	 *
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
		
		if(prPrognoseFertig && prStoerfallFertig) {
			synchronized(this) {
				notify();
			}
		}
	}
	
	/**
	 * Sollen Asserts genutzt werden
	 * @param useAssert
	 */
	public void benutzeAssert(boolean useAssert) {
		this.useAssert = useAssert;
	}
		
	public void dataRequest(SystemObject object, DataDescription dataDescription, byte state) {
		//VOID
		
	}

	public boolean isRequestSupported(SystemObject object, DataDescription dataDescription) {
		return false;
	}
	
}
