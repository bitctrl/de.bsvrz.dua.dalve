package de.bsvrz.dua.dalve.pruef;

import de.bsvrz.dua.dalve.DatenaufbereitungLVETest;
import de.bsvrz.dua.dalve.util.TestErgebnisAnalyseImporter;
import de.bsvrz.sys.funclib.bitctrl.dua.DUAKonstanten;
import stauma.dav.clientside.ClientDavInterface;
import stauma.dav.clientside.ClientReceiverInterface;
import stauma.dav.clientside.Data;
import stauma.dav.clientside.DataDescription;
import stauma.dav.clientside.ReceiveOptions;
import stauma.dav.clientside.ReceiverRole;
import stauma.dav.clientside.ResultData;
import stauma.dav.configuration.interfaces.SystemObject;
import sys.funclib.ArgumentList;
import sys.funclib.debug.Debug;

/**
 * Prüft (Vergleicht) Analyse-Datensätze der Fahrstreifen
 * 
 * @author Görlitz
 *
 */
public class PruefeDaLVEAnalyse
implements ClientReceiverInterface {

	/**
	 * Logger
	 */
	protected Debug LOGGER = Debug.getLogger();
	
	/**
	 * Datenverteilerverbindung
	 */
	private ClientDavInterface dav = null;
	
	/**
	 * Empfangsdatenbeschreibung
	 */
	private DataDescription DD_KZDFS_EMPF = null;
	
	/**
	 * Ergbnisimporter für Analysewerte der FS
	 */
	private TestErgebnisAnalyseImporter importAnaFS;
	
	/**
	 * Halten das aktuelle SOLL-Ergebnis der CSV-Datei
	 */
	private Data ergebnisFS1;
	private Data ergebnisFS2;
	private Data ergebnisFS3;
	
	/**
	 * Zeitstempel, auf den gewartet wird
	 */
	private long pruefZeit;
	
	/**
	 * Gibt den Prüfungsabschluss des jeweiligen FS an
	 */
	private boolean pruefungFS1fertig = false;
	private boolean pruefungFS2fertig = false;
	private boolean pruefungFS3fertig = false;
	
	/**
	 * Aufrufende Klasse
	 */
	private DatenaufbereitungLVETest caller;
	
	/**
	 * Prüferthreads für FS 1-3
	 */
	private VergleicheDaLVEAnalyse verglFS1 = new VergleicheDaLVEAnalyse(this,1);
	private VergleicheDaLVEAnalyse verglFS2 = new VergleicheDaLVEAnalyse(this,2);
	private VergleicheDaLVEAnalyse verglFS3 = new VergleicheDaLVEAnalyse(this,3);
	
	/**
	 * Initialisiert Prüferobjekt
	 * @param dav Datenverteilerverbindung
	 * @param FS Systemobjekt des Fahrstreifens
	 * @param fsIndex Fahrstreifenindex (1-3)
	 * @param TEST_DATEN_VERZ Testdatenverzeichnis
	 * @throws Exception
	 */
	public PruefeDaLVEAnalyse(DatenaufbereitungLVETest caller, ClientDavInterface dav,
							  SystemObject[] FS,  String csvQuelle)
	throws Exception {
		this.dav = dav;
		this.caller = caller;
		
		/*
		 * Empfängeranmeldung aller 3 Fahrstreifen
		 */
		DD_KZDFS_EMPF = new DataDescription(this.dav.getDataModel().getAttributeGroup("	atg.verkehrsDatenKurzZeitFs"),
				  this.dav.getDataModel().getAspect("asp.analyse"),
				  (short)0);

		dav.subscribeReceiver(this, FS, DD_KZDFS_EMPF, ReceiveOptions.normal(), ReceiverRole.receiver());
		
		/*
		 * Initialsiert Ergebnisimporter
		 */
		importAnaFS = new TestErgebnisAnalyseImporter(dav, csvQuelle);
	}
	
	/**
	 * Importiert nächsten Ergebnisdatensatz und setzt Prüfzeitstempel
	 * @param pruefZeit Prüfzeitstempel
	 */
	public void naechsterDatensatz(long pruefZeit) {
		this.pruefZeit = pruefZeit;
		importAnaFS.importNaechsteZeile();
		ergebnisFS1 = importAnaFS.getFSAnalyseDatensatz(1);
		ergebnisFS2 = importAnaFS.getFSAnalyseDatensatz(2);
		ergebnisFS3 = importAnaFS.getFSAnalyseDatensatz(3);
		
		pruefungFS1fertig = false;
		pruefungFS2fertig = false;
		pruefungFS3fertig = false;
	}
	
	/**
	 * Wird von den Prüferthreads getriggert und
	 * benachrichtigt, wenn die Prüfung aller 3 FS
	 * abgeschlossen ist, die Aufrufende Klasse
	 * @param FS Fahstreifenindex des Prüferthreads (1-3)
	 */
	public void doNotify(int FS) {
		switch(FS) {
			case 1: {
				pruefungFS1fertig = true;
				break;
			}
			case 2: {
				pruefungFS2fertig = true;
				break;
			}
			case 3: {
				pruefungFS3fertig = true;
				break;
			}
		}
		if(pruefungFS1fertig && pruefungFS2fertig && pruefungFS3fertig) {
			caller.doNotify();
		}
	}
	
	/**
	 * {@inheritDoc}
	 */
	public void update(ResultData[] results) {
		for (ResultData result : results) {
			//Pruefe Ergebnisdatensatz auf Zeitstempel
			if (result.getDataDescription().equals(DD_KZDFS_EMPF) &&
				result.getData() != null &&
				result.getDataTime() == pruefZeit) {

				try {
					//Ermittle FS und pruefe Daten
					if(result.getObject().getName().endsWith(".1")) {
						verglFS1.vergleiche(result.getData(),ergebnisFS1);
					} else if(result.getObject().getName().endsWith(".2")) {
						verglFS2.vergleiche(result.getData(),ergebnisFS2);
					} else if(result.getObject().getName().endsWith(".3")) {
						verglFS3.vergleiche(result.getData(),ergebnisFS3);
					}
				} catch(Exception e) {}

				LOGGER.info("Zu prüfendes Datum empfangen. Prüfe...");
			}
		}		
	}
}

