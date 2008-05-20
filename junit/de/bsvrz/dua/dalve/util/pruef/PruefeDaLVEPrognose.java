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
 * Pr�ft (Vergleicht) Analyse-Datens�tze der Fahrstreifen
 * 
 * @author G�rlitz
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
	 * Ergbnisimporter f�r Prognosewerte der FS
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
	 * Gibt den Pr�fungsabschluss des jeweiligen FS an
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
	 * Pr�ferthreads f�r alle Modi
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
	 * Die erlaubte Abweichung zwischen erwartetem und berechnetem Prognosewert
	 */
	protected int ergebnisWertToleranz;
	
	
	/**
	 * Initialisiert Pr�ferobjekt
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
		this.ergebnisWertToleranz = caller.getErgebnisWertToleranz();
		
		/*
		 * Empf�ngeranmeldung f�r Prognose und gegl�ttete Werte
		 */
		AttributeGroup atgFSPrognose = this.dav.getDataModel().getAttributeGroup("atg.verkehrsDatenKurzZeitTrendExtraPolationFs");
		AttributeGroup atgFSGeglaettet = this.dav.getDataModel().getAttributeGroup("atg.verkehrsDatenKurzZeitGegl�ttetFs");
		
		DD_KZDFS_PF_EMPF = new DataDescription(atgFSPrognose, this.dav.getDataModel().getAspect("asp.prognoseFlink"), (short)0);
		DD_KZDFS_PN_EMPF = new DataDescription(atgFSPrognose, this.dav.getDataModel().getAspect("asp.prognoseNormal"), (short)0);
		DD_KZDFS_PT_EMPF = new DataDescription(atgFSPrognose, this.dav.getDataModel().getAspect("asp.prognoseTr�ge"), (short)0);

		DD_KZDFS_GF_EMPF = new DataDescription(atgFSGeglaettet, this.dav.getDataModel().getAspect("asp.prognoseFlink"), (short)0);
		DD_KZDFS_GN_EMPF = new DataDescription(atgFSGeglaettet, this.dav.getDataModel().getAspect("asp.prognoseNormal"), (short)0);
		DD_KZDFS_GT_EMPF = new DataDescription(atgFSGeglaettet, this.dav.getDataModel().getAspect("asp.prognoseTr�ge"), (short)0);
		
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
		
		System.out.println("Pr�ferklasse f�r Prognosewerte initialisiert");
	}
	
	/**
	 * Importiert n�chsten Ergebnisdatensatz und setzt Pr�fzeitstempel
	 * @param pruefZeit Pr�fzeitstempel
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
		
		LOGGER.info("Pr�ferklasse f�r Prognosewerte parametriert -> Zeit: "+pruefZeit);
	}
	
	/**
	 * Wird von den Pr�ferthreads getriggert und
	 * benachrichtigt, wenn die Pr�fung aller Daten
	 * abgeschlossen ist, die Aufrufende Klasse
	 * @param FS Fahstreifenindex des Pr�ferthreads (1-3)
	 */
	public void doNotify(int mode) {
		switch(mode) {
			case MODE_PFLINK: {
				pruefungFS1PFlinkFertig = true;
				break;
			}
			case MODE_PNORMAL: {
				pruefungFS1PNormalFertig = true;
				break;
			}
			case MODE_PTRAEGE: {
				pruefungFS1PTraegeFertig = true;
				break;
			}
			case MODE_GFLINK: {
				pruefungFS1GFlinkFertig = true;
				break;
			}
			case MODE_GNORMAL: {
				pruefungFS1GNormalFertig = true;
				break;
			}
			case MODE_GTRAEGE: {
				pruefungFS1GTraegeFertig = true;
				break;
			}
		}
		if(pruefungFS1PFlinkFertig && pruefungFS1PNormalFertig && pruefungFS1PTraegeFertig &&
				pruefungFS1GFlinkFertig && pruefungFS1GNormalFertig && pruefungFS1GTraegeFertig) {
			LOGGER.info("Alle Prognosedaten gepr�ft. Benachrichtige Hauptthread...");
			caller.doNotify(caller.ID_PRUEFER_PROGNOSE);
		}
	}
	
	/**
	 * Gibt einen repr�sentativen Text zum �bergebenen Modus zur�ck
	 * @param mode Der Modus
	 * @return Der repr�sentativen Text
	 */
	public String getModusText(final int mode) {
		switch(mode) {
			case MODE_PFLINK: return("Prognose Flink");
			case MODE_PNORMAL: return("Prognose Normal");
			case MODE_PTRAEGE: return("Prognose Tr�ge");
			case MODE_GFLINK: return("Gegl�ttet Flink");
			case MODE_GNORMAL: return("Gegl�ttet Normal");
			case MODE_GTRAEGE: return("Gegl�ttet Tr�ge");
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
						verglPFlink.vergleiche(result.getData(),ergebnisFSProgFlink,csvIndex);
					} else if(result.getDataDescription().equals(DD_KZDFS_PN_EMPF)) {
						verglPNormal.vergleiche(result.getData(),ergebnisFSProgNormal,csvIndex);
					} else if(result.getDataDescription().equals(DD_KZDFS_PT_EMPF)) {
						verglPTraege.vergleiche(result.getData(),ergebnisFSProgTraege,csvIndex);
					} else if(result.getDataDescription().equals(DD_KZDFS_GF_EMPF)) {
						verglGFlink.vergleiche(result.getData(),ergebnisFSGlattFlink,csvIndex);
					} else if(result.getDataDescription().equals(DD_KZDFS_GN_EMPF)) {
						verglGNormal.vergleiche(result.getData(),ergebnisFSGlattNormal,csvIndex);
					} else if(result.getDataDescription().equals(DD_KZDFS_GT_EMPF)) {
						verglGTraege.vergleiche(result.getData(),ergebnisFSGlattTraege,csvIndex);
					}
				} catch(Exception e) {}
			}
		}		
	}
}

