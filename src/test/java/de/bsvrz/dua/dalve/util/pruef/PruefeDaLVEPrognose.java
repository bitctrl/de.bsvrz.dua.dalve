/*
 * Segment 4 Datenübernahme und Aufbereitung (DUA), SWE 4.7 Datenaufbereitung LVE
 * Copyright (C) 2007-2015 BitCtrl Systems GmbH
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 51
 * Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 *
 * Contact Information:<br>
 * BitCtrl Systems GmbH<br>
 * Weißenfelser Straße 67<br>
 * 04229 Leipzig<br>
 * Phone: +49 341-490670<br>
 * mailto: info@bitctrl.de
 */
package de.bsvrz.dua.dalve.util.pruef;

import org.junit.Assert;

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
 * Prüft (Vergleicht) Analyse-Datensätze der Fahrstreifen.
 *
 * @author Görlitz
 */
public class PruefeDaLVEPrognose implements ClientReceiverInterface {

	/** Logger. */
	private static final Debug LOGGER = Debug.getLogger();

	/** Datenverteilerverbindung. */
	private ClientDavInterface dav = null;

	/** Empfangsdatenbeschreibung. */
	private DataDescription DD_KZDFS_PF_EMPF = null;

	/** The dd kzdfs pn empf. */
	private DataDescription DD_KZDFS_PN_EMPF = null;

	/** The dd kzdfs pt empf. */
	private DataDescription DD_KZDFS_PT_EMPF = null;

	/** The dd kzdfs gf empf. */
	private DataDescription DD_KZDFS_GF_EMPF = null;

	/** The dd kzdfs gn empf. */
	private DataDescription DD_KZDFS_GN_EMPF = null;

	/** The dd kzdfs gt empf. */
	private DataDescription DD_KZDFS_GT_EMPF = null;

	/** Ergbnisimporter für Prognosewerte der FS. */
	private final TestErgebnisPrognoseImporter importProgFS;

	/** Halten das aktuelle SOLL-Ergebnis der CSV-Datei. */
	private Data ergebnisFSProgFlink;

	/** The ergebnis fs prog normal. */
	private Data ergebnisFSProgNormal;

	/** The ergebnis fs prog traege. */
	private Data ergebnisFSProgTraege;

	/** The ergebnis fs glatt flink. */
	private Data ergebnisFSGlattFlink;

	/** The ergebnis fs glatt normal. */
	private Data ergebnisFSGlattNormal;

	/** The ergebnis fs glatt traege. */
	private Data ergebnisFSGlattTraege;

	/** Zeitstempel, auf den gewartet wird. */
	private long pruefZeit;

	/** Gibt den Prüfungsabschluss des jeweiligen FS an. */
	private boolean pruefungFS1PFlinkFertig = false;

	/** The pruefung f s1 p normal fertig. */
	private boolean pruefungFS1PNormalFertig = false;

	/** The pruefung f s1 p traege fertig. */
	private boolean pruefungFS1PTraegeFertig = false;

	/** The pruefung f s1 g flink fertig. */
	private boolean pruefungFS1GFlinkFertig = false;

	/** The pruefung f s1 g normal fertig. */
	private boolean pruefungFS1GNormalFertig = false;

	/** The pruefung f s1 g traege fertig. */
	private boolean pruefungFS1GTraegeFertig = false;

	/** Aufrufende Klasse. */
	protected DaLVETestPrognose caller;

	/** Initial CSV-Index. */
	private int csvIndex = 1;

	/** Datenmodi. */
	protected static final int MODE_PFLINK = 1;

	/** The Constant MODE_PNORMAL. */
	protected static final int MODE_PNORMAL = 2;

	/** The Constant MODE_PTRAEGE. */
	protected static final int MODE_PTRAEGE = 3;

	/** The Constant MODE_GFLINK. */
	protected static final int MODE_GFLINK = 4;

	/** The Constant MODE_GNORMAL. */
	protected static final int MODE_GNORMAL = 5;

	/** The Constant MODE_GTRAEGE. */
	protected static final int MODE_GTRAEGE = 6;

	/** Prüferthreads für alle Modi. */
	private final VergleicheDaLVEPrognose verglPFlink = new VergleicheDaLVEPrognose(this,
			MODE_PFLINK);

	/** The vergl p normal. */
	private final VergleicheDaLVEPrognose verglPNormal = new VergleicheDaLVEPrognose(this,
			MODE_PNORMAL);

