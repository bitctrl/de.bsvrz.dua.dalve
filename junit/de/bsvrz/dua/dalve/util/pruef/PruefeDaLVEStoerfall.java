package de.bsvrz.dua.dalve.util.pruef;

import junit.framework.Assert;
import de.bsvrz.dav.daf.main.ClientDavInterface;
import de.bsvrz.dav.daf.main.ClientReceiverInterface;
import de.bsvrz.dav.daf.main.Data;
import de.bsvrz.dav.daf.main.DataDescription;
import de.bsvrz.dav.daf.main.ReceiveOptions;
import de.bsvrz.dav.daf.main.ReceiverRole;
import de.bsvrz.dav.daf.main.ResultData;
import de.bsvrz.dav.daf.main.config.SystemObject;
import de.bsvrz.dua.dalve.DaLVETestPrognose;
import de.bsvrz.dua.dalve.util.TestErgebnisStoerfallImporter;
import de.bsvrz.sys.funclib.bitctrl.dua.DUAUtensilien;
import de.bsvrz.sys.funclib.debug.Debug;

/**
 * Prüft (Vergleicht) Analyse-Datensätze der Fahrstreifen
 * 
 * @author Görlitz
 *
 */
public class PruefeDaLVEStoerfall
implements ClientReceiverInterface {

	/**
	 * Logger
	 */
	protected Debug LOGGER = Debug.getLogger();
	
	/**
	 * Sollen Asserts benutzt werden?
	 */
	protected boolean useAssert = false;
	
	/**
	 * Datenverteilerverbindung
	 */
	private ClientDavInterface dav = null;
	
	/**
	 * Empfangsdatenbeschreibung für Störfallindikator vstMARZ
	 */
	private DataDescription DD_vst_MARZ = null;
	
	/**
	 * Empfangsdatenbeschreibung für Störfallindikator vstNRW
	 */
	private DataDescription DD_vst_NRW = null;
	
	/**
	 * Empfangsdatenbeschreibung für Störfallindikator vstRDS
	 */
	private DataDescription DD_vst_RDS = null;
	
	/**
	 * Ergbnisimporter für Störfallindikatoren des FS
	 */
	private TestErgebnisStoerfallImporter importSF;
	
	/**
	 * Halten das aktuelle SOLL-Ergebnis der CSV-Datei
	 */
	private Data ergebnisVstMARZ;
	private Data ergebnisVstNRW;
	private Data ergebnisVstRDS;
	
	/**
	 * Zeitstempel, auf den gewartet wird
	 */
	private long pruefZeit;
	
	/**
	 * Gibt den Prüfungsabschluss des jeweiligen Störfalles an
	 */
	private boolean pruefungVstMARZfertig = false;
	private boolean pruefungVstNRWfertig = false;
	private boolean pruefungVstRDSfertig = false;
	
	/**
	 * ID der StörfallIndikator
	 */
	protected static final int ID_MARZ = 0;
	protected static final int ID_NRW = 1;
	protected static final int ID_RDS = 2;
	
	/**
	 * Aufrufende Klasse
	 */
	private DaLVETestPrognose caller;
	
	/**
	 * Prüferthreads für Störfallindikatoren
	 */
	private VergleicheDaLVEStoerfall verglVstMARZ = new VergleicheDaLVEStoerfall(this,ID_MARZ);
	private VergleicheDaLVEStoerfall verglVstNRW = new VergleicheDaLVEStoerfall(this,ID_NRW);
	private VergleicheDaLVEStoerfall verglVstRDS = new VergleicheDaLVEStoerfall(this, ID_RDS);
	
	/**
	 * Initial CSV-Index
	 */
	private int csvIndex = 1;
	
	/**
	 * Das Fahrstreifenobjekt
	 */
	private SystemObject FS;
	
	/**
	 * Das Messquerschnittsobjekt
	 */
	private SystemObject MQ;
	
	/**
	 * Initialisiert Prüferobjekt
	 * @param dav Datenverteilerverbindung
	 * @param FS Systemobjekt des Fahrstreifens
	 * @param MQ Systemobjekt des Messquerschnittes
	 * @param csvQuelle Testdatenverzeichnis
	 * @throws Exception
	 */
	public PruefeDaLVEStoerfall(DaLVETestPrognose caller, ClientDavInterface dav,
							  SystemObject FS, SystemObject MQ,  String csvQuelle, boolean useAssert)
	throws Exception {
		this.dav = dav;
		this.caller = caller;
		this.useAssert = useAssert;

		this.FS = FS;
		this.MQ = MQ;
		
		/*
		 * Empfängeranmeldung aller Störfallindikatoren
		 */
		DD_vst_MARZ = new DataDescription(this.dav.getDataModel().getAttributeGroup("atg.störfallZustand"), //$NON-NLS-1$
				  this.dav.getDataModel().getAspect("asp.störfallVerfahrenMARZ"), //$NON-NLS-1$
				  (short)0);
		
		DD_vst_NRW = new DataDescription(this.dav.getDataModel().getAttributeGroup("atg.störfallZustand"), //$NON-NLS-1$
				  this.dav.getDataModel().getAspect("asp.störfallVerfahrenNRW"), //$NON-NLS-1$
				  (short)0);
		
		DD_vst_RDS = new DataDescription(this.dav.getDataModel().getAttributeGroup("atg.störfallZustand"), //$NON-NLS-1$
				  this.dav.getDataModel().getAspect("asp.störfallVerfahrenRDS"), //$NON-NLS-1$
				  (short)0);

		dav.subscribeReceiver(this, FS, DD_vst_MARZ, ReceiveOptions.normal(), ReceiverRole.receiver());
		dav.subscribeReceiver(this, FS, DD_vst_NRW, ReceiveOptions.normal(), ReceiverRole.receiver());
		dav.subscribeReceiver(this, MQ, DD_vst_RDS, ReceiveOptions.normal(), ReceiverRole.receiver());
		
		/*
		 * Initialsiert Ergebnisimporter
		 */
		importSF = new TestErgebnisStoerfallImporter(dav, csvQuelle);
		
		System.out.println("Prüferklasse für Störfallindikatoren initialisiert"); //$NON-NLS-1$
	}
	
	/**
	 * Liefert die Bezeichnung des StörfallIndikators der übergebenen Verfahrens-ID
	 * @param idSF Die ID des StörfallIndikators dessen Bezeichnung angefordert wird
	 * @return Die Bezeichnung des Verfahrens
	 */
	protected String getStoerfallIndikatorBezeichnung(int idSF) {
		switch(idSF) {
			case ID_MARZ: {
				return "MARZ";
			}
			case ID_NRW: {
				return "NRW";
			}
			case ID_RDS: {
				return "RDS";
			}
		}
		return null;
	}
	
	/**
	 * Importiert nächsten Ergebnisdatensatz und setzt Prüfzeitstempel
	 * @param pruefZeit Prüfzeitstempel
	 */
	public void naechsterDatensatz(long pruefZeit) {
		this.pruefZeit = pruefZeit;
		csvIndex++;
		importSF.importNaechsteZeile();
		ergebnisVstMARZ = importSF.getSFvstMARZ();
		ergebnisVstNRW = importSF.getSFvstNRW();
		ergebnisVstRDS = importSF.getSFvstRDS();
		
		pruefungVstMARZfertig = false;
		pruefungVstNRWfertig = false;
		pruefungVstRDSfertig = false;
	}
	
	/**
	 * Wird von den Prüferthreads getriggert und
	 * benachrichtigt, wenn die Prüfung aller Störfallindikatoren
	 * abgeschlossen ist, die Aufrufende Klasse
	 * @param idSF ID des StörfallIndikators des Prüferthreads (1-3)
	 */
	public void doNotify(int idSF) {
		switch(idSF) {
			case ID_MARZ: {
				pruefungVstMARZfertig = true;
				break;
			}
			case ID_NRW: {
				pruefungVstNRWfertig = true;
				break;
			}
			case ID_RDS: {
				pruefungVstRDSfertig = true;
				break;
			}
		}

		if((pruefungVstMARZfertig && pruefungVstNRWfertig && pruefungVstRDSfertig) || !caller.prStoerfall) {
//		if((pruefungVstMARZfertig && pruefungVstNRWfertig) || !caller.prStoerfall) {
			LOGGER.info("Alle Störfallindikatoren geprüft. Benachrichtige Hauptthread..."); //$NON-NLS-1$
			caller.doNotify(caller.ID_PRUEFER_STOERFALL);
		}
	}
	
	/**
	 * {@inheritDoc}
	 */
	public void update(ResultData[] results) {
		for (ResultData result : results) {
			//Pruefe Ergebnisdatensatz auf Zeitstempel
			if (result.getData() != null &&
				result.getDataTime() == pruefZeit) {

				try {
					//Ermittle SF und pruefe Daten
					if(result.getDataDescription().equals(DD_vst_MARZ) && result.getObject().equals(FS)) { //$NON-NLS-1$
						verglVstMARZ.vergleiche(result.getData(),ergebnisVstMARZ,csvIndex);
					} else if(result.getDataDescription().equals(DD_vst_NRW) && result.getObject().equals(FS)) { //$NON-NLS-1$
						verglVstNRW.vergleiche(result.getData(),ergebnisVstNRW,csvIndex);
					} else if(result.getDataDescription().equals(DD_vst_RDS) && result.getObject().equals(MQ)) { //$NON-NLS-1$
						verglVstRDS.vergleiche(result.getData(),ergebnisVstRDS,csvIndex);
					}
				} catch(Exception e) {}
			}
		}		
	}
}