class VergleicheDaLVEPrognose extends Thread {
	
	/**
	 * Die Ident dieses Pr�ferthreads
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
	 * "der Algorithmus in der Pr�fspezifikation verwendet nicht qKfz sondern die
	 * Bemessungsverkehrsst�rke qB der Analysetabelle. Unter dieser Voraussetzung ist die
	 * Berechnung richtig."
	 */
	private String[] attributNamenPraefixP = {"qB", //$NON-NLS-1$
											 "vKfz"}; //$NON-NLS-1$
	/**
	 * Attributpfade der ATG
	 * 
	 * Kappich Mail vom 09.04.08:
	 * 
	 * "der Algorithmus in der Pr�fspezifikation verwendet nicht qKfz sondern die
	 * Bemessungsverkehrsst�rke qB der Analysetabelle. Unter dieser Voraussetzung ist die
	 * Berechnung richtig."
	 */
	private String[] attributNamenPraefixG = {"qB", //$NON-NLS-1$
											 "vKfz", //$NON-NLS-1$
											 "kB"}; //$NON-NLS-1$
	
	/**
	 * 	Die verwendeten Attributpfade der ATG
	 */
	private String[] attributNamenPraefix;
	
	/**
	 * Attributnamen
	 */
	private String[] attributNamen = {".Wert" }; //$NON-NLS-1$
	
	private String attPraefix; 
	
	
	/**
	 * Initialisiert Pr�ferthread
	 * @param caller Aufrufende Klasse
	 * @param fsIndex Zu pr�fender Fahrstreifen
	 */
	public VergleicheDaLVEPrognose(PruefeDaLVEPrognose caller, int mode) {
		this.caller = caller;
		this.mode = mode;
		
		if(mode <= 3) {
			this.attPraefix = "P";
			this.attributNamenPraefix = attributNamenPraefixP;
		} else {
			this.attPraefix = "G";
			this.attributNamenPraefix = attributNamenPraefixG;
		}
		
		System.out.println("Pr�fthread [PT] ("+caller.getModusText(mode)+") initialisiert"); //$NON-NLS-1$ //$NON-NLS-2$
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

		synchronized(this) {
			//wecke Thread
			this.notify();
		}
	}
	
	/**
	 * Pr�fthread
	 */
	public void run() {
		//Thread l�uft bis Programmende
		while(true) {
			//warte mit Pr�fung bis geweckt
			doWait();
			
			//vergleiche
			doVergleich();
		}
	}
	
	/**
	 * F�hrt vergleich durch 
	 *
	 */
	private void doVergleich() {
		String attributPfad = null;
		String csvDS = ident+" (Z:"+csvIndex+")"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		String loggerOut = csvDS+" Vergleichsergebnis:\n"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		int sollWert;
		int istWert;
		
		boolean isError = false;
		
		for(int i=0;i<attributNamenPraefix.length;i++) {
			for(int j=0;j<attributNamen.length;j++) {
				attributPfad = attributNamenPraefix[i] + attPraefix + attributNamen[j];
				sollWert = DUAUtensilien.getAttributDatum(attributPfad, sollErgebnis).asUnscaledValue().intValue();
				istWert = DUAUtensilien.getAttributDatum(attributPfad, istErgebnis).asUnscaledValue().intValue();
				if(sollWert >= (istWert - caller.ergebnisWertToleranz) &&
						sollWert <= (istWert + caller.ergebnisWertToleranz)) {
					loggerOut += "OK : "+attributPfad+" -> "+sollWert+" (SOLL) == (IST) "+istWert + "\n"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
				} else {
					isError = true;
					String errOut = "ERR: "+attributPfad+" -> "+sollWert+" (SOLL) <> (IST) "+istWert;
					loggerOut += errOut + "\n";
					
					if(caller.useAssert) {
						Assert.assertTrue(csvDS+" "+errOut, false);
					}
				}
			}
		}

		if(isError && !caller.useAssert) {
			System.out.println(loggerOut);						
		}
		
		LOGGER.info(loggerOut);
		
		//Benachrichtige aufrufende Klasse und �bermittle FS-Index(1-3) 
		caller.doNotify(mode);
	}
	
	/**
	 * L�sst Pr�fthread warten
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