	/** The vergl p traege. */
	private final VergleicheDaLVEPrognose verglPTraege = new VergleicheDaLVEPrognose(this,
			MODE_PTRAEGE);

	/** The vergl g flink. */
	private final VergleicheDaLVEPrognose verglGFlink = new VergleicheDaLVEPrognose(this,
			MODE_GFLINK);

	/** The vergl g normal. */
	private final VergleicheDaLVEPrognose verglGNormal = new VergleicheDaLVEPrognose(this,
			MODE_GNORMAL);

	/** The vergl g traege. */
	private final VergleicheDaLVEPrognose verglGTraege = new VergleicheDaLVEPrognose(this,
			MODE_GTRAEGE);

	/** Sollen Asserts genutzt werden. */
	protected boolean useAssert;

	/** Die erlaubte Abweichung zwischen erwartetem und berechnetem Prognosewert. */
	protected int ergebnisWertToleranz;

	/**
	 * Initialisiert Prüferobjekt.
	 *
	 * @param caller
	 *            the caller
	 * @param dav
	 *            Datenverteilerverbindung
	 * @param FS
	 *            Systemobjekt des Fahrstreifens
	 * @param csvQuelle
	 *            Testdatenverzeichnis
	 * @param useAsserts
	 *            the use asserts
	 * @throws Exception
	 *             the exception
	 */
	public PruefeDaLVEPrognose(final DaLVETestPrognose caller, final ClientDavInterface dav,
			final SystemObject[] FS, final String csvQuelle, final boolean useAsserts)
					throws Exception {
		this.dav = dav;
		this.caller = caller;
		useAssert = useAsserts;
		ergebnisWertToleranz = caller.getErgebnisWertToleranz();

		/*
		 * Empfängeranmeldung für Prognose und geglättete Werte
		 */
		final AttributeGroup atgFSPrognose = this.dav.getDataModel()
				.getAttributeGroup("atg.verkehrsDatenKurzZeitTrendExtraPolationFs");
		final AttributeGroup atgFSGeglaettet = this.dav.getDataModel()
				.getAttributeGroup("atg.verkehrsDatenKurzZeitGeglättetFs");

		DD_KZDFS_PF_EMPF = new DataDescription(atgFSPrognose,
				this.dav.getDataModel().getAspect("asp.prognoseFlink"));
		DD_KZDFS_PN_EMPF = new DataDescription(atgFSPrognose,
				this.dav.getDataModel().getAspect("asp.prognoseNormal"));
		DD_KZDFS_PT_EMPF = new DataDescription(atgFSPrognose,
				this.dav.getDataModel().getAspect("asp.prognoseTräge"));

		DD_KZDFS_GF_EMPF = new DataDescription(atgFSGeglaettet,
				this.dav.getDataModel().getAspect("asp.prognoseFlink"));
		DD_KZDFS_GN_EMPF = new DataDescription(atgFSGeglaettet,
				this.dav.getDataModel().getAspect("asp.prognoseNormal"));
		DD_KZDFS_GT_EMPF = new DataDescription(atgFSGeglaettet,
				this.dav.getDataModel().getAspect("asp.prognoseTräge"));

		dav.subscribeReceiver(this, FS, DD_KZDFS_PF_EMPF, ReceiveOptions.normal(),
				ReceiverRole.receiver());
		dav.subscribeReceiver(this, FS, DD_KZDFS_PN_EMPF, ReceiveOptions.normal(),
				ReceiverRole.receiver());
		dav.subscribeReceiver(this, FS, DD_KZDFS_PT_EMPF, ReceiveOptions.normal(),
				ReceiverRole.receiver());
		dav.subscribeReceiver(this, FS, DD_KZDFS_GF_EMPF, ReceiveOptions.normal(),
				ReceiverRole.receiver());
		dav.subscribeReceiver(this, FS, DD_KZDFS_GN_EMPF, ReceiveOptions.normal(),
				ReceiverRole.receiver());
		dav.subscribeReceiver(this, FS, DD_KZDFS_GT_EMPF, ReceiveOptions.normal(),
				ReceiverRole.receiver());

		/*
		 * Initialsiert Ergebnisimporter
		 */
		importProgFS = new TestErgebnisPrognoseImporter(dav, csvQuelle);

		System.out.println("Prüferklasse für Prognosewerte initialisiert");
	}

