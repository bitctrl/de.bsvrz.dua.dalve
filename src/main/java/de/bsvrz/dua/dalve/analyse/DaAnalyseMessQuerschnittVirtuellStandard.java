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
package de.bsvrz.dua.dalve.analyse;

import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import de.bsvrz.dav.daf.main.Data;
import de.bsvrz.dav.daf.main.DataDescription;
import de.bsvrz.dav.daf.main.ReceiveOptions;
import de.bsvrz.dav.daf.main.ReceiverRole;
import de.bsvrz.dav.daf.main.ResultData;
import de.bsvrz.dav.daf.main.config.SystemObject;
import de.bsvrz.dua.dalve.ErfassungsIntervallDauerMQ;
import de.bsvrz.sys.funclib.bitctrl.dua.DUAInitialisierungsException;
import de.bsvrz.sys.funclib.bitctrl.dua.DUAKonstanten;
import de.bsvrz.sys.funclib.bitctrl.dua.MesswertUnskaliert;
import de.bsvrz.sys.funclib.bitctrl.dua.ObjektWecker;
import de.bsvrz.sys.funclib.bitctrl.dua.lve.MessQuerschnitt;
import de.bsvrz.sys.funclib.bitctrl.dua.lve.MessQuerschnittVirtuell;
import de.bsvrz.sys.funclib.bitctrl.dua.lve.typen.MessQuerschnittVirtuellLage;
import de.bsvrz.sys.funclib.bitctrl.dua.schnittstellen.IObjektWeckerListener;
import de.bsvrz.sys.funclib.debug.Debug;

// TODO: Auto-generated Javadoc
/**
 * In diesem Objekt werden alle aktuellen Werte die zur Berechnung der Analysewerte eines virtuellen
 * Messquerschnitts notwendig sind gespeichert. Wenn die Werte für ein bestimmtes Intervall bereit
 * stehen (oder eine Timeout abgelaufen ist), wird eine Berechnung durchgeführt und der Wert
 * publiziert. <br>
 * <b>Achtung: Verfahren auf Basis der Konfigurationsdaten aus Attributgruppe
 * <code>atg.messQuerschnittVirtuellStandard</code>.</b><br>
 *
 * @author BitCtrl Systems GmbH, Thierfelder
 *
 * @version $Id$
 */
