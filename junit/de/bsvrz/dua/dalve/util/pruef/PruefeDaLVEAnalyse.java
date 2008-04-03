package de.bsvrz.dua.dalve.util.pruef;

import de.bsvrz.dav.daf.main.ClientDavInterface;
import de.bsvrz.dav.daf.main.ClientReceiverInterface;
import de.bsvrz.dav.daf.main.Data;
import de.bsvrz.dav.daf.main.DataDescription;
import de.bsvrz.dav.daf.main.ReceiveOptions;
import de.bsvrz.dav.daf.main.ReceiverRole;
import de.bsvrz.dav.daf.main.ResultData;
import de.bsvrz.dav.daf.main.config.SystemObject;
import de.bsvrz.dua.dalve.DatenaufbereitungLVETest;
import de.bsvrz.dua.dalve.util.TestErgebnisAnalyseImporter;
import de.bsvrz.sys.funclib.bitctrl.dua.DUAUtensilien;
import de.bsvrz.sys.funclib.debug.Debug;

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
	 * Initial CSV-Index
	 */
	private int csvIndex = 1;
	
	
	/**
	 * Initialisiert Prüferobjekt
	 * @param dav Datenverteilerverbindung
	 * @param FS Systemobjekt des Fahrstreifens
	 * @param csvQuelle Testdatenverzeichnis
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
		DD_KZDFS_EMPF = new DataDescription(this.dav.getDataModel().getAttributeGroup("atg.verkehrsDatenKurzZeitFs"), //$NON-NLS-1$
				  this.dav.getDataModel().getAspect("asp.analyse"), //$NON-NLS-1$
				  (short)0);

		dav.subscribeReceiver(this, FS, DD_KZDFS_EMPF, ReceiveOptions.normal(), ReceiverRole.receiver());
		
		/*
		 * Initialsiert Ergebnisimporter
		 */
		importAnaFS = new TestErgebnisAnalyseImporter(dav, csvQuelle);
		
		System.out.println("Prüferklasse initialisiert"); //$NON-NLS-1$
	}
	
	/**
	 * Importiert nächsten Ergebnisdatensatz und setzt Prüfzeitstempel
	 * @param pruefZeit Prüfzeitstempel
	 */
	public void naechsterDatensatz(long pruefZeit) {
		this.pruefZeit = pruefZeit;
		csvIndex++;
		importAnaFS.importNaechsteZeile();
		ergebnisFS1 = importAnaFS.getFSAnalyseDatensatz(1);
		ergebnisFS2 = importAnaFS.getFSAnalyseDatensatz(2);
		ergebnisFS3 = importAnaFS.getFSAnalyseDatensatz(3);
		
		pruefungFS1fertig = false;
		pruefungFS2fertig = false;
		pruefungFS3fertig = false;
		
		System.out.println("Prüferklasse parametriert -> Zeit: "+pruefZeit); //$NON-NLS-1$
	}
	
	/**
	 * Wird von den Prüferthreads getriggert und
	 * benachrichtigt, wenn die Prüfung aller 3 FS
	 * abgeschlossen ist, die Aufrufende Klasse
	 * @param FS Fahstreifenindex des Prüferthreads (1-3)
	 */
	public void doNotify(int FS) {
		System.out.println("Vergleich der Daten (FS"+FS+":Z"+csvIndex+") abgeschlossen");  //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$
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
			System.out.println("Alle FS geprüft. Benachrichtige Hauptthread..."); //$NON-NLS-1$
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
					if(result.getObject().getName().endsWith(".1")) { //$NON-NLS-1$
						System.out.println("Zu prüfendes Datum (FS1) empfangen. Vergleiche..."); //$NON-NLS-1$
						verglFS1.vergleiche(result.getData(),ergebnisFS1,csvIndex);
					} else if(result.getObject().getName().endsWith(".2")) { //$NON-NLS-1$
						System.out.println("Zu prüfendes Datum (FS2) empfangen. Vergleiche..."); //$NON-NLS-1$
						verglFS2.vergleiche(result.getData(),ergebnisFS2,csvIndex);
					} else if(result.getObject().getName().endsWith(".3")) { //$NON-NLS-1$
						System.out.println("Zu prüfendes Datum (FS3) empfangen. Vergleiche..."); //$NON-NLS-1$
						verglFS3.vergleiche(result.getData(),ergebnisFS3,csvIndex);
					}
				} catch(Exception e) {}
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
	 * Aktueller CSV-Index der SOLL- und IST-Daten
	 */
	private int csvIndex;
	
	/**
	 * Attributpfade der ATG
	 */
	private String[] attributNamenPraefix = {"qKfz", //$NON-NLS-1$
											 "qPkw", //$NON-NLS-1$
											 "qLkw", //$NON-NLS-1$
											 "vKfz", //$NON-NLS-1$
											 "vPkw", //$NON-NLS-1$
											 "vLkw", //$NON-NLS-1$
											 "vgKfz", //$NON-NLS-1$
											 "b", //$NON-NLS-1$
											 "sKfz", //$NON-NLS-1$
											 "aLkw", //$NON-NLS-1$
											 "kKfz", //$NON-NLS-1$
											 "kLkw", //$NON-NLS-1$
											 "kPkw", //$NON-NLS-1$
											 "qB",				 //$NON-NLS-1$							 
											 "kB"}; //$NON-NLS-1$
	
	/**
	 * Attributnamen
	 */
	private String[] attributNamen = {".Wert", //$NON-NLS-1$
									  ".Status.Erfassung.NichtErfasst", //$NON-NLS-1$
									  ".Status.PlFormal.WertMax", //$NON-NLS-1$
									  ".Status.PlFormal.WertMin", //$NON-NLS-1$
									  ".Status.PlLogisch.WertMaxLogisch", //$NON-NLS-1$
									  ".Status.PlLogisch.WertMinLogisch", //$NON-NLS-1$**/
									  ".Status.MessWertErsetzung.Implausibel", //$NON-NLS-1$
	  								  ".Status.MessWertErsetzung.Interpoliert", //$NON-NLS-1$
	  								  ".Güte.Index" }; //$NON-NLS-1$
	
	
	/**
	 * Initialisiert Prüferthread
	 * @param caller Aufrufende Klasse
	 * @param fsIndex Zu prüfender Fahrstreifen
	 */
	public VergleicheDaLVEAnalyse(PruefeDaLVEAnalyse caller, int fsIndex) {
		this.caller = caller;
		this.fsIndex = fsIndex;
		System.out.println("Prüfthread [PT] initialisiert (FS "+fsIndex+")"); //$NON-NLS-1$ //$NON-NLS-2$
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
		System.out.println("[PT"+fsIndex+"] Zu vergleichende Daten empfangen"); //$NON-NLS-1$ //$NON-NLS-2$
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
			System.out.println("[PT"+fsIndex+"] Warte auf Trigger"); //$NON-NLS-1$ //$NON-NLS-2$
			doWait();
			//vergleiche
			System.out.println("[PT"+fsIndex+"] Vergleiche Daten (Z "+csvIndex+")..."); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			doVergleich();
		}
	}
	
	/**
	 * Führt vergleich durch 
	 *
	 */
	private void doVergleich() {
		String loggerOut = "[PT"+fsIndex+"] Vergleichsergebnis des FS "+fsIndex+" Zeile "+csvIndex+"\n\r"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		String attributPfad = null;
		String csvDS = "[FS:"+fsIndex+"-Zeile:"+csvIndex+"]"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		long sollWert;
		long istWert;
		for(int i=0;i<attributNamenPraefix.length;i++) {
			for(int j=0;j<attributNamen.length;j++) {
				attributPfad = attributNamenPraefix[i] + attributNamen[j];
				sollWert = DUAUtensilien.getAttributDatum(attributPfad, sollErgebnis).asUnscaledValue().longValue();
				istWert = DUAUtensilien.getAttributDatum(attributPfad, istErgebnis).asUnscaledValue().longValue();
				
				boolean sollIstGleich = false;
				
				/**
				 * Toleranz gegenueber Rundungsfehlern in den Testdaten:
				 */
				if(attributNamen[j].endsWith("Index")){ //$NON-NLS-1$
					if(sollWert < 0){
						sollIstGleich = sollWert == istWert;	
					}else{
						sollIstGleich = Math.abs(sollWert - istWert) < 2;
					}
				}else if(attributNamenPraefix[i].startsWith("k") && attributNamen[j].endsWith("Wert")){  //$NON-NLS-1$//$NON-NLS-2$
					if(sollWert < 0){
						sollIstGleich = sollWert == istWert;	
					}else{
						sollIstGleich = Math.abs(sollWert - istWert) <= 1;
					}					
				}else{
					sollIstGleich = sollWert == istWert;
				}
				
				if(sollIstGleich) {
//					//System.out.println(csvDS+" OK : "+attributPfad+" -> "+sollWert+" (SOLL) == (IST) "+istWert);
//					loggerOut += csvDS+" OK : "+attributPfad+" -> "+sollWert+" (SOLL) == (IST) "+istWert + "\n"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
				} else {
//					System.out.println(csvDS +" ERR: "+attributPfad+" -> "+sollWert+" (SOLL) <> (IST) "+istWert + "\n"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
					loggerOut += csvDS+ " ERR: "+attributPfad+" -> "+sollWert+" (SOLL) <> (IST) "+istWert + "\n"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
				}
			}
		}
		System.out.println(loggerOut);
		System.out.println("[PT"+fsIndex+"] Prüfung FS "+fsIndex+" Zeile "+csvIndex+" abgeschlossen. Benachrichtige Prüfklasse..."); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		//Benachrichtige aufrufende Klasse und übermittle FS-Index(1-3) 
		caller.doNotify(fsIndex);
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
				System.out.println("Error: Pruefer-Thread " + fsIndex + " (wait)"); //$NON-NLS-1$ //$NON-NLS-2$
			}
		}
	}
}