	/**
	 * Importiert nächsten Ergebnisdatensatz und setzt Prüfzeitstempel.
	 *
	 * @param pruefZeit
	 *            Prüfzeitstempel
	 */
	public void naechsterDatensatz(final long pruefZeit) {
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

		LOGGER.info("Prüferklasse für Prognosewerte parametriert -> Zeit: " + pruefZeit);
	}

	/**
	 * Wird von den Prüferthreads getriggert und benachrichtigt, wenn die Prüfung aller Daten
	 * abgeschlossen ist, die Aufrufende Klasse.
	 *
	 * @param mode
	 *            the mode
	 */
	public void doNotify(final int mode) {
		switch (mode) {
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
		if (pruefungFS1PFlinkFertig && pruefungFS1PNormalFertig && pruefungFS1PTraegeFertig
				&& pruefungFS1GFlinkFertig && pruefungFS1GNormalFertig
				&& pruefungFS1GTraegeFertig) {
			LOGGER.info("Alle Prognosedaten geprüft. Benachrichtige Hauptthread...");
			caller.doNotify(DaLVETestPrognose.ID_PRUEFER_PROGNOSE);
		}
	}

	/**
	 * Gibt einen repräsentativen Text zum übergebenen Modus zurück.
	 *
	 * @param mode
	 *            Der Modus
	 * @return Der repräsentativen Text
	 */
	public String getModusText(final int mode) {
		switch (mode) {
		case MODE_PFLINK:
			return ("Prognose Flink");
		case MODE_PNORMAL:
			return ("Prognose Normal");
		case MODE_PTRAEGE:
			return ("Prognose Träge");
		case MODE_GFLINK:
			return ("Geglättet Flink");
		case MODE_GNORMAL:
			return ("Geglättet Normal");
		case MODE_GTRAEGE:
			return ("Geglättet Träge");
		default:
			return null;
		}
	}

	@Override
	public void update(final ResultData[] results) {
		for (final ResultData result : results) {
			// Pruefe Ergebnisdatensatz auf Zeitstempel
			if ((result.getData() != null) && (result.getDataTime() == pruefZeit)) {

				try {
					// Ermittle Modus
					if (result.getDataDescription().equals(DD_KZDFS_PF_EMPF)) {
						verglPFlink.vergleiche(result.getData(), ergebnisFSProgFlink, csvIndex);
					} else if (result.getDataDescription().equals(DD_KZDFS_PN_EMPF)) {
						verglPNormal.vergleiche(result.getData(), ergebnisFSProgNormal, csvIndex);
					} else if (result.getDataDescription().equals(DD_KZDFS_PT_EMPF)) {
						verglPTraege.vergleiche(result.getData(), ergebnisFSProgTraege, csvIndex);
					} else if (result.getDataDescription().equals(DD_KZDFS_GF_EMPF)) {
						verglGFlink.vergleiche(result.getData(), ergebnisFSGlattFlink, csvIndex);
					} else if (result.getDataDescription().equals(DD_KZDFS_GN_EMPF)) {
						verglGNormal.vergleiche(result.getData(), ergebnisFSGlattNormal, csvIndex);
					} else if (result.getDataDescription().equals(DD_KZDFS_GT_EMPF)) {
						verglGTraege.vergleiche(result.getData(), ergebnisFSGlattTraege, csvIndex);
					}
				} catch (final Exception e) {
				}
			}
		}
	}
}

class VergleicheDaLVEPrognose extends Thread {

	/**
	 * Die Ident dieses Prüferthreads
	 */
	private final String ident;

	/**
	 * Logger
	 */
	private static final Debug LOGGER = Debug.getLogger();

	/**
	 * Aufrufende Klasse
	 */
	private final PruefeDaLVEPrognose caller;

	/**
	 * Modus
	 */
	private final int mode;

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
	 * Bemessungsverkehrsstärke qB der Analysetabelle. Unter dieser Voraussetzung ist die Berechnung
	 * richtig."
	 */
	private final String[] attributNamenPraefixP = { "qB", //$NON-NLS-1$
			"vKfz" }; //$NON-NLS-1$
	/**
	 * Attributpfade der ATG
	 *
	 * Kappich Mail vom 09.04.08:
	 *
	 * "der Algorithmus in der Prüfspezifikation verwendet nicht qKfz sondern die
	 * Bemessungsverkehrsstärke qB der Analysetabelle. Unter dieser Voraussetzung ist die Berechnung
	 * richtig."
	 */
	private final String[] attributNamenPraefixG = { "qB", //$NON-NLS-1$
			"vKfz", //$NON-NLS-1$
			"kB" }; //$NON-NLS-1$

