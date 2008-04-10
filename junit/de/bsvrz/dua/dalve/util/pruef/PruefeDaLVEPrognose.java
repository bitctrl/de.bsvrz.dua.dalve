package de.bsvrz.dua.dalve.util.pruef;

import junit.framework.Assert;
import de.bsvrz.dav.daf.main.ClientDavInterface;
import de.bsvrz.dav.daf.main.ClientReceiverInterface;
import de.bsvrz.dav.daf.main.Data;
import de.bsvrz.dav.daf.main.DataDescription;
import de.bsvrz.dav.daf.main.ReceiveOptions;
import de.bsvrz.dav.daf.main.ReceiverRole;
import de.bsvrz.dav.daf.main.ResultData;
import de.bsvrz.dav.daf.main.config.AttributeGroup;
import de.bsvrz.dav.daf.main.config.SystemObject;
import de.bsvrz.dua.dalve.DaLVETestPrognose;
import de.bsvrz.dua.dalve.util.TestErgebnisPrognoseImporter;
import de.bsvrz.sys.funclib.bitctrl.dua.DUAUtensilien;
import de.bsvrz.sys.funclib.debug.Debug;

/**
 * Prüft (Vergleicht) Analyse-Datensätze der Fahrstreifen
 * 
 * @author Görlitz
 *
 */