class VergleicheDaLVEStoerfall extends Thread {
	
	/**
	 * Logger
	 */
	protected Debug LOGGER = Debug.getLogger();
	
	/**
	 * Aufrufende Klasse
	 */
	private PruefeDaLVEStoerfall caller; 
	
	/**
	 * Zu prüfende ID des StoerfallVerfahrens
	 */
	private int id_SF;
	
	/**
	 * Zu prüfender StoerfallIndikator
	 */
	private final String stoerfallInidikator;
	
	/**
	 * Zu vergleichendes SOLL- und IST-Ergebnis 
	 */
	private Data sollErgebnis;
	private Data istErgebnis;
	
	/**
	 * Aktueller CSV-Index der SOLL- und IST-Daten
	 */
	private int csvIndex;
	
	/**
	 * Initialisiert Prüferthread
	 * @param caller Aufrufende Klasse
	 * @param id_SF Zu prüfender Störfallindikator
	 */
	public VergleicheDaLVEStoerfall(PruefeDaLVEStoerfall caller, int id_SF) {
		this.caller = caller;
		this.id_SF = id_SF;
		this.stoerfallInidikator = caller.getStoerfallIndikatorBezeichnung(id_SF);
		System.out.println("Prüfthread [PT] ("+stoerfallInidikator+") initialisiert"); //$NON-NLS-1$ //$NON-NLS-2$
		//starte Thread
		this.start();
	}
	
