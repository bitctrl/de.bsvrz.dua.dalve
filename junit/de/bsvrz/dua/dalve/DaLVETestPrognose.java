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
 * �berpr�fung der Prognosewerte und St�rfallindikatoren
 * @author G�rlitz
 */
public class DaLVETestPrognose implements ClientSenderInterface {

	public static final boolean prStoerfall = false;
	
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
	 * Zu pr�fender FS
	 */
	private SystemObject FS1;
	
	/**
	 * Zu pr�fender MQ
	 */
	private SystemObject MQ1;
	
	/**
	 * Sende-Datenbeschreibung f�r KZD (FS)
	 */
	public static DataDescription DD_KZD_SEND_FS = null;
	
	/**
	 * Sende-Datenbeschreibung f�r KZD (QM)
	 */
	public static DataDescription DD_KZD_SEND_MQ = null;
	
	/**
	 * Datenimporter (FS)
	 */
	private TestAnalyseFahrstreifenImporter importFS;

	/**
	 * Datenimporter (MQ)
	 */
	private TestAnalyseMessquerschnittImporter importMQ;
	
	/**
	 * Sollen Asserts genutzt werden
	 */
	private boolean useAssert;
	
	/**
	 * ID der Pr�ferklasse f�r Prognosedaten
	 */
	public static final int ID_PRUEFER_PROGNOSE = 0;
	
	/**
	 * ID der Pr�ferklasse f�r St�rfallIndikatoren
	 */
	public static final int ID_PRUEFER_STOERFALL = 1;

	/**
	 * Gibt an ob die Pr�ferklasse f�r Prognosedaten die Pr�fung abgeschlossen hat
	 */
	private boolean prPrognoseFertig = false;
	
	/**
	 * Gibt an, ob die St�rfallIndikatoren-Pr�fung abgeschlossen ist
	 */
	private boolean prStoerfallFertig = false;
	
	/**
	 * Die erlaubte Abweichung zwischen erwartetem und geliefertem Wert
	 */
	private int ergebnisWertToleranz = 0;
	
	/**
	 * Gibt an, ob eine �berpr�fung der St�rfallindikatoren durchgef�hrt werden soll
	 */
	private boolean testStoerfall = true;
	
	
	/**
	 * Initialsiert die �berpr�fung der Prognosewerte und St�rfallindikatoren
	 * @param dav Datenverteilerverbindung
	 * @param alLogger Loggerargumente
	 * @param TEST_DATEN_VERZ Testdatenverzeichnis
	 * @throws Exception
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
		 * Meldet Sender f�r KZD unter dem Aspekt Analyse an
		 */
		FS1 = this.dav.getDataModel().getObject("AAA.Test.fs.kzd.1"); //$NON-NLS-1$
		MQ1 = this.dav.getDataModel().getObject("mq.a100.0000"); //$NON-NLS-1$
		
		DD_KZD_SEND_FS = new DataDescription(this.dav.getDataModel().getAttributeGroup(DUAKonstanten.ATG_KURZZEIT_FS),
				  this.dav.getDataModel().getAspect(DUAKonstanten.ASP_ANALYSE),
				  (short)0);
		
		DD_KZD_SEND_MQ = new DataDescription(this.dav.getDataModel().getAttributeGroup(DUAKonstanten.ATG_KURZZEIT_MQ),
				  this.dav.getDataModel().getAspect(DUAKonstanten.ASP_ANALYSE),
				  (short)0);
		
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

	public void testPrognose() throws Exception {
		System.out.println("Pr�fe Datenaufbereitung LVE - Prognosewerte..."); //$NON-NLS-1$
		
		Data zeileFS1;
		Data zeileMQ1;
		
		//aktueller Pr�fzeitstempel
		long aktZeit = System.currentTimeMillis();
		
		int csvIndex = 2;
		
		/*
		 * Pr�ferklasse Prognosewerte
		 * Empf�ngt Daten und vergleicht mit SOLL-Wert 
		 */
		PruefeDaLVEPrognose prDaLVEPrognose = new PruefeDaLVEPrognose(this, dav, new SystemObject[]{FS1}, TEST_DATEN_VERZ + "Prognose", useAssert); //$NON-NLS-1$

		/*
		 * Pr�ferklasse Prognosewerte
		 * Empf�ngt Daten und vergleicht mit SOLL-Wert 
		 */
		PruefeDaLVEStoerfall prDaLVEStoerfall = new PruefeDaLVEStoerfall(this, dav, FS1, MQ1, TEST_DATEN_VERZ + "Prognose", useAssert); //$NON-NLS-1$

		//Lese bei Importer und Pr�fer den n�chsten Datensatz ein
		importFS.importNaechsteZeile();
		importMQ.importNaechsteZeile();
		prDaLVEPrognose.naechsterDatensatz(aktZeit);
		if(testStoerfall) {
			prDaLVEStoerfall.naechsterDatensatz(aktZeit);
		}
		
		int er = 0;
		//Pr�fe solange Daten vorhanden
		while((zeileFS1=importFS.getDatensatz(1)) != null) {
			zeileMQ1=importMQ.getDatensatz();
			
			ResultData resultat_FS = new ResultData(FS1, DD_KZD_SEND_FS, aktZeit, zeileFS1);
			ResultData resultat_MQ = new ResultData(MQ1, DD_KZD_SEND_MQ, aktZeit, zeileMQ1);
			
			System.out.println("Sende Analysedaten: FS|MQ 1 -> Zeile: " + csvIndex + " - Zeit: " + aktZeit); //$NON-NLS-1$ //$NON-NLS-2$
			LOGGER.info("Sende Analysedaten: FS|MQ 1 -> Zeile: " + csvIndex + " - Zeit: " + aktZeit);
			
			synchronized(this) {
				this.dav.sendData(resultat_FS);
				this.dav.sendData(resultat_MQ);
			
				//Warte auf Pr�fungsabschluss aller FS f�r diesen Datensatz
				doWait();
			}
			
			csvIndex++;
			
			//setze neue Pr�fzeit
			aktZeit = aktZeit + Constants.MILLIS_PER_MINUTE;
			
			//Lese bei Importer und Pr�fer den n�chsten Datensatz ein
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
	 * L�sst Thread warten
	 * @throws Exception
	 */
	private void doWait() throws Exception {
		synchronized(this) {
			wait();
		}
	}
	
	/**
	 * Weckt Thread wenn Prognose- und St�rfalldaten verglichen wurden
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

	/**
	 * Lifert die erlaubte Abweichung zwischen erwartetem und berechnetem Prognosewert
	 * @return Die erlaubte Abweichung zwischen erwartetem und berechnetem Prognosewert
	 */
	public int getErgebnisWertToleranz() {
		return ergebnisWertToleranz;
	}

	/**
	 * Setzt die erlaubte Abweichung zwischen erwartetem und berechnetem Prognosewert
	 * @param ergebnisWertToleranz Erlaubte Abweichung zwischen erwartetem und berechnetem Prognosewert
	 */
	public void setErgebnisWertToleranz(int ergebnisWertToleranz) {
		this.ergebnisWertToleranz = ergebnisWertToleranz;
	}

	/**
	 * Soll eine �berpr�fung der St�rfallindikatoren durchgef�hrt werden
	 * @param testStoerfall
	 */
	public void setTestStoerfall(boolean testStoerfall) {
		this.testStoerfall = testStoerfall;
	}
	
}