public class PruefeDaLVEPrognose
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
	private DataDescription DD_KZDFS_PF_EMPF = null;
	private DataDescription DD_KZDFS_PN_EMPF = null;
	private DataDescription DD_KZDFS_PT_EMPF = null;
	private DataDescription DD_KZDFS_GF_EMPF = null;
	private DataDescription DD_KZDFS_GN_EMPF = null;
	private DataDescription DD_KZDFS_GT_EMPF = null;
	
	/**
	 * Ergbnisimporter für Prognosewerte der FS
	 */
	private TestErgebnisPrognoseImporter importProgFS;
	
	/**
	 * Halten das aktuelle SOLL-Ergebnis der CSV-Datei
	 */
	private Data ergebnisFSProgFlink;
	private Data ergebnisFSProgNormal;
	private Data ergebnisFSProgTraege;
	private Data ergebnisFSGlattFlink;
	private Data ergebnisFSGlattNormal;
	private Data ergebnisFSGlattTraege;
	
	/**
	 * Zeitstempel, auf den gewartet wird
	 */
	private long pruefZeit;
	
	/**
	 * Gibt den Prüfungsabschluss des jeweiligen FS an
	 */
	private boolean pruefungFS1PFlinkFertig = false;
	private boolean pruefungFS1PNormalFertig = false;
	private boolean pruefungFS1PTraegeFertig = false;
	private boolean pruefungFS1GFlinkFertig = false;
	private boolean pruefungFS1GNormalFertig = false;
	private boolean pruefungFS1GTraegeFertig = false;
	
	/**
	 * Aufrufende Klasse
	 */
	protected DaLVETestPrognose caller;
	
	/**
	 * Initial CSV-Index
	 */
	private int csvIndex = 1;
	
	/**
	 * Datenmodi
	 */
	protected static final int MODE_PFLINK = 1;
	protected static final int MODE_PNORMAL = 2;
	protected static final int MODE_PTRAEGE = 3;
	protected static final int MODE_GFLINK = 4;
	protected static final int MODE_GNORMAL = 5;
	protected static final int MODE_GTRAEGE = 6;
	
	/**
	 * Prüferthreads für alle Modi
	 */
	private VergleicheDaLVEPrognose verglPFlink = new VergleicheDaLVEPrognose(this, MODE_PFLINK);
	private VergleicheDaLVEPrognose verglPNormal = new VergleicheDaLVEPrognose(this, MODE_PNORMAL);
	private VergleicheDaLVEPrognose verglPTraege = new VergleicheDaLVEPrognose(this, MODE_PTRAEGE);
	private VergleicheDaLVEPrognose verglGFlink = new VergleicheDaLVEPrognose(this, MODE_GFLINK);
	private VergleicheDaLVEPrognose verglGNormal = new VergleicheDaLVEPrognose(this, MODE_GNORMAL);
	private VergleicheDaLVEPrognose verglGTraege = new VergleicheDaLVEPrognose(this, MODE_GTRAEGE);
	
	/**
	 * Sollen Asserts genutzt werden
	 */
	protected boolean useAssert;
	
	
	/**
	 * Initialisiert Prüferobjekt
	 * @param dav Datenverteilerverbindung
	 * @param FS Systemobjekt des Fahrstreifens
	 * @param csvQuelle Testdatenverzeichnis
	 * @throws Exception
	 */
	public PruefeDaLVEPrognose(DaLVETestPrognose caller, ClientDavInterface dav,
							  SystemObject[] FS, String csvQuelle, boolean useAsserts)
	throws Exception {
		this.dav = dav;
		this.caller = caller;
		this.useAssert = useAsserts;
		
		/*
		 * Empfängeranmeldung für Prognose und geglättete Werte
		 */
		AttributeGroup atgFSPrognose = this.dav.getDataModel().getAttributeGroup("atg.verkehrsDatenKurzZeitTrendExtraPolationFs");
		AttributeGroup atgFSGeglaettet = this.dav.getDataModel().getAttributeGroup("atg.verkehrsDatenKurzZeitGeglättetFs");
		
		DD_KZDFS_PF_EMPF = new DataDescription(atgFSPrognose, this.dav.getDataModel().getAspect("asp.prognoseFlink"), (short)0);
		DD_KZDFS_PN_EMPF = new DataDescription(atgFSPrognose, this.dav.getDataModel().getAspect("asp.prognoseNormal"), (short)0);
		DD_KZDFS_PT_EMPF = new DataDescription(atgFSPrognose, this.dav.getDataModel().getAspect("asp.prognoseTräge"), (short)0);

		DD_KZDFS_GF_EMPF = new DataDescription(atgFSGeglaettet, this.dav.getDataModel().getAspect("asp.prognoseFlink"), (short)0);
		DD_KZDFS_GN_EMPF = new DataDescription(atgFSGeglaettet, this.dav.getDataModel().getAspect("asp.prognoseNormal"), (short)0);
		DD_KZDFS_GT_EMPF = new DataDescription(atgFSGeglaettet, this.dav.getDataModel().getAspect("asp.prognoseTräge"), (short)0);
		
		dav.subscribeReceiver(this, FS, DD_KZDFS_PF_EMPF, ReceiveOptions.normal(), ReceiverRole.receiver());
		dav.subscribeReceiver(this, FS, DD_KZDFS_PN_EMPF, ReceiveOptions.normal(), ReceiverRole.receiver());
		dav.subscribeReceiver(this, FS, DD_KZDFS_PT_EMPF, ReceiveOptions.normal(), ReceiverRole.receiver());
		dav.subscribeReceiver(this, FS, DD_KZDFS_GF_EMPF, ReceiveOptions.normal(), ReceiverRole.receiver());
		dav.subscribeReceiver(this, FS, DD_KZDFS_GN_EMPF, ReceiveOptions.normal(), ReceiverRole.receiver());
		dav.subscribeReceiver(this, FS, DD_KZDFS_GT_EMPF, ReceiveOptions.normal(), ReceiverRole.receiver());
		
		/*
		 * Initialsiert Ergebnisimporter
		 */
		importProgFS = new TestErgebnisPrognoseImporter(dav, csvQuelle);
		
		System.out.println("Prüferklasse initialisiert");
	}
	
	/**
	 * Importiert nächsten Ergebnisdatensatz und setzt Prüfzeitstempel
	 * @param pruefZeit Prüfzeitstempel
	 */
	public void naechsterDatensatz(long pruefZeit) {
		this.pruefZeit = pruefZeit;
		csvIndex++;
		importProgFS.importNaechsteZeile();
		ergebnisFSProgFlink = importProgFS.getFSPrognoseFlinkDatensatz();
		ergebnisFSProgNormal = importProgFS.getFSPrognoseNormalDatensatz();
		ergebnisFSProgTraege = importProgFS.getFSPrognoseTraegeDatensatz();
		ergebnisFSGlattFlink = importProgFS.getFSGeglaettetFlinkDatensatz();
		ergebnisFSGlattNormal = importProgFS.getFSGeglaettetNormalDatensatz();
		ergebnisFSGlattTraege = importProgFS.getFSGeglaettetTraegeDatensatz();
		
		pruefungFS1PFlinkFertig = false;
		pruefungFS1PNormalFertig = false;
		pruefungFS1PTraegeFertig = false;
		pruefungFS1GFlinkFertig = false;
		pruefungFS1GNormalFertig = false;
		pruefungFS1GTraegeFertig = false;
		
		//System.out.println("Prüferklasse parametriert -> Zeit: "+pruefZeit);
		LOGGER.info("Prüferklasse parametriert -> Zeit: "+pruefZeit);
	}
	
	/**
	 * Wird von den Prüferthreads getriggert und
	 * benachrichtigt, wenn die Prüfung aller Daten
	 * abgeschlossen ist, die Aufrufende Klasse
	 * @param FS Fahstreifenindex des Prüferthreads (1-3)
	 */
	public void doNotify(int mode) {
		switch(mode) {
			case MODE_PFLINK: {
				//System.out.println("Vergleich der Prognosedaten (Flink:Z"+csvIndex+") abgeschlossen");
				pruefungFS1PFlinkFertig = true;
				break;
			}
			case MODE_PNORMAL: {
				//System.out.println("Vergleich der Prognosedaten (Normal:Z"+csvIndex+") abgeschlossen");
				pruefungFS1PNormalFertig = true;
				break;
			}
			case MODE_PTRAEGE: {
				//System.out.println("Vergleich der Prognosedaten (Träge:Z"+csvIndex+") abgeschlossen");
				pruefungFS1PTraegeFertig = true;
				break;
			}
			case MODE_GFLINK: {
				//System.out.println("Vergleich der geglätteten Daten (Flink:Z"+csvIndex+") abgeschlossen");
				pruefungFS1GFlinkFertig = true;
				break;
			}
			case MODE_GNORMAL: {
				//System.out.println("Vergleich der geglätteten Daten (Normal:Z"+csvIndex+") abgeschlossen");
				pruefungFS1GNormalFertig = true;
				break;
			}
			case MODE_GTRAEGE: {
				//System.out.println("Vergleich der geglätteten Daten (Träge:Z"+csvIndex+") abgeschlossen");
				pruefungFS1GTraegeFertig = true;
				break;
			}
		}
		if(pruefungFS1PFlinkFertig && pruefungFS1PNormalFertig && pruefungFS1PTraegeFertig &&
				pruefungFS1GFlinkFertig && pruefungFS1GNormalFertig && pruefungFS1GTraegeFertig) {
			//System.out.println("Alle Daten geprüft. Benachrichtige Hauptthread...");
			LOGGER.info("Alle Daten geprüft. Benachrichtige Hauptthread...");
			caller.doNotify();
		}
	}
	
	/**
	 * Gibt einen repräsentativen Text zum übergebenen Modus zurück
	 * @param mode Der Modus
	 * @return Der repräsentativen Text
	 */
	public String getModusText(final int mode) {
		switch(mode) {
			case MODE_PFLINK: return("Prognose Flink");
			case MODE_PNORMAL: return("Prognose Normal");
			case MODE_PTRAEGE: return("Prognose Träge");
			case MODE_GFLINK: return("Geglättet Flink");
			case MODE_GNORMAL: return("Geglättet Normal");
			case MODE_GTRAEGE: return("Geglättet Träge");
			default: return null;
		}
	}
	
	/**
	 * {@inheritDoc}
	 */
	public void update(ResultData[] results) {
		for (ResultData result : results) {
			//Pruefe Ergebnisdatensatz auf Zeitstempel
			if (result.getData() != null &&	result.getDataTime() == pruefZeit) {

				try {
					//Ermittle Modus
					if(result.getDataDescription().equals(DD_KZDFS_PF_EMPF)) {
						//System.out.println("Zu prüfendes Prognosedatum (Flink) empfangen. Vergleiche...");
						verglPFlink.vergleiche(result.getData(),ergebnisFSProgFlink,csvIndex);
					} else if(result.getDataDescription().equals(DD_KZDFS_PN_EMPF)) {
						//System.out.println("Zu prüfendes Prognosedatum (Normal) empfangen. Vergleiche...");
						verglPNormal.vergleiche(result.getData(),ergebnisFSProgNormal,csvIndex);
					} else if(result.getDataDescription().equals(DD_KZDFS_PT_EMPF)) {
						//System.out.println("Zu prüfendes Prognosedatum (Träge) empfangen. Vergleiche...");
						verglPTraege.vergleiche(result.getData(),ergebnisFSProgTraege,csvIndex);
					} else if(result.getDataDescription().equals(DD_KZDFS_GF_EMPF)) {
						//System.out.println("Zu prüfendes geglättetes Datum (Flink) empfangen. Vergleiche...");
						verglGFlink.vergleiche(result.getData(),ergebnisFSGlattFlink,csvIndex);
					} else if(result.getDataDescription().equals(DD_KZDFS_GN_EMPF)) {
						//System.out.println("Zu prüfendes geglättetes Datum (Normal) empfangen. Vergleiche...");
						verglGNormal.vergleiche(result.getData(),ergebnisFSGlattNormal,csvIndex);
					} else if(result.getDataDescription().equals(DD_KZDFS_GT_EMPF)) {
						//System.out.println("Zu prüfendes geglättetes Datum (Träge) empfangen. Vergleiche...");
						verglGTraege.vergleiche(result.getData(),ergebnisFSGlattTraege,csvIndex);
					}
				} catch(Exception e) {}
			}
		}		
	}
}