	/**
	 * Vergleiche SOLL- und IST-Ergebnisdatensatz
	 * @param sollErgebnis SOLL-Datensatz
	 * @param istErgebnis IST-Datensatz
	 */
	public void vergleiche(Data istErgebnis, Data sollErgebnis, int csvIndex) {
		this.sollErgebnis = sollErgebnis;
		this.istErgebnis = istErgebnis;
		this.csvIndex = csvIndex;

		synchronized(this) {
			//wecke Thread
			this.notify();
		}
	}
	
	/**
	 * Prüfthread
	 */
	public void run() {
		//Thread läuft bis Programmende
		while(true) {
			//warte nit prüfung bis geweckt
			doWait();

			//vergleiche
			doVergleich();
		}
	}
	
	/**
	 * Führt vergleich durch 
	 *
	 */
	private void doVergleich() {
		String csvDS = "[SF:"+stoerfallInidikator+"-Z:"+csvIndex+"]"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		String loggerOut = csvDS + " Vergleichsergebnis:\n"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		
		int sollWert;
		int istWert;

		sollWert = DUAUtensilien.getAttributDatum("Situation", sollErgebnis).asUnscaledValue().intValue();
		istWert = DUAUtensilien.getAttributDatum("Situation", istErgebnis).asUnscaledValue().intValue();
			
		boolean isError = false;
		
		if(sollWert == istWert) {
			loggerOut += csvDS+" OK: -> "+sollWert+" (SOLL) == (IST) "+istWert + "\n"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
			if(id_SF != caller.ID_RDS && id_SF != caller.ID_NRW){
				System.out.println(" SF : " + sollWert + "==" + istWert);
			}
		} else {
			isError = true;
			
			String err = csvDS+ " ERR: -> "+sollWert+" (SOLL) <> (IST) "+istWert; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ 
			loggerOut += err + "\n";
			
			if(caller.useAssert) {
				//TODO:Stoerfaelle NRW und RDS werden ignoriert 
				if(id_SF != caller.ID_RDS && id_SF != caller.ID_NRW)
					Assert.assertTrue(err, false);
			}
		}

		if(isError && !caller.useAssert) {
			//TODO:Stoerfaelle NRW und RDS werden ignoriert 
			if(id_SF != caller.ID_RDS && id_SF != caller.ID_NRW)
				System.out.println(loggerOut);
		}
		
		LOGGER.info(loggerOut);
		
		//Benachrichtige aufrufende Klasse und übermittle SF-ID 
		caller.doNotify(id_SF);
	}
	
	/**
	 * Laesst Prüfthread warten
	 *
	 */
	private void doWait() {
		synchronized(this) {
			try {
				this.wait();
			} catch (Exception e) {
				System.out.println("Error: Pruefer-Thread " + stoerfallInidikator + " (wait)"); //$NON-NLS-1$ //$NON-NLS-2$
			}
		}
	}
}
