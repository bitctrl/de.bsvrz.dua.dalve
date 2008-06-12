/**
 * Segment 4 Datenübernahme und Aufbereitung (DUA), SWE 4.7 Datenaufbereitung LVE
 * Copyright (C) 2007 BitCtrl Systems GmbH 
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

/**
 * In diesem Objekt werden alle aktuellen Werte die zur Berechnung der
 * Analysewerte eines virtuellen Messquerschnitts notwendig sind gespeichert.
 * Wenn die Werte für ein bestimmtes Intervall bereit stehen (oder eine Timeout
 * abgelaufen ist), wird eine Berechnung durchgeführt und der Wert publiziert.
 * <br><b>Achtung: Verfahren auf Basis der Konfigurationsdaten aus 
 * Attributgruppe <code>atg.messQuerschnittVirtuellStandard</code>.</b><br>
 * 
 * @author BitCtrl Systems GmbH, Thierfelder
 * 
 * @version $Id$
 */
public class DaAnalyseMessQuerschnittVirtuellStandard extends
		DaAnalyseMessQuerschnitt implements IObjektWeckerListener {

	/**
	 * Informiert dieses Objekt darüber, dass das Timeout für die Berechnung der
	 * Analysedaten abgelaufen ist.
	 */
	private static final ObjektWecker WECKER = new ObjektWecker();

	/**
	 * Mapt alle hier betrachteten Messquerschnitte auf das letzte von ihnen
	 * empfangene Analysedatum.
	 */
	private Map<SystemObject, ResultData> aktuelleMQAnalysen = new HashMap<SystemObject, ResultData>();

	/**
	 * Alle MQ, die auf der Hauptfahrbahn liegen.
	 */
	private Collection<SystemObject> mqAufHauptfahrbahn = new HashSet<SystemObject>();

	/**
	 * der aufgelößte virtuelle Messquerschnitt.
	 */
	private MessQuerschnittVirtuell mqv = null;

	/**
	 * Zeigt an, ob der Messquerschnitt <b>vor</b> der Anschlussstelle, die
	 * durch diesen virtuellen MQ repräsentiert wird, direkt erfasst ist.
	 */
	private boolean mqVorErfasst = true;

	/**
	 * Zeigt an, ob der Messquerschnitt <b>nach</b> der Anschlussstelle, die
	 * durch diesen virtuellen MQ repräsentiert wird, direkt erfasst ist.
	 */
	private boolean mqNachErfasst = true;

	/**
	 * Zeigt an, ob der Messquerschnitt <b>mittig</b> der Anschlussstelle, die
	 * durch diesen virtuellen MQ repräsentiert wird, direkt erfasst ist.
	 */
	private boolean mqMitteErfasst = true;

	/**
	 * Zeigt an, ob der Messquerschnitt <b>Einfahrt</b> der Anschlussstelle,
	 * die durch diesen virtuellen MQ repräsentiert wird, direkt erfasst ist.
	 */
	private boolean mqEinfahrtErfasst = false;

	/**
	 * Zeigt an, ob der Messquerschnitt <b>Ausfahrt</b> der Anschlussstelle,
	 * die durch diesen virtuellen MQ repräsentiert wird, direkt erfasst ist.
	 */
	private boolean mqAusfahrtErfasst = false;

	/**
	 * Tracker fuer die Erfassungsintervalldauer des MQ.
	 */
	private ErfassungsIntervallDauerMQ mqT = null;

	/**
	 * Initialisiert dieses Objekt und gibt die initialisierte Instanz zurück.
	 * Nach dieser Initialisierung ist das Objekt auf alle Daten (seiner
	 * assoziierten Messquerschnitte) angemeldet und analysiert ggf. Daten
	 * 
	 * @param analyseModul
	 *            Verbindung zum Analysemodul (zum Publizieren)
	 * @param messQuerschnittVirtuell
	 *            der virtuelle Messquerschnitt
	 * @return die initialisierte Instanz dieses Objekts
	 * @throws DUAInitialisierungsException
	 *             wenn die Konfigurationsdaten des virtuellen MQs nicht
	 *             vollständig ausgelesen werden konnte
	 */
	public DaAnalyseMessQuerschnittVirtuellStandard initialisiere(
			MqAnalyseModul analyseModul, SystemObject messQuerschnittVirtuell)
			throws DUAInitialisierungsException {

		this.mqT = ErfassungsIntervallDauerMQ.getInstanz(mqAnalyse.getDav(),
				messQuerschnittVirtuell);
		if (this.mqT == null) {
			throw new RuntimeException("Erfassungsintervalldauer von VMQ "
					+ messQuerschnittVirtuell + " kann nicht ermittelt werden.");
		}

		if (mqAnalyse == null) {
			mqAnalyse = analyseModul;
		}
		this.messQuerschnitt = messQuerschnittVirtuell;
		this.mqv = MessQuerschnittVirtuell.getInstanz(messQuerschnitt);

		if (mqv.getMQVirtuellLage().equals(MessQuerschnittVirtuellLage.VOR)) {
			this.mqVorErfasst = false;
		} else if (mqv.getMQVirtuellLage().equals(
				MessQuerschnittVirtuellLage.MITTE)) {
			this.mqMitteErfasst = false;
		} else if (mqv.getMQVirtuellLage().equals(
				MessQuerschnittVirtuellLage.NACH)) {
			this.mqNachErfasst = false;
		} else {
			throw new DUAInitialisierungsException(
					"Virtueller Messquerschnitt " + messQuerschnittVirtuell
							+ " kann nicht ausgewertet werden. Grund: Lage ("
							+ mqv.getMQVirtuellLage() + ")");
		}

		MessQuerschnitt mqVor = mqv.getMQVor();
		if (mqVor != null) {
			this.aktuelleMQAnalysen.put(mqVor.getSystemObject(), null);
			this.mqAufHauptfahrbahn.add(mqVor.getSystemObject());
		}

		MessQuerschnitt mqMitte = mqv.getMQMitte();
		if (mqMitte != null) {
			this.aktuelleMQAnalysen.put(mqMitte.getSystemObject(), null);
			this.mqAufHauptfahrbahn.add(mqMitte.getSystemObject());
		}

		MessQuerschnitt mqNach = mqv.getMQNach();
		if (mqNach != null) {
			this.aktuelleMQAnalysen.put(mqNach.getSystemObject(), null);
			this.mqAufHauptfahrbahn.add(mqNach.getSystemObject());
		}

		MessQuerschnitt mqEin = mqv.getMQEinfahrt();
		if (mqEin != null) {
			this.mqEinfahrtErfasst = true;
			this.aktuelleMQAnalysen.put(mqEin.getSystemObject(), null);
		}

		MessQuerschnitt mqAus = mqv.getMQAusfahrt();
		if (mqAus != null) {
			this.mqAusfahrtErfasst = true;
			this.aktuelleMQAnalysen.put(mqAus.getSystemObject(), null);
		}

		boolean nichtBetrachtet = false;
		if (this.mqAufHauptfahrbahn.size() == 0) {
			Debug.getLogger().warning(
					"Auf der Hauptfahrbahn des " + //$NON-NLS-1$
							"virtuellen MQ " + messQuerschnittVirtuell
							+ " sind keine MQ referenziert"); //$NON-NLS-1$ //$NON-NLS-2$
			nichtBetrachtet = true;
		}
		if (!mqAusfahrtErfasst || !mqEinfahrtErfasst) {
			Debug
					.getLogger()
					.warning("Beim virtuellen MQ " + messQuerschnittVirtuell + //$NON-NLS-1$
							" sind nicht Ein- UND Ausfahrt definiert (beide gleichzeitig)"); //$NON-NLS-1$
			nichtBetrachtet = true;
		}

		if (nichtBetrachtet) {
			return null;
		}

		this.parameter = new AtgVerkehrsDatenKurzZeitAnalyseMq(mqAnalyse
				.getDav(), messQuerschnitt);
		mqAnalyse.getDav().subscribeReceiver(
				this,
				this.aktuelleMQAnalysen.keySet(),
				new DataDescription(mqAnalyse.getDav().getDataModel()
						.getAttributeGroup("atg.verkehrsDatenKurzZeitMq"), //$NON-NLS-1$
						mqAnalyse.getDav().getDataModel().getAspect(
								"asp.analyse"), //$NON-NLS-1$
						(short) 0), ReceiveOptions.normal(),
				ReceiverRole.receiver());

		return this;
	}

	/**
	 * Dieser Methode sollten alle aktuellen Daten für alle mit diesem
	 * Messquerschnitt assoziierten Fahrstreifen übergeben werden. Ggf. wird
	 * dadurch dann eine Berechnung der Analysewerte dieses Messquerschnittes
	 * ausgelöst.
	 * 
	 * @param triggerDatum
	 *            ein Analyse-Datum eines assoziierten Messquerschnitts
	 * @return ein Analysedatum für diesen virtuellen Messquerschnitt, wenn das
	 *         <code>triggerDatum</code> eine Berechnung ausgelöst hat, oder
	 *         <code>null</code> sonst
	 */
	@Override
	public ResultData trigger(ResultData triggerDatum) {
		ResultData ergebnis = null;
		this.aktuelleMQAnalysen.put(triggerDatum.getObject(), triggerDatum);

		if (this.isKeineDaten()) {
			ergebnis = new ResultData(this.messQuerschnitt,
					MqAnalyseModul.pubBeschreibung,
					triggerDatum.getDataTime(), null);
		} else {
			if (triggerDatum.getData() != null) {
				if (this.isAlleDatenVollstaendig()) {
					ergebnis = this.getErgebnisAufBasisAktuellerDaten();
				} else {
					/**
					 * Ggf. Timeout einstellen
					 */
					if (!WECKER.isWeckerGestelltFuer(this)) {
						long tT = this.mqT.getT();
						if (tT != ErfassungsIntervallDauerMQ.NOCH_NICHT_ERMITTELBAR) {
							long timeoutZeitStempel = triggerDatum
									.getDataTime()
									+ tT + tT / 2;
							WECKER.setWecker(this, timeoutZeitStempel);
						}
					}
				}
			}
		}

		return ergebnis;
	}

	/**
	 * Ermittelt, ob dieser virtuelle Messquerschnitt zur Zeit auf
	 * <code>keine Daten</code> stehen sollte.<br>
	 * Dies ist dann der Fall, wenn fuer mindestens einen assoziierten
	 * Messquerschnitt keine Nutzdaten zur Verfuegung stehen.
	 * 
	 * @return ob dieser virtuelle Messquerschnitt zur Zeit auf
	 *         <code>keine Daten</code> stehen sollte
	 */
	private boolean isKeineDaten() {
		synchronized (this.aktuelleMQAnalysen) {
			for (SystemObject mq : this.aktuelleMQAnalysen.keySet()) {
				ResultData aktuellesMQDatum = this.aktuelleMQAnalysen.get(mq);
				if (aktuellesMQDatum == null
						|| aktuellesMQDatum.getData() == null) {
					return true;
				}
			}
		}

		return false;
	}

	/**
	 * Erfragt, ob von den MQ, die an diesem virtuellen MQ erfasst sind, alle
	 * ein Datum mit Nutzdaten geliefert haben, dessen Zeitstempel später als
	 * der des letzten hier errechneten Analysedatums ist.
	 * 
	 * @return ob alle Daten zur Berechnung eines neuen Intervalls da sind
	 */
	protected final boolean isAlleDatenVollstaendig() {
		boolean alleDatenVollstaendig = true;

		long letzteBerechnungsZeit = this.letztesErgebnis == null ? -1
				: this.letztesErgebnis.getDataTime();

		if (this.mqVorErfasst) {
			ResultData vorData = this.aktuelleMQAnalysen.get(this.mqv
					.getMQVor().getSystemObject());
			alleDatenVollstaendig &= vorData != null
					&& vorData.getData() != null
					&& vorData.getDataTime() > letzteBerechnungsZeit;
		}
		if (alleDatenVollstaendig && this.mqNachErfasst) {
			ResultData nachData = this.aktuelleMQAnalysen.get(this.mqv
					.getMQNach().getSystemObject());
			alleDatenVollstaendig &= nachData != null
					&& nachData.getData() != null
					&& nachData.getDataTime() > letzteBerechnungsZeit;
		}
		if (alleDatenVollstaendig && this.mqMitteErfasst) {
			ResultData mitteData = this.aktuelleMQAnalysen.get(this.mqv
					.getMQMitte().getSystemObject());
			alleDatenVollstaendig &= mitteData != null
					&& mitteData.getData() != null
					&& mitteData.getDataTime() > letzteBerechnungsZeit;
		}

		if (alleDatenVollstaendig && this.mqEinfahrtErfasst) {
			ResultData einData = this.aktuelleMQAnalysen.get(this.mqv
					.getMQEinfahrt().getSystemObject());
			alleDatenVollstaendig &= einData != null
					&& einData.getData() != null
					&& einData.getDataTime() > letzteBerechnungsZeit;
		}
		if (alleDatenVollstaendig && this.mqAusfahrtErfasst) {
			ResultData ausData = this.aktuelleMQAnalysen.get(this.mqv
					.getMQAusfahrt().getSystemObject());
			alleDatenVollstaendig &= ausData != null
					&& ausData.getData() != null
					&& ausData.getDataTime() > letzteBerechnungsZeit;
		}

		return alleDatenVollstaendig;
	}

	/**
	 * Diese Methode geht davon aus, dass keine weiteren Werte zur Berechnung
	 * des Analysedatums eintreffen werden und berechnet mit allen im Moment
	 * gepufferten Daten das Analysedatum.
	 * 
	 * @return ein Analysedatum
	 */
	private synchronized ResultData getErgebnisAufBasisAktuellerDaten() {
		ResultData ergebnis = null;

		WECKER.setWecker(this, ObjektWecker.AUS);

		Data analyseDatum = mqAnalyse.getDav().createData(
				MqAnalyseModul.pubBeschreibung.getAttributeGroup());

		/**
		 * Ermittle Werte für
		 * <code>VKfz, VLkw, VPkw, VgKfz, B, Bmax, SKfz</code> und
		 * <code>VDelta</code> via Ersetzung
		 */
		final String[] attErsetzung = new String[] {
				"VKfz", "VLkw", "VPkw", "VgKfz", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
				"B", "BMax", "SKfz", "VDelta" }; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$		
		for (String attName : attErsetzung) {
			ResultData ersetzung = this.getErsatzDatum(attName);

			if (ersetzung != null) {
				new MesswertUnskaliert(attName, ersetzung.getData())
						.kopiereInhaltNachModifiziereIndex(analyseDatum);
			} else {
				Debug
						.getLogger()
						.error(
								"Es konnte kein Ersetzungsdatum fuer " + this.messQuerschnitt + //$NON-NLS-1$
										" im Attribut " + attName
										+ " ermittelt werden"); //$NON-NLS-1$ //$NON-NLS-2$					
				MesswertUnskaliert mw = new MesswertUnskaliert(attName);
				mw
						.setWertUnskaliert(DUAKonstanten.NICHT_ERMITTELBAR_BZW_FEHLERHAFT);
				mw.kopiereInhaltNachModifiziereIndex(analyseDatum);
			}
		}

		/**
		 * Ermittle Werte für <code>QKfz, QLkw</code> und <code>QPkw</code>
		 */
		final String[] attBilanz = new String[] { "QKfz", "QLkw", "QPkw" }; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$		
		for (String attName : attBilanz) {
			this.setBilanzDatum(analyseDatum, attName);
		}

		/**
		 * Berechne Werte für <code>ALkw, KKfz, KPkw, KLkw, QB</code> und
		 * <code>KB</code>
		 */
		this.berechneLkwAnteil(analyseDatum);
		this.berechneDichte(analyseDatum, "Kfz"); //$NON-NLS-1$
		this.berechneDichte(analyseDatum, "Lkw"); //$NON-NLS-1$
		this.berechneDichte(analyseDatum, "Pkw"); //$NON-NLS-1$
		this.berechneBemessungsVerkehrsstaerke(analyseDatum);
		this.berechneBemessungsdichte(analyseDatum);

		ResultData aktuellesReferenzDatum = this.getAktuellesReferenzDatum();
		if (aktuellesReferenzDatum == null
				|| aktuellesReferenzDatum.getData() == null) {
			ergebnis = new ResultData(this.messQuerschnitt,
					MqAnalyseModul.pubBeschreibung, aktuellesReferenzDatum
							.getDataTime(), analyseDatum);
		} else {
			/**
			 * Notbremse
			 */
			ergebnis = new ResultData(this.messQuerschnitt,
					MqAnalyseModul.pubBeschreibung,
					System.currentTimeMillis(), null);
		}

		return ergebnis;
	}

	/**
	 * Wird aufgerufen, wenn das Timeout für die Publikation eines Analysedatums
	 * überschritten wurde.
	 */
	public void alarm() {
		ResultData resultat = this.getErgebnisAufBasisAktuellerDaten();

		assert (resultat != null);

		this.publiziere(resultat);
	}

	/**
	 * Publiziert eine Analysedatum (so nicht <code>null</code> übergeben
	 * wurde).
	 * 
	 * @param ergebnis
	 *            ein neu berechntes Analysedatum (oder <code>null</code>)
	 */
	private synchronized void publiziere(final ResultData ergebnis) {
		if (ergebnis != null) {
			/**
			 * nur echt neue Daten versenden
			 */
			if (this.letztesErgebnis == null
					|| this.letztesErgebnis.getDataTime() < ergebnis
							.getDataTime()) {
				ResultData publikationsDatum = null;

				if (ergebnis.getData() == null) {
					/**
					 * Das folgende Flag zeigt an, ob dieser MQ zur Zeit auf
					 * "keine Daten" steht. Dies ist der Fall,<br>
					 * 1. wenn noch nie ein Datum für diesen MQ berechnet
					 * (versendet) wurde, oder<br>
					 * 2. wenn das letzte für diesen MQ berechnete (versendete)
					 * Datum keine Daten hatte.
					 */
					boolean aktuellKeineDaten = this.letztesErgebnis == null
							|| this.letztesErgebnis.getData() == null;

					if (!aktuellKeineDaten) {
						publikationsDatum = ergebnis;
					}
				} else {
					publikationsDatum = ergebnis;
					/**
					 * Ein Datum wurde berechnet. Lösche alle gepufferten
					 * MQ-Daten
					 */
					for (SystemObject mq : this.aktuelleMQAnalysen.keySet()) {
						this.aktuelleMQAnalysen.put(mq, null);
					}
				}

				if (publikationsDatum != null) {
					this.letztesErgebnis = ergebnis;
					mqAnalyse.sendeDaten(publikationsDatum);
				}
			}
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void update(ResultData[] resultate) {
		if (resultate != null) {
			for (ResultData resultat : resultate) {
				if (resultat != null) {
					this.publiziere(this.trigger(resultat));
				}
			}
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void finalize() throws Throwable {
		Debug.getLogger().warning("Der virtuelle MQ " + this.messQuerschnitt + //$NON-NLS-1$
				" wird nicht mehr analysiert"); //$NON-NLS-1$
	}

	/***************************************************************************
	 * * Berechnungs-Methoden * *
	 **************************************************************************/

	/**
	 * Erfragt das Ersatzdatum für diesen virtuellen Messquerschnitt in den
	 * Attributen <code>VKfz, VLkw, VPkw, VgKfz, B, Bmax, SKfz</code> und
	 * <code>VDelta</code>.
	 * 
	 * @param attName
	 *            der Name des Attributs, für das ein Ersatzdatum gefunden
	 *            werden soll
	 * @return das Ersatzdatum für diesen virtuellen Messquerschnitt in den
	 *         Attributen <code>VKfz, VLkw, VPkw, VgKfz, B, Bmax, SKfz</code>
	 *         und <code>VDelta</code> oder <code>null</code>, wenn dieses
	 *         nicht ermittelt werden konnte, weil z.B. alle MQs erfasst sind
	 *         (wäre ein Konfigurationsfehler)
	 */
	private ResultData getErsatzDatum(String attName) {
		ResultData ersatzDatum = null;

		/**
		 * 1. MQVor nicht direkt erfasst
		 */
		if (!this.mqVorErfasst) {
			ResultData mqDataMitte = this.aktuelleMQAnalysen.get(this.mqv
					.getMQMitte());

			if (isDatumOk(mqDataMitte)) {
				ersatzDatum = mqDataMitte;
			}

			if (!isDatumNutzbar(ersatzDatum, attName)) {
				ResultData mqDataNach = this.aktuelleMQAnalysen.get(this.mqv
						.getMQNach());
				if (isDatumOk(mqDataNach)) {
					ersatzDatum = mqDataNach;
				}
			}
		}

		/**
		 * Wenn Ersetzungsdatum noch nicht gefunden ist, mache weiter
		 */
		if (!isDatumOk(ersatzDatum)) {

			/**
			 * 2. MQMitte nicht direkt erfasst
			 */
			if (!this.mqMitteErfasst) {
				ResultData mqDataVor = this.aktuelleMQAnalysen.get(this.mqv
						.getMQVor());

				if (isDatumOk(mqDataVor)) {
					ersatzDatum = mqDataVor;
				}

				if (!isDatumNutzbar(ersatzDatum, attName)) {
					ResultData mqDataNach = this.aktuelleMQAnalysen
							.get(this.mqv.getMQNach());
					if (isDatumOk(mqDataNach)) {
						ersatzDatum = mqDataNach;
					}
				}
			}

			if (!isDatumOk(ersatzDatum)) {

				/**
				 * 3. MQNach nicht direkt erfasst
				 */
				if (!this.mqNachErfasst) {
					ResultData mqDataMitte = this.aktuelleMQAnalysen
							.get(this.mqv.getMQMitte());

					if (isDatumOk(mqDataMitte)) {
						ersatzDatum = mqDataMitte;
					}

					if (!isDatumNutzbar(ersatzDatum, attName)) {
						ResultData mqDataVor = this.aktuelleMQAnalysen
								.get(this.mqv.getMQVor());
						if (isDatumOk(mqDataVor)) {
							ersatzDatum = mqDataVor;
						}
					}
				}
			}
		}

		return ersatzDatum;
	}

	/**
	 * Erfragt das aktuelle Referenzdatum. Das ist das Datum, dessen Zeitstempel
	 * und Intervall mit dem des Analysedatums identisch ist, dass auf Basis der
	 * aktuellen Daten produziert werden kann
	 * 
	 * @return das aktuelle Referenzdatum, oder <code>null</code>
	 */
	private ResultData getAktuellesReferenzDatum() {
		ResultData ergebnis = null;

		for (ResultData aktuellesDatum : this.aktuelleMQAnalysen.values()) {
			if (aktuellesDatum != null && aktuellesDatum.getData() != null) {
				/**
				 * Dieses Datum wird zur Berechnung des Ausgangsdatums
				 * herangezogen
				 */
				ergebnis = aktuellesDatum;
			}
		}

		return ergebnis;
	}

	/**
	 * Setzt die Verkehrsstärke für diesen virtuellen Messquerschnitt in den
	 * Attributen <code>QKfz, QLkw</code> und <code>QPkw</code>.
	 * 
	 * @param attName
	 *            der Name des Attributs, für das die Verkehrsstärke gesetzt
	 *            werden soll
	 */
	private final void setBilanzDatum(Data analyseDatum, String attName) {
		QWert Q = null;

		/**
		 * 1. MQVor nicht direkt erfasst: Q(MQVor)=Q(MQMitte)+Q(MQAus). Wenn an
		 * MQMitte der jeweilige Wert nicht vorhanden ist, gilt:
		 * Q(MQVor)=Q(MQNach)+Q(MQAus)-Q(MQEin).
		 */
		if (!this.mqVorErfasst) {
			QWert QMitte = new QWert(this.aktuelleMQAnalysen.get(this.mqv
					.getMQMitte()), attName);
			QWert QAus = new QWert(this.aktuelleMQAnalysen.get(this.mqv
					.getMQAusfahrt()), attName);

			Q = QWert.summe(QMitte, QAus);

			if (Q == null || !Q.isExportierbarNach(analyseDatum)
					|| !Q.isVerrechenbar()) {
				QWert QNach = new QWert(this.aktuelleMQAnalysen.get(this.mqv
						.getMQNach()), attName);
				QWert QEin = new QWert(this.aktuelleMQAnalysen.get(this.mqv
						.getMQEinfahrt()), attName);

				if (QNach.isVerrechenbar() && QEin.isVerrechenbar()) {
					if (Q == null || !Q.isExportierbarNach(analyseDatum)) {
						Q = QWert.differenz(QWert.summe(QNach, QAus), QEin);
					} else {
						/**
						 * Also Q != null und Q ist exportierbar
						 */
						if (!Q.isVerrechenbar()) {
							QWert dummy = QWert.differenz(QWert.summe(QNach,
									QAus), QEin);
							if (dummy != null
									&& dummy.isExportierbarNach(analyseDatum)
									&& dummy.isVerrechenbar()) {
								Q = dummy;
							}
						}
					}
				}
			}
		}

		/**
		 * Wenn Ersetzungsdatum noch nicht gefunden ist, mache weiter
		 */
		if (Q == null || !Q.isExportierbarNach(analyseDatum)
				|| !Q.isVerrechenbar()) {

			/**
			 * 2. MQMitte nicht direkt erfasst: Q(MQMitte)=Q(MQVor)-Q(MQAus).
			 * Wenn an MQVor der jeweilige Wert nicht vorhanden ist, gilt
			 * Q(MQMitte)=Q(MQNach)-Q(MQEin).
			 */
			if (!this.mqMitteErfasst) {
				QWert QVor = new QWert(this.aktuelleMQAnalysen.get(this.mqv
						.getMQVor()), attName);
				QWert QAus = new QWert(this.aktuelleMQAnalysen.get(this.mqv
						.getMQAusfahrt()), attName);

				Q = QWert.differenz(QVor, QAus);

				if (Q == null || !Q.isExportierbarNach(analyseDatum)
						|| !Q.isVerrechenbar()) {
					QWert QNach = new QWert(this.aktuelleMQAnalysen
							.get(this.mqv.getMQNach()), attName);
					QWert QEin = new QWert(this.aktuelleMQAnalysen.get(this.mqv
							.getMQEinfahrt()), attName);

					if (QNach.isVerrechenbar() && QEin.isVerrechenbar()) {
						if (Q == null || !Q.isExportierbarNach(analyseDatum)) {
							Q = QWert.differenz(QNach, QEin);
						} else {
							/**
							 * Also Q != null und Q ist exportierbar
							 */
							if (!Q.isVerrechenbar()) {
								QWert dummy = QWert.differenz(QNach, QEin);
								if (dummy != null
										&& dummy
												.isExportierbarNach(analyseDatum)
										&& dummy.isVerrechenbar()) {
									Q = dummy;
								}
							}
						}
					}
				}
			}

			if (Q == null || !Q.isExportierbarNach(analyseDatum)
					|| !Q.isVerrechenbar()) {

				/**
				 * 3. MQNach nicht direkt erfasst Q(MQNach)=Q(MQMitte)+Q(MQEin).
				 * Wenn an MQMitte der jeweilige Wert nicht vorhanden ist, gilt
				 * Q(MQNach)=Q(MQVor)+Q(MQEin)-Q(MQAus).
				 */
				if (!this.mqNachErfasst) {
					QWert QMitte = new QWert(this.aktuelleMQAnalysen
							.get(this.mqv.getMQMitte()), attName);
					QWert QEin = new QWert(this.aktuelleMQAnalysen.get(this.mqv
							.getMQEinfahrt()), attName);

					Q = QWert.summe(QMitte, QEin);

					if (Q == null || !Q.isExportierbarNach(analyseDatum)
							|| !Q.isVerrechenbar()) {
						QWert QVor = new QWert(this.aktuelleMQAnalysen
								.get(this.mqv.getMQVor()), attName);
						QWert QAus = new QWert(this.aktuelleMQAnalysen
								.get(this.mqv.getMQAusfahrt()), attName);

						if (QVor.isVerrechenbar() && QAus.isVerrechenbar()) {
							if (Q == null
									|| !Q.isExportierbarNach(analyseDatum)) {
								Q = QWert.differenz(QWert.summe(QVor, QEin),
										QAus);
							} else {
								/**
								 * Also Q != null und Q ist exportierbar
								 */
								if (!Q.isVerrechenbar()) {
									QWert dummy = QWert.differenz(QWert.summe(
											QVor, QEin), QAus);
									if (dummy != null
											&& dummy
													.isExportierbarNach(analyseDatum)
											&& dummy.isVerrechenbar()) {
										Q = dummy;
									}
								}
							}
						}
					}
				}
			}
		}

		MesswertUnskaliert mw = new MesswertUnskaliert(attName);
		if (Q == null || !Q.isExportierbarNach(analyseDatum)) {
			mw
					.setWertUnskaliert(DUAKonstanten.NICHT_ERMITTELBAR_BZW_FEHLERHAFT);
		} else {
			mw = Q.getWert();
		}
		mw.kopiereInhaltNachModifiziereIndex(analyseDatum);
	}

	/**
	 * Erfragt, ob das übergebene Datum im Sinne der Wertersetzung brauchbar
	 * ist. Dies ist dann der Fall, wenn das Datum Nutzdaten enthält und dessen
	 * Datenzeit echt älter als die des letzten publizierten Analysedatums ist.
	 * 
	 * @param datum
	 *            ein Analysedatum eines MQ
	 * @return ob das übergebene Datum im Sinne der Wertersetzung brauchbar ist
	 */
	private boolean isDatumOk(ResultData datum) {
		boolean ergebnis = false;

		if (datum != null && datum.getData() != null) {
			final long letzterAnalyseZeitStempel = this.letztesErgebnis == null ? -1
					: this.letztesErgebnis.getDataTime();
			ergebnis = datum.getDataTime() > letzterAnalyseZeitStempel;
		}

		return ergebnis;
	}

	/**
	 * Erfragt, ob das übergebene Datum im übergebenen Attribut sinnvolle
	 * Nutzdaten (Werte >= 0 hat).
	 * 
	 * @param datum
	 *            ein Analysedatum
	 * @param attName
	 *            der Name des Attributs
	 * @return ob das übergebene Datum im übergebenen Attribut sinnvolle Daten
	 */
	private boolean isDatumNutzbar(ResultData datum, String attName) {
		boolean ergebnis = false;

		if (datum != null && datum.getData() != null) {
			ergebnis = new MesswertUnskaliert(attName, datum.getData())
					.getWertUnskaliert() >= 0;
		}

		return ergebnis;
	}
}