class VergleicheDaLVEPrognose extends Thread {
	
	/**
	 * Die Ident dieses Prüferthreads
	 */
	private String ident;
	
	/**
	 * Logger
	 */
	protected Debug LOGGER = Debug.getLogger();
	
	/**
	 * Aufrufende Klasse
	 */
	private PruefeDaLVEPrognose caller; 
	
	/**
	 * Modus
	 */
	private int mode;
	
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
	 * 
	 * Kappich Mail vom 09.04.08:
	 * 
	 * "der Algorithmus in der Prüfspezifikation verwendet nicht qKfz sondern die
	 * Bemessungsverkehrsstärke qB der Analysetabelle. Unter dieser Voraussetzung ist die
	 * Berechnung richtig."
	 */
	private String[] attributNamenPraefix = {"qB", //$NON-NLS-1$
											 "vPkw"}; //$NON-NLS-1$
	
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
	
	private String attPraefix; 
	
	
	/**
	 * Initialisiert Prüferthread
	 * @param caller Aufrufende Klasse
	 * @param fsIndex Zu prüfender Fahrstreifen
	 */
	public VergleicheDaLVEPrognose(PruefeDaLVEPrognose caller, int mode) {
		this.caller = caller;
		this.mode = mode;
		
		if(mode <= 3) {
			this.attPraefix = "P";
		} else {
			this.attPraefix = "G";
		}
		
		System.out.println("Prüfthread [PT] initialisiert ("+caller.getModusText(mode)+")"); //$NON-NLS-1$ //$NON-NLS-2$
		this.ident = "[PT "+caller.getModusText(mode)+"]";
		
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
//		System.out.println(ident+" Zu vergleichende Daten empfangen"); //$NON-NLS-1$ //$NON-NLS-2$
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
			//warte mit Prüfung bis geweckt
			//System.out.println(ident+" Warte auf Trigger"); //$NON-NLS-1$ //$NON-NLS-2$
			doWait();
			
			//vergleiche
			//System.out.println(ident+" Vergleiche Daten (Z "+csvIndex+")..."); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			LOGGER.info(ident+" Vergleiche Daten (Z "+csvIndex+")..."); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			doVergleich();
		}
	}
	
	/**
	 * Führt vergleich durch 
	 *
	 */
	private void doVergleich() {
		String attributPfad = null;
		String csvDS = ident+" (Z:"+csvIndex+")"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		String loggerOut = csvDS+" Vergleichsergebnis:\n\n"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		int sollWert;
		int istWert;
		
		for(int i=0;i<attributNamenPraefix.length;i++) {
			for(int j=0;j<attributNamen.length;j++) {
				attributPfad = attributNamenPraefix[i] + attPraefix + attributNamen[j];
				sollWert = DUAUtensilien.getAttributDatum(attributPfad, sollErgebnis).asUnscaledValue().intValue();
				istWert = DUAUtensilien.getAttributDatum(attributPfad, istErgebnis).asUnscaledValue().intValue();
				if(sollWert == istWert) {
					loggerOut += "OK : "+attributPfad+" -> "+sollWert+" (SOLL) == (IST) "+istWert + "\n"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
				} else {
					String errOut = "ERR: "+attributPfad+" -> "+sollWert+" (SOLL) <> (IST) "+istWert;
					loggerOut += errOut + "\n";
					
					System.out.println(csvDS+" "+errOut);
					
					if(caller.useAssert) {
						Assert.assertTrue(csvDS+" "+errOut, false);
					}
				}
			}
		}

		LOGGER.info(loggerOut);
		
		//System.out.println(csvDS+" Prüfung abgeschlossen.");
		LOGGER.info(csvDS+" Prüfung abgeschlossen.");
		
		
		//Benachrichtige aufrufende Klasse und übermittle FS-Index(1-3) 
		caller.doNotify(mode);
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
				System.out.println("Error: "+ident+" (wait)");
			}
		}
	}
}