class VergleicheDaLVEAnalyse extends Thread {
	
	/**
	 * Logger
	 */
	protected Debug LOGGER = Debug.getLogger();
	
	/**
	 * Aufrufende Klasse
	 */
	private PruefeDaLVEAnalyse caller; 
	
	/**
	 * Zu prüfender FS
	 */
	private int fsIndex;
	
	/**
	 * Zu vergleichendes SOLL- und IST-Ergebnis 
	 */
	private Data sollErgebnis;
	private Data istErgebnis;
	
	/**
	 * Initialisiert Prüferthread
	 * @param caller Aufrufende Klasse
	 * @param fsIndex Zu prüfender Fahrstreifen
	 */
	public VergleicheDaLVEAnalyse(PruefeDaLVEAnalyse caller, int fsIndex) {
		this.caller = caller;
		this.fsIndex = fsIndex;
		//starte Thread
		this.start();
	}
	
	/**
	 * Vergleiche SOLL- und IST-Ergebnisdatensatz
	 * @param sollErgebnis SOLL-Datensatz
	 * @param istErgebnis IST-Datensatz
	 */
	public void vergleiche(Data sollErgebnis, Data istErgebnis) {
		this.sollErgebnis = sollErgebnis;
		this.istErgebnis = istErgebnis;
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
		//TODO:vergleiche
		caller.doNotify(fsIndex);
	}
	
	/**
	 * Lässt Prüfthread warten
	 *
	 */
	private void doWait() {
		synchronized(this) {
			try {
				this.wait();
			} catch (Exception e) {
				LOGGER.error("Error: Prüfer-Thread "+fsIndex+" (wait)");
			}
		}
	}
}