public class DaAnalyseMessQuerschnittVirtuellStandard extends DaAnalyseMessQuerschnitt implements
		IObjektWeckerListener {

	private static final Debug LOGGER = Debug.getLogger();

	/**
	 * Informiert dieses Objekt darüber, dass das Timeout für die Berechnung der Analysedaten
	 * abgelaufen ist.
	 */
	private static final ObjektWecker WECKER = new ObjektWecker();

	/**
	 * Mapt alle hier betrachteten Messquerschnitte auf das letzte von ihnen empfangene
	 * Analysedatum.
	 */
	private final Map<SystemObject, ResultData> aktuelleMQAnalysen = new HashMap<SystemObject, ResultData>();

	/**
	 * Alle MQ, die auf der Hauptfahrbahn liegen.
	 */
	private final Collection<SystemObject> mqAufHauptfahrbahn = new HashSet<SystemObject>();

	/**
	 * der aufgelößte virtuelle Messquerschnitt.
	 */
	private MessQuerschnittVirtuell mqv = null;

	/**
	 * Zeigt an, ob der Messquerschnitt <b>vor</b> der Anschlussstelle, die durch diesen virtuellen
	 * MQ repräsentiert wird, direkt erfasst ist.
	 */
	private boolean mqVorErfasst = true;

	/**
	 * Zeigt an, ob der Messquerschnitt <b>nach</b> der Anschlussstelle, die durch diesen virtuellen
	 * MQ repräsentiert wird, direkt erfasst ist.
	 */
	private boolean mqNachErfasst = true;

	/**
	 * Zeigt an, ob der Messquerschnitt <b>mittig</b> der Anschlussstelle, die durch diesen
	 * virtuellen MQ repräsentiert wird, direkt erfasst ist.
	 */
	private boolean mqMitteErfasst = true;

	/**
	 * Zeigt an, ob der Messquerschnitt <b>Einfahrt</b> der Anschlussstelle, die durch diesen
	 * virtuellen MQ repräsentiert wird, direkt erfasst ist.
	 */
	private boolean mqEinfahrtErfasst = true;

	/**
	 * Zeigt an, ob der Messquerschnitt <b>Ausfahrt</b> der Anschlussstelle, die durch diesen
	 * virtuellen MQ repräsentiert wird, direkt erfasst ist.
	 */
	private boolean mqAusfahrtErfasst = true;

	/**
	 * Tracker fuer die Erfassungsintervalldauer des MQ.
	 */
	private ErfassungsIntervallDauerMQ mqT = null;

	/**
	 * Die zu berechnende Lage.
	 */
	private MessQuerschnittVirtuellLage lage = null;

	/**
	 * Initialisiert dieses Objekt und gibt die initialisierte Instanz zurück. Nach dieser
	 * Initialisierung ist das Objekt auf alle Daten (seiner assoziierten Messquerschnitte)
	 * angemeldet und analysiert ggf. Daten
	 *
	 * @param analyseModul
	 *            Verbindung zum Analysemodul (zum Publizieren)
	 * @param messQuerschnittVirtuell
	 *            der virtuelle Messquerschnitt
	 * @return die initialisierte Instanz dieses Objekts
	 * @throws DUAInitialisierungsException
	 *             wenn die Konfigurationsdaten des virtuellen MQs nicht vollständig ausgelesen
	 *             werden konnte
	 */
	@Override
	public DaAnalyseMessQuerschnittVirtuellStandard initialisiere(
			final MqAnalyseModul analyseModul, final SystemObject messQuerschnittVirtuell)
					throws DUAInitialisierungsException {
		if (mqAnalyse == null) {
			mqAnalyse = analyseModul;
		}

		if (messQuerschnittVirtuell.getPid().equals("mq.MQ_61.240_HFB_NO_NACH")) {
			System.out.println();
		}

		mqT = ErfassungsIntervallDauerMQ.getInstanz(mqAnalyse.getDav(), messQuerschnittVirtuell);
		if (mqT == null) {
			throw new RuntimeException("Erfassungsintervalldauer von VMQ "
					+ messQuerschnittVirtuell + " kann nicht ermittelt werden.");
		}

		messQuerschnitt = messQuerschnittVirtuell;
		mqv = MessQuerschnittVirtuell.getInstanz(messQuerschnitt);

		lage = mqv.getMQVirtuellLage();

		final MessQuerschnitt mqVor = mqv.getMQVor();
		if (mqVor != null) {
			aktuelleMQAnalysen.put(mqVor.getSystemObject(), null);
			mqAufHauptfahrbahn.add(mqVor.getSystemObject());
		} else {
			mqVorErfasst = false;
		}

		final MessQuerschnitt mqMitte = mqv.getMQMitte();
		if (mqMitte != null) {
			aktuelleMQAnalysen.put(mqMitte.getSystemObject(), null);
			mqAufHauptfahrbahn.add(mqMitte.getSystemObject());
		} else {
			mqMitteErfasst = false;
		}

		final MessQuerschnitt mqNach = mqv.getMQNach();
		if (mqNach != null) {
			aktuelleMQAnalysen.put(mqNach.getSystemObject(), null);
			mqAufHauptfahrbahn.add(mqNach.getSystemObject());
		} else {
			mqNachErfasst = false;
		}

		final MessQuerschnitt mqEin = mqv.getMQEinfahrt();
		if (mqEin != null) {
			aktuelleMQAnalysen.put(mqEin.getSystemObject(), null);
		} else {
			mqEinfahrtErfasst = false;
		}

		final MessQuerschnitt mqAus = mqv.getMQAusfahrt();
		if (mqAus != null) {
			aktuelleMQAnalysen.put(mqAus.getSystemObject(), null);
		} else {
			mqAusfahrtErfasst = false;
		}

		boolean nichtBetrachtet = false;
		if (mqAufHauptfahrbahn.size() == 0) {
			LOGGER.warning("Auf der Hauptfahrbahn des " + //$NON-NLS-1$
					"virtuellen MQ " + messQuerschnittVirtuell + " sind keine MQ referenziert"); //$NON-NLS-1$
			nichtBetrachtet = true;
		}

		if ((lage == null) || nichtBetrachtet) {
			return null;
		}

		parameter = new AtgVerkehrsDatenKurzZeitAnalyseMq(mqAnalyse.getDav(), messQuerschnitt);
		mqAnalyse.getDav().subscribeReceiver(
				this,
				aktuelleMQAnalysen.keySet(),
				new DataDescription(mqAnalyse.getDav().getDataModel()
						.getAttributeGroup("atg.verkehrsDatenKurzZeitMq"), //$NON-NLS-1$
						mqAnalyse.getDav().getDataModel().getAspect("asp.analyse")), //$NON-NLS-1$
						ReceiveOptions.normal(), ReceiverRole.receiver());

		return this;
	}

	/**
	 * Dieser Methode sollten alle aktuellen Daten für alle mit diesem Messquerschnitt assoziierten
	 * Fahrstreifen übergeben werden. Ggf. wird dadurch dann eine Berechnung der Analysewerte dieses
	 * Messquerschnittes ausgelöst.
	 *
	 * @param triggerDatum
	 *            ein Analyse-Datum eines assoziierten Messquerschnitts
	 * @return ein Analysedatum für diesen virtuellen Messquerschnitt, wenn das
	 *         <code>triggerDatum</code> eine Berechnung ausgelöst hat, oder <code>null</code> sonst
	 */
	@Override
	public ResultData trigger(final ResultData triggerDatum) {
		if (messQuerschnitt.getPid().equals("mq.MQ_61.240_HFB_NO_NACH")) {
			System.out.println();
		}

		ResultData ergebnis = null;
		aktuelleMQAnalysen.put(triggerDatum.getObject(), triggerDatum);

		// if (this.isKeineDaten()) {
		// if (this.messQuerschnitt.getPid().equals(
		// "mq.MQ_61.240_HFB_NO_NACH")) {
		// System.out.println("keine Daten");
		// }
		// ergebnis = new ResultData(this.messQuerschnitt,
		// MqAnalyseModul.pubBeschreibung, triggerDatum.getDataTime(),
		// null);
		// } else {
		if (triggerDatum.getData() != null) {
			if (isAlleDatenVollstaendig()) {
				if (messQuerschnitt.getPid().equals("mq.MQ_61.240_HFB_NO_NACH")) {
					// for (SystemObject obj : this.aktuelleMQAnalysen
					// .keySet()) {
					// ResultData r = this.aktuelleMQAnalysen.get(obj);
					// if (r != null) {
					// System.out.println(obj.getPid()
					// + " --> "
					// + this.aktuelleMQAnalysen.get(obj)
					// .getDataTime());
					// } else {
					// System.out.println(obj.getPid() + " --> "
					// + this.aktuelleMQAnalysen.get(obj));
					// }
					// break;
					// }
					// System.out.println();
				}
				ergebnis = getErgebnisAufBasisAktuellerDaten();
			} else {
				/**
				 * Ggf. Timeout einstellen
				 */
				if (!WECKER.isWeckerGestelltFuer(this)) {
					final long tT = mqT.getT();
					if (tT != ErfassungsIntervallDauerMQ.NOCH_NICHT_ERMITTELBAR) {
						final long timeoutZeitStempel = triggerDatum.getDataTime() + tT + (tT / 2);
						WECKER.setWecker(this, timeoutZeitStempel);
					}
				}
			}
		}
		// }

		return ergebnis;
	}

	/**
	 * Ermittelt, ob dieser virtuelle Messquerschnitt zur Zeit auf <code>keine Daten</code> stehen
	 * sollte.<br>
	 * Dies ist dann der Fall, wenn fuer mindestens einen assoziierten Messquerschnitt keine
	 * Nutzdaten zur Verfuegung stehen.
	 *
	 * @return ob dieser virtuelle Messquerschnitt zur Zeit auf <code>keine Daten</code> stehen
	 *         sollte
	 */
	private boolean isKeineDaten() {
		synchronized (aktuelleMQAnalysen) {
			for (final SystemObject mq : aktuelleMQAnalysen.keySet()) {
				final ResultData aktuellesMQDatum = aktuelleMQAnalysen.get(mq);
				if ((aktuellesMQDatum == null) || (aktuellesMQDatum.getData() == null)) {
					return true;
				}
			}
		}

		return false;
	}

	/**
	 * Erfragt, ob von den MQ, die an diesem virtuellen MQ erfasst sind, alle ein Datum mit
	 * Nutzdaten geliefert haben, dessen Zeitstempel später als der des letzten hier errechneten
	 * Analysedatums ist.
	 *
	 * @return ob alle Daten zur Berechnung eines neuen Intervalls da sind
	 */
	protected final boolean isAlleDatenVollstaendig() {
		boolean alleDatenVollstaendig = true;

		final long letzteBerechnungsZeit = letztesErgebnis == null ? -1 : letztesErgebnis
				.getDataTime();

		if (messQuerschnitt.getPid().equals("mq.MQ_61.240_HFB_NO_NACH")) {
			System.out.println("Voll: " + new Date(letzteBerechnungsZeit));
		}

		if (mqVorErfasst) {
			final ResultData vorData = getMQData(mqv.getMQVor());
			alleDatenVollstaendig &= (vorData != null) && (vorData.getData() != null)
					&& (vorData.getDataTime() > letzteBerechnungsZeit);

			if ((vorData != null) && messQuerschnitt.getPid().equals("mq.MQ_61.240_HFB_NO_NACH")) {
				System.out.println("VOR: " + new Date(vorData.getDataTime()));
			}
		}
		if (alleDatenVollstaendig && mqNachErfasst) {
			final ResultData nachData = getMQData(mqv.getMQNach());
			alleDatenVollstaendig &= (nachData != null) && (nachData.getData() != null)
					&& (nachData.getDataTime() > letzteBerechnungsZeit);
			if ((nachData != null) && messQuerschnitt.getPid().equals("mq.MQ_61.240_HFB_NO_NACH")) {
				System.out.println("NACH: " + new Date(nachData.getDataTime()));
			}

		}
		if (alleDatenVollstaendig && mqMitteErfasst) {
			final ResultData mitteData = getMQData(mqv.getMQMitte());
			alleDatenVollstaendig &= (mitteData != null) && (mitteData.getData() != null)
					&& (mitteData.getDataTime() > letzteBerechnungsZeit);
			if ((mitteData != null) && messQuerschnitt.getPid().equals("mq.MQ_61.240_HFB_NO_NACH")) {
				System.out.println("MITTE: " + new Date(mitteData.getDataTime()));
			}

		}

		if (alleDatenVollstaendig && mqEinfahrtErfasst) {
			final ResultData einData = getMQData(mqv.getMQEinfahrt());
			alleDatenVollstaendig &= (einData != null) && (einData.getData() != null)
					&& (einData.getDataTime() > letzteBerechnungsZeit);
			if ((einData != null) && messQuerschnitt.getPid().equals("mq.MQ_61.240_HFB_NO_NACH")) {
				System.out.println("EIN: " + new Date(einData.getDataTime()));
			}

		}
		if (alleDatenVollstaendig && mqAusfahrtErfasst) {
			final ResultData ausData = getMQData(mqv.getMQAusfahrt());
			alleDatenVollstaendig &= (ausData != null) && (ausData.getData() != null)
					&& (ausData.getDataTime() > letzteBerechnungsZeit);
			if ((ausData != null) && messQuerschnitt.getPid().equals("mq.MQ_61.240_HFB_NO_NACH")) {
				System.out.println("AUS: " + new Date(ausData.getDataTime()));
			}

		}

		return alleDatenVollstaendig;
	}

	/**
	 * Diese Methode geht davon aus, dass keine weiteren Werte zur Berechnung des Analysedatums
	 * eintreffen werden und berechnet mit allen im Moment gepufferten Daten das Analysedatum.
	 *
	 * @return ein Analysedatum
	 */
	private synchronized ResultData getErgebnisAufBasisAktuellerDaten() {
		ResultData ergebnis = null;

		WECKER.setWecker(this, ObjektWecker.AUS);

		final Data analyseDatum = mqAnalyse.getDav().createData(
				MqAnalyseModul.pubBeschreibung.getAttributeGroup());

		/**
		 * Ermittle Werte für <code>VKfz, VLkw, VPkw, VgKfz, B, Bmax, SKfz</code> und
		 * <code>VDelta</code> via Ersetzung
		 */
		final String[] attErsetzung = new String[] { "VKfz", "VLkw", "VPkw", "VgKfz", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
				"B", "BMax", "SKfz", "VDelta" }; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		for (final String attName : attErsetzung) {
			final ResultData ersetzung = getErsatzDatum(attName);

			if (ersetzung != null) {
				new MesswertUnskaliert(attName, ersetzung.getData())
				.kopiereInhaltNachModifiziereIndex(analyseDatum);
			} else {
				LOGGER.error("Es konnte kein Ersetzungsdatum fuer " + messQuerschnitt + //$NON-NLS-1$
						" im Attribut " + attName + " ermittelt werden"); //$NON-NLS-1$ //$NON-NLS-2$
				final MesswertUnskaliert mw = new MesswertUnskaliert(attName);
				mw.setWertUnskaliert(DUAKonstanten.NICHT_ERMITTELBAR_BZW_FEHLERHAFT);
				mw.kopiereInhaltNachModifiziereIndex(analyseDatum);
			}
		}

		/**
		 * Ermittle Werte für <code>QKfz, QLkw</code> und <code>QPkw</code>
		 */
		final String[] attBilanz = new String[] { "QKfz", "QLkw", "QPkw" }; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		for (final String attName : attBilanz) {
			setBilanzDatum(analyseDatum, attName);
		}

		/**
		 * Berechne Werte für <code>ALkw, KKfz, KPkw, KLkw, QB</code> und <code>KB</code>
		 */
		berechneLkwAnteil(analyseDatum);
		berechneDichte(analyseDatum, "Kfz"); //$NON-NLS-1$
		berechneDichte(analyseDatum, "Lkw"); //$NON-NLS-1$
		berechneDichte(analyseDatum, "Pkw"); //$NON-NLS-1$
		berechneBemessungsVerkehrsstaerke(analyseDatum);
		berechneBemessungsdichte(analyseDatum);

		final ResultData aktuellesReferenzDatum = getAktuellesReferenzDatum();
		if ((aktuellesReferenzDatum != null) && (aktuellesReferenzDatum.getData() != null)) {
			ergebnis = new ResultData(messQuerschnitt, MqAnalyseModul.pubBeschreibung,
					aktuellesReferenzDatum.getDataTime(), analyseDatum);
		} else {
			/**
			 * Notbremse
			 */
			ergebnis = new ResultData(messQuerschnitt, MqAnalyseModul.pubBeschreibung,
					System.currentTimeMillis(), null);
		}

		return ergebnis;
	}

	/**
	 * Wird aufgerufen, wenn das Timeout für die Publikation eines Analysedatums überschritten
	 * wurde.
	 */
	@Override
	public void alarm() {
		final ResultData resultat = getErgebnisAufBasisAktuellerDaten();

		assert (resultat != null);

		if (messQuerschnitt.getPid().equals("mq.MQ_61.240_HFB_NO_NACH")) {
			System.out.println("alarm: " + new Date(resultat.getDataTime()));
		}

		publiziere(resultat);
	}

	/**
	 * Publiziert eine Analysedatum (so nicht <code>null</code> übergeben wurde).
	 *
	 * @param ergebnis
	 *            ein neu berechntes Analysedatum (oder <code>null</code>)
	 */
	private synchronized void publiziere(final ResultData ergebnis) {
		if (ergebnis != null) {

			if (messQuerschnitt.getPid().equals("mq.MQ_61.240_HFB_NO_NACH")) {
				System.out.println("1: " + new Date(ergebnis.getDataTime()));
			}

			/**
			 * nur echt neue Daten versenden
			 */
			if ((letztesErgebnis == null)
					|| (letztesErgebnis.getDataTime() < ergebnis.getDataTime())) {
				ResultData publikationsDatum = null;

				if (ergebnis.getData() == null) {
					/**
					 * Das folgende Flag zeigt an, ob dieser MQ zur Zeit auf "keine Daten" steht.
					 * Dies ist der Fall,<br>
					 * 1. wenn noch nie ein Datum für diesen MQ berechnet (versendet) wurde, oder<br>
					 * 2. wenn das letzte für diesen MQ berechnete (versendete) Datum keine Daten
					 * hatte.
					 */
					final boolean aktuellKeineDaten = (letztesErgebnis == null)
							|| (letztesErgebnis.getData() == null);

					if (!aktuellKeineDaten) {
						publikationsDatum = ergebnis;
					}
				} else {
					publikationsDatum = ergebnis;
					/**
					 * Ein Datum wurde berechnet. Lösche alle gepufferten MQ-Daten
					 */
					for (final SystemObject mq : aktuelleMQAnalysen.keySet()) {
						aktuelleMQAnalysen.put(mq, null);
					}
				}

				if (publikationsDatum != null) {
					letztesErgebnis = ergebnis;

					if (messQuerschnitt.getPid().equals("mq.MQ_61.240_HFB_NO_NACH")) {
						System.out.println("2: " + new Date(letztesErgebnis.getDataTime()));
					}

					mqAnalyse.sendeDaten(publikationsDatum);
				}
			}
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void update(final ResultData[] resultate) {
		if (resultate != null) {
			for (final ResultData resultat : resultate) {
				if (resultat != null) {
					publiziere(trigger(resultat));
				}
			}
		}
	}

	/**
	 * ************************************************************************* *
	 * Berechnungs-Methoden * *
	 * ************************************************************************.
	 *
	 * @param attName
	 *            the att name
	 * @return the ersatz datum
	 */

	/**
	 * Erfragt das Ersatzdatum für diesen virtuellen Messquerschnitt in den Attributen
	 * <code>VKfz, VLkw, VPkw, VgKfz, B, Bmax, SKfz</code> und <code>VDelta</code>.
	 *
	 * @param attName
	 *            der Name des Attributs, für das ein Ersatzdatum gefunden werden soll
	 * @return das Ersatzdatum für diesen virtuellen Messquerschnitt in den Attributen
	 *         <code>VKfz, VLkw, VPkw, VgKfz, B, Bmax, SKfz</code> und <code>VDelta</code> oder
	 *         <code>null</code>, wenn dieses nicht ermittelt werden konnte, weil z.B. alle MQs
	 *         erfasst sind (wäre ein Konfigurationsfehler)
	 */
	private ResultData getErsatzDatum(final String attName) {
		ResultData ersatzDatum = null;

		if (lage.equals(MessQuerschnittVirtuellLage.VOR)) {
			/**
			 * 1. MQVor nicht direkt erfasst
			 */
			final ResultData mqDataMitte = getMQData(mqv.getMQMitte());

			if (isDatumOk(mqDataMitte)) {
				ersatzDatum = mqDataMitte;
			}

			if (!isDatumNutzbar(ersatzDatum, attName)) {
				final ResultData mqDataNach = getMQData(mqv.getMQNach());
				if (isDatumOk(mqDataNach)) {
					ersatzDatum = mqDataNach;
				}
			}
		} else if (lage.equals(MessQuerschnittVirtuellLage.MITTE)) {
			/**
			 * 2. MQMitte nicht direkt erfasst
			 */
			final ResultData mqDataVor = getMQData(mqv.getMQVor());

			if (isDatumOk(mqDataVor)) {
				ersatzDatum = mqDataVor;
			}

			if (!isDatumNutzbar(ersatzDatum, attName)) {
				final ResultData mqDataNach = getMQData(mqv.getMQNach());
				if (isDatumOk(mqDataNach)) {
					ersatzDatum = mqDataNach;
				}
			}
		} else if (lage.equals(MessQuerschnittVirtuellLage.NACH)) {
			/**
			 * 3. MQNach nicht direkt erfasst
			 */
			final ResultData mqDataMitte = getMQData(mqv.getMQMitte());

			if (isDatumOk(mqDataMitte)) {
				ersatzDatum = mqDataMitte;
			}

			if (!isDatumNutzbar(ersatzDatum, attName)) {
				final ResultData mqDataVor = getMQData(mqv.getMQVor());
				if (isDatumOk(mqDataVor)) {
					ersatzDatum = mqDataVor;
				}
			}
		}

		return ersatzDatum;
	}

	/**
	 * Erfragt das aktuelle Referenzdatum. Das ist das Datum, dessen Zeitstempel und Intervall mit
	 * dem des Analysedatums identisch ist, dass auf Basis der aktuellen Daten produziert werden
	 * kann
	 *
	 * @return das aktuelle Referenzdatum, oder <code>null</code>
	 */
	private ResultData getAktuellesReferenzDatum() {
		ResultData ergebnis = null;

		for (final ResultData aktuellesDatum : aktuelleMQAnalysen.values()) {
			if ((aktuellesDatum != null) && (aktuellesDatum.getData() != null)) {
				/**
				 * Dieses Datum wird zur Berechnung des Ausgangsdatums herangezogen
				 */
				ergebnis = aktuellesDatum;
			}
		}

		return ergebnis;
	}

	/**
	 * Gets the MQ data.
	 *
	 * @param mq
	 *            the mq
	 * @return the MQ data
	 */
	private ResultData getMQData(final MessQuerschnitt mq) {
		ResultData rd = null;
		if (mq != null) {
			rd = aktuelleMQAnalysen.get(mq.getSystemObject());
		}
		return rd;
	}

	/**
	 * Setzt die Verkehrsstärke für diesen virtuellen Messquerschnitt in den Attributen
	 * <code>QKfz, QLkw</code> und <code>QPkw</code>.
	 *
	 * @param analyseDatum
	 *            the analyse datum
	 * @param attName
	 *            der Name des Attributs, für das die Verkehrsstärke gesetzt werden soll
	 */
	private void setBilanzDatum(final Data analyseDatum, final String attName) {
		QWert Q = null;

		if (lage.equals(MessQuerschnittVirtuellLage.VOR)) {
			/**
			 * 1. MQVor nicht direkt erfasst: Q(MQVor)=Q(MQMitte)+Q(MQAus). Wenn an MQMitte der
			 * jeweilige Wert nicht vorhanden ist, gilt: Q(MQVor)=Q(MQNach)+Q(MQAus)-Q(MQEin).
			 */
			final QWert QMitte = new QWert(getMQData(mqv.getMQMitte()), attName);
			final QWert QAus = new QWert(getMQData(mqv.getMQAusfahrt()), attName);

			Q = QWert.summe(QMitte, QAus);

			if ((Q == null) || !Q.isExportierbarNach(analyseDatum) || !Q.isVerrechenbar()) {
				final QWert QNach = new QWert(getMQData(mqv.getMQNach()), attName);
				final QWert QEin = new QWert(getMQData(mqv.getMQEinfahrt()), attName);

				if (QNach.isVerrechenbar() && QEin.isVerrechenbar()) {
					if ((Q == null) || !Q.isExportierbarNach(analyseDatum)) {
						Q = QWert.differenz(QWert.summe(QNach, QAus), QEin);
					} else {
						/**
						 * Also Q != null und Q ist exportierbar
						 */
						if (!Q.isVerrechenbar()) {
							final QWert dummy = QWert.differenz(QWert.summe(QNach, QAus), QEin);
							if ((dummy != null) && dummy.isExportierbarNach(analyseDatum)
									&& dummy.isVerrechenbar()) {
								Q = dummy;
							}
						}
					}
				}
			}
		} else if (lage.equals(MessQuerschnittVirtuellLage.MITTE)) {
			/**
			 * 2. MQMitte nicht direkt erfasst: Q(MQMitte)=Q(MQVor)-Q(MQAus). Wenn an MQVor der
			 * jeweilige Wert nicht vorhanden ist, gilt Q(MQMitte)=Q(MQNach)-Q(MQEin).
			 */
			final QWert QVor = new QWert(getMQData(mqv.getMQVor()), attName);
			final QWert QAus = new QWert(getMQData(mqv.getMQAusfahrt()), attName);

			Q = QWert.differenz(QVor, QAus);

			if ((Q == null) || !Q.isExportierbarNach(analyseDatum) || !Q.isVerrechenbar()) {
				final QWert QNach = new QWert(getMQData(mqv.getMQNach()), attName);
				final QWert QEin = new QWert(getMQData(mqv.getMQEinfahrt()), attName);

				if (QNach.isVerrechenbar() && QEin.isVerrechenbar()) {
					if ((Q == null) || !Q.isExportierbarNach(analyseDatum)) {
						Q = QWert.differenz(QNach, QEin);
					} else {
						/**
						 * Also Q != null und Q ist exportierbar
						 */
						if (!Q.isVerrechenbar()) {
							final QWert dummy = QWert.differenz(QNach, QEin);
							if ((dummy != null) && dummy.isExportierbarNach(analyseDatum)
									&& dummy.isVerrechenbar()) {
								Q = dummy;
							}
						}
					}
				}
			}
		} else if (lage.equals(MessQuerschnittVirtuellLage.NACH)) {
			/**
			 * 3. MQNach nicht direkt erfasst Q(MQNach)=Q(MQMitte)+Q(MQEin). Wenn an MQMitte der
			 * jeweilige Wert nicht vorhanden ist, gilt Q(MQNach)=Q(MQVor)+Q(MQEin)-Q(MQAus).
			 */
			final QWert QMitte = new QWert(getMQData(mqv.getMQMitte()), attName);
			final QWert QEin = new QWert(getMQData(mqv.getMQEinfahrt()), attName);

			Q = QWert.summe(QMitte, QEin);

			if ((Q == null) || !Q.isExportierbarNach(analyseDatum) || !Q.isVerrechenbar()) {
				final QWert QVor = new QWert(getMQData(mqv.getMQVor()), attName);
				final QWert QAus = new QWert(getMQData(mqv.getMQAusfahrt()), attName);

				if (QVor.isVerrechenbar() && QAus.isVerrechenbar()) {
					if ((Q == null) || !Q.isExportierbarNach(analyseDatum)) {
						Q = QWert.differenz(QWert.summe(QVor, QEin), QAus);
					} else {
						/**
						 * Also Q != null und Q ist exportierbar
						 */
						if (!Q.isVerrechenbar()) {
							final QWert dummy = QWert.differenz(QWert.summe(QVor, QEin), QAus);
							if ((dummy != null) && dummy.isExportierbarNach(analyseDatum)
									&& dummy.isVerrechenbar()) {
								Q = dummy;
							}
						}
					}
				}
			}
		}

		MesswertUnskaliert mw = new MesswertUnskaliert(attName);
		if ((Q == null) || !Q.isExportierbarNach(analyseDatum)) {
			mw.setWertUnskaliert(DUAKonstanten.NICHT_ERMITTELBAR_BZW_FEHLERHAFT);
		} else {
			mw = Q.getWert();
		}
		mw.kopiereInhaltNachModifiziereIndex(analyseDatum);
	}

	/**
	 * Erfragt, ob das übergebene Datum im Sinne der Wertersetzung brauchbar ist. Dies ist dann der
	 * Fall, wenn das Datum Nutzdaten enthält und dessen Datenzeit echt älter als die des letzten
	 * publizierten Analysedatums ist.
	 *
	 * @param datum
	 *            ein Analysedatum eines MQ
	 * @return ob das übergebene Datum im Sinne der Wertersetzung brauchbar ist
	 */
	private boolean isDatumOk(final ResultData datum) {
		boolean ergebnis = false;

		if ((datum != null) && (datum.getData() != null)) {
			final long letzterAnalyseZeitStempel = letztesErgebnis == null ? -1 : letztesErgebnis
					.getDataTime();
			ergebnis = datum.getDataTime() > letzterAnalyseZeitStempel;
		}

		return ergebnis;
	}

	/**
	 * Erfragt, ob das übergebene Datum im übergebenen Attribut sinnvolle Nutzdaten (Werte >= 0
	 * hat).
	 *
	 * @param datum
	 *            ein Analysedatum
	 * @param attName
	 *            der Name des Attributs
	 * @return ob das übergebene Datum im übergebenen Attribut sinnvolle Daten
	 */
	private boolean isDatumNutzbar(final ResultData datum, final String attName) {
		boolean ergebnis = false;

		if ((datum != null) && (datum.getData() != null)) {
			ergebnis = new MesswertUnskaliert(attName, datum.getData()).getWertUnskaliert() >= 0;
		}

		return ergebnis;
	}
}