	/**
	 * Die verwendeten Attributpfade der ATG
	 */
	private String[] attributNamenPraefix;

	/**
	 * Attributnamen
	 */
	private final String[] attributNamen = { ".Wert" }; //$NON-NLS-1$

	private String attPraefix;

	/**
	 * Initialisiert Prüferthread.
	 *
	 * @param caller
	 *            Aufrufende Klasse
	 * @param mode
	 *            der Prüfmodus
	 */
	public VergleicheDaLVEPrognose(final PruefeDaLVEPrognose caller, final int mode) {
		this.caller = caller;
		this.mode = mode;

		if (mode <= 3) {
			attPraefix = "P";
			attributNamenPraefix = attributNamenPraefixP;
		} else {
			attPraefix = "G";
			attributNamenPraefix = attributNamenPraefixG;
		}

		System.out.println("Prüfthread [PT] (" + caller.getModusText(mode) + ") initialisiert"); //$NON-NLS-1$ //$NON-NLS-2$
		ident = "[PT " + caller.getModusText(mode) + "]";

		// starte Thread
		start();
	}

	/**
	 * Vergleiche SOLL- und IST-Ergebnisdatensatz
	 *
	 * @param sollErgebnis
	 *            SOLL-Datensatz
	 * @param istErgebnis
	 *            IST-Datensatz
	 */
	public void vergleiche(final Data istErgebnis, final Data sollErgebnis, final int csvIndex) {
		this.sollErgebnis = sollErgebnis;
		this.istErgebnis = istErgebnis;
		this.csvIndex = csvIndex;

		synchronized (this) {
			// wecke Thread
			notify();
		}
	}

	/**
	 * Prüfthread
	 */
	@Override
	public void run() {
		// Thread läuft bis Programmende
		while (true) {
			// warte mit Prüfung bis geweckt
			doWait();

			// vergleiche
			doVergleich();
		}
	}

	/**
	 * Führt vergleich durch
	 *
	 */
	private void doVergleich() {
		String attributPfad = null;
		final String csvDS = ident + " (Z:" + csvIndex + ")"; //$NON-NLS-1$ //$NON-NLS-2$
		String loggerOut = csvDS + " Vergleichsergebnis:\n"; //$NON-NLS-1$
		int sollWert;
		int istWert;

		boolean isError = false;

		for (final String element : attributNamenPraefix) {
			for (final String element2 : attributNamen) {
				attributPfad = element + attPraefix + element2;
				sollWert = DUAUtensilien.getAttributDatum(attributPfad, sollErgebnis)
						.asUnscaledValue().intValue();
				istWert = DUAUtensilien.getAttributDatum(attributPfad, istErgebnis)
						.asUnscaledValue().intValue();
				if ((sollWert >= (istWert - caller.ergebnisWertToleranz))
						&& (sollWert <= (istWert + caller.ergebnisWertToleranz))) {
					loggerOut += "OK : " + attributPfad + " -> " + sollWert + " (SOLL) == (IST) " //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
							+ istWert + "\n"; //$NON-NLS-1$
					System.out.println("PROG: " + sollWert + "==" + istWert + " (" + attributPfad
							+ " --> " + caller.getModusText(mode) + ")");
				} else {
					isError = true;
					final String errOut = "ERR: " + attributPfad + " -> " + sollWert
							+ " (SOLL) <> (IST) " + istWert;
					loggerOut += errOut + "\n";

					if (caller.useAssert) {
						Assert.assertTrue(csvDS + " " + errOut, false);
					}
				}
			}
		}

		if (isError && !caller.useAssert) {
			System.out.println(loggerOut);
		}

		LOGGER.info(loggerOut);

		// Benachrichtige aufrufende Klasse und übermittle FS-Index(1-3)
		caller.doNotify(mode);
	}

	/**
	 * Lässt Prüfthread warten
	 *
	 */
	private void doWait() {
		synchronized (this) {
			try {
				this.wait();
			} catch (final Exception e) {
				System.out.println("Error: " + ident + " (wait)");
			}
		}
	}
}
